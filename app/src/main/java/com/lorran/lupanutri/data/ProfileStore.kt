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

data class Profile(val goal: Goal = Goal.EQUILIBRIO, val restrictions: Set<Restriction> = emptySet())

class ProfileStore(private val context: Context) {
    private val goalKey = stringPreferencesKey("goal")
    private val restrictionsKey = stringSetPreferencesKey("restrictions")

    val profile: Flow<Profile> = context.dataStore.data.map { prefs ->
        Profile(
            goal = prefs[goalKey]?.let { runCatching { Goal.valueOf(it) }.getOrNull() } ?: Goal.EQUILIBRIO,
            restrictions = prefs[restrictionsKey].orEmpty()
                .mapNotNull { runCatching { Restriction.valueOf(it) }.getOrNull() }.toSet(),
        )
    }

    suspend fun setGoal(goal: Goal) {
        context.dataStore.edit { it[goalKey] = goal.name }
    }

    suspend fun toggle(r: Restriction) {
        context.dataStore.edit { prefs ->
            val cur = prefs[restrictionsKey].orEmpty()
            prefs[restrictionsKey] = if (r.name in cur) cur - r.name else cur + r.name
        }
    }
}
