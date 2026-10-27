package com.example

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.screens.academics.AcademicsScreen
import com.example.ui.screens.gym.GymScreen
import com.example.ui.screens.reset.DailyResetScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.tasks.TasksMatrixScreen
import com.example.ui.screens.today.TodayDesignPreview
import com.example.ui.screens.today.TodayToolsScreen
import com.example.ui.screens.workspace.*
import com.example.ui.theme.LifeOsTheme
import com.example.ui.theme.LocalLifeOsColors
import com.example.viewmodel.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private var reminderRequest by mutableStateOf<Pair<String, String>?>(null)
    private fun readReminder(intent: Intent) {
        val action = intent.getStringExtra("work_action") ?: return
        reminderRequest = action to intent.getStringExtra("block_id").orEmpty()
        intent.removeExtra("work_action")
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readReminder(intent)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) readReminder(intent)
        setContent {
            val life: LifeOsViewModel = viewModel()
            val model: WorkspaceViewModel = viewModel()
            val focus: TodayPrototypeViewModel = viewModel()
            val destination by model.destination.collectAsStateWithLifecycle()
            val slots by model.slots.collectAsStateWithLifecycle()
            val reduced by model.reduceMotion.collectAsStateWithLifecycle()
            val theme by life.themeMode.collectAsStateWithLifecycle()
            val session by focus.session.collectAsStateWithLifecycle()
            val savingSession by focus.saving.collectAsStateWithLifecycle()
            val openSession by focus.openSessionPanel.collectAsStateWithLifecycle()
            val message by model.message.collectAsStateWithLifecycle()
            var launcher by rememberSaveable { mutableStateOf(false) }
            var panel by rememberSaveable { mutableStateOf(false) }
            var tools by rememberSaveable { mutableStateOf(false) }
            var preview by rememberSaveable { mutableStateOf(false) }
            var trainingSplit by rememberSaveable { mutableStateOf<String?>(null) }
            var previous by rememberSaveable { mutableStateOf(Destination.TODAY.id) }
            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            val state = rememberSaveableStateHolder()
            LaunchedEffect(reminderRequest) {
                reminderRequest?.let { (action, id) -> model.handleReminder(action, id, focus); reminderRequest = null }
            }
            val navigate: (Destination) -> Unit = { to ->
                if (to != destination) previous = destination.id
                launcher = false
                model.navigate(to)
            }
            val legacyNavigate: (LifeOsTab) -> Unit = { tab -> navigate(when (tab) {
                LifeOsTab.TODAY -> Destination.TODAY
                LifeOsTab.GYM -> Destination.FITNESS
                LifeOsTab.ACADEMICS -> Destination.COLLEGE
                LifeOsTab.TASKS -> Destination.TASKS
                LifeOsTab.RESET -> Destination.RESET
            }) }
            LaunchedEffect(lifecycle, session.phase) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    while (true) {
                        now = System.currentTimeMillis()
                        focus.refresh()
                        delay(if (session.phase == FocusPhase.RUNNING) 1000L else 15_000L)
                    }
                }
            }
            LaunchedEffect(openSession) { if (openSession) { panel = true; focus.consumeSessionPanelRequest() } }
            BackHandler(enabled = launcher || tools || preview || destination != Destination.TODAY) {
                when {
                    launcher -> launcher = false
                    tools -> tools = false
                    preview -> preview = false
                    destination == Destination.SETTINGS -> navigate(Destination.fromId(previous)?.takeIf { it != Destination.SETTINGS } ?: Destination.TODAY)
                    else -> navigate(Destination.TODAY)
                }
            }
            LifeOsTheme(mode = theme) {
                val colors = LocalLifeOsColors.current
                val density = LocalDensity.current
                val dockReserve = workspaceDockHeight() + 20.dp
                var capsuleHeight by remember { mutableStateOf(74.dp) }
                val contentLayer = rememberGraphicsLayer()
                val backdropLayer = rememberGraphicsLayer()
                SideEffect {
                    backdropLayer.renderEffect = if (android.os.Build.VERSION.SDK_INT >= 31) BlurEffect(20f, 20f, TileMode.Clamp) else null
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.isAppearanceLightStatusBars = !colors.isDark
                    controller.isAppearanceLightNavigationBars = !colors.isDark
                }
                CompositionLocalProvider(LocalReducedMotion provides reduced, LocalWorkspaceBackdrop provides backdropLayer) {
                    Box(Modifier.fillMaxSize().background(colors.background)) {
                        // Capture only the page scene. Glass overlays cannot capture themselves.
                        CompositionLocalProvider(LocalWorkspaceBackdrop provides null) {
                            Box(Modifier.fillMaxSize().then(if (launcher) Modifier.clearAndSetSemantics {} else Modifier)
                                .drawWithContent {
                                    contentLayer.record { this@drawWithContent.drawContent() }
                                    backdropLayer.record { drawLayer(contentLayer) }
                                    drawLayer(contentLayer)
                                }.background(Brush.radialGradient(listOf(colors.primaryAccent.copy(alpha = 0.14f), colors.background), center = Offset(750f, 300f), radius = 1300f))) {
                                Column(Modifier.fillMaxSize()) {
                                    WorkspaceTopBar(destination.title, destination.purpose, theme, life::setThemeMode,
                                        { navigate(Destination.SETTINGS) }, false, 0f)
                                    Box(Modifier.weight(1f).padding(bottom = dockReserve + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                        if (session.phase != FocusPhase.IDLE) capsuleHeight + 12.dp else 0.dp).clipToBounds()) {
                                        AnimatedContent(destination, transitionSpec = {
                                            (fadeIn(tween(if (reduced) 0 else 180)) + slideInVertically(tween(if (reduced) 0 else 260)) { it / 20 }) togetherWith
                                                fadeOut(tween(if (reduced) 0 else 140))
                                        }, label = "workspace_destination") { target ->
                                            state.SaveableStateProvider(target.id) {
                                                when (target) {
                                                    Destination.TODAY -> LiveTodayScreen(life, model, focus, now) { tools = true }
                                                    Destination.PLAN -> PlanScreen(life, model, focus, now)
                                                    Destination.COLLEGE -> AcademicsScreen(life, onStartWork = { assignment ->
                                                        model.begin(focus, assignment.title, "ASSIGNMENT", assignment.id.toString(), firstStep = assignment.description)
                                                    })
                                                    Destination.STUDY -> StudyScreen(life, model, focus)
                                                    Destination.FITNESS -> GymScreen(life, onLogTraining = { trainingSplit = it })
                                                    Destination.TASKS -> TasksMatrixScreen(life, onStartFocus = { task -> model.begin(focus, task.title, "TASK", task.id.toString()) })
                                                    Destination.PROJECTS -> ProjectsScreen(model, focus)
                                                    Destination.CAPTURE -> CaptureScreen(model)
                                                    Destination.EXAMS -> ExamPrepScreen(life, model, focus, now)
                                                    Destination.PRACTICE -> PracticeScreen(model, focus, now)
                                                    Destination.REVIEW -> ReviewScreen(life, model, focus, now)
                                                    Destination.RESET -> DailyResetScreen(life, onFinishReset = { navigate(Destination.TODAY) })
                                                    Destination.SETTINGS -> SettingsScreen(life, { navigate(Destination.fromId(previous) ?: Destination.TODAY) }, workspace = model, onPreview = { preview = true },
                                                        onWipeWorkspace = { model.clearForUserWipe(focus) }, canWipeWorkspace = !savingSession)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        AnimatedVisibility(session.phase != FocusPhase.IDLE && !launcher, modifier = Modifier.align(Alignment.BottomCenter),
                            enter = fadeIn(tween(if (reduced) 0 else 220)) + expandVertically(tween(if (reduced) 0 else 300)),
                            exit = fadeOut(tween(if (reduced) 0 else 140))) {
                            BackdropGlass(Modifier.navigationBarsPadding().padding(start = 22.dp, end = 22.dp, bottom = dockReserve)
                                .fillMaxWidth().onSizeChanged { capsuleHeight = with(density) { it.height.toDp() } }
                                .clickable { panel = true }, radius = 24) {
                                Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(session.title, color = colors.textPrimary, fontSize = 13.sp, maxLines = 1)
                                        Text(if (session.phase == FocusPhase.FINISHED) "Ready to review" else if (session.phase == FocusPhase.PAUSED) "Paused · tap to continue" else "In focus · tap to expand", color = colors.primaryAccent, fontSize = 11.sp)
                                    }
                                    val seconds = session.remainingSeconds(now)
                                    Text("%02d:%02d".format(seconds / 60, seconds % 60), color = colors.textPrimary, fontSize = 20.sp)
                                }
                            }
                        }
                        WorkspaceDock(slots, destination, launcher, { launcher = it }, navigate)
                        val lecture by life.selectedLectureForDetail.collectAsStateWithLifecycle()
                        val attendance by life.attendanceRecords.collectAsStateWithLifecycle()
                        LaunchedEffect(lecture?.id) { if (lecture != null) { tools = false; preview = false } }
                        if (lecture != null) LectureDetailSheet(lecture, attendanceRecords = attendance, onDismiss = life::closeLectureDetail,
                            onRecordAttendance = life::recordAttendance)
                        message?.let { text ->
                            AlertDialog(onDismissRequest = model::clearMessage, title = { Text("Your workspace") }, text = { Text(text) },
                                confirmButton = { TextButton(onClick = model::clearMessage) { Text("OK") } })
                        }
                        if (panel && session.phase != FocusPhase.IDLE) SessionPanel(focus, now, { model.resumeSession(focus) }) { panel = false }
                        trainingSplit?.let { split ->
                            RecordDialog("Training · $split", listOf("Actual duration in minutes", "Training note / next time"), onDismiss = { trainingSplit = null }) {
                                model.recordTraining(split, it[0].toIntOrNull() ?: 0, it[1])
                            }
                        }
                        if (tools || preview) Dialog(onDismissRequest = { tools = false; preview = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                            Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
                                TextButton(onClick = { tools = false; preview = false }) { Text("Back to your live workspace") }
                                Box(Modifier.weight(1f)) {
                                    if (preview) TodayDesignPreview(life, legacyNavigate, onOpenSettings = { preview = false; navigate(Destination.SETTINGS) })
                                    else TodayToolsScreen(life, { tools = false; legacyNavigate(it) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
