package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class GithubSettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "github_settings_prefs",
        Context.MODE_PRIVATE
    )

    // Default hardcoded credentials
    private val defaultUsername = "mokulmondol41-hue"
    private val defaultToken = "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"

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
