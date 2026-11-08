package com.kno.flightinfo.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object Scheduler {
    private const val PERIODIC = "flight_sync"
    private const val NOW = "flight_sync_now"

    private val net = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Interval minimum WorkManager adalah 15 menit; Android bisa menundanya saat Doze/hemat daya. */
    fun schedule(ctx: Context, minutes: Int, replace: Boolean) {
        val req = PeriodicWorkRequestBuilder<FlightSyncWorker>(minutes.coerceAtLeast(15).toLong(), TimeUnit.MINUTES)
            .setConstraints(net)
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            PERIODIC,
            if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            req,
        )
    }

    fun refreshNow(ctx: Context) {
        val req = OneTimeWorkRequestBuilder<FlightSyncWorker>().setConstraints(net).build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, req)
    }
}
