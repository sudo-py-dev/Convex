package com.convex.app.ui.operation.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.convex.app.R
import com.convex.app.domain.model.OperationParam

@Composable
fun paramField(
    param: OperationParam,
    value: String,
    onValueChange: (String) -> Unit,
    onFileSelected: (Uri, String, String) -> Unit,
    context: Context,
) {
    when (param) {
        is OperationParam.FilePicker ->
            filePickerField(
                param = param,
                value = value,
                onFileSelected = onFileSelected,
                context = context,
            )

        is OperationParam.SliderParam -> sliderField(param, value, onValueChange)

        is OperationParam.DropdownParam -> dropdownField(param, value, onValueChange)

        is OperationParam.TextParam -> textParamField(param, value, onValueChange)

        is OperationParam.SwitchParam -> switchField(param, value, onValueChange)
    }
}

@Composable
private fun filePickerField(
    param: OperationParam.FilePicker,
    value: String,
    onFileSelected: (Uri, String, String) -> Unit,
    context: Context,
) {
    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            val displayName = context.getDisplayName(uri)
            val realPath = uri.toString()
            onFileSelected(uri, displayName, realPath)
        }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(param.labelRes) + if (param.required) " *" else "",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { launcher.launch(param.mimeTypes.toTypedArray()) },
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.btn_pick_file))
            }

            if (value.isNotBlank()) {
                val filename =
                    value.substringAfterLast('/').let {
                        if (it.length > 24) it.take(21) + "…" else it
                    }
                AssistChip(
                    onClick = { launcher.launch(param.mimeTypes.toTypedArray()) },
                    label = { Text(filename, style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Outlined.InsertDriveFile,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun sliderField(
    param: OperationParam.SliderParam,
    value: String,
    onValueChange: (String) -> Unit,
) {
    val current = value.toFloatOrNull() ?: param.defaultValue
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(param.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = param.valueFormat.format(current),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = current,
            onValueChange = { onValueChange(it.toString()) },
            valueRange = param.min..param.max,
            steps = param.steps,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                param.valueFormat.format(param.min),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                param.valueFormat.format(param.max),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun dropdownField(
    param: OperationParam.DropdownParam,
    value: String,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel =
        param.options.firstOrNull { it.second == value }?.first
            ?: param.options.getOrNull(param.defaultIndex)?.first ?: ""

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(param.labelRes) + if (param.required) " *" else "",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable),
                shape = MaterialTheme.shapes.medium,
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                param.options.forEach { (label, ffVal) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onValueChange(ffVal)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

@Composable
private fun textParamField(
    param: OperationParam.TextParam,
    value: String,
    onValueChange: (String) -> Unit,
) {
    val isOutputPath = param.id.contains("output", ignoreCase = true)
    if (isOutputPath) {
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("*/*")
        ) { uri ->
            uri?.let { onValueChange(it.toString()) }
        }
        val isUri = value.startsWith("content://")

        OutlinedTextField(
            value = if (isUri) {
                val context = LocalContext.current
                remember(value) {
                    runCatching { context.getDisplayName(Uri.parse(value)) }.getOrDefault("output_file")
                }
            } else {
                value
            },
            onValueChange = onValueChange,
            label = {
                Text(stringResource(param.labelRes) + if (param.required) " *" else "")
            },
            singleLine = param.singleLine,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            readOnly = isUri,
            trailingIcon = {
                if (isUri) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(
                            imageVector = Icons.Outlined.Cancel,
                            contentDescription = stringResource(R.string.desc_clear_custom_output),
                        )
                    }
                } else {
                    IconButton(onClick = {
                        val defaultName = value.ifBlank { param.defaultValue }.ifBlank { "output.mp4" }
                        launcher.launch(defaultName)
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = stringResource(R.string.desc_select_custom_output),
                        )
                    }
                }
            },
        )
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(stringResource(param.labelRes) + if (param.required) " *" else "")
            },
            singleLine = param.singleLine,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
    }
}

@Composable
private fun switchField(
    param: OperationParam.SwitchParam,
    value: String,
    onValueChange: (String) -> Unit,
) {
    val checked = value.toBooleanStrictOrNull() ?: param.defaultValue
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(param.labelRes),
            style = MaterialTheme.typography.bodyMedium,
        )
        Switch(
            checked = checked,
            onCheckedChange = { onValueChange(it.toString()) },
        )
    }
}

private fun Context.getDisplayName(uri: Uri): String {
    return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        cursor.getString(nameIndex)
    } ?: uri.lastPathSegment ?: "file"
}
