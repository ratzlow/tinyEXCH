package net.tinyexch.ob.match

import net.tinyexch.ob.Orderbook
import net.tinyexch.ob.price.safeguard.VolatilityInterruptionGuard
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

    def referencePrice = 200.0
    def Orderbook ob

    def setup() {
        ob = new Orderbook( new ContinuousMatchEngine(referencePrice, VolatilityInterruptionGuard.NO_OP) )
        ob.open()
    }


    def "order with many partial executions"() {
        int shareNo = 1_000;
        given: "open orderbook with ref price of $referencePrice and $shareNo MKTs"
        shareNo.times { ob.submit(buyM(1), NEW) }
        def buyQty = ob.buySide.orders.sum { order -> order.orderQty }
        assert buyQty == shareNo

        when: "New big sell order meets standing buy orders"
        def bigSellOrder = sellM(shareNo)
        def trades = ob.submit(bigSellOrder, NEW).trades

        then: "All orders are executed with $shareNo shares"
        shareNo == trades.size()
        ob.buySide.orders.empty
        ob.sellSide.orders.empty
        trades.each { trade ->
            trade.executionQty == 1
            trade.price == referencePrice.doubleValue()
            bigSellOrder.clientOrderID == trade.sell.clientOrderID
        }
        def executedBuyOrderIDs = trades.collect { it.buy.clientOrderID } as Set
        executedBuyOrderIDs.size() == shareNo
    }


    def "partial match"() {
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
        trade.executionQty == sell_1.orderQty
        trade.price == referencePrice.doubleValue()
        trade.buy.clientOrderID == buy_1.clientOrderID
        trade.sell.clientOrderID == sell_1.clientOrderID
    }
}
