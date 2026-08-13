package com.iliyateam.aseman

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object RefreshScheduler {
    private const val WORK = "aseman_refresh"


    suspend fun schedule(ctx: Context) {
        val pd = ctx.applicationContext.dataStore.data.first()
        val mins = (pd[Prefs.KEY_REFRESH] ?: "30").toIntOrNull() ?: 30
        val wm = WorkManager.getInstance(ctx)
        if (mins <= 0) {
            wm.cancelUniqueWork(WORK)
            return
        }
        val req = PeriodicWorkRequestBuilder<WeatherWorker>(
            maxOf(15, mins).toLong(), TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }
}