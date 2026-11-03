package com.onelabs.oneplayer.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onelabs.oneplayer.data.license.LicenseManager
import com.onelabs.oneplayer.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DevicePairingScreen(
    licenseManager: LicenseManager
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val coroutineScope = rememberCoroutineScope()

    val licenseStatus by licenseManager.licenseStatus.collectAsState()
    var isChecking by remember { mutableStateOf(false) }
    var syncResultMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // License / Trial Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("LICENSE & TRIAL STATUS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(licenseStatus.status, color = if (licenseStatus.isTrialActive) NeonLime else DangerRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                if (licenseStatus.isTrialActive) NeonLime.copy(alpha = 0.2f) else DangerRed.copy(alpha = 0.2f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "${licenseStatus.daysRemaining} Days Left",
                            color = if (licenseStatus.isTrialActive) NeonLime else DangerRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { (licenseStatus.daysRemaining.toFloat() / 31f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = NeonCyan,
                    trackColor = DarkSurfaceVariant
                )
            }
        }

        // MAC & Device Key Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("DEVICE IDENTIFICATION", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                // MAC Address Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("MAC Address", color = TextMuted, fontSize = 11.sp)
                        Text(
                            licenseStatus.macAddress,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    IconButton(
                        onClick = {
                            clipboardManager.setPrimaryClip(ClipData.newPlainText("MAC", licenseStatus.macAddress))
                            syncResultMsg = "MAC Address copied to clipboard!"
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy MAC", tint = NeonCyan)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Device Key Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("6-Digit Device Key", color = TextMuted, fontSize = 11.sp)
                        Text(
                            licenseStatus.deviceKey,
                            color = NeonCyan,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    }
                    IconButton(
                        onClick = {
                            clipboardManager.setPrimaryClip(ClipData.newPlainText("Device Key", licenseStatus.deviceKey))
                            syncResultMsg = "Device Key copied!"
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Key", tint = NeonCyan)
                    }
                }
            }
        }

        // Action Buttons: Sync Cloud & Check Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    isChecking = true
                    coroutineScope.launch {
                        val res = licenseManager.syncCloudPlaylists()
                        isChecking = false
                        syncResultMsg = res.second
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("sync_cloud_button")
            ) {
                if (isChecking) {
                    CircularProgressIndicator(color = DarkBackground, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = DarkBackground)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync Cloud", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        licenseManager.checkLicense()
                        syncResultMsg = "License validated with server!"
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("check_license_button")
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Check License")
            }
        }

        // Sync result feedback banner
        syncResultMsg?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(msg, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { syncResultMsg = null }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // How to Pair Instructions Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("HOW TO PAIR VIA ONEAPK.RO", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                listOf(
                    "1. Open https://oneapk.ro on your smartphone, tablet, or PC browser.",
                    "2. Enter your MAC address (${licenseStatus.macAddress}) and 6-digit Device Key (${licenseStatus.deviceKey}).",
                    "3. Add your IPTV M3U Playlist URLs or Xtream Codes login credentials.",
                    "4. Tap 'Sync Cloud' above to automatically load your channels, movies, and series on this device."
                ).forEach { step ->
                    Text(
                        step,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(60.dp))
    }
}
