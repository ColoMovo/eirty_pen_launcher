package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: LauncherViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        var currentLevel by remember { mutableIntStateOf(1) } // 1 -> Desktop level, 2 -> Settings level

        Surface(modifier = Modifier.fillMaxSize()) {
          AnimatedContent(
            targetState = currentLevel,
            transitionSpec = {
              if (targetState > initialState) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                  slideOutHorizontally { width -> -width } + fadeOut()
                )
              } else {
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                  slideOutHorizontally { width -> width } + fadeOut()
                )
              }
            },
            label = "levelTransition"
          ) { level ->
            when (level) {
              1 -> HomeScreen(
                viewModel = viewModel,
                onOpenSettings = { currentLevel = 2 }
              )
              2 -> SettingsScreen(
                viewModel = viewModel,
                onBack = { currentLevel = 1 }
              )
            }
          }
        }
      }
    }
  }
}

