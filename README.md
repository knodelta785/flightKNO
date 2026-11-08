# Flight KNO — info kedatangan & keberangkatan flight internasional

Aplikasi Android (Kotlin, Jetpack Compose, Glance widget, WorkManager).
Bandara default **Kualanamu (KNO)**, bisa diganti di Pengaturan (daftar cepat atau ketik kode IATA/ICAO lain).

## Cara membuka & menjalankan
1. Buka folder ini di **Android Studio** (Ladybug / 2024.2 atau lebih baru). Biarkan Gradle sync
   (Studio akan mengunduh Gradle 8.9 dan dependensi; butuh internet).
2. Jalankan ke HP/emulator Android 8.0+ (minSdk 26).
3. Izinkan notifikasi saat diminta. Tambahkan widget "Flight KNO" dari layar utama (tekan lama → Widget).

## Fitur
- Tab **Kedatangan** dan **Keberangkatan**: jam (estimasi vs jadwal), maskapai, asal/tujuan, pesawat, status, penanda delay/batal.
- Filter **hanya internasional** (negara asal/tujuan ≠ negara bandara).
- **Notifikasi custom** (Pengaturan):
  - rentang jam landing dan rentang jam berangkat (waktu lokal bandara, boleh lewat tengah malam),
  - diingatkan 30/60/120/180 menit sebelum jadwal,
  - gaya **Bar** (banner biasa) atau **Bubble** (Android 11+),
  - notifikasi **delay** (ambang 10/15/30/60 menit) dan **pembatalan**,
  - tombol "Kirim notifikasi contoh".
- **Widget** layar utama: 3 kedatangan + 3 keberangkatan internasional berikutnya, tombol ⟳, ketuk untuk membuka app.
- Pengecekan latar belakang tiap 15/30/60 menit (WorkManager).

## Struktur kode (app/src/main/java/com/kno/flightinfo)
| Folder | Isi |
|---|---|
| `data/` | model, `Fr24Client` (sumber data), pengaturan (DataStore), cache |
| `notify/` | `Notifier` (bar/bubble), `NotificationEngine` (aturan kapan notif dikirim) |
| `work/` | `SyncCore`, `FlightSyncWorker`, `Scheduler` |
| `widget/` | widget Glance |
| `ui/` | Compose UI, ViewModel, BubbleActivity |

## PENTING: sumber data Flightradar24
`data/Fr24Client.kt` memakai endpoint papan bandara yang dipakai situs flightradar24.com.
**Ini bukan API resmi**: formatnya bisa berubah, bisa diblokir/dibatasi, dan ketentuan layanan
Flightradar24 melarang penggunaan ulang data tanpa izin. Cocok untuk pemakaian pribadi/percobaan.
Untuk dipublikasikan, pakai API resmi berlangganan Flightradar24 (https://fr24api.flightradar24.com)
dan ganti isi `Fr24Client.fetch()` — bagian lain aplikasi tidak perlu diubah.

## Catatan
- Proyek ini belum dikompilasi di lingkungan pembuatnya; jika Gradle sync/build menampilkan error kecil
  (versi library, import), perbaiki sesuai pesan Android Studio.
- WorkManager membatasi interval minimum 15 menit, dan Android dapat menunda saat Doze/hemat daya.
  Untuk notifikasi yang lebih real-time diperlukan server push.
