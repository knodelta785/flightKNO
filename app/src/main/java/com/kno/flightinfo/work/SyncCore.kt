package com.kno.flightinfo.work

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.kno.flightinfo.data.Board
import com.kno.flightinfo.data.BoardCache
import com.kno.flightinfo.data.Fr24Client
import com.kno.flightinfo.data.SettingsStore
import com.kno.flightinfo.notify.NotificationEngine
import com.kno.flightinfo.widget.FlightWidget

object SyncCore {
    /** Ambil data terbaru, simpan ke cache, perbarui widget, dan (opsional) kirim notifikasi. */
    suspend fun run(ctx: Context, notify: Boolean): Pair<Board, Board> {
        val s = SettingsStore.read(ctx)
        val arr = Fr24Client.fetch(s.airport, arrivals = true)
        val dep = Fr24Client.fetch(s.airport, arrivals = false)
        BoardCache.save(ctx, arr)
        BoardCache.save(ctx, dep)
        FlightWidget().updateAll(ctx)
        if (notify) NotificationEngine.process(ctx, s, arr, dep)
        return arr to dep
    }
}
