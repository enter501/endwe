package tw.bailudangruo.endwe.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tw.bailudangruo.endwe.data.FavoriteLocationStore
import tw.bailudangruo.endwe.data.OpenMeteoWeatherRepository
import tw.bailudangruo.endwe.data.WeatherRepository
import tw.bailudangruo.endwe.model.TaiwanLocations
import tw.bailudangruo.endwe.model.WeatherLocation
import tw.bailudangruo.endwe.model.WeatherSnapshot
import kotlin.math.abs

data class WeatherUiState(
    val selectedLocation: WeatherLocation = TaiwanLocations.first(),
    val favoriteLocations: List<WeatherLocation> = TaiwanLocations,
    val searchQuery: String = "",
    val searchResults: List<WeatherLocation> = emptyList(),
    val isSearching: Boolean = false,
    val searchMessage: String? = null,
    val weather: WeatherSnapshot? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

class WeatherViewModel(
    application: Application,
    private val repository: WeatherRepository,
    private val favoriteStore: FavoriteLocationStore,
) : AndroidViewModel(application) {
    constructor(application: Application) : this(
        application = application,
        repository = OpenMeteoWeatherRepository(),
        favoriteStore = FavoriteLocationStore(application),
    )

    private val initialFavorites = favoriteStore.load()
    private val _uiState = MutableStateFlow(
        WeatherUiState(
            selectedLocation = initialFavorites.firstOrNull() ?: TaiwanLocations.first(),
            favoriteLocations = initialFavorites,
        )
    )
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var requestJob: Job? = null
    private var searchJob: Job? = null

    init {
        loadWeather(showFullLoading = true)
    }

    fun selectLocation(location: WeatherLocation) {
        clearSearch()
        if (location.matches(_uiState.value.selectedLocation)) return
        _uiState.value = _uiState.value.copy(
            selectedLocation = location,
            weather = null,
            errorMessage = null,
        )
        loadWeather(showFullLoading = true)
    }

    fun updateSearchQuery(query: String) {
        searchJob?.cancel()
        val normalized = query.trim()
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            searchResults = emptyList(),
            isSearching = normalized.length >= 2,
            searchMessage = if (normalized.isNotEmpty() && normalized.length < 2) {
                "請輸入至少兩個字"
            } else {
                null
            },
        )
        if (normalized.length < 2) return

        searchJob = viewModelScope.launch {
            delay(350)
            try {
                val results = repository.searchLocations(normalized)
                _uiState.value = _uiState.value.copy(
                    searchResults = results,
                    isSearching = false,
                    searchMessage = if (results.isEmpty()) "找不到符合的城市" else null,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchMessage = error.localizedMessage ?: "城市搜尋暫時無法使用",
                )
            }
        }
    }

    fun toggleFavorite(location: WeatherLocation) {
        val favorites = _uiState.value.favoriteLocations
        val existing = favorites.firstOrNull { it.matches(location) }
        val updated = if (existing != null) {
            favorites - existing
        } else {
            if (favorites.size >= MAX_FAVORITES) {
                _uiState.value = _uiState.value.copy(
                    searchMessage = "最多可收藏 $MAX_FAVORITES 個城市",
                )
                return
            }
            favorites + location
        }

        favoriteStore.save(updated)
        _uiState.value = _uiState.value.copy(
            favoriteLocations = updated,
            searchMessage = null,
        )
    }

    fun refresh() {
        loadWeather(showFullLoading = _uiState.value.weather == null)
    }

    private fun clearSearch() {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            searchResults = emptyList(),
            isSearching = false,
            searchMessage = null,
        )
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

    private fun WeatherLocation.matches(other: WeatherLocation): Boolean =
        abs(latitude - other.latitude) < 0.01 &&
            abs(longitude - other.longitude) < 0.01

    private companion object {
        const val MAX_FAVORITES = 12
    }
}
