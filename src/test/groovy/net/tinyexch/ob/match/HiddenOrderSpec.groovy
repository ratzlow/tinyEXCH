package net.tinyexch.ob.match

import net.tinyexch.ob.Orderbook
import net.tinyexch.ob.price.safeguard.VolatilityInterruptionGuard
import net.tinyexch.order.Order
import spock.lang.Specification

import static net.tinyexch.ob.SubmitType.NEW
import static net.tinyexch.ob.match.OrderFactory.*

/**
 * In the open order book, hidden orders are not disclosed to the market participants. None hidden orders are executed
 * with priority
 *
 * @author ratzlow@gmail.com
 * @since 2015-04-06
 */
class HiddenOrderSpec extends Specification {

    def buyH_Lim = buyH_Lim(200, 6000, time("09:01:00"))

    def "market order hits orderbook with limit and hidden order at same limit price"() {
        given: "An opened orderbook"
        def buyL =     buyL(200, 4000, time("09:02:00"))
        def sellM = sellM(4000)

        expect: "None-hidden order gets executed"
        execInternal( buyL, sellM, buyH_Lim.clientOrderID, 200)
    }

    def "market order hits orderbook with limit and hidden order at different limit price"() {
        given: "an open order book"
        def buyL  = buyL(199, 1000, time("09:02:00"))
        def sellM = sellM(6000)

        expect: "Hidden order gets executed"
        execInternal( buyL, sellM, buyL.clientOrderID, 200)
    }

    def execInternal( Order buyL, Order sellM, String expectedStandingBuyClientOrderID, double expectedExecutionPrice ) {
        def referencePrice = 200.0
        def ob = new Orderbook( new ContinuousMatchEngine(referencePrice, VolatilityInterruptionGuard.NO_OP) )
        ob.open()
        assert ob.submit(buyH_Lim, NEW).trades.empty
        assert ob.submit(buyL, NEW).trades.empty
        def trades = ob.submit(sellM, NEW).trades
        assert trades.size() == 1

        def trade = trades.first()
        assert trade.executionQty == sellM.orderQty
        assert trade.price == expectedExecutionPrice
        assert ob.buySide.orders.size() == 1
        assert ob.buySide.orders.findAll {it.clientOrderID == expectedStandingBuyClientOrderID}.size() == 1
        assert ob.sellSide.orders.empty

        true;
    }
}
