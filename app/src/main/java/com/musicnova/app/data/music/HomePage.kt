package com.musicnova.app.data.music

import androidx.compose.runtime.Immutable

@Immutable
data class HomePage(
    val shelves: List<HomeShelf> = emptyList(),
    val chips: List<HomeChip> = emptyList(),
    val continuationToken: String? = null,
    val backgroundUrl: String? = null,
)
