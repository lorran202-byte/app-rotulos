package com.lorran.lupanutri.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lorran.lupanutri.domain.Goal
import com.lorran.lupanutri.domain.Restriction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "perfil")

data class Profile(
    val goal: Goal = Goal.EQUILIBRIO,
    val restrictions: Set<Restriction> = emptySet(),
    val offUser: String = "",
    val offPassword: String = "",
) {
    val hasOffAccount get() = offUser.isNotBlank() && offPassword.isNotBlank()
}

class ProfileStore(private val context: Context) {
    private val goalKey = stringPreferencesKey("goal")
    private val restrictionsKey = stringSetPreferencesKey("restrictions")
    private val offUserKey = stringPreferencesKey("off_user")
    private val offPassKey = stringPreferencesKey("off_password")

    val profile: Flow<Profile> = context.dataStore.data.map { prefs ->
        Profile(
            goal = prefs[goalKey]?.let { runCatching { Goal.valueOf(it) }.getOrNull() } ?: Goal.EQUILIBRIO,
            restrictions = prefs[restrictionsKey].orEmpty()
                .mapNotNull { runCatching { Restriction.valueOf(it) }.getOrNull() }.toSet(),
            offUser = prefs[offUserKey].orEmpty(),
            offPassword = prefs[offPassKey].orEmpty(),
        )
    }

    suspend fun setGoal(goal: Goal) {
        context.dataStore.edit { it[goalKey] = goal.name }
    }

    suspend fun setOffAccount(user: String, password: String) {
        context.dataStore.edit {
            it[offUserKey] = user.trim()
            it[offPassKey] = password
        }
    }

    suspend fun toggle(r: Restriction) {
        context.dataStore.edit { prefs ->
            val cur = prefs[restrictionsKey].orEmpty()
            prefs[restrictionsKey] = if (r.name in cur) cur - r.name else cur + r.name
        }
    }
}
