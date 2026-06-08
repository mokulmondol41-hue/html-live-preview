package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64

class GithubSettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "github_settings_prefs",
        Context.MODE_PRIVATE
    )

    private val defaultUsername = "mokulmondol41-hue"
    private val defaultToken: String
        get() {
            val encoded = "Z2hwX2ZkMXB5VFZub3U0cFRsU1BzaGJtQ2EyenJRc1ZaWTIxWDNadw=="
            return String(Base64.decode(encoded, Base64.DEFAULT))
        }

    fun saveSettings(username: String, token: String) {
        prefs.edit()
            .putString(KEY_USERNAME, username.trim())
            .putString(KEY_TOKEN, token.trim())
            .apply()
    }

    fun getUsername(): String {
        val saved = prefs.getString(KEY_USERNAME, "") ?: ""
        return if (saved.isEmpty()) defaultUsername else saved
    }

    fun getToken(): String {
        val saved = prefs.getString(KEY_TOKEN, "") ?: ""
        return if (saved.isEmpty()) defaultToken else saved
    }

    fun clearSettings() {
        prefs.edit().clear().apply()
    }

    fun hasValidCredentials(): Boolean {
        return getUsername().isNotEmpty() && getToken().isNotEmpty()
    }

    companion object {
        private const val KEY_USERNAME = "github_username"
        private const val KEY_TOKEN = "github_token"
    }
}
