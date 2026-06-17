package com.convex.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.convex.app.data.prefs.AppPreferences
import com.convex.app.domain.model.SessionRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val recentSessions: List<SessionRecord> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val prefs: AppPreferences
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = prefs.sessionHistory
        .map { history -> HomeUiState(recentSessions = history.take(5)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
