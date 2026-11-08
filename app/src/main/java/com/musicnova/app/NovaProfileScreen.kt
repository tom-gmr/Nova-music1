package com.musicnova.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ProfileBg = Color(0xFF0B0910)
private val ProfileCard = Color(0xFF1C1725)
private val ProfilePurple = Color(0xFF9277B8)
private val ProfileLavender = Color(0xFFD1C1EB)
private val ProfileMuted = Color(0xFFAAA4B5)

@Composable
fun NovaProfileScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileBg)
            .padding(horizontal = 18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF59436F),
                                Color(0xFF292035)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = ProfileLavender,
                    modifier = Modifier.size(55.dp)
                )
            }

            Spacer(Modifier.height(15.dp))

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

        // PREMIUM PROFILE CARD

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = ProfileCard
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text(
                    text = "MUSIC NOVA",
                    color = ProfileLavender,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(9.dp))

                Text(
                    text = "Feel every beat.",
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = "Discover music and make every moment yours.",
                    color = ProfileMuted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(25.dp))

        Text(
            text = "PREFERENCES",
            color = ProfileMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(12.dp))

        ProfileOption(
            icon = Icons.Default.Equalizer,
            title = "Audio Experience",
            subtitle = "Sound quality and playback"
        )

        ProfileOption(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            subtitle = "Music updates and alerts"
        )

        ProfileOption(
            icon = Icons.Default.Palette,
            title = "Appearance",
            subtitle = "Dark theme and visual style"
        )

        ProfileOption(
            icon = Icons.Default.Info,
            title = "About Music Nova",
            subtitle = "App information and version"
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
                        text = "Made for music lovers",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Music Nova • Version 1.0",
                        color = ProfileMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(25.dp))
    }
}

@Composable
private fun ProfileOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = subtitle,
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
