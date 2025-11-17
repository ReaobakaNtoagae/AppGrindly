package com.example.grindlyapp1.network

import org.xml.sax.Locator

data class HustlerBooking(
    var id: String = "",
    val hustlerId: String = "",
    val serviceTitle: String = "",
    val date: String = "",
    val status: String = ""
) {
    fun isUpcoming(): Boolean {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
            val bookingDate = sdf.parse(date)
            bookingDate?.after(java.util.Date()) ?: false
        } catch (e: Exception) {
            false
        }
    }
}
