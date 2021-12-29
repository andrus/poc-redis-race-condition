_This repository is devoted to one particular case study needed to inspect conditions and reasonos for some non-predicted behaviour on one of the systems. If you are not knowing what's it all feel free to dismiss this repo - nothing interesting here._

# [PoC] Redis Race Condition

## Plan

> **WARNING!** _This part of document was written before first line of code hit the hard drive - changes in final product may occure._

- write initial plan for PoC
- define changed preassumtions and write them down
- init git repo
- prepare chassis
  - repository layout
  - docker compose script
  - java project for service
  - java project for test runner
  - db startup script
- prepare test environment
  - implement REST service logic
  - implement jUnit testing
  - reproduce un-intended behaviour
- fix bug(-s)
- _(optional)_ think of optimisation
- _(optional)_ add some fancy whistles in documentation
- clean-up solution and documentation
- publish and report

## Preassumptions

As stated in PoC description we'll have these containers:

- 1 X Redis (cache facility)
- 1 X MySQL (DB representation)
- 2 X REST services (app logic emulation)

And before start coding there are two additions to base problem formulation:

- there will be no extra updater service, because it's not really important to update database all the time. It's needed only in-time with test running so, I thought it will be a good idea to merge updater service from base description and test runner in single java-based jUnit-encapsulating entity.
- nginx is a good option for well-defined "static"-like configuration, but here we have a PoC, so I think it will be good to make it agile in a way. So I thought of traefik, it's also lightweight, reliable and blah-blah-blah, but mostly it is important that it can config itself from labels in docker compose, so we can easily scale application while working with PoC.

## Implementation details

Due to NetBeans 12 problematic compatibility with jUnit 5 we are stick with version 4 for now, to make more compatible solution.

MySQL, Redis and Traefik have single official image available on DockerHub, so there are no questions to be asked. But there is one in case of java base image - I choose Amazon Coretto image, because it outperformed all competitors in our unternal study couple weeks ago.

## Leftover

Spent about an hour on Bootique module mechanics - maybe we just don't click with it. Will look at it again soon.

Tried as hard as I could, but failed to fully reproduce degraded cache state under load - situation where just two callers ask Redis for write in specific order and delays is hard to achieve, possible though. Best achieved result is test-stand proof of failure and fix. And sparadical degradet results for one (couple) of clients, fixed with following overwrites (in case of blind write strategies) or with re-read (in case of check-and-set-based stratefies).

## Finding

First of all current implementation of PoC does not help with checking right way to cook a Redis. It helped us to validate that race condition occures, also we've realised that it's not 'that' common and mostly can happen when load is high.

Strangely we've seen that Redis can prioritise requests which ask value of key added first.Maybe that was a coicidence, but couple times we've seen such strange behaviour.

There may be interesting behaviour if one service WATCH some key and others write equal value for that key. Not sure, but it's interesting to check if it will fail transaction.

## Performance

After testing blind write, check and set and pessimistic lock performance (see METRICS.md) it's can be said that adding optimistic locking into the mix adds about third of time (on average in this primitive case). Also it's can be seen that pessimistic lock behaves worse than check and set, due to hotspot on locking key - hoped that Redis will optimaize this key, but actually it is not acting in that way.
