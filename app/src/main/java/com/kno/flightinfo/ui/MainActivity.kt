package com.kno.flightinfo.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.kno.flightinfo.data.SettingsStore
import com.kno.flightinfo.work.Scheduler
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* hasil tidak perlu ditangani */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Pastikan pengecekan berkala berjalan di latar belakang.
        lifecycleScope.launch {
            val s = SettingsStore.read(applicationContext)
            Scheduler.schedule(applicationContext, s.refreshMinutes, replace = false)
        }

        setContent { AppRoot(vm) }
    }
}
