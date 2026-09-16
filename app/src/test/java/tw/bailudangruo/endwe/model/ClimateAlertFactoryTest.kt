package tw.bailudangruo.endwe.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClimateAlertFactoryTest {
    @Test
    fun severeConditionsCreateExpectedAlerts() {
        val current = CurrentWeather(
            temperature = 37.0,
            apparentTemperature = 41.0,
            humidity = 72,
            precipitation = 0.0,
            windSpeed = 12.0,
            weatherCode = 0,
            isDay = true,
            observedAt = "2026-09-15T14:00",
        )
        val today = DailyForecast(
            date = "2026-09-15",
            weatherCode = 95,
            maximumTemperature = 38.0,
            minimumTemperature = 29.0,
            precipitationProbability = 85,
            uvIndex = 11.2,
            maximumWindSpeed = 45.0,
        )

        val alerts = ClimateAlertFactory.create(current, today)

        assertEquals(4, alerts.size)
        assertTrue(alerts.any { it.title == "高溫警示" && it.level == AlertLevel.Danger })
        assertTrue(alerts.any { it.title == "紫外線偏高" && it.level == AlertLevel.Danger })
        assertTrue(alerts.any { it.title == "降雨機率高" })
        assertTrue(alerts.any { it.title == "強風提醒" })
    }

    @Test
    fun mildConditionsHaveNoAlerts() {
        val current = CurrentWeather(
            temperature = 27.0,
            apparentTemperature = 28.0,
            humidity = 60,
            precipitation = 0.0,
            windSpeed = 8.0,
            weatherCode = 1,
            isDay = true,
            observedAt = "2026-09-15T09:00",
        )
        val today = DailyForecast(
            date = "2026-09-15",
            weatherCode = 1,
            maximumTemperature = 30.0,
            minimumTemperature = 24.0,
            precipitationProbability = 20,
            uvIndex = 5.0,
            maximumWindSpeed = 18.0,
        )

        assertTrue(ClimateAlertFactory.create(current, today).isEmpty())
    }
}
