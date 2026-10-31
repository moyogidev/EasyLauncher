package com.example.easylauncher

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.ContextCompat

/** Launches the app/intent represented by a [LauncherButton]. */
object AppLauncher {

    fun launch(activity: Activity, button: LauncherButton) {
        if (button.action == "url") {
            launchUrl(activity, button)
            return
        }

        val intent: Intent? = when (button.action) {
            "dial" -> Intent(Intent.ACTION_DIAL)
            "camera" -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            "contacts" -> Intent(Intent.ACTION_VIEW).setData(ContactsContract.Contacts.CONTENT_URI)
            "photos" -> Intent(Intent.ACTION_VIEW)
                .setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            else -> button.packageName?.let {
                activity.packageManager.getLaunchIntentForPackage(it)
            }
        }

        if (intent == null) {
            Toast.makeText(activity, R.string.not_installed, Toast.LENGTH_SHORT).show()
            return
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            activity.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(activity, R.string.cannot_open, Toast.LENGTH_SHORT).show()
        }
    }

    /** Opens a web address, preferring [LauncherButton.packageName] (e.g. Chrome). */
    private fun launchUrl(activity: Activity, button: LauncherButton) {
        val url = button.data
        if (url.isNullOrBlank()) {
            Toast.makeText(activity, R.string.cannot_open, Toast.LENGTH_SHORT).show()
            return
        }
        val uri = Uri.parse(url)

        // First try the preferred browser (Chrome) if one was requested.
        val preferred = button.packageName
        if (!preferred.isNullOrEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, uri)
                .setPackage(preferred)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                activity.startActivity(intent)
                return
            } catch (e: Exception) {
                // Chrome not installed — fall through to the default browser.
            }
        }

        try {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Toast.makeText(activity, R.string.cannot_open, Toast.LENGTH_SHORT).show()
        }
    }

    /** Resolves the drawable to show for a button: built-in icon, else app icon. */
    fun iconFor(activity: Activity, button: LauncherButton): Drawable? {
        IconCatalog.res(button.iconKey)?.let {
            return ContextCompat.getDrawable(activity, it)
        }
        val pkg = button.packageName ?: return null
        return try {
            activity.packageManager.getApplicationIcon(pkg)
        } catch (e: Exception) {
            null
        }
    }
}
