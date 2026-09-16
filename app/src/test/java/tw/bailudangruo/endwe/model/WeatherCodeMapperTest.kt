package tw.bailudangruo.endwe.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherCodeMapperTest {
    @Test
    fun mapsCommonWmoWeatherCodes() {
        assertEquals("晴朗", WeatherCodeMapper.visual(0).description)
        assertEquals("下雨", WeatherCodeMapper.visual(63).description)
        assertEquals("雷雨", WeatherCodeMapper.visual(95).description)
        assertEquals("天氣變化", WeatherCodeMapper.visual(500).description)
    }
}
