package net.tinyexch.ob.match

import net.tinyexch.ob.Orderbook
import net.tinyexch.ob.OrderbookState
import net.tinyexch.ob.price.safeguard.VolatilityInterruptionGuard
import net.tinyexch.order.Order
import net.tinyexch.order.Trade
import spock.lang.Specification

import java.time.Instant

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
    def iceberg_1 = sellLimitIceberg( 201, 50_000, time("09:05:00"), 10_000)
    def iceberg_2 = sellLimitIceberg( 201, 30_000, time("09:08:01"), 5_000)
    def standingSell_1 = sellL(203, 500, time("08:55:00"))
    def standingSell_2 = sellL(201, 2000, time("09:13:13") )
    def standingBuy_1 = buyL(202, 6000, time("09:01:00"))
    def standingBuy_2 = buyL(201, 2000, time("09:02:00"))

    def "Submit multiple iceberg orders and check the execution of visible and hidden quantities"() {
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
        placeIceberg()

        and: "Place market order on empty buy side"
        placeMarketOrderAndMatchAgainstStandingIceberg()

        and: "Another iceberg order is entered in the book"
        placeAnotherSellIcebergOrder()

        and: "Bid market order is placed and fully matched"
        placeBidMarketOrder()

        and: "Ask sell limit order is placed but cannot be executed because of empty bid side"
        placeSellLimitOrder()

        and: "Submit a Bid market order which will be fully matched"
        placeAnotherBidMarketOrder()
    }

    private def placeAnotherBidMarketOrder() {
        def submitTime = time("09:15:00")
        def buyMarket = buyM(23_000, submitTime)
        def trades = ob.submit(buyMarket, NEW).trades
        assert trades.sum {it.executionQty} == buyMarket.orderQty : "order is fully executed with $buyMarket.orderQty"

        assertTrade(trades[0], 8000, 201, iceberg_1.clientOrderID, buyMarket.clientOrderID, time("09:10:40"))
        assertTrade(trades[1], 5000, 201, iceberg_2.clientOrderID, buyMarket.clientOrderID, time("09:10:40"))
        assertTrade(trades[2], 2000, 201, standingSell_2.clientOrderID, buyMarket.clientOrderID, time("09:13:13"))
        assertTrade(trades[3], 8000, 201, iceberg_1.clientOrderID, buyMarket.clientOrderID, time("09:15:00"))

        def remainingFirstIceberg = ob.sellSide.orders.find {it.clientOrderID == iceberg_1.clientOrderID}
        def remainingSecondIceberg = ob.sellSide.orders.find {it.clientOrderID == iceberg_2.clientOrderID}

        assert ob.sellSide.orders.count {it.clientOrderID == standingSell_2.clientOrderID} == 0
        assert ob.buySide.orders.empty : "buy side must be fully executed"
        assertRemainingIceberg(remainingFirstIceberg, 2000, 10_000, submitTime)
        assertRemainingIceberg(remainingSecondIceberg, 5000, 15_000, submitTime)

        return true;
    }

    private def placeSellLimitOrder() {
        def trades = ob.submit(standingSell_2, NEW).trades
        List<Order> limitOrders = new ArrayList<>(ob.sellSide.limitOrders)
        Collections.sort(limitOrders, MatchEngine.SELL_PRICE_TIME_ORDERING );
        def idxInBook = limitOrders.findIndexOf {it.clientOrderID == standingSell_2.clientOrderID}

        assert idxInBook == 2 : "according to price/time prio it is on 3rd idx"
        assert trades.empty

        return true;
    }

    def placeBidMarketOrder() {
        def buyOrderSubmitTime = time("09:10:40")
        def buyMarket = buyM(14_000, buyOrderSubmitTime)
        def trades = ob.submit(buyMarket, NEW).trades

        def sellOrders = ob.sellSide.orders

        def remainingFirstIcebergInBook = sellOrders.find { it.clientOrderID == iceberg_1.clientOrderID }
        def remainingSecIcebergInBook = sellOrders.find { it.clientOrderID == iceberg_2.clientOrderID }

        assert remainingFirstIcebergInBook.timestamp == buyOrderSubmitTime
        assert remainingFirstIcebergInBook.cumQty == 2000
        assert remainingFirstIcebergInBook.leavesQty == 8000
        assert remainingFirstIcebergInBook.hiddenQty == 20_000

        assert remainingSecIcebergInBook.timestamp == buyOrderSubmitTime
        assert remainingSecIcebergInBook.hiddenQty == 20_000
        assert sellOrders.find {it.clientOrderID == standingSell_1.clientOrderID}.timestamp == time("08:55:00")

        assert trades.sum {it.executionQty} == buyMarket.orderQty : "order is fully executed with 14000"
        assert trades.size() == 3

        assertTrade( trades[0], 7000, 201, iceberg_1.clientOrderID, buyMarket.clientOrderID, time("09:07:00") )
        assertTrade( trades[1], 5000, 201, iceberg_2.clientOrderID, buyMarket.clientOrderID, time("09:08:01") )
        assertTrade( trades[2], 2000, 201, iceberg_1.clientOrderID, buyMarket.clientOrderID, time("09:10:40") )

        return true
    }

    def placeAnotherSellIcebergOrder() {
        assert ob.submit(iceberg_2, NEW).trades.empty

        Order[] orders = ob.sellSide.limitOrders.toArray(new Order[0])
        int idx_iceberg_1 = orders.findIndexOf {it.clientOrderID == iceberg_1.clientOrderID}
        int idx_iceberg_2 = orders.findIndexOf {it.clientOrderID == iceberg_2.clientOrderID}
        assert idx_iceberg_1 < idx_iceberg_2 : "first iceberg has older timestamp so should have precedence"

        assert orders.find {it.clientOrderID == iceberg_1.clientOrderID}.timestamp == time("09:07:00")
        assert orders.find {it.clientOrderID == iceberg_2.clientOrderID}.timestamp == time("09:08:01")
        assert orders.find {it.clientOrderID == standingSell_1.clientOrderID}.timestamp == time("08:55:00")

        def standingIceberg_2 = ob.sellSide.orders.find {it.clientOrderID == iceberg_2.clientOrderID}
        assert standingIceberg_2.orderQty == 5_000
        assert standingIceberg_2.hiddenQty == 25_000
        assert standingIceberg_2.cumQty == 0

        return true
    }

    def placeIceberg() {
        def trades = ob.submit(iceberg_1, NEW).trades

        assert trades.size() == 2 : "standing buy orders are executed"
        def fstTrade = trades[0]
        def secTrade = trades[1]
        assert fstTrade.executionQty == standingBuy_1.orderQty && fstTrade.price == standingBuy_1.price
        assert secTrade.executionQty == standingBuy_2.orderQty && secTrade.price == standingBuy_2.price

        def remainingSellOrders = ob.sellSide.orders
        assert remainingSellOrders.size() == 2

        def untouchedStandingSell = remainingSellOrders.find {it.clientOrderID == standingSell_1.clientOrderID}
        assert untouchedStandingSell.cumQty == 0

        def remainingPeak = remainingSellOrders.find {it.clientOrderID == iceberg_1.clientOrderID}
        assert remainingPeak.orderQty == iceberg_1.displayQty
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
        assert trades.sum {it.executionQty} == incomingMarket.orderQty

        assertTrade( trades[0], 2000, 201, iceberg_1.clientOrderID, incomingMarket.clientOrderID, iceberg_1.timestamp )
        assertTrade( trades[1], 3000, 201, iceberg_1.clientOrderID, incomingMarket.clientOrderID, incomingMarket.timestamp )

        def remainingIceberg = ob.sellSide.orders.find {it.clientOrderID == iceberg_1.clientOrderID}
        // timestamp is updated on iceberg
        assertRemainingIceberg( remainingIceberg, 7000, 30_000, incomingMarket.timestamp )

        def remainingSellLimit = ob.sellSide.orders.find { it.clientOrderID == standingSell_1.clientOrderID}
        remainingSellLimit.leavesQty == standingSell_1.leavesQty && remainingSellLimit.price == standingSell_1.price
    }

    def assertTrade(Trade trade, int expectedExecQty, double expectedPrice,
                    String expectedSellClientOrderID, String expectedBuyClientOrderID,
                    Instant expectedSellTimestamp ) {
        assert trade.sell.clientOrderID == expectedSellClientOrderID
        assert trade.buy.clientOrderID == expectedBuyClientOrderID
        assert trade.executionQty == expectedExecQty
        assert trade.price == expectedPrice
        assert trade.sell.timestamp == expectedSellTimestamp
    }

    def assertRemainingIceberg(Order remainingFirstIceberg, int leavesQty, int hiddenQty, Instant timestamp) {
        assert remainingFirstIceberg.leavesQty == leavesQty
        assert remainingFirstIceberg.hiddenQty == hiddenQty
        assert remainingFirstIceberg.timestamp == timestamp
    }
}
