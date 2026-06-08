package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

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

    fun getUsername(): String {
        val saved = prefs.getString(KEY_USERNAME, "") ?: ""
        return if (saved.isEmpty()) BuildConfig.HOSTING_USERNAME else saved
    }

    fun getToken(): String {
        val saved = prefs.getString(KEY_TOKEN, "") ?: ""
        return if (saved.isEmpty()) BuildConfig.HOSTING_TOKEN else saved
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
