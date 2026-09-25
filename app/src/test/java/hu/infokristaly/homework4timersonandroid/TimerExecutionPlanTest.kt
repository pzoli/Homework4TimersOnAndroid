package hu.infokristaly.homework4timersonandroid

import hu.infokristaly.homework4timersonandroid.data.IntervalItemType
import hu.infokristaly.homework4timersonandroid.data.TimerIntervalItem
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerExecutionPlanTest {

    private fun buildExecutionPlan(items: List<TimerIntervalItem>): List<TimerIntervalItem> {
        val steps = mutableListOf<TimerIntervalItem>()
        fun expand(range: IntRange) {
            var i = range.first
            while (i <= range.last) {
                val item = items[i]
                when (item.itemType) {
                    IntervalItemType.INTERVAL -> {
                        if (item.minutes > 0) {
                            steps.add(item)
                        }
                        i++
                    }
                    IntervalItemType.OPEN_BRACKET -> {
                        var depth = 1
                        var j = i + 1
                        while (j <= range.last) {
                            if (items[j].itemType == IntervalItemType.OPEN_BRACKET) {
                                depth++
                            } else if (items[j].itemType == IntervalItemType.CLOSE_BRACKET) {
                                depth--
                                if (depth == 0) break
                            }
                            j++
                        }
                        if (j <= range.last && items[j].itemType == IntervalItemType.CLOSE_BRACKET) {
                            val count = maxOf(1, items[i].repeatCount, items[j].repeatCount)
                            val subRange = (i + 1)..<j
                            for (r in 0 until count) {
                                expand(subRange)
                            }
                            i = j + 1
                        } else {
                            i++
                        }
                    }
                    IntervalItemType.CLOSE_BRACKET -> {
                        i++
                    }
                }
            }
        }

        if (items.isNotEmpty()) {
            expand(0 until items.size)
        }
        return steps
    }

    private fun depthOf(targetItem: TimerIntervalItem, items: List<TimerIntervalItem>): Int {
        var currentDepth = 0
        for (item in items) {
            if (item.id == targetItem.id) {
                if (item.itemType == IntervalItemType.CLOSE_BRACKET) {
                    return maxOf(0, currentDepth - 1)
                }
                return currentDepth
            }
            if (item.itemType == IntervalItemType.OPEN_BRACKET) {
                currentDepth++
            } else if (item.itemType == IntervalItemType.CLOSE_BRACKET) {
                currentDepth = maxOf(0, currentDepth - 1)
            }
        }
        return 0
    }

    @Test
    fun testSampleExecutionPlanUnrolling() {
        val warmup = TimerIntervalItem(minutes = 1, label = "Warmup")
        val open = TimerIntervalItem(itemType = IntervalItemType.OPEN_BRACKET)
        val mainWork = TimerIntervalItem(minutes = 2, label = "Main")
        val close = TimerIntervalItem(itemType = IntervalItemType.CLOSE_BRACKET, repeatCount = 3)
        val cooldown = TimerIntervalItem(minutes = 1, label = "Cooldown")

        val items = listOf(warmup, open, mainWork, close, cooldown)
        val plan = buildExecutionPlan(items)

        // Expected: Warmup, Main, Main, Main, Cooldown -> Total 5 steps
        assertEquals(5, plan.size)
        assertEquals("Warmup", plan[0].label)
        assertEquals("Main", plan[1].label)
        assertEquals("Main", plan[2].label)
        assertEquals("Main", plan[3].label)
        assertEquals("Cooldown", plan[4].label)
    }

    @Test
    fun testDepthCalculation() {
        val warmup = TimerIntervalItem(minutes = 1, label = "Warmup")
        val open = TimerIntervalItem(itemType = IntervalItemType.OPEN_BRACKET)
        val mainWork = TimerIntervalItem(minutes = 2, label = "Main")
        val close = TimerIntervalItem(itemType = IntervalItemType.CLOSE_BRACKET, repeatCount = 3)
        val cooldown = TimerIntervalItem(minutes = 1, label = "Cooldown")

        val items = listOf(warmup, open, mainWork, close, cooldown)

        assertEquals(0, depthOf(warmup, items))
        assertEquals(0, depthOf(open, items))
        assertEquals(1, depthOf(mainWork, items))
        assertEquals(0, depthOf(close, items))
        assertEquals(0, depthOf(cooldown, items))
    }

    @Test
    fun testNestedIntervalGroupsExecutionPlan() {
        // Outer group x2: ( Warmup, Inner group x3: ( Sprint ), Cooldown )
        val outerOpen = TimerIntervalItem(itemType = IntervalItemType.OPEN_BRACKET)
        val run1 = TimerIntervalItem(minutes = 1, label = "Run 1")
        val innerOpen = TimerIntervalItem(itemType = IntervalItemType.OPEN_BRACKET)
        val sprint = TimerIntervalItem(minutes = 2, label = "Sprint")
        val innerClose = TimerIntervalItem(itemType = IntervalItemType.CLOSE_BRACKET, repeatCount = 3)
        val rest = TimerIntervalItem(minutes = 1, label = "Rest")
        val outerClose = TimerIntervalItem(itemType = IntervalItemType.CLOSE_BRACKET, repeatCount = 2)

        val items = listOf(outerOpen, run1, innerOpen, sprint, innerClose, rest, outerClose)
        val plan = buildExecutionPlan(items)

        // Expected per outer cycle: Run 1, Sprint, Sprint, Sprint, Rest -> 5 steps
        // Total 2 outer cycles = 10 steps
        assertEquals(10, plan.size)
        val labels = plan.map { it.label }
        val expected = listOf(
            "Run 1", "Sprint", "Sprint", "Sprint", "Rest",
            "Run 1", "Sprint", "Sprint", "Sprint", "Rest"
        )
        assertEquals(expected, labels)
    }

    @Test
    fun testMultipleSiblingNestedGroups() {
        // Parent x2: ( G1 x2: ( A ), G2 x3: ( B ) )
        val parentOpen = TimerIntervalItem(itemType = IntervalItemType.OPEN_BRACKET)
        val g1Open = TimerIntervalItem(itemType = IntervalItemType.OPEN_BRACKET)
        val a = TimerIntervalItem(minutes = 1, label = "A")
        val g1Close = TimerIntervalItem(itemType = IntervalItemType.CLOSE_BRACKET, repeatCount = 2)
        val g2Open = TimerIntervalItem(itemType = IntervalItemType.OPEN_BRACKET)
        val b = TimerIntervalItem(minutes = 2, label = "B")
        val g2Close = TimerIntervalItem(itemType = IntervalItemType.CLOSE_BRACKET, repeatCount = 3)
        val parentClose = TimerIntervalItem(itemType = IntervalItemType.CLOSE_BRACKET, repeatCount = 2)

        val items = listOf(parentOpen, g1Open, a, g1Close, g2Open, b, g2Close, parentClose)
        val plan = buildExecutionPlan(items)

        // Expected per parent cycle: A, A, B, B, B (5 steps)
        // 2 parent cycles = 10 steps
        assertEquals(10, plan.size)
        val labels = plan.map { it.label }
        val expected = listOf(
            "A", "A", "B", "B", "B",
            "A", "A", "B", "B", "B"
        )
        assertEquals(expected, labels)
    }

    @Test
    fun testDeeplyNestedGroups() {
        // Level 1 x2 -> Level 2 x3 -> Level 3 x2 ( Item ) => 2 * 3 * 2 = 12 steps
        val l1Open = TimerIntervalItem(itemType = IntervalItemType.OPEN_BRACKET)
        val l2Open = TimerIntervalItem(itemType = IntervalItemType.OPEN_BRACKET)
        val l3Open = TimerIntervalItem(itemType = IntervalItemType.OPEN_BRACKET)
        val work = TimerIntervalItem(minutes = 1, label = "Work")
        val l3Close = TimerIntervalItem(itemType = IntervalItemType.CLOSE_BRACKET, repeatCount = 2)
        val l2Close = TimerIntervalItem(itemType = IntervalItemType.CLOSE_BRACKET, repeatCount = 3)
        val l1Close = TimerIntervalItem(itemType = IntervalItemType.CLOSE_BRACKET, repeatCount = 2)

        val items = listOf(l1Open, l2Open, l3Open, work, l3Close, l2Close, l1Close)
        val plan = buildExecutionPlan(items)

        assertEquals(12, plan.size)
        assertEquals(List(12) { "Work" }, plan.map { it.label })
    }

    @Test
    fun testOpenAndCloseBracketRepeatCounts() {
        // Open bracket has repeatCount = 4, close bracket has repeatCount = 1 -> should repeat 4 times
        val open = TimerIntervalItem(itemType = IntervalItemType.OPEN_BRACKET, repeatCount = 4)
        val item = TimerIntervalItem(minutes = 1, label = "Exercise")
        val close = TimerIntervalItem(itemType = IntervalItemType.CLOSE_BRACKET, repeatCount = 1)

        val items = listOf(open, item, close)
        val plan = buildExecutionPlan(items)

        assertEquals(4, plan.size)
        assertEquals(List(4) { "Exercise" }, plan.map { it.label })
    }

}
