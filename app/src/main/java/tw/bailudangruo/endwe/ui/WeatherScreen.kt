package tw.bailudangruo.endwe.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import tw.bailudangruo.endwe.model.AlertLevel
import tw.bailudangruo.endwe.model.ClimateAlert
import tw.bailudangruo.endwe.model.CurrentWeather
import tw.bailudangruo.endwe.model.DailyForecast
import tw.bailudangruo.endwe.model.HourlyForecast
import tw.bailudangruo.endwe.model.TaiwanLocations
import tw.bailudangruo.endwe.model.WeatherCodeMapper
import tw.bailudangruo.endwe.model.WeatherLocation
import tw.bailudangruo.endwe.model.WeatherSnapshot
import tw.bailudangruo.endwe.ui.theme.EndweTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WeatherApp(viewModel: WeatherViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    EndweTheme {
        WeatherScreen(
            uiState = uiState,
            onLocationSelected = viewModel::selectLocation,
            onRefresh = viewModel::refresh,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherScreen(
    uiState: WeatherUiState,
    onLocationSelected: (WeatherLocation) -> Unit,
    onRefresh: () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
        ) {
            Header(onRefresh)
            LocationSelector(
                selected = uiState.selectedLocation,
                onLocationSelected = onLocationSelected,
            )

            AnimatedContent(
                targetState = uiState.isLoading,
                label = "weather-loading",
            ) { isLoading ->
                when {
                    isLoading -> LoadingContent()
                    uiState.weather != null -> WeatherContent(uiState.weather)
                    else -> ErrorContent(
                        message = uiState.errorMessage ?: "目前無法取得天氣資料。",
                        onRetry = onRefresh,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "氣候監控",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "即時掌握天氣與風險",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Surface(
            onClick = onRefresh,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "↻", fontSize = 25.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun LocationSelector(
    selected: WeatherLocation,
    onLocationSelected: (WeatherLocation) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TaiwanLocations.forEach { location ->
            val isSelected = location == selected
            Surface(
                onClick = { onLocationSelected(location) },
                shape = RoundedCornerShape(22.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Text(
                    text = "${location.name} · ${location.area}",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "正在讀取最新天氣…",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "🌧️", fontSize = 54.sp)
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRetry) {
            Text("重新載入")
        }
    }
}

@Composable
private fun WeatherContent(weather: WeatherSnapshot) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        CurrentWeatherCard(weather)
        if (weather.alerts.isNotEmpty()) {
            SectionTitle("氣候提醒")
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                weather.alerts.forEach { AlertCard(it) }
            }
        }
        SectionTitle("未來 12 小時")
        HourlyForecastRow(weather.hourly)
        SectionTitle("7 日預報")
        DailyForecastList(weather.daily)
        Text(
            text = "資料來源：Open-Meteo · 更新 ${formatObservedTime(weather.current.observedAt)}",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun CurrentWeatherCard(weather: WeatherSnapshot) {
    val current = weather.current
    val visual = WeatherCodeMapper.visual(current.weatherCode, current.isDay)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1565C0), Color(0xFF00ACC1)),
                    )
                )
                .padding(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = weather.location.name,
                        color = Color.White.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${current.temperature.toInt()}°",
                        color = Color.White,
                        fontSize = 72.sp,
                        lineHeight = 80.sp,
                        fontWeight = FontWeight.Light,
                    )
                    Text(
                        text = "${visual.description} · 體感 ${current.apparentTemperature.toInt()}°",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(text = visual.symbol, fontSize = 64.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricCard("濕度", "${current.humidity}%", Modifier.weight(1f))
                MetricCard("降雨", "${formatOneDecimal(current.precipitation)} mm", Modifier.weight(1f))
                MetricCard("風速", "${current.windSpeed.toInt()} km/h", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun AlertCard(alert: ClimateAlert) {
    val accent = when (alert.level) {
        AlertLevel.Notice -> Color(0xFF61B7FF)
        AlertLevel.Warning -> Color(0xFFFFD166)
        AlertLevel.Danger -> Color(0xFFFF8A80)
    }
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = alert.title,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = alert.message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun HourlyForecastRow(hourly: List<HourlyForecast>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(hourly) { forecast ->
            val visual = WeatherCodeMapper.visual(forecast.weatherCode)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = formatHour(forecast.time),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = visual.symbol,
                        fontSize = 28.sp,
                        modifier = Modifier.padding(vertical = 7.dp),
                    )
                    Text(
                        text = "${forecast.temperature.toInt()}°",
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "💧 ${forecast.precipitationProbability}%",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyForecastList(daily: List<DailyForecast>) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        daily.forEachIndexed { index, forecast ->
            val visual = WeatherCodeMapper.visual(forecast.weatherCode)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatDay(forecast.date, index),
                        modifier = Modifier.width(72.dp),
                        fontWeight = FontWeight.Medium,
                    )
                    Text(text = visual.symbol, fontSize = 26.sp)
                    Text(
                        text = "${forecast.precipitationProbability}%",
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .weight(1f),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = "${forecast.minimumTemperature.toInt()}°",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = " / ${forecast.maximumTemperature.toInt()}°",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private fun formatHour(value: String): String = runCatching {
    LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("HH:mm"))
}.getOrDefault(value.takeLast(5))

private fun formatDay(value: String, index: Int): String {
    if (index == 0) return "今天"
    if (index == 1) return "明天"
    return runCatching {
        LocalDate.parse(value).format(DateTimeFormatter.ofPattern("M/d E", Locale.TAIWAN))
    }.getOrDefault(value)
}

private fun formatObservedTime(value: String): String = runCatching {
    LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("M/d HH:mm"))
}.getOrDefault(value.replace("T", " "))

private fun formatOneDecimal(value: Double): String =
    String.format(Locale.TAIWAN, "%.1f", value)
