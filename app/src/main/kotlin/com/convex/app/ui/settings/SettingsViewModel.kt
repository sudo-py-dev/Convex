package com.convex.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.convex.app.data.prefs.AppPreferences
import com.convex.app.domain.model.AppLanguage
import com.convex.app.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val technicalMode: Boolean = false,
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val prefs: AppPreferences,
    ) : ViewModel() {
        val uiState: StateFlow<SettingsUiState> =
            combine(
                prefs.themeMode,
                prefs.language,
                prefs.technicalMode,
            ) { theme, lang, techMode ->
                SettingsUiState(themeMode = theme, language = lang, technicalMode = techMode)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

        fun setTheme(mode: ThemeMode) = viewModelScope.launch { prefs.setThemeMode(mode) }

        fun setLanguage(lang: AppLanguage) = viewModelScope.launch { prefs.setLanguage(lang) }

        fun setTechnicalMode(enabled: Boolean) = viewModelScope.launch { prefs.setTechnicalMode(enabled) }
    }
