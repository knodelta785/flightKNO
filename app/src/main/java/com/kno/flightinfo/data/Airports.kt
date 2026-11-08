package com.kno.flightinfo.data

data class AirportInfo(val iata: String, val name: String)

object Airports {
    /** Daftar pilihan cepat. Pengguna juga bisa mengetik kode IATA/ICAO lain di Pengaturan. */
    val list = listOf(
        AirportInfo("KNO", "Kualanamu, Medan"),
        AirportInfo("CGK", "Soekarno-Hatta, Jakarta"),
        AirportInfo("HLP", "Halim Perdanakusuma, Jakarta"),
        AirportInfo("DPS", "Ngurah Rai, Bali"),
        AirportInfo("SUB", "Juanda, Surabaya"),
        AirportInfo("YIA", "Yogyakarta Intl"),
        AirportInfo("BTH", "Hang Nadim, Batam"),
        AirportInfo("PDG", "Minangkabau, Padang"),
        AirportInfo("PKU", "Sultan Syarif Kasim II, Pekanbaru"),
        AirportInfo("BPN", "SAMS Sepinggan, Balikpapan"),
        AirportInfo("UPG", "Sultan Hasanuddin, Makassar"),
        AirportInfo("SIN", "Changi, Singapura"),
        AirportInfo("KUL", "Kuala Lumpur Intl"),
        AirportInfo("PEN", "Penang Intl"),
        AirportInfo("BKK", "Suvarnabhumi, Bangkok"),
        AirportInfo("DMK", "Don Mueang, Bangkok"),
        AirportInfo("HKG", "Hong Kong Intl"),
        AirportInfo("ICN", "Incheon, Seoul"),
        AirportInfo("NRT", "Narita, Tokyo"),
        AirportInfo("DOH", "Hamad, Doha"),
        AirportInfo("JED", "King Abdulaziz, Jeddah"),
        AirportInfo("SYD", "Kingsford Smith, Sydney"),
    )

    fun name(code: String): String =
        list.firstOrNull { it.iata.equals(code, ignoreCase = true) }?.name ?: code.uppercase()
}
