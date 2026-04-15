package com.miiverseplaza.net

import com.miiverseplaza.model.PlazaProfile
import com.miiverseplaza.model.Encounter
import org.json.JSONObject
import org.json.JSONArray

object PlazaProtocol {
    const val PORT = 43555
    private const val TYPE_BEACON = "beacon"
    private const val TYPE_SYSTEM_SYNC = "system_sync"

    fun encodeBeacon(profile: PlazaProfile): ByteArray {
        val payload = JSONObject()
            .put("type", TYPE_BEACON)
            .put("deviceId", profile.deviceId)
            .put("nickname", profile.nickname)
            .put("favoriteGame", profile.favoriteGame)
            .put("statusMessage", profile.statusMessage)
            .put("platform", profile.platform)

        return payload.toString().toByteArray(Charsets.UTF_8)
    }

    fun decodeProfile(json: String): PlazaProfile? {
        return runCatching {
            val obj = JSONObject(json)
            if (obj.optString("type") != TYPE_BEACON) {
                return null
            }

            PlazaProfile(
                deviceId = obj.getString("deviceId"),
                nickname = obj.optString("nickname", "Unknown"),
                favoriteGame = obj.optString("favoriteGame", "Unknown"),
                statusMessage = obj.optString("statusMessage", ""),
                platform = obj.optString("platform", "Unknown")
            )
        }.getOrNull()
    }

    fun encodeSystemSync(
        profile: PlazaProfile,
        targets: List<String>,
        encounters: List<Encounter>
    ): String {
        return JSONObject()
            .put("type", TYPE_SYSTEM_SYNC)
            .put("deviceId", profile.deviceId)
            .put("profile", JSONObject()
                .put("nickname", profile.nickname)
                .put("favoriteGame", profile.favoriteGame)
                .put("statusMessage", profile.statusMessage)
                .put("sourcePlatform", profile.platform))
            .put("targets", targets)
            .put("encounters", JSONArray().apply {
                encounters.forEach { encounter ->
                    put(
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
            })
            .put("instructions", JSONObject()
                .put("mode", "inject_into_system")
                .put("updatedBy", "Miiverse Plaza Android")
                .put("notes", "Homebrew bridge should persist the profile and encounter list into the local plaza save area."))
            .toString(2)
    }
}
