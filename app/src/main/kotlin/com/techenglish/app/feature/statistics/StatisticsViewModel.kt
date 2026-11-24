package com.techenglish.app.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techenglish.domain.model.UserStats
import com.techenglish.domain.usecase.GetStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface StatisticsUiState {
    data object Loading : StatisticsUiState
    data class Error(val message: String) : StatisticsUiState
    data class Content(val stats: UserStats) : StatisticsUiState
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    getStatistics: GetStatisticsUseCase
) : ViewModel() {

    val uiState: StateFlow<StatisticsUiState> = getStatistics()
        .map<UserStats, StatisticsUiState> { StatisticsUiState.Content(it) }
        .flowOn(Dispatchers.Default)
        .catch { emit(StatisticsUiState.Error(it.localizedMessage ?: "Не удалось загрузить статистику")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = StatisticsUiState.Loading
        )
}
