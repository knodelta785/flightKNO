package com.kno.flightinfo.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

object SettingsStore {
    private val AIRPORT = stringPreferencesKey("airport")
    private val INTL = booleanPreferencesKey("intl_only")
    private val ARR_ON = booleanPreferencesKey("arr_on")
    private val DEP_ON = booleanPreferencesKey("dep_on")
    private val ARR_FROM = intPreferencesKey("arr_from")
    private val ARR_TO = intPreferencesKey("arr_to")
    private val DEP_FROM = intPreferencesKey("dep_from")
    private val DEP_TO = intPreferencesKey("dep_to")
    private val LEAD = intPreferencesKey("lead")
    private val STYLE = stringPreferencesKey("style")
    private val DELAY_ON = booleanPreferencesKey("delay_on")
    private val DELAY_MIN = intPreferencesKey("delay_min")
    private val REFRESH = intPreferencesKey("refresh")
    private val NOTIFIED = stringSetPreferencesKey("notified")

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            airport = this[AIRPORT] ?: d.airport,
            internationalOnly = this[INTL] ?: d.internationalOnly,
            arrivalsOn = this[ARR_ON] ?: d.arrivalsOn,
            departuresOn = this[DEP_ON] ?: d.departuresOn,
            arrFrom = this[ARR_FROM] ?: d.arrFrom,
            arrTo = this[ARR_TO] ?: d.arrTo,
            depFrom = this[DEP_FROM] ?: d.depFrom,
            depTo = this[DEP_TO] ?: d.depTo,
            leadMinutes = this[LEAD] ?: d.leadMinutes,
            style = runCatching { NotifStyle.valueOf(this[STYLE] ?: "BAR") }.getOrDefault(NotifStyle.BAR),
            delayOn = this[DELAY_ON] ?: d.delayOn,
            delayThreshold = this[DELAY_MIN] ?: d.delayThreshold,
            refreshMinutes = this[REFRESH] ?: d.refreshMinutes,
        )
    }

    fun flow(ctx: Context): Flow<AppSettings> =
        ctx.applicationContext.dataStore.data.map { it.toSettings() }

    suspend fun read(ctx: Context): AppSettings = flow(ctx).first()

    suspend fun save(ctx: Context, s: AppSettings) {
        ctx.applicationContext.dataStore.edit { p ->
            p[AIRPORT] = s.airport
            p[INTL] = s.internationalOnly
            p[ARR_ON] = s.arrivalsOn
            p[DEP_ON] = s.departuresOn
            p[ARR_FROM] = s.arrFrom
            p[ARR_TO] = s.arrTo
            p[DEP_FROM] = s.depFrom
            p[DEP_TO] = s.depTo
            p[LEAD] = s.leadMinutes
            p[STYLE] = s.style.name
            p[DELAY_ON] = s.delayOn
            p[DELAY_MIN] = s.delayThreshold
            p[REFRESH] = s.refreshMinutes
        }
    }

    /** Notifikasi yang sudah dikirim (agar tidak dobel): kunci -> epoch detik. */
    suspend fun loadNotified(ctx: Context): Map<String, Long> {
        val set = ctx.applicationContext.dataStore.data.first()[NOTIFIED] ?: emptySet()
        return set.mapNotNull {
            val i = it.lastIndexOf('|')
            if (i < 0) null else it.substring(0, i) to (it.substring(i + 1).toLongOrNull() ?: 0L)
        }.toMap()
    }

    suspend fun saveNotified(ctx: Context, m: Map<String, Long>) {
        ctx.applicationContext.dataStore.edit { p ->
            p[NOTIFIED] = m.map { "${it.key}|${it.value}" }.toSet()
        }
    }
}
