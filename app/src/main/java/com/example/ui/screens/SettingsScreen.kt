package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LauncherSettings
import com.example.viewmodel.LauncherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.launcherSettings.collectAsState()
    
    // Choose the background based on Settings background theme for consistent immersive look even in settings!
    val bgBrush = getBackgroundBrush(settings.backgroundTheme)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "宿主桌面设置 (第二层级)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "自定义横页词典笔专属桌面排版与特性",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    }
                }

                // Close Return Button
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Done, "完成", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("完成设置", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Scrollable Content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()) // Horizontal dictionary-pen friendly dual columns!
            ) {
                // Column 1: Appearance & Widgets Layout
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(360.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeader("一、个性化氛围 & 时钟")

                    // Background Theme option
                    SettingCard(title = "桌面背景氛围") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val themes = listOf("深邃星空", "森林绿意", "晨曦微光", "极客黑客", "极简深色")
                            themes.forEach { theme ->
                                TagButton(
                                    label = theme,
                                    isSelected = settings.backgroundTheme == theme,
                                    onClick = { viewModel.updateBackgroundTheme(theme) }
                                )
                            }
                        }
                    }

                    // Clock Style option
                    SettingCard(title = "时间与日期样式") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val styles = listOf("大字数字", "圆盘时钟", "极简文字")
                            styles.forEach { style ->
                                TagButton(
                                    label = style,
                                    isSelected = settings.clockStyle == style,
                                    onClick = { viewModel.updateClockStyle(style) }
                                )
                            }
                        }
                    }

                    // Weather location city option
                    SettingCard(title = "天气城市定位") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val cities = listOf("北京市", "上海市", "广州市", "深圳市", "成都市", "杭州市")
                            cities.forEach { city ->
                                TagButton(
                                    label = city,
                                    isSelected = settings.weatherCity == city,
                                    onClick = { viewModel.updateWeatherCity(city) }
                                )
                            }
                        }
                    }
                }

                // Vertical Divider
                Spacer(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp)
                )

                // Column 2: App drawer settings & Dev utility
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(360.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeader("二、应用抽屉 & 系统消息")

                    // Layout toggle: List vs grid
                    SettingCard(title = "应用栏网格/列表排版") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TagButton(
                                label = "🧩 网格布局",
                                isSelected = !settings.appDrawerListLayout,
                                onClick = { viewModel.updateAppLayout(false) },
                                modifier = Modifier.weight(1f)
                            )
                            TagButton(
                                label = "📃 列表布局",
                                isSelected = settings.appDrawerListLayout,
                                onClick = { viewModel.updateAppLayout(true) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Columns count (only if Grid)
                    if (!settings.appDrawerListLayout) {
                        SettingCard(title = "网格布局横向展示列数") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                (3..5).forEach { col ->
                                    TagButton(
                                        label = "$col 列",
                                        isSelected = settings.appDrawerColumns == col,
                                        onClick = { viewModel.updateAppDrawerColumns(col) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Notification simulation testing utilities
                    SettingCard(title = "桌面通知功能测试") {
                        Column {
                            Text(
                                text = "点击下方按钮模拟产生临时消息，以检验侧拉通知栏分屏显示效果 :",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 9.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.addNotification(
                                            "新单词学习打卡成功",
                                            "您今天已连续打卡 7 天！累积词汇量突破 1200 个，再接再厉！",
                                            "task"
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text("模拟核心打卡", fontSize = 9.sp)
                                }

                                Button(
                                    onClick = {
                                        viewModel.addNotification(
                                            "极低电量警告",
                                            "词典笔电量即将耗尽，当前剩余 5%，请及时连接 Type-C 充电器。",
                                            "system"
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text("模拟电量告警", fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF90CAF9),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

@Composable
fun SettingCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            content()
        }
    }
}

@Composable
fun TagButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF1976D2) else Color.White.copy(alpha = 0.08f),
            contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        modifier = modifier.height(30.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}
