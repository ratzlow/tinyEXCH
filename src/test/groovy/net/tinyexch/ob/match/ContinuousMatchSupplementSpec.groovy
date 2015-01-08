package net.tinyexch.ob.match

import net.tinyexch.ob.Orderbook
import net.tinyexch.ob.SubmitType
import spock.lang.Specification

import static net.tinyexch.ob.match.OrderFactory.*

/**
 * Arbitrary scenarios related to continuous trading matching.
 *
 * @author ratzlow@gmail.com
 * @since 2014-12-27
 */
class ContinuousMatchSupplementSpec extends Specification {

    def referencePrice = 200.0;

    def "Partial match"() {
        given: "The orderbook has standing liquidity and a reference price of $referencePrice"
        def matchEngine = new ContinuousMatchEngine(referencePrice)
        def ob = new Orderbook(matchEngine)

        when: "BUY side has: MKT:3000; MKT:3000"
        def buy_1 = buyM(3000, time("09:01:00"))
        def buy_2 = buyM(3000, time("09:02:00"))

        def trades_1 = ob.submit( buy_1, SubmitType.NEW)
        def trades_2 = ob.submit( buy_2, SubmitType.NEW)

        then: "No order on other side, so orders will remain in the book"
        trades_1.empty
        trades_2.empty

        when: "SELL side: place MKT:1000"
        def sell_1 = sellM(1000, time("09:03:00"))
        def trades_3 = ob.submit(sell_1, SubmitType.NEW)

        then: "BUY: 2 orders left; SELL: empty with one partial execution"
        trades_3.size() == 1
        def trade = trades_3.first()
        trade.executionQty == sell_1.getOrderQty()
        trade.price == referencePrice
        trade.buy.clientOrderID == buy_1.clientOrderID
        trade.sell.clientOrderID == sell_1.clientOrderID
    }
}
