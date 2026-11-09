package com.musicnova.app.data.music

import androidx.compose.runtime.Immutable

/**
 * Home screen mood and activity category.
 * Examples: Relax, Energize, Workout, Commute and Focus.
 */
@Immutable
data class HomeChip(
    val title: String,
    val params: String? = null,
    val browseId: String? = null,
)
