package hu.infokristaly.homework4timersonandroid.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.infokristaly.homework4timersonandroid.R
import hu.infokristaly.homework4timersonandroid.data.SavedIntervalList
import hu.infokristaly.homework4timersonandroid.data.TimerIntervalItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPresetsSheet(
    savedLists: List<SavedIntervalList>,
    currentItems: List<TimerIntervalItem>,
    activePresetID: String,
    onDismiss: () -> Unit,
    onLoadPreset: (SavedIntervalList) -> Unit,
    onSaveCurrentAsNew: (String) -> Unit,
    onRenamePreset: (SavedIntervalList, String) -> Unit,
    onDeletePreset: (SavedIntervalList) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isShowingSaveAsNewDialog by remember { mutableStateOf(false) }
    var saveAsNewName by remember { mutableStateOf("") }

    var presetToRename by remember { mutableStateOf<SavedIntervalList?>(null) }
    var renameText by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd. HH:mm", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.saved_presets),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (savedLists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = null,
                            modifier = Modifier.height(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.no_saved_presets_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.no_saved_presets_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(savedLists, key = { it.id }) { list ->
                        val isActive = list.id == activePresetID
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = list.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isActive) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            AssistChip(
                                                onClick = { },
                                                label = { Text(stringResource(R.string.active)) }
                                            )
                                        }
                                    }

                                    Row {
                                        IconButton(onClick = {
                                            presetToRename = list
                                            renameText = list.name
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_name))
                                        }
                                        IconButton(onClick = { onDeletePreset(list) }) {
                                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                Text(
                                    text = "${stringResource(R.string.items_count_format, list.items.size)} • ${dateFormat.format(Date(list.createdAt))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedButton(
                                    onClick = {
                                        onLoadPreset(list)
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isActive) stringResource(R.string.reload) else stringResource(R.string.load))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (currentItems.isNotEmpty()) {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        saveAsNewName = ""
                        isShowingSaveAsNewDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.save_current_as_new))
                }
            }
        }
    }

    if (isShowingSaveAsNewDialog) {
        AlertDialog(
            onDismissRequest = { isShowingSaveAsNewDialog = false },
            title = { Text(stringResource(R.string.save_sequence_as_new_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.enter_sequence_name))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = saveAsNewName,
                        onValueChange = { saveAsNewName = it },
                        label = { Text(stringResource(R.string.template_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = saveAsNewName.trim()
                        if (trimmed.isNotEmpty()) {
                            onSaveCurrentAsNew(trimmed)
                            isShowingSaveAsNewDialog = false
                        }
                    },
                    enabled = saveAsNewName.trim().isNotEmpty()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { isShowingSaveAsNewDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (presetToRename != null) {
        AlertDialog(
            onDismissRequest = { presetToRename = null },
            title = { Text(stringResource(R.string.rename_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.enter_new_name))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text(stringResource(R.string.new_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = presetToRename
                        val trimmed = renameText.trim()
                        if (target != null && trimmed.isNotEmpty()) {
                            onRenamePreset(target, trimmed)
                            presetToRename = null
                        }
                    },
                    enabled = renameText.trim().isNotEmpty()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { presetToRename = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
