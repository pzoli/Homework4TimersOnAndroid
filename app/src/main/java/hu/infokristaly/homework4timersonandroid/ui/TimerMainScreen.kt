package hu.infokristaly.homework4timersonandroid.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.infokristaly.homework4timersonandroid.data.IntervalItemType
import hu.infokristaly.homework4timersonandroid.data.TimerIntervalItem
import hu.infokristaly.homework4timersonandroid.viewmodel.TimerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerMainScreen(viewModel: TimerViewModel) {
    val context = LocalContext.current

    val storedItems by viewModel.storedItems.collectAsState()
    val savedLists by viewModel.savedLists.collectAsState()
    val activePresetID by viewModel.activePresetID.collectAsState()
    val activePresetName by viewModel.activePresetName.collectAsState()
    val autoContinue by viewModel.autoContinue.collectAsState()

    val isRunning by viewModel.isRunning.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val isWaitingForAcknowledgment by viewModel.isWaitingForAcknowledgment.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val currentStepIndex by viewModel.currentStepIndex.collectAsState()
    val progressIndex by viewModel.progressIndex.collectAsState()
    val totalStepsCount by viewModel.totalStepsCount.collectAsState()
    val currentLabel by viewModel.currentLabel.collectAsState()
    val isShowingSaveSuccessToast by viewModel.isShowingSaveSuccessToast.collectAsState()

    var activeSheetItem by remember { mutableStateOf<TimerIntervalItem?>(null) }
    var isCreatingNewItem by remember { mutableStateOf(false) }

    var isShowingPresetsSheet by remember { mutableStateOf(false) }
    var isShowingSettingsSheet by remember { mutableStateOf(false) }
    var isShowingMenuDropdown by remember { mutableStateOf(false) }

    var isShowingSaveAsNewAlert by remember { mutableStateOf(false) }
    var saveAsNewName by remember { mutableStateOf("") }

    var isShowingNewTemplateAlert by remember { mutableStateOf(false) }

    val currentLoadedPreset = remember(savedLists, activePresetID) {
        savedLists.find { it.id == activePresetID }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Intervallum időzítő",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { isShowingSettingsSheet = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Beállítások")
                    }

                    Box {
                        IconButton(onClick = { isShowingMenuDropdown = true }) {
                            Icon(Icons.Default.Folder, contentDescription = "Sablonok menü")
                        }
                        DropdownMenu(
                            expanded = isShowingMenuDropdown,
                            onDismissRequest = { isShowingMenuDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Mentett sablonok") },
                                onClick = {
                                    isShowingMenuDropdown = false
                                    isShowingPresetsSheet = true
                                },
                                leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Beállítások") },
                                onClick = {
                                    isShowingMenuDropdown = false
                                    isShowingSettingsSheet = true
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Új sablon") },
                                onClick = {
                                    isShowingMenuDropdown = false
                                    if (storedItems.isEmpty()) {
                                        viewModel.createNewEmptyTemplate(context)
                                    } else {
                                        isShowingNewTemplateAlert = true
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = {
                                    val presetNameText = if (currentLoadedPreset != null) " (${currentLoadedPreset.name})" else ""
                                    Text("Mentés$presetNameText")
                                },
                                onClick = {
                                    isShowingMenuDropdown = false
                                    if (currentLoadedPreset != null) {
                                        viewModel.handleSaveAction(context)
                                    } else {
                                        saveAsNewName = activePresetName
                                        isShowingSaveAsNewAlert = true
                                    }
                                },
                                enabled = storedItems.isNotEmpty(),
                                leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Mentés újként...") },
                                onClick = {
                                    isShowingMenuDropdown = false
                                    saveAsNewName = if (activePresetName.isEmpty()) "" else "$activePresetName másolata"
                                    isShowingSaveAsNewAlert = true
                                },
                                enabled = storedItems.isNotEmpty(),
                                leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) }
                            )
                        }
                    }

                    IconButton(onClick = { isCreatingNewItem = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Új elem")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Loaded preset header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = "Betöltött sablon: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (activePresetName.isEmpty()) "Nincs (Egyéni)" else activePresetName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (currentLoadedPreset != null && storedItems.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { viewModel.handleSaveAction(context) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.height(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mentés", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Running Status Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Futás állapota",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isRunning && currentLabel != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentLabel ?: "",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                if (isWaitingForAcknowledgment) {
                                    AssistChip(
                                        onClick = { },
                                        label = { Text("Nyugtázásra vár", color = Color.White) },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                } else if (isPaused) {
                                    AssistChip(
                                        onClick = { },
                                        label = { Text("Felfüggesztve", color = Color.White) },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (isWaitingForAcknowledgment) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Az intervallum lejárt! Koppints a gombra a következő szakasz indításához.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.acknowledgeNextStep(context) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Következő szakasz indítása")
                                    }
                                }
                            } else {
                                Text(
                                    text = "Hátralévő idő: ${viewModel.formatTime(remainingSeconds)}",
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (currentStepIndex != null && totalStepsCount != null) {
                                    Text(
                                        text = "Lépés ${(currentStepIndex ?: 0) + 1}/$totalStepsCount",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (progressIndex != null) {
                                    Text(
                                        text = "Lépés ${(progressIndex ?: 0) + 1}/${storedItems.size}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Nincs futó szakasz",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interval list
                Box(modifier = Modifier.weight(1f)) {
                    if (storedItems.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.height(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Nincs intervallum",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Koppints a + gombra új intervallum vagy zárójel hozzáadásához, vagy tölts be egy mentett sablont.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(storedItems, key = { _, item -> item.id }) { index, item ->
                                val depth = viewModel.depthOf(item, storedItems)
                                val isActiveRow = isRunning && progressIndex == index
                                val rowTextColor = if (isActiveRow) Color.Black else Color.Unspecified
                                val rowSecondaryTextColor = if (isActiveRow) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = (depth * 16).dp)
                                        .clickable { activeSheetItem = item },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isActiveRow) Color(0xFFB3E5FC)
                                        else MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            when (item.itemType) {
                                                IntervalItemType.INTERVAL -> {
                                                    Text(
                                                        text = "${item.minutes} perc",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        color = rowTextColor
                                                    )
                                                    if (item.label.isNotEmpty()) {
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Text(
                                                            text = item.label,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = rowTextColor
                                                        )
                                                    }
                                                }
                                                IntervalItemType.OPEN_BRACKET -> {
                                                    Text(
                                                        text = "(",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 20.sp,
                                                        color = if (isActiveRow) Color.Black else Color(0xFF2196F3)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Nyitó zárójel",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = rowSecondaryTextColor
                                                    )
                                                }
                                                IntervalItemType.CLOSE_BRACKET -> {
                                                    Text(
                                                        text = ")",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 20.sp,
                                                        color = if (isActiveRow) Color.Black else Color(0xFF2196F3)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "${item.repeatCount}× ismétlés",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = rowTextColor
                                                    )
                                                }
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { viewModel.moveItemUp(item) },
                                                enabled = index > 0
                                            ) {
                                                Icon(Icons.Default.ArrowUpward, contentDescription = "Up", modifier = Modifier.height(18.dp))
                                            }
                                            IconButton(
                                                onClick = { viewModel.moveItemDown(item) },
                                                enabled = index < storedItems.size - 1
                                            ) {
                                                Icon(Icons.Default.ArrowDownward, contentDescription = "Down", modifier = Modifier.height(18.dp))
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteItem(item) }
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Törlés", tint = MaterialTheme.colorScheme.error, modifier = Modifier.height(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val canPlay = (!isRunning || isPaused || isWaitingForAcknowledgment) &&
                            viewModel.buildExecutionPlan(storedItems).isNotEmpty()

                    Button(
                        onClick = {
                            if (isWaitingForAcknowledgment) {
                                viewModel.acknowledgeNextStep(context)
                            } else if (isPaused) {
                                viewModel.resume()
                            } else {
                                viewModel.start(context)
                            }
                        },
                        enabled = canPlay,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Lejátszás")
                    }

                    Button(
                        onClick = { viewModel.pause() },
                        enabled = isRunning && !isPaused && !isWaitingForAcknowledgment,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Felfüggesztés")
                    }

                    Button(
                        onClick = { viewModel.stop(context) },
                        enabled = isRunning,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Leállítás")
                    }
                }
            }

            // Toast overlay
            AnimatedVisibility(
                visible = isShowingSaveSuccessToast,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = CircleShape,
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = "Sablon sikeresen mentve!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    // Sheets & Dialogs
    if (activeSheetItem != null || isCreatingNewItem) {
        IntervalEditSheet(
            itemToEdit = activeSheetItem,
            initialType = IntervalItemType.INTERVAL,
            onDismiss = {
                activeSheetItem = null
                isCreatingNewItem = false
            },
            onSave = { type, minutes, label, repeatCount ->
                viewModel.addOrUpdateItem(activeSheetItem, type, minutes, label, repeatCount)
                activeSheetItem = null
                isCreatingNewItem = false
            }
        )
    }

    if (isShowingPresetsSheet) {
        SavedPresetsSheet(
            savedLists = savedLists,
            currentItems = storedItems,
            activePresetID = activePresetID,
            onDismiss = { isShowingPresetsSheet = false },
            onLoadPreset = { preset ->
                viewModel.loadPreset(context, preset)
            },
            onSaveCurrentAsNew = { name ->
                viewModel.saveAsNewPreset(name)
            },
            onRenamePreset = { preset, newName ->
                viewModel.renamePreset(preset, newName)
            },
            onDeletePreset = { preset ->
                viewModel.deletePreset(preset)
            }
        )
    }

    if (isShowingSettingsSheet) {
        SettingsSheet(
            autoContinue = autoContinue,
            onAutoContinueChanged = { viewModel.setAutoContinue(it) },
            onDismiss = { isShowingSettingsSheet = false }
        )
    }

    if (isShowingSaveAsNewAlert) {
        AlertDialog(
            onDismissRequest = { isShowingSaveAsNewAlert = false },
            title = { Text("Mentés új sablonként") },
            text = {
                Column {
                    Text("Add meg az új sablon nevét:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = saveAsNewName,
                        onValueChange = { saveAsNewName = it },
                        label = { Text("Sablon neve") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveAsNewPreset(saveAsNewName)
                        isShowingSaveAsNewAlert = false
                    },
                    enabled = saveAsNewName.trim().isNotEmpty()
                ) {
                    Text("Mentés")
                }
            },
            dismissButton = {
                TextButton(onClick = { isShowingSaveAsNewAlert = false }) {
                    Text("Mégse")
                }
            }
        )
    }

    if (isShowingNewTemplateAlert) {
        AlertDialog(
            onDismissRequest = { isShowingNewTemplateAlert = false },
            title = { Text("Új üres sablon") },
            text = {
                Text("Biztosan új üres sablont szeretnél létrehozni? A munkaterület elemei törlődnek.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createNewEmptyTemplate(context)
                        isShowingNewTemplateAlert = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Új sablon létrehozása")
                }
            },
            dismissButton = {
                TextButton(onClick = { isShowingNewTemplateAlert = false }) {
                    Text("Mégse")
                }
            }
        )
    }
}
