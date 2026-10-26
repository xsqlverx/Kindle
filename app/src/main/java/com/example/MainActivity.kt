package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import androidx.core.view.WindowCompat
import com.example.data.model.AppThemeMode
import com.example.ui.components.LectureDetailSheet
import com.example.ui.components.LifeOsTab
import com.example.ui.components.TodayPrototypeDock
import com.example.ui.components.WorkspaceTopBar
import com.example.ui.screens.academics.AcademicsScreen
import com.example.ui.screens.gym.GymScreen
import com.example.ui.screens.reset.DailyResetScreen
import com.example.ui.screens.tasks.TasksMatrixScreen
import com.example.ui.screens.today.TodayScreen
import com.example.ui.theme.LifeOsTheme
import com.example.ui.theme.LocalLifeOsColors
import com.example.viewmodel.LifeOsViewModel
import com.example.viewmodel.TodayPrototypeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: LifeOsViewModel = composeViewModel()
            val focusModel: TodayPrototypeViewModel = composeViewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

            LifeOsTheme(mode = if (currentTab == LifeOsTab.TODAY) AppThemeMode.NIGHT_GRAPHITE else themeMode) {
                val lifeColors = LocalLifeOsColors.current
                val density = LocalDensity.current
                val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                var headerHeight by remember { mutableStateOf(76.dp) }
                val screenState = rememberSaveableStateHolder()
                SideEffect {
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    insetsController.isAppearanceLightStatusBars = !lifeColors.isDark
                    insetsController.isAppearanceLightNavigationBars = !lifeColors.isDark
                }

                // Full screen fluid canvas with atmospheric ambient light glows
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(lifeColors.background)
                        .drawBehind {
                            // Top subtle luminous orb in Iris
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        lifeColors.primaryAccent.copy(alpha = if (lifeColors.isDark) 0.045f else 0.08f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.8f, size.height * 0.12f),
                                    radius = size.width * 0.7f
                                )
                            )
                            // Bottom ambient glow in Secondary Accent
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        lifeColors.secondaryAccent.copy(alpha = if (lifeColors.isDark) 0.025f else 0.05f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.2f, size.height * 0.85f),
                                    radius = size.width * 0.8f
                                )
                            )
                        }
                ) {
                    var showSettings by remember { mutableStateOf(false) }
                    val selectedLectureForDetail by viewModel.selectedLectureForDetail.collectAsStateWithLifecycle()
                    val attendanceRecords by viewModel.attendanceRecords.collectAsStateWithLifecycle()
                    val lectureStatus by viewModel.currentLectureStatus.collectAsStateWithLifecycle()

                    val settingsProgress by animateFloatAsState(
                        targetValue = if (showSettings) 1f else 0f,
                        animationSpec = tween(
                            durationMillis = if (showSettings) 340 else 320,
                            easing = FastOutSlowInEasing
                        ),
                        label = "settingsProgress"
                    )

                    // Spring animation for hiding bottom nav bar when sheet or settings is open
                    val navBarHideProgress by animateFloatAsState(
                        targetValue = if (showSettings || selectedLectureForDetail != null) 1f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "navBarHideProgress"
                    )

                    BackHandler(enabled = showSettings) {
                        showSettings = false
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            val dockModifier = Modifier.graphicsLayer {
                                alpha = (1f - navBarHideProgress * 1.5f).coerceIn(0f, 1f)
                                translationY = navBarHideProgress * 64.dp.toPx()
                            }
                            TodayPrototypeDock(currentTab, { viewModel.setTab(it) }, dockModifier)
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(top = if (currentTab == LifeOsTab.TODAY) 0.dp else headerHeight)
                                .padding(bottom = innerPadding.calculateBottomPadding())
                                .clipToBounds()
                        ) {
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = {
                                    val forward = targetState.ordinal > initialState.ordinal
                                    val animationSpec = tween<IntOffset>(
                                        durationMillis = 260,
                                        easing = FastOutSlowInEasing
                                    )
                                    if (forward) {
                                        slideInHorizontally(
                                            initialOffsetX = { fullWidth -> fullWidth },
                                            animationSpec = animationSpec
                                        ) togetherWith slideOutHorizontally(
                                            targetOffsetX = { fullWidth -> -fullWidth },
                                            animationSpec = animationSpec
                                        )
                                    } else {
                                        slideInHorizontally(
                                            initialOffsetX = { fullWidth -> -fullWidth },
                                            animationSpec = animationSpec
                                        ) togetherWith slideOutHorizontally(
                                            targetOffsetX = { fullWidth -> fullWidth },
                                            animationSpec = animationSpec
                                        )
                                    }
                                },
                                label = "mainTabHorizontalSlide",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clipToBounds()
                            ) { tab ->
                                screenState.SaveableStateProvider(tab.name) {
                                    when (tab) {
                                        LifeOsTab.TODAY -> TodayScreen(
                                            viewModel = viewModel,
                                            onNavigateToTab = { viewModel.setTab(it) },
                                            onOpenSettings = { showSettings = true }
                                        )
                                        LifeOsTab.GYM -> GymScreen(
                                            viewModel = viewModel
                                        )
                                        LifeOsTab.ACADEMICS -> AcademicsScreen(
                                            viewModel = viewModel
                                        )
                                        LifeOsTab.TASKS -> TasksMatrixScreen(
                                            viewModel = viewModel,
                                            onStartFocus = { task ->
                                                focusModel.start(task.title)
                                                focusModel.requestSessionPanel()
                                                viewModel.setTab(LifeOsTab.TODAY)
                                            }
                                        )
                                        LifeOsTab.RESET -> DailyResetScreen(
                                            viewModel = viewModel,
                                            onFinishReset = { viewModel.setTab(LifeOsTab.TODAY) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Settings corner-origin expanding / collapsing overlay
                    if (settingsProgress > 0.001f) {
                        // Ambient dim scrim
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f * settingsProgress))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showSettings = false
                                }
                        )

                        // Asymmetric Settings Motion: Bottom Sheet Slide-Up on open, Corner-origin Collapse on close
                        val isOpening = showSettings
                        val progress = settingsProgress

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    if (isOpening) {
                                        // Distinctive Full-Height Modal Sheet Slide-Up
                                        val sheetHeight = if (size.height > 0f) size.height else 2000f
                                        transformOrigin = TransformOrigin(0.5f, 1.0f)
                                        translationX = 0f
                                        translationY = sheetHeight * (1f - progress)
                                        scaleX = 0.94f + 0.06f * progress
                                        scaleY = 0.94f + 0.06f * progress
                                        alpha = (progress * 1.5f).coerceIn(0f, 1f)
                                        clip = true
                                        shape = RoundedCornerShape(
                                            topStart = (28.dp * (1f - progress)).coerceAtLeast(0.dp),
                                            topEnd = (28.dp * (1f - progress)).coerceAtLeast(0.dp),
                                            bottomStart = 0.dp,
                                            bottomEnd = 0.dp
                                        )
                                    } else {
                                        // Preserved closing collapse directly into the Settings icon
                                        transformOrigin = TransformOrigin(0.92f, 0.045f)
                                        scaleX = 0.04f + 0.96f * progress
                                        scaleY = 0.04f + 0.96f * progress
                                        translationX = (1f - progress) * 140.dp.toPx()
                                        translationY = 0f
                                        alpha = (progress * 2f).coerceIn(0f, 1f)
                                        clip = true
                                        shape = RoundedCornerShape((32.dp * (1f - progress)).coerceAtLeast(0.dp))
                                    }
                                }
                        ) {
                            com.example.ui.screens.settings.SettingsScreen(
                                viewModel = viewModel,
                                onBack = { showSettings = false }
                            )
                        }
                    }

                    // Top App Bar Glass anchored on top
                    if (currentTab != LifeOsTab.TODAY || showSettings) WorkspaceTopBar(
                        modifier = Modifier.onSizeChanged { size ->
                            headerHeight = (with(density) { size.height.toDp() } - statusInset).coerceAtLeast(76.dp)
                        },
                        title = when (currentTab) {
                            LifeOsTab.TODAY -> "Life OS"
                            LifeOsTab.GYM -> "Fitness"
                            LifeOsTab.ACADEMICS -> "College"
                            LifeOsTab.TASKS -> "Tasks"
                            LifeOsTab.RESET -> "Daily Reset"
                        },
                        subtitle = when (currentTab) {
                            LifeOsTab.TODAY -> null
                            LifeOsTab.GYM -> "TRAINING & HEALTH"
                            LifeOsTab.ACADEMICS -> "TIMETABLE & ATTENDANCE"
                            LifeOsTab.TASKS -> "FOCUS & PRIORITIES"
                            LifeOsTab.RESET -> "NIGHT PROTOCOL"
                        },
                        currentThemeMode = themeMode,
                        onThemeModeChanged = { viewModel.setThemeMode(it) },
                        onOpenSettings = { showSettings = !showSettings },
                        isSettingsOpen = showSettings,
                        settingsProgress = settingsProgress
                    )

                    // 60% Slide-Up Lecture Detail Sheet with Spring Motion & Zero Overlap
                    if (selectedLectureForDetail != null) {
                        LectureDetailSheet(
                            entry = selectedLectureForDetail,
                            isOngoingOverride = if (selectedLectureForDetail?.id == lectureStatus.entry?.id) lectureStatus.isOngoing else null,
                            attendanceRecords = attendanceRecords,
                            onDismiss = { viewModel.closeLectureDetail() },
                            onRecordAttendance = { code, attended ->
                                viewModel.recordAttendance(code, attended)
                            }
                        )
                    }
                }
            }
        }
    }
}
