package az.fleetra.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "fleetra_session")

data class UserSession(val token: String, val username: String, val role: String)

// Session storage backed by Jetpack DataStore, plus an in-memory cache so
// the OkHttp auth interceptor (which runs synchronously, off the main
// thread, on OkHttp's own dispatcher) can read the current token without
// needing to suspend or block on disk I/O for every single request.
object TokenStore {
    private lateinit var appContext: Context

    @Volatile var cachedToken: String? = null
        private set
    @Volatile var cachedUsername: String? = null
        private set
    @Volatile var cachedRole: String? = null
        private set

    private val KEY_TOKEN = stringPreferencesKey("token")
    private val KEY_USERNAME = stringPreferencesKey("username")
    private val KEY_ROLE = stringPreferencesKey("role")

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // Called once at process start (see FleetraApplication) so the cache is
    // warm before the first screen renders.
    suspend fun hydrate() {
        val prefs = appContext.dataStore.data.first()
        cachedToken = prefs[KEY_TOKEN]
        cachedUsername = prefs[KEY_USERNAME]
        cachedRole = prefs[KEY_ROLE]
    }

    suspend fun saveSession(token: String, username: String, role: String) {
        appContext.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            prefs[KEY_USERNAME] = username
            prefs[KEY_ROLE] = role
        }
        cachedToken = token
        cachedUsername = username
        cachedRole = role
    }

    suspend fun clearSession() {
        appContext.dataStore.edit { it.clear() }
        cachedToken = null
        cachedUsername = null
        cachedRole = null
    }
}
