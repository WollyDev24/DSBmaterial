package dev.wolly.dsbmaterial

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.wolly.dsbmaterial.data.DataStoreManager
import dev.wolly.dsbmaterial.data.SubstitutionEntry
import dev.wolly.dsbmaterial.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar

private data class WidgetColors(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val headerBg: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color
)

private fun computeThemeColors(context: Context, themeIndex: Int, dynamicColor: Boolean): WidgetColors {
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val scheme = dynamicDarkColorScheme(context)
        return WidgetColors(
            primary = scheme.primary,
            background = scheme.background,
            surface = scheme.surface,
            headerBg = scheme.surfaceVariant,
            onSurface = scheme.onSurface,
            onSurfaceVariant = scheme.onSurfaceVariant
        )
    }

    val preset = themePresets.getOrElse(themeIndex) { themePresets[0] }
    return WidgetColors(
        primary = preset.primary,
        background = preset.background,
        surface = preset.surface,
        headerBg = preset.surfaceVariant,
        onSurface = preset.onSurface,
        onSurfaceVariant = preset.onSurfaceVariant
    )
}

class DSBWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dataStoreManager = DataStoreManager(context)
        val isRoomFirst = dataStoreManager.swapDataFlow.first()
        val themeIndex = dataStoreManager.themeIndexFlow.first()
        val dynamicColor = dataStoreManager.dynamicColorFlow.first()
        val colors = computeThemeColors(context, themeIndex, dynamicColor)
        val result = loadEntries(context)

        provideContent {
            GlanceTheme {
                WidgetContent(
                    entries = result.entries,
                    headerText = result.headerText,
                    isRoomFirst = isRoomFirst,
                    colors = colors,
                    context = context
                )
            }
        }
    }

    private data class WidgetResult(
        val entries: List<SubstitutionEntry>,
        val headerText: String
    )

    private suspend fun loadEntries(context: Context): WidgetResult = withContext(Dispatchers.IO) {
        try {
            val dataStoreManager = DataStoreManager(context)
            val archiveJson = dataStoreManager.archiveFlow.first() ?: return@withContext WidgetResult(emptyList(), "")
            if (archiveJson.isEmpty()) return@withContext WidgetResult(emptyList(), "")

            val type = object : TypeToken<List<SubstitutionEntry>>() {}.type
            val allEntries: List<SubstitutionEntry> = Gson().fromJson(archiveJson, type)

            val todayDayName = getTodayDayName()
            val todayDateStr = getTodayDateStr()

            val todayEntries = allEntries.filter { entry ->
                val lower = entry.day.lowercase()
                (todayDayName.isNotEmpty() && lower.startsWith(todayDayName.lowercase())) || lower.contains(todayDateStr)
            }

            if (todayEntries.isNotEmpty()) {
                return@withContext WidgetResult(todayEntries, todayDayName.ifEmpty { allEntries.firstOrNull()?.day ?: "" })
            }

            val dayOrder = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag")
            val todayIndex = if (todayDayName.isNotEmpty()) {
                dayOrder.indexOfFirst { it.equals(todayDayName, ignoreCase = true) }
            } else -1

            val dateRegex = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")
            val upcomingDay = allEntries
                .map { it.day }
                .distinct()
                .filter { day ->
                    val lower = day.lowercase()
                    !lower.contains("samstag") && !lower.contains("sonntag") &&
                    !lower.contains("saturday") && !lower.contains("sunday")
                }
                .sortedBy { day ->
                    val match = dateRegex.find(day)
                    if (match != null) {
                        val (d, m, y) = match.destructured
                        y.toLong() * 10000 + m.toLong() * 100 + d.toLong()
                    } else {
                        val matchIndex = dayOrder.indexOfFirst { day.lowercase().startsWith(it.lowercase()) }
                        if (matchIndex >= 0) {
                            1000L + matchIndex
                        } else {
                            2000L
                        }
                    }
                }
                .firstOrNull { day ->
                    if (todayIndex < 0) {
                        true
                    } else {
                        val dayIndex = dayOrder.indexOfFirst { day.lowercase().startsWith(it.lowercase()) }
                        if (dayIndex >= 0) dayIndex > todayIndex else true
                    }
                }

            if (upcomingDay != null) {
                val upcomingEntries = allEntries.filter { it.day == upcomingDay }
                val header = upcomingDay.replace(Regex(",?\\s*den\\s+\\d{2}\\.\\d{2}\\.\\d{4}"), "").trim()
                return@withContext WidgetResult(upcomingEntries, header)
            }

            WidgetResult(emptyList(), todayDayName)
        } catch (e: Exception) {
            WidgetResult(emptyList(), getTodayDayName())
        }
    }

    private fun getTodayDayName(): String {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Montag"
            Calendar.TUESDAY -> "Dienstag"
            Calendar.WEDNESDAY -> "Mittwoch"
            Calendar.THURSDAY -> "Donnerstag"
            Calendar.FRIDAY -> "Freitag"
            else -> ""
        }
    }

    private fun getTodayDateStr(): String {
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)
        return "%02d.%02d.%04d".format(day, month, year)
    }
}

@Composable
private fun WidgetContent(
    entries: List<SubstitutionEntry>,
    headerText: String,
    isRoomFirst: Boolean,
    colors: WidgetColors,
    context: Context
) {
    val textColor = ColorProvider(day = colors.onSurface, night = colors.onSurface)
    val secondaryTextColor = ColorProvider(day = colors.onSurfaceVariant, night = colors.onSurfaceVariant)
    val primaryTextColor = ColorProvider(day = colors.primary, night = colors.primary)

    val titleSize = 24.sp
    val countSize = 16.sp
    val labelSize = 14.sp
    val entrySize = 18.sp
    val typeSize = 16.sp
    val padH = 18.dp
    val padV = 14.dp
    val entryPadV = 4.dp
    val spacerTitle = 6.dp
    val spacerHeader = 4.dp
    val periodW = 45.dp
    val roomW = 60.dp
    val typeW = 95.dp

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(ColorProvider(day = colors.background, night = colors.background))
            .clickable(actionStartActivity<MainActivity>())
            .padding(horizontal = padH, vertical = padV)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_calendar),
                    contentDescription = null,
                    modifier = GlanceModifier.size(24.dp).padding(top = 4.dp)
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = headerText,
                    style = TextStyle(
                        fontSize = titleSize,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                if (entries.isNotEmpty()) {
                    Text(
                        text = "${entries.size}",
                        style = TextStyle(
                            fontSize = countSize,
                            color = secondaryTextColor
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(spacerTitle))

            if (entries.isNotEmpty()) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(day = colors.headerBg, night = colors.headerBg))
                        .cornerRadius(12.dp)
                        .padding(vertical = 6.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Per.", style = TextStyle(fontSize = labelSize, fontWeight = FontWeight.Bold, color = secondaryTextColor), modifier = GlanceModifier.width(periodW))
                    Text(text = "Subj.", style = TextStyle(fontSize = labelSize, fontWeight = FontWeight.Bold, color = secondaryTextColor), modifier = GlanceModifier.defaultWeight())
                    Text(text = "Room", style = TextStyle(fontSize = labelSize, fontWeight = FontWeight.Bold, color = secondaryTextColor), modifier = GlanceModifier.width(roomW))
                    Text(text = "Type", style = TextStyle(fontSize = labelSize, fontWeight = FontWeight.Bold, color = secondaryTextColor), modifier = GlanceModifier.width(typeW))
                }

                Spacer(modifier = GlanceModifier.height(spacerHeader))

                val displayEntries = entries.take(6)
                displayEntries.forEach { entry ->
                    val roomDisplay = if (isRoomFirst) entry.room else entry.art
                    val typeDisplay = if (isRoomFirst) entry.art else entry.room

                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(ColorProvider(day = colors.surface, night = colors.surface))
                            .cornerRadius(12.dp)
                            .padding(vertical = entryPadV, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = entry.lesson, style = TextStyle(fontSize = entrySize, fontWeight = FontWeight.Bold, color = textColor), modifier = GlanceModifier.width(periodW))
                        Text(text = entry.subject, style = TextStyle(fontSize = entrySize, fontWeight = FontWeight.Bold, color = primaryTextColor), modifier = GlanceModifier.defaultWeight())
                        Text(text = roomDisplay.ifEmpty { "—" }, style = TextStyle(fontSize = entrySize, fontWeight = FontWeight.Bold, color = textColor), modifier = GlanceModifier.width(roomW))

                        val typeColorHex = when {
                            typeDisplay.lowercase().contains("entfall") -> 0xFFFF6B6B.toInt()
                            typeDisplay.lowercase().contains("vertretung") -> 0xFFFFB74D.toInt()
                            typeDisplay.lowercase().contains("verlegung") || typeDisplay.lowercase().contains("verschiebung") -> 0xFF64B5F6.toInt()
                            else -> 0xFF888888.toInt()
                        }

                        Box(
                            modifier = GlanceModifier
                                .width(typeW)
                                .cornerRadius(8.dp)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = typeDisplay, style = TextStyle(fontSize = typeSize, fontWeight = FontWeight.Bold, color = ColorProvider(day = Color(typeColorHex), night = Color(typeColorHex))), maxLines = 1)
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(2.dp))
                }

                if (entries.size > 6) {
                    Box(modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "+${entries.size - 6} more",
                            style = TextStyle(fontSize = labelSize, color = secondaryTextColor)
                        )
                    }
                }
            } else {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No substitutions",
                        style = TextStyle(fontSize = entrySize, color = secondaryTextColor)
                    )
                }
            }
        }
    }
}