package dev.wolly.dsbmaterial.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {
    companion object {
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
        val CLASS_NAME = stringPreferencesKey("class_name")
        val SWAP_DATA = booleanPreferencesKey("swap_data")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SORT_PERIOD = booleanPreferencesKey("sort_period")
        val ARCHIVE = stringPreferencesKey("archive")
        val THEME_INDEX = intPreferencesKey("theme_index")
        val NAV_HIDDEN = booleanPreferencesKey("nav_hidden")
        val SELECTED_CLASSES = stringPreferencesKey("selected_classes")
        val USE_CUSTOM_FONT = booleanPreferencesKey("use_custom_font")
        val FONT_WEIGHT = floatPreferencesKey("font_weight")
        val FONT_WIDTH = floatPreferencesKey("font_width")
        val FONT_OPSZ = floatPreferencesKey("font_opsz")
        val FONT_SLNT = floatPreferencesKey("font_slnt")
        val FONT_GRAD = floatPreferencesKey("font_grad")
        val FONT_ROND = floatPreferencesKey("font_rond")
    }

    val usernameFlow: Flow<String?> = context.dataStore.data.map { it[USERNAME] }
    val passwordFlow: Flow<String?> = context.dataStore.data.map { it[PASSWORD] }
    val classNameFlow: Flow<String?> = context.dataStore.data.map { it[CLASS_NAME] }
    val swapDataFlow: Flow<Boolean> = context.dataStore.data.map { it[SWAP_DATA] ?: true }
    val dynamicColorFlow: Flow<Boolean> = context.dataStore.data.map { it[DYNAMIC_COLOR] ?: true }
    val sortPeriodFlow: Flow<Boolean> = context.dataStore.data.map { it[SORT_PERIOD] ?: true }
    val archiveFlow: Flow<String?> = context.dataStore.data.map { it[ARCHIVE] }
    val themeIndexFlow: Flow<Int> = context.dataStore.data.map { it[THEME_INDEX] ?: 0 }
    val navHiddenFlow: Flow<Boolean> = context.dataStore.data.map { it[NAV_HIDDEN] ?: false }
    val selectedClassesFlow: Flow<String?> = context.dataStore.data.map { it[SELECTED_CLASSES] }
    val useCustomFontFlow: Flow<Boolean> = context.dataStore.data.map { it[USE_CUSTOM_FONT] ?: false }
    val fontWeightFlow: Flow<Float> = context.dataStore.data.map { it[FONT_WEIGHT] ?: 400f }
    val fontWidthFlow: Flow<Float> = context.dataStore.data.map { it[FONT_WIDTH] ?: 100f }
    val fontOpszFlow: Flow<Float> = context.dataStore.data.map { it[FONT_OPSZ] ?: 14f }
    val fontSlntFlow: Flow<Float> = context.dataStore.data.map { it[FONT_SLNT] ?: 0f }
    val fontGradFlow: Flow<Float> = context.dataStore.data.map { it[FONT_GRAD] ?: 0f }
    val fontRondFlow: Flow<Float> = context.dataStore.data.map { it[FONT_ROND] ?: 0f }

    suspend fun saveCredentials(username: String, password: String, className: String) {
        context.dataStore.edit { settings ->
            settings[USERNAME] = username
            settings[PASSWORD] = password
            settings[CLASS_NAME] = className
        }
    }

    suspend fun saveSelectedClasses(classes: List<String>) {
        context.dataStore.edit { it[SELECTED_CLASSES] = classes.joinToString(",") }
    }

    suspend fun saveSwapPreference(isRoomFirst: Boolean) {
        context.dataStore.edit { it[SWAP_DATA] = isRoomFirst }
    }

    suspend fun saveDynamicColorPreference(enabled: Boolean) {
        context.dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    suspend fun saveThemeIndex(index: Int) {
        context.dataStore.edit { it[THEME_INDEX] = index }
    }

    suspend fun saveSortPreference(enabled: Boolean) {
        context.dataStore.edit { it[SORT_PERIOD] = enabled }
    }

    suspend fun saveArchive(json: String) {
        context.dataStore.edit { it[ARCHIVE] = json }
    }
    
    suspend fun saveNavHiddenPreference(hidden: Boolean) {
        context.dataStore.edit { it[NAV_HIDDEN] = hidden }
    }

    suspend fun saveCustomFont(enabled: Boolean) {
        context.dataStore.edit { it[USE_CUSTOM_FONT] = enabled }
    }

    suspend fun saveFontWeight(value: Float) {
        context.dataStore.edit { it[FONT_WEIGHT] = value }
    }

    suspend fun saveFontWidth(value: Float) {
        context.dataStore.edit { it[FONT_WIDTH] = value }
    }

    suspend fun saveFontOpsz(value: Float) {
        context.dataStore.edit { it[FONT_OPSZ] = value }
    }

    suspend fun saveFontSlnt(value: Float) {
        context.dataStore.edit { it[FONT_SLNT] = value }
    }

    suspend fun saveFontGrad(value: Float) {
        context.dataStore.edit { it[FONT_GRAD] = value }
    }

    suspend fun saveFontRond(value: Float) {
        context.dataStore.edit { it[FONT_ROND] = value }
    }

    suspend fun clearCredentials() {
        context.dataStore.edit { it.clear() }
    }
}
