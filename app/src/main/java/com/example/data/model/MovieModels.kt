package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

data class Movie(
    val id: Int,
    val title: String,
    val genre: String,
    val duration: Int, // minutes
    val rating: Float, // average user rating e.g. 4.8
    @Json(name = "age_rating") val ageRating: String, // e.g. T18, T16, P
    @Json(name = "release_date") val releaseDate: String,
    val synopsis: String,
    @Json(name = "poster_url") val posterUrl: String,
    @Json(name = "banner_url") val bannerUrl: String,
    @Json(name = "is_now_showing") val isNowShowing: Boolean = true,
    val director: String = "N/A",
    val cast: String = "N/A"
)

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @Json(name = "movie_id") val movieId: Int,
    val author: String,
    val rating: Int, // 1 to 5 stars
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tickets")
data class Ticket(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @Json(name = "movie_id") val movieId: Int,
    @Json(name = "movie_title") val movieTitle: String,
    @Json(name = "movie_poster") val moviePoster: String,
    val cinema: String,
    @Json(name = "date_time") val dateTime: String,
    val seats: String,
    @Json(name = "total_price") val totalPrice: Int,
    val combo: String,
    val barcode: String,
    val timestamp: Long = System.currentTimeMillis(),
    @Json(name = "user_email") val userEmail: String = "",
    @Json(name = "user_name") val userName: String = "",
    @Json(name = "promo_code") val promoCode: String = ""
)

data class UserProfile(
    val email: String,
    val name: String,
    val points: Int = 150,
    val balance: Int = 500000
)

@Entity(tableName = "promo_codes")
data class PromoCode(
    @PrimaryKey val code: String,
    @Json(name = "discount_amount") val discountAmount: Int,
    val description: String,
    @Json(name = "is_used") val isUsed: Boolean = false
)

data class UserNotification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val timestamp: String,
    val type: String = "info" // "booking", "promo", "system"
)

