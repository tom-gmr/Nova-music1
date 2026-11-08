package com.musicnova.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NovaHeroCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(190.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF302344),
                        Color(0xFF171222),
                        Color(0xFF48345F)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
              Column(
            modifier = Modifier.padding(22.dp)
        ) {
            Text(
                text = "FEEL THE SOUND",
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                color = Color(0xFFC9B9E5)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your music.\nYour universe.",
                fontSize = 27.sp,
                lineHeight = 32.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "DISCOVER YOUR NEXT FAVOURITE",
                fontSize = 9.sp,
                letterSpacing = 1.5.sp,
                color = Color(0xFFCEC4DC)
            )
        }
    }
}
