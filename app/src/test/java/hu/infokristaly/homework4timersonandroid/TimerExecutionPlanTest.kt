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
                            val count = maxOf(1, items[j].repeatCount)
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
}
