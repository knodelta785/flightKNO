package com.kno.flightinfo.data

import java.util.Locale

object Fmt {
    /** Jam:menit di zona waktu bandara (offset detik). */
    fun time(epochSec: Long, offsetSec: Long): String {
        if (epochSec <= 0) return "--:--"
        val s = (((epochSec + offsetSec) % 86400) + 86400) % 86400
        return String.format(Locale.US, "%02d:%02d", s / 3600, (s % 3600) / 60)
    }

    fun minuteOfDay(epochSec: Long, offsetSec: Long): Int {
        val s = (((epochSec + offsetSec) % 86400) + 86400) % 86400
        return (s / 60).toInt()
    }

    fun hm(minutes: Int): String = String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)

    /** Apakah menit-dalam-hari m ada di rentang [from, to]; mendukung rentang lewat tengah malam. */
    fun inWindow(m: Int, from: Int, to: Int): Boolean =
        if (from <= to) m in from..to else (m >= from || m <= to)

    /** Terjemahan ringkas status Flightradar24 ke Bahasa Indonesia. */
    fun statusId(text: String): String {
        val t = text.trim()
        val map = listOf(
            "Landed" to "Mendarat",
            "Departed" to "Berangkat",
            "Scheduled" to "Terjadwal",
            "Estimated" to "Estimasi",
            "Delayed" to "Tertunda",
            "Canceled" to "Dibatalkan",
            "Cancelled" to "Dibatalkan",
            "Diverted" to "Dialihkan",
            "Unknown" to "—",
        )
        for ((en, id) in map) if (t.startsWith(en, ignoreCase = true)) return id + t.substring(en.length)
        return t.ifBlank { "—" }
    }
}
