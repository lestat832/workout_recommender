package com.workoutapp.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Phase 4a: reps-in-reserve chip row. Renders six chips (0..5) below an
 * exercise's set list once all prescribed sets are complete. Tapping an
 * unselected chip selects it; tapping the currently-selected chip clears the
 * value back to null (callback fires with null). This is the only way a user
 * can correct a mistap.
 *
 * @param selectedRir currently-selected value, or null if not yet logged
 * @param onSelect invoked with the new value (0..5 to set, null to clear)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RirChipRow(
    selectedRir: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(
            text = "Reps in reserve?",
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (0..5).forEach { value ->
                val isSelected = selectedRir == value
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(if (isSelected) null else value) },
                    label = { Text(value.toString()) },
                    colors = FilterChipDefaults.filterChipColors()
                )
            }
        }
        if (selectedRir == null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "how many more reps could you have done?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
