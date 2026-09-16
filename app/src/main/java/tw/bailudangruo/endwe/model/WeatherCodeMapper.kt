package tw.bailudangruo.endwe.model

data class WeatherVisual(
    val description: String,
    val symbol: String,
)

object WeatherCodeMapper {
    fun visual(code: Int, isDay: Boolean = true): WeatherVisual = when (code) {
        0 -> WeatherVisual("晴朗", if (isDay) "☀️" else "🌙")
        1 -> WeatherVisual("大致晴朗", if (isDay) "🌤️" else "🌙")
        2 -> WeatherVisual("局部多雲", "⛅")
        3 -> WeatherVisual("陰天", "☁️")
        45, 48 -> WeatherVisual("有霧", "🌫️")
        51, 53, 55 -> WeatherVisual("毛毛雨", "🌦️")
        56, 57 -> WeatherVisual("凍雨", "🌧️")
        61, 63, 65 -> WeatherVisual("下雨", "🌧️")
        66, 67 -> WeatherVisual("凍雨", "🌧️")
        71, 73, 75, 77 -> WeatherVisual("降雪", "🌨️")
        80, 81, 82 -> WeatherVisual("陣雨", "🌦️")
        85, 86 -> WeatherVisual("陣雪", "🌨️")
        95 -> WeatherVisual("雷雨", "⛈️")
        96, 99 -> WeatherVisual("強雷雨", "⛈️")
        else -> WeatherVisual("天氣變化", "🌡️")
    }
}
