package com.kno.flightinfo.notify

import android.content.Context
import com.kno.flightinfo.data.AppSettings
import com.kno.flightinfo.data.Board
import com.kno.flightinfo.data.Flight
import com.kno.flightinfo.data.Fmt
import com.kno.flightinfo.data.SettingsStore

/**
 * Menentukan notifikasi mana yang harus dikirim berdasarkan pengaturan pengguna:
 *  - rentang jam (waktu lokal bandara) untuk kedatangan dan keberangkatan,
 *  - X menit sebelum landing/berangkat,
 *  - delay >= ambang, dan pembatalan.
 * Setiap notifikasi dikirim sekali saja (dicatat di SettingsStore.notified).
 */
object NotificationEngine {
    private const val KEEP_SECONDS = 48 * 3600L

    suspend fun process(ctx: Context, s: AppSettings, arr: Board, dep: Board) {
        val now = System.currentTimeMillis() / 1000
        val notified = SettingsStore.loadNotified(ctx)
            .filterValues { now - it < KEEP_SECONDS }
            .toMutableMap()
        val queue = mutableListOf<Triple<Board, Flight, Notifier.Kind>>()

        fun mark(key: String, b: Board, f: Flight, kind: Notifier.Kind) {
            if (notified.containsKey(key)) return
            notified[key] = now
            queue += Triple(b, f, kind)
        }

        fun scan(b: Board, enabled: Boolean, from: Int, to: Int) {
            if (!enabled) return
            for (f in b.flights) {
                if (f.isDone) continue
                if (s.internationalOnly && !f.international) continue

                val eff = f.effective
                val until = eff - now
                val effInWindow = Fmt.inWindow(Fmt.minuteOfDay(eff, b.tzOffset), from, to)
                val schedInWindow = Fmt.inWindow(Fmt.minuteOfDay(f.scheduled, b.tzOffset), from, to)

                if (f.isCancelled) {
                    if (schedInWindow && f.scheduled - now < 24 * 3600) mark("C|${f.key}", b, f, Notifier.Kind.CANCEL)
                    continue
                }
                if (until in 0..(s.leadMinutes * 60L) && effInWindow) {
                    mark("U|${f.key}", b, f, Notifier.Kind.UPCOMING)
                }
                if (s.delayOn && f.delayMinutes >= s.delayThreshold &&
                    until < 12 * 3600 && until > -2 * 3600 && schedInWindow
                ) {
                    // kunci memuat kelipatan 10 menit: notifikasi ulang jika delay bertambah parah
                    mark("D|${f.key}|${f.delayMinutes / 10}", b, f, Notifier.Kind.DELAY)
                }
            }
        }

        scan(arr, s.arrivalsOn, s.arrFrom, s.arrTo)
        scan(dep, s.departuresOn, s.depFrom, s.depTo)

        queue.forEach { (b, f, k) -> Notifier.send(ctx, b, f, k, s.style) }
        SettingsStore.saveNotified(ctx, notified)
    }
}
