package tw.bailudangruo.endwe.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import tw.bailudangruo.endwe.model.ClimateAlertFactory
import tw.bailudangruo.endwe.model.CurrentWeather
import tw.bailudangruo.endwe.model.DailyForecast
import tw.bailudangruo.endwe.model.HourlyForecast
import tw.bailudangruo.endwe.model.WeatherLocation
import tw.bailudangruo.endwe.model.WeatherSnapshot
import java.net.HttpURLConnection
import java.net.URL

interface WeatherRepository {
    suspend fun getWeather(location: WeatherLocation): WeatherSnapshot
}

class OpenMeteoWeatherRepository : WeatherRepository {
    override suspend fun getWeather(location: WeatherLocation): WeatherSnapshot =
        withContext(Dispatchers.IO) {
            val endpoint = buildString {
                append("https://api.open-meteo.com/v1/forecast")
                append("?latitude=${location.latitude}")
                append("&longitude=${location.longitude}")
                append("&current=temperature_2m,relative_humidity_2m,apparent_temperature,")
                append("is_day,precipitation,weather_code,wind_speed_10m")
                append("&hourly=temperature_2m,precipitation_probability,weather_code")
                append("&daily=weather_code,temperature_2m_max,temperature_2m_min,")
                append("uv_index_max,precipitation_probability_max,wind_speed_10m_max")
                append("&timezone=auto&forecast_days=7&forecast_hours=24")
            }

            val connection = URL(endpoint).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent", "Endwe/1.0")

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw IllegalStateException("天氣服務暫時無法使用（HTTP ${responseCode}）")
                }

                val body = connection.inputStream.bufferedReader().use { it.readText() }
                parseWeather(JSONObject(body), location)
            } finally {
                connection.disconnect()
            }
        }

    internal fun parseWeather(root: JSONObject, location: WeatherLocation): WeatherSnapshot {
        val currentJson = root.getJSONObject("current")
        val current = CurrentWeather(
            temperature = currentJson.getDouble("temperature_2m"),
            apparentTemperature = currentJson.getDouble("apparent_temperature"),
            humidity = currentJson.getInt("relative_humidity_2m"),
            precipitation = currentJson.getDouble("precipitation"),
            windSpeed = currentJson.getDouble("wind_speed_10m"),
            weatherCode = currentJson.getInt("weather_code"),
            isDay = currentJson.getInt("is_day") == 1,
            observedAt = currentJson.getString("time"),
        )

        val hourlyJson = root.getJSONObject("hourly")
        val hourlyTimes = hourlyJson.getJSONArray("time")
        val hourlyTemperatures = hourlyJson.getJSONArray("temperature_2m")
        val hourlyRain = hourlyJson.getJSONArray("precipitation_probability")
        val hourlyCodes = hourlyJson.getJSONArray("weather_code")
        val hourlyCount = minOf(
            12,
            hourlyTimes.length(),
            hourlyTemperatures.length(),
            hourlyRain.length(),
            hourlyCodes.length(),
        )
        val hourly = (0 until hourlyCount).map { index ->
            HourlyForecast(
                time = hourlyTimes.getString(index),
                temperature = hourlyTemperatures.getDouble(index),
                precipitationProbability = hourlyRain.intOrZero(index),
                weatherCode = hourlyCodes.getInt(index),
            )
        }

        val dailyJson = root.getJSONObject("daily")
        val dailyDates = dailyJson.getJSONArray("time")
        val dailyCodes = dailyJson.getJSONArray("weather_code")
        val dailyMaximums = dailyJson.getJSONArray("temperature_2m_max")
        val dailyMinimums = dailyJson.getJSONArray("temperature_2m_min")
        val dailyRain = dailyJson.getJSONArray("precipitation_probability_max")
        val dailyUv = dailyJson.getJSONArray("uv_index_max")
        val dailyWind = dailyJson.getJSONArray("wind_speed_10m_max")
        val dailyCount = minOf(
            7,
            dailyDates.length(),
            dailyCodes.length(),
            dailyMaximums.length(),
            dailyMinimums.length(),
            dailyRain.length(),
            dailyUv.length(),
            dailyWind.length(),
        )
        val daily = (0 until dailyCount).map { index ->
            DailyForecast(
                date = dailyDates.getString(index),
                weatherCode = dailyCodes.getInt(index),
                maximumTemperature = dailyMaximums.getDouble(index),
                minimumTemperature = dailyMinimums.getDouble(index),
                precipitationProbability = dailyRain.intOrZero(index),
                uvIndex = dailyUv.doubleOrZero(index),
                maximumWindSpeed = dailyWind.doubleOrZero(index),
            )
        }

        return WeatherSnapshot(
            location = location,
            current = current,
            hourly = hourly,
            daily = daily,
            alerts = ClimateAlertFactory.create(current, daily.firstOrNull()),
            timezone = root.optString("timezone", "Asia/Taipei"),
        )
    }

    private fun JSONArray.intOrZero(index: Int): Int =
        if (isNull(index)) 0 else optInt(index, 0)

    private fun JSONArray.doubleOrZero(index: Int): Double =
        if (isNull(index)) 0.0 else optDouble(index, 0.0)
}
