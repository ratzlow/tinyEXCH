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
- how much can the processing be decomposed?
    - steps of equal _size_?
    - _ordering_ important?
- back pressure
- are there natural partitions to seggragate work
    (see also natural concurrency of the domain: give an example:
    NewSingleOrder plain vanilla vs. allocating shares across orders)
- do you need to treat channels/clients with QoS
- become aware of the size of your unit of work (preferably: many small, equally sized units) - same cost?
- scale up vs. scale out (neg. example: clustered OH vs. single OH-Thread)

- measure (What would be your ideal case if none of those technical framework obstacles where in your way?)
	- TransactionSynchronization
	- waiting on I/O (file reading/DB)
	- Serialization
	- Logging
	- GC
	- the actual business logic

----

## Putting analysis into action
- steps can be reflected in different frameworks
    - by actors,
    - fork-join-frameworks (plain java)
    - future-chains (Rx)
    - e.g.handlers etc.)
- are your plain old unit test compatible with that new processing model?

----

## General achievements in the project
- people talk to each other (this agile team thingy)
- no big post go-live panic meetings because of regression errors
- dramatically improved time-to-reproduce

----

## Lessons learnt (so far)
- design to test
    - quality can put into action - if you have clear concept
    - don't forget the fourth dimension: time
- log everything - in a structured way
- _Nothing is final_ (see Pragmatic Programmer)

----




Why do you distribute (horizontal scaling - really?!)
	dependent on your natural concurrency model

Messaging
	is a wrapped up event
	brokerless messaging (e.g. Chronicle)
	Can you use the same technology for inner process and inter process communication?



the power of a single thread

Is your application running in interpreted mode?
	jitting starts after logic has been executed 10,000x

How much of this applies to none-message driven systems? EVA-Prinzip

We do
-----
Run our stateful business logic in one thread by serializing all requests through it


Lessons learnt
--------------
Legacy systems can be really cool - because you see how it is being used - you don't have to guess
Consistency outperforms beauty
"Constraints" ... is not only this database thingy
"Just let's make it a string to be flexible - is __not__ cool!"
Idioms are more important than technology
Anyone can write a calculation module - only few know how to write good tests
Refactor on the go
Not changing a systems does not contribute to more stability! Hallo@Mgrs
Changing a system without understanding it does not contribute to more stability! Hallo@Devs
How much flexibility do you really need to accomodate future requirements? Hallo@RE/Architects