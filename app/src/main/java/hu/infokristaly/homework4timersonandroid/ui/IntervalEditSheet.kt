package hu.infokristaly.homework4timersonandroid.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import hu.infokristaly.homework4timersonandroid.data.IntervalItemType
import hu.infokristaly.homework4timersonandroid.data.TimerIntervalItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntervalEditSheet(
    itemToEdit: TimerIntervalItem?,
    initialType: IntervalItemType = IntervalItemType.INTERVAL,
    onDismiss: () -> Unit,
    onSave: (type: IntervalItemType, minutes: Int, label: String, repeatCount: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var itemType by remember { mutableStateOf(itemToEdit?.itemType ?: initialType) }
    var minutesText by remember { mutableStateOf(itemToEdit?.minutes?.toString() ?: "1") }
    var labelText by remember { mutableStateOf(itemToEdit?.label ?: "") }
    var repeatCount by remember { mutableIntStateOf(itemToEdit?.repeatCount ?: 2) }

    val isFormValid = remember(itemType, minutesText, repeatCount) {
        when (itemType) {
            IntervalItemType.INTERVAL -> (minutesText.toIntOrNull() ?: 0) > 0
            IntervalItemType.OPEN_BRACKET -> true
            IntervalItemType.CLOSE_BRACKET -> repeatCount >= 1
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Mégse")
                }
                Text(
                    text = if (itemToEdit == null) "Új felvétel" else "Szerkesztés",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        val m = minutesText.toIntOrNull() ?: 1
                        onSave(itemType, m, labelText.trim(), repeatCount)
                        onDismiss()
                    },
                    enabled = isFormValid
                ) {
                    Text(if (itemToEdit == null) "Hozzáadás" else "Mentés")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Elem típusa", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                val types = listOf(
                    IntervalItemType.INTERVAL to "Intervallum",
                    IntervalItemType.OPEN_BRACKET to "Nyitó (",
                    IntervalItemType.CLOSE_BRACKET to "Záró )"
                )
                types.forEachIndexed { index, pair ->
                    SegmentedButton(
                        selected = itemType == pair.first,
                        onClick = { itemType = pair.first },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size)
                    ) {
                        Text(pair.second)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (itemType) {
                IntervalItemType.INTERVAL -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = minutesText,
                                onValueChange = { minutesText = it },
                                label = { Text("Perc") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = labelText,
                                onValueChange = { labelText = it },
                                label = { Text("Címke") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
                IntervalItemType.OPEN_BRACKET -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Text(
                            text = "Nyitó zárójel ( megadása a csoportos ismétlés kezdéséhez.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                IntervalItemType.CLOSE_BRACKET -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ismétlések száma: $repeatCount", style = MaterialTheme.typography.bodyLarge)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (repeatCount > 1) repeatCount-- }
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Csökkentés")
                                    }
                                    IconButton(
                                        onClick = { if (repeatCount < 99) repeatCount++ }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Növelés")
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "A nyitó és záró zárójel közötti intervallumok ennyiszer fognak megismétlődni.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
