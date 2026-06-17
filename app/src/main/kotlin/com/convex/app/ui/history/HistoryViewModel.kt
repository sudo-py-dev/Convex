package com.convex.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.convex.app.data.prefs.AppPreferences
import com.convex.app.domain.model.SessionRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val sessions: List<SessionRecord> = emptyList(),
    val showClearConfirm: Boolean = false,
)

@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        private val prefs: AppPreferences,
    ) : ViewModel() {
        val uiState: StateFlow<HistoryUiState> =
            prefs.sessionHistory
                .map { HistoryUiState(sessions = it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

        fun requestClear() =
            viewModelScope.launch {
                // Update show confirm flag - handled via simple local state in screen
            }

        fun clearHistory() =
            viewModelScope.launch {
                prefs.clearHistory()
            }
    }
