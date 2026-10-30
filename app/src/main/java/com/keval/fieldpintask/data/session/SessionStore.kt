package com.keval.fieldpintask.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.keval.fieldpintask.domain.model.Session
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class SessionStore @Inject constructor(private val dataStore: DataStore<Preferences>) {

    val session: Flow<Session?> = dataStore.data.map { prefs ->
        val token = prefs[TOKEN] ?: return@map null
        Session(token, prefs[EMAIL].orEmpty(), prefs[NAME].orEmpty())
    }

    suspend fun token(): String? = session.first()?.token

    suspend fun save(session: Session) {
        dataStore.edit {
            it[TOKEN] = session.token
            it[EMAIL] = session.email
            it[NAME] = session.displayName
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val TOKEN = stringPreferencesKey("token")
        val EMAIL = stringPreferencesKey("email")
        val NAME = stringPreferencesKey("name")
    }
}
