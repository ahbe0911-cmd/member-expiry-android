package ir.memberexpiry.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Ready<T>(val value: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

internal fun <T> ViewModel.asUiState(source: Flow<T>): StateFlow<UiState<T>> = source
    .map<T, UiState<T>> { UiState.Ready(it) }
    .retryWhen { cause, _ ->
        if (cause is Error) return@retryWhen false
        emit(UiState.Error("خواندن اطلاعات ممکن نشد؛ دوباره تلاش کنید."))
        delay(3_000)
        true
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
