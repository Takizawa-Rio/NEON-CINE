package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Movie
import com.example.data.model.Review
import com.example.data.model.Ticket
import com.example.data.model.PromoCode
import kotlinx.coroutines.flow.Flow

@Dao
interface CinemaDao {

    @Query("SELECT * FROM movies")
    fun getAllMovies(): Flow<List<Movie>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: Movie)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<Movie>)



    @Query("SELECT * FROM reviews WHERE movieId = :movieId ORDER BY timestamp DESC")
    fun getReviewsForMovie(movieId: String): Flow<List<Review>>

    @Query("SELECT COUNT(*) FROM reviews WHERE movieId = :movieId")
    suspend fun getReviewCount(movieId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: Review)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<Review>)



    @Query("SELECT * FROM tickets ORDER BY timestamp DESC")
    fun getAllTickets(): Flow<List<Ticket>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: Ticket)

    // Bổ sung thêm hàm chèn danh sách vé
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickets(tickets: List<Ticket>)



    @Query("SELECT * FROM promo_codes")
    fun getAllPromoCodes(): Flow<List<PromoCode>>

    @Query("SELECT * FROM promo_codes WHERE code = :code LIMIT 1")
    suspend fun getPromoCode(code: String): PromoCode?

    @Query("SELECT COUNT(*) FROM promo_codes")
    suspend fun getPromoCodeCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromoCode(promoCode: PromoCode)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromoCodes(promoCodes: List<PromoCode>)

    @Query("UPDATE promo_codes SET isUsed = :isUsed WHERE code = :code")
    suspend fun updatePromoCodeUsage(code: String, isUsed: Boolean)
}