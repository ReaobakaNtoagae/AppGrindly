package com.example.grindlyapp1.network


data class Booking(
    val bookingId: String = "",
    val clientId: String = "",
    val hustlerId: String = "",
    val serviceId: String? = null,
    val serviceTitle: String? = null,
    val date: String = "",
    val price: Double? = null,
    val location: String = "",
    val paymentMethod: String = "",
    val notes: String = "",
    var status: String = "",


    val createdAt: String? = null,
    val updatedAt: String? = null,

    val client: ClientInfo? = null,
    val hustler: HustlerProfile? = null,
    val service: Service? = null,
) {
    fun isUpcoming(): Boolean {
        return try {
            val bookingDate = java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                java.util.Locale.getDefault()
            ).parse(date)

            bookingDate?.after(java.util.Date()) ?: false
        } catch (e: Exception) {
            false
        }
    }
}

data class ClientInfo(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = ""
)


data class BookingRequest(
    val clientId: String,
    val hustlerId: String,
    val serviceId: String? = null,
    val serviceTitle: String,
    val date: String,
    val price: Double,
    val location: String,
    val paymentMethod: String = "",
    val notes: String = ""
)


data class BookingStatusUpdateRequest(
    val status: String
)

data class BookingStatusUpdateResponse(
    val message: String,
    val booking: Booking
)

