package com.musicnova.app

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val ProfileBg = Color(0xFF0B0910)
private val ProfileCard = Color(0xFF1C1725)
private val ProfilePurple = Color(0xFF9277B8)
private val ProfileLavender = Color(0xFFD1C1EB)
private val ProfileMuted = Color(0xFFAAA4B5)

private data class ProfileAvatar(
    val id: String,
    val title: String,
    val icon: ImageVector
)

private val profileAvatars = listOf(
    ProfileAvatar("music", "Music", Icons.Default.MusicNote),
    ProfileAvatar("headphones", "Headphones", Icons.Default.Headphones),
    ProfileAvatar("album", "Vinyl", Icons.Default.Album),
    ProfileAvatar("equalizer", "Equalizer", Icons.Default.Equalizer),
    ProfileAvatar("favorite", "Favorite", Icons.Default.Favorite),
    ProfileAvatar("person", "Classic", Icons.Default.Person)
)

@Composable
fun NovaProfileScreen() {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("nova_profile", 0)
    }

    var profilePicture by remember {
        mutableStateOf(
            preferences.getString("profile_picture", "builtin:person")
                ?: "builtin:person"
        )
    }

    var showPhotoOptions by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Some document providers do not support persistable access.
            }

            profilePicture = uri.toString()
            preferences.edit()
                .putString("profile_picture", profilePicture)
                .apply()
        }
    }

    fun saveAvatar(id: String) {
        profilePicture = "builtin:$id"
        preferences.edit()
            .putString("profile_picture", profilePicture)
            .apply()
        showAvatarPicker = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF59436F),
                                Color(0xFF292035)
                            )
                        ),
                        CircleShape
                    )
                    .clickable {
                        showPhotoOptions = true
                    },
                contentAlignment = Alignment.Center
            ) {
                if (profilePicture.startsWith("builtin:")) {
                    val avatarId = profilePicture.removePrefix("builtin:")
                    val avatar = profileAvatars.find { it.id == avatarId }
                        ?: profileAvatars.last()

                    Icon(
                        imageVector = avatar.icon,
                        contentDescription = "Profile picture",
                        tint = ProfileLavender,
                        modifier = Modifier.size(58.dp)
                    )
                } else {
                    AsyncImage(
                        model = Uri.parse(profilePicture),
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ProfileCard, CircleShape),
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(34.dp)
                        .background(ProfilePurple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Change picture",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = { showPhotoOptions = true }
            ) {
                Text(
                    "Change Profile Picture",
                    color = ProfileLavender,
                    fontSize = 13.sp
                )
            }

            Text(
                text = "Music Nova User",
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(5.dp))

            Text(
                text = "YOUR MUSIC. YOUR WORLD.",
                color = ProfileMuted,
                fontSize = 10.sp,
                letterSpacing = 2.sp
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = ProfileCard
            )
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    "MUSIC NOVA",
                    color = ProfileLavender,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(9.dp))

                Text(
                    "Feel every beat.",
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    "Discover music and make every moment yours.",
                    color = ProfileMuted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(25.dp))

        Text(
            "PREFERENCES",
            color = ProfileMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(12.dp))

        ProfileOption(
            Icons.Default.Equalizer,
            "Audio Experience",
            "Sound quality and playback"
        )

        ProfileOption(
            Icons.Default.Notifications,
            "Notifications",
            "Music updates and alerts"
        )

        ProfileOption(
            Icons.Default.Palette,
            "Appearance",
            "Dark theme and visual style"
        )

        ProfileOption(
            Icons.Default.Info,
            "About Music Nova",
            "App information and version"
        )

        Spacer(Modifier.height(22.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF17131E)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = ProfileLavender,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        "Made for music lovers",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        "Music Nova • Version 1.0",
                        color = ProfileMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(25.dp))
    }

    if (showPhotoOptions) {
        AlertDialog(
            onDismissRequest = { showPhotoOptions = false },
            containerColor = ProfileCard,
            title = {
                Text("Change Profile Picture", color = Color.White)
            },
            text = {
                Column {
                    ProfileDialogOption(
                        Icons.Default.PhotoLibrary,
                        "Choose from Gallery"
                    ) {
                        showPhotoOptions = false
                        galleryLauncher.launch(arrayOf("image/*"))
                    }

                    ProfileDialogOption(
                        Icons.Default.Face,
                        "Choose Inbuilt Avatar"
                    ) {
                        showPhotoOptions = false
                        showAvatarPicker = true
                    }

                    ProfileDialogOption(
                        Icons.Default.Delete,
                        "Remove Photo"
                    ) {
                        saveAvatar("person")
                        showPhotoOptions = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoOptions = false }) {
                    Text("Cancel", color = ProfileLavender)
                }
            }
        )
    }

    if (showAvatarPicker) {
        AlertDialog(
            onDismissRequest = { showAvatarPicker = false },
            containerColor = ProfileCard,
            title = {
                Text("Choose Your Avatar", color = Color.White)
            },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(220.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(profileAvatars) { avatar ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFF30263D),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    saveAvatar(avatar.id)
                                }
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                avatar.icon,
                                contentDescription = avatar.title,
                                tint = ProfileLavender,
                                modifier = Modifier.size(34.dp)
                            )

                            Spacer(Modifier.height(5.dp))

                            Text(
                                avatar.title,
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAvatarPicker = false }) {
                    Text("Close", color = ProfileLavender)
                }
            }
        )
    }
}

@Composable
private fun ProfileDialogOption(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = ProfileLavender
        )

        Spacer(Modifier.width(14.dp))

        Text(
            title,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ProfileOption(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(43.dp)
                .background(ProfileCard, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ProfileLavender,
                modifier = Modifier.size(21.dp)
            )
        }

        Spacer(Modifier.width(13.dp))

        Column {
            Text(
                title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(3.dp))

            Text(
                subtitle,
                color = ProfileMuted,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.weight(1f))

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = ProfileMuted
        )
    }
}
