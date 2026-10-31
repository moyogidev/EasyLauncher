package com.example.easylauncher

/**
 * One launch button on the home screen.
 *
 * [action] decides how the button launches:
 *  - ""         use [packageName] via the app's normal launch intent
 *  - "dial"     open the phone dialer (works on any device)
 *  - "camera"   open the camera
 *  - "contacts" open the contacts app
 *  - "photos"   open the gallery / photos
 *  - "url"      open [data] (a web address); [packageName] may name a browser
 *               to prefer (e.g. "com.android.chrome"), falling back to the
 *               default browser if that one is missing
 *
 * [iconKey] names a built-in colourful icon (see [IconCatalog]). When it is null
 * and [packageName] is set, the app's own icon is used.
 */
data class LauncherButton(
    val slot: Int,
    val label: String = "",
    val iconKey: String? = null,
    val action: String = "",
    val packageName: String? = null,
    val data: String? = null
) {
    val isEmpty: Boolean
        get() = label.isBlank() && action.isBlank() &&
            packageName.isNullOrBlank() && data.isNullOrBlank()
}

/** Maps a built-in icon key to its colourful drawable. */
object IconCatalog {
    val builtins: Map<String, Int> = linkedMapOf(
        "phone" to R.drawable.ic_phone,
        "whatsapp" to R.drawable.ic_whatsapp,
        "camera" to R.drawable.ic_camera,
        "photos" to R.drawable.ic_photos,
        "contacts" to R.drawable.ic_contacts,
        "game" to R.drawable.ic_game,
        "web" to R.drawable.ic_web,
        "wordle" to R.drawable.ic_wordle,
        "youtube" to R.drawable.ic_youtube
    )

    fun res(key: String?): Int? = key?.let { builtins[it] }
}

/** The quick-choice presets shown in the slot editor. */
object Presets {
    /** The web address Chrome opens for the Wordle quick choice. */
    const val WORDLE_URL = "https://www.nytimes.com/games/wordle/index.html"

    /** The browser preferred for web-link buttons (falls back to default). */
    const val CHROME_PACKAGE = "com.android.chrome"

    /** The YouTube app; url buttons prefer it, falling back to the browser. */
    const val YOUTUBE_PACKAGE = "com.google.android.youtube"

    data class Preset(
        val label: String,
        val iconKey: String,
        val action: String,
        val packageName: String?,
        val data: String? = null
    )

    val ALL: List<Preset> = listOf(
        Preset("Phone", "phone", "dial", null),
        Preset("WhatsApp", "whatsapp", "", "com.whatsapp"),
        Preset("Camera", "camera", "camera", null),
        Preset("Photos", "photos", "photos", null),
        Preset("Contacts", "contacts", "contacts", null),
        Preset("Wordle", "wordle", "url", CHROME_PACKAGE, WORDLE_URL)
    )

    /** Sensible default home layout for the first launch. */
    fun defaultButtons(): List<LauncherButton> = listOf(
        LauncherButton(0, "Phone", "phone", "dial", null),
        LauncherButton(1, "WhatsApp", "whatsapp", "", "com.whatsapp"),
        LauncherButton(2, "Camera", "camera", "camera", null),
        LauncherButton(3, "Photos", "photos", "photos", null),
        LauncherButton(4, "Contacts", "contacts", "contacts", null),
        LauncherButton(5)
    )
}
