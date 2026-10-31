package com.example.easylauncher

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Watches active notifications and records which packages currently have a
 * clearable notification (a new message, a missed call, etc.). The home screen
 * reads this to show a red dot on the matching button.
 *
 * Requires the user to grant "Notification access" once
 * (Settings → Notifications → Notification access), offered from the launcher's
 * settings screen.
 */
class NotificationBadgeService : NotificationListenerService() {

    override fun onListenerConnected() {
        refresh()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refresh()
    }

    private fun refresh() {
        val packages: Set<String> = try {
            activeNotifications
                ?.filter { it.isClearable }      // only "new/unread" style notifications
                ?.map { it.packageName }
                ?.toSet()
                ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
        ConfigStore(this).setNotifiedPackages(packages)
    }
}
