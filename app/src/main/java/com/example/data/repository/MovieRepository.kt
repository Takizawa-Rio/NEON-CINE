package com.example.data.repository

import com.example.BuildConfig
import com.example.data.database.CinemaDao
import com.example.data.model.Movie
import com.example.data.model.Review
import com.example.data.model.Ticket
import com.example.data.model.PromoCode
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.Part
import com.example.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MovieRepository(private val cinemaDao: CinemaDao) {


    val allTickets: Flow<List<Ticket>> = cinemaDao.getAllTickets()
    val allPromoCodes: Flow<List<PromoCode>> = cinemaDao.getAllPromoCodes()
    val allMovies: Flow<List<Movie>> = cinemaDao.getAllMovies()

    fun getReviewsForMovie(movieId: String): Flow<List<Review>> {
        // Mỗi khi lấy đánh giá, tự động fetch dữ liệu mới nhất từ Supabase về Room
        com.example.data.supabase.SupabaseSyncService.fetchReviewsFromSupabase(movieId) { remoteReviews ->
            if (remoteReviews.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    cinemaDao.insertReviews(remoteReviews)
                }
            }
        }
        return cinemaDao.getReviewsForMovie(movieId)
    }

    // === 2. ĐỒNG BỘ DỮ LIỆU TỪ SUPABASE VỀ ROOM DB ===
    suspend fun syncInitialData() = withContext(Dispatchers.IO) {
        // Tải toàn bộ dữ liệu động từ Supabase khi mở app
        syncMoviesFromSupabase()
        syncPromoCodesFromSupabase()
        syncTicketsFromSupabase()
    }

    suspend fun syncMoviesFromSupabase() = withContext(Dispatchers.IO) {
        com.example.data.supabase.SupabaseSyncService.fetchMoviesFromSupabase { remoteMovies ->
            if (!remoteMovies.isNullOrEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    cinemaDao.insertMovies(remoteMovies)
                }
            }
        }
    }

    suspend fun syncTicketsFromSupabase() = withContext(Dispatchers.IO) {
        com.example.data.supabase.SupabaseSyncService.fetchTicketsFromSupabase { remoteTickets ->
            if (remoteTickets.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    cinemaDao.insertTickets(remoteTickets)
                }
            }
        }
    }

    suspend fun syncPromoCodesFromSupabase() = withContext(Dispatchers.IO) {
        com.example.data.supabase.SupabaseSyncService.fetchPromoCodesFromSupabase { remotePromos ->
            if (remotePromos.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    cinemaDao.insertPromoCodes(remotePromos)
                }
            }
        }
    }


    suspend fun addReview(review: Review) = withContext(Dispatchers.IO) {
        cinemaDao.insertReview(review)
        com.example.data.supabase.SupabaseSyncService.pushReviewToSupabase(review) {}
    }

    suspend fun bookTicket(ticket: Ticket) = withContext(Dispatchers.IO) {
        cinemaDao.insertTicket(ticket)
        com.example.data.supabase.SupabaseSyncService.pushTicketToSupabase(ticket) {}
    }

    suspend fun getPromoCode(code: String): PromoCode? = withContext(Dispatchers.IO) {
        cinemaDao.getPromoCode(code)
    }

    suspend fun updatePromoCodeUsage(code: String, isUsed: Boolean) = withContext(Dispatchers.IO) {
        cinemaDao.updatePromoCodeUsage(code, isUsed)
        com.example.data.supabase.SupabaseSyncService.updatePromoCodeUsageInSupabase(code, isUsed) {}
    }


    suspend fun analyzeReviewsWithAI(movieId: String, movieTitle: String, reviews: List<Review>): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (reviews.isEmpty()) {
            return@withContext "Chưa có đánh giá nào cho phim $movieTitle để AI phân tích."
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Vui lòng cấu hình Gemini API Key để sử dụng tính năng phân tích AI."
        }

        val reviewsText = reviews.joinToString("\n") { "- Tác giả ${it.author} (${it.rating} sao): ${it.content}" }
        val systemPrompt = "Bạn là Trợ lý AI phân tích đánh giá phim chuyên nghiệp của Momo Cinema. Trả lời bằng tiếng Việt lịch sự, thân thiện và khách quan."
        val prompt = """
            Dưới đây là danh sách các đánh giá thực tế từ khách hàng cho bộ phim: "$movieTitle".

            Các đánh giá:
            $reviewsText

            Nhiệm vụ của bạn là phân tích tổng hợp các đánh giá trên và cung cấp một báo cáo chi tiết bằng tiếng Việt định dạng Markdown sạch sẽ.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Momo AI không thể tạo phân tích vào lúc này. Vui lòng thử lại sau!"
        } catch (e: Exception) {
            "Lỗi kết nối AI: ${e.localizedMessage}"
        }
    }
}