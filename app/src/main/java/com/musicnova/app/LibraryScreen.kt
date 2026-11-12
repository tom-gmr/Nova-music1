"Playlists" -> {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Your Playlists",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    "${playlists.size} playlists",
                    color = LibMuted,
                    fontSize = 12.sp
                )
            }

            FilledTonalIconButton(
                onClick = {
                    playlistName = ""
                    showCreateDialog = true
                },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color(0xFF352B42),
                    contentColor = LibAccent
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Create playlist"
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // Create playlist card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(
                            Color(0xFF40334F),
                            Color(0xFF211B2B)
                        )
                    )
                )
                .clickable {
                    playlistName = ""
                    showCreateDialog = true
                }
                .padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(0xFF57446D)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = LibAccent,
                    modifier = Modifier.size(29.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "Create new playlist",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    "Collect songs your way",
                    color = LibMuted,
                    fontSize = 12.sp
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = LibAccent
            )
        }

        Spacer(Modifier.height(22.dp))

        if (playlists.isEmpty()) {
            LibraryEmptyState(
                "No playlists yet",
                "Create your first playlist."
            )
        } else {
            Text(
                "All Playlists",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(13.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(15.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(playlists, key = { it.name }) { playlist ->

                    // Playlist artwork cover
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF30283C))
                            .clickable {
                                selectedPlaylist = playlist.name
                            }
                    ) {
                        val cover =
                            playlist.tracks.firstOrNull()?.artworkUrl

                        if (!cover.isNullOrBlank()) {
                            AsyncImage(
                                model = cover,
                                contentDescription = playlist.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.QueueMusic,
                                    contentDescription = null,
                                    tint = LibAccent,
                                    modifier = Modifier.size(55.dp)
                                )
                            }
                        }

                        // Bottom dark gradient
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0x6609080D),
                                            Color(0xFF09080D)
                                        )
                                    )
                                )
                        )

                        // Playlist information
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(
                                    start = 17.dp,
                                    end = 12.dp,
                                    bottom = 15.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    playlist.name,
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )

                                Spacer(Modifier.height(5.dp))

                                Text(
                                    "${playlist.tracks.size} songs",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }

                            if (playlist.tracks.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        onPlay(playlist.tracks.first())
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.PlayCircleFilled,
                                        contentDescription = "Play playlist",
                                        tint = Color.White,
                                        modifier = Modifier.size(35.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    selectedPlaylist = playlist.name
                                }
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Open playlist",
                                    tint = Color.White,
                                    modifier = Modifier.size(27.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

