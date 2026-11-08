@file:OptIn(ExperimentalMaterial3Api::class)

package com.kno.flightinfo.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kno.flightinfo.R
import com.kno.flightinfo.data.Airports
import com.kno.flightinfo.data.AppSettings
import com.kno.flightinfo.data.Board
import com.kno.flightinfo.data.Flight
import com.kno.flightinfo.data.Fmt
import com.kno.flightinfo.data.NotifStyle
import com.kno.flightinfo.notify.Notifier

private val AMBER = Color(0xFFD97706)
private val RED = Color(0xFFDC2626)
private val GREEN = Color(0xFF16A34A)

@Composable
fun AppRoot(vm: MainViewModel) {
    val s by vm.settings.collectAsState()
    val arr by vm.arrivals.collectAsState()
    val dep by vm.departures.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Info Flight · ${s.airport}", fontWeight = FontWeight.Bold)
                            Text(Airports.name(s.airport), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    actions = {
                        IconButton(onClick = vm::refresh) { Icon(Icons.Default.Refresh, contentDescription = "Muat ulang") }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == 0, onClick = { tab = 0 },
                        icon = { Icon(painterResource(R.drawable.ic_flight), null, Modifier.rotate(180f)) },
                        label = { Text("Kedatangan") },
                    )
                    NavigationBarItem(
                        selected = tab == 1, onClick = { tab = 1 },
                        icon = { Icon(painterResource(R.drawable.ic_flight), null) },
                        label = { Text("Keberangkatan") },
                    )
                    NavigationBarItem(
                        selected = tab == 2, onClick = { tab = 2 },
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("Pengaturan") },
                    )
                }
            },
        ) { pad ->
            Column(Modifier.padding(pad).fillMaxSize()) {
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                when (tab) {
                    0 -> FlightList(arr, s, error)
                    1 -> FlightList(dep, s, error)
                    else -> SettingsScreen(s, vm::save)
                }
            }
        }
    }
}

@Composable
private fun FlightList(board: Board?, s: AppSettings, error: String?) {
    val list = remember(board, s.internationalOnly) {
        board?.flights.orEmpty()
            .filter { !s.internationalOnly || it.international }
            .sortedBy { it.effective }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (error != null) {
            item { Text("Gagal memuat: $error", color = MaterialTheme.colorScheme.error) }
        }
        if (list.isEmpty()) {
            item {
                Text(
                    if (board == null) "Memuat data…"
                    else if (s.internationalOnly) "Tidak ada penerbangan internasional saat ini."
                    else "Tidak ada penerbangan.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(list, key = { it.key }) { FlightCard(it, board?.tzOffset ?: 0L) }
    }
}

@Composable
private fun FlightCard(f: Flight, tz: Long) {
    val delayed = f.delayMinutes >= 15
    val accent = when {
        f.isCancelled -> RED
        delayed -> AMBER
        f.isDone -> GREEN
        else -> MaterialTheme.colorScheme.onSurface
    }
    Card(Modifier.fillMaxWidth().alpha(if (f.isDone) 0.6f else 1f)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(72.dp)) {
                Text(Fmt.time(f.effective, tz), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accent)
                if (f.effective != f.scheduled && f.scheduled > 0) {
                    Text(
                        Fmt.time(f.scheduled, tz), fontSize = 12.sp,
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "${f.number} · ${f.airline}", fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${if (f.isArrival) "Dari" else "Ke"} ${f.otherCity} (${f.otherIata})",
                    fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (f.aircraft.isNotBlank()) {
                    Text(f.aircraft, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val label = when {
                f.isCancelled -> "Dibatalkan"
                delayed && !f.isDone -> "Delay +${f.delayMinutes}m"
                else -> Fmt.statusId(f.status).substringBefore(' ')
            }
            Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.15f)) {
                Text(
                    label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

// ───────────────────────────── Pengaturan ─────────────────────────────

@Composable
private fun SettingsScreen(s: AppSettings, onSave: (AppSettings) -> Unit) {
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Title("Bandara")
        AirportPicker(s, onSave)
        SwitchRow("Hanya flight internasional", s.internationalOnly) { onSave(s.copy(internationalOnly = it)) }

        HorizontalDivider()
        Title("Notifikasi kedatangan (landing)")
        SwitchRow("Aktifkan", s.arrivalsOn) { onSave(s.copy(arrivalsOn = it)) }
        if (s.arrivalsOn) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Rentang jam landing")
                TimeButton(s.arrFrom) { onSave(s.copy(arrFrom = it)) }
                Text("–")
                TimeButton(s.arrTo) { onSave(s.copy(arrTo = it)) }
            }
        }

        HorizontalDivider()
        Title("Notifikasi keberangkatan")
        SwitchRow("Aktifkan", s.departuresOn) { onSave(s.copy(departuresOn = it)) }
        if (s.departuresOn) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Rentang jam berangkat")
                TimeButton(s.depFrom) { onSave(s.copy(depFrom = it)) }
                Text("–")
                TimeButton(s.depTo) { onSave(s.copy(depTo = it)) }
            }
        }
        Text(
            "Jam memakai waktu lokal bandara. Rentang boleh melewati tengah malam (mis. 22:00–05:00).",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text("Ingatkan sebelum jadwal")
        ChipRow(listOf(30, 60, 120, 180), s.leadMinutes, { "$it mnt" }) { onSave(s.copy(leadMinutes = it)) }

        HorizontalDivider()
        Title("Delay")
        SwitchRow("Beritahu jika delay", s.delayOn) { onSave(s.copy(delayOn = it)) }
        if (s.delayOn) {
            Text("Ambang delay")
            ChipRow(listOf(10, 15, 30, 60), s.delayThreshold, { "≥ $it mnt" }) { onSave(s.copy(delayThreshold = it)) }
        }

        HorizontalDivider()
        Title("Gaya notifikasi")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = s.style == NotifStyle.BAR, onClick = { onSave(s.copy(style = NotifStyle.BAR)) },
                label = { Text("Bar (banner)") },
            )
            FilterChip(
                selected = s.style == NotifStyle.BUBBLE, onClick = { onSave(s.copy(style = NotifStyle.BUBBLE)) },
                label = { Text("Bubble") },
            )
        }
        Text(
            "Bubble butuh Android 11+ dan harus diizinkan di pengaturan notifikasi sistem; jika tidak, otomatis tampil sebagai bar.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = { Notifier.sendTest(ctx, s.style) }) { Text("Kirim notifikasi contoh") }

        HorizontalDivider()
        Title("Pengecekan latar belakang")
        ChipRow(listOf(15, 30, 60), s.refreshMinutes, { "tiap $it mnt" }) { onSave(s.copy(refreshMinutes = it)) }
        Text(
            "Minimum 15 menit (batas Android). Saat hemat daya/Doze, pengecekan bisa tertunda.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Title(text: String) = Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ChipRow(options: List<Int>, selected: Int, label: (Int) -> String, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            FilterChip(selected = selected == o, onClick = { onSelect(o) }, label = { Text(label(o)) })
        }
    }
}

@Composable
private fun TimeButton(minutes: Int, onPick: (Int) -> Unit) {
    val ctx = LocalContext.current
    OutlinedButton(onClick = {
        TimePickerDialog(ctx, { _, h, m -> onPick(h * 60 + m) }, minutes / 60, minutes % 60, true).show()
    }) { Text(Fmt.hm(minutes)) }
}

@Composable
private fun AirportPicker(s: AppSettings, onSave: (AppSettings) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf("") }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("${s.airport} · ${Airports.name(s.airport)}", Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Airports.list.forEach { a ->
                DropdownMenuItem(
                    text = { Text("${a.iata} · ${a.name}") },
                    onClick = { onSave(s.copy(airport = a.iata)); expanded = false },
                )
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = custom,
            onValueChange = { custom = it.filter(Char::isLetter).uppercase().take(4) },
            label = { Text("Kode IATA/ICAO lain") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Button(onClick = {
            if (custom.length in 3..4) { onSave(s.copy(airport = custom)); custom = "" }
        }) { Text("Pakai") }
    }
}
