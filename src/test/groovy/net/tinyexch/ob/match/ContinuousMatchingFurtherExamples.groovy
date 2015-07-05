package net.tinyexch.ob.match

import net.tinyexch.exchange.event.DefaultNotificationListener
import net.tinyexch.exchange.event.produce.NewTradeEventHandler
import net.tinyexch.exchange.event.produce.VolatilityInterruptionEventHandler
import net.tinyexch.exchange.trading.form.continuous.ContinuousTrading
import net.tinyexch.ob.Orderbook
import net.tinyexch.ob.price.safeguard.VolatilityInterruptionGuard
import net.tinyexch.order.Order
import net.tinyexch.order.OrderType
import spock.lang.Specification

import static net.tinyexch.ob.SubmitType.NEW
import static net.tinyexch.ob.match.OrderFactory.*

/**
 * Extended examples for continuous matching.
 *
 * @author ratzlow@gmail.com
 * @since 2015-01-24
 */
class ContinuousMatchingFurtherExamples extends Specification {

    def referencePrice = 200.0

    /**
     *
     * The incoming market-to-limit order can only be partially executed against the best bid limit order in the
     * order book at € 203. The remaining part of the market-to-limit order (2000) is entered into the order book with
     * a limit equal to the price of the executed part at € 203.
     */
    def "Partial execution of a market-to-limit order"() {
        given: "An open order book"
        def standingBuy_1 = buyL(203, 1000, time("09:01:00"))
        def standingBuy_2 = buyL(202, 1000, time("09:02:00"))
        def ob = new Orderbook( new ContinuousMatchEngine(referencePrice, VolatilityInterruptionGuard.NO_OP) )
        ob.open()

        expect: "Limit orders only on the other side of the order book. No executions."
        ob.submit(standingBuy_1, NEW).trades.empty
        ob.submit(standingBuy_2, NEW).trades.empty

        when: "A market-to-limit order meets an"
        def incomingSell = sellMtoL(3000, time("09:05:00"))
        Match match = ob.submit(incomingSell, NEW)

        then: "The incoming MktToLim order can only be partially executed against best bid limit order in order book at € 203"
        match.trades.size() == 1
        def trade = match.trades.first()
        trade.executionQty == 1000
        trade.price == standingBuy_1.price
        trade.price == 203
        trade.buy.clientOrderID == standingBuy_1.clientOrderID
        trade.sell.clientOrderID == incomingSell.clientOrderID

        and: "Removed best buy and added remaining sell qty as limit"
        ob.buySide.orders.size() == 1
        def remainingBuy = ob.buySide.orders.first()
        remainingBuy.clientOrderID == standingBuy_2.clientOrderID
        remainingBuy.leavesQty == standingBuy_2.orderQty

        and: "The remaining part of the MktToLim (2000) entered into order book with limit == price of executed part at € 203"
        ob.sellSide.orders.size() == 1
        def remainingSell = ob.sellSide.orders.first()
        remainingSell.price == 203
        remainingSell.price == standingBuy_1.price
        remainingSell.leavesQty == incomingSell.orderQty - trade.executionQty
        remainingSell.orderType == OrderType.LIMIT
    }


    /**
     * Initiation of a volatility interruption. A limit order meets an order book in which there are market orders and
     * limit orders on the other side of the order book.
     * The reference price is € 200 and the price range is +/- 2% of the last determined price. The limit of the
     * incoming ask order lies outside the pre-defined price range and an execution is not carried out. The ask
     * order is entered in the order book and continuous trading is interrupted by an auction.
     *
     * @link 13.2.2.2 Further Examples
     */
    def "raise volatility interruption in continuous trading as incoming order exceeds static price range"() {

        given: "we are running in continuous trading"
        def notificationListener = new DefaultNotificationListener()
        boolean volatilityInterruptionFired = false
        def trades = []
        notificationListener.volatilityInterruptionEventHandler =
                { volatilityInterruptionFired = true } as VolatilityInterruptionEventHandler
        notificationListener.newTradeEventHandler =
                { trades << it } as NewTradeEventHandler

        def volatilityInterruptionGuard = new VolatilityInterruptionGuard(referencePrice, 2, referencePrice + 1, 1)
        def matchEngine = new ContinuousMatchEngine(referencePrice, volatilityInterruptionGuard)
        def continuousTrading = new ContinuousTrading( notificationListener, matchEngine )
        continuousTrading.orderbook.open()

        when: "submit BUY orders to BUY side"
        def standingMarket = buyM(6000, time("09:01:00"))
        def standingLimit = buyL(202D, 1000, time("09:02:00"))
        continuousTrading.submit(standingMarket, NEW)
        continuousTrading.submit(standingLimit, NEW)

        then: "they are unmatched and remain in the book. Reference price has not changed"
        def orderbookBuySide = continuousTrading.orderbook.buySide
        def buySideChecks = {
            orderbookBuySide.limitOrders.size() == 1
            orderbookBuySide.marketOrders.size() == 1
        }
        buySideChecks

        orderbookBuySide.limitOrders.size() == 1
        orderbookBuySide.marketOrders.size() == 1

        when: "SELL order is submitted with limit price exceeding price range"
        Order incoming = sellL(220D, 1000, time("10:01:00"))

        then: "No trade is emitted, sell will be added to OB, and reference price remained untouched"
        continuousTrading.submit(incoming, NEW)

        and: "Volatility interruption event fired"
        volatilityInterruptionFired

        and: "No trades emitted"
        trades.size() == 0

        and: "check trading form"
        continuousTrading.volatilityInterrupted

        and: "orderbook BUY side remained untouched"
        buySideChecks

        and: "incoming SELL order was stored in the book"
        def sellOrders = continuousTrading.orderbook.sellSide.orders
        sellOrders.size() == 1
        sellOrders[0].clientOrderID == incoming.clientOrderID
        sellOrders[0].leavesQty == incoming.orderQty
    }

}
