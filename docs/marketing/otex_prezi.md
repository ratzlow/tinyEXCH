# Otex: modernizing legacy application

## What is it?
- order routing system in CS private banking
- broker for various external clients (3rd party banks, ...) --> pic: CS external landscape (generic trading network)
- part in the landscape; trading life cycle pretrade, execution/fullfillment, posttrade --> pic: CS internal landscape
- user groups: AOF, (MidOffice), RM
- --> what is the business case?
- top n most important app in PB --> current ranking?

-----

## History
- started 10y ago
- classic go-live prototype
- comprises typical legacy software properties:
    - testabillity
    - _special engineering_ in contrast to _JAP_
    - built upon the idioms/technology available at this time (incl. C++, Python, XSLT)
    - high fluctuation in staff --> numbers?

-----

## Evolution of idioms
- CQRS
    - event sourcing
    - eventual consistency
- logical vs physical components
- logical channels vs. physical queues
- scaling
    - traditional: scale out ... make use of boxes
    - multi-core age: scale up: make use of cores
    - cloud: N/A
- processing model
    - traditional: sync APIs
    - back in town: async msg passing // actually: very well established by somehow forgotten

-----

## Evolution of technology
- persistence
    - traditional: RDBMS, ORM, DAO
    - alternative: journal, serialization
- transaction definition
    - traditional: TX management API around TX resources
    - alternative: driven by unit of work, isolation=Serialized if single threaded
- messaging
    - traditional: MOM with central broker
    - alternative: brokerless
    - // TCP guarantees delivery: utilize it!
- processing model
    - traditional: EJB, MDB (usually only async at the interface, not to process unit of work)
    - alternative: reactive - cmd pipelines, actor model,
    - Disruptor, Rx, Akka, Storm

----

## Benefit
- You do not refactor without purpose?!
    - performance
    - testability
    - productivity (turn-around-times)
    - stability
    - understandability

----

## Summary
- Legacy can be transformed!
- Not changing a systems does not contribute to more stability! Hallo@Mgrs
- Changing a system without understanding it does not contribute to more stability! Hallo@Devs
- How much flexibility do you really need to meet future requirements? Hallo@RE/Architects