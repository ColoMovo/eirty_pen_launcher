package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "launcher_settings")
data class LauncherSettings(
    @PrimaryKey val id: Int = 1,
    val backgroundTheme: String = "深邃星空", // 深邃星空, 森林绿意, 极简深色, 极客黑客, 晨曦微光
    val clockStyle: String = "大字数字", // 大字数字, 圆盘时钟, 极简文字, 商务排版
    val tempUnit: String = "℃", // ℃, ℉
    val showNowPlaying: Boolean = true,
    val weatherCity: String = "北京市",
    val weatherCondition: String = "多云转晴", // 晴, 多云, 阴, 雨, 雪
    val weatherTemp: Int = 24,
    val fakeNowPlayingTitle: String = "夜空中最亮的星",
    val fakeNowPlayingArtist: String = "逃跑计划",
    val nowPlayingProgress: Float = 0.45f,
    val wifiEnabled: Boolean = true,
    val bluetoothEnabled: Boolean = false,
    val volumeLevel: Int = 60,
    val brightnessLevel: Int = 80,
    val ringMode: String = "铃声", // 铃声, 振动, 静音
    val appDrawerColumns: Int = 4,
    val appDrawerListLayout: Boolean = false // false for Grid, true for List
)

@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val time: String,
    val appName: String = "系统提示",
    val category: String = "system" // system, music, weather, task, update
)

@Dao
interface LauncherDao {
    @Query("SELECT * FROM launcher_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<LauncherSettings?>

    @Query("SELECT * FROM launcher_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): LauncherSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: LauncherSettings)

    @Query("SELECT * FROM notifications ORDER BY id DESC")
    fun getAllNotificationsFlow(): Flow<List<NotificationItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationItem)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: Int)

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()
}

@Database(entities = [LauncherSettings::class, NotificationItem::class], version = 1, exportSchema = false)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun launcherDao(): LauncherDao
}
