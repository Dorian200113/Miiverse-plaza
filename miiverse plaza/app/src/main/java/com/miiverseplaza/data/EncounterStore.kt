package com.miiverseplaza.data

import android.content.Context
import com.miiverseplaza.model.Encounter
import com.miiverseplaza.model.PlazaProfile
import org.json.JSONArray
import org.json.JSONObject

class EncounterStore(context: Context) {
    private val prefs = context.getSharedPreferences("plaza_encounters", Context.MODE_PRIVATE)

    fun loadEncounters(): List<Encounter> {
        val raw = prefs.getString(KEY_ENCOUNTERS, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val profile = item.getJSONObject("profile")
                    add(
                        Encounter(
                            profile = PlazaProfile(
                                deviceId = profile.getString("deviceId"),
                                nickname = profile.optString("nickname", "Unknown"),
                                favoriteGame = profile.optString("favoriteGame", "Unknown"),
                                statusMessage = profile.optString("statusMessage", ""),
                                platform = profile.optString("platform", "Unknown")
                            ),
                            timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                            hostAddress = item.optString("hostAddress", "unknown")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveEncounters(encounters: List<Encounter>) {
        val array = JSONArray()
        encounters.forEach { encounter ->
            array.put(
                JSONObject()
                    .put("timestamp", encounter.timestamp)
                    .put("hostAddress", encounter.hostAddress)
                    .put(
                        "profile",
                        JSONObject()
                            .put("deviceId", encounter.profile.deviceId)
                            .put("nickname", encounter.profile.nickname)
                            .put("favoriteGame", encounter.profile.favoriteGame)
                            .put("statusMessage", encounter.profile.statusMessage)
                            .put("platform", encounter.profile.platform)
                    )
            )
        }

        prefs.edit().putString(KEY_ENCOUNTERS, array.toString()).apply()
    }

    companion object {
        private const val KEY_ENCOUNTERS = "encounters"
    }
}
