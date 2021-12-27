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

## Finding

First of all current implementation of PoC does not help with checking right way to cook a Redis. It helped us to validate that race condition occures, also we've realised that it's not 'that' common and mostly can happen when load is high.

Strangely we've seen that Redis can prioritise requests which ask value of key added first.Maybe that was a coicidence, but couple times we've seen such strange behaviour.

## Next steps

As it've been denoted above current implementation doesn't help us to learn to cook, so we need:

- recreate test stand which will steadily reproduce race condition
- implement fix for race condition on a test stand
- check obtained solutions on current implementation (production-like test)
- write down recipe for well cooked Redis
