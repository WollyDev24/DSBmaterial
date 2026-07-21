package dev.wolly.dsbmaterial.ui

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.wolly.dsbmaterial.AutoFetchWorker
import dev.wolly.dsbmaterial.DSBWidget
import dev.wolly.dsbmaterial.api.DSBMobileAPI
import dev.wolly.dsbmaterial.data.DataStoreManager
import dev.wolly.dsbmaterial.data.SubstitutionEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Stable
sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val entries: List<SubstitutionEntry>) : UiState()
    data class Error(val message: String) : UiState()
    object NeedsLogin : UiState()
    data class SelectingClass(val classes: List<String>, val u: String, val p: String) : UiState()
}

@Stable
data class StatsData(
    val totalEntries: Int = 0,
    val totalDays: Int = 0,
    val mostCancelledSubject: String = "",
    val mostCancelledCount: Int = 0,
    val typeBreakdown: List<Pair<String, Int>> = emptyList(),
    val busiestLesson: String = "",
    val busiestLessonCount: Int = 0,
    val classCount: Int = 0,
    val subjectCount: Int = 0,
    val roomCount: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStoreManager = DataStoreManager(application)
    private val gson = Gson()
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    val isRoomFirst: StateFlow<Boolean> = dataStoreManager.swapDataFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dynamicColor: StateFlow<Boolean> = dataStoreManager.dynamicColorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val sortByPeriod: StateFlow<Boolean> = dataStoreManager.sortPeriodFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val themeIndex: StateFlow<Int> = dataStoreManager.themeIndexFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val navHidden: StateFlow<Boolean> = dataStoreManager.navHiddenFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val useCustomFont: StateFlow<Boolean> = dataStoreManager.useCustomFontFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val fontWeight: StateFlow<Float> = dataStoreManager.fontWeightFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 400f)

    val fontWidth: StateFlow<Float> = dataStoreManager.fontWidthFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100f)

    val fontOpsz: StateFlow<Float> = dataStoreManager.fontOpszFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 14f)

    val fontSlnt: StateFlow<Float> = dataStoreManager.fontSlntFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val fontGrad: StateFlow<Float> = dataStoreManager.fontGradFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val fontRond: StateFlow<Float> = dataStoreManager.fontRondFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    private val _archive = MutableStateFlow<List<SubstitutionEntry>>(emptyList())
    val archive: StateFlow<List<SubstitutionEntry>> = _archive

    private val _selectedClasses = MutableStateFlow<List<String>>(emptyList())
    val selectedClasses: StateFlow<List<String>> = _selectedClasses

    val autoFetchEnabled: StateFlow<Boolean> = dataStoreManager.autoFetchEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoFetchInterval: StateFlow<Int> = dataStoreManager.autoFetchIntervalFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    val notificationsEnabled: StateFlow<Boolean> = dataStoreManager.notificationsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _selectedCalendarDay = MutableStateFlow<String?>(null)
    val selectedCalendarDay: StateFlow<String?> = _selectedCalendarDay

    private var lastSuccessEntries: List<SubstitutionEntry> = emptyList()
    private var isDemoMode = false

    init {
        checkCredentialsAndFetch()
        loadArchive()
        loadSelectedClasses()
        scheduleAutoFetchOnStartup()
    }

    private fun loadArchive() {
        viewModelScope.launch {
            dataStoreManager.archiveFlow.collect { json ->
                if (!json.isNullOrEmpty()) {
                    val type = object : TypeToken<List<SubstitutionEntry>>() {}.type
                    val entries: List<SubstitutionEntry> = gson.fromJson(json, type)
                    _archive.value = sortArchive(entries)
                }
            }
        }
    }

    private fun loadSelectedClasses() {
        viewModelScope.launch {
            dataStoreManager.selectedClassesFlow.collect { json ->
                if (!json.isNullOrEmpty()) {
                    _selectedClasses.value = json.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }
            }
        }
    }

    private fun scheduleAutoFetchOnStartup() {
        viewModelScope.launch {
            val enabled = dataStoreManager.autoFetchEnabledFlow.first()
            val interval = dataStoreManager.autoFetchIntervalFlow.first()
            scheduleAutoFetch(enabled, interval)
        }
    }

    private fun parseDaySortKey(day: String): Long {
        val dateRegex = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")
        val match = dateRegex.find(day)
        if (match != null) {
            val (d, m, y) = match.destructured
            return y.toLong() * 10000 + m.toLong() * 100 + d.toLong()
        }
        val dayNames = listOf(
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
            "montag", "dienstag", "mittwoch", "donnerstag", "freitag", "samstag", "sonntag"
        )
        val index = dayNames.indexOfFirst { day.lowercase().startsWith(it) }
        if (index >= 0) return (index % 7).toLong() + 1
        return Long.MAX_VALUE
    }

    private fun sortArchive(entries: List<SubstitutionEntry>): List<SubstitutionEntry> {
        return entries.sortedWith(
            compareBy<SubstitutionEntry> { parseDaySortKey(it.day) }
                .thenBy { it.lesson.filter { c -> c.isDigit() }.toIntOrNull() ?: 999 }
        )
    }

    fun archiveSubstitutions(entries: List<SubstitutionEntry>? = null) {
        val toArchive = entries ?: lastSuccessEntries
        if (toArchive.isNotEmpty()) {
            viewModelScope.launch {
                val newArchive = (toArchive + _archive.value).distinctBy { 
                    it.day + it.lesson + it.subject + it.room + it.art + it.text 
                }
                val sortedArchive = sortArchive(newArchive)
                _archive.value = sortedArchive
                dataStoreManager.saveArchive(gson.toJson(sortedArchive))
                updateWidget()
            }
        }
    }

    fun removeFromArchive(entry: SubstitutionEntry) {
        viewModelScope.launch {
            val newArchive = _archive.value.filter { it != entry }
            _archive.value = newArchive
            dataStoreManager.saveArchive(gson.toJson(newArchive))
            updateWidget()
        }
    }

    fun clearArchive() {
        viewModelScope.launch {
            _archive.value = emptyList()
            dataStoreManager.saveArchive("")
            updateWidget()
        }
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    private fun updateWidget() {
        viewModelScope.launch {
            try {
                val manager = androidx.glance.appwidget.GlanceAppWidgetManager(getApplication())
                val glanceIds = manager.getGlanceIds(DSBWidget::class.java)
                glanceIds.forEach { glanceId ->
                    DSBWidget().update(getApplication(), glanceId)
                }
            } catch (_: Exception) {}
        }
    }

    fun setThemeIndex(index: Int) {
        viewModelScope.launch {
            dataStoreManager.saveThemeIndex(index)
            updateWidget()
        }
    }

    fun toggleColumnOrder() {
        viewModelScope.launch {
            dataStoreManager.saveSwapPreference(!isRoomFirst.value)
            updateWidget()
        }
    }

    fun toggleDynamicColor() {
        viewModelScope.launch {
            dataStoreManager.saveDynamicColorPreference(!dynamicColor.value)
            updateWidget()
        }
    }

    fun toggleNavHidden() {
        viewModelScope.launch {
            dataStoreManager.saveNavHiddenPreference(!navHidden.value)
        }
    }

    fun toggleCustomFont() {
        viewModelScope.launch {
            dataStoreManager.saveCustomFont(!useCustomFont.value)
        }
    }

    fun setFontWeight(value: Float) {
        viewModelScope.launch { dataStoreManager.saveFontWeight(value) }
    }

    fun setFontWidth(value: Float) {
        viewModelScope.launch { dataStoreManager.saveFontWidth(value) }
    }

    fun setFontOpsz(value: Float) {
        viewModelScope.launch { dataStoreManager.saveFontOpsz(value) }
    }

    fun setFontSlnt(value: Float) {
        viewModelScope.launch { dataStoreManager.saveFontSlnt(value) }
    }

    fun setFontGrad(value: Float) {
        viewModelScope.launch { dataStoreManager.saveFontGrad(value) }
    }

    fun setFontRond(value: Float) {
        viewModelScope.launch { dataStoreManager.saveFontRond(value) }
    }

    fun toggleAutoFetch() {
        viewModelScope.launch {
            val newValue = !autoFetchEnabled.value
            dataStoreManager.saveAutoFetchEnabled(newValue)
            scheduleAutoFetch(newValue, autoFetchInterval.value)
        }
    }

    fun setAutoFetchInterval(minutes: Int) {
        viewModelScope.launch {
            dataStoreManager.saveAutoFetchInterval(minutes)
            if (autoFetchEnabled.value) {
                scheduleAutoFetch(true, minutes)
            }
        }
    }

    fun toggleNotifications() {
        viewModelScope.launch {
            dataStoreManager.saveNotificationsEnabled(!notificationsEnabled.value)
        }
    }

    private fun scheduleAutoFetch(enabled: Boolean, intervalMinutes: Int) {
        val workManager = WorkManager.getInstance(getApplication())
        workManager.cancelUniqueWork(AutoFetchWorker.WORK_NAME)
        if (enabled) {
            val request = PeriodicWorkRequestBuilder<AutoFetchWorker>(
                intervalMinutes.toLong(), TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()
            workManager.enqueueUniquePeriodicWork(
                AutoFetchWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }

    fun selectCalendarDay(day: String?) {
        _selectedCalendarDay.value = day
    }

    fun getArchiveDays(): List<String> {
        return _archive.value.groupBy { it.day }.keys.sortedBy { parseDaySortKey(it) }
    }

    fun getArchiveEntriesForDay(day: String): List<SubstitutionEntry> {
        return _archive.value.filter { it.day == day }
    }

    fun getArchiveDates(): List<Pair<String, Int>> {
        return _archive.value
            .groupBy { it.day }
            .map { (day, entries) -> day to entries.size }
            .sortedBy { parseDaySortKey(it.first) }
    }

    fun getStatsData(): StatsData {
        val entries = _archive.value
        if (entries.isEmpty()) return StatsData()

        val totalEntries = entries.size
        val entriesByDay = entries.groupBy { it.day }
        val totalDays = entriesByDay.size

        val entriesBySubject = entries.groupBy { it.subject }
        val mostCancelledSubject = entriesBySubject.maxByOrNull { it.value.size }?.key ?: ""
        val mostCancelledCount = entriesBySubject.maxOfOrNull { it.value.size } ?: 0

        val entriesByType = entries.groupBy { it.art }
        val typeBreakdown = entriesByType.map { (type, list) -> type to list.size }
            .sortedByDescending { it.second }

        val entriesByLesson = entries.groupBy { it.lesson }
        val busiestLesson = entriesByLesson.maxByOrNull { it.value.size }?.key ?: ""
        val busiestLessonCount = entriesByLesson.maxOfOrNull { it.value.size } ?: 0

        val entriesByClass = entries.groupBy { it.className }
        val entriesByRoom = entries.groupBy { it.room }

        return StatsData(
            totalEntries = totalEntries,
            totalDays = totalDays,
            mostCancelledSubject = mostCancelledSubject,
            mostCancelledCount = mostCancelledCount,
            typeBreakdown = typeBreakdown,
            busiestLesson = busiestLesson,
            busiestLessonCount = busiestLessonCount,
            classCount = entriesByClass.size,
            subjectCount = entriesBySubject.size,
            roomCount = entriesByRoom.size
        )
    }

    fun toggleSortByPeriod() {
        viewModelScope.launch {
            dataStoreManager.saveSortPreference(!sortByPeriod.value)
            if (_uiState.value is UiState.Success) {
                _uiState.value = UiState.Success(sortEntries(lastSuccessEntries))
            }
        }
    }

    fun addSelectedClass(className: String) {
        if (className.isBlank()) return
        val trimmed = className.trim()
        if (_selectedClasses.value.contains(trimmed)) return
        viewModelScope.launch {
            val updated = _selectedClasses.value + trimmed
            _selectedClasses.value = updated
            dataStoreManager.saveSelectedClasses(updated)
            fetchData()
        }
    }

    fun removeSelectedClass(className: String) {
        viewModelScope.launch {
            val updated = _selectedClasses.value.filter { it != className }
            _selectedClasses.value = updated
            dataStoreManager.saveSelectedClasses(updated)
            fetchData()
        }
    }

    fun openSettings() {
        _selectedTab.value = 2
    }

    fun closeSettings() {
        _selectedTab.value = 0
        if (lastSuccessEntries.isNotEmpty()) {
            _uiState.value = UiState.Success(sortEntries(lastSuccessEntries))
        } else {
            checkCredentialsAndFetch()
        }
    }

    fun changeClass() {
        viewModelScope.launch {
            val u = dataStoreManager.usernameFlow.first() ?: ""
            val p = dataStoreManager.passwordFlow.first() ?: ""
            if (u.isNotEmpty() && p.isNotEmpty()) {
                fetchClasses(u, p)
            } else {
                _uiState.value = UiState.NeedsLogin
            }
        }
    }

    fun cancelClassSelection() {
        viewModelScope.launch {
            val className = dataStoreManager.classNameFlow.first() ?: ""
            if (className.isEmpty()) {
                _uiState.value = UiState.NeedsLogin
            } else {
                openSettings()
            }
        }
    }

    fun checkCredentialsAndFetch() {
        if (isDemoMode) {
            loginDemo()
            return
        }
        viewModelScope.launch {
            val username = dataStoreManager.usernameFlow.first()
            val password = dataStoreManager.passwordFlow.first()
            val className = dataStoreManager.classNameFlow.first() ?: ""

            if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
                _uiState.value = UiState.NeedsLogin
            } else if (className.isEmpty()) {
                fetchClasses(username, password)
            } else {
                fetchData(username, password, className)
            }
        }
    }

    fun login(username: String, password: String) {
        isDemoMode = false
        viewModelScope.launch {
            fetchClasses(username, password)
        }
    }

    fun loginDemo() {
        isDemoMode = true
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            delay(1000)
            val demoEntries = listOf(
                SubstitutionEntry("Montag", "Vertretung", "10a", "1 - 2", "Mathematik", "R101", "", "", "Lehrer krank", ""),
                SubstitutionEntry("Montag", "Entfall", "10a", "3", "Physik", "R102", "", "", "", ""),
                SubstitutionEntry("Dienstag", "Raumänderung", "10a", "5", "Englisch", "Turnhalle", "", "", "Wasserschaden in R105", ""),
                SubstitutionEntry("Mittwoch", "Vertretung", "10a", "4 - 5", "Geschichte", "R203", "", "", "", "")
            )
            lastSuccessEntries = demoEntries
            _uiState.value = UiState.Success(sortEntries(demoEntries))
            archiveSubstitutions(demoEntries)
        }
    }

    private suspend fun fetchClasses(u: String, p: String) {
        _uiState.value = UiState.Loading
        try {
            val api = DSBMobileAPI(u, p)
            val classes = api.getAvailableClasses()
            if (classes.isEmpty()) {
                _uiState.value = UiState.Error("No classes found. Check your credentials.")
            } else {
                _uiState.value = UiState.SelectingClass(classes, u, p)
            }
        } catch (e: Exception) {
            _uiState.value = UiState.Error(e.message ?: "Login failed")
        }
    }

    fun selectClass(username: String, password: String, className: String) {
        viewModelScope.launch {
            dataStoreManager.saveCredentials(username, password, className)
            fetchData(username, password, className)
        }
    }

    fun selectAllClasses(username: String, password: String) {
        viewModelScope.launch {
            dataStoreManager.saveCredentials(username, password, "")
            fetchData(username, password, "")
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            dataStoreManager.clearCredentials()
            _uiState.value = UiState.NeedsLogin
            _selectedTab.value = 0
            _selectedClasses.value = emptyList()
        }
    }

    fun fetchData() {
        viewModelScope.launch {
            val username = dataStoreManager.usernameFlow.first() ?: return@launch
            val password = dataStoreManager.passwordFlow.first() ?: return@launch
            val className = dataStoreManager.classNameFlow.first() ?: ""
            if (username.isEmpty() || password.isEmpty()) return@launch

            _isRefreshing.value = true
            try {
                val api = DSBMobileAPI(username, password)
                val allRaw = api.getSubstitutions("")

                val filtered = if (className.isEmpty() && _selectedClasses.value.isEmpty()) {
                    allRaw
                } else {
                    val allClassNames = mutableSetOf<String>()
                    if (className.isNotEmpty()) allClassNames.add(className)
                    allClassNames.addAll(_selectedClasses.value)
                    allRaw.filter { entry ->
                        allClassNames.any { cls -> entry.className.equals(cls, ignoreCase = true) }
                    }
                }

                val deduped = filtered.distinctBy { it.day + it.lesson + it.subject + it.room + it.art + it.text }
                lastSuccessEntries = deduped
                _uiState.value = UiState.Success(sortEntries(deduped))
                archiveSubstitutions(deduped)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun fetchData(u: String, p: String, c: String) {
        _uiState.value = UiState.Loading
        try {
            val api = DSBMobileAPI(u, p)
            val allRaw = api.getSubstitutions("")

            val filtered = if (c.isEmpty() && _selectedClasses.value.isEmpty()) {
                allRaw
            } else {
                val allClassNames = mutableSetOf<String>()
                if (c.isNotEmpty()) allClassNames.add(c)
                allClassNames.addAll(_selectedClasses.value)
                allRaw.filter { entry ->
                    allClassNames.any { cls -> entry.className.equals(cls, ignoreCase = true) }
                }
            }

            val deduped = filtered.distinctBy { it.day + it.lesson + it.subject + it.room + it.art + it.text }
            lastSuccessEntries = deduped
            _uiState.value = UiState.Success(sortEntries(deduped))
            archiveSubstitutions(deduped)
        } catch (e: Exception) {
            _uiState.value = UiState.Error(e.message ?: "Unknown error")
        }
    }

    private fun sortEntries(entries: List<SubstitutionEntry>): List<SubstitutionEntry> {
        val byDay = compareBy<SubstitutionEntry> { parseDaySortKey(it.day) }
        if (!sortByPeriod.value) return entries.sortedWith(byDay)
        return entries.sortedWith(
            byDay.thenBy { it.lesson.filter { c -> c.isDigit() }.toIntOrNull() ?: 999 }
        )
    }
}
