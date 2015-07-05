package net.tinyexch.ob.match

import net.tinyexch.ob.Orderbook
import net.tinyexch.ob.OrderbookState
import net.tinyexch.ob.price.safeguard.VolatilityInterruptionGuard
import spock.lang.Specification

import static net.tinyexch.ob.SubmitType.NEW
import static net.tinyexch.ob.match.OrderFactory.*

/**
 * Number of scenarios reflecting OB situations with Midpoint orders. Midpoint orders execute at middle of bid/ask
 * spread and can have a MAQ ... Minimum Acceptable Quantity. Matching precedence is by volume/time priority
 *
 * @author ratzlow@gmail.com
 * @since 2015-04-07
 */
class MidpointOrderSpec extends Specification {

    def startingMidpointPrice = 199.5D
    Orderbook ob
    ContinuousMatchEngine continuousMatchEngine


    def setup() {
        def referencePrice = 200.0
        continuousMatchEngine = new ContinuousMatchEngine(referencePrice, VolatilityInterruptionGuard.NO_OP, startingMidpointPrice)
        ob = new Orderbook(continuousMatchEngine)
        ob.open()
        assert ob.state == OrderbookState.OPEN
        assert continuousMatchEngine.midpointPrice == startingMidpointPrice
    }

    /**
     * Although the highest bid limit exceeds the midpoint, it is lower than the lowest ask limit. Therefore,
     * the incoming ask midpoint order is entered in the order book. No orders are executed.
     *
     * Current bid/ask spread is defined as := 197 � 202 (midpoint = 199.50) --> 199.50 < 203 on ask side --> no execution
     */
    def "midpoint ask meets midpoint best - because of limit it is not executable_Ex1"() {
        when: "one standing midpoint buy limit order is in the book"
        def standingBuyMid_Lim = buyMid_Lim(200, 6000, time("09:01:00"))
        assert ob.submit( standingBuyMid_Lim, NEW).trades.empty

        then: "adding midpoint sell limit will not execute since ask price is too high"
        def sellMid_Lim = sellMid_Lim( 203, 6000, time("09:05:00") )
        ob.submit( sellMid_Lim, NEW ).trades.empty
        ob.sellSideMidpoint.orders.collect { it.clientOrderID == sellMid_Lim.clientOrderID}.size() == 1
        ob.buySideMidpoint.orders.collect { it.clientOrderID == standingBuyMid_Lim.clientOrderID}.size() == 1
        ob.buySideMidpoint.orders.collect { it.clientOrderID == standingBuyMid_Lim.clientOrderID}.size() == 1
        continuousMatchEngine.midpointPrice == 201.5D
    }


    /**
     * A limited midpoint order meets an order book with limited midpoint orders only on the other side.
     * The midpoint orders cross, but not at the midpoint. Therefore, they are not executable.
     * The currently available Xetra bid/ask spread is 197 � 202 (midpoint = 199.50)
     *
     * Although the highest bid limit is higher than the lowest ask limit, it does not exceed the midpoint.
     * The incoming ask midpoint order is entered in the order book. No orders are executed. There are crossed midpoint
     * orders which, however, are not executable at the midpoint of the currently available bid/ask spread
     */
    def "limited midpoint orders cross but not at midpoint - no execution_Ex2"() {
        when: "one standing midpoint buy limit order is in the book"
        def standingBuyMid_Lim = buyMid_Lim(198, 6000, time("09:01:00"))
        assert ob.submit( standingBuyMid_Lim, NEW).trades.empty

        then: "Although the highest bid limit is higher than the lowest ask limit, it does not exceed the midpoint"
        def sellMid_Lim = sellMid_Lim( 197, 6000, time("09:05:00") )
        ob.submit( sellMid_Lim, NEW ).trades.empty
        ob.sellSideMidpoint.orders.collect { it.clientOrderID == sellMid_Lim.clientOrderID}.size() == 1
        ob.buySideMidpoint.orders.collect { it.clientOrderID == standingBuyMid_Lim.clientOrderID}.size() == 1
        continuousMatchEngine.midpointPrice == 197.5D
    }


    /**
     * A limited midpoint order meets an order book with limited midpoint orders only on the other side.
     * The midpoint orders cross at the midpoint and are executed.
     * The currently available bid/ask spread is 197 � 202 (midpoint = 199.50)
     *
     * The order book is crossed at the midpoint of the currently available bid/ask spread
     */
    def "limited midpoint order meets limited midpoint on other side and is executable_Ex3"() {
        when: "one standing midpoint buy limit order is in the book"
        def standingBuyMid_Lim = buyMid_Lim(200, 6000, time("09:01:00"))
        assert ob.submit( standingBuyMid_Lim, NEW).trades.empty

        then: "6000 executed @ 199.50. 2000 remaining incoming ask midpoint order entered in book"
        def sellMid_Lim = sellMid_Lim( 197, 8000, time("09:05:00") )
        def trades = ob.submit( sellMid_Lim, NEW ).trades

        trades.size() == 1
        def trade = trades.first()
        trade.price == startingMidpointPrice
        trade.executionQty == 6000

        and: "orderbook empty on buy side"
        ob.buySideMidpoint.orders.empty

        and: "orderbook has remaining on sell side"
        def remainingSell = ob.sellSideMidpoint.orders.first()
        remainingSell.leavesQty == 2000
    }
}
