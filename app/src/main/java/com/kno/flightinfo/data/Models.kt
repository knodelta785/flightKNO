package com.kno.flightinfo.data

import org.json.JSONArray
import org.json.JSONObject

/** Satu baris penerbangan (kedatangan atau keberangkatan). Semua waktu = epoch detik UTC. */
data class Flight(
    val number: String,
    val airline: String,
    val aircraft: String,
    val otherCity: String,
    val otherIata: String,
    val international: Boolean,
    val scheduled: Long,
    val estimated: Long,
    val real: Long,
    val status: String,
    val isArrival: Boolean,
) {
    val key: String get() = "${if (isArrival) "A" else "D"}$number@$scheduled"

    /** Waktu terbaik yang diketahui: aktual > estimasi > jadwal. */
    val effective: Long
        get() = when {
            real > 0 -> real
            estimated > 0 -> estimated
            else -> scheduled
        }

    val delayMinutes: Int
        get() = if (scheduled > 0 && effective > 0) ((effective - scheduled) / 60).toInt() else 0

    val isCancelled: Boolean get() = status.contains("cancel", ignoreCase = true)

    /** Sudah mendarat / sudah berangkat. */
    val isDone: Boolean get() = real > 0

    fun toJson(): JSONObject = JSONObject().apply {
        put("number", number)
        put("airline", airline)
        put("aircraft", aircraft)
        put("otherCity", otherCity)
        put("otherIata", otherIata)
        put("international", international)
        put("scheduled", scheduled)
        put("estimated", estimated)
        put("real", real)
        put("status", status)
        put("isArrival", isArrival)
    }

    companion object {
        fun fromJson(o: JSONObject) = Flight(
            number = o.optString("number"),
            airline = o.optString("airline"),
            aircraft = o.optString("aircraft"),
            otherCity = o.optString("otherCity"),
            otherIata = o.optString("otherIata"),
            international = o.optBoolean("international", true),
            scheduled = o.optLong("scheduled"),
            estimated = o.optLong("estimated"),
            real = o.optLong("real"),
            status = o.optString("status"),
            isArrival = o.optBoolean("isArrival"),
        )
    }
}

/** Papan kedatangan atau keberangkatan satu bandara. */
data class Board(
    val airport: String,
    val name: String,
    val isArrival: Boolean,
    val tzOffset: Long,      // detik, offset zona waktu bandara
    val updatedAt: Long,     // epoch detik
    val flights: List<Flight>,
) {
    fun toJson(): String = JSONObject().apply {
        put("airport", airport)
        put("name", name)
        put("isArrival", isArrival)
        put("tzOffset", tzOffset)
        put("updatedAt", updatedAt)
        put("flights", JSONArray().also { arr -> flights.forEach { arr.put(it.toJson()) } })
    }.toString()

    companion object {
        fun fromJson(s: String): Board {
            val o = JSONObject(s)
            val arr = o.optJSONArray("flights") ?: JSONArray()
            val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let(Flight::fromJson) }
            return Board(
                airport = o.optString("airport"),
                name = o.optString("name"),
                isArrival = o.optBoolean("isArrival"),
                tzOffset = o.optLong("tzOffset"),
                updatedAt = o.optLong("updatedAt"),
                flights = list,
            )
        }
    }
}

enum class NotifStyle { BAR, BUBBLE }

data class AppSettings(
    val airport: String = "KNO",
    val internationalOnly: Boolean = true,
    val arrivalsOn: Boolean = true,
    val departuresOn: Boolean = true,
    /** Rentang jam (menit sejak 00:00, waktu lokal bandara). Boleh melewati tengah malam. */
    val arrFrom: Int = 0,
    val arrTo: Int = 24 * 60 - 1,
    val depFrom: Int = 0,
    val depTo: Int = 24 * 60 - 1,
    /** Notifikasi muncul berapa menit sebelum landing / berangkat. */
    val leadMinutes: Int = 60,
    val style: NotifStyle = NotifStyle.BAR,
    val delayOn: Boolean = true,
    val delayThreshold: Int = 15,
    val refreshMinutes: Int = 15,
)
