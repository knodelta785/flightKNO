package com.kno.flightinfo.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.kno.flightinfo.R
import com.kno.flightinfo.data.Board
import com.kno.flightinfo.data.Flight
import com.kno.flightinfo.data.Fmt
import com.kno.flightinfo.data.NotifStyle
import com.kno.flightinfo.ui.BubbleActivity
import com.kno.flightinfo.ui.MainActivity

object Notifier {
    enum class Kind { UPCOMING, DELAY, CANCEL }

    const val CH_BAR = "flight_bar"
    const val CH_BUBBLE = "flight_bubble"

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_BAR, "Notifikasi flight (bar)", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifikasi flight gaya bar/banner biasa"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_BUBBLE, "Notifikasi flight (bubble)", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifikasi flight gaya bubble melayang"
                if (Build.VERSION.SDK_INT >= 29) setAllowBubbles(true)
            }
        )
    }

    fun send(ctx: Context, board: Board, f: Flight, kind: Kind, style: NotifStyle) {
        val tz = board.tzOffset
        val sched = Fmt.time(f.scheduled, tz)
        val eff = Fmt.time(f.effective, tz)
        val route = "${if (f.isArrival) "Dari" else "Ke"} ${f.otherCity.ifBlank { f.otherIata }} (${f.otherIata})"
        val verb = if (f.isArrival) "mendarat" else "berangkat"

        val title = when (kind) {
            Kind.UPCOMING -> "Segera $verb · ${f.number}"
            Kind.DELAY -> "Delay ${f.delayMinutes} menit · ${f.number}"
            Kind.CANCEL -> "Dibatalkan · ${f.number}"
        }
        val body = buildString {
            if (f.airline.isNotBlank()) append(f.airline).append('\n')
            append(route).append('\n')
            append("${board.airport} · jadwal $sched")
            if (kind != Kind.CANCEL && f.effective != f.scheduled) append(" → estimasi $eff")
        }
        post(ctx, f.key.hashCode(), title, body, f.airline.ifBlank { f.number }, style)
    }

    /** Notifikasi contoh untuk mencoba gaya yang dipilih. */
    fun sendTest(ctx: Context, style: NotifStyle) {
        post(
            ctx, 424242,
            "Delay 35 menit · AK 389",
            "AirAsia\nDari Kuala Lumpur (KUL)\nKNO · jadwal 14:30 → estimasi 15:05",
            "AirAsia", style,
        )
    }

    private fun post(ctx: Context, id: Int, title: String, body: String, sender: String, style: NotifStyle) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        ensureChannels(ctx)

        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val useBubble = style == NotifStyle.BUBBLE && Build.VERSION.SDK_INT >= 30
        val builder = NotificationCompat.Builder(ctx, if (useBubble) CH_BUBBLE else CH_BAR)
            .setSmallIcon(R.drawable.ic_flight)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(open)

        if (useBubble) {
            val person = Person.Builder().setName(sender.ifBlank { "Flight" }).setImportant(true).build()
            val shortcutId = "flight_${sender.hashCode()}"
            val icon = IconCompat.createWithResource(ctx, R.drawable.ic_flight)
            ShortcutManagerCompat.pushDynamicShortcut(
                ctx,
                ShortcutInfoCompat.Builder(ctx, shortcutId)
                    .setLongLived(true)
                    .setShortLabel(sender.ifBlank { "Flight" })
                    .setIntent(Intent(ctx, MainActivity::class.java).setAction(Intent.ACTION_VIEW))
                    .setPerson(person)
                    .setIcon(icon)
                    .build(),
            )
            val bubbleIntent = PendingIntent.getActivity(
                ctx, id,
                Intent(ctx, BubbleActivity::class.java)
                    .putExtra(BubbleActivity.EXTRA_TITLE, title)
                    .putExtra(BubbleActivity.EXTRA_BODY, body),
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val bubble = NotificationCompat.BubbleMetadata.Builder(bubbleIntent, icon)
                .setDesiredHeight(600)
                .setAutoExpandBubble(false)
                .setSuppressNotification(false)
                .build()
            builder
                .setShortcutId(shortcutId)
                .setBubbleMetadata(bubble)
                .setStyle(
                    NotificationCompat.MessagingStyle(person)
                        .setConversationTitle(title)
                        .addMessage(body.replace('\n', ' '), System.currentTimeMillis(), person)
                )
        } else {
            builder
                .setContentTitle(title)
                .setContentText(body.lines().drop(1).joinToString(" · "))
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        try {
            nm.notify(id, builder.build())
        } catch (_: SecurityException) {
            // izin notifikasi dicabut di tengah jalan; abaikan
        }
    }
}
