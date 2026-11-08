package com.kno.flightinfo.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Mengambil papan kedatangan/keberangkatan dari endpoint papan bandara Flightradar24
 * (endpoint yang sama dengan yang dipakai situs flightradar24.com).
 *
 * PENTING: ini BUKAN API resmi. Flightradar24 bisa mengubah format, membatasi, atau memblokir
 * akses kapan saja, dan ketentuan layanan mereka melarang penggunaan ulang data tanpa izin.
 * Untuk produksi / dipublikasikan, gunakan API resmi berlangganan Flightradar24 dan
 * ganti isi fungsi [fetch] saja; bagian aplikasi lain tidak perlu berubah.
 */
object Fr24Client {
    private const val BASE = "https://api.flightradar24.com/common/v1/airport.json"
    private const val USER_AGENT = "KualanamuFlightInfo/1.0 (Android)"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private class State {
        var name: String = ""
        var tz: Long = 0L
        var tzSet: Boolean = false
    }

    suspend fun fetch(airport: String, arrivals: Boolean, pages: Int = 2): Board =
        withContext(Dispatchers.IO) {
            val code = airport.trim().lowercase()
            val out = mutableListOf<Flight>()
            val st = State()
            for (page in 1..pages) {
                val root = try {
                    fetchPage(code, arrivals, page)
                } catch (e: IOException) {
                    if (page == 1) throw e else break
                }
                val data = parsePage(root, arrivals, out, st)
                if (data == 0) break
            }
            Board(
                airport = airport.uppercase(),
                name = st.name.ifBlank { Airports.name(airport) },
                isArrival = arrivals,
                tzOffset = st.tz,
                updatedAt = System.currentTimeMillis() / 1000,
                flights = out.distinctBy { it.key },
            )
        }

    private fun fetchPage(code: String, arrivals: Boolean, page: Int): JSONObject {
        val url = BASE.toHttpUrl().newBuilder()
            .addQueryParameter("code", code)
            .addQueryParameter("plugin[]", "")
            .addQueryParameter("plugin-setting[schedule][mode]", if (arrivals) "arrivals" else "departures")
            .addQueryParameter("plugin-setting[schedule][timestamp]", (System.currentTimeMillis() / 1000).toString())
            .addQueryParameter("page", page.toString())
            .addQueryParameter("limit", "100")
            .build()
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Flightradar24 menolak permintaan (HTTP ${resp.code})")
            val body = resp.body?.string() ?: throw IOException("Respons kosong")
            return try {
                JSONObject(body)
            } catch (e: Exception) {
                throw IOException("Format respons tidak dikenali")
            }
        }
    }

    // --- helper JSON aman-null (org.json mengubah JSON null menjadi string "null") ---
    private fun JSONObject?.o(k: String): JSONObject? = this?.optJSONObject(k)
    private fun JSONObject?.s(k: String): String =
        if (this == null || isNull(k)) "" else optString(k, "")
    private fun JSONObject?.l(k: String): Long =
        if (this == null || isNull(k)) 0L else optLong(k, 0L)

    /** @return jumlah baris data pada halaman ini. */
    private fun parsePage(root: JSONObject, arrivals: Boolean, out: MutableList<Flight>, st: State): Int {
        val plugin = root.o("result").o("response").o("airport").o("pluginData")
        if (st.name.isBlank()) st.name = plugin.o("details").s("name")
        val data: JSONArray = plugin.o("schedule").o(if (arrivals) "arrivals" else "departures")
            ?.optJSONArray("data") ?: return 0

        for (i in 0 until data.length()) {
            val f = data.optJSONObject(i).o("flight") ?: continue
            val number = f.o("identification").o("number").s("default")
            if (number.isBlank()) continue

            val ap = f.o("airport")
            val home = ap.o(if (arrivals) "destination" else "origin")
            val other = ap.o(if (arrivals) "origin" else "destination")

            if (!st.tzSet) {
                val off = home.o("timezone")
                if (off != null && !off.isNull("offset")) {
                    st.tz = off.optLong("offset", 0L)
                    st.tzSet = true
                }
            }

            val hc = home.o("position").o("country")
            val oc = other.o("position").o("country")
            val hCode = hc.s("code")
            val oCode = oc.s("code")
            val intl = when {
                hCode.isNotBlank() && oCode.isNotBlank() -> !hCode.equals(oCode, ignoreCase = true)
                hc.s("name").isNotBlank() && oc.s("name").isNotBlank() ->
                    !hc.s("name").equals(oc.s("name"), ignoreCase = true)
                else -> true
            }

            val key = if (arrivals) "arrival" else "departure"
            val t = f.o("time")

            out += Flight(
                number = number,
                airline = f.o("airline").s("name"),
                aircraft = f.o("aircraft").o("model").s("text"),
                otherCity = other.o("position").o("region").s("city").ifBlank { other.s("name") },
                otherIata = other.o("code").s("iata"),
                international = intl,
                scheduled = t.o("scheduled").l(key),
                estimated = t.o("estimated").l(key),
                real = t.o("real").l(key),
                status = f.o("status").s("text"),
                isArrival = arrivals,
            )
        }
        return data.length()
    }
}
