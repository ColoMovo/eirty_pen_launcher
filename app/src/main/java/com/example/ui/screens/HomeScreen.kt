package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppInfo
import com.example.data.AppLauncherHelper
import com.example.data.LauncherSettings
import com.example.data.NotificationItem
import com.example.viewmodel.LauncherState
import com.example.viewmodel.LauncherViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val launcherState by viewModel.launcherState.collectAsState()
    val settings by viewModel.launcherSettings.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    
    // UI state & drag helper
    var swipeOffset by remember { mutableStateOf(0f) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Animated weights for split-screen layouts
    val drawerWeight by animateFloatAsState(
        targetValue = if (launcherState == LauncherState.APP_DRAWER) 0.5f else 0.0001f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "drawerWeight"
    )
    val homeWeight by animateFloatAsState(
        targetValue = if (launcherState == LauncherState.NORMAL) 1f else 0.5f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "homeWeight"
    )
    val controlWeight by animateFloatAsState(
        targetValue = if (launcherState == LauncherState.CONTROL_CENTER) 0.5f else 0.0001f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "controlWeight"
    )

    // Dynamic linear gradients background based on settings
    val backgroundBrush = getBackgroundBrush(settings.backgroundTheme)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .pointerInput(Unit) {
                // Drag gesture to swipe in/out panels
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (swipeOffset > 80f) {
                            // Swiped Right -> Pull Left Drawer in or close Right Panel
                            when (launcherState) {
                                LauncherState.CONTROL_CENTER -> viewModel.setLauncherState(LauncherState.NORMAL)
                                LauncherState.NORMAL -> viewModel.setLauncherState(LauncherState.APP_DRAWER)
                                else -> {}
                            }
                        } else if (swipeOffset < -80f) {
                            // Swiped Left -> Pull Right Panel in or close Left Panel
                            when (launcherState) {
                                LauncherState.APP_DRAWER -> viewModel.setLauncherState(LauncherState.NORMAL)
                                LauncherState.NORMAL -> viewModel.setLauncherState(LauncherState.CONTROL_CENTER)
                                else -> {}
                            }
                        }
                        swipeOffset = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        swipeOffset += dragAmount
                    }
                )
            }
    ) {
        // Master Container of Three Cards
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // -------------------------------------------------------------
            // CARD 2: APP DRAWER (Left Half, slides in/out)
            // -------------------------------------------------------------
            if (drawerWeight > 0.05f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(drawerWeight)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    AppDrawerCard(
                        viewModel = viewModel,
                        settings = settings,
                        onClose = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            viewModel.setLauncherState(LauncherState.NORMAL)
                        }
                    )
                }
            }

            // -------------------------------------------------------------
            // CARD 1: MAIN HOME DESKTOP (Full, or slides right/left as Right/Left half)
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(homeWeight)
            ) {
                HomeWidgetCard(
                    viewModel = viewModel,
                    settings = settings,
                    isCompact = launcherState != LauncherState.NORMAL,
                    onOpenSettings = onOpenSettings,
                    onShowApps = { viewModel.setLauncherState(LauncherState.APP_DRAWER) },
                    onShowControl = { viewModel.setLauncherState(LauncherState.CONTROL_CENTER) }
                )
            }

            // -------------------------------------------------------------
            // CARD 3: CONTROL & NOTIFICATION CENTER (Right Half, slides in/out)
            // -------------------------------------------------------------
            if (controlWeight > 0.05f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(controlWeight)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    ControlCenterCard(
                        viewModel = viewModel,
                        settings = settings,
                        notifications = notifications,
                        onClose = { viewModel.setLauncherState(LauncherState.NORMAL) }
                    )
                }
            }
        }

        // Custom Overlay Mock Brightness Control Layer (Dimmer)
        val dimmerAlpha = (100 - settings.brightnessLevel) / 100f * 0.70f // max 70% dimming
        if (dimmerAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimmerAlpha))
                    .pointerInput(Unit) {} // consume touch
            )
        }
    }
}

// -------------------------------------------------------------
// CARD 1 Detail implementation (Home Widget Panel)
// -------------------------------------------------------------
@Composable
fun HomeWidgetCard(
    viewModel: LauncherViewModel,
    settings: LauncherSettings,
    isCompact: Boolean,
    onOpenSettings: () -> Unit,
    onShowApps: () -> Unit,
    onShowControl: () -> Unit
) {
    // Current live time and date updates
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    var currentDayOfWeek by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance()
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            
            currentTime = timeFormat.format(cal.time)
            currentDate = dateFormat.format(cal.time)
            currentDayOfWeek = dayFormat.format(cal.time)
            delay(1000)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main unified layout inside Card 1 (Vertical layout: Clock first, with Weather and Now Playing under it!)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 48.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Clock & Date Display
            ClockDisplayWidget(
                style = settings.clockStyle,
                time = currentTime,
                date = currentDate,
                dayOfWeek = currentDayOfWeek,
                isCompact = isCompact
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Weather & Music playing under the clock
            Column(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Weather row
                WeatherWidget(settings = settings, onOpenSettings = onOpenSettings)

                // Music Control Widget
                if (settings.showNowPlaying) {
                    NowPlayingWidget(viewModel = viewModel)
                }
            }
        }

        // -------------------------------------------------------------
        // Quick Navigation Arrow Edge Handles (Tappable visual assists!)
        // -------------------------------------------------------------
        if (isCompact) {
            // Show return home/normal floating circle button on the empty-half of screen
            val isDrawerOpen = homeWeightStateIsDrawer(isCompact, viewModel)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = if (isDrawerOpen) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                IconButton(
                    onClick = { viewModel.setLauncherState(LauncherState.NORMAL) },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .shadow(4.dp)
                ) {
                    Icon(
                        imageVector = if (isDrawerOpen) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                        contentDescription = "返回桌面中央",
                        tint = Color.White
                    )
                }
            }
        } else {
            // Left Edge Button -> Opens App Drawer
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .padding(start = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onShowApps,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
                    modifier = Modifier.width(32.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = "应用",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "打开应用",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Right Edge Button -> Opens Control center
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onShowControl,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
                    modifier = Modifier.width(32.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "控制中心",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "打开控制",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun homeWeightStateIsDrawer(isCompact: Boolean, vm: LauncherViewModel): Boolean {
    val state by vm.launcherState.collectAsState()
    return state == LauncherState.APP_DRAWER
}

// -------------------------------------------------------------
// Component Widget: Custom Clock (Supports Beautiful analog dial drawing!)
// -------------------------------------------------------------
@Composable
fun ClockDisplayWidget(
    style: String,
    time: String,
    date: String,
    dayOfWeek: String,
    isCompact: Boolean
) {
    Card(
        modifier = Modifier
            .widthIn(max = 300.dp)
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (style == "圆盘时钟") {
                // Draw stunning 2D circular clock on canvas
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 110.dp else 135.dp)
                        .drawBehind {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = size.width / 2f
                            
                            // Draw dial border
                            drawCircle(
                                color = Color.White.copy(alpha = 0.15f),
                                radius = radius,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            
                            // Draw ticks
                            for (i in 0 until 12) {
                                val angleRad = Math.toRadians((i * 30).toDouble())
                                val tickStart = Offset(
                                    (center.x + (radius - 12.dp.toPx()) * sin(angleRad)).toFloat(),
                                    (center.y - (radius - 12.dp.toPx()) * cos(angleRad)).toFloat()
                                )
                                val tickEnd = Offset(
                                    (center.x + radius * sin(angleRad)).toFloat(),
                                    (center.y - radius * cos(angleRad)).toFloat()
                                )
                                drawLinearRoute(tickStart, tickEnd, Color.White.copy(alpha = if (i % 3 == 0) 0.7f else 0.3f), if (i % 3 == 0) 2.5f.dp.toPx() else 1f.dp.toPx())
                            }

                            // Draw current clock hour, minute, second pointers based on system time
                            val cal = Calendar.getInstance()
                            val hour = cal.get(Calendar.HOUR)
                            val minute = cal.get(Calendar.MINUTE)
                            val second = cal.get(Calendar.SECOND)

                            // Hour pointer (short, tick)
                            val hrAngle = Math.toRadians((hour * 30 + minute * 0.5f).toDouble())
                            val hrLength = radius * 0.5f
                            val hrEnd = Offset(
                                (center.x + hrLength * sin(hrAngle)).toFloat(),
                                (center.y - hrLength * cos(hrAngle)).toFloat()
                            )
                            drawLinearRoute(center, hrEnd, Color.White, 4f.dp.toPx())

                            // Minute pointer (longer, slim)
                            val minAngle = Math.toRadians((minute * 6).toDouble())
                            val minLength = radius * 0.75f
                            val minEnd = Offset(
                                (center.x + minLength * sin(minAngle)).toFloat(),
                                (center.y - minLength * cos(minAngle)).toFloat()
                            )
                            drawLinearRoute(center, minEnd, Color.White.copy(alpha = 0.85f), 2.5f.dp.toPx())

                            // Second pointer (extremely thin, accent)
                            val secAngle = Math.toRadians((second * 6).toDouble())
                            val secLength = radius * 0.85f
                            val secEnd = Offset(
                                (center.x + secLength * sin(secAngle)).toFloat(),
                                (center.y - secLength * cos(secAngle)).toFloat()
                            )
                            drawLinearRoute(center, secEnd, Color(0xFFFF5722), 1.5f.dp.toPx())

                            // Center pin point
                            drawCircle(color = Color.White, radius = 4f.dp.toPx())
                            drawCircle(color = Color(0xFFFF5722), radius = 2f.dp.toPx())
                        }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$date | $dayOfWeek",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            } else {
                // Typography Digital Clock Styles
                val timeSize = if (style == "大字数字") (if (isCompact) 54.sp else 68.sp) else 38.sp
                val spacingTop = if (style == "商务排版") 2.dp else 4.dp

                Text(
                    text = time,
                    fontSize = timeSize,
                    color = Color.White,
                    fontWeight = if (style == "大字数字") FontWeight.ExtraBold else FontWeight.Light,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(spacingTop))

                Text(
                    text = if (style == "极简文字") "Today is $dayOfWeek" else "$date $dayOfWeek",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLinearRoute(start: Offset, end: Offset, color: Color, strokeWidth: Float) {
    drawLine(color = color, start = start, end = end, strokeWidth = strokeWidth)
}

// -------------------------------------------------------------
// Component Widget: Weather Details
// -------------------------------------------------------------
@Composable
fun WeatherWidget(
    settings: LauncherSettings,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenSettings() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Weather icon based on condition
                val weatherIcon = when (settings.weatherCondition) {
                    "晴", "晴天" -> Icons.Default.WbSunny
                    "雨", "小雨", "雷阵雨" -> Icons.Default.Umbrella
                    "雪" -> Icons.Default.AcUnit
                    "多云", "多云转晴" -> Icons.Default.CloudQueue
                    else -> Icons.Default.Cloud
                }
                val iconColor = when (settings.weatherCondition) {
                    "晴", "晴天" -> Color(0xFFFFC107)
                    "多云", "多云转晴" -> Color(0xFF90CAF9)
                    else -> Color.White
                }
                
                Icon(
                    imageVector = weatherIcon,
                    contentDescription = settings.weatherCondition,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = settings.weatherCity,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = settings.weatherCondition,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }

            Text(
                text = "${settings.weatherTemp}${settings.tempUnit}",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -------------------------------------------------------------
// Component Widget: Now Playing (Music controls)
// -------------------------------------------------------------
@Composable
fun NowPlayingWidget(
    viewModel: LauncherViewModel
) {
    val track by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.musicProgress.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Song Name & Artist Info
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val scale by rememberInfiniteTransition().animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )
                    
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "音符",
                        tint = if (isPlaying) Color(0xFF388E3C) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(20.dp)
                            .then(if (isPlaying) Modifier.shadow(2.dp) else Modifier)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Text(
                            text = track.title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Playback Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.prevTrack() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "上一首",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "播放暂停",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.nextTrack() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "下一首",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Animated Linear Progress slider
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFFAF22FF),
                trackColor = Color.White.copy(alpha = 0.15f)
            )
        }
    }
}

// -------------------------------------------------------------
// CARD 2 Detail implementation (App Drawer Card)
// -------------------------------------------------------------
@Composable
fun AppDrawerCard(
    viewModel: LauncherViewModel,
    settings: LauncherSettings,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val query by viewModel.searchQuery.collectAsState()
    val appsList by viewModel.filteredApps.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // App search & Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "应用抽屉",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Styled Search Field
        TextField(
            value = query,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("搜索系统/扫描应用...", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Search, "搜索", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp)) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(Icons.Default.Clear, "清空", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            } else null,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                disabledContainerColor = Color.White.copy(alpha = 0.05f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { /* hide kb already handled */ })
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Applications layout (Supports list or grid!)
        if (appsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "未找到匹配的应用",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        } else if (settings.appDrawerListLayout) {
            // LIST LAYOUT
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(appsList) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                AppLauncherHelper.launchApp(context, app) { msg ->
                                    Toast
                                        .makeText(context, msg, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppDrawerIcon(app)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = app.label,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else {
            // GRID LAYOUT
            val cols = settings.appDrawerColumns.coerceIn(3, 5)
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(appsList) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                AppLauncherHelper.launchApp(context, app) { msg ->
                                    Toast
                                        .makeText(context, msg, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        AppDrawerIcon(app)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = app.label,
                            color = Color.White,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppDrawerIcon(app: AppInfo) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(android.graphics.Color.parseColor(app.iconColorHex)).copy(alpha = 0.25f))
            .border(1.dp, Color(android.graphics.Color.parseColor(app.iconColorHex)).copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = app.mockIcon,
            contentDescription = app.label,
            tint = Color(android.graphics.Color.parseColor(app.iconColorHex)),
            modifier = Modifier.size(18.dp)
        )
    }
}

// -------------------------------------------------------------
// CARD 3 Detail implementation (Control Center & Notifications)
// -------------------------------------------------------------
@Composable
fun ControlCenterCard(
    viewModel: LauncherViewModel,
    settings: LauncherSettings,
    notifications: List<NotificationItem>,
    onClose: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 -> Control, 1 -> Notifications

    Column(modifier = Modifier.fillMaxSize()) {
        // Card top header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Tab Header Swapper
            Row {
                TabHeaderItem(title = "快捷控制", isActive = activeTab == 0, onClick = { activeTab = 0 })
                Spacer(modifier = Modifier.width(12.dp))
                TabHeaderItem(title = "通知中心 (${notifications.size})", isActive = activeTab == 1, onClick = { activeTab = 1 })
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Content Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (activeTab == 0) {
                // EXQUISITE CONTROL WIDGETS
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick state toggles
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Wifi Toggle
                            QuickToggleButton(
                                label = "Wi-Fi",
                                icon = if (settings.wifiEnabled) Icons.Default.Wifi else Icons.Default.WifiOff,
                                isActive = settings.wifiEnabled,
                                onClick = { viewModel.toggleWifi() },
                                modifier = Modifier.weight(1f)
                            )

                            // Bluetooth Toggle
                            QuickToggleButton(
                                label = "蓝牙",
                                icon = if (settings.bluetoothEnabled) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                                isActive = settings.bluetoothEnabled,
                                onClick = { viewModel.toggleBluetooth() },
                                modifier = Modifier.weight(1f)
                            )

                            // Sound Mode Toggle
                            QuickToggleButton(
                                label = settings.ringMode,
                                icon = when (settings.ringMode) {
                                    "振动" -> Icons.Default.Vibration
                                    "静音" -> Icons.Default.VolumeMute
                                    else -> Icons.Default.VolumeUp
                                },
                                isActive = settings.ringMode != "静音",
                                onClick = { viewModel.toggleRingMode() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Brightness slider
                    item {
                        SliderSettingsItem(
                            label = "屏幕显示亮度",
                            value = settings.brightnessLevel,
                            icon = Icons.Default.WbSunny,
                            onValueChange = { viewModel.setBrightnessLevel(it) }
                        )
                    }

                    // Audio level slider
                    item {
                        SliderSettingsItem(
                            label = "系统多媒体音量",
                            value = settings.volumeLevel,
                            icon = Icons.Default.VolumeUp,
                            onValueChange = { viewModel.setVolumeLevel(it) }
                        )
                    }

                    // Simulated Battery or information bar
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.BatteryChargingFull, "电量", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("电量充满: 100%", color = Color.White, fontSize = 11.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DeveloperMode, "硬核", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("正常模式", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                // SCROLLABLE NOTIFICATION LIST
                Column(modifier = Modifier.fillMaxSize()) {
                    if (notifications.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无系统任何通知消息",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        // Clear All Button top
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { viewModel.clearNotifications() },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, "清空", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("清空通知", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(notifications, key = { it.id }) { item ->
                                NotificationRowItem(
                                    item = item,
                                    onDelete = { viewModel.removeNotification(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabHeaderItem(
    title: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = title,
        color = if (isActive) Color.White else Color.White.copy(alpha = 0.45f),
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
fun QuickToggleButton(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(58.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF1E88E5).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f)
        ),
        border = BorderStroke(
            1.dp, 
            if (isActive) Color(0xFF1E88E5).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color(0xFF90CAF9) else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = if (isActive) Color.White else Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SliderSettingsItem(
    label: String,
    value: Int,
    icon: ImageVector,
    onValueChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, label, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                }
                Text("${value}%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF42A5F5),
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )
        }
    }
}

@Composable
fun NotificationRowItem(
    item: NotificationItem,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // Header of individual notification
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val iconB = when (item.category) {
                            "music" -> Icons.Default.MusicNote
                            "weather" -> Icons.Default.WbSunny
                            "task" -> Icons.Default.CheckCircle
                            else -> Icons.Default.Notifications
                        }
                        val tintB = when (item.category) {
                            "music" -> Color(0xFF9C27B0)
                            "weather" -> Color(0xFFCDDC39)
                            "task" -> Color(0xFF4CAF50)
                            else -> Color(0xFF2196F3)
                        }
                        
                        Icon(iconB, item.appName, tint = tintB, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        
                        Text(
                            text = item.appName,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = item.time,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 9.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Heading / Title
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(1.dp))

                // Body content text
                Text(
                    text = item.content,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }

            // Swipe / Single click delete close box icon
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .padding(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "清除该通知",
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

// Helper: Custom Theme gradients generator
@Composable
fun getBackgroundBrush(themeName: String): Brush {
    return when (themeName) {
        "森林绿意" -> Brush.linearGradient(
            listOf(Color(0xFF0F3014), Color(0xFF13532A), Color(0xFF156535)),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
        "晨曦微光" -> Brush.radialGradient(
            listOf(Color(0xFF1E3C72), Color(0xFF0F1E33)),
            radius = 1600f
        )
        "极客黑客" -> Brush.verticalGradient(
            listOf(Color(0xFF000000), Color(0xFF0F1B12), Color(0xFF050505))
        )
        "极简深色" -> Brush.linearGradient(
            listOf(Color(0xFF121212), Color(0xFF1A1A1A), Color(0xFF151515))
        )
        else -> { // 深邃星空
            Brush.linearGradient(
                listOf(Color(0xFF0F2027), Color(0xFF182C35), Color(0xFF203A43)),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        }
    }
}
