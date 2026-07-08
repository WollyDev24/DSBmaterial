package dev.wolly.dsbmaterial.data

import androidx.compose.runtime.Immutable

@Immutable
data class SubstitutionEntry(
    val day: String,
    val art: String,
    val className: String,
    val lesson: String,
    val subject: String,
    val room: String,
    val vertrVon: String,
    val nach: String,
    val text: String,
    val rawText: String
)

data class PlanInfo(
    val title: String,
    val date: String,
    val url: String,
    val isHtml: Boolean = false
)
