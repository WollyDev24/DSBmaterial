## Objective
Fix Kotlin compilation errors, visual regressions, and build failures in DSB_Material Android app.

## Important Details
- Build environment lacks Java/JDK — user runs `./gradlew app:compileDebugKotlin` externally.
- `uiState` is a delegated property (`val uiState by viewModel.uiState.collectAsState()`) — not smart-castable in `DSBApp`, but accessible as function param in `OverlayContent`.
- `UiState.SelectingClass` has `classes: List<String>`, `u: String`, `p: String`.
- XML resource files require `<?xml?>` declaration on line 1 — no comments or license headers before it.

## Work State
### Completed
- **Navbar position fix**: Wrapped bottom-nav `if` block in `Box(Modifier.fillMaxSize())` to enable `Modifier.align(Alignment.BottomCenter)`.
- **Duplicate overlay removed**: Removed dead `AnimatedVisibility` block (lines 467–489) that duplicated `OverlayContent`.
- **Transparent backgrounds**: Added `background(MaterialTheme.colorScheme.background)` to root composables of `ThemePickerScreen`, `AboutScreen`, `ClassSelectionScreen`, `LoginScreen`.
- **Immersive pages**: TopAppBar hidden when `showAbout` or `uiState is UiState.NeedsLogin` via conditional `if` wrapping the `topBar` slot (line ~345).
- **Icon XML wiring**: Reverted `ic_launcher.xml` / `ic_launcher_round.xml` to committed version (uses `@mipmap/ic_launcher_foreground` for foreground/monochrome, `@color/ic_launcher_background` as background color).
- **XML parsing fix**: Removed Apache license headers from `drawable/ic_launcher_background.xml`, `mipmap-anydpi-v26/ic_launcher.xml`, `mipmap-anydpi-v26/ic_launcher_round.xml` so `<?xml?>` is on line 1 (required by XML spec / AAPT2).
- **Braces**: 339 `{` and 339 `}` — balanced.

### Blocked
- Java/JDK unavailable — cannot run builds directly. User needs to rebuild externally.

## Next Move
Run `./gradlew clean && ./gradlew app:compileDebugKotlin` to clear stale cached XML copies and verify build.

## Relevant Files
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `app/src/main/res/drawable/ic_launcher_background.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/java/dev/wolly/dsbmaterial/MainActivity.kt`
