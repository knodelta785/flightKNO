package com.kno.flightinfo.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kno.flightinfo.data.AppSettings
import com.kno.flightinfo.data.Board
import com.kno.flightinfo.data.BoardCache
import com.kno.flightinfo.data.Flight
import com.kno.flightinfo.data.Fmt
import com.kno.flightinfo.data.SettingsStore
import com.kno.flightinfo.ui.MainActivity
import com.kno.flightinfo.work.Scheduler

private val BG = Color(0xFF0F172A)
private val WHITE = Color(0xFFF8FAFC)
private val MUTED = Color(0xFF94A3B8)
private val GREEN = Color(0xFF4ADE80)
private val AMBER = Color(0xFFFBBF24)
private val RED = Color(0xFFF87171)

class FlightWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val s = SettingsStore.read(context)
        val arr = BoardCache.load(context, true)
        val dep = BoardCache.load(context, false)
        provideContent { WidgetContent(s, arr, dep) }
    }
}

class FlightWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FlightWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Scheduler.refreshNow(context)
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: androidx.glance.action.ActionParameters) {
        Scheduler.refreshNow(context)
    }
}

private fun upcoming(b: Board?, s: AppSettings): List<Flight> =
    b?.flights.orEmpty()
        .filter { !it.isDone && (!s.internationalOnly || it.international) }
        .sortedBy { it.effective }
        .take(3)

@Composable
private fun WidgetContent(s: AppSettings, arr: Board?, dep: Board?) {
    val updated = (arr ?: dep)?.let { Fmt.time(it.updatedAt, it.tzOffset) } ?: "--:--"
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(BG)
            .cornerRadius(20.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "✈ ${s.airport}",
                style = TextStyle(color = ColorProvider(WHITE), fontSize = 15.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.defaultWeight())
            Text("upd $updated", style = TextStyle(color = ColorProvider(MUTED), fontSize = 10.sp))
            Text(
                "  ⟳",
                style = TextStyle(color = ColorProvider(WHITE), fontSize = 16.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.clickable(actionRunCallback<RefreshAction>()),
            )
        }
        Section("KEDATANGAN", arr, upcoming(arr, s))
        Section("KEBERANGKATAN", dep, upcoming(dep, s))
    }
}

@Composable
private fun Section(title: String, board: Board?, list: List<Flight>) {
    Text(
        title,
        style = TextStyle(color = ColorProvider(MUTED), fontSize = 10.sp, fontWeight = FontWeight.Bold),
        modifier = GlanceModifier.padding(top = 6.dp, bottom = 2.dp),
    )
    if (list.isEmpty()) {
        Text(
            if (board == null) "Ketuk ⟳ untuk memuat" else "Tidak ada penerbangan",
            style = TextStyle(color = ColorProvider(MUTED), fontSize = 11.sp),
        )
        return
    }
    val tz = board?.tzOffset ?: 0L
    list.forEach { f -> FlightRow(f, tz) }
}

@Composable
private fun FlightRow(f: Flight, tz: Long) {
    val delay = f.delayMinutes
    val (tag, color) = when {
        f.isCancelled -> "BATAL" to RED
        delay >= 15 -> "+${delay}m" to AMBER
        else -> "tepat" to GREEN
    }
    Row(GlanceModifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            Fmt.time(f.effective, tz),
            style = TextStyle(color = ColorProvider(if (delay >= 15 || f.isCancelled) color else WHITE), fontSize = 12.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.width(44.dp),
        )
        Text(
            f.number,
            style = TextStyle(color = ColorProvider(WHITE), fontSize = 12.sp),
            maxLines = 1,
            modifier = GlanceModifier.width(64.dp),
        )
        Text(
            f.otherIata.ifBlank { f.otherCity },
            style = TextStyle(color = ColorProvider(MUTED), fontSize = 12.sp),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(tag, style = TextStyle(color = ColorProvider(color), fontSize = 11.sp, fontWeight = FontWeight.Medium))
    }
}
