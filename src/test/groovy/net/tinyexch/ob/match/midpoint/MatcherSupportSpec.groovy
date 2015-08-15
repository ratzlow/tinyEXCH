package net.tinyexch.ob.match.midpoint

import net.tinyexch.ob.OrderbookSide
import net.tinyexch.ob.match.MatchEngine
import net.tinyexch.order.Side
import spock.lang.Specification

import static net.tinyexch.ob.match.OrderFactory.*

/**
 * Test algo to identify the theoretical executable qty.
 *
 * @author ratzlow@gmail.com
 * @since 2015-07-30
 * @see net.tinyexch.ob.match.MidpointOrderSpec
 */
class MatcherSupportSpec extends Specification {

    def "gather available Qty"() {

        when: "Limited midpoints standing with MAQ"
        def standingBuyMid_Lim = buyMid_Lim(201, 5000, time("09:01:00"))
        def standingBuyMid_LimMAQ = buyMid_LimMAQ(200, 4000, time("09:02:00"), 3000)
        OrderbookSide buySide = new OrderbookSide( Side.BUY, MatchEngine.MIDPOINT_COMPARATOR );
        buySide.add( standingBuyMid_Lim )
        buySide.add( standingBuyMid_LimMAQ )

        def sellMid_LimMAQ = sellMid_LimMAQ(200, 6000, time("09:02:00"), 6000)
        def collector = MatcherSupport.SELF.collectQty(sellMid_LimMAQ, buySide)

        then: "2 matches are found"
        collector.potentialMatches.size() == 2
        def first = collector.potentialMatches[0]
        def second = collector.potentialMatches[1]

        // we deal with a stack - LIFO
        first.otherSide.clientOrderID == standingBuyMid_LimMAQ.clientOrderID
        second.otherSide.clientOrderID == standingBuyMid_Lim.clientOrderID

        second.executableQty == 3000
        first.executableQty == 3000
    }
}
