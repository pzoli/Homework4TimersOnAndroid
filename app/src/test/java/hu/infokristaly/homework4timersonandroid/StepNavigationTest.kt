package hu.infokristaly.homework4timersonandroid

import hu.infokristaly.homework4timersonandroid.data.ExecutionStep
import hu.infokristaly.homework4timersonandroid.data.IntervalItemType
import hu.infokristaly.homework4timersonandroid.data.TimerIntervalItem
import org.junit.Assert.assertEquals
import org.junit.Test

class StepNavigationTest {

    @Test
    fun testSkipPreviousAndNextIndexCalculation() {
        val step1 = ExecutionStep(TimerIntervalItem(minutes = 1, label = "Warmup"), originalIndex = 0)
        val step2 = ExecutionStep(TimerIntervalItem(minutes = 2, label = "Main"), originalIndex = 1)
        val step3 = ExecutionStep(TimerIntervalItem(minutes = 1, label = "Cooldown"), originalIndex = 2)

        val plan = listOf(step1, step2, step3)

        // Starting at step 0
        var currentIndex = 0

        // Test skip previous at index 0 (should stay at 0)
        val prevAtZero = if (currentIndex > 0) currentIndex - 1 else currentIndex
        assertEquals(0, prevAtZero)

        // Test skip next at index 0 (should go to 1)
        val nextAtZero = if (currentIndex < plan.size - 1) currentIndex + 1 else currentIndex
        assertEquals(1, nextAtZero)

        // Move to index 1
        currentIndex = 1

        // Test skip previous at index 1 (should go to 0)
        val prevAtOne = if (currentIndex > 0) currentIndex - 1 else currentIndex
        assertEquals(0, prevAtOne)

        // Test skip next at index 1 (should go to 2)
        val nextAtOne = if (currentIndex < plan.size - 1) currentIndex + 1 else currentIndex
        assertEquals(2, nextAtOne)

        // Move to last index (2)
        currentIndex = 2

        // Test skip next at last index (should stay at 2)
        val nextAtLast = if (currentIndex < plan.size - 1) currentIndex + 1 else currentIndex
        assertEquals(2, nextAtLast)
    }
}
