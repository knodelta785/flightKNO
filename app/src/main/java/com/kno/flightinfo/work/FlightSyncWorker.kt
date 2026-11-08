package com.kno.flightinfo.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.IOException

class FlightSyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result =
        try {
            SyncCore.run(applicationContext, notify = true)
            Result.success()
        } catch (e: IOException) {
            // jaringan / ditolak server: coba lagi sebentar, lalu tunggu jadwal berikutnya
            if (runAttemptCount < 2) Result.retry() else Result.success()
        } catch (e: Exception) {
            Result.success()
        }
}
