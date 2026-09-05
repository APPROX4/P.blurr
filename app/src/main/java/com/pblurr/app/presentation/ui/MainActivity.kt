package com.pblurr.app.presentation.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pblurr.app.presentation.ui.debug.DebugScreen
import com.pblurr.app.presentation.ui.editor.EditorScreen
import com.pblurr.app.presentation.ui.editor.manual.ManualMaskEditorScreen
import com.pblurr.app.presentation.ui.home.HomeScreen
import com.pblurr.app.presentation.ui.processing.ProcessingScreen
import com.pblurr.app.presentation.ui.settings.SettingsScreen
import com.pblurr.app.presentation.ui.theme.PblurrTheme

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Processing : Screen("processing")
    object Editor : Screen("editor")
    object ManualEditor : Screen("manual_editor")
    object Settings : Screen("settings")
    object Debug : Screen("debug")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PblurrTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PblurrNavHost(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun PblurrNavHost(viewModel: MainViewModel) {
    val navController = rememberNavController()

    val uiState by viewModel.uiState.collectAsState()
    val censorOptions by viewModel.censorOptions.collectAsState()
    val showMaskOverlay by viewModel.showMaskOverlay.collectAsState()
    val showRawBoxes by viewModel.showRawBoxes.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()
    val exportResult by viewModel.exportResult.collectAsState()

    // Handle export notifications
    LaunchedEffect(exportResult) {
        val result = exportResult ?: return@LaunchedEffect
        result.onSuccess { uri ->
            Toast.makeText(navController.context, "Saved to Gallery!", Toast.LENGTH_LONG).show()
            viewModel.clearExportResult()
        }.onFailure { err ->
            Toast.makeText(navController.context, "Save failed: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
            viewModel.clearExportResult()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                updateInfo = updateInfo,
                onDismissUpdate = { viewModel.dismissUpdate() },
                onPhotoSelected = { uri ->
                    viewModel.processImageUri(uri)
                    navController.navigate(Screen.Processing.route)
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToDebug = { navController.navigate(Screen.Debug.route) }
            )
        }

        composable(Screen.Processing.route) {
            when (val state = uiState) {
                is UIState.Processing -> {
                    ProcessingScreen(
                        stage = state.stage,
                        onCancel = { navController.navigateUp() }
                    )
                }
                is UIState.Success -> {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Editor.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                }
                is UIState.Error -> {
                    LaunchedEffect(state) {
                        Toast.makeText(navController.context, state.message, Toast.LENGTH_LONG).show()
                        navController.navigateUp()
                    }
                }
                else -> {}
            }
        }

        composable(Screen.Editor.route) {
            val state = uiState as? UIState.Success
            if (state != null) {
                EditorScreen(
                    result = state.result,
                    censorOptions = censorOptions,
                    showMaskOverlay = showMaskOverlay,
                    showRawBoxes = showRawBoxes,
                    onUpdateOptions = { viewModel.updateCensorOptions(it) },
                    onToggleMaskOverlay = { viewModel.toggleMaskOverlay() },
                    onToggleRawBoxes = { viewModel.toggleRawBoxes() },
                    onOpenManualEditor = { navController.navigate(Screen.ManualEditor.route) },
                    onSaveImage = { viewModel.exportCensoredImage() },
                    onNavigateBack = { navController.navigate(Screen.Home.route) },
                    onNavigateToDebug = { navController.navigate(Screen.Debug.route) }
                )
            }
        }

        composable(Screen.ManualEditor.route) {
            val original = viewModel.currentOriginalBitmap
            val mask = viewModel.currentMaskBitmap
            if (original != null && mask != null) {
                ManualMaskEditorScreen(
                    originalBitmap = original,
                    initialMask = mask,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onUndo = { viewModel.undoMaskEdit() },
                    onRedo = { viewModel.redoMaskEdit() },
                    onClearAll = { viewModel.clearMask() },
                    onRestoreAiMask = { viewModel.restoreAiMask() },
                    onDone = { editedMask ->
                        viewModel.updateManualMask(editedMask)
                        navController.navigateUp()
                    },
                    onCancel = { navController.navigateUp() }
                )
            }
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                censorOptions = censorOptions,
                onUpdateOptions = { viewModel.updateCensorOptions(it) },
                onResetSettings = { viewModel.resetSettings() },
                onNavigateBack = { navController.navigateUp() },
                onNavigateToDebug = { navController.navigate(Screen.Debug.route) }
            )
        }

        composable(Screen.Debug.route) {
            DebugScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}
