package tw.bailudangruo.endwe.model

data class WeatherLocation(
    val name: String,
    val area: String,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String = "TW",
)

data class CurrentWeather(
    val temperature: Double,
    val apparentTemperature: Double,
    val humidity: Int,
    val precipitation: Double,
    val windSpeed: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val observedAt: String,
)

data class AirQuality(
    val usAqi: Int,
    val pm25: Double,
    val pm10: Double,
    val observedAt: String,
)

data class HourlyForecast(
    val time: String,
    val temperature: Double,
    val precipitationProbability: Int,
    val weatherCode: Int,
)

data class DailyForecast(
    val date: String,
    val weatherCode: Int,
    val maximumTemperature: Double,
    val minimumTemperature: Double,
    val precipitationProbability: Int,
    val uvIndex: Double,
    val maximumWindSpeed: Double,
    val precipitationSum: Double = 0.0,
    val maximumWindGust: Double = 0.0,
)

data class ClimateAlert(
    val title: String,
    val message: String,
    val level: AlertLevel,
    val source: String? = null,
)

enum class AlertLevel {
    Notice,
    Warning,
    Danger,
}

data class WeatherSnapshot(
    val location: WeatherLocation,
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
    val alerts: List<ClimateAlert>,
    val timezone: String,
    val airQuality: AirQuality? = null,
)

val TaiwanLocations = listOf(
    WeatherLocation("高雄", "南部", 22.6273, 120.3014),
    WeatherLocation("台南", "南部", 22.9999, 120.2269),
    WeatherLocation("屏東", "南部", 22.6628, 120.4916),
    WeatherLocation("台北", "北部", 25.0330, 121.5654),
)
