package net.tinyexch.ob.match

import net.tinyexch.exchange.event.DefaultNotificationListener
import net.tinyexch.exchange.event.produce.NewTradeEventHandler
import net.tinyexch.exchange.event.produce.VolatilityInterruptionEventHandler
import net.tinyexch.exchange.trading.form.continuous.ContinuousTrading
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
 * @link 13.2.4
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
        ob.buySideMidpoint.orders.collect { it.clientOrderID == standingBuyMid_Lim.clientOrderID}.size() == 1
        ob.sellSideMidpoint.orders.collect { it.clientOrderID == sellMid_Lim.clientOrderID}.size() == 1
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

    /**
     * The highest bid limit exceeds the Xetra midpoint (199.50) and is higher than the lowest ask limit, with the
     * latter being below the Xetra midpoint. The order book is crossed at the midpoint of the currently available
     * bid/ask spread. However, the potential execution price of € 199.50 would trigger a volatility interruption.
     * Therefore, the incoming midpoint order is entered in the order book and no execution takes place.
     * No volatility interruption is triggered.
     */
    def "price range exceeded but no volatility interruption triggered and nothing executed_Ex4"() {
        def notificationListener = new DefaultNotificationListener()
        boolean volatilityInterruptionFired = false
        def trades = []
        notificationListener.volatilityInterruptionEventHandler =
                { volatilityInterruptionFired = true } as VolatilityInterruptionEventHandler
        notificationListener.newTradeEventHandler =
                { trades << it } as NewTradeEventHandler

        def referencePrice = 210.0
        def guard = new VolatilityInterruptionGuard(180D, 5F, 185D, 5F)
        def matchEngine = new ContinuousMatchEngine(referencePrice, guard, startingMidpointPrice)
        def continuousTrading = new ContinuousTrading( notificationListener, matchEngine )
        def ob = continuousTrading.orderbook
        ob.open()

        when: "one standing midpoint buy limit order is in the book"
        def standingBuyMid_Lim = buyMid_Lim(200, 6000, time("09:01:00"))
        continuousTrading.submit( standingBuyMid_Lim, NEW)

        then: "6000 executed @ 199.50. 2000 remaining incoming ask midpoint order entered in book"
        def sellMid_Lim = sellMid_Lim( 199, 6000, time("09:05:00") )
        continuousTrading.submit( sellMid_Lim, NEW )
        trades.empty
        !volatilityInterruptionFired
        ob.sellSideMidpoint.orders.collect { it.clientOrderID == sellMid_Lim.clientOrderID}.size() == 1
        ob.buySideMidpoint.orders.collect { it.clientOrderID == standingBuyMid_Lim.clientOrderID}.size() == 1
        matchEngine.midpointPrice == startingMidpointPrice
    }


    /**
     * In order to release the order book crossed at the midpoint, strict volume/time priority is disregarded.
     * Execution takes place at the midpoint.
     * To optimize the executable volume, the incoming ask midpoint order is executed against the MAQ (3000 shares) of
     * the bid midpoint order with lower priority and against 3000 shares of the bid midpoint order with higher priority
     */
    def "Crossing with MAQ involved"() {
        when: "Limited midpoints standing with MAQ"
        def standingBuyMid_Lim = buyMid_Lim(201, 5000, time("09:01:00"))
        def standingBuyMid_LimMAQ = buyMid_LimMAQ(200, 4000, time("09:02:00"), 3000)
        ob.submit( standingBuyMid_Lim, NEW ).trades.empty
        ob.submit( standingBuyMid_LimMAQ, NEW ).trades.empty

        then: "both buys are standing liquidity"
        ob.buySideMidpoint.orders.size() == 2

        when: "ask order is submitted with MAQ against standing liquidity"
        def sellMid_LimMAQ = sellMid_LimMAQ(199, 6000, time("09:02:00"), 6000)
        def trades = ob.submit(sellMid_LimMAQ, NEW).trades

        then: "2 trades are generated to maximize executable volume"
        trades.size() == 2
        ob.buySideMidpoint.orders.size() == 2
        ob.sellSideMidpoint.orders.size() == 0

        and: "the trades are created by volume-time priority"
        def firstTrade = trades[0]
        firstTrade.buy.clientOrderID == standingBuyMid_Lim.clientOrderID
        firstTrade.executionQty == 3000

        def secondTrade = trades[1]
        secondTrade.buy.clientOrderID == standingBuyMid_LimMAQ.clientOrderID
        secondTrade.executionQty == 3000
    }
}