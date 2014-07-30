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


Setup backlog
-------------
- tE-3: later -> Currently, only subscriptions rights will have a minimum order size greater than the minimum tradable unit.
- tE-14: expiration/cancel of orders needs to be implemented EOD; Check if to include field GTT (GoodTillTime) as well. How to avoid batching? Use a Timer?! Accuracy?
- each equity requires a designated sponsor ... configure sponsors (1-n) when setting up a listing -> refer to "Designated Sponsor Guide", chap 5