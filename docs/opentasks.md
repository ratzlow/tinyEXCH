Open Requirements
-----------------
- Discretionary Order ... what is it? Does it matter for this project?
- Search for spec for clearing & settlement
- Xetra liquidity index ... check how they measure liquidity index
- What is the difference between Quote and Order if both go into OB?


Business Rules
---------------


Epic
----
UC: Add listing of new instruments
- via GUI (min order size, min trading unit)
- import from file

UC: setup trading segment -> assign listings to it

Compose Index

Technical infrastructure
------------------------
- monitoring of business TX
- monitoring of resources (thread pool, etc)
- testing: spock
- implementation of validation rules via rule engine feasible?
- processing frameworks/methaphers: disruptor, chronicle, Rx, Storm, Akka


Setup backlog
-------------
- tE-3: later -> Currently, only subscriptions rights will have a minimum order size greater than the minimum tradable unit.
- tE-14: expiration/cancel of orders needs to be implemented EOD; Check if to include field GTT (GoodTillTime) as well. How to avoid batching? Use a Timer?! Accuracy?
- each equity requires a designated sponsor ... configure sponsors (1-n) when setting up a listing -> refer to "Designated Sponsor Guide", chap 5
- provide filtered visibility access to OB content, e.g. to what depth someone (role dependent?) might look in the OB
- what msgs are available to request OB status?
- impl trading models: IPO, OTC

Marketing
---------

Why and where should I use it?

- Internal markets like dark pools
- grey markets
- internal crossing networks
- Create backends for sell side market simulators

Docs
-----

- http://cdn.batstrading.com/resources/membership/BATS_Auction_Process.pdf
- http://batstrading.com/support/