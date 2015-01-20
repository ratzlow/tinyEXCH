package net.tinyexch.ob.match

import net.tinyexch.exchange.event.DefaultNotificationListener
import net.tinyexch.exchange.event.produce.NewTradeEventHandler
import net.tinyexch.exchange.event.produce.VolatilityInterruptionEventHandler
import net.tinyexch.exchange.trading.form.continuous.ContinuousTrading
import net.tinyexch.ob.Orderbook
import net.tinyexch.ob.price.safeguard.VolatilityInterruptionGuard
import net.tinyexch.order.Order
import spock.lang.Specification

import static net.tinyexch.ob.SubmitType.NEW
import static net.tinyexch.ob.match.OrderFactory.*

/**
 * Arbitrary scenarios related to continuous trading matching.
 *
 * @author ratzlow@gmail.com
 * @since 2014-12-27
 */
// TODO (FRa) : (FRa) : test update of dyn ref price after successful matches
// TODO (FRa) : (FRa) : clear volatility interruption on trading form
class ContinuousMatchSupplementSpec extends Specification {

    def referencePrice = 200.0;

    def "Partial match"() {
        given: "The orderbook has standing liquidity and a reference price of $referencePrice"
        def matchEngine = new ContinuousMatchEngine(referencePrice, VolatilityInterruptionGuard.NO_OP)
        def ob = new Orderbook(matchEngine)
        ob.open()

        when: "BUY side has: MKT:3000; MKT:3000"
        def buy_1 = buyM(3000, time("09:01:00"))
        def buy_2 = buyM(3000, time("09:02:00"))

        def trades_1 = ob.submit( buy_1, NEW).trades
        def trades_2 = ob.submit( buy_2, NEW).trades

        then: "No order on other side, so orders will remain in the book"
        trades_1.empty
        trades_2.empty

        when: "SELL side: place MKT:1000"
        def sell_1 = sellM(1000, time("09:03:00"))
        def trades_3 = ob.submit(sell_1, NEW).trades

        then: "BUY: 2 orders left; SELL: empty with one partial execution"
        trades_3.size() == 1
        def trade = trades_3.first()
        trade.executionQty == sell_1.getOrderQty()
        trade.price == referencePrice
        trade.buy.clientOrderID == buy_1.clientOrderID
        trade.sell.clientOrderID == sell_1.clientOrderID
    }

    /**
     * Initiation of a volatility interruption. A limit order meets an order book in which there are market orders and
     * limit orders on the other side of the order book.
     * The reference price is € 200 and the price range is +/- 2% of the last determined price. The limit of the
     * incoming ask order lies outside the pre-defined price range and an execution is not carried out. The ask
     * order is entered in the order book and continuous trading is interrupted by an auction.
     */
    def "raise volatility interruption in continuous trading as incoming order exceeds static price range"() {

        given: "we are running in continuous trading"
        // TODO (FRa) : (FRa) : add check interruption was raised
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
