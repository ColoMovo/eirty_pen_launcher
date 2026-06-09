package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String = "",
    val isMock: Boolean = false,
    val mockIcon: ImageVector = Icons.Default.Apps,
    val iconColorHex: String = "#6200EE"
)

object AppLauncherHelper {
    // Generates a list of stylish built-in apps tailored for a Horizontal Dictionary Pen
    fun getMockDictionaryPenApps(): List<AppInfo> {
        return listOf(
            AppInfo("查词翻译", "com.dict.translator", "TranslatorActivity", true, Icons.Default.Translate, "#FF5722"),
            AppInfo("生词本", "com.dict.wordbook", "WordbookActivity", true, Icons.Default.Book, "#4CAF50"),
            AppInfo("扫读记录", "com.dict.scanner", "ScannerActivity", true, Icons.Default.CameraAlt, "#2196F3"),
            AppInfo("智能语音", "com.dict.ai", "AiAssistantActivity", true, Icons.Default.Psychology, "#9C27B0"),
            AppInfo("录音机", "com.dict.recorder", "RecorderActivity", true, Icons.Default.Mic, "#E91E63"),
            AppInfo("历史记录", "com.dict.history", "HistoryActivity", true, Icons.Default.History, "#607D8B"),
            AppInfo("计算器", "com.dict.calculator", "CalcActivity", true, Icons.Default.Calculate, "#FFC107"),
            AppInfo("单词打卡", "com.dict.wordchallenge", "ChallengeActivity", true, Icons.Default.CheckCircle, "#009688"),
            AppInfo("系统设置", "com.dict.settings", "SettingsActivity", true, Icons.Default.Settings, "#795548")
        )
    }

    // Merges real installed apps with dictionary pen native/mock apps
    fun loadAllApps(context: Context): List<AppInfo> {
        val appList = mutableListOf<AppInfo>()
        
        // 1. Load real launcher apps
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        try {
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            for (info in resolveInfos) {
                val label = info.loadLabel(pm).toString()
                val pkgName = info.activityInfo.packageName
                val actName = info.activityInfo.name
                
                // Skip our own launcher package to avoid launching recursive loop
                if (pkgName == context.packageName) continue
                
                appList.add(
                    AppInfo(
                        label = label,
                        packageName = pkgName,
                        activityName = actName,
                        isMock = false,
                        mockIcon = Icons.Default.Android,
                        iconColorHex = "#3DDC84"
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Add the specialized dictionary pen apps for visual fullness and launcher utility
        val mockApps = getMockDictionaryPenApps()
        for (mock in mockApps) {
            // Only add mock app if there isn't already a real package starting with the same app name
            if (appList.none { it.label == mock.label }) {
                appList.add(mock)
            }
        }

        return appList.sortedBy { it.label }
    }

    // Launches an app safely
    fun launchApp(context: Context, app: AppInfo, onLaunched: (String) -> Unit) {
        if (app.isMock) {
            // It is a mock system app for the pen, show launcher simulation
            onLaunched("模拟启动: ${app.label} [${app.packageName}]")
        } else {
            // It is a real installed app, start the intent
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                if (intent != null) {
                    context.startActivity(intent)
                    onLaunched("已启动: ${app.label}")
                } else {
                    onLaunched("启动失败: 找不到该程序的启动入口")
                }
            } catch (e: Exception) {
                onLaunched("启动出错: ${e.localizedMessage}")
            }
        }
    }
}
