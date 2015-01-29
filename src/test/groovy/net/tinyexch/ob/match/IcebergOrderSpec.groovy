package net.tinyexch.ob.match

import net.tinyexch.ob.Orderbook
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

    def "1.) orderbook contains a set of standing orders"() {
        given: "An opened orderbook"
        def referencePrice = 200.0
        def ob = new Orderbook( new ContinuousMatchEngine(referencePrice, VolatilityInterruptionGuard.NO_OP) )
        ob.open()

        expect: "Submitted limit orders are not crossed"
        def standingSell_1 = sellL(203, 500, time("08:55:00"))
        def standingBuy_1 = buyL(202, 6000, time("09:01:00"))
        def standingBuy_2 = buyL(201, 2000, time("09:02:00"))

        ob.submit(standingSell_1, NEW).trades.empty
        ob.submit(standingBuy_1, NEW).trades.empty
        ob.submit(standingBuy_2, NEW).trades.empty

        ob.sellSide.orders.size() == 1
        ob.buySide.orders.size() == 2

        when: "Iceberg is submitted"
        def iceberg = sellLimitIceberg( 201, 50_000, time("09:05:00"), 10_000)
        def trades = ob.submit( iceberg, NEW ).trades

        then: "standing buy orders are executed"
        trades.size() == 2
        def fstTrade = trades[0]
        def secTrade = trades[1]
        fstTrade.executionQty == standingBuy_1.orderQty
        fstTrade.price == standingBuy_1.price
        secTrade.executionQty == standingBuy_2.orderQty
        secTrade.price == standingBuy_2.price

        def remainingSellOrders = ob.sellSide.orders
        remainingSellOrders.size() == 2

        def untouchedStandingSell = remainingSellOrders.find { it.clientOrderID == standingSell_1.clientOrderID}
        untouchedStandingSell.cumQty == 0

        def remainingPeak = remainingSellOrders.find {it.clientOrderID == iceberg.clientOrderID }
        remainingPeak.orderQty == iceberg.displayQty
        remainingPeak.cumQty == standingBuy_1.orderQty + standingBuy_2.orderQty
        remainingPeak.icebergCumQty == remainingPeak.cumQty
    }
}
