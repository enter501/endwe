package tw.bailudangruo.endwe.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import tw.bailudangruo.endwe.model.AirQuality
import tw.bailudangruo.endwe.model.AlertLevel
import tw.bailudangruo.endwe.model.ClimateAlert
import tw.bailudangruo.endwe.model.ClimateAlertFactory
import tw.bailudangruo.endwe.model.CurrentWeather
import tw.bailudangruo.endwe.model.DailyForecast
import tw.bailudangruo.endwe.model.HourlyForecast
import tw.bailudangruo.endwe.model.WeatherLocation
import tw.bailudangruo.endwe.model.WeatherSnapshot
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

interface WeatherRepository {
    suspend fun getWeather(location: WeatherLocation): WeatherSnapshot
    suspend fun searchLocations(query: String): List<WeatherLocation>
}

class OpenMeteoWeatherRepository : WeatherRepository {
    override suspend fun getWeather(location: WeatherLocation): WeatherSnapshot =
        withContext(Dispatchers.IO) {
            val weatherJson = getJson(weatherEndpoint(location), "天氣服務")
            val airQuality = runCatching {
                parseAirQuality(getJson(airQualityEndpoint(location), "空氣品質服務"))
            }.getOrNull()
            val officialAlerts = if (location.isInTaiwan()) {
                runCatching {
                    parseOfficialAlerts(
                        getText(NCDR_ALERT_ENDPOINT, "民生示警服務"),
                    )
                }.getOrDefault(emptyList())
            } else {
                emptyList()
            }

            parseWeather(
                root = weatherJson,
                location = location,
                airQuality = airQuality,
                officialAlerts = officialAlerts,
            )
        }

    override suspend fun searchLocations(query: String): List<WeatherLocation> =
        withContext(Dispatchers.IO) {
            val encoded = URLEncoder.encode(query.trim(), Charsets.UTF_8.name())
            val root = getJson(
                "https://geocoding-api.open-meteo.com/v1/search" +
                    "?name=$encoded&count=8&language=zh&format=json",
                "城市搜尋服務",
            )
            val results = root.optJSONArray("results") ?: return@withContext emptyList()
            buildList {
                for (index in 0 until results.length()) {
                    val item = results.optJSONObject(index) ?: continue
                    val name = item.optString("name").trim()
                    if (name.isEmpty()) continue
                    val areaParts = listOf(
                        item.optString("admin1"),
                        item.optString("admin2"),
                        item.optString("country"),
                    ).map(String::trim).filter(String::isNotEmpty).distinct()
                    add(
                        WeatherLocation(
                            name = name,
                            area = areaParts.joinToString(" · "),
                            latitude = item.getDouble("latitude"),
                            longitude = item.getDouble("longitude"),
                            countryCode = item.optString("country_code"),
                        )
                    )
                }
            }.distinctBy { "${it.name}:${it.latitude}:${it.longitude}" }
        }

    private fun weatherEndpoint(location: WeatherLocation): String = buildString {
        append("https://api.open-meteo.com/v1/forecast")
        append("?latitude=${location.latitude}")
        append("&longitude=${location.longitude}")
        append("&current=temperature_2m,relative_humidity_2m,apparent_temperature,")
        append("is_day,precipitation,weather_code,wind_speed_10m")
        append("&hourly=temperature_2m,precipitation_probability,weather_code")
        append("&daily=weather_code,temperature_2m_max,temperature_2m_min,")
        append("uv_index_max,precipitation_probability_max,wind_speed_10m_max,")
        append("precipitation_sum,wind_gusts_10m_max")
        append("&timezone=auto&forecast_days=7&forecast_hours=24")
    }

    private fun airQualityEndpoint(location: WeatherLocation): String = buildString {
        append("https://air-quality-api.open-meteo.com/v1/air-quality")
        append("?latitude=${location.latitude}")
        append("&longitude=${location.longitude}")
        append("&current=us_aqi,pm10,pm2_5")
        append("&timezone=auto")
    }

    private fun getJson(endpoint: String, serviceName: String): JSONObject =
        JSONObject(getText(endpoint, serviceName).trim().trimStart('\uFEFF'))

    private fun getText(endpoint: String, serviceName: String): String {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "Endwe/1.1")

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("$serviceName 暫時無法使用（HTTP $responseCode）")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    internal fun parseWeather(
        root: JSONObject,
        location: WeatherLocation,
        airQuality: AirQuality? = null,
        officialAlerts: List<ClimateAlert> = emptyList(),
    ): WeatherSnapshot {
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
        val dailyRainSum = dailyJson.optJSONArray("precipitation_sum")
        val dailyGust = dailyJson.optJSONArray("wind_gusts_10m_max")
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
                precipitationSum = dailyRainSum.doubleOrZero(index),
                maximumWindGust = dailyGust.doubleOrZero(index),
            )
        }

        val forecastAlerts = ClimateAlertFactory.create(current, daily.firstOrNull())
        return WeatherSnapshot(
            location = location,
            current = current,
            hourly = hourly,
            daily = daily,
            alerts = (officialAlerts + forecastAlerts).distinctBy { it.title },
            timezone = root.optString("timezone", "Asia/Taipei"),
            airQuality = airQuality,
        )
    }

    internal fun parseAirQuality(root: JSONObject): AirQuality {
        val current = root.getJSONObject("current")
        return AirQuality(
            usAqi = current.optDouble("us_aqi", 0.0).roundToInt(),
            pm25 = current.optDouble("pm2_5", 0.0),
            pm10 = current.optDouble("pm10", 0.0),
            observedAt = current.optString("time"),
        )
    }

    internal fun parseOfficialAlerts(body: String): List<ClimateAlert> {
        val root = JSONObject(body.trim().trimStart('\uFEFF'))
        val entries = mutableListOf<JSONObject>()
        collectAlertEntries(root, entries)

        return entries.mapNotNull { entry ->
            val title = jsonText(entry.opt("title")).trim()
            val summary = listOf(
                jsonText(entry.opt("summary")),
                jsonText(entry.opt("description")),
                jsonText(entry.opt("content")),
            ).firstOrNull { it.isNotBlank() }.orEmpty().trim()
            val searchable = "$title $summary".lowercase()
            if (ALERT_KEYWORDS.none(searchable::contains)) return@mapNotNull null

            val danger = DANGER_KEYWORDS.any(searchable::contains)
            ClimateAlert(
                title = if (title.isBlank()) "官方天氣警報" else title,
                message = summary.ifBlank { "請留意中央與地方政府最新防災資訊。" },
                level = if (danger) AlertLevel.Danger else AlertLevel.Warning,
                source = "NCDR 民生示警",
            )
        }.distinctBy { "${it.title}:${it.message}" }.take(6)
    }

    private fun collectAlertEntries(value: Any?, result: MutableList<JSONObject>) {
        when (value) {
            is JSONObject -> {
                if (value.has("title") && (
                        value.has("summary") ||
                            value.has("description") ||
                            value.has("content")
                        )
                ) {
                    result += value
                }
                val keys = value.keys()
                while (keys.hasNext()) {
                    collectAlertEntries(value.opt(keys.next()), result)
                }
            }

            is JSONArray -> {
                for (index in 0 until value.length()) {
                    collectAlertEntries(value.opt(index), result)
                }
            }
        }
    }

    private fun jsonText(value: Any?): String = when (value) {
        null, JSONObject.NULL -> ""
        is String -> value
        is JSONObject -> listOf("#text", "content", "value", "text")
            .firstNotNullOfOrNull { key ->
                value.opt(key)?.let(::jsonText)?.takeIf(String::isNotBlank)
            }.orEmpty()

        is JSONArray -> buildList {
            for (index in 0 until value.length()) {
                jsonText(value.opt(index)).takeIf(String::isNotBlank)?.let(::add)
            }
        }.joinToString(" ")

        else -> value.toString()
    }

    private fun WeatherLocation.isInTaiwan(): Boolean =
        countryCode.equals("TW", ignoreCase = true) ||
            (latitude in 21.5..26.5 && longitude in 118.0..123.5)

    private fun JSONArray.intOrZero(index: Int): Int =
        if (isNull(index)) 0 else optInt(index, 0)

    private fun JSONArray.doubleOrZero(index: Int): Double =
        if (isNull(index)) 0.0 else optDouble(index, 0.0)

    private fun JSONArray?.doubleOrZero(index: Int): Double =
        if (this == null || index >= length() || isNull(index)) 0.0 else optDouble(index, 0.0)

    private companion object {
        const val NCDR_ALERT_ENDPOINT = "https://alerts.ncdr.nat.gov.tw/JSONAtomFeed.ashx"

        val ALERT_KEYWORDS = listOf(
            "颱風",
            "豪雨",
            "大雨",
            "強風",
            "typhoon",
            "tropical cyclone",
            "heavy rain",
            "torrential rain",
        )

        val DANGER_KEYWORDS = listOf(
            "超大豪雨",
            "陸上颱風",
            "紅色",
            "extreme",
            "torrential rain",
        )
    }
}
