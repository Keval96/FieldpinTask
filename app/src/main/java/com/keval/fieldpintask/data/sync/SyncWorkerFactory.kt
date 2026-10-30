package com.keval.fieldpintask.data.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.keval.fieldpintask.domain.repository.AuthRepository
import com.keval.fieldpintask.domain.repository.SyncRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncWorkerFactory @Inject constructor(
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository,
) : WorkerFactory() {
    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? =
        if (workerClassName == SyncWorker::class.java.name) {
            SyncWorker(appContext, workerParameters, syncRepository, authRepository)
        } else {
            null
        }
}
