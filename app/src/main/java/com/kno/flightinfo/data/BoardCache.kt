package com.kno.flightinfo.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Cache terakhir papan penerbangan di penyimpanan internal; dipakai UI dan widget. */
object BoardCache {
    private fun file(ctx: Context, arrivals: Boolean) =
        File(ctx.applicationContext.filesDir, if (arrivals) "arrivals.json" else "departures.json")

    suspend fun save(ctx: Context, board: Board) = withContext(Dispatchers.IO) {
        file(ctx, board.isArrival).writeText(board.toJson())
    }

    suspend fun load(ctx: Context, arrivals: Boolean): Board? = withContext(Dispatchers.IO) {
        runCatching {
            val f = file(ctx, arrivals)
            if (f.exists()) Board.fromJson(f.readText()) else null
        }.getOrNull()
    }

    suspend fun clear(ctx: Context) = withContext(Dispatchers.IO) {
        file(ctx, true).delete()
        file(ctx, false).delete()
    }
}
