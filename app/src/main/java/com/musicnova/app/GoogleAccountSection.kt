package com.musicnova.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.musicnova.app.auth.GoogleAuthManager
import kotlinx.coroutines.launch

private val AccountCard = Color(0xFF1C1725)
private val AccountPurple = Color(0xFF9277B8)
private val AccountLavender = Color(0xFFD1C1EB)
private val AccountMuted = Color(0xFFAAA4B5)

@Composable
fun GoogleAccountSection() {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val scope = rememberCoroutineScope()

    var user by remember {
        mutableStateOf<FirebaseUser?>(auth.currentUser)
    }

    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener {
            user = it.currentUser
        }

        auth.addAuthStateListener(listener)

        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = AccountCard
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = AccountLavender,
                    modifier = Modifier.size(25.dp)
                )

                Spacer(Modifier.width(10.dp))

                Text(
                    "CLOUD ACCOUNT",
                    color = AccountLavender,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.height(15.dp))

            if (user != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = AccountLavender,
                        modifier = Modifier.size(42.dp)
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            user?.displayName ?: "Music Nova User",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            user?.email ?: "",
                            color = AccountMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "Google account connected",
                    color = AccountLavender,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            loading = true
                            errorMessage = null

                            try {
                                GoogleAuthManager.signOut(context)
                            } catch (e: Exception) {
                                errorMessage =
                                    e.localizedMessage ?: "Sign out failed"
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out")
                }
            } else {
                Text(
                    "Sign in to connect your Music Nova account.",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    "Cloud playlist backup and sync will be added next.",
                    color = AccountMuted,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = {
                        scope.launch {
                            loading = true
                            errorMessage = null

                            try {
                                GoogleAuthManager.signIn(context)
                            } catch (e: Exception) {
                                errorMessage =
                                    e.localizedMessage ?: "Google sign-in failed"
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccountPurple
                    )
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Login, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Continue with Google")
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(10.dp))

                Text(
                    errorMessage!!,
                    color = Color(0xFFFF8A8A),
                    fontSize = 12.sp
                )
            }
        }
    }
}
