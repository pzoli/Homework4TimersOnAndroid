package hu.infokristaly.homework4timersonandroid.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hu.infokristaly.homework4timersonandroid.data.ExecutionStep
import hu.infokristaly.homework4timersonandroid.data.IntervalItemType
import hu.infokristaly.homework4timersonandroid.data.PreferencesRepository
import hu.infokristaly.homework4timersonandroid.data.SavedIntervalList
import hu.infokristaly.homework4timersonandroid.data.TimerIntervalItem
import hu.infokristaly.homework4timersonandroid.data.toSavedItem
import hu.infokristaly.homework4timersonandroid.service.TimerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = PreferencesRepository(application)

    private val _storedItems = MutableStateFlow<List<TimerIntervalItem>>(emptyList())
    val storedItems: StateFlow<List<TimerIntervalItem>> = _storedItems.asStateFlow()

    private val _savedLists = MutableStateFlow<List<SavedIntervalList>>(emptyList())
    val savedLists: StateFlow<List<SavedIntervalList>> = _savedLists.asStateFlow()

    private val _activePresetID = MutableStateFlow("")
    val activePresetID: StateFlow<String> = _activePresetID.asStateFlow()

    private val _activePresetName = MutableStateFlow("")
    val activePresetName: StateFlow<String> = _activePresetName.asStateFlow()

    private val _autoContinue = MutableStateFlow(false)
    val autoContinue: StateFlow<Boolean> = _autoContinue.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _isWaitingForAcknowledgment = MutableStateFlow(false)
    val isWaitingForAcknowledgment: StateFlow<Boolean> = _isWaitingForAcknowledgment.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _currentStepIndex = MutableStateFlow<Int?>(null)
    val currentStepIndex: StateFlow<Int?> = _currentStepIndex.asStateFlow()

    private val _progressIndex = MutableStateFlow<Int?>(null)
    val progressIndex: StateFlow<Int?> = _progressIndex.asStateFlow()

    private val _totalStepsCount = MutableStateFlow<Int?>(null)
    val totalStepsCount: StateFlow<Int?> = _totalStepsCount.asStateFlow()

    private val _currentLabel = MutableStateFlow<String?>(null)
    val currentLabel: StateFlow<String?> = _currentLabel.asStateFlow()

    private val _isShowingSaveSuccessToast = MutableStateFlow(false)
    val isShowingSaveSuccessToast: StateFlow<Boolean> = _isShowingSaveSuccessToast.asStateFlow()

    private var executionPlan: List<ExecutionStep> = emptyList()
    private var timerJob: Job? = null

    init {
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        _autoContinue.value = repo.autoContinueNextInterval
        _activePresetID.value = repo.activePresetID
        _activePresetName.value = repo.activePresetName
        _savedLists.value = repo.getSavedPresets()

        var items = repo.getWorkspaceItems()
        if (!repo.didInsertSamples && items.isEmpty()) {
            items = TimerIntervalItem.samples
            repo.saveWorkspaceItems(items)
            repo.didInsertSamples = true
        }
        _storedItems.value = items
    }

    fun setAutoContinue(value: Boolean) {
        _autoContinue.value = value
        repo.autoContinueNextInterval = value
    }

    fun buildExecutionPlan(items: List<TimerIntervalItem>): List<ExecutionStep> {
        val steps = mutableListOf<ExecutionStep>()

        fun expand(range: IntRange) {
            var i = range.first
            while (i <= range.last) {
                val item = items[i]
                when (item.itemType) {
                    IntervalItemType.INTERVAL -> {
                        if (item.minutes > 0) {
                            steps.add(ExecutionStep(item = item, originalIndex = i))
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

    fun depthOf(targetItem: TimerIntervalItem, items: List<TimerIntervalItem>): Int {
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

    fun start(context: Context) {
        if (_isRunning.value && _isPaused.value) {
            resume()
            return
        }
        if (_isRunning.value && _isWaitingForAcknowledgment.value) {
            acknowledgeNextStep(context)
            return
        }

        val plan = buildExecutionPlan(_storedItems.value)
        if (_isRunning.value || plan.isEmpty()) return

        executionPlan = plan
        _isRunning.value = true
        _isPaused.value = false
        _isWaitingForAcknowledgment.value = false
        _totalStepsCount.value = plan.size

        setupStep(context, 0)
        startTicking(context)
    }

    fun pause() {
        if (!_isRunning.value || _isPaused.value) return
        _isPaused.value = true
        _isWaitingForAcknowledgment.value = false
        timerJob?.cancel()
    }

    fun resume() {
        if (!_isRunning.value || !_isPaused.value) return
        _isPaused.value = false
        startTicking(getApplication())
    }

    fun stop(context: Context) {
        timerJob?.cancel()
        timerJob = null

        TimerService.stopService(context)

        _isRunning.value = false
        _isPaused.value = false
        _isWaitingForAcknowledgment.value = false
        _progressIndex.value = null
        _currentStepIndex.value = null
        _totalStepsCount.value = null
        _currentLabel.value = null
        _remainingSeconds.value = 0
        executionPlan = emptyList()
    }

    fun acknowledgeNextStep(context: Context) {
        val current = _currentStepIndex.value ?: return
        if (!_isRunning.value || !_isWaitingForAcknowledgment.value) return

        val nextIndex = current + 1
        if (nextIndex < executionPlan.size) {
            _isWaitingForAcknowledgment.value = false
            setupStep(context, nextIndex)
            startTicking(context)
        } else {
            stop(context)
        }
    }

    private fun setupStep(context: Context, stepIndex: Int) {
        if (stepIndex >= executionPlan.size) return
        val step = executionPlan[stepIndex]
        _currentStepIndex.value = stepIndex
        _progressIndex.value = step.originalIndex
        _currentLabel.value = if (step.item.label.isEmpty()) "Intervallum" else step.item.label
        _remainingSeconds.value = maxOf(0, step.item.minutes * 60)

        val title = _currentLabel.value ?: "Intervallum"
        val body = "Hátralévő idő: ${formatTime(_remainingSeconds.value)}"
        TimerService.startService(context, title, body)
    }

    private fun startTicking(context: Context) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _isRunning.value && !_isPaused.value) {
                delay(1000L)
                val currentRem = _remainingSeconds.value
                if (currentRem > 1) {
                    _remainingSeconds.value = currentRem - 1
                    val title = _currentLabel.value ?: "Intervallum"
                    val body = "Hátralévő idő: ${formatTime(_remainingSeconds.value)}"
                    TimerService.updateNotification(context, title, body)
                } else {
                    _remainingSeconds.value = 0
                    val currentIdx = _currentStepIndex.value ?: 0
                    val isLast = currentIdx >= executionPlan.size - 1
                    val title = _currentLabel.value ?: "Intervallum"
                    val body = if (isLast) "Az összes időzítés lejárt!" else "Időzítés lejárt!"

                    TimerService.playAlarm(context, title, body)

                    if (_autoContinue.value) {
                        val nextIndex = currentIdx + 1
                        if (nextIndex < executionPlan.size) {
                            setupStep(context, nextIndex)
                        } else {
                            stop(context)
                            break
                        }
                    } else {
                        _isWaitingForAcknowledgment.value = true
                        timerJob?.cancel()
                        break
                    }
                }
            }
        }
    }

    fun addOrUpdateItem(
        targetItem: TimerIntervalItem?,
        type: IntervalItemType,
        minutes: Int,
        label: String,
        repeatCount: Int
    ) {
        val currentList = _storedItems.value.toMutableList()
        if (targetItem != null) {
            val index = currentList.indexOfFirst { it.id == targetItem.id }
            if (index != -1) {
                currentList[index] = targetItem.copy(
                    itemType = type,
                    minutes = minutes,
                    label = label,
                    repeatCount = repeatCount
                )
            }
        } else {
            val newItem = TimerIntervalItem(
                itemType = type,
                minutes = minutes,
                label = label,
                repeatCount = repeatCount,
                createdAt = System.currentTimeMillis()
            )
            currentList.add(newItem)
        }
        _storedItems.value = currentList
        repo.saveWorkspaceItems(currentList)
    }

    fun deleteItem(item: TimerIntervalItem) {
        val currentList = _storedItems.value.toMutableList()
        currentList.removeAll { it.id == item.id }
        _storedItems.value = currentList
        repo.saveWorkspaceItems(currentList)
    }

    fun moveItemUp(item: TimerIntervalItem) {
        val currentList = _storedItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == item.id }
        if (index > 0) {
            val temp = currentList[index]
            currentList[index] = currentList[index - 1]
            currentList[index - 1] = temp
            _storedItems.value = currentList
            repo.saveWorkspaceItems(currentList)
        }
    }

    fun moveItemDown(item: TimerIntervalItem) {
        val currentList = _storedItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == item.id }
        if (index != -1 && index < currentList.size - 1) {
            val temp = currentList[index]
            currentList[index] = currentList[index + 1]
            currentList[index + 1] = temp
            _storedItems.value = currentList
            repo.saveWorkspaceItems(currentList)
        }
    }

    fun handleSaveAction(context: Context) {
        val currentPreset = _savedLists.value.find { it.id == _activePresetID.value }
        if (currentPreset != null) {
            currentPreset.items = _storedItems.value.map { it.toSavedItem() }
            repo.saveSavedPresets(_savedLists.value)
            showToast()
        }
    }

    fun saveAsNewPreset(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return

        val savedItems = _storedItems.value.map { it.toSavedItem() }
        val newPreset = SavedIntervalList(
            name = trimmed,
            createdAt = System.currentTimeMillis(),
            items = savedItems
        )
        val currentPresets = _savedLists.value.toMutableList()
        currentPresets.add(0, newPreset)

        _savedLists.value = currentPresets
        _activePresetID.value = newPreset.id
        _activePresetName.value = trimmed

        repo.saveSavedPresets(currentPresets)
        repo.activePresetID = newPreset.id
        repo.activePresetName = trimmed

        showToast()
    }

    fun createNewEmptyTemplate(context: Context) {
        if (_isRunning.value) {
            stop(context)
        }
        _storedItems.value = emptyList()
        _activePresetID.value = ""
        _activePresetName.value = ""

        repo.saveWorkspaceItems(emptyList())
        repo.activePresetID = ""
        repo.activePresetName = ""
    }

    fun loadPreset(context: Context, preset: SavedIntervalList) {
        if (_isRunning.value) {
            stop(context)
        }
        val newItems = preset.createTimerIntervalItems()
        _storedItems.value = newItems
        _activePresetID.value = preset.id
        _activePresetName.value = preset.name

        repo.saveWorkspaceItems(newItems)
        repo.activePresetID = preset.id
        repo.activePresetName = preset.name
    }

    fun deletePreset(preset: SavedIntervalList) {
        val currentPresets = _savedLists.value.toMutableList()
        currentPresets.removeAll { it.id == preset.id }
        _savedLists.value = currentPresets
        repo.saveSavedPresets(currentPresets)

        if (_activePresetID.value == preset.id) {
            _activePresetID.value = ""
            _activePresetName.value = ""
            repo.activePresetID = ""
            repo.activePresetName = ""
        }
    }

    fun renamePreset(preset: SavedIntervalList, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return

        val currentPresets = _savedLists.value.toMutableList()
        val index = currentPresets.indexOfFirst { it.id == preset.id }
        if (index != -1) {
            currentPresets[index].name = trimmed
            _savedLists.value = currentPresets
            repo.saveSavedPresets(currentPresets)

            if (_activePresetID.value == preset.id) {
                _activePresetName.value = trimmed
                repo.activePresetName = trimmed
            }
        }
    }

    private fun showToast() {
        viewModelScope.launch {
            _isShowingSaveSuccessToast.value = true
            delay(2000L)
            _isShowingSaveSuccessToast.value = false
        }
    }

    fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }
}
