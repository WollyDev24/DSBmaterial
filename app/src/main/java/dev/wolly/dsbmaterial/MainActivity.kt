package dev.wolly.dsbmaterial

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.wolly.dsbmaterial.data.SubstitutionEntry
import dev.wolly.dsbmaterial.ui.MainViewModel
import dev.wolly.dsbmaterial.ui.UiState
import dev.wolly.dsbmaterial.ui.theme.DSBMaterialTheme
import dev.wolly.dsbmaterial.ui.theme.themePresets

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
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
            DSBMaterialTheme(
                themeIndex = themeIndex,
                dynamicColor = dynamicColor
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
    
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var selectedDay by remember { mutableStateOf<String?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var cardRect by remember { mutableStateOf(Rect.Zero) }
    var isDismissing by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showDebug by remember { mutableStateOf(false) }
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

    val showNavCondition by remember(showThemePicker, showAbout, uiState, pagerState.currentPage) {
        derivedStateOf {
            !showThemePicker && !showAbout && uiState !is UiState.NeedsLogin && uiState !is UiState.Loading && uiState !is UiState.SelectingClass
        }
    }

    BackHandler(enabled = showSheet || showThemePicker || showAbout || uiState is UiState.SelectingClass || pagerState.currentPage != 0) {
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
        } else if (showAbout) {
            showAbout = false
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
            exit = slideOutHorizontally { -it } + fadeOut(tween(200))
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
                            pagerState.currentPage == 1 -> stringResource(R.string.label_archive)
                            pagerState.currentPage == 2 -> stringResource(R.string.title_settings)
                            else -> stringResource(R.string.title_main)
                        },
                        collapseFraction = collapseFraction,
                        actions = {
                            if (!showThemePicker) {
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
                                        modifier = Modifier.nestedScroll(scrollTrackerConnection),
                                        onDayClick = { day, bounds ->
                                            selectedDay = day
                                            cardRect = bounds
                                            showSheet = true
                                        }
                                   )
                                   
                                   if (showSheet && selectedDay != null) {
                                       val dayEntries = currentUiState.entries.filter { it.day == selectedDay }
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
                            } else if (currentUiState is UiState.Error) {
                                ErrorScreen(currentUiState.message, onRetry = { viewModel.fetchData() })
                            } else {
                                Box(Modifier.fillMaxSize())
                            }
                        }
                        1 -> ArchiveScreen(archiveEntries, isRoomFirst, onRemove = viewModel::removeFromArchive, modifier = Modifier.nestedScroll(scrollTrackerConnection))
                        2 -> SettingsScreen(
                            isRoomFirst = isRoomFirst,
                            sortByPeriod = sortByPeriod,
                            dynamicColor = dynamicColor,
                            navHidden = navHidden,
                            onToggleOrder = viewModel::toggleColumnOrder,
                            onToggleSort = viewModel::toggleSortByPeriod,
                            onToggleDynamic = viewModel::toggleDynamicColor,
                            onToggleNavHidden = viewModel::toggleNavHidden,
                            onOpenThemePicker = { showThemePicker = true },
                            onChangeClass = viewModel::changeClass,
                            onLogout = viewModel::logout,
                            onAbout = { showAbout = true },
                            modifier = Modifier.nestedScroll(scrollTrackerConnection)
                        )
                        }
                    }

            OverlayContent(
                showThemePicker = showThemePicker,
                showAbout = showAbout,
                showDebug = showDebug,
                uiState = uiState,
                themeIndex = themeIndex,
                dynamicColor = dynamicColor,
                onSelectTheme = { viewModel.setThemeIndex(it) },
                onCloseThemePicker = { showThemePicker = false },
                onCloseAbout = { showAbout = false },
                onOpenDebug = { showAbout = false; showDebug = true },
                onCloseDebug = { showDebug = false },
                onSelectClass = { u, p, cls -> viewModel.selectClass(u, p, cls) },
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
fun ArchiveScreen(entries: List<SubstitutionEntry>, isRoomFirst: Boolean, onRemove: (SubstitutionEntry) -> Unit, modifier: Modifier = Modifier) {
    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.8f, animationSpec = tween(400))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(16.dp))
                    Text("No archived substitutions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    } else {
        val grouped = entries.groupBy { it.day }
        LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            grouped.forEach { (day, dayEntries) ->
                item {
                    Text(day, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp).animateItem(fadeInSpec = tween(500)))
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
    onToggleOrder: () -> Unit,
    onToggleSort: () -> Unit,
    onToggleDynamic: () -> Unit,
    onToggleNavHidden: () -> Unit,
    onOpenThemePicker: () -> Unit,
    onChangeClass: () -> Unit,
    onLogout: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp).animateItem(fadeInSpec = tween(500))
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
        modifier = Modifier.fillMaxWidth().animateContentSize().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
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
    showAbout: Boolean,
    showDebug: Boolean,
    uiState: UiState,
    themeIndex: Int,
    dynamicColor: Boolean,
    onSelectTheme: (Int) -> Unit,
    onCloseThemePicker: () -> Unit,
    onCloseAbout: () -> Unit,
    onOpenDebug: () -> Unit,
    onCloseDebug: () -> Unit,
    onSelectClass: (String, String, String) -> Unit,
    onLogin: (String, String) -> Unit,
    onLoginDemo: () -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = showThemePicker || showAbout || showDebug || uiState is UiState.NeedsLogin || uiState is UiState.Loading || uiState is UiState.SelectingClass,
        enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300)),
        exit = fadeOut(tween(250)) + scaleOut(targetScale = 0.92f, animationSpec = tween(250))
    ) {
        when {
            showThemePicker -> ThemePickerScreen(
                currentIndex = themeIndex,
                dynamicColor = dynamicColor,
                onSelect = onSelectTheme,
                onBack = onCloseThemePicker
            )
            showAbout -> AboutScreen(onBack = onCloseAbout, onDebugTap = onOpenDebug)
            showDebug -> DebugModeScreen(onBack = onCloseDebug)
            uiState is UiState.Loading -> LoadingScreen()
            uiState is UiState.SelectingClass -> {
                val s = uiState
                ClassSelectionScreen(
                    classes = s.classes,
                    onClassSelected = { cls -> onSelectClass(s.u, s.p, cls) }
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
fun ClassSelectionScreen(classes: List<String>, onClassSelected: (String) -> Unit) {
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
            items(classes, key = { it }) { cls ->
                Surface(
                    onClick = { onClassSelected(cls) },
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.animateItem(fadeInSpec = tween(500)).fillMaxWidth()
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

@Composable
fun DayList(entries: List<SubstitutionEntry>, onDayClick: (String, Rect) -> Unit, selectedDay: String? = null, cardAlpha: Float = 1f, modifier: Modifier = Modifier) {
    val dayData = remember(entries) {
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
        distinctDays to counts
    }
    val days = dayData.first
    val dayCounts = dayData.second
    
    if (days.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.msg_no_substitutions))
        }
    } else {
        val isTablet = isExpandedScreen()
        val pad = remember(isTablet) { PaddingValues(if (isTablet) 20.dp else 20.dp) }
        val spacing = remember(isTablet) { if (isTablet) 16.dp else 24.dp }

        if (isTablet) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(280.dp),
                modifier = modifier.fillMaxSize(),
                contentPadding = pad,
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                items(count = days.size, key = { days[it] }) { index ->
                    val day = days[index]
                    val isOrigin = isTablet && selectedDay != null && day == selectedDay
                    val cardBounds = remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }
                    Box(
                        modifier = Modifier
                            .animateItem(fadeInSpec = tween(500))
                            .fillMaxSize()
                            .alpha(if (isOrigin) cardAlpha else 1f)
                            .onGloballyPositioned { cardBounds.value = it.boundsInRoot() },
                        contentAlignment = Alignment.TopStart
                    ) {
                        StaggeredFadeIn(index) {
                            DayCard(day, dayCounts[day] ?: 0) {
                                if (!isOrigin) onDayClick(day, cardBounds.value)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(120.dp)) }
            }
        } else {
            LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = pad, verticalArrangement = Arrangement.spacedBy(spacing)) {
                itemsIndexed(days, key = { _, it -> it }) { index, day ->
                    StaggeredFadeIn(index) {
                        Box(modifier = Modifier.animateItem(fadeInSpec = tween(500))) {
                            DayCard(day, dayCounts[day] ?: 0) { onDayClick(day, Rect(0f, 0f, 0f, 0f)) }
                        }
                    }
                }
                item { Spacer(Modifier.height(120.dp)) }
            }
        }
    }
}

@Composable
private fun DayCard(day: String, count: Int, onDayClick: (String) -> Unit) {
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isPressed) 0.12f else 0.08f)),
        interactionSource = interactionSource,
        onClick = { }
    ) {
        Column(modifier = Modifier.padding(dp(16.dp, 24.dp))) {
            Text(text = day, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (count == 1) stringResource(R.string.format_substitutions_count_one, count)
                       else stringResource(R.string.format_substitutions_count_many, count),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    val headerFontSize by animateFloatAsState(
        targetValue = if (isExpanded) 36f else 22f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "header_font_size"
    )
    val containerPadding by animateDpAsState(
        targetValue = if (isExpanded) 8.dp else 12.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "container_padding"
    )
    val textPaddingStart by animateDpAsState(
        targetValue = if (isExpanded) 12.dp else 16.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "text_padding_start"
    )
    val textPaddingTop by animateDpAsState(
        targetValue = if (isExpanded) 8.dp else 16.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "text_padding_top"
    )

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = containerPadding)) {
        Text(
            text = day,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = headerFontSize.sp,
                lineHeight = (headerFontSize * 1.2f).sp
            ),
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = textPaddingStart, top = textPaddingTop, bottom = 16.dp)
        )
        
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
                Box(modifier = Modifier.animateItem(fadeInSpec = tween(500))) {
                    SubstitutionTableRow(entry, isRoomFirst)
                }
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

@Composable
fun StaggeredFadeIn(index: Int, delayMs: Int = 80, content: @Composable () -> Unit) {
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(400, delayMillis = index * delayMs),
        label = "staggered_alpha"
    )
    Box(modifier = Modifier.graphicsLayer { this.alpha = alpha }) { content() }
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
