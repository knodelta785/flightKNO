package com.kno.flightinfo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.glance.appwidget.updateAll
import com.kno.flightinfo.data.AppSettings
import com.kno.flightinfo.data.Board
import com.kno.flightinfo.data.BoardCache
import com.kno.flightinfo.data.SettingsStore
import com.kno.flightinfo.widget.FlightWidget
import com.kno.flightinfo.work.Scheduler
import com.kno.flightinfo.work.SyncCore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext

    val settings: StateFlow<AppSettings> =
        SettingsStore.flow(ctx).stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _arrivals = MutableStateFlow<Board?>(null)
    private val _departures = MutableStateFlow<Board?>(null)
    private val _loading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val arrivals = _arrivals.asStateFlow()
    val departures = _departures.asStateFlow()
    val loading = _loading.asStateFlow()
    val error = _error.asStateFlow()

    init {
        viewModelScope.launch {
            _arrivals.value = BoardCache.load(ctx, true)
            _departures.value = BoardCache.load(ctx, false)
            refresh()
        }
    }

    fun refresh() {
        if (_loading.value) return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                // notify = true: buka aplikasi juga ikut mengecek aturan notifikasi
                val (a, d) = SyncCore.run(ctx, notify = true)
                _arrivals.value = a
                _departures.value = d
            } catch (e: Exception) {
                _error.value = e.message ?: "Gagal memuat data"
            } finally {
                _loading.value = false
            }
        }
    }

    fun save(new: AppSettings) {
        val old = settings.value
        viewModelScope.launch {
            SettingsStore.save(ctx, new)
            if (new.refreshMinutes != old.refreshMinutes) {
                Scheduler.schedule(ctx, new.refreshMinutes, replace = true)
            }
            if (!new.airport.equals(old.airport, ignoreCase = true)) {
                BoardCache.clear(ctx)
                _arrivals.value = null
                _departures.value = null
                FlightWidget().updateAll(ctx)
                refresh()
            } else {
                FlightWidget().updateAll(ctx)
            }
        }
    }
}
