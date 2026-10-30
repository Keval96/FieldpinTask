package com.keval.fieldpintask.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.keval.fieldpintask.domain.repository.AuthRepository
import com.keval.fieldpintask.domain.repository.OfflineException
import com.keval.fieldpintask.domain.repository.SyncRepository
import kotlinx.coroutines.flow.first

/** Pushes pending changes in the background while a user is signed in. */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (authRepository.session.first() == null) return Result.success()
        return syncRepository.sync().fold(
            onSuccess = { Result.success() },
            onFailure = { if (it is OfflineException || runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure() },
        )
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
    }
}
