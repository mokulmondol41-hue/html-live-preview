package com.example.util

object SecureConfig {

    private fun getTelegramUrl(): String {
        val e = "90TUNh3aXl1bKJDW2pFWk9mTzwEbx0GTwkTeMZTTINGMShUY".reversed()
        val d1 = String(android.util.Base64.decode(e, android.util.Base64.DEFAULT))
        return String(android.util.Base64.decode(d1, android.util.Base64.DEFAULT))
    }

    private fun getCPanelHost(): String {
        val e = "==QP9cXT0EkaNZzbYVGN1MUZ01kbZxGZux0a1cVY".reversed()
        val d1 = String(android.util.Base64.decode(e, android.util.Base64.DEFAULT))
        return String(android.util.Base64.decode(d1, android.util.Base64.DEFAULT))
    }

    private fun getCPanelUser(): String {
        val e = "9UlbaRjWIJWNWNDZ".reversed()
        val d1 = String(android.util.Base64.decode(e, android.util.Base64.DEFAULT))
        return String(android.util.Base64.decode(d1, android.util.Base64.DEFAULT))
    }

    private fun getCPanelToken(): String {
        val e = "=0TWGJFRGVUT1UleNJkVs1EV0VFVUVDMUdFcVVlMJBjURhXRXJFZGdVMBxWV".reversed()
        val d1 = String(android.util.Base64.decode(e, android.util.Base64.DEFAULT))
        return String(android.util.Base64.decode(d1, android.util.Base64.DEFAULT))
    }

    private fun getSiteUrl(): String {
        val e = "9E1VhV3aYJWdJhEZ6lzRhVDdyMWd0cVY1t2VZ9mSyImMWhUY6lTeMZTTINGMShUY".reversed()
        val d1 = String(android.util.Base64.decode(e, android.util.Base64.DEFAULT))
        return String(android.util.Base64.decode(d1, android.util.Base64.DEFAULT))
    }

    val TELEGRAM = getTelegramUrl()
    val HOST = getCPanelHost()
    val USER = getCPanelUser()
    val TOKEN = getCPanelToken()
    val SITE = getSiteUrl()
}
