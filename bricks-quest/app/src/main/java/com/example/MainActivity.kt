package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.audio.SoundManager
import com.example.data.GameStorage
import com.example.engine.GameEngine
import com.example.model.GameMode
import com.example.ui.GameScreen
import com.example.ui.LevelMapScreen
import com.example.ui.MainMenuScreen
import com.example.ui.ShopScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SpaceNavyDark

sealed interface AppScreen {
    object Splash : AppScreen
    object MainMenu : AppScreen
    object LevelMap : AppScreen
    data class Game(val mode: GameMode, val stageNumber: Int) : AppScreen
    data class Shop(val initialTab: String) : AppScreen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val storage = remember { GameStorage(context) }
                val soundManager = remember {
                    SoundManager(context).apply {
                        isSoundEnabled = storage.soundEnabled
                        isHapticsEnabled = storage.hapticsEnabled
                    }
                }
                val engine = remember { GameEngine(soundManager, storage) }

                var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.MainMenu) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SpaceNavyDark
                ) {
                    Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                        when (screen) {
                            is AppScreen.Splash -> {
                                MainMenuScreen(
                                    storage = storage,
                                    soundManager = soundManager,
                                    onStartPlay = { currentScreen = AppScreen.LevelMap },
                                    onOpenGameMode = { mode, stage -> currentScreen = AppScreen.Game(mode, stage) },
                                    onOpenShop = { tab -> currentScreen = AppScreen.Shop(tab) }
                                )
                            }
                            is AppScreen.MainMenu -> {
                                MainMenuScreen(
                                    storage = storage,
                                    soundManager = soundManager,
                                    onStartPlay = {
                                        currentScreen = AppScreen.LevelMap
                                    },
                                    onOpenGameMode = { mode, stage ->
                                        currentScreen = AppScreen.Game(mode, stage)
                                    },
                                    onOpenShop = { tab ->
                                        currentScreen = AppScreen.Shop(tab)
                                    }
                                )
                            }
                            is AppScreen.LevelMap -> {
                                BackHandler {
                                    currentScreen = AppScreen.MainMenu
                                }
                                LevelMapScreen(
                                    storage = storage,
                                    soundManager = soundManager,
                                    onBack = {
                                        currentScreen = AppScreen.MainMenu
                                    },
                                    onStartStage = { stage ->
                                        currentScreen = AppScreen.Game(GameMode.QUEST, stage)
                                    },
                                    onStartClassic = {
                                        currentScreen = AppScreen.Game(GameMode.ENDLESS, 1)
                                    },
                                    onStartRush = {
                                        currentScreen = AppScreen.Game(GameMode.RUSH_100, 1)
                                    },
                                    onOpenShop = {
                                        currentScreen = AppScreen.Shop("DIAMONDS")
                                    },
                                    onOpenBallSkins = {
                                        currentScreen = AppScreen.Shop("BALLS")
                                    }
                                )
                            }
                            is AppScreen.Game -> {
                                GameScreen(
                                    engine = engine,
                                    soundManager = soundManager,
                                    storage = storage,
                                    mode = screen.mode,
                                    stageNumber = screen.stageNumber,
                                    onStageChanged = { /* Continuous in-game progression handled by GameEngine without screen recreation */ },
                                    onBackToMenu = {
                                        currentScreen = AppScreen.MainMenu
                                    },
                                    onSelectStages = {
                                        currentScreen = AppScreen.LevelMap
                                    }
                                )
                            }
                            is AppScreen.Shop -> {
                                BackHandler {
                                    currentScreen = AppScreen.MainMenu
                                }
                                ShopScreen(
                                    storage = storage,
                                    soundManager = soundManager,
                                    onBack = {
                                        currentScreen = AppScreen.MainMenu
                                    },
                                    initialTab = screen.initialTab
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
