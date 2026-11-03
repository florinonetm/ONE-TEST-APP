package com.onelabs.oneplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onelabs.oneplayer.data.model.UserProfile
import com.onelabs.oneplayer.data.repository.PlaylistRepository
import com.onelabs.oneplayer.ui.theme.*

@Composable
fun ProfilesScreen(
    repository: PlaylistRepository
) {
    val profiles by repository.profiles.collectAsState()
    val activeProfile by repository.activeProfile.collectAsState()

    var showPinDialogFor by remember { mutableStateOf<UserProfile?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text("User Profiles & Parental Controls", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Switch user to access customized favorites, history, or Kids Mode.", color = TextSecondary, fontSize = 13.sp)

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(profiles, key = { it.id }) { prof ->
                val isActive = prof.id == activeProfile?.id
                val avatarColor = remember(prof.avatarColorHex) {
                    try {
                        Color(android.graphics.Color.parseColor(prof.avatarColorHex))
                    } catch (_: Exception) {
                        NeonCyan
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) DarkSurfaceVariant else DarkSurface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (prof.pin != null && !isActive) {
                                showPinDialogFor = prof
                                enteredPin = ""
                                pinError = false
                            } else {
                                repository.setActiveProfile(prof)
                            }
                        }
                        .testTag("profile_card_${prof.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(avatarColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = DarkBackground,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(prof.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                if (prof.isKids) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(NeonLime.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("KIDS", color = NeonLime, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (prof.pin != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.Lock, contentDescription = "PIN Protected", tint = AmberGold, modifier = Modifier.size(14.dp))
                                }
                            }
                            Text(
                                if (isActive) "Currently Active Profile" else "Tap to switch",
                                color = if (isActive) NeonCyan else TextMuted,
                                fontSize = 12.sp
                            )
                        }

                        if (isActive) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = NeonCyan, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }

    // PIN Dialog
    showPinDialogFor?.let { targetProfile ->
        AlertDialog(
            onDismissRequest = { showPinDialogFor = null },
            title = { Text("Enter Parental PIN", color = Color.White) },
            containerColor = DarkSurface,
            text = {
                Column {
                    Text("This profile is protected with a 4-digit PIN.", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            if (it.length <= 4) enteredPin = it
                            pinError = false
                        },
                        label = { Text("4-Digit PIN (Default: 1234)") },
                        isError = pinError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Incorrect PIN. Please try again.", color = DangerRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredPin == (targetProfile.pin ?: "1234")) {
                            repository.setActiveProfile(targetProfile)
                            showPinDialogFor = null
                        } else {
                            pinError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Unlock", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialogFor = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
