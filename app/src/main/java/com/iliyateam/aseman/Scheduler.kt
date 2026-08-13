
package com.iliyateam.aseman

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import java.time.Duration

object RefreshScheduler {

    private const val WORK = "aseman_refresh"

    suspend fun schedule(
        ctx: Context,
        refreshMinutes: Int? = null
    ) {
        val appContext = ctx.applicationContext
        val wm = WorkManager.getInstance(appContext)

        val mins = refreshMinutes ?: run {
            val pd = appContext.dataStore.data.first()
            pd[Prefs.KEY_REFRESH]
                ?.toIntOrNull()
                ?: 30
        }

        if (mins <= 0) {
            wm.cancelUniqueWork(WORK)
            return
        }

        val interval = maxOf(15, mins)

        val request = PeriodicWorkRequestBuilder<WeatherWorker>(
            Duration.ofMinutes(interval.toLong())
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(
                        NetworkType.CONNECTED
                    )
                    .build()
            )
            .build()

        wm.enqueueUniquePeriodicWork(
            WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}

