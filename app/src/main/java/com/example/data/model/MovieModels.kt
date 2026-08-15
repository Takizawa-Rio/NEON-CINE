package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

data class Movie(
    val id: Int = 0,
    val title: String = "",
    val genre: String = "",
    val duration: Int = 120, // minutes
    val rating: Float = 4.8f, // average user rating e.g. 4.8
    @Json(name = "age_rating") val ageRating: String = "T18", // e.g. T18, T16, P
    @Json(name = "release_date") val releaseDate: String = "2026",
    val synopsis: String = "",
    @Json(name = "poster_url") val posterUrl: String = "",
    @Json(name = "banner_url") val bannerUrl: String = "",
    @Json(name = "is_now_showing") val isNowShowing: Boolean = true,
    val director: String = "N/A",
    val cast: String = "N/A",
    val price: Int = 0,
    val stringId: String = ""
)

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @Json(name = "movieId") val movieId: Int = 0,
    val author: String = "",
    val rating: Int = 5, // 1 to 5 stars
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tickets")
data class Ticket(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @Json(name = "movie_id") val movieId: Int = 0,
    @Json(name = "movie_title") val movieTitle: String = "",
    @Json(name = "movie_poster") val moviePoster: String = "",
    val cinema: String = "Neon Cine Space",
    @Json(name = "date_time") val dateTime: String = "",
    val seats: String = "",
    @Json(name = "total_price") val totalPrice: Int = 0,
    val combo: String = "",
    val barcode: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    @Json(name = "user_email") val userEmail: String = "",
    @Json(name = "user_name") val userName: String = "",
    @Json(name = "promo_code") val promoCode: String = ""
)

data class UserProfile(
    val email: String = "",
    val name: String = "",
    val points: Int = 150,
    val balance: Int = 500000
)

@Entity(tableName = "promo_codes")
data class PromoCode(
    @PrimaryKey val code: String = "",
    @Json(name = "discount_amount") val discountAmount: Int = 0,
    val description: String = "",
    @Json(name = "is_used") val isUsed: Boolean = false
)

data class UserNotification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val timestamp: String,
    val type: String = "info" // "booking", "promo", "system"
)

data class Product(
    val id: Int = 0,
    val name: String = "",
    val price: Int = 0,
    @Json(name = "image_url") val imageUrl: String = "",
    val description: String = "",
    val type: String = "combo"
)

data class Showtime(
    val id: Int = 0,
    @Json(name = "movie_id") val movieId: Int = 0,
    val movieStringId: String = "",
    @Json(name = "start_time") val startTime: String = "",
    @Json(name = "end_time") val endTime: String = "",
    val price: Int = 0,
    @Json(name = "regular_price") val regularPrice: Double? = null,
    @Json(name = "vip_price") val vipPrice: Double? = null,
    @Json(name = "vip_percent") val vipPercent: Double? = null,
    @Json(name = "show_date") val date: String = "",
    @Json(name = "room_id") val roomId: String = "",
    val cinema: String = "Neon Cine Space - Vincom Xuân Khánh"
) {
    val effectivePrice: Int
        get() {
            val reg = regularPrice?.toInt() ?: 0
            if (reg > 0) return reg
            return price
        }

    val effectiveVipPrice: Int
        get() {
            val vip = vipPrice?.toInt() ?: 0
            if (vip > 0) return vip
            if (vipPercent != null && effectivePrice > 0) {
                return if (vipPercent > 5.0) {
                    (effectivePrice * (1.0 + vipPercent / 100.0)).toInt()
                } else {
                    (effectivePrice * vipPercent).toInt()
                }
            }
            return effectivePrice
        }
}

data class Booking(
    val id: Int = 0,
    @Json(name = "movie_id") val movieId: Int = 0,
    @Json(name = "showtime_id") val showtimeId: Int = 0,
    val seats: String = "",
    @Json(name = "total_price") val totalPrice: Int = 0,
    @Json(name = "user_email") val userEmail: String = ""
)

data class SeatItem(
    val code: String = "",
    val row: String = "",
    val col: Int = 0,
    val type: String = "NORMAL" // "NORMAL", "VIP", "COUPLE"
)

data class ScreeningRoom(
    val id: String = "",
    val name: String = "",
    val totalSeats: Int = 0,
    val regularSeats: Int = 0,
    val vipSeats: Int = 0,
    val rowsCount: Int = 0,
    val colsCount: Int = 0,
    val hasAisle: Boolean = true,
    val seatLayout: List<SeatItem> = emptyList(),
    val status: String = "HOAT_DONG"
)




