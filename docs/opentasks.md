Open Requirements
-----------------
- Discretionary Order ... what is it? Does it matter for this project?
- Search for spec for clearing & settlement
- Xetra liquidity index ... check how they measure liquidity index
- What is the difference between Quote and Order if both go into OB?
- Analyse which order types different exchanges accept (categorized by product type)

Business Rules
---------------


Epic
----
UC: Add listing of new instruments
- via GUI (min order size, min trading unit)
- import from file

UC: setup trading segment -> assign listings to it

Compose Index

Cool features
-------------
- accept market situation dependent order type algorithms (TWAP, Ladder, Sniper, Third, Iceberg)
- extendable model for orders, so only match relevant attriutes are mandatory but actual order might have much more (API-Design); what is the effect on persistence (maybe just view objects with reference to "full one" in GRID)
- Offer additional market models e.g. for betting, ebay like auctions, reverse auctions; this could be offered as commercial feature


Technical infrastructure
------------------------
- monitoring of business TX
- monitoring of resources (thread pool, etc)
- testing: spock
- implementation of validation rules via rule engine feasible?
- processing frameworks/methaphers: disruptor, chronicle, Rx, Storm, Akka
- Authentification: SSL client side


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
- consider external markets as reference price source
- P2P market network and routing for instruments not listed or not liquid in local market
- BEST exec: stay below ref market price & don' charge brokerage fee for interned orders; being self market maker and taking reduced spread
- create on demand OB where orders can be matched within limited period, if period is exceeded forward to prefered market
- setting up own exchange must provide benefit for BUY & SELL side; how can this be aligned with distributed EXCH idea (see BitCoin)
- what assets can be traded: energy, polution certificates, crypto currencies, commodities
- Stress the things you need to consider when building your own match engine/OB/Exchange


Docs
-----

- http://cdn.batstrading.com/resources/membership/BATS_Auction_Process.pdf
- http://batstrading.com/support/