package tw.bailudangruo.endwe.model

object ClimateAlertFactory {
    fun create(current: CurrentWeather, today: DailyForecast?): List<ClimateAlert> {
        val alerts = mutableListOf<ClimateAlert>()

        if (current.temperature >= 36.0) {
            alerts += ClimateAlert(
                title = "高溫警示",
                message = "目前 ${current.temperature.toInt()}°C，外出請補充水分並避免長時間曝曬。",
                level = AlertLevel.Danger,
                source = "Open-Meteo 預報",
            )
        } else if (current.temperature >= 33.0) {
            alerts += ClimateAlert(
                title = "高溫提醒",
                message = "目前體感 ${current.apparentTemperature.toInt()}°C，請留意熱傷害。",
                level = AlertLevel.Warning,
                source = "Open-Meteo 預報",
            )
        }

        today?.let {
            if (it.uvIndex >= 8.0) {
                alerts += ClimateAlert(
                    title = "紫外線偏高",
                    message = "今日 UV 指數最高 ${"%.1f".format(it.uvIndex)}，建議加強防曬。",
                    level = if (it.uvIndex >= 11.0) AlertLevel.Danger else AlertLevel.Warning,
                    source = "Open-Meteo 預報",
                )
            }

            when {
                current.precipitation >= 40.0 || it.precipitationSum >= 200.0 -> {
                    alerts += ClimateAlert(
                        title = "豪雨風險預警",
                        message = "預報雨勢已達豪雨風險門檻，請遠離低窪地區並留意官方警報。",
                        level = AlertLevel.Danger,
                        source = "Open-Meteo 預報",
                    )
                }

                it.precipitationSum >= 80.0 -> {
                    alerts += ClimateAlert(
                        title = "大雨風險提醒",
                        message = "今日預估累積雨量 ${it.precipitationSum.toInt()} mm，請留意積淹水。",
                        level = AlertLevel.Warning,
                        source = "Open-Meteo 預報",
                    )
                }

                it.precipitationProbability >= 70 -> {
                    alerts += ClimateAlert(
                        title = "降雨機率高",
                        message = "今日最高降雨機率 ${it.precipitationProbability}%，外出記得帶傘。",
                        level = AlertLevel.Notice,
                        source = "Open-Meteo 預報",
                    )
                }
            }

            val strongestWind = maxOf(it.maximumWindSpeed, it.maximumWindGust)
            if (strongestWind >= 118.0) {
                alerts += ClimateAlert(
                    title = "颱風等級強風風險",
                    message = "今日最大陣風可能達 ${strongestWind.toInt()} km/h，請確認防颱措施並留意官方颱風警報。",
                    level = AlertLevel.Danger,
                    source = "Open-Meteo 預報",
                )
            } else if (strongestWind >= 40.0) {
                alerts += ClimateAlert(
                    title = "強風提醒",
                    message = "今日最大風速可能達 ${strongestWind.toInt()} km/h。",
                    level = AlertLevel.Warning,
                    source = "Open-Meteo 預報",
                )
            }
        }

        return alerts
    }
}
