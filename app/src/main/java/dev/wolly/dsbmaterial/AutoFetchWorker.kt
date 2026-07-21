package dev.wolly.dsbmaterial

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.wolly.dsbmaterial.api.DSBMobileAPI
import dev.wolly.dsbmaterial.data.DataStoreManager
import kotlinx.coroutines.flow.first

class AutoFetchWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dataStoreManager = DataStoreManager(applicationContext)
        val username = dataStoreManager.usernameFlow.first() ?: return Result.failure()
        val password = dataStoreManager.passwordFlow.first() ?: return Result.failure()
        val className = dataStoreManager.classNameFlow.first() ?: return Result.failure()
        if (username.isEmpty() || password.isEmpty()) return Result.failure()

        val lastKnownJson = dataStoreManager.archiveFlow.first() ?: ""
        val lastKnownEntryCount = if (lastKnownJson.isNotEmpty()) {
            try {
                val type = object : com.google.gson.reflect.TypeToken<List<dev.wolly.dsbmaterial.data.SubstitutionEntry>>() {}.type
                val entries: List<dev.wolly.dsbmaterial.data.SubstitutionEntry> = com.google.gson.Gson().fromJson(lastKnownJson, type)
                entries.size
            } catch (_: Exception) { 0 }
        } else 0

        return try {
            val api = DSBMobileAPI(username, password)
            val allRaw = api.getSubstitutions("")

            val allClassNames = mutableSetOf(className)
            val selectedClassesStr = dataStoreManager.selectedClassesFlow.first()
            if (!selectedClassesStr.isNullOrEmpty()) {
                allClassNames.addAll(selectedClassesStr.split(",").map { it.trim() }.filter { it.isNotEmpty() })
            }

            val filtered = allRaw.filter { entry ->
                allClassNames.any { cls -> entry.className.equals(cls, ignoreCase = true) }
            }

            val deduped = filtered.distinctBy { it.day + it.lesson + it.subject + it.room + it.art + it.text }

            val newArchive = (deduped + (com.google.gson.Gson().fromJson<List<dev.wolly.dsbmaterial.data.SubstitutionEntry>>(
                lastKnownJson,
                object : com.google.gson.reflect.TypeToken<List<dev.wolly.dsbmaterial.data.SubstitutionEntry>>() {}.type
            ).let { if (lastKnownJson.isEmpty()) emptyList() else it })).distinctBy {
                it.day + it.lesson + it.subject + it.room + it.art + it.text
            }

            dataStoreManager.saveArchive(com.google.gson.Gson().toJson(newArchive))

            if (newArchive.size > lastKnownEntryCount) {
                val newCount = newArchive.size - lastKnownEntryCount
                sendNotification(newCount)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun sendNotification(newCount: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, DSBApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget_calendar)
            .setContentTitle(applicationContext.getString(R.string.notif_title))
            .setContentText(applicationContext.getString(R.string.notif_text, newCount))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "dsb_auto_fetch"
    }
}
