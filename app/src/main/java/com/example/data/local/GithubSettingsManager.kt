package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class GithubSettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "github_settings_prefs",
        Context.MODE_PRIVATE
    )

    fun saveSettings(username: String, token: String) {
        prefs.edit()
            .putString(KEY_USERNAME, username.trim())
            .putString(KEY_TOKEN, token.trim())
            .apply()
    }

    fun getUsername(): String = prefs.getString(KEY_USERNAME, "") ?: ""

    fun getToken(): String = prefs.getString(KEY_TOKEN, "") ?: ""

    fun clearSettings() {
        prefs.edit().clear().apply()
    }

    fun hasValidCredentials(): Boolean = true // FTP credentials are hardcoded in BuildConfig

    companion object {
        private const val KEY_USERNAME = "github_username"
        private const val KEY_TOKEN = "github_token"
    }
}
