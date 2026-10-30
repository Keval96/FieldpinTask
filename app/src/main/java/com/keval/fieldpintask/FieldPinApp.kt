package com.keval.fieldpintask

import android.app.Application
import androidx.work.Configuration
import com.keval.fieldpintask.data.sync.SyncScheduler
import com.keval.fieldpintask.data.sync.SyncWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import org.maplibre.android.MapLibre

@HiltAndroidApp
class FieldPinApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: SyncWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        SyncScheduler.schedule(this)
    }
}
