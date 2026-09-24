package hu.infokristaly.homework4timersonandroid.data

import java.util.UUID

enum class IntervalItemType {
    INTERVAL,
    OPEN_BRACKET,
    CLOSE_BRACKET
}

data class TimerIntervalItem(
    val id: String = UUID.randomUUID().toString(),
    var minutes: Int = 1,
    var label: String = "",
    var itemType: IntervalItemType = IntervalItemType.INTERVAL,
    var repeatCount: Int = 1,
    var createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        val samples: List<TimerIntervalItem>
            get() {
                val baseTime = System.currentTimeMillis()
                return listOf(
                    TimerIntervalItem(
                        minutes = 1,
                        label = "Bemelegítés",
                        createdAt = baseTime
                    ),
                    TimerIntervalItem(
                        itemType = IntervalItemType.OPEN_BRACKET,
                        createdAt = baseTime + 1000
                    ),
                    TimerIntervalItem(
                        minutes = 2,
                        label = "Fő szakasz",
                        createdAt = baseTime + 2000
                    ),
                    TimerIntervalItem(
                        itemType = IntervalItemType.CLOSE_BRACKET,
                        repeatCount = 3,
                        createdAt = baseTime + 3000
                    ),
                    TimerIntervalItem(
                        minutes = 1,
                        label = "Levezetés",
                        createdAt = baseTime + 4000
                    )
                )
            }
    }
}

data class SavedIntervalItem(
    val id: String = UUID.randomUUID().toString(),
    val minutes: Int = 0,
    val label: String = "",
    val itemType: IntervalItemType = IntervalItemType.INTERVAL,
    val repeatCount: Int = 1
)

fun TimerIntervalItem.toSavedItem(): SavedIntervalItem {
    return SavedIntervalItem(
        id = this.id,
        minutes = this.minutes,
        label = this.label,
        itemType = this.itemType,
        repeatCount = this.repeatCount
    )
}

fun SavedIntervalItem.toTimerItem(createdAtOffsetMillis: Long = 0): TimerIntervalItem {
    return TimerIntervalItem(
        id = UUID.randomUUID().toString(),
        minutes = this.minutes,
        label = this.label,
        itemType = this.itemType,
        repeatCount = this.repeatCount,
        createdAt = System.currentTimeMillis() + createdAtOffsetMillis
    )
}

data class SavedIntervalList(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    var items: List<SavedIntervalItem> = emptyList()
) {
    fun createTimerIntervalItems(): List<TimerIntervalItem> {
        val baseTime = System.currentTimeMillis()
        return items.mapIndexed { index, item ->
            item.toTimerItem(createdAtOffsetMillis = index.toLong())
        }
    }
}

data class ExecutionStep(
    val item: TimerIntervalItem,
    val originalIndex: Int
)
