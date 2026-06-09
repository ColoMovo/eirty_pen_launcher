package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LauncherRepository(private val dao: LauncherDao) {
    val settings: Flow<LauncherSettings> = dao.getSettingsFlow().map { it ?: LauncherSettings() }
    val notifications: Flow<List<NotificationItem>> = dao.getAllNotificationsFlow()

    suspend fun getSettings(): LauncherSettings {
        return dao.getSettings() ?: LauncherSettings().also {
            dao.insertOrUpdateSettings(it)
        }
    }

    suspend fun updateSettings(settings: LauncherSettings) {
        dao.insertOrUpdateSettings(settings)
    }

    suspend fun addNotification(notification: NotificationItem) {
        dao.insertNotification(notification)
    }

    suspend fun deleteNotification(id: Int) {
        dao.deleteNotificationById(id)
    }

    suspend fun clearAllNotifications() {
        dao.deleteAllNotifications()
    }
}
