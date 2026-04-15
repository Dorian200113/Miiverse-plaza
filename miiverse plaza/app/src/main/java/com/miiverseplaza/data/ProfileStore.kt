package com.miiverseplaza.data

import android.content.Context
import com.miiverseplaza.model.PlazaProfile
import java.util.UUID

class ProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("plaza_profile", Context.MODE_PRIVATE)

    fun loadProfile(): PlazaProfile {
        val deviceId = prefs.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_DEVICE_ID, it).apply()
        }

        return PlazaProfile(
            deviceId = deviceId,
            nickname = prefs.getString(KEY_NICKNAME, "Plaza Visitor") ?: "Plaza Visitor",
            favoriteGame = prefs.getString(KEY_GAME, "Mario Kart DS") ?: "Mario Kart DS",
            statusMessage = prefs.getString(KEY_STATUS, "Op zoek naar een encounter!") ?: "Op zoek naar een encounter!",
            platform = prefs.getString(KEY_PLATFORM, "Android") ?: "Android"
        )
    }

    fun saveProfile(profile: PlazaProfile) {
        prefs.edit()
            .putString(KEY_DEVICE_ID, profile.deviceId)
            .putString(KEY_NICKNAME, profile.nickname)
            .putString(KEY_GAME, profile.favoriteGame)
            .putString(KEY_STATUS, profile.statusMessage)
            .putString(KEY_PLATFORM, profile.platform)
            .apply()
    }

    companion object {
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_GAME = "favorite_game"
        private const val KEY_STATUS = "status_message"
        private const val KEY_PLATFORM = "platform"
    }
}
