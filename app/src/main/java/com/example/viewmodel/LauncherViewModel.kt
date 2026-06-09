package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class LauncherState {
    APP_DRAWER,      // Card 2 on Left (50%), Card 1 on Right (50%)
    NORMAL,          // Card 1 Full Screen
    CONTROL_CENTER  // Card 1 on Left (50%), Card 3 on Right (50%)
}

data class MusicTrack(
    val title: String,
    val artist: String,
    val durationSeconds: Int = 240
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    // Database Initialization
    private val database: LauncherDatabase by lazy {
        Room.databaseBuilder(
            application,
            LauncherDatabase::class.java,
            "launcher_database"
        ).fallbackToDestructiveMigration().build()
    }

    private val repository: LauncherRepository by lazy {
        LauncherRepository(database.launcherDao())
    }

    // App State
    private val _launcherState = MutableStateFlow(LauncherState.NORMAL)
    val launcherState: StateFlow<LauncherState> = _launcherState.asStateFlow()

    // Loaded settings
    val launcherSettings: StateFlow<LauncherSettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LauncherSettings()
        )

    // Loaded notifications
    val notifications: StateFlow<List<NotificationItem>> = repository.notifications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Applications list
    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    
    val searchQuery = MutableStateFlow("")
    
    val filteredApps: StateFlow<List<AppInfo>> = combine(_allApps, searchQuery) { apps, query ->
        if (query.isBlank()) {
            apps
        } else {
            apps.filter { it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Music Player State & Simulator Loop
    val musicTracks = listOf(
        MusicTrack("夜空中最亮的星", "逃跑计划", 252),
        MusicTrack("起风了", "买辣椒也用券", 280),
        MusicTrack("晴天", "周杰伦", 269),
        MusicTrack("平凡之路", "朴树", 301),
        MusicTrack("稻香", "周杰伦", 283),
        MusicTrack("消愁", "毛不易", 258)
    )
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrackIndex = MutableStateFlow(0)
    val currentTrack = _currentTrackIndex.map { musicTracks[it] }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = musicTracks[0]
    )

    private val _musicProgress = MutableStateFlow(0.35f) // progress percentage 0.0 - 1.0f
    val musicProgress: StateFlow<Float> = _musicProgress.asStateFlow()

    private var musicJob: Job? = null

    // System Utilities (Audio settings)
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        // Prepare initial settings
        viewModelScope.launch(Dispatchers.IO) {
            repository.getSettings() // Forces default instantiation on db first-launch
            loadApplications()
            preloadDemoNotificationsIfNeeded()
        }
        
        // Setup Music playback progress update coroutine
        observePlaybackState()
    }

    private fun observePlaybackState() {
        viewModelScope.launch {
            isPlaying.collect { playing ->
                musicJob?.cancel()
                if (playing) {
                    musicJob = viewModelScope.launch {
                        while (true) {
                            delay(1000)
                            val currentProg = _musicProgress.value
                            val increment = 1.0f / (currentTrack.value.durationSeconds)
                            var nextProg = currentProg + increment
                            if (nextProg >= 1.0f) {
                                nextProg = 0f
                                nextTrack()
                            } else {
                                _musicProgress.value = nextProg
                            }
                            // Sync current playback state details to settings temporarily so the widget displays properly
                            val currentS = repository.getSettings()
                            repository.updateSettings(
                                currentS.copy(
                                    fakeNowPlayingTitle = currentTrack.value.title,
                                    fakeNowPlayingArtist = currentTrack.value.artist,
                                    nowPlayingProgress = _musicProgress.value
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Load installed applications list
    fun loadApplications() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = AppLauncherHelper.loadAllApps(getApplication())
            _allApps.value = apps
        }
    }

    private suspend fun preloadDemoNotificationsIfNeeded() {
        val settings = repository.getSettings()
        // If notifications are empty, load a few helpful ones
        val currentList = database.launcherDao().getSettingsFlow().first()
        // Wait, check first
        viewModelScope.launch {
            repository.notifications.take(1).collect { list ->
                if (list.isEmpty()) {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val curTime = sdf.format(Date())
                    
                    repository.addNotification(
                        NotificationItem(
                            title = "背单词提醒",
                            content = "今日尚有 35 个词汇需要进行词典笔复习打卡，请点击开启生词本进行学习。",
                            time = curTime,
                            appName = "生词馆",
                            category = "task"
                        )
                    )
                    repository.addNotification(
                        NotificationItem(
                            title = "词典笔提示",
                            content = "您的设备已成功安装「词典笔极简滑动启动器」。左右滑动可浏览应用列表和控制面板。",
                            time = curTime,
                            appName = "系统桌面",
                            category = "system"
                        )
                    )
                    repository.addNotification(
                        NotificationItem(
                            title = "天气预警",
                            content = "北京市今日天气转晴，当前气温24℃，夜间有微风，适宜出行。",
                            time = curTime,
                            appName = "天气服务",
                            category = "weather"
                        )
                    )
                }
            }
        }
    }

    // Set Launcher Level 1 Screen Position
    fun setLauncherState(state: LauncherState) {
        viewModelScope.launch {
            _launcherState.value = state
        }
    }

    // Modify specific launcher settings
    fun updateBackgroundTheme(themeName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettings()
            repository.updateSettings(current.copy(backgroundTheme = themeName))
        }
    }

    fun updateClockStyle(style: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettings()
            repository.updateSettings(current.copy(clockStyle = style))
        }
    }

    fun updateTemperatureUnit(unit: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettings()
            repository.updateSettings(current.copy(tempUnit = unit))
        }
    }

    fun updateWeatherCity(city: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettings()
            var fakeTemp = current.weatherTemp
            var fakeCond = current.weatherCondition
            when (city) {
                "北京市" -> { fakeTemp = 24; fakeCond = "多云转晴" }
                "上海市" -> { fakeTemp = 26; fakeCond = "晴天" }
                "广州市" -> { fakeTemp = 29; fakeCond = "雷阵雨" }
                "深圳市" -> { fakeTemp = 28; fakeCond = "多云" }
                "成都市" -> { fakeTemp = 22; fakeCond = "阴天" }
                "杭州市" -> { fakeTemp = 25; fakeCond = "小雨" }
                else -> { fakeTemp = (18..32).random(); fakeCond = listOf("晴天", "多云", "阴天", "小雨").random() }
            }
            repository.updateSettings(current.copy(weatherCity = city, weatherTemp = fakeTemp, weatherCondition = fakeCond))
            
            // Post notification about city change
            addNotification(
                "天气追踪更新",
                "已成功将天气定位切换至「$city」，当前温度 : ${fakeTemp}${current.tempUnit} ($fakeCond)。",
                "weather"
            )
        }
    }

    fun updateAppDrawerColumns(columns: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettings()
            repository.updateSettings(current.copy(appDrawerColumns = columns))
        }
    }

    fun updateAppLayout(isList: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettings()
            repository.updateSettings(current.copy(appDrawerListLayout = isList))
        }
    }

    // Toggle Wifi
    fun toggleWifi() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettings()
            val nextState = !current.wifiEnabled
            repository.updateSettings(current.copy(wifiEnabled = nextState))
            
            addNotification(
                title = "网络状态变动",
                content = if (nextState) "Wi-Fi 已开启，正在搜索并连接已知网络..." else "Wi-Fi 已关闭，本地无线数据传输已停用。",
                category = "system"
            )
        }
    }

    // Toggle Bluetooth
    fun toggleBluetooth() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettings()
            val nextState = !current.bluetoothEnabled
            repository.updateSettings(current.copy(bluetoothEnabled = nextState))
            
            addNotification(
                title = "蓝牙状态变动",
                content = if (nextState) "蓝牙服务已开启，词典笔正在扫描无线耳机等外设..." else "蓝牙已关闭，已断开所有蓝牙外设连接。",
                category = "system"
            )
        }
    }

    // Toggle Ring Mode
    fun toggleRingMode() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettings()
            val modes = listOf("铃声", "振动", "静音")
            val nextIndex = (modes.indexOf(current.ringMode) + 1) % modes.size
            val nextMode = modes[nextIndex]
            
            // Try to set real audio manager modes
            try {
                when (nextMode) {
                    "铃声" -> audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    "振动" -> audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    "静音" -> audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                }
            } catch (e: Exception) {
                // Ignore background permission exceptions on some Android TV/API profiles
            }

            repository.updateSettings(current.copy(ringMode = nextMode))
            addNotification("声音模式调整", "系统铃声/声音配置已切换至: 「$nextMode」。", "system")
        }
    }

    // Volume level
    fun setVolumeLevel(level: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettings()
            val bounded = level.coerceIn(0, 100)
            repository.updateSettings(current.copy(volumeLevel = bounded))
            
            // Adjust real Android volume
            try {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = (bounded * maxVol) / 100
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Brightness level
    fun setBrightnessLevel(level: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettings()
            val bounded = level.coerceIn(0, 100)
            repository.updateSettings(current.copy(brightnessLevel = bounded))
            // Actual system brightness toggling is highly restricted (requires system permission write settings),
            // we will simulate it globally in visual UI overlay brightness lowering! Highly responsive.
        }
    }

    // Music control methods
    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun playTrack(index: Int) {
        val nextIdx = index.coerceIn(0, musicTracks.size - 1)
        _currentTrackIndex.value = nextIdx
        _musicProgress.value = 0f
        _isPlaying.value = true
        
        viewModelScope.launch(Dispatchers.IO) {
            val settings = repository.getSettings()
            val track = musicTracks[nextIdx]
            repository.updateSettings(
                settings.copy(
                    fakeNowPlayingTitle = track.title,
                    fakeNowPlayingArtist = track.artist,
                    nowPlayingProgress = 0f
                )
            )
            addNotification("现在播放", "♬ 词典笔正在播放: ${track.title} - ${track.artist}", "music")
        }
    }

    fun nextTrack() {
        val nextIdx = (_currentTrackIndex.value + 1) % musicTracks.size
        playTrack(nextIdx)
    }

    fun prevTrack() {
        val prevIdx = if (_currentTrackIndex.value - 1 < 0) musicTracks.size - 1 else _currentTrackIndex.value - 1
        playTrack(prevIdx)
    }

    fun seekProgress(progress: Float) {
        _musicProgress.value = progress.coerceIn(0f, 1f)
    }

    // Notification operations
    fun addNotification(title: String, content: String, category: String = "system") {
        viewModelScope.launch(Dispatchers.IO) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val curTime = sdf.format(Date())
            repository.addNotification(
                NotificationItem(
                    title = title,
                    content = content,
                    time = curTime,
                    category = category,
                    appName = when (category) {
                        "music" -> "音乐播放器"
                        "weather" -> "天气广播"
                        "task" -> "日程生词"
                        else -> "系统桌面"
                    }
                )
            )
        }
    }

    fun removeNotification(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNotification(id)
        }
    }

    fun clearNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllNotifications()
            addNotification("消息列表", "已清空通知中心的所有消息通知。", "system")
        }
    }
}
