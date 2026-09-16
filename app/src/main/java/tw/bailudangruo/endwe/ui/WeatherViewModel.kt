package tw.bailudangruo.endwe.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tw.bailudangruo.endwe.data.OpenMeteoWeatherRepository
import tw.bailudangruo.endwe.data.WeatherRepository
import tw.bailudangruo.endwe.model.TaiwanLocations
import tw.bailudangruo.endwe.model.WeatherLocation
import tw.bailudangruo.endwe.model.WeatherSnapshot

data class WeatherUiState(
    val selectedLocation: WeatherLocation = TaiwanLocations.first(),
    val weather: WeatherSnapshot? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

class WeatherViewModel(
    private val repository: WeatherRepository = OpenMeteoWeatherRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var requestJob: Job? = null

    init {
        loadWeather(showFullLoading = true)
    }

    fun selectLocation(location: WeatherLocation) {
        if (location == _uiState.value.selectedLocation) return
        _uiState.value = _uiState.value.copy(
            selectedLocation = location,
            weather = null,
            errorMessage = null,
        )
        loadWeather(showFullLoading = true)
    }

    fun refresh() {
        loadWeather(showFullLoading = _uiState.value.weather == null)
    }

    private fun loadWeather(showFullLoading: Boolean) {
        requestJob?.cancel()
        val location = _uiState.value.selectedLocation
        _uiState.value = _uiState.value.copy(
            isLoading = showFullLoading,
            isRefreshing = !showFullLoading,
            errorMessage = null,
        )

        requestJob = viewModelScope.launch {
            try {
                val weather = repository.getWeather(location)
                _uiState.value = _uiState.value.copy(
                    weather = weather,
                    isLoading = false,
                    isRefreshing = false,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = error.localizedMessage ?: "無法取得天氣資料，請稍後再試。",
                )
            }
        }
    }
}
