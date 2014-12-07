Otex
====

----

## What is it?
- part in the landscape; trading life cycle pretrade, execution/fullfillment, posttrade
- user groups
- does it make or cost money

----

## History
- legacy at it's best - especially testabillity and coverage
- idioms (distribution, failure tolerance, thread (usage), ...)
- technology _special engineering_ in contrast to _JAP_
- team size: (how many devs over it's lifetime)
- technology evolution: C++, Python, XSLT --> Java
- 3 main components (RuleEngine, StateEngine, GUI)
    - all build as separate applications (e.g. no shared domain model, different technology stacks)
- testing
    - coverage
    - number of test cases
    - manual vs. automated testing
    - performance tests
    - ultimate goal: automating the automation
    - conclusion: the more distributed the harder to test

----

## The new idioms
- CQRS
- SEDA
- event sourcing
- eventual consistency
- logical vs physical components
- logical channels vs. physical queues

----

## You should think about
- persistence vs. database
	- What is state/persistence?
	- What do you do with it?
- how do you define your TX? Is it really this DB thingy or is a business _Unit of Work_
- concurrency build in or plugged on?
    - Can you use an existing concurrency model?
    - Do you really understand your adopted model (life cycle, thread context resurrection if thread dies, ...)?
- how much can processing be decomposed?
    - steps of equal _size_?
    - _ordering_ important?
    - are there natural _partitions_ to segregate work ("natural concurrency of the domain", e.g. NewSingleOrder plain vanilla vs. allocating shares across orders)
- back pressure
- QoS: relevant for channels/clients? Do you need a _fast lane_?
- become aware of the size of your unit of work (preferably: many small, equally sized units) - same cost?
- scale up vs. scale out (neg. example: clustered OH vs. single OH-Thread)

- measure (What would be your ideal case if none of those technical framework obstacles where in your way?)
	- TransactionSynchronization
	- waiting on I/O (file reading/DB)
	- Serialization
	- Logging
	- GC
	- the actual business logic
	- JIT compiled (infamous 10k execs)

----

## Messaging
- the preferred communication model in a share nothing model (okay, share little ;)
- "Communicate to share state and do _not_ share state to communicate"
- brokerless messaging (e.g. Chronicle, Informatica Ultra Messaging)
- Can you use the same technology for inner process and inter process communication?

----

## Putting analysis into action
- steps can be reflected in different frameworks
    - by actors,
    - fork-join-frameworks (plain java)
    - future-chains (Rx)
    - e.g.handlers etc.)
- are your plain old unit test compatible with that new processing model?

----

## not so technical improvements
- people talk to each other (this agile team thingy)
- no big post go-live panic meetings because of regression errors
- dramatically improved time-to-reproduce

----

## Lessons learnt (so far)
- design to test
    - quality can put into action - if you have clear concept
    - don't forget the fourth dimension: time
- log everything - in a structured way
- _Nothing is final_ Pragmatic Programmer
- the power of a single thread

----

## None event based systems
- speak: SOA
- "share nothing" still applies
- natural concurrency still applies

----

## Crazy changes
- Run our stateful business logic through one thread
    - no TX synchronization anymore needed
    - no TX isolation anymore needed
    - no resource synchronization needed, just plain none concurrent Java

----

## Gimme the buzz
- Legacy systems can be really cool - because you see how it is being used - you don't have to guess
- Consistency kills beauty
- "Constraints" ... is not only this database thingy
- "Just let's make it a string to be flexible - is __not__ cool!"
- Idioms are more important than technology
- Anyone can code some business rules - only few know how to write good tests
- Refactor on the go

----

## Role based blaming
- Not changing a systems does not contribute to more stability! Hallo@Mgrs
- Changing a system without understanding it does not contribute to more stability! Hallo@Devs
- How much flexibility do you really need to meet future requirements? Hallo@RE/Architects

----
## Alternative technology stacks
- Storm https://storm.apache.org/
- Akka
- Rx (RxJava)
- VERT.X http://vertx.io/
- 

----

## Resources
- Martin Thompson http://www.infoq.com/news/2014/10/thompson-reactive-manifesto-2
- Peter Lawrey http://vanillajava.blogspot.ch/2014/09/an-inconvenient-latency.html
- Martin Fowler http://martinfowler.com/bliki/TechnicalDebt.html
- http://www.reactivemanifesto.org/