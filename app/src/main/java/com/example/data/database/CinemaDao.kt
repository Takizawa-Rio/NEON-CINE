package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Review
import com.example.data.model.Ticket
import com.example.data.model.PromoCode
import kotlinx.coroutines.flow.Flow

@Dao
interface CinemaDao {
    @Query("SELECT * FROM reviews WHERE movieId = :movieId ORDER BY timestamp DESC")
    fun getReviewsForMovie(movieId: Int): Flow<List<Review>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: Review)

    @Query("SELECT * FROM tickets ORDER BY timestamp DESC")
    fun getAllTickets(): Flow<List<Ticket>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: Ticket)

    @Query("DELETE FROM tickets WHERE id = :ticketId")
    suspend fun deleteTicket(ticketId: Int)

    @Query("DELETE FROM tickets WHERE id IN (:ticketIds)")
    suspend fun deleteTickets(ticketIds: List<Int>)

    @Query("DELETE FROM tickets")
    suspend fun deleteAllTickets()

    @Query("SELECT COUNT(*) FROM reviews WHERE movieId = :movieId")
    suspend fun getReviewCount(movieId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<Review>)

    @Query("SELECT * FROM promo_codes")
    fun getAllPromoCodes(): Flow<List<PromoCode>>

    @Query("SELECT * FROM promo_codes WHERE code = :code LIMIT 1")
    suspend fun getPromoCode(code: String): PromoCode?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromoCode(promoCode: PromoCode)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromoCodes(promoCodes: List<PromoCode>)

    @Query("UPDATE promo_codes SET isUsed = :isUsed WHERE code = :code")
    suspend fun updatePromoCodeUsage(code: String, isUsed: Boolean)

    @Query("SELECT COUNT(*) FROM promo_codes")
    suspend fun getPromoCodeCount(): Int
}
