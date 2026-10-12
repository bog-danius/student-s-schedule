package com.example.lab4_ui_components

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.lab4_ui_components.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }

        val host = supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        val nav = host.navController
        b.bottomNav.setupWithNavController(nav)

        val topLevel = setOf(R.id.homeFragment, R.id.scheduleFragment, R.id.tasksFragment, R.id.profileFragment)
        nav.addOnDestinationChangedListener { _, destination, _ ->
            b.navContainer.isVisible = destination.id in topLevel
        }

        requestNotificationPermission()
    }

    fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun requestNotificationPermission() {
        if (!hasNotificationPermission()) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
