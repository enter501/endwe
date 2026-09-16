package tw.bailudangruo.endwe.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import tw.bailudangruo.endwe.model.TaiwanLocations
import tw.bailudangruo.endwe.model.WeatherLocation

class FavoriteLocationStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "endwe_favorite_locations",
        Context.MODE_PRIVATE,
    )

    fun load(): List<WeatherLocation> {
        if (!preferences.getBoolean(KEY_INITIALIZED, false)) {
            save(TaiwanLocations)
            return TaiwanLocations
        }

        val raw = preferences.getString(KEY_LOCATIONS, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        WeatherLocation(
                            name = item.getString("name"),
                            area = item.optString("area"),
                            latitude = item.getDouble("latitude"),
                            longitude = item.getDouble("longitude"),
                            countryCode = item.optString("countryCode", "TW"),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(locations: List<WeatherLocation>) {
        val array = JSONArray()
        locations.forEach { location ->
            array.put(
                JSONObject()
                    .put("name", location.name)
                    .put("area", location.area)
                    .put("latitude", location.latitude)
                    .put("longitude", location.longitude)
                    .put("countryCode", location.countryCode)
            )
        }
        preferences.edit()
            .putBoolean(KEY_INITIALIZED, true)
            .putString(KEY_LOCATIONS, array.toString())
            .apply()
    }

    private companion object {
        const val KEY_INITIALIZED = "initialized"
        const val KEY_LOCATIONS = "locations"
    }
}
