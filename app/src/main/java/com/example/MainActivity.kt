package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.NoiseControlOff
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ai.configureAppCheck
import com.example.model.AppDestination
import com.example.ui.screens.AncControlScreen
import com.example.ui.screens.DevicesScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.OnboardingSetupSheet
import com.example.ui.screens.SettingsSheet
import com.example.ui.screens.SoundStudioScreen
import com.example.ui.screens.SystemUpdateScreen
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.HardikBudsTheme
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingRedMuted
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverText
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK)
        )
        val initialRoute = intent?.getStringExtra("open_route")
        setContent {
            HardikBudsTheme {
                HardikBudsRootApp(initialRoute = initialRoute)
            }
        }
    }
}

@Composable
fun HardikBudsRootApp(
    initialRoute: String? = null,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val deviceState by viewModel.deviceState.collectAsStateWithLifecycle()
    val prefsState by viewModel.preferencesState.collectAsStateWithLifecycle()
    val updateStatus by viewModel.updateStatus.collectAsStateWithLifecycle()
    val aiDoctorState by viewModel.aiDoctorState.collectAsStateWithLifecycle()
    val showOnboarding by viewModel.showOnboardingSheet.collectAsStateWithLifecycle()
    val showSettings by viewModel.showSettingsSheet.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onPermissionsResult()
    }

    val navItems = listOf(
        AppDestination.HOME to Icons.Rounded.Dashboard,
        AppDestination.ANC to Icons.Rounded.NoiseControlOff,
        AppDestination.STUDIO to Icons.Rounded.GraphicEq,
        AppDestination.DEVICES to Icons.Rounded.Bluetooth,
        AppDestination.UPDATE to Icons.Rounded.SystemUpdate
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppDestination.HOME.route

    val navigateTo: (AppDestination) -> Unit = { dest ->
        navController.navigate(dest.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val launchBluetoothSettings: () -> Unit = {
        runCatching { context.startActivity(viewModel.bluetoothSettingsIntent()) }
    }

    val launchCompanionApp: () -> Unit = {
        val intent = viewModel.companionAppIntent()
        if (intent != null) {
            runCatching { context.startActivity(intent) }
        } else {
            Toast.makeText(context, "Official HeyMelody companion app not installed", Toast.LENGTH_SHORT).show()
        }
    }

    val launchApkInstaller: (String) -> Unit = { apkPath ->
        if (!viewModel.canInstallPackages()) {
            Toast.makeText(
                context,
                "Allow 'Install unknown apps' for Hardik Buds Pro, then tap Install again.",
                Toast.LENGTH_LONG
            ).show()
            runCatching { context.startActivity(viewModel.unknownSourcesSettingsIntent()) }
        } else {
            val installIntent = viewModel.packageInstallerIntent(apkPath)
            if (installIntent != null) {
                runCatching { context.startActivity(installIntent) }
                    .onFailure {
                        Toast.makeText(context, "Unable to launch package installer", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = AmoledBlack,
            contentWindowInsets = WindowInsets.statusBars,
            bottomBar = {
                if (!useRail) {
                    NothingBottomBar(
                        items = navItems,
                        currentRoute = currentRoute,
                        onSelect = navigateTo
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NothingNavRail(
                        items = navItems,
                        currentRoute = currentRoute,
                        onSelect = navigateTo
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = if (initialRoute == AppDestination.UPDATE.route) {
                            AppDestination.UPDATE.route
                        } else {
                            AppDestination.HOME.route
                        },
                        enterTransition = { fadeIn(animationSpec = tween(180)) },
                        exitTransition = { fadeOut(animationSpec = tween(180)) }
                    ) {
                        composable(AppDestination.HOME.route) {
                            HomeDashboardScreen(
                                deviceState = deviceState,
                                audioState = audioState,
                                prefsState = prefsState,
                                updateStatus = updateStatus,
                                onRequestPermissions = {
                                    permissionLauncher.launch(viewModel.requiredBluetoothPermissions())
                                },
                                onRefreshBluetooth = { viewModel.refreshBluetooth() },
                                onProbeGatt = { viewModel.probeConnectedGatt() },
                                onToggleEngine = { viewModel.setEngineEnabled(it) },
                                onToggleFindRing = { viewModel.toggleFindEarbudsRing() },
                                onNavigate = navigateTo,
                                onOpenSettings = { viewModel.openSettings() },
                                onOpenOnboarding = { viewModel.openOnboarding() }
                            )
                        }
                        composable(AppDestination.ANC.route) {
                            AncControlScreen(
                                deviceState = deviceState,
                                hapticsEnabled = prefsState.hapticsEnabled,
                                onSelectMode = { mode, level -> viewModel.selectAncMode(mode, level) },
                                onOpenBluetoothSettings = launchBluetoothSettings,
                                onOpenCompanionApp = launchCompanionApp
                            )
                        }
                        composable(AppDestination.STUDIO.route) {
                            SoundStudioScreen(
                                audioState = audioState,
                                prefsState = prefsState,
                                onToggleEngine = { viewModel.setEngineEnabled(it) },
                                onToggleLimiter = { viewModel.setLimiterActive(it) },
                                onToggleTestSignal = { viewModel.toggleAcousticTestSignal() },
                                onSelectMode = { viewModel.setSoundProfileMode(it) },
                                onSelectPreset = { viewModel.applyPreset(it) },
                                onSaveCustomPreset = { viewModel.saveCurrentAsCustomPreset(it) },
                                onDeleteCustomPreset = { viewModel.deleteCustomPreset(it) },
                                onBandChange = { idx, db -> viewModel.setBandGain(idx, db) },
                                onBassChange = { viewModel.setBassBoost(it) },
                                onTrebleChange = { viewModel.setTrebleBoost(it) },
                                onVocalChange = { viewModel.setVocalBoost(it) },
                                onClarityChange = { viewModel.setClarity(it) },
                                onVolumeEnhanceChange = { viewModel.setVolumeEnhanceDb(it) },
                                onBalanceChange = { viewModel.setStereoBalance(it) },
                                onSpatialChange = { viewModel.setSpatialVirtualizer(it) },
                                onResetFlat = { viewModel.resetAudioToFlat() }
                            )
                        }
                        composable(AppDestination.DEVICES.route) {
                            DevicesScreen(
                                deviceState = deviceState,
                                prefsState = prefsState,
                                onRequestPermissions = {
                                    permissionLauncher.launch(viewModel.requiredBluetoothPermissions())
                                },
                                onStartScan = { viewModel.startBleScan() },
                                onStopScan = { viewModel.stopBleScan() },
                                onPairOrConnect = { viewModel.pairOrConnect(it) },
                                onToggleAutoReconnect = { viewModel.setAutoReconnect(it) },
                                onToggleFindRing = { viewModel.toggleFindEarbudsRing() },
                                onProbeGatt = { viewModel.probeConnectedGatt() },
                                onOpenBluetoothSettings = launchBluetoothSettings,
                                onOpenCompanionApp = launchCompanionApp
                            )
                        }
                        composable(AppDestination.UPDATE.route) {
                            SystemUpdateScreen(
                                currentVersion = viewModel.currentAppVersion(),
                                updateStatus = updateStatus,
                                aiDoctorState = aiDoctorState,
                                prefsState = prefsState,
                                onCheckForUpdates = { viewModel.checkForUpdates() },
                                onDownloadUpdate = { viewModel.downloadUpdate() },
                                onInstallUpdate = launchApkInstaller,
                                onSaveRepoUrl = { viewModel.updateGithubRepoUrl(it) },
                                onDiagnoseError = { viewModel.diagnoseCustomError(it) },
                                onRequestBtPermissions = {
                                    permissionLauncher.launch(viewModel.requiredBluetoothPermissions())
                                },
                                onOpenBtSettings = launchBluetoothSettings
                            )
                        }
                    }
                }
            }
        }
    }

    if (showOnboarding) {
        OnboardingSetupSheet(
            deviceState = deviceState,
            hapticsEnabled = prefsState.hapticsEnabled,
            onRequestPermissions = { permissionLauncher.launch(viewModel.requiredBluetoothPermissions()) },
            onOpenBluetoothSettings = launchBluetoothSettings,
            onComplete = { viewModel.dismissOnboarding(markCompleted = true) },
            onDismiss = { viewModel.dismissOnboarding(markCompleted = false) }
        )
    }

    if (showSettings) {
        SettingsSheet(
            prefsState = prefsState,
            currentVersion = viewModel.currentAppVersion(),
            onToggleHaptics = { viewModel.setHapticsEnabled(it) },
            onToggleAutoReconnect = { viewModel.setAutoReconnect(it) },
            onToggleAutoUpdate = { viewModel.setAutoUpdateCheck(it) },
            onToggleAutoAiHeal = { viewModel.setAutoAiHeal(it) },
            onSafeThresholdChange = { viewModel.setSafeListeningThreshold(it) },
            onOpenOnboarding = { viewModel.openOnboarding() },
            onDismiss = { viewModel.closeSettings() }
        )
    }
}

@Composable
private fun NothingBottomBar(
    items: List<Pair<AppDestination, ImageVector>>,
    currentRoute: String,
    onSelect: (AppDestination) -> Unit
) {
    NavigationBar(
        containerColor = ObsidianSurface,
        contentColor = PureWhite,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        items.forEach { (dest, icon) ->
            val selected = currentRoute == dest.route
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(dest) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = dest.label
                    )
                },
                label = {
                    Text(
                        text = dest.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NothingRed,
                    selectedTextColor = PureWhite,
                    indicatorColor = NothingRedMuted,
                    unselectedIconColor = SilverText,
                    unselectedTextColor = SilverText
                ),
                modifier = Modifier.testTag("nav_${dest.route}")
            )
        }
    }
}

@Composable
private fun NothingNavRail(
    items: List<Pair<AppDestination, ImageVector>>,
    currentRoute: String,
    onSelect: (AppDestination) -> Unit
) {
    NavigationRail(
        containerColor = ObsidianSurface,
        contentColor = PureWhite,
        modifier = Modifier.fillMaxHeight()
    ) {
        items.forEach { (dest, icon) ->
            val selected = currentRoute == dest.route
            NavigationRailItem(
                selected = selected,
                onClick = { onSelect(dest) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = dest.label
                    )
                },
                label = {
                    Text(
                        text = dest.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = NothingRed,
                    selectedTextColor = PureWhite,
                    indicatorColor = NothingRedMuted,
                    unselectedIconColor = SilverText,
                    unselectedTextColor = SilverText
                ),
                modifier = Modifier.testTag("rail_${dest.route}")
            )
        }
    }
}
