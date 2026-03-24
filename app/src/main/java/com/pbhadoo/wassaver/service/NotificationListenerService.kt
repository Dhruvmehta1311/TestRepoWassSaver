package com.pbhadoo.wassaver.service

import android.app.Notification
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.pbhadoo.wassaver.data.model.DeletedMessage
import com.pbhadoo.wassaver.utils.MessageStore

class NotificationListenerService : NotificationListenerService() {

    private val whatsappPackages = setOf("com.whatsapp", "com.whatsapp.w4b")

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName ?: return
        if (pkg !in whatsappPackages) return

        // Check if capture is enabled
        if (!MessageStore.isCaptureEnabled(applicationContext)) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

        // Skip group summary notifications
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        // Detect group messages - title format is usually "GroupName" and subtext has sender
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val isGroup = subText != null
        val senderName = if (isGroup) title else title
        val groupName = subText

        // Create unique sender ID
        val senderId = "${pkg}_${title.replace(" ", "_").lowercase()}"

        val message = DeletedMessage(
            id = System.currentTimeMillis(),
            senderId = senderId,
            senderName = senderName,
            text = text,
            timestamp = System.currentTimeMillis(),
            isGroup = isGroup,
            groupName = groupName,
            packageName = pkg
        )

        MessageStore.saveMessage(applicationContext, message)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // We don't need to do anything here — we already captured the message
    }
}
