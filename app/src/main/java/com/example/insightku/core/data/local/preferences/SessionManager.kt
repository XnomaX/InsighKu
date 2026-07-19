package com.example.insightku.core.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_ID = stringPreferencesKey("user_id")
        private val HAS_SEEDED_CATEGORIES = booleanPreferencesKey("has_seeded_categories")
    }

    val isLoggedIn: Flow<Boolean> = context.sessionDataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN] ?: false
    }

    val userEmail: Flow<String> = context.sessionDataStore.data.map { preferences ->
        preferences[USER_EMAIL] ?: ""
    }

    val userName: Flow<String> = context.sessionDataStore.data.map { preferences ->
        preferences[USER_NAME] ?: ""
    }

    val userId: Flow<String> = context.sessionDataStore.data.map { preferences ->
        preferences[USER_ID] ?: ""
    }

    suspend fun saveLoginSession(email: String, name: String, userId: String = "") {
        context.sessionDataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = true
            preferences[USER_EMAIL] = email
            preferences[USER_NAME] = name
            preferences[USER_ID] = userId
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun getHasSeededCategories(): Boolean {
        return context.sessionDataStore.data.map { it[HAS_SEEDED_CATEGORIES] ?: false }.first()
    }

    suspend fun setHasSeededCategories(value: Boolean) {
        context.sessionDataStore.edit { preferences ->
            preferences[HAS_SEEDED_CATEGORIES] = value
        }
    }

    suspend fun setLoggedIn(isLoggedIn: Boolean) {
        context.sessionDataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = isLoggedIn
        }
    }

    suspend fun getLoginStatus(): Boolean {
        return isLoggedIn.first()
    }

    suspend fun getUserData(): Triple<String, String, String> {
        val email = userEmail.first()
        val name = userName.first()
        val id = userId.first()
        return Triple(email, name, id)
    }
}
