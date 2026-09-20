package com.example

import android.content.Context
import android.content.SharedPreferences

object SupabaseConfig {
    private const val PREFS_NAME = "supabase_config_prefs"
    private const val KEY_CUSTOM_URL = "custom_supabase_url"
    private const val KEY_CUSTOM_ANON_KEY = "custom_supabase_anon_key"

    @Volatile
    private var cachedUrl: String? = null

    @Volatile
    private var cachedAnonKey: String? = null

    val supabaseUrl: String
        get() = getSupabaseUrl(null)

    val supabaseAnonKey: String
        get() = getSupabaseAnonKey(null)

    fun getSupabaseUrl(context: Context? = null): String {
        cachedUrl?.let { if (it.isNotBlank()) return it }

        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val custom = prefs.getString(KEY_CUSTOM_URL, null)
            if (!custom.isNullOrBlank()) {
                cachedUrl = custom
                return custom
            }
        }

        // Read from BuildConfig (injected via Secrets Gradle plugin from .env / .env.example)
        val buildConfigUrl = try {
            BuildConfig.SUPABASE_URL
        } catch (_: Throwable) {
            ""
        }

        val resolved = if (!buildConfigUrl.isNullOrBlank() && !buildConfigUrl.contains("your-project")) {
            buildConfigUrl
        } else {
            "https://your-project.supabase.co"
        }

        cachedUrl = resolved
        return resolved
    }

    fun getSupabaseAnonKey(context: Context? = null): String {
        cachedAnonKey?.let { if (it.isNotBlank()) return it }

        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val custom = prefs.getString(KEY_CUSTOM_ANON_KEY, null)
            if (!custom.isNullOrBlank()) {
                cachedAnonKey = custom
                return custom
            }
        }

        // Read from BuildConfig
        val buildConfigKey = try {
            BuildConfig.SUPABASE_ANON_KEY
        } catch (_: Throwable) {
            ""
        }

        val resolved = if (!buildConfigKey.isNullOrBlank() && !buildConfigKey.contains("placeholder")) {
            buildConfigKey
        } else {
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.anon-key-placeholder"
        }

        cachedAnonKey = resolved
        return resolved
    }

    fun updateConfig(context: Context, url: String, anonKey: String) {
        val sanitizedUrl = url.trim().removeSuffix("/")
        val sanitizedKey = anonKey.trim()

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_CUSTOM_URL, sanitizedUrl)
            .putString(KEY_CUSTOM_ANON_KEY, sanitizedKey)
            .apply()

        cachedUrl = sanitizedUrl
        cachedAnonKey = sanitizedKey
    }

    fun resetToDefaults(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        cachedUrl = null
        cachedAnonKey = null
    }

    fun isConfigured(context: Context? = null): Boolean {
        val url = getSupabaseUrl(context)
        val key = getSupabaseAnonKey(context)
        return url.isNotBlank() && !url.contains("your-project") && key.isNotBlank() && !key.contains("placeholder")
    }
}
