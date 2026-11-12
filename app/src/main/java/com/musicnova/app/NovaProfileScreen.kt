
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
private val ProfileBorder = Color(0xFF342A40)

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
                // Persistable permission may not be supported.
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

        // TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "YOUR SPACE",
                    color = ProfileMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    "Profile",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(ProfileCard, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = ProfileLavender
                )
            }
        }

        // PROFILE HERO CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = ProfileCard
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF30233F),
                                ProfileCard
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF9277B8),
                                        Color(0xFF392B4A)
                                    )
                                ),
                                CircleShape
                            )
                            .padding(4.dp)
                            .background(ProfileBg, CircleShape)
                            .clickable {
                                showPhotoOptions = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (profilePicture.startsWith("builtin:")) {
                            val avatarId =
                                profilePicture.removePrefix("builtin:")
                            val avatar =
                                profileAvatars.find { it.id == avatarId }
                                    ?: profileAvatars.last()

                            Icon(
                                avatar.icon,
                                contentDescription = "Profile picture",
                                tint = ProfileLavender,
                                modifier = Modifier.size(54.dp)
                            )
                        } else {
                            AsyncImage(
                                model = Uri.parse(profilePicture),
                                contentDescription = "Profile picture",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ProfileBg, CircleShape)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Music Nova User",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        "YOUR MUSIC. YOUR WORLD.",
                        color = ProfileMuted,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { showPhotoOptions = true },
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ProfileLavender
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            ProfileBorder
                        )
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text("Edit Profile Picture", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // GOOGLE ACCOUNT
        SectionHeading("CLOUD ACCOUNT")
        Spacer(Modifier.height(12.dp))
        GoogleAccountSection()

        Spacer(Modifier.height(25.dp))

        // PREFERENCES
        SectionHeading("PREFERENCES")

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = ProfileCard
            )
        ) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                ProfileOption(
                    Icons.Default.Equalizer,
                    "Audio Experience",
                    "Sound quality and playback"
                )

                ProfileDivider()

                ProfileOption(
                    Icons.Default.Notifications,
                    "Notifications",
                    "Music updates and alerts"
                )

                ProfileDivider()

                ProfileOption(
                    Icons.Default.Palette,
                    "Appearance",
                    "Dark theme and visual style"
                )

                ProfileDivider()

                ProfileOption(
                    Icons.Default.Info,
                    "About Music Nova",
                    "App information and version"
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // FOOTER
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF17131E)
            )
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            Color(0xFF30233F),
                            RoundedCornerShape(15.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = ProfileLavender,
                        modifier = Modifier.size(25.dp)
                    )
                }

                Spacer(Modifier.width(13.dp))

                Column {
                    Text(
                        "Made for music lovers",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        "Music Nova • Version 1.0",
                        color = ProfileMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(30.dp))
    }

    // PHOTO OPTIONS DIALOG
    if (showPhotoOptions) {
        AlertDialog(
            onDismissRequest = { showPhotoOptions = false },
            containerColor = ProfileCard,
            title = {
                Text(
                    "Change Profile Picture",
                    color = Color.White
                )
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
                TextButton(
                    onClick = { showPhotoOptions = false }
                ) {
                    Text("Cancel", color = ProfileLavender)
                }
            }
        )
    }

    // AVATAR PICKER
    if (showAvatarPicker) {
        AlertDialog(
            onDismissRequest = { showAvatarPicker = false },
            containerColor = ProfileCard,
            title = {
                Text(
                    "Choose Your Avatar",
                    color = Color.White
                )
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
                TextButton(
                    onClick = { showAvatarPicker = false }
                ) {
                    Text("Close", color = ProfileLavender)
                }
            }
        )
    }
}

@Composable
private fun SectionHeading(title: String) {
    Text(
        title,
        color = ProfileMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp
    )
}

@Composable
private fun ProfileDivider() {
    HorizontalDivider(
        color = ProfileBorder.copy(alpha = 0.65f),
        thickness = 0.7.dp
    )
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
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(43.dp)
                .background(
                    Color(0xFF30263D),
                    RoundedCornerShape(14.dp)
                ),
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

        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                subtitle,
                color = ProfileMuted,
                fontSize = 11.sp
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = ProfileMuted
        )
    }
}
