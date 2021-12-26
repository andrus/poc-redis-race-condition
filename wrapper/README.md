_This is a part of PoC repository. Source in this direvtory has no sence without big picture in mind, so please read initial readme in root of this repository._

# [PoC] Redis Race Condition

> **WARNING!** Part of this code base is written badly and contains logical errors - it's OK, due we are testing how to cook Redis well and though we are obliged to make mistakes.

## Motivation

After initial testing in a production-like environment under stress load we've been able to catch bad behaviour of demo. In timespan from 10 seconds to couple minutes (10000 to 10000000 requests in) situation of degreaded cache may occure. In last testing session we've limitted test case to stop at first degraded response found (ability to restore bad value with help of more blind-writing clients was ommited) to limit test time. It's important to keep in mind that in case of one degraded response it's not 100% 'endgame' situation, if there is no sync facilities involved we can end up with right value or wrong whichever client had written list value in update sequence - so even if one degraded occures it may be a problem and we'll try to eliminate it in this side-project.

## Stand description

To make reprodction more stable we need to use single point of orchestration - there will be no services. Also we don't really need network lags and docker-induced complications - so this test stand is a simple Java application running jUnit tests as a common one (without bells and whistles).

In initial PoC we've been dealing ith database, and in current circumstances it makes things harder, because DB itself make serives kida synchronized (not fully, but there is not such problem as value degradation due to ACID principle and single updater service), but we can imitate DB access in code.

We are using testcontainers to run Redis in a docker - this is a compromise, we could ask tester to run service by-hand, but it will mean that we won't have fresh Redis before every test (and we want that, because we are for equal rights for every test case :) ), or tester will need to run tests one by one - it's exhausting and doesn't make any sence.

Also this test stand is in same repository and you can ask: "Dude, what the hell?" - reason behind is that we gonna use same code from stand in production-like test than. It will be a good use for already done job, and we'll be able to test each solution under load simply by swithcnig implementation classes.

Lastly, we are trying to make a clear box, not a black one - test must be verbose enought to see what is happening inside easily.

## Initial assumptions

There is only one actual way to degrade cache - to give last in a queue of writers bad value. This can be reproduced with any number of threads, but to keep things simple we will try to do our best with two threads and hand crafted (eco, vegan, bio-dgradable, soy-based, gluten-free) delays. In the table below time shedule for degraded cache is depicted - we'll try to replicate this behaviour first and than find a solution.

| Time |  A  |  B  | Database | Redis | Data flow |
| :--: | :-: | :-: | :------: | :---: | --------- |
|  0   |  —  |  —  |   old    |  old  | —         |
|  1   |  —  | old |   old    |  old  | Redis → A |
|  2   | old |  —  |   old    |  old  | Redis → B |

Initial state (time 00) both threads don't have a clue about value is, so after they both got a request they'll run to cache (Redis) see an old and return it (time 01-02) - nice.

| Time |  A  |  B  | Database | Redis | Data flow    |
| :--: | :-: | :-: | :------: | :---: | ------------ |
|  0   |  —  |  —  |   old    |   —   | —            |
|  1   |  —  |  —  |   old    |   —   | Database → A |
|  2   | old |  —  |   old    |   —   | Database → B |
|  3   | old | old |   old    |   —   | B → Redis    |
|  4   | old |  —  |   old    |  old  | A → Redis    |
|  5   |  —  |  —  |   old    |  old  | A → Redis    |
|  7   |  —  | old |   old    |  old  | Redis → A    |
|  8   | old |  —  |   old    |  old  | Redis → B    |

But than end of life for cache value happens and now both threads will need to update cache, but in this situation everything is gonna be alright. Both ask Redis, recieve nothing, run to DB and get 'old', both writes 'old' back to cache and respond - well it's OK for now, but there is an important thing. In this case there will be no degraded cache because database value hasn't changed, so there is no difference on value actuality even in case there'll be hundreds of threads writeing in any order - cache will be OK, because every value will be the same

| Time |  A  |  B  | Database |  Redis  | Data flow          |
| :--: | :-: | :-: | :------: | :-----: | ------------------ |
|  0   |  —  |  —  |   old    |    —    | —                  |
|  1   |  —  |  —  |   old    |    —    | Database → A       |
|  2   | old |  —  |   old    |    —    | **new → Database** |
|  3   | old |  —  | **new**  |    —    | Database → B       |
|  4   | old | new |   new    |    —    | B → Redis          |
|  5   | old |  —  |   new    | **new** | A → Redis          |
|  6   |  —  |  —  |   new    | **old** | —                  |
|  7   |  —  | old |   new    |   old   | Redis → A          |
|  8   | old |  —  |   new    |   old   | Redis → B          |

And finaly - "The Kill Sequence" or "How to mess up your cache in 6 easy steps". Everything looks that same as in previous sequence, but here we have step 2 with new value coming to the database between A & B requests, so we have a split situation when two clients has different values and gona try to commit their version of actual to the Redis, and this is where situation becomes hairy. And that's the test sequence we've been looking for!
