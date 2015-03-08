package net.tinyexch.ob.match

import net.tinyexch.ob.Orderbook
import net.tinyexch.ob.OrderbookState
import net.tinyexch.ob.price.safeguard.VolatilityInterruptionGuard
import spock.lang.Specification

import static net.tinyexch.ob.SubmitType.NEW
import static net.tinyexch.ob.match.OrderFactory.*

/**
 * Submit an Iceberg order to an orderbook. We have an initial order book situation which changes stepwise in multiple
 * stages shows functionality of iceberg orders.
 *
 * @author ratzlow@gmail.com
 * @since 2015-01-26
 */
class IcebergOrderSpec extends Specification {

    Orderbook ob
    def iceberg = sellLimitIceberg( 201, 50_000, time("09:05:00"), 10_000)
    def standingSell_1 = sellL(203, 500, time("08:55:00"))
    def standingBuy_1 = buyL(202, 6000, time("09:01:00"))
    def standingBuy_2 = buyL(201, 2000, time("09:02:00"))


    def "1.) orderbook contains a set of standing orders and place iceberg"() {
        given: "An opened orderbook"
        def referencePrice = 200.0
        ob = new Orderbook( new ContinuousMatchEngine(referencePrice, VolatilityInterruptionGuard.NO_OP) )
        ob.open()
        ob.state == OrderbookState.OPEN

        expect: "Submitted limit orders are not crossed"
        ob.submit(standingSell_1, NEW).trades.empty
        ob.submit(standingBuy_1, NEW).trades.empty
        ob.submit(standingBuy_2, NEW).trades.empty
        ob.sellSide.orders.size() == 1
        ob.buySide.orders.size() == 2

        and: "Iceberg is submitted & remains in book"
        submitIceberg()

        and: "Place market order on empty buy side"
        placeMarketOrderAndMatchAgainstStandingIceberg()
    }


    def submitIceberg() {
        def trades = ob.submit(iceberg, NEW).trades

        assert trades.size() == 2 : "standing buy orders are executed"
        def fstTrade = trades[0]
        def secTrade = trades[1]
        assert fstTrade.executionQty == standingBuy_1.orderQty && fstTrade.price == standingBuy_1.price
        assert secTrade.executionQty == standingBuy_2.orderQty && secTrade.price == standingBuy_2.price

        def remainingSellOrders = ob.sellSide.orders
        assert remainingSellOrders.size() == 2

        def untouchedStandingSell = remainingSellOrders.find {it.clientOrderID == standingSell_1.clientOrderID}
        assert untouchedStandingSell.cumQty == 0

        def remainingPeak = remainingSellOrders.find {it.clientOrderID == iceberg.clientOrderID}
        assert remainingPeak.orderQty == iceberg.displayQty
        assert remainingPeak.cumQty == standingBuy_1.orderQty + standingBuy_2.orderQty
        assert remainingPeak.cumQty == 8000
        assert remainingPeak.leavesQty == 2000
        assert remainingPeak.icebergCumQty == remainingPeak.cumQty

        return true
    }


    def placeMarketOrderAndMatchAgainstStandingIceberg() {
        assert ob.sellSide.orders.size() == 2 : "Another buy MKT is placed"
        assert ob.buySide.orders.empty

        def incomingMarket = buyM(5000, time("09:07:00"))
        def trades = ob.submit(incomingMarket, NEW).trades
        assert trades.size() == 2 : "It is immediately fully executed"
        assert incomingMarket.orderQty == trades.sum {it.executionQty}

        def firstTrade = trades[0]
        assert firstTrade.executionQty == 2000 && firstTrade.price == 201
        assert firstTrade.buy.clientOrderID == incomingMarket.clientOrderID
        assert firstTrade.sell.clientOrderID == iceberg.clientOrderID
        assert firstTrade.sell.timestamp == iceberg.timestamp

        def secTrade = trades[1]
        assert secTrade.buy.clientOrderID == incomingMarket.clientOrderID
        assert secTrade.sell.clientOrderID == iceberg.clientOrderID
        assert secTrade.executionQty == 3000 && secTrade.price == 201 : "iceberg order qty is utilized"
        assert secTrade.sell.timestamp == incomingMarket.timestamp

        def remainingIceberg = ob.sellSide.orders.find {it.clientOrderID == iceberg.clientOrderID}
        assert remainingIceberg.timestamp == incomingMarket.timestamp : "timestamp is updated on iceberg"
        assert remainingIceberg.leavesQty == 7000
        assert remainingIceberg.hiddenQty == 30_000

        def remainingSellLimit = ob.sellSide.orders.find { it.clientOrderID == standingSell_1.clientOrderID}
        remainingSellLimit.leavesQty == standingSell_1.leavesQty && remainingSellLimit.price == standingSell_1.price
    }
}
