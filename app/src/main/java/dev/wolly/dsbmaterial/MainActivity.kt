package dev.wolly.dsbmaterial

import android.content.Intent
import android.os.Bundle
import dev.wolly.dsbmaterial.BuildConfig
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.wolly.dsbmaterial.data.SubstitutionEntry
import dev.wolly.dsbmaterial.ui.MainViewModel
import dev.wolly.dsbmaterial.ui.StatsData
import dev.wolly.dsbmaterial.ui.UiState
import dev.wolly.dsbmaterial.ui.theme.DSBMaterialTheme
import dev.wolly.dsbmaterial.ui.theme.themePresets

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val dynamicColor by viewModel.dynamicColor.collectAsState()
            val themeIndex by viewModel.themeIndex.collectAsState()
            val useCustomFont by viewModel.useCustomFont.collectAsState()
            val fontWeight by viewModel.fontWeight.collectAsState()
            val fontWidth by viewModel.fontWidth.collectAsState()
            val fontOpsz by viewModel.fontOpsz.collectAsState()
            val fontSlnt by viewModel.fontSlnt.collectAsState()
            val fontGrad by viewModel.fontGrad.collectAsState()
            val fontRond by viewModel.fontRond.collectAsState()
            DSBMaterialTheme(
                themeIndex = themeIndex,
                dynamicColor = dynamicColor,
                useCustomFont = useCustomFont,
                fontWeight = fontWeight,
                fontWidth = fontWidth,
                fontOpsz = fontOpsz,
                fontSlnt = fontSlnt,
                fontGrad = fontGrad,
                fontRond = fontRond
            ) {
                DSBApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DSBApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val isRoomFirst by viewModel.isRoomFirst.collectAsState()
    val sortByPeriod by viewModel.sortByPeriod.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val themeIndex by viewModel.themeIndex.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val archiveEntries by viewModel.archive.collectAsState()
    val navHidden by viewModel.navHidden.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedClasses by viewModel.selectedClasses.collectAsState()
    val autoFetchEnabled by viewModel.autoFetchEnabled.collectAsState()
    val autoFetchInterval by viewModel.autoFetchInterval.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val useCustomFont by viewModel.useCustomFont.collectAsState()
    val fontWeight by viewModel.fontWeight.collectAsState()
    val fontWidth by viewModel.fontWidth.collectAsState()
    val fontOpsz by viewModel.fontOpsz.collectAsState()
    val fontSlnt by viewModel.fontSlnt.collectAsState()
    val fontGrad by viewModel.fontGrad.collectAsState()
    val fontRond by viewModel.fontRond.collectAsState()
    
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var selectedDay by remember { mutableStateOf<String?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var cardRect by remember { mutableStateOf(Rect.Zero) }
    var isDismissing by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showTypographyPicker by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showDebug by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var calendarSelectedDay by remember { mutableStateOf<String?>(null) }
    var collapseFraction by remember { mutableFloatStateOf(0f) }

    val scrollTrackerConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f) {
                    collapseFraction = (collapseFraction + kotlin.math.abs(available.y) / 150f).coerceIn(0f, 1f)
                } else if (available.y > 0f) {
                    collapseFraction = (collapseFraction - available.y / 150f).coerceIn(0f, 1f)
                }
                return Offset.Zero
            }
        }
    }

    val cardAlpha by animateFloatAsState(
        targetValue = when {
            isDismissing -> 1f
            selectedDay != null -> 0f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 400),
        label = "cardAlpha"
    )

    // Sync pager with viewModel
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }
    
    LaunchedEffect(pagerState.currentPage) {
        if (selectedTab != pagerState.currentPage) {
            viewModel.setTab(pagerState.currentPage)
        }
    }

    val isTablet = isExpandedScreen()

    val showNavCondition by remember(showThemePicker, showTypographyPicker, showAbout, showDebug, showCalendar, showStats, uiState, pagerState.currentPage) {
        derivedStateOf {
            !showThemePicker && !showTypographyPicker && !showAbout && !showDebug && !showCalendar && !showStats && uiState !is UiState.NeedsLogin && uiState !is UiState.Loading && uiState !is UiState.SelectingClass
        }
    }

    BackHandler(enabled = showSheet || showThemePicker || showTypographyPicker || showAbout || showDebug || showCalendar || showStats || uiState is UiState.SelectingClass || pagerState.currentPage != 0) {
        if (showSheet) {
            if (isTablet) {
                scope.launch {
                    isDismissing = true
                    delay(250)
                    showSheet = false
                    selectedDay = null
                    isDismissing = false
                }
            } else {
                showSheet = false
                selectedDay = null
            }
        } else if (showThemePicker) {
            showThemePicker = false
        } else if (showTypographyPicker) {
            showTypographyPicker = false
        } else if (showAbout) {
            showAbout = false
        } else if (showDebug) {
            showDebug = false
        } else if (showCalendar) {
            showCalendar = false
        } else if (showStats) {
            showStats = false
        } else if (uiState is UiState.SelectingClass) {
            viewModel.cancelClassSelection()
        } else {
            scope.launch { pagerState.scrollToPage(0) }
        }
    }

    Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        AnimatedVisibility(
            visible = isExpandedScreen() && showNavCondition,
            enter = slideInHorizontally { -it } + fadeIn(tween(300)),
            exit = slideOutHorizontally(tween(0)) { -it } + fadeOut(tween(0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(120.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                NavigationRail(
                    modifier = Modifier.fillMaxSize().padding(top = 48.dp)
                        .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                NavigationRailItem(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.scrollToPage(0) } },
                    icon = { Icon(Icons.Rounded.Home, contentDescription = stringResource(R.string.title_main), modifier = Modifier.size(32.dp)) },
                    label = { Text(stringResource(R.string.title_main), maxLines = 1, softWrap = false) },
                    alwaysShowLabel = true
                )
                Spacer(Modifier.height(16.dp))
                NavigationRailItem(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.scrollToPage(1) } },
                    icon = { Icon(Icons.Rounded.Archive, contentDescription = stringResource(R.string.label_archive), modifier = Modifier.size(32.dp)) },
                    label = { Text(stringResource(R.string.label_archive), maxLines = 1, softWrap = false) },
                    alwaysShowLabel = true
                )
                Spacer(Modifier.height(16.dp))
                NavigationRailItem(
                    selected = pagerState.currentPage == 2,
                    onClick = { scope.launch { pagerState.scrollToPage(2) } },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.title_settings), modifier = Modifier.size(32.dp)) },
                    label = { Text(stringResource(R.string.title_settings), maxLines = 1, softWrap = false) },
                    alwaysShowLabel = true
                )
                Spacer(Modifier.weight(1f))
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (!showAbout && uiState !is UiState.NeedsLogin) {
                    CollapsingTopBar(
                        title = when {
                            showThemePicker -> stringResource(R.string.label_theme_picker)
                            showTypographyPicker -> stringResource(R.string.label_typography)
                            pagerState.currentPage == 1 -> stringResource(R.string.label_archive)
                            pagerState.currentPage == 2 -> stringResource(R.string.title_settings)
                            else -> stringResource(R.string.title_main)
                        },
                        collapseFraction = collapseFraction,
                        actions = {
                            if (!showThemePicker && !showTypographyPicker) {
                                if (pagerState.currentPage == 0 && (uiState is UiState.Success || uiState is UiState.Idle)) {
                                    val isRefreshing = uiState is UiState.Loading
                                    val refreshRotation by animateFloatAsState(
                                        targetValue = if (isRefreshing) 360f else 0f,
                                        animationSpec = if (isRefreshing) infiniteRepeatable(
                                            animation = tween(1000, easing = LinearEasing)
                                        ) else tween(0),
                                        label = "refresh_rotation"
                                    )
                                    IconButton(onClick = { viewModel.fetchData() }) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = stringResource(R.string.action_refresh),
                                            modifier = Modifier.rotate(refreshRotation)
                                        )
                                    }
                                } else if (pagerState.currentPage == 1 && archiveEntries.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.clearArchive() }) {
                                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear archive")
                                    }
                                }
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = dp(640.dp, 840.dp))
                    ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                    when (page) {
                     0 -> { // Substitutions
                              val currentUiState = uiState
                              if (currentUiState is UiState.Success) {
                                     DayList(
                                        entries = currentUiState.entries,
                                        selectedDay = selectedDay,
                                        cardAlpha = cardAlpha,
                                        isRefreshing = isRefreshing,
                                        onRefresh = { viewModel.fetchData() },
                                        modifier = Modifier.nestedScroll(scrollTrackerConnection),
                                        onDayClick = { day, bounds ->
                                            selectedDay = day
                                            cardRect = bounds
                                            showSheet = true
                                        }
                                   )
                            } else if (currentUiState is UiState.Error) {
                                ErrorScreen(currentUiState.message, onRetry = { viewModel.fetchData() })
                            } else {
                                Box(Modifier.fillMaxSize())
                            }
                        }
                        1 -> ArchiveScreen(
                            archiveEntries,
                            isRoomFirst,
                            onRemove = viewModel::removeFromArchive,
                            onOpenCalendar = { showCalendar = true },
                            onOpenStats = { showStats = true },
                            modifier = Modifier.nestedScroll(scrollTrackerConnection)
                        )
                        2 -> SettingsScreen(
                            isRoomFirst = isRoomFirst,
                            sortByPeriod = sortByPeriod,
                            dynamicColor = dynamicColor,
                            navHidden = navHidden,
                            selectedClasses = selectedClasses,
                            autoFetchEnabled = autoFetchEnabled,
                            autoFetchInterval = autoFetchInterval,
                            notificationsEnabled = notificationsEnabled,
                            onToggleOrder = viewModel::toggleColumnOrder,
                            onToggleSort = viewModel::toggleSortByPeriod,
                            onToggleDynamic = viewModel::toggleDynamicColor,
                            onToggleNavHidden = viewModel::toggleNavHidden,
                            onOpenThemePicker = { showThemePicker = true },
                            onOpenTypographyPicker = { showTypographyPicker = true },
                            onToggleAutoFetch = viewModel::toggleAutoFetch,
                            onSetAutoFetchInterval = { viewModel.setAutoFetchInterval(it) },
                            onToggleNotifications = viewModel::toggleNotifications,
                            onChangeClass = viewModel::changeClass,
                            onLogout = viewModel::logout,
                            onAbout = { showAbout = true },
                            onAddClass = viewModel::addSelectedClass,
                            onRemoveClass = viewModel::removeSelectedClass,
                            modifier = Modifier.nestedScroll(scrollTrackerConnection)
                        )
                        }
                    }
                    }

            if (showSheet && selectedDay != null) {
                val currentState = uiState
                if (currentState is UiState.Success) {
                val dayEntries = currentState.entries.filter { it.day == selectedDay }
                if (isTablet) {
                    TabletSubstitutionPopup(
                        selectedDay = selectedDay!!,
                        entries = dayEntries,
                        isRoomFirst = isRoomFirst,
                        cardRect = cardRect,
                        onDismissStart = { isDismissing = true },
                        onDismiss = { showSheet = false; selectedDay = null; isDismissing = false }
                    )
                } else {
                    ModalBottomSheet(
                        onDismissRequest = { showSheet = false },
                        sheetState = sheetState,
                        shape = MaterialTheme.shapes.extraLarge,
                        dragHandle = {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            if (sheetState.currentValue == SheetValue.PartiallyExpanded) {
                                                sheetState.expand()
                                            } else {
                                                sheetState.partialExpand()
                                            }
                                        }
                                    }
                                    .padding(top = 16.dp, bottom = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                BottomSheetDefaults.DragHandle()
                            }
                        }
                    ) {
                        SubstitutionViewer(selectedDay!!, dayEntries, isRoomFirst, true)
                    }
                }
            }
            }

            OverlayContent(
                showThemePicker = showThemePicker,
                showTypographyPicker = showTypographyPicker,
                showAbout = showAbout,
                showDebug = showDebug,
                showCalendar = showCalendar,
                showStats = showStats,
                uiState = uiState,
                themeIndex = themeIndex,
                dynamicColor = dynamicColor,
                useCustomFont = useCustomFont,
                fontWeight = fontWeight,
                fontWidth = fontWidth,
                fontOpsz = fontOpsz,
                fontSlnt = fontSlnt,
                fontGrad = fontGrad,
                fontRond = fontRond,
                allArchiveEntries = archiveEntries,
                isRoomFirst = isRoomFirst,
                calendarEntries = archiveEntries.groupBy { it.day }.map { (day, entries) -> day to entries.size }.sortedBy { day ->
                    val dateRegex = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")
                    val match = dateRegex.find(day.first)
                    if (match != null) {
                        val (d, m, y) = match.destructured
                        y.toLong() * 10000 + m.toLong() * 100 + d.toLong()
                    } else Long.MAX_VALUE
                },
                statsData = remember(archiveEntries) {
                    if (archiveEntries.isEmpty()) StatsData()
                    else {
                        val entriesBySubject = archiveEntries.groupBy { it.subject }
                        val entriesByType = archiveEntries.groupBy { it.art }
                        StatsData(
                            totalEntries = archiveEntries.size,
                            totalDays = archiveEntries.groupBy { it.day }.size,
                            mostCancelledSubject = entriesBySubject.maxByOrNull { it.value.size }?.key ?: "",
                            mostCancelledCount = entriesBySubject.maxOfOrNull { it.value.size } ?: 0,
                            typeBreakdown = entriesByType.map { (type, list) -> type to list.size }.sortedByDescending { it.second },
                            busiestLesson = archiveEntries.groupBy { it.lesson }.maxByOrNull { it.value.size }?.key ?: "",
                            busiestLessonCount = archiveEntries.groupBy { it.lesson }.maxOfOrNull { it.value.size } ?: 0,
                            classCount = archiveEntries.groupBy { it.className }.size,
                            subjectCount = entriesBySubject.size,
                            roomCount = archiveEntries.groupBy { it.room }.size
                        )
                    }
                },
                calendarSelectedDay = calendarSelectedDay,
                onSelectTheme = { viewModel.setThemeIndex(it) },
                onCloseThemePicker = { showThemePicker = false },
                onCloseTypographyPicker = { showTypographyPicker = false },
                onToggleCustomFont = viewModel::toggleCustomFont,
                onFontWeightChange = { viewModel.setFontWeight(it) },
                onFontWidthChange = { viewModel.setFontWidth(it) },
                onFontOpszChange = { viewModel.setFontOpsz(it) },
                onFontSlntChange = { viewModel.setFontSlnt(it) },
                onFontGradChange = { viewModel.setFontGrad(it) },
                onFontRondChange = { viewModel.setFontRond(it) },
                onCloseAbout = { showAbout = false },
                onOpenDebug = { showAbout = false; showDebug = true },
                onCloseDebug = { showDebug = false },
                onCalendarDayClick = { calendarSelectedDay = if (calendarSelectedDay == it) null else it },
                onCloseCalendar = { showCalendar = false },
                onCloseStats = { showStats = false },
                onSelectClass = { u, p, cls -> viewModel.selectClass(u, p, cls) },
                onSelectAllClasses = { u, p -> viewModel.selectAllClasses(u, p) },
                onLogin = viewModel::login,
                onLoginDemo = viewModel::loginDemo
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
    if (!isExpandedScreen() && showNavCondition) {
        if (navHidden) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(if (isSelected) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 40.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                    .fillMaxWidth()
                    .height(dp(72.dp, 88.dp)),
                    shape = RoundedCornerShape(36.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 12.dp,
                    border = BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val itemWidth = maxWidth / 3
                        val indicatorOffset by animateDpAsState(
                            targetValue = itemWidth * pagerState.currentPage,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "indicator_offset"
                        )
                        Box(
                            modifier = Modifier
                                .offset(x = indicatorOffset)
                                .width(itemWidth)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(36.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        )
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val items = listOf(
                                Icons.Rounded.Home to R.string.title_main,
                                Icons.Rounded.Archive to R.string.label_archive,
                                Icons.Rounded.Settings to R.string.title_settings
                            )
                            items.forEachIndexed { index, (icon, labelRes) ->
                                val isSelected = pagerState.currentPage == index
                                val scale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.25f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
                                    label = "nav_scale_$index"
                                )
                                val iconTint by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    animationSpec = tween(300),
                                    label = "nav_icon_tint_$index"
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable(
                                            indication = null,
                                            interactionSource = null
                                        ) {
                                            scope.launch { pagerState.scrollToPage(index) }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = stringResource(labelRes),
                                        tint = iconTint,
                                        modifier = Modifier.size(26.dp).graphicsLayer(scaleX = scale, scaleY = scale)
                        )
                        }
                    }
                }
            }
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
fun CalendarViewScreen(
    archiveDates: List<Pair<String, Int>>,
    allArchiveEntries: List<SubstitutionEntry>,
    isRoomFirst: Boolean,
    selectedDay: String?,
    onDayClick: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(enabled = selectedDay != null) {
        onDayClick(selectedDay!!)
    }
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (selectedDay != null) {
                        onDayClick(selectedDay)
                    } else {
                        onBack()
                    }
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
                Text(
                    text = stringResource(R.string.label_calendar),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (selectedDay != null) {
                val dayEntries = allArchiveEntries.filter { it.day == selectedDay }
                SubstitutionViewer(selectedDay, dayEntries, isRoomFirst, true)
            } else {
                CalendarView(
                    archiveDates = archiveDates,
                    selectedDay = selectedDay,
                    isRoomFirst = isRoomFirst,
                    onDayClick = onDayClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun StatsViewScreen(
    statsData: StatsData,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
                Text(
                    text = stringResource(R.string.label_statistics),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            StatsScreen(
                statsData = statsData,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
@Composable
fun ThemePickerScreen(
    currentIndex: Int,
    dynamicColor: Boolean,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit
) {
    val themeNames = listOf(
        stringResource(R.string.theme_green),
        stringResource(R.string.theme_blue),
        stringResource(R.string.theme_purple),
        stringResource(R.string.theme_red),
        stringResource(R.string.theme_orange),
        stringResource(R.string.theme_cyan),
        stringResource(R.string.theme_pink)
    )

    val selectedTheme = themePresets.getOrNull(currentIndex)

    val animatedPrimary by animateColorAsState(
        targetValue = selectedTheme?.primary ?: Color.Transparent,
        animationSpec = tween(400),
        label = "preview_primary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = selectedTheme?.secondary ?: Color.Transparent,
        animationSpec = tween(400),
        label = "preview_secondary"
    )
    val animatedPrimaryContainer by animateColorAsState(
        targetValue = selectedTheme?.primaryContainer ?: Color.Transparent,
        animationSpec = tween(400),
        label = "preview_primary_container"
    )
    val animatedBg by animateColorAsState(
        targetValue = (selectedTheme?.primaryContainer ?: Color.Transparent).copy(alpha = 0.3f),
        animationSpec = tween(400),
        label = "preview_bg"
    )
    val animatedTextColor by animateColorAsState(
        targetValue = selectedTheme?.onPrimaryContainer ?: Color.Transparent,
        animationSpec = tween(400),
        label = "preview_text_color"
    )

    var screenEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { screenEntered = true }

    val springBounce = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedVisibility(
            visible = screenEntered,
            enter = slideInHorizontally(animationSpec = tween(400)) { -it } + fadeIn(tween(300)),
            label = "header_enter"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
                Text(
                    text = stringResource(R.string.label_theme_picker),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            AnimatedVisibility(
                visible = screenEntered,
                enter = fadeIn(tween(300, delayMillis = 120)) +
                    slideInVertically(animationSpec = tween(400, delayMillis = 120)) { -it / 2 },
                label = "preview_enter"
            ) {
                if (selectedTheme != null) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = animatedBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(dp(20.dp, 28.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                AnimatedContent(
                                    targetState = currentIndex,
                                    transitionSpec = {
                                        val dir = if (targetState > initialState) 1 else -1
                                        (slideInVertically { it * dir } + fadeIn(tween(300)))
                                            .togetherWith(slideOutVertically { -it * dir } + fadeOut(tween(200)))
                                    },
                                    label = "theme_name_transition"
                                ) { idx ->
                                    Text(
                                        text = themeNames.getOrElse(idx) { "Theme $idx" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = animatedTextColor
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.label_accent_color),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = animatedTextColor.copy(alpha = 0.7f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    Modifier
                                        .size(dp(32.dp, 44.dp))
                                        .clip(CircleShape)
                                        .background(animatedPrimary)
                                )
                                Box(
                                    Modifier
                                        .size(dp(32.dp, 44.dp))
                                        .clip(CircleShape)
                                        .background(animatedPrimaryContainer)
                                )
                                Box(
                                    Modifier
                                        .size(dp(32.dp, 44.dp))
                                        .clip(CircleShape)
                                        .background(animatedSecondary)
                                )
                            }
                        }
                    }
                }
            }

            if (!dynamicColor) {
                Spacer(Modifier.height(28.dp))

                AnimatedVisibility(
                    visible = screenEntered,
                    enter = fadeIn(tween(300, delayMillis = 200)) +
                        slideInVertically(animationSpec = tween(300, delayMillis = 200)) { it / 4 },
                    label = "grid_label_enter"
                ) {
                    Text(
                        text = stringResource(R.string.label_theme_picker),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(dp(72.dp, 100.dp)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = dp(320.dp, 500.dp)),
                    horizontalArrangement = Arrangement.spacedBy(dp(12.dp, 24.dp)),
                    verticalArrangement = Arrangement.spacedBy(dp(12.dp, 24.dp)),
                    userScrollEnabled = false
                ) {
                    items(count = themePresets.size, key = { it }) { index ->
                        ThemeSwatchItem(
                            index = index,
                            currentIndex = currentIndex,
                            springBounce = springBounce,
                            onSelect = onSelect
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))
            }

            Spacer(Modifier.height(28.dp))
        }

        AnimatedVisibility(
            visible = screenEntered,
            enter = slideInVertically(animationSpec = tween(400, delayMillis = 350)) { it } +
                fadeIn(tween(300, delayMillis = 350)),
            label = "apply_button_enter"
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dp(56.dp, 64.dp)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = stringResource(R.string.action_apply),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSwatchItem(
    index: Int,
    currentIndex: Int,
    springBounce: SpringSpec<Float>,
    onSelect: (Int) -> Unit
) {
    val theme = themePresets[index]
    val isSelected = currentIndex == index

    val selectPulse = remember { Animatable(1f) }
    LaunchedEffect(isSelected) {
        if (isSelected) {
            selectPulse.snapTo(1f)
            selectPulse.animateTo(
                0.85f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
            selectPulse.animateTo(
                1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press_scale"
    )

    var itemVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(250 + index * 60L)
        itemVisible = true
    }

    val entranceAlpha by animateFloatAsState(
        targetValue = if (itemVisible) 1f else 0f,
        animationSpec = tween(400),
        label = "entrance_alpha"
    )
    val entranceScale by animateFloatAsState(
        targetValue = if (itemVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 1f,
            stiffness = Spring.StiffnessLow
        ),
        label = "entrance_scale"
    )
    val displayScale = entranceScale * pressScale * selectPulse.value
    val indicatorScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "indicator_scale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                alpha = entranceAlpha
                scaleX = displayScale
                scaleY = displayScale
            }
            .clickable(
                indication = null,
                interactionSource = interactionSource
            ) { onSelect(index) },
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxSize().drawBehind {
            val s = size
            clipPath(Path().apply { addOval(Rect(Offset.Zero, s)) }) {
                clipRect(right = s.width / 2f) {
                    drawRect(color = theme.primary, size = s)
                }
                clipRect(left = s.width / 2f, bottom = s.height / 2f) {
                    drawRect(color = theme.secondary, size = s)
                }
                clipRect(left = s.width / 2f, top = s.height / 2f) {
                    drawRect(color = theme.primaryContainer, size = s)
                }
                if (indicatorScale > 0f) {
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.4f * indicatorScale),
                        radius = s.minDimension / 2f * indicatorScale
                    )
                    val r = s.minDimension / 2f * 0.3f * indicatorScale
                    drawCircle(
                        color = Color.White,
                        radius = r
                    )
                }
            }
        })
    }
}

@Composable
fun ArchiveScreen(
    entries: List<SubstitutionEntry>,
    isRoomFirst: Boolean,
    onRemove: (SubstitutionEntry) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.8f, animationSpec = tween(400))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.msg_no_substitutions), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    } else {
        val grouped = entries.groupBy { it.day }
        LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onOpenCalendar, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.large) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_open_calendar))
                    }
                    OutlinedButton(onClick = onOpenStats, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.large) {
                        Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_open_stats))
                    }
                }
            }
            grouped.forEach { (day, dayEntries) ->
                item {
                    Text(day, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                }
                items(dayEntries, key = { it.day + it.lesson + it.subject }) { entry ->
                    Card(
        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    ) {
                        Column {
                            Row(modifier = Modifier.padding(dp(20.dp, 28.dp)), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SubstitutionTableRowContent(entry, isRoomFirst)
                                }
                                IconButton(onClick = { onRemove(entry) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(120.dp)) }
        }
    }
}

@Composable
fun SettingsScreen(
    isRoomFirst: Boolean,
    sortByPeriod: Boolean,
    dynamicColor: Boolean,
    navHidden: Boolean,
    selectedClasses: List<String> = emptyList(),
    autoFetchEnabled: Boolean = false,
    autoFetchInterval: Int = 30,
    notificationsEnabled: Boolean = true,
    onToggleOrder: () -> Unit,
    onToggleSort: () -> Unit,
    onToggleDynamic: () -> Unit,
    onToggleNavHidden: () -> Unit,
    onOpenThemePicker: () -> Unit,
    onOpenTypographyPicker: () -> Unit,
    onToggleAutoFetch: () -> Unit = {},
    onSetAutoFetchInterval: (Int) -> Unit = {},
    onToggleNotifications: () -> Unit = {},
    onChangeClass: () -> Unit,
    onLogout: () -> Unit,
    onAbout: () -> Unit,
    onAddClass: (String) -> Unit = {},
    onRemoveClass: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var newClassName by remember { mutableStateOf("") }
    var showAddClassField by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
                Text(
                    stringResource(R.string.label_preferences),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )
            }

            item {
                SettingCard {
                    Column {
                        SettingItem(
                            title = stringResource(R.string.action_swap_data),
                            description = if (isRoomFirst) stringResource(R.string.desc_swap_default) else stringResource(R.string.desc_swap_active),
                            icon = Icons.Default.SwapHoriz,
                            isActive = !isRoomFirst,
                            trailing = { ExpressiveSwitch(checked = !isRoomFirst, onCheckedChange = { onToggleOrder() }) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        SettingItem(
                            title = stringResource(R.string.label_sort_period),
                            description = stringResource(R.string.desc_sort_period),
                            icon = Icons.Default.Sort,
                            isActive = sortByPeriod,
                            trailing = { ExpressiveSwitch(checked = sortByPeriod, onCheckedChange = { onToggleSort() }) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        SettingItem(
                            title = stringResource(R.string.label_dynamic_color),
                            description = stringResource(R.string.desc_dynamic_color),
                            icon = Icons.Default.Palette,
                            isActive = dynamicColor,
                            trailing = { ExpressiveSwitch(checked = dynamicColor, onCheckedChange = { onToggleDynamic() }) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        SettingItem(
                            title = "Minimal Nav Bar",
                            description = "Show dots instead of the nav bar",
                            icon = Icons.Default.Circle,
                            isActive = navHidden,
                            trailing = { ExpressiveSwitch(checked = navHidden, onCheckedChange = { onToggleNavHidden() }) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        SettingItem(
                            title = stringResource(R.string.label_typography),
                            description = stringResource(R.string.desc_custom_font),
                            icon = Icons.Default.FormatSize,
                            onClick = onOpenTypographyPicker
                        )
                        if (!dynamicColor) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                            SettingItem(
                                title = stringResource(R.string.label_theme_picker),
                                description = stringResource(R.string.desc_theme_picker),
                                icon = Icons.Default.ColorLens,
                                onClick = onOpenThemePicker
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.label_auto_fetch),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )
            }

            item {
                SettingCard {
                    Column {
                        SettingItem(
                            title = stringResource(R.string.label_auto_fetch),
                            description = stringResource(R.string.desc_auto_fetch),
                            icon = Icons.Default.Sync,
                            isActive = autoFetchEnabled,
                            trailing = { ExpressiveSwitch(checked = autoFetchEnabled, onCheckedChange = { onToggleAutoFetch() }) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        SettingItem(
                            title = stringResource(R.string.label_notifications),
                            description = stringResource(R.string.desc_notifications),
                            icon = Icons.Default.Notifications,
                            isActive = notificationsEnabled,
                            trailing = { ExpressiveSwitch(checked = notificationsEnabled, onCheckedChange = { onToggleNotifications() }) }
                        )
                        if (autoFetchEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                            var sliderValue by remember { mutableFloatStateOf(autoFetchInterval.toFloat()) }
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.label_fetch_interval), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.format_interval, sliderValue.toInt()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.height(8.dp))
                                Slider(
                                    value = sliderValue,
                                    onValueChange = { sliderValue = it },
                                    onValueChangeFinished = { onSetAutoFetchInterval(sliderValue.toInt()) },
                                    valueRange = 15f..120f,
                                    steps = 6
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.label_multi_class),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )
            }

            item {
                SettingCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.label_multi_class_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedClasses.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            selectedClasses.forEach { cls ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Class, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(cls, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { onRemoveClass(cls) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        if (showAddClassField) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = newClassName,
                                    onValueChange = { newClassName = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text(stringResource(R.string.label_class_hint)) },
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.large
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (newClassName.isNotBlank()) {
                                            onAddClass(newClassName)
                                            newClassName = ""
                                            showAddClassField = false
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        } else {
                            TextButton(onClick = { showAddClassField = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.label_add_class))
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.label_account),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )
            }

            item {
                SettingCard {
                    Column {
                        SettingItem(
                            title = stringResource(R.string.action_switch_class),
                            description = stringResource(R.string.desc_switch_class),
                            icon = Icons.Default.School,
                            onClick = onChangeClass
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        SettingItem(
                            title = stringResource(R.string.action_logout),
                            description = stringResource(R.string.desc_logout),
                            icon = Icons.Filled.Logout,
                            iconColor = MaterialTheme.colorScheme.error,
                            onClick = onLogout
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                SettingCard(onClick = onAbout) {
                    SettingItem(
                        title = stringResource(R.string.label_about),
                        description = stringResource(R.string.desc_about),
                        icon = Icons.Default.Info
                    )
                }
            }
            
            item { Spacer(Modifier.height(120.dp)) }
    }
}

@Composable
fun SettingCard(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        content = content
    )
}

@Composable
fun SettingItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    isActive: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val animatedIconBg by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                      else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        animationSpec = tween(300),
        label = "setting_icon_bg"
    )
    val animatedIconTint by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary
                      else iconColor,
        animationSpec = tween(300),
        label = "setting_icon_tint"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = MaterialTheme.shapes.large,
            color = animatedIconBg
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = animatedIconTint)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit, onDebugTap: () -> Unit = {}) {
    var tapCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(tapCount) {
        if (tapCount >= 7) {
            tapCount = 0
            onDebugTap()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.6f, animationSpec = tween(400))
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.size(140.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(400, delayMillis = 120)) + slideInVertically { it / 4 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.title_main), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(R.string.desc_about), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(24.dp))
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(400, delayMillis = 240)) + slideInVertically { it / 4 }
            ) {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "An open-source material you replacement for the DSBmobile app. Built with modern Jetpack Compose for a better user experience.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(24.dp),
                        lineHeight = 22.sp
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { tapCount++ }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugModeScreen(onBack: () -> Unit) {
    var showLoading by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Debug Mode", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
            HorizontalDivider(thickness = 0.5.dp)
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                item {
                    Text("Animations", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
                }
                item {
                    SettingCard(onClick = { showLoading = !showLoading }) {
                        SettingItem(
                            title = "Loading Animation",
                            description = if (showLoading) "Tap to hide" else "Tap to preview the loading screen",
                            icon = Icons.Default.Info
                        )
                    }
                }
                item {
                    SettingCard(onClick = { showError = !showError }) {
                        SettingItem(
                            title = "Error Screen",
                            description = if (showError) "Tap to hide" else "Tap to preview the error screen",
                            icon = Icons.Default.Warning
                        )
                    }
                }
                item {
                    Text("Components", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
                }
                item {
                    var switchState by remember { mutableStateOf(true) }
                    SettingCard(onClick = { switchState = !switchState }) {
                        SettingItem(
                            title = "Expressive Switch",
                            description = "Interactive toggle preview",
                            icon = Icons.Default.Settings,
                            trailing = { ExpressiveSwitch(checked = switchState, onCheckedChange = { switchState = !switchState }) }
                        )
                    }
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Type Badges", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf("Entfall" to Color(0xFFD32F2F), "Vertretung" to Color(0xFFFFA000), "Verlegung" to Color(0xFF1976D2), "Eigenvertretung" to Color(0xFF7B1FA2)).forEach { (label, color) ->
                                    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.15f)) {
                                        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf("Betreuung" to Color(0xFF388E3C), "Raumänderung" to Color(0xFF00796B)).forEach { (label, color) ->
                                    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.15f)) {
                                        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Typography Scale", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Headline Large", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                            Text("Title Medium", style = MaterialTheme.typography.titleMedium)
                            Text("Body Large", style = MaterialTheme.typography.bodyLarge)
                            Text("Body Medium", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Label Small", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
        if (showLoading) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { showLoading = false }) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Surface(modifier = Modifier.size(200.dp), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                        LoadingScreen()
                    }
                }
            }
        }
        if (showError) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { showError = false }) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Surface(modifier = Modifier.size(300.dp, 200.dp), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                        ErrorScreen(message = "This is a debug error message for testing.", onRetry = { showError = false })
                    }
                }
            }
        }
    }
}

@Composable
fun OverlayContent(
    showThemePicker: Boolean,
    showTypographyPicker: Boolean,
    showAbout: Boolean,
    showDebug: Boolean,
    showCalendar: Boolean,
    showStats: Boolean,
    uiState: UiState,
    themeIndex: Int,
    dynamicColor: Boolean,
    useCustomFont: Boolean,
    fontWeight: Float,
    fontWidth: Float,
    fontOpsz: Float,
    fontSlnt: Float,
    fontGrad: Float,
    fontRond: Float,
    calendarEntries: List<Pair<String, Int>>,
    allArchiveEntries: List<SubstitutionEntry>,
    isRoomFirst: Boolean,
    statsData: StatsData,
    calendarSelectedDay: String?,
    onSelectTheme: (Int) -> Unit,
    onCloseThemePicker: () -> Unit,
    onCloseTypographyPicker: () -> Unit,
    onToggleCustomFont: () -> Unit,
    onFontWeightChange: (Float) -> Unit,
    onFontWidthChange: (Float) -> Unit,
    onFontOpszChange: (Float) -> Unit,
    onFontSlntChange: (Float) -> Unit,
    onFontGradChange: (Float) -> Unit,
    onFontRondChange: (Float) -> Unit,
    onCloseAbout: () -> Unit,
    onOpenDebug: () -> Unit,
    onCloseDebug: () -> Unit,
    onCalendarDayClick: (String) -> Unit,
    onCloseCalendar: () -> Unit,
    onCloseStats: () -> Unit,
    onSelectClass: (String, String, String) -> Unit,
    onSelectAllClasses: (String, String) -> Unit,
    onLogin: (String, String) -> Unit,
    onLoginDemo: () -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = showThemePicker || showTypographyPicker || showAbout || showDebug || showCalendar || showStats || uiState is UiState.NeedsLogin || uiState is UiState.Loading || uiState is UiState.SelectingClass,
        enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300)),
        exit = fadeOut(tween(250)) + scaleOut(targetScale = 0.92f, animationSpec = tween(250))
    ) {
        when {
            showCalendar -> CalendarViewScreen(
                archiveDates = calendarEntries,
                allArchiveEntries = allArchiveEntries,
                isRoomFirst = isRoomFirst,
                selectedDay = calendarSelectedDay,
                onDayClick = onCalendarDayClick,
                onBack = onCloseCalendar
            )
            showStats -> StatsViewScreen(
                statsData = statsData,
                onBack = onCloseStats
            )
            showThemePicker -> ThemePickerScreen(
                currentIndex = themeIndex,
                dynamicColor = dynamicColor,
                onSelect = onSelectTheme,
                onBack = onCloseThemePicker
            )
            showTypographyPicker -> TypographyPickerScreen(
                useCustomFont = useCustomFont,
                fontWeight = fontWeight,
                fontWidth = fontWidth,
                fontOpsz = fontOpsz,
                fontSlnt = fontSlnt,
                fontGrad = fontGrad,
                fontRond = fontRond,
                onToggleCustomFont = onToggleCustomFont,
                onFontWeightChange = onFontWeightChange,
                onFontWidthChange = onFontWidthChange,
                onFontOpszChange = onFontOpszChange,
                onFontSlntChange = onFontSlntChange,
                onFontGradChange = onFontGradChange,
                onFontRondChange = onFontRondChange,
                onBack = onCloseTypographyPicker
            )
            showAbout -> AboutScreen(onBack = onCloseAbout, onDebugTap = onOpenDebug)
            showDebug -> DebugModeScreen(onBack = onCloseDebug)
            uiState is UiState.Loading -> LoadingScreen()
            uiState is UiState.SelectingClass -> {
                val s = uiState
                ClassSelectionScreen(
                    classes = s.classes,
                    onClassSelected = { cls -> onSelectClass(s.u, s.p, cls) },
                    onShowAll = { onSelectAllClasses(s.u, s.p) }
                )
            }
            uiState is UiState.NeedsLogin -> LoginScreen(onLogin = onLogin, onLoginDemo = onLoginDemo)
        }
    }
}

@Composable
fun LoadingScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) { index ->
                    val bounce by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = keyframes {
                                durationMillis = 1200
                                0f at 0 using FastOutSlowInEasing
                                -20f at 200 using FastOutSlowInEasing
                                0f at 400 using FastOutSlowInEasing
                                0f at 1200
                            },
                            repeatMode = RepeatMode.Restart,
                            initialStartOffset = StartOffset(index * 150)
                        ),
                        label = "bounce_$index"
                    )
                    val dotScale by infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 0.8f,
                        animationSpec = infiniteRepeatable(
                            animation = keyframes {
                                durationMillis = 1200
                                0.8f at 0 using FastOutSlowInEasing
                                1.2f at 200 using FastOutSlowInEasing
                                0.8f at 400 using FastOutSlowInEasing
                                0.8f at 1200
                            },
                            repeatMode = RepeatMode.Restart,
                            initialStartOffset = StartOffset(index * 150)
                        ),
                        label = "dot_scale_$index"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f + (index * 0.35f)),
                        animationSpec = tween(300),
                        label = "dot_color_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer {
                                translationY = bounce
                                scaleX = dotScale
                                scaleY = dotScale
                            }
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.msg_loading),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.8f, animationSpec = tween(400))
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

            AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(400, delayMillis = 120)) + slideInVertically { it / 4 }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.msg_error), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Text(message, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        Spacer(Modifier.height(32.dp))
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(400, delayMillis = 240)) + slideInVertically { it / 4 }
        ) {
            Button(onClick = onRetry, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit, onLoginDemo: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Detect if keyboard is open to scale the icon
    val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val iconSize by animateDpAsState(if (isKeyboardOpen) 60.dp else 100.dp, label = "icon_size")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = dp(32.dp, 48.dp), vertical = dp(48.dp, 64.dp)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(iconSize),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.School, 
                    contentDescription = null, 
                    modifier = Modifier.size(iconSize * 0.5f), 
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.title_login), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(40.dp))
        
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.label_username)) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.label_password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            singleLine = true
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = { onLogin(username, password) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text(stringResource(R.string.action_continue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onLoginDemo,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text(stringResource(R.string.label_demo_mode), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        // Extra space at bottom to ensure scrollability when keyboard is up
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ClassSelectionScreen(classes: List<String>, onClassSelected: (String) -> Unit, onShowAll: () -> Unit = {}) {
    var customClass by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.title_select_class), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.desc_select_class), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        
        OutlinedTextField(
            value = customClass,
            onValueChange = { customClass = it },
            label = { Text(stringResource(R.string.label_manual_class)) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            trailingIcon = {
                if (customClass.isNotEmpty()) {
                    IconButton(
                        onClick = { onClassSelected(customClass) },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.action_submit))
                    }
                }
            }
        )
        
        Spacer(Modifier.height(32.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Surface(
                    onClick = onShowAll,
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(R.string.label_show_all_classes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
            items(classes, key = { it }) { cls ->
                Surface(
                    onClick = { onClassSelected(cls) },
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Class, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(16.dp))
                        Text(cls, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayList(entries: List<SubstitutionEntry>, onDayClick: (String, Rect) -> Unit, selectedDay: String? = null, cardAlpha: Float = 1f, isRefreshing: Boolean = false, onRefresh: () -> Unit = {}, modifier: Modifier = Modifier) {
    var filterQuery by remember { mutableStateOf("") }

    val allDayData = remember(entries) {
        val filtered = entries.filter { day ->
            val lowerDay = day.day.lowercase()
            !lowerDay.contains("samstag") && !lowerDay.contains("sonntag") &&
            !lowerDay.contains("saturday") && !lowerDay.contains("sunday")
        }
        val distinctDays = filtered.map { it.day }.distinct()
        val counts = mutableMapOf<String, Int>()
        for (entry in filtered) {
            counts[entry.day] = (counts[entry.day] ?: 0) + 1
        }
        val allEntriesByDay = filtered.groupBy { it.day }
        Triple(distinctDays, counts, allEntriesByDay)
    }
    val days = allDayData.first
    val dayCounts = allDayData.second
    val allEntriesByDay = allDayData.third
    val classesByDay = remember(allDayData) {
        val filtered = entries.filter { day ->
            val lowerDay = day.day.lowercase()
            !lowerDay.contains("samstag") && !lowerDay.contains("sonntag") &&
            !lowerDay.contains("saturday") && !lowerDay.contains("sunday")
        }
        filtered.groupBy { it.day }.mapValues { (_, dayEntries) ->
            dayEntries.map { it.className }.filter { it.isNotEmpty() }.distinct()
        }
    }

    val filteredDays = remember(days, filterQuery, allEntriesByDay) {
        if (filterQuery.isBlank()) {
            days
        } else {
            val q = filterQuery.lowercase()
            days.filter { day ->
                allEntriesByDay[day]?.any { entry ->
                    entry.subject.lowercase().contains(q) ||
                    entry.room.lowercase().contains(q) ||
                    entry.art.lowercase().contains(q) ||
                    entry.text.lowercase().contains(q) ||
                    entry.lesson.lowercase().contains(q)
                } == true
            }
        }
    }

    val todayDayName = remember {
        val cal = Calendar.getInstance()
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Montag"
            Calendar.TUESDAY -> "Dienstag"
            Calendar.WEDNESDAY -> "Mittwoch"
            Calendar.THURSDAY -> "Donnerstag"
            Calendar.FRIDAY -> "Freitag"
            Calendar.SATURDAY -> "Samstag"
            Calendar.SUNDAY -> "Sonntag"
            else -> ""
        }
    }

    val todayDateStr = remember {
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)
        String.format("%02d.%02d.%04d", day, month, year)
    }

    val isToday = remember(days) {
        days.any { day ->
            val lower = day.lowercase()
            lower.startsWith(todayDayName.lowercase()) ||
            lower.contains(todayDateStr)
        }
    }

    val nextUpDay = remember(days, isToday, todayDayName, todayDateStr) {
        if (isToday) null
        else {
            val dayOrder = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
            val todayIndex = dayOrder.indexOfFirst { it.equals(todayDayName, ignoreCase = true) }
            if (todayIndex < 0) null
            else {
                val dateRegex = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")
                days.sortedBy { day ->
                    val match = dateRegex.find(day)
                    if (match != null) {
                        val (d, m, y) = match.destructured
                        y.toLong() * 10000 + m.toLong() * 100 + d.toLong()
                    } else {
                        val matchIndex = dayOrder.indexOfFirst { day.lowercase().startsWith(it.lowercase()) }
                        if (matchIndex >= 0) {
                            val adjustedIndex = if (matchIndex < todayIndex) matchIndex + 7 else matchIndex
                            adjustedIndex.toLong()
                        } else Long.MAX_VALUE
                    }
                }.firstOrNull()
            }
        }
    }

    if (days.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.msg_no_substitutions))
        }
    } else {
        val isTablet = isExpandedScreen()
        val pad = remember(isTablet) { PaddingValues(if (isTablet) 20.dp else 20.dp) }
        val spacing = remember(isTablet) { if (isTablet) 16.dp else 24.dp }

        val pullRefreshState = rememberPullToRefreshState()

        if (isTablet) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                state = pullRefreshState,
                modifier = modifier.fillMaxSize()
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(280.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = pad,
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    if (filterQuery.isBlank()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            FilterBar(filterQuery = filterQuery, onFilterChange = { filterQuery = it })
                        }
                    }
                    items(count = filteredDays.size, key = { filteredDays[it] }) { index ->
                        val day = filteredDays[index]
                        val isOrigin = isTablet && selectedDay != null && day == selectedDay
                        val cardBounds = remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }
                        val isCurrentDay = isToday && day.lowercase().startsWith(todayDayName.lowercase())
                        val isNext = !isCurrentDay && nextUpDay != null && day == nextUpDay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(if (isOrigin) cardAlpha else 1f)
                                .onGloballyPositioned { cardBounds.value = it.boundsInRoot() },
                            contentAlignment = Alignment.TopStart
                        ) {
                            DayCard(day, dayCounts[day] ?: 0, classes = classesByDay[day] ?: emptyList(), isToday = isCurrentDay, isNextUp = isNext) {
                                if (!isOrigin) onDayClick(day, cardBounds.value)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(120.dp)) }
                }
            }
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                state = pullRefreshState,
                modifier = modifier.fillMaxSize()
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = pad, verticalArrangement = Arrangement.spacedBy(spacing)) {
                    item {
                        FilterBar(filterQuery = filterQuery, onFilterChange = { filterQuery = it })
                    }
                    items(filteredDays, key = { it }) { day ->
                        val isCurrentDay = isToday && day.lowercase().startsWith(todayDayName.lowercase())
                        val isNext = !isCurrentDay && nextUpDay != null && day == nextUpDay
                        DayCard(day, dayCounts[day] ?: 0, classes = classesByDay[day] ?: emptyList(), isToday = isCurrentDay, isNextUp = isNext) { onDayClick(day, Rect(0f, 0f, 0f, 0f)) }
                    }
                    item { Spacer(Modifier.height(120.dp)) }
                }
            }
        }
    }
}

@Composable
fun FilterBar(filterQuery: String, onFilterChange: (String) -> Unit) {
    OutlinedTextField(
        value = filterQuery,
        onValueChange = onFilterChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.label_filter_hint)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingIcon = {
            if (filterQuery.isNotEmpty()) {
                IconButton(onClick = { onFilterChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun DayCard(day: String, count: Int, classes: List<String> = emptyList(), isToday: Boolean = false, isNextUp: Boolean = false, onDayClick: (String) -> Unit) {
    val isTablet = isExpandedScreen()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "daycard_press_scale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 1.dp else 4.dp,
        animationSpec = tween(200),
        label = "daycard_elevation"
    )
    Card(
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = when {
            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            isNextUp -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.primary.copy(alpha = if (isPressed) 0.12f else 0.08f)
        }),
        interactionSource = interactionSource,
        onClick = { }
    ) {
        Column(modifier = Modifier.padding(dp(16.dp, 24.dp))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = day, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                if (isToday) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = stringResource(R.string.label_today),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else if (isNextUp) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondary,
                    ) {
                        Text(
                            text = stringResource(R.string.label_next_up),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (count == 1) stringResource(R.string.format_substitutions_count_one, count)
                       else stringResource(R.string.format_substitutions_count_many, count),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (classes.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    classes.forEach { cls ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = cls,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(if (isTablet) 16.dp else 24.dp))
            Button(
                onClick = { onDayClick(day) },
                modifier = Modifier.fillMaxWidth().height(if (isTablet) 48.dp else dp(56.dp, 64.dp)),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.action_view_substitutions), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SubstitutionViewer(
    day: String, 
    entries: List<SubstitutionEntry>, 
    isRoomFirst: Boolean, 
    isExpanded: Boolean
) {
    val context = LocalContext.current
    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "expand_progress"
    )
    val headerFontSize = 22f + (36f - 22f) * expandProgress
    val containerPadding = lerp(12.dp, 8.dp, expandProgress)
    val textPaddingStart = lerp(16.dp, 12.dp, expandProgress)
    val textPaddingTop = lerp(16.dp, 8.dp, expandProgress)

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = containerPadding)) {
        Row(
            modifier = Modifier.padding(start = textPaddingStart, top = textPaddingTop, end = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = day,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = headerFontSize.sp,
                    lineHeight = (headerFontSize * 1.2f).sp
                ),
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                val shareText = buildShareText(day, entries, isRoomFirst)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_SUBJECT, day)
                }
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_share)))
            }) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.action_share), tint = MaterialTheme.colorScheme.primary)
            }
        }
        
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Row(modifier = Modifier.padding(vertical = 14.dp, horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                TableHeaderCell(stringResource(R.string.label_period_short), 1f)
                TableHeaderCell(stringResource(R.string.label_subject_short), 1.8f)
                TableHeaderCell(stringResource(R.string.label_room), 1.4f)
                TableHeaderCell(stringResource(R.string.label_type), 2f)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f), 
            contentPadding = PaddingValues(bottom = 120.dp, start = 8.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(entries, key = { it.day + it.lesson + it.subject }) { entry ->
                SubstitutionTableRow(entry, isRoomFirst)
            }
        }
    }
}

@Composable
private fun TabletSubstitutionPopup(
    selectedDay: String,
    entries: List<SubstitutionEntry>,
    isRoomFirst: Boolean,
    cardRect: Rect,
    onDismissStart: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val popAlpha = remember { Animatable(0f) }
    val popScale = remember { Animatable(0.85f) }

    LaunchedEffect(Unit) {
        launch { popAlpha.animateTo(1f, tween(durationMillis = 300)) }
        popScale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow))
    }

    fun dismiss() {
        scope.launch {
            onDismissStart()
            launch { popAlpha.animateTo(0f, tween(durationMillis = 200)) }
            popScale.animateTo(0.85f, tween(durationMillis = 250, easing = FastOutSlowInEasing))
            onDismiss()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (popAlpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f * popAlpha.value))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { dismiss() }
                    )
            )
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX = popScale.value
                    scaleY = popScale.value
                    alpha = popAlpha.value
                }
                .fillMaxWidth(0.7f)
                .widthIn(max = 480.dp)
                .heightIn(max = 400.dp)
                .clickable(enabled = false) {},
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                SubstitutionViewer(
                    day = selectedDay,
                    entries = entries,
                    isRoomFirst = isRoomFirst,
                    isExpanded = true
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { dismiss() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun RowScope.TableHeaderCell(text: String, weight: Float) {
    Text(
        text = text, 
        modifier = Modifier.weight(weight), 
        style = MaterialTheme.typography.labelMedium, 
        fontWeight = FontWeight.ExtraBold, 
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        letterSpacing = 1.sp
    )
}

@Composable
fun SubstitutionTableRow(
    entry: SubstitutionEntry, 
    isRoomFirst: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        )
    ) {
        SubstitutionTableRowContent(entry, isRoomFirst)
    }
}

@Composable
fun SubstitutionTableRowContent(
    entry: SubstitutionEntry,
    isRoomFirst: Boolean
) {
    val roomDisplay = if (isRoomFirst) entry.room else entry.art
    val typeDisplay = if (isRoomFirst) entry.art else entry.room

    Column(modifier = Modifier.padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TableCell(entry.lesson, 1f, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
            TableCell(entry.subject, 1.8f, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            TableCell(roomDisplay.ifEmpty { "—" }, 1.4f, fontWeight = FontWeight.Bold)
            val defaultTypeColor = MaterialTheme.colorScheme.secondary
            val typeColor = remember(typeDisplay, defaultTypeColor) {
                val lower = typeDisplay.lowercase()
                when {
                    lower.contains("entfall") -> Color(0xFFD32F2F)
                    lower.contains("vertretung") -> Color(0xFFF57C00)
                    lower.contains("verlegung") || lower.contains("verschiebung") -> Color(0xFF1976D2)
                    lower.contains("eigenvertretung") -> Color(0xFF7B1FA2)
                    lower.contains("betreuung") -> Color(0xFF388E3C)
                    lower.contains("raumänderung") || lower.contains("raum") -> Color(0xFF00838F)
                    else -> defaultTypeColor
                }
            }
            val typeBgColor = typeColor.copy(alpha = 0.12f)
            Box(
                modifier = Modifier
                    .weight(2f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(typeBgColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = typeDisplay,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = typeColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        if (entry.className.isNotEmpty()) {
            Text(
                text = entry.className,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        if (entry.text.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = entry.text,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.TableCell(
    text: String, 
    weight: Float, 
    fontWeight: FontWeight = FontWeight.Normal, 
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge
    ) {
    Text(
        text = text, 
        modifier = Modifier.weight(weight), 
        style = style, 
        fontWeight = fontWeight, 
        color = color, 
        maxLines = 2
    )
}

private fun buildShareText(day: String, entries: List<SubstitutionEntry>, isRoomFirst: Boolean): String {
    val sb = StringBuilder()
    sb.appendLine("📚 $day")
    sb.appendLine("─".repeat(20))
    for (entry in entries) {
        val roomDisplay = if (isRoomFirst) entry.room else entry.art
        val typeDisplay = if (isRoomFirst) entry.art else entry.room
        sb.appendLine("${entry.lesson} | ${entry.subject} | $roomDisplay | $typeDisplay")
        if (entry.text.isNotEmpty()) {
            sb.appendLine("  → ${entry.text}")
        }
    }
    sb.appendLine()
    sb.appendLine("DSBmaterial: https://github.com/wollydev24/DSBmaterial")
    return sb.toString()
}

@Composable
private fun isExpandedScreen(): Boolean {
    val configuration = LocalConfiguration.current
    return remember(configuration) { configuration.screenWidthDp >= 600 }
}

@Composable
private fun dp(compact: Dp, expanded: Dp): Dp {
    val isExpanded = isExpandedScreen()
    return remember(isExpanded, compact, expanded) { if (isExpanded) expanded else compact }
}

@Composable
fun CollapsingTopBar(
    title: String,
    collapseFraction: Float,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val expandedHeight = 64.dp
    val collapsedHeight = 48.dp
    val expandFraction = 1f - collapseFraction.coerceIn(0f, 1f)
    val currentHeight = ((expandedHeight.value * expandFraction) + (collapsedHeight.value * (1f - expandFraction))).dp
    val currentFontSize = 34f * expandFraction + 20f * (1f - expandFraction)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(currentHeight)
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontSize = currentFontSize.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = actions
            )
        }
    }
}

@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "thumb_offset"
    )
    val bgColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(300),
        label = "bg_color"
    )
    val thumbScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "thumb_scale"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(200),
        label = "icon_alpha"
    )
    val trackWidth = 56.dp
    val trackHeight = 32.dp
    val thumbSize = 24.dp
    val padding = 4.dp

    Box(
        modifier = modifier
            .size(trackWidth, trackHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable { onCheckedChange() },
        contentAlignment = Alignment.CenterStart
    ) {
        val thumbX by animateDpAsState(
            targetValue = if (checked) trackWidth - thumbSize - padding else padding,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "thumb_x"
        )
        Box(
            modifier = Modifier
                .offset(x = thumbX)
                .size(thumbSize)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (checked) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(14.dp).alpha(iconAlpha),
                tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
            )
        }
    }
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
@Composable
fun TypographyPickerScreen(
    useCustomFont: Boolean,
    fontWeight: Float,
    fontWidth: Float,
    fontOpsz: Float,
    fontSlnt: Float,
    fontGrad: Float,
    fontRond: Float,
    onToggleCustomFont: () -> Unit,
    onFontWeightChange: (Float) -> Unit,
    onFontWidthChange: (Float) -> Unit,
    onFontOpszChange: (Float) -> Unit,
    onFontSlntChange: (Float) -> Unit,
    onFontGradChange: (Float) -> Unit,
    onFontRondChange: (Float) -> Unit,
    onBack: () -> Unit
) {
    var localWeight by remember { mutableFloatStateOf(fontWeight) }
    var localWidth by remember { mutableFloatStateOf(fontWidth) }
    var localOpsz by remember { mutableFloatStateOf(fontOpsz) }
    var localSlnt by remember { mutableFloatStateOf(fontSlnt) }
    var localGrad by remember { mutableFloatStateOf(fontGrad) }
    var localRond by remember { mutableFloatStateOf(fontRond) }

    LaunchedEffect(fontWeight) { localWeight = fontWeight }
    LaunchedEffect(fontWidth) { localWidth = fontWidth }
    LaunchedEffect(fontOpsz) { localOpsz = fontOpsz }
    LaunchedEffect(fontSlnt) { localSlnt = fontSlnt }
    LaunchedEffect(fontGrad) { localGrad = fontGrad }
    LaunchedEffect(fontRond) { localRond = fontRond }

    var screenEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { screenEntered = true }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedVisibility(
            visible = screenEntered,
            enter = slideInHorizontally(animationSpec = tween(400)) { -it } + fadeIn(tween(300)),
            label = "header_enter"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
                Text(
                    text = stringResource(R.string.label_typography),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            AnimatedVisibility(
                visible = screenEntered,
                enter = fadeIn(tween(300, delayMillis = 120)) +
                    slideInVertically(animationSpec = tween(400, delayMillis = 120)) { it / 3 },
                label = "content_enter"
            ) {
                Column {
                    SettingCard {
                        SettingItem(
                            title = stringResource(R.string.label_custom_font),
                            description = stringResource(R.string.desc_custom_font),
                            icon = Icons.Default.FormatSize,
                            isActive = useCustomFont,
                            trailing = { ExpressiveSwitch(checked = useCustomFont, onCheckedChange = { onToggleCustomFont() }) }
                        )
                    }

                    if (useCustomFont) {
                        Spacer(Modifier.height(16.dp))

                        val previewFamily = remember(localWeight, localWidth, localOpsz, localSlnt, localGrad, localRond) {
                            FontFamily(
                                Font(
                                    resId = R.font.google_sans_flex,
                                    variationSettings = FontVariation.Settings(
                                        FontVariation.weight(localWeight.toInt()),
                                        FontVariation.width(localWidth / 100f),
                                        FontVariation.slant(localSlnt),
                                        FontVariation.Setting("opsz", localOpsz),
                                        FontVariation.Setting("GRAD", localGrad),
                                        FontVariation.Setting("ROND", localRond)
                                    )
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = stringResource(R.string.title_main),
                                    fontFamily = previewFamily,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.label_preview_font_body),
                                    fontFamily = previewFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Aa Bb Cc 123 !@#",
                                    fontFamily = previewFamily,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                                FontSlider(label = stringResource(R.string.label_font_weight), value = localWeight, valueRange = 100f..900f, steps = 7, displayValue = { v -> "${v.toInt()}" }, onValueChange = { localWeight = it }, onValueChangeFinished = { onFontWeightChange(localWeight) })
                                FontSlider(label = stringResource(R.string.label_font_width), value = localWidth, valueRange = 62.5f..100f, steps = 14, displayValue = { v -> "%.1f".format(v) }, onValueChange = { localWidth = it }, onValueChangeFinished = { onFontWidthChange(localWidth) })
                                FontSlider(label = stringResource(R.string.label_font_opsz), value = localOpsz, valueRange = 8f..144f, steps = 0, displayValue = { v -> "${v.toInt()}" }, onValueChange = { localOpsz = it }, onValueChangeFinished = { onFontOpszChange(localOpsz) })
                                FontSlider(label = stringResource(R.string.label_font_slnt), value = localSlnt, valueRange = -10f..0f, steps = 9, displayValue = { v -> "${v.toInt()}" }, onValueChange = { localSlnt = it }, onValueChangeFinished = { onFontSlntChange(localSlnt) })
                                FontSlider(label = stringResource(R.string.label_font_grad), value = localGrad, valueRange = -200f..150f, steps = 0, displayValue = { v -> "${v.toInt()}" }, onValueChange = { localGrad = it }, onValueChangeFinished = { onFontGradChange(localGrad) })
                                FontSlider(label = stringResource(R.string.label_font_rond), value = localRond, valueRange = 0f..100f, steps = 0, displayValue = { v -> "${v.toInt()}" }, onValueChange = { localRond = it }, onValueChangeFinished = { onFontRondChange(localRond) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FontSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayValue: (Float) -> String,
    onValueChangeFinished: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = displayValue(value),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
fun CalendarView(
    archiveDates: List<Pair<String, Int>>,
    selectedDay: String?,
    isRoomFirst: Boolean,
    onDayClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateRegex = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")

    val monthNamesDe = listOf("Januar", "Februar", "März", "April", "Mai", "Juni",
        "Juli", "August", "September", "Oktober", "November", "Dezember")

    val withDates = mutableListOf<Pair<Triple<Int, Int, Int>, Pair<String, Int>>>()
    val withoutDates = mutableListOf<Pair<String, Int>>()

    archiveDates.forEach { (day, count) ->
        val match = dateRegex.find(day)
        if (match != null) {
            val (d, m, y) = match.destructured
            withDates.add(Triple(m.toInt(), d.toInt(), y.toInt()) to (day to count))
        } else {
            withoutDates.add(day to count)
        }
    }

    val groupedByMonth = withDates.groupBy { it.first.first }.toSortedMap()

    if (archiveDates.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.label_no_calendar_data), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
            }
        }
    } else {
        LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            groupedByMonth.forEach { (month, entries) ->
                item {
                    val monthName = monthNamesDe.getOrElse(month - 1) { "" }
                    val year = entries.first().first.third
                    Text(
                        text = "$monthName $year",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                }
                items(entries.map { it.second }) { (day, count) ->
                    CalendarDayCard(day, count, isSelected = day == selectedDay, onClick = { onDayClick(day) })
                }
            }

            if (withoutDates.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.label_archive),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                }
                items(withoutDates) { (day, count) ->
                    CalendarDayCard(day, count, isSelected = day == selectedDay, onClick = { onDayClick(day) })
                }
            }

            item { Spacer(Modifier.height(120.dp)) }
        }
    }
}

@Composable
private fun CalendarDayCard(day: String, count: Int, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(day, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.format_substitutions_count_many, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StatsScreen(
    statsData: StatsData,
    modifier: Modifier = Modifier
) {
    if (statsData.totalEntries == 0) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.label_no_stats), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
            }
        }
    } else {
        LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(stringResource(R.string.label_statistics), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
            }

            item {
                SettingCard {
                    Column {
                        StatItem(icon = Icons.Default.FormatListBulleted, label = stringResource(R.string.stats_total_entries), value = statsData.totalEntries.toString())
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        StatItem(icon = Icons.Default.DateRange, label = stringResource(R.string.stats_total_days), value = statsData.totalDays.toString())
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        StatItem(icon = Icons.Default.Person, label = stringResource(R.string.stats_classes), value = statsData.classCount.toString())
                    }
                }
            }

            item {
                SettingCard {
                    Column {
                        StatItem(icon = Icons.Default.MenuBook, label = stringResource(R.string.stats_subjects), value = statsData.subjectCount.toString())
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        StatItem(icon = Icons.Default.MeetingRoom, label = stringResource(R.string.stats_rooms), value = statsData.roomCount.toString())
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.stats_most_cancelled), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
            }

            item {
                SettingCard {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(statsData.mostCancelledSubject, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text("${statsData.mostCancelledCount} entries", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.stats_busiest_lesson), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
            }

            item {
                SettingCard {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(statsData.busiestLesson, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text("${statsData.busiestLessonCount} entries", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            if (statsData.typeBreakdown.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.stats_type_breakdown), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
                }

                item {
                    SettingCard {
                        Column {
                            statsData.typeBreakdown.forEachIndexed { index, (type, count) ->
                                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val typeColor = when {
                                        type.lowercase().contains("entfall") -> MaterialTheme.colorScheme.error
                                        type.lowercase().contains("vertretung") -> Color(0xFFFFB74D)
                                        else -> MaterialTheme.colorScheme.secondary
                                    }
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(typeColor))
                                    Spacer(Modifier.width(12.dp))
                                    Text(type, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                    Text("$count", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                if (index < statsData.typeBreakdown.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(120.dp)) }
        }
    }
}

@Composable
fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
