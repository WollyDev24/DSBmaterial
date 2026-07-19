package dev.wolly.dsbmaterial.ui

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.wolly.dsbmaterial.api.DSBMobileAPI
import dev.wolly.dsbmaterial.data.DataStoreManager
import dev.wolly.dsbmaterial.data.SubstitutionEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Stable
sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val entries: List<SubstitutionEntry>) : UiState()
    data class Error(val message: String) : UiState()
    object NeedsLogin : UiState()
    data class SelectingClass(val classes: List<String>, val u: String, val p: String) : UiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStoreManager = DataStoreManager(application)
    private val gson = Gson()
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab

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

    private val _archive = MutableStateFlow<List<SubstitutionEntry>>(emptyList())
    val archive: StateFlow<List<SubstitutionEntry>> = _archive

    private var lastSuccessEntries: List<SubstitutionEntry> = emptyList()
    private var isDemoMode = false

    init {
        checkCredentialsAndFetch()
        loadArchive()
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
            }
        }
    }

    fun removeFromArchive(entry: SubstitutionEntry) {
        viewModelScope.launch {
            val newArchive = _archive.value.filter { it != entry }
            _archive.value = newArchive
            dataStoreManager.saveArchive(gson.toJson(newArchive))
        }
    }

    fun clearArchive() {
        viewModelScope.launch {
            _archive.value = emptyList()
            dataStoreManager.saveArchive("")
        }
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    fun setThemeIndex(index: Int) {
        viewModelScope.launch {
            dataStoreManager.saveThemeIndex(index)
        }
    }

    fun toggleColumnOrder() {
        viewModelScope.launch {
            dataStoreManager.saveSwapPreference(!isRoomFirst.value)
        }
    }

    fun toggleDynamicColor() {
        viewModelScope.launch {
            dataStoreManager.saveDynamicColorPreference(!dynamicColor.value)
        }
    }

    fun toggleNavHidden() {
        viewModelScope.launch {
            dataStoreManager.saveNavHiddenPreference(!navHidden.value)
        }
    }

    fun toggleSortByPeriod() {
        viewModelScope.launch {
            dataStoreManager.saveSortPreference(!sortByPeriod.value)
            // Re-sort if we have data
            if (_uiState.value is UiState.Success) {
                _uiState.value = UiState.Success(sortEntries(lastSuccessEntries))
            }
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
            kotlinx.coroutines.delay(1000)
            val demoEntries = listOf(
                SubstitutionEntry("Monday", "Substitution", "10a", "1 - 2", "Math", "R101", "", "", "Teacher sick", ""),
                SubstitutionEntry("Monday", "Cancellation", "10a", "3", "Physics", "R102", "", "", "", ""),
                SubstitutionEntry("Tuesday", "Room Change", "10a", "5", "English", "Gym", "", "", "Water damage in R105", ""),
                SubstitutionEntry("Wednesday", "Substitution", "10a", "4 - 5", "History", "R203", "", "", "", "")
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
    
    fun logout() {
        viewModelScope.launch {
            dataStoreManager.clearCredentials()
            _uiState.value = UiState.NeedsLogin
            _selectedTab.value = 0
        }
    }

    fun fetchData() {
        checkCredentialsAndFetch()
    }

    private suspend fun fetchData(u: String, p: String, c: String) {
        _uiState.value = UiState.Loading
        try {
            val api = DSBMobileAPI(u, p)
            val entries = api.getSubstitutions(c)
            lastSuccessEntries = entries
            _uiState.value = UiState.Success(sortEntries(entries))
            // Auto-archive
            archiveSubstitutions(entries)
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
