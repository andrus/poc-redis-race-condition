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

<!--
## How to use
## Next steps possible
## Tools used
-->
