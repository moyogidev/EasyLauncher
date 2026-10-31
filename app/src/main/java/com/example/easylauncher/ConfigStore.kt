package com.example.easylauncher

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Persists the launcher configuration (button count, button definitions and the
 * settings PIN) in SharedPreferences. Buttons are stored as a JSON array.
 */
class ConfigStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("easy_launcher", Context.MODE_PRIVATE)

    init {
        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            setCount(DEFAULT_COUNT)
            saveButtons(Presets.defaultButtons())
            prefs.edit().putBoolean(KEY_INITIALIZED, true).apply()
        }
    }

    // --- Button count ---------------------------------------------------------

    fun getCount(): Int = prefs.getInt(KEY_COUNT, DEFAULT_COUNT)

    fun setCount(count: Int) {
        val safe = if (count == 8) 8 else 6
        prefs.edit().putInt(KEY_COUNT, safe).apply()
    }

    // --- Buttons --------------------------------------------------------------

    /** Returns exactly [getCount] buttons, padding with empty slots as needed. */
    fun getButtons(): List<LauncherButton> {
        val stored = readButtons()
        val count = getCount()
        return (0 until count).map { slot ->
            stored.firstOrNull { it.slot == slot } ?: LauncherButton(slot)
        }
    }

    fun getButton(slot: Int): LauncherButton =
        readButtons().firstOrNull { it.slot == slot } ?: LauncherButton(slot)

    fun saveButton(button: LauncherButton) {
        val all = readButtons().filter { it.slot != button.slot }.toMutableList()
        all.add(button)
        saveButtons(all)
    }

    private fun saveButtons(buttons: List<LauncherButton>) {
        val array = JSONArray()
        buttons.forEach { b ->
            array.put(JSONObject().apply {
                put("slot", b.slot)
                put("label", b.label)
                put("iconKey", b.iconKey ?: JSONObject.NULL)
                put("action", b.action)
                put("packageName", b.packageName ?: JSONObject.NULL)
                put("data", b.data ?: JSONObject.NULL)
            })
        }
        prefs.edit().putString(KEY_BUTTONS, array.toString()).apply()
    }

    private fun readButtons(): List<LauncherButton> {
        val raw = prefs.getString(KEY_BUTTONS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                LauncherButton(
                    slot = o.getInt("slot"),
                    label = o.optString("label", ""),
                    iconKey = o.optStringOrNull("iconKey"),
                    action = o.optString("action", ""),
                    packageName = o.optStringOrNull("packageName"),
                    data = o.optStringOrNull("data")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- PIN ------------------------------------------------------------------

    fun hasPin(): Boolean = !prefs.getString(KEY_PIN, null).isNullOrEmpty()

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN, hash(pin)).apply()
    }

    fun checkPin(pin: String): Boolean =
        prefs.getString(KEY_PIN, null) == hash(pin)

    // --- Notification badges --------------------------------------------------

    /** Packages that currently have an active (clearable) notification. */
    fun getNotifiedPackages(): Set<String> =
        prefs.getStringSet(KEY_NOTIFIED_PACKAGES, emptySet()) ?: emptySet()

    /** Stores the set, but only if it changed (avoids needless UI refreshes). */
    fun setNotifiedPackages(packages: Set<String>) {
        if (getNotifiedPackages() == packages) return
        prefs.edit().putStringSet(KEY_NOTIFIED_PACKAGES, packages).apply()
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun hash(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_INITIALIZED = "initialized"
        private const val KEY_COUNT = "count"
        private const val KEY_BUTTONS = "buttons"
        private const val KEY_PIN = "pin"
        private const val DEFAULT_COUNT = 6
        const val KEY_NOTIFIED_PACKAGES = "notif_packages"
    }
}

private fun JSONObject.optStringOrNull(name: String): String? {
    if (isNull(name)) return null
    val s = optString(name, "")
    return s.ifEmpty { null }
}
