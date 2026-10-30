package com.keval.fieldpintask.data.repository

import androidx.room.withTransaction
import com.keval.fieldpintask.data.PhotoStore
import com.keval.fieldpintask.data.local.FieldPinDatabase
import com.keval.fieldpintask.data.remote.FieldPinApi
import com.keval.fieldpintask.data.remote.LoginRequest
import com.keval.fieldpintask.data.session.SessionStore
import com.keval.fieldpintask.domain.model.Session
import com.keval.fieldpintask.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: FieldPinApi,
    private val sessionStore: SessionStore,
    private val database: FieldPinDatabase,
    private val photoStore: PhotoStore,
) : AuthRepository {

    override val session: Flow<Session?> = sessionStore.session

    override suspend fun login(email: String, password: String): Result<Session> = try {
        val response = api.login(LoginRequest(email.trim(), password))
        val session = Session(response.token, response.email, response.displayName)
        sessionStore.save(session)
        Result.success(session)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun logout() {
        database.withTransaction {
            database.siteDao().clear()
            database.inspectionDao().clear()
        }
        photoStore.clear()
        sessionStore.clear()
    }
}
