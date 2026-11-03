package com.onelabs.oneplayer

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.onelabs.oneplayer.data.license.LicenseManager
import com.onelabs.oneplayer.data.repository.PlaylistRepository
import com.onelabs.oneplayer.ui.MainScreen
import com.onelabs.oneplayer.ui.theme.ONEPlayerTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: PlaylistRepository
    private lateinit var licenseManager: LicenseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen on during playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        repository = PlaylistRepository(applicationContext)
        licenseManager = LicenseManager(applicationContext)

        setContent {
            ONEPlayerTheme {
                MainScreen(
                    repository = repository,
                    licenseManager = licenseManager
                )
            }
        }
    }
}
