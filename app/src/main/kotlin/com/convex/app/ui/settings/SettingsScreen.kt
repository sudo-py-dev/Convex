package com.convex.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.convex.app.BuildConfig
import com.convex.app.R
import com.convex.app.domain.model.AppLanguage
import com.convex.app.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun settingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Theme ──────────────────────────────────────────────────────────
            settingsGroupHeader(
                icon = Icons.Outlined.Palette,
                title = stringResource(R.string.settings_theme),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    val label =
                        when (mode) {
                            ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                            ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                            ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                        }
                    SegmentedButton(
                        shape =
                            SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ThemeMode.entries.size,
                            ),
                        onClick = { viewModel.setTheme(mode) },
                        selected = uiState.themeMode == mode,
                        icon = {
                            SegmentedButtonDefaults.Icon(active = uiState.themeMode == mode) {
                                Icon(
                                    imageVector =
                                        when (mode) {
                                            ThemeMode.SYSTEM -> Icons.Outlined.PhoneAndroid
                                            ThemeMode.LIGHT -> Icons.Outlined.LightMode
                                            ThemeMode.DARK -> Icons.Outlined.DarkMode
                                        },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        },
                    ) { Text(label, style = MaterialTheme.typography.labelMedium) }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // ── Language ───────────────────────────────────────────────────────
            settingsGroupHeader(
                icon = Icons.Outlined.Language,
                title = stringResource(R.string.settings_language),
            )
            var languageExpanded by remember { mutableStateOf(false) }
            val currentLanguageLabel =
                when (uiState.language) {
                    AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
                    AppLanguage.ENGLISH -> stringResource(R.string.settings_language_en)
                    AppLanguage.HEBREW -> stringResource(R.string.settings_language_iw)
                }

            ExposedDropdownMenuBox(
                expanded = languageExpanded,
                onExpandedChange = { languageExpanded = !languageExpanded },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = currentLanguageLabel,
                    onValueChange = {},
                    readOnly = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(languageExpanded) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = MenuAnchorType.PrimaryNotEditable),
                    shape = MaterialTheme.shapes.medium,
                    colors =
                        ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        ),
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
                ExposedDropdownMenu(
                    expanded = languageExpanded,
                    onDismissRequest = { languageExpanded = false },
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val label =
                            when (lang) {
                                AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
                                AppLanguage.ENGLISH -> stringResource(R.string.settings_language_en)
                                AppLanguage.HEBREW -> stringResource(R.string.settings_language_iw)
                            }
                        DropdownMenuItem(
                            text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                viewModel.setLanguage(lang)
                                languageExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // ── Technical Mode ─────────────────────────────────────────────────
            settingsGroupHeader(
                icon = Icons.Outlined.Terminal,
                title = stringResource(R.string.settings_technical_mode),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_technical_mode)) },
                supportingContent = { Text(stringResource(R.string.settings_technical_mode_desc)) },
                trailingContent = {
                    Switch(
                        checked = uiState.technicalMode,
                        onCheckedChange = { viewModel.setTechnicalMode(it) },
                    )
                },
                leadingContent = { Icon(Icons.Outlined.Terminal, contentDescription = null) },
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // ── About ──────────────────────────────────────────────────────────
            settingsGroupHeader(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.settings_about),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version)) },
                trailingContent = { Text(BuildConfig.VERSION_NAME) },
                leadingContent = { Icon(Icons.Outlined.Tag, contentDescription = null) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_ffmpeg_kit)) },
                trailingContent = { Text(stringResource(R.string.settings_ffmpeg_kit_version)) },
                leadingContent = { Icon(Icons.Outlined.Terminal, contentDescription = null) },
            )
        }
    }
}

@Composable
private fun settingsGroupHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
