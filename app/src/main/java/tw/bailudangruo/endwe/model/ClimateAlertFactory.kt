package tw.bailudangruo.endwe.model

object ClimateAlertFactory {
    fun create(current: CurrentWeather, today: DailyForecast?): List<ClimateAlert> {
        val alerts = mutableListOf<ClimateAlert>()

        if (current.temperature >= 36.0) {
            alerts += ClimateAlert(
                title = "高溫警示",
                message = "目前 ${current.temperature.toInt()}°C，外出請補充水分並避免長時間曝曬。",
                level = AlertLevel.Danger,
            )
        } else if (current.temperature >= 33.0) {
            alerts += ClimateAlert(
                title = "高溫提醒",
                message = "目前體感 ${current.apparentTemperature.toInt()}°C，請留意熱傷害。",
                level = AlertLevel.Warning,
            )
        }

        today?.let {
            if (it.uvIndex >= 8.0) {
                alerts += ClimateAlert(
                    title = "紫外線偏高",
                    message = "今日 UV 指數最高 ${"%.1f".format(it.uvIndex)}，建議加強防曬。",
                    level = if (it.uvIndex >= 11.0) AlertLevel.Danger else AlertLevel.Warning,
                )
            }
            if (it.precipitationProbability >= 70) {
                alerts += ClimateAlert(
                    title = "降雨機率高",
                    message = "今日最高降雨機率 ${it.precipitationProbability}%，外出記得帶傘。",
                    level = AlertLevel.Notice,
                )
            }
            if (it.maximumWindSpeed >= 40.0) {
                alerts += ClimateAlert(
                    title = "強風提醒",
                    message = "今日最大風速可能達 ${it.maximumWindSpeed.toInt()} km/h。",
                    level = AlertLevel.Warning,
                )
            }
        }

        return alerts
    }
}
