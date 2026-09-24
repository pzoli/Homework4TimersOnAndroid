package hu.infokristaly.homework4timersonandroid.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("homework4timers_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_DID_INSERT_SAMPLES = "didInsertSamples"
        private const val KEY_ACTIVE_PRESET_ID = "activePresetID"
        private const val KEY_ACTIVE_PRESET_NAME = "activePresetName"
        private const val KEY_AUTO_CONTINUE = "autoContinueNextInterval"
        private const val KEY_APP_LANGUAGE = "appLanguage"
        private const val KEY_WORKSPACE_ITEMS = "workspaceItems"
        private const val KEY_SAVED_PRESETS = "savedPresets"
    }

    var didInsertSamples: Boolean
        get() = prefs.getBoolean(KEY_DID_INSERT_SAMPLES, false)
        set(value) = prefs.edit().putBoolean(KEY_DID_INSERT_SAMPLES, value).apply()

    var activePresetID: String
        get() = prefs.getString(KEY_ACTIVE_PRESET_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ACTIVE_PRESET_ID, value).apply()

    var activePresetName: String
        get() = prefs.getString(KEY_ACTIVE_PRESET_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ACTIVE_PRESET_NAME, value).apply()

    var autoContinueNextInterval: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONTINUE, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CONTINUE, value).apply()

    var appLanguage: String
        get() = prefs.getString(KEY_APP_LANGUAGE, "hu") ?: "hu"
        set(value) = prefs.edit().putString(KEY_APP_LANGUAGE, value).apply()

    fun getWorkspaceItems(): List<TimerIntervalItem> {
        val json = prefs.getString(KEY_WORKSPACE_ITEMS, null) ?: return emptyList()
        val type = object : TypeToken<List<TimerIntervalItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveWorkspaceItems(items: List<TimerIntervalItem>) {
        val json = gson.toJson(items)
        prefs.edit().putString(KEY_WORKSPACE_ITEMS, json).apply()
    }

    fun getSavedPresets(): List<SavedIntervalList> {
        val json = prefs.getString(KEY_SAVED_PRESETS, null) ?: return emptyList()
        val type = object : TypeToken<List<SavedIntervalList>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSavedPresets(presets: List<SavedIntervalList>) {
        val json = gson.toJson(presets)
        prefs.edit().putString(KEY_SAVED_PRESETS, json).apply()
    }
}
