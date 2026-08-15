package com.example.data.supabase

import android.util.Log
import com.example.data.model.Movie
import com.example.data.model.Review
import com.example.data.model.Ticket
import com.example.data.model.UserProfile
import com.example.data.model.PromoCode
import com.example.data.model.Product
import com.example.data.model.Showtime
import com.example.data.model.Booking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * SupabaseSyncService - Bộ đồng bộ hóa dữ liệu thời gian thực giữa ứng dụng Kotlin và Supabase PostgreSQL.
 * Sử dụng Supabase REST API (PostgREST) qua OkHttp để đảm bảo hiệu năng cao, nhẹ và độc lập.
 */
object SupabaseSyncService {
    private const val TAG = "SupabaseSync"

    // Thông tin Supabase
    private const val SUPABASE_URL = "https://rdfnidhtqoyshjawnxqt.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJkZm5pZGh0cW95c2hqYXdueHF0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODM0MjUwNTcsImV4cCI6MjA5OTAwMTA1N30.nVMPKkGdcy_pSGseK2mdxBkg9skOzB69GMpyK-iSApw"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Đồng bộ danh sách đánh giá của phim từ Supabase table 'reviews'
     */
    fun fetchReviewsFromSupabase(movieId: Int, onResult: (List<Review>) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id")) {
            return
        }

        // Supabase reviews table uses movieId column
        val url = "$SUPABASE_URL/rest/v1/reviews?movieId=eq.$movieId&select=*"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Lỗi fetch reviews từ Supabase: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        // Thử fallback query không lọc hoặc lọc theo movie_id nếu cần
                        fetchReviewsFallback(movieId, onResult)
                        return
                    }
                    val bodyString = response.body?.string() ?: return
                    val reviews = parseReviewsJson(bodyString)
                    onResult(reviews)
                    Log.d(TAG, "Fetch thành công ${reviews.size} reviews từ Supabase!")
                }
            }
        })
    }

    private fun fetchReviewsFallback(movieId: Int, onResult: (List<Review>) -> Unit) {
        val url = "$SUPABASE_URL/rest/v1/reviews?select=*"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) return
                    val bodyString = response.body?.string() ?: return
                    val allReviews = parseReviewsJson(bodyString)
                    val filtered = if (movieId > 0) allReviews.filter { it.movieId == movieId } else allReviews
                    onResult(filtered)
                }
            }
        })
    }

    private fun parseReviewsJson(bodyString: String): List<Review> {
        val list = mutableListOf<Review>()
        try {
            val jsonArray = JSONArray(bodyString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val mId = when {
                    obj.has("movieId") -> obj.optInt("movieId", 0)
                    obj.has("movie_id") -> obj.optInt("movie_id", 0)
                    else -> 0
                }
                list.add(
                    Review(
                        id = obj.optInt("id", i + 1),
                        movieId = mId,
                        author = obj.optString("author", "Khán giả Neon"),
                        rating = obj.optInt("rating", 5),
                        content = obj.optString("content", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi parse reviews: ${e.message}")
        }
        return list
    }

    /**
     * Đẩy đánh giá mới lên Supabase table 'reviews'
     */
    fun pushReviewToSupabase(review: Review, onComplete: (Boolean) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id")) {
            onComplete(true)
            return
        }

        val url = "$SUPABASE_URL/rest/v1/reviews"
        val json = JSONObject().apply {
            put("movieId", review.movieId)
            put("author", review.author)
            put("rating", review.rating)
            put("content", review.content)
            put("timestamp", review.timestamp)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(json.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Lỗi kết nối đẩy Review lên Supabase: ${e.message}")
                onComplete(false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    Log.d(TAG, "Đã gửi Review lên Supabase: ${response.code}")
                    onComplete(response.isSuccessful)
                }
            }
        })
    }

    /**
     * Đồng bộ danh sách vé đã đặt từ Supabase table 'tickets'
     */
    fun fetchTicketsFromSupabase(onResult: (List<Ticket>) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id")) {
            return
        }

        val url = "$SUPABASE_URL/rest/v1/tickets?select=*&order=created_at.desc"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Lỗi fetch tickets từ Supabase: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        fetchTicketsFallback(onResult)
                        return
                    }
                    val bodyString = response.body?.string() ?: return
                    val tickets = parseTicketsJson(bodyString)
                    onResult(tickets)
                    Log.d(TAG, "Fetch thành công ${tickets.size} tickets từ Supabase!")
                }
            }
        })
    }

    private fun fetchTicketsFallback(onResult: (List<Ticket>) -> Unit) {
        val url = "$SUPABASE_URL/rest/v1/tickets?select=*"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) return
                    val bodyString = response.body?.string() ?: return
                    val tickets = parseTicketsJson(bodyString)
                    onResult(tickets)
                }
            }
        })
    }

    private fun parseTicketsJson(bodyString: String): List<Ticket> {
        val list = mutableListOf<Ticket>()
        try {
            val jsonArray = JSONArray(bodyString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val mId = when {
                    obj.has("movieId") -> obj.optInt("movieId", 0)
                    obj.has("movie_id") -> obj.optInt("movie_id", 0)
                    else -> 0
                }
                list.add(
                    Ticket(
                        id = obj.optInt("id", i + 1),
                        movieId = mId,
                        movieTitle = obj.optString("movie_title", obj.optString("movieTitle", "Phim")),
                        moviePoster = obj.optString("movie_poster", obj.optString("moviePoster", "")),
                        cinema = obj.optString("cinema", "Neon Cine Space"),
                        dateTime = obj.optString("date_time", obj.optString("dateTime", "")),
                        seats = obj.optString("seats", obj.optString("seat_code", "")),
                        totalPrice = obj.optInt("total_price", obj.optInt("price", 0)),
                        combo = obj.optString("combo", ""),
                        barcode = obj.optString("barcode", "NEON-${System.currentTimeMillis() % 100000}"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        userEmail = obj.optString("user_email", obj.optString("userEmail", "")),
                        userName = obj.optString("user_name", obj.optString("userName", "")),
                        promoCode = obj.optString("promo_code", obj.optString("promoCode", ""))
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi parse tickets: ${e.message}")
        }
        return list
    }

    /**
     * Đẩy vé đã thanh toán lên Supabase table 'tickets'
     */
    fun pushTicketToSupabase(ticket: Ticket, onComplete: (Boolean) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id")) {
            onComplete(true)
            return
        }

        val url = "$SUPABASE_URL/rest/v1/tickets"
        val json = JSONObject().apply {
            put("price", ticket.totalPrice)
            put("seat_code", ticket.seats)
            put("status", "paid")
        }.toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(json.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Lỗi kết nối đẩy vé lên Supabase: ${e.message}")
                onComplete(false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    Log.d(TAG, "Đã lưu vé lên Supabase: ${response.code}")
                    onComplete(response.isSuccessful)
                }
            }
        })
    }

    /**
     * Đẩy Profile người dùng lên Supabase table 'profiles'
     */
    fun pushProfileToSupabase(profile: UserProfile, onComplete: (Boolean) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id") || profile.email.isBlank()) {
            onComplete(true)
            return
        }

        val url = "$SUPABASE_URL/rest/v1/profiles"
        val json = JSONObject().apply {
            put("email", profile.email)
            put("name", profile.name)
            put("points", profile.points)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .post(json.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Lỗi kết nối đẩy Profile lên Supabase: ${e.message}")
                onComplete(false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    Log.d(TAG, "Đã lưu Profile lên Supabase: ${response.code}")
                    onComplete(response.isSuccessful)
                }
            }
        })
    }

    /**
     * Đồng bộ và tìm kiếm Profile người dùng từ Supabase table 'profiles' theo email
     */
    fun fetchProfileFromSupabase(email: String, onResult: (UserProfile?) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id") || email.isBlank()) {
            onResult(null)
            return
        }

        val url = "$SUPABASE_URL/rest/v1/profiles?email=eq.$email&select=*"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Lỗi fetch profile từ Supabase: ${e.message}")
                onResult(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Supabase profile phản hồi lỗi: ${response.code}")
                        onResult(null)
                        return
                    }
                    val bodyString = response.body?.string() ?: ""
                    try {
                        val jsonArray = JSONArray(bodyString)
                        if (jsonArray.length() > 0) {
                            val obj = jsonArray.getJSONObject(0)
                            onResult(
                                UserProfile(
                                    email = obj.optString("email", email),
                                    name = obj.optString("name", "Thành viên Neon"),
                                    points = obj.optInt("points", 150),
                                    balance = 500000
                                )
                            )
                        } else {
                            onResult(null)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi phân tích JSON profile: ${e.message}")
                        onResult(null)
                    }
                }
            }
        })
    }

    /**
     * Đồng bộ danh sách phim từ Supabase table 'movies'
     */
    fun fetchMoviesFromSupabase(onResult: (List<Movie>?) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id")) {
            onResult(null)
            return
        }

        val url = "$SUPABASE_URL/rest/v1/movies?select=*"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Lỗi fetch movies từ Supabase: ${e.message}")
                onResult(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Supabase movies phản hồi lỗi: ${response.code}")
                        onResult(null)
                        return
                    }
                    val bodyString = response.body?.string() ?: ""
                    val movies = parseMoviesJson(bodyString)
                    onResult(movies)
                }
            }
        })
    }

    private fun parseMoviesJson(bodyString: String): List<Movie> {
        val list = mutableListOf<Movie>()
        try {
            val jsonArray = JSONArray(bodyString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val idVal = obj.opt("id")
                val stringId = idVal?.toString() ?: ""
                val intId = when (idVal) {
                    is Int -> idVal
                    is Number -> idVal.toInt()
                    is String -> idVal.toIntOrNull() ?: kotlin.math.abs(idVal.hashCode())
                    else -> i + 1
                }
                val rawRating = if (!obj.isNull("rating")) obj.optDouble("rating", 4.8).toFloat() else 4.8f
                list.add(
                    Movie(
                        id = intId,
                        title = obj.optString("title", "Phim Mới"),
                        genre = obj.optString("genre", "Phim Chiếu Rạp"),
                        duration = obj.optInt("duration", 120),
                        rating = rawRating,
                        ageRating = if (obj.has("age_rating")) obj.optString("age_rating") else obj.optString("ageRating", "T18"),
                        releaseDate = if (obj.has("release_date")) obj.optString("release_date") else obj.optString("releaseDate", "2026"),
                        synopsis = obj.optString("synopsis", ""),
                        posterUrl = if (obj.has("poster_url")) obj.optString("poster_url") else obj.optString("posterUrl", ""),
                        bannerUrl = if (obj.has("banner_url")) obj.optString("banner_url") else obj.optString("bannerUrl", ""),
                        isNowShowing = if (obj.has("is_now_showing")) obj.optBoolean("is_now_showing") else obj.optBoolean("isNowShowing", true),
                        director = obj.optString("director", "Đang cập nhật"),
                        cast = obj.optString("cast", "Đang cập nhật"),
                        price = obj.optInt("price", 0),
                        stringId = stringId
                    )
                )
            }
            Log.d(TAG, "Đã tải thành công ${list.size} phim từ Supabase!")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi phân tích JSON movies: ${e.message}")
        }
        return list
    }

    /**
     * Đồng bộ danh sách sản phẩm / combo bắp nước từ Supabase table 'products' hoặc 'combos'
     */
    fun fetchProductsFromSupabase(onResult: (List<Product>) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id")) return

        val url = "$SUPABASE_URL/rest/v1/products?select=*"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                fetchCombosFromSupabase(onResult)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        fetchCombosFromSupabase(onResult)
                        return
                    }
                    val bodyString = response.body?.string() ?: ""
                    val list = parseProductsJson(bodyString)
                    if (list.isNotEmpty()) {
                        onResult(list)
                        Log.d(TAG, "Fetch thành công ${list.size} products từ Supabase!")
                    } else {
                        fetchCombosFromSupabase(onResult)
                    }
                }
            }
        })
    }

    fun fetchCombosFromSupabase(onResult: (List<Product>) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id")) return

        val url = "$SUPABASE_URL/rest/v1/combos?select=*"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Lỗi fetch combos từ Supabase: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) return
                    val bodyString = response.body?.string() ?: ""
                    val list = parseProductsJson(bodyString)
                    if (list.isNotEmpty()) {
                        onResult(list)
                        Log.d(TAG, "Fetch thành công ${list.size} combos từ Supabase!")
                    }
                }
            }
        })
    }

    private fun parseProductsJson(bodyString: String): List<Product> {
        val list = mutableListOf<Product>()
        try {
            val jsonArray = JSONArray(bodyString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = when {
                    obj.has("id") -> obj.optInt("id", i + 1)
                    else -> i + 1
                }
                val name = when {
                    obj.has("name") -> obj.optString("name")
                    obj.has("ten") -> obj.optString("ten")
                    obj.has("ten_san_pham") -> obj.optString("ten_san_pham")
                    obj.has("ten_combo") -> obj.optString("ten_combo")
                    obj.has("title") -> obj.optString("title")
                    obj.has("combo_name") -> obj.optString("combo_name")
                    else -> "Combo Bắp + Nước"
                }
                var price = when {
                    obj.has("price") -> obj.optInt("price", 0)
                    obj.has("gia") -> obj.optInt("gia", 0)
                    obj.has("gia_tien") -> obj.optInt("gia_tien", 0)
                    obj.has("don_gia") -> obj.optInt("don_gia", 0)
                    obj.has("combo_price") -> obj.optInt("combo_price", 0)
                    else -> 0
                }
                val type = when {
                    obj.has("type") -> obj.optString("type")
                    obj.has("loai") -> obj.optString("loai")
                    name.contains("Nước", ignoreCase = true) || name.contains("Coca", ignoreCase = true) || name.contains("Sprite", ignoreCase = true) -> "drink"
                    name.contains("Combo", ignoreCase = true) -> "combo"
                    else -> "snack"
                }
                // Nếu bảng Supabase không có cột giá hoặc giá bằng 0, tự động gán giá hợp lý theo tên/loại
                if (price <= 0) {
                    price = when {
                        name.contains("Family", ignoreCase = true) || name.contains("Party", ignoreCase = true) -> 145000
                        name.contains("Couple", ignoreCase = true) || name.contains("Đôi", ignoreCase = true) -> 95000
                        name.contains("Combo", ignoreCase = true) -> 65000
                        name.contains("Nước", ignoreCase = true) || name.contains("Coca", ignoreCase = true) -> 32000
                        name.contains("Dasani", ignoreCase = true) || name.contains("Suối", ignoreCase = true) -> 20000
                        else -> 45000
                    }
                }
                val imageUrl = when {
                    obj.has("image_url") -> obj.optString("image_url")
                    obj.has("image") -> obj.optString("image")
                    obj.has("hinh_anh") -> obj.optString("hinh_anh")
                    obj.has("anh") -> obj.optString("anh")
                    else -> ""
                }
                val desc = when {
                    obj.has("description") -> obj.optString("description")
                    obj.has("mo_ta") -> obj.optString("mo_ta")
                    obj.has("desc") -> obj.optString("desc")
                    else -> if (type == "combo") "Bắp rang bơ giòn rụm + nước ngọt mát lạnh" else "Thức ăn / đồ uống phục vụ tại rạp"
                }
                list.add(Product(id, name, price, imageUrl, desc, type))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi parse JSON products/combos: ${e.message}")
        }
        return list
    }

    /**
     * Đồng bộ danh sách mã giảm giá từ Supabase table 'promo_codes'
     */
    fun fetchPromoCodesFromSupabase(onResult: (List<PromoCode>) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id")) {
            return
        }

        val url = "$SUPABASE_URL/rest/v1/promo_codes?select=*"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Lỗi fetch promo_codes từ Supabase: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Supabase promo_codes phản hồi lỗi: ${response.code}")
                        return
                    }
                    val bodyString = response.body?.string() ?: return
                    val promoCodes = parsePromoCodesJson(bodyString)
                    onResult(promoCodes)
                    Log.d(TAG, "Fetch thành công ${promoCodes.size} promo_codes từ Supabase!")
                }
            }
        })
    }

    private fun parsePromoCodesJson(bodyString: String): List<PromoCode> {
        val list = mutableListOf<PromoCode>()
        try {
            val jsonArray = JSONArray(bodyString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val code = obj.optString("code", "")
                val discountAmount = when {
                    obj.has("discount_amount") -> obj.optInt("discount_amount", 0)
                    obj.has("discountAmount") -> obj.optInt("discountAmount", 0)
                    obj.has("discount") -> obj.optInt("discount", 0)
                    else -> 0
                }
                val desc = obj.optString("description", "")
                val isUsed = if (obj.has("is_used")) obj.optBoolean("is_used") else obj.optBoolean("isUsed", false)
                if (code.isNotBlank()) {
                    list.add(
                        PromoCode(
                            code = code,
                            discountAmount = discountAmount,
                            description = desc,
                            isUsed = isUsed
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi parse promo_codes: ${e.message}")
        }
        return list
    }

    /**
     * Đẩy mã giảm giá lên Supabase table 'promo_codes'
     */
    fun pushPromoCodeToSupabase(promoCode: PromoCode, onComplete: (Boolean) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id") || promoCode.code.isBlank()) {
            onComplete(true)
            return
        }

        val url = "$SUPABASE_URL/rest/v1/promo_codes"
        val json = JSONObject().apply {
            put("code", promoCode.code)
            put("discount_amount", promoCode.discountAmount)
            put("description", promoCode.description)
            put("is_used", promoCode.isUsed)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .post(json.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Lỗi kết nối đẩy PromoCode lên Supabase: ${e.message}")
                onComplete(false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    Log.d(TAG, "Đã lưu PromoCode lên Supabase: ${response.code}")
                    onComplete(response.isSuccessful)
                }
            }
        })
    }

    /**
     * Cập nhật trạng thái sử dụng của mã giảm giá trên Supabase table 'promo_codes'
     */
    fun updatePromoCodeUsageInSupabase(code: String, isUsed: Boolean, onComplete: (Boolean) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id") || code.isBlank()) {
            onComplete(true)
            return
        }

        val url = "$SUPABASE_URL/rest/v1/promo_codes?code=eq.$code"
        val payload = JSONObject().apply {
            put("is_used", isUsed)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .addHeader("Content-Type", "application/json")
            .patch(payload.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Lỗi kết nối cập nhật trạng thái PromoCode trên Supabase: ${e.message}")
                onComplete(false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    Log.d(TAG, "Đã cập nhật trạng thái PromoCode trên Supabase: ${response.code}")
                    onComplete(response.isSuccessful)
                }
            }
        })
    }

    /**
     * Đồng bộ danh sách lịch chiếu (showtimes) từ Supabase table 'showtimes'
     */
    fun fetchShowtimesFromSupabase(onResult: (List<Showtime>) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id")) {
            onResult(emptyList())
            return
        }

        val url = "$SUPABASE_URL/rest/v1/showtimes?select=*"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Lỗi fetch showtimes từ Supabase: ${e.message}")
                onResult(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Supabase showtimes phản hồi lỗi: ${response.code}")
                        onResult(emptyList())
                        return
                    }
                    val bodyString = response.body?.string() ?: ""
                    val list = mutableListOf<Showtime>()
                    try {
                        val jsonArray = JSONArray(bodyString)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val mIdStr = if (obj.has("movie_id")) obj.optString("movie_id") else obj.optString("movieId", "")
                            val mIdObj = if (obj.has("movie_id")) obj.get("movie_id") else obj.opt("movieId")
                            val mId = when (mIdObj) {
                                is Int -> mIdObj
                                is Number -> mIdObj.toInt()
                                is String -> mIdObj.toIntOrNull() ?: kotlin.math.abs(mIdObj.hashCode())
                                else -> 0
                            }
                            var sTime = when {
                                obj.has("start_time") -> obj.optString("start_time")
                                obj.has("startTime") -> obj.optString("startTime")
                                else -> ""
                            }
                            if (sTime.length >= 5 && sTime.contains(":")) {
                                sTime = sTime.substring(0, 5) // Lấy HH:mm bỏ phần :ss nếu có
                            }

                            var eTime = when {
                                obj.has("end_time") -> obj.optString("end_time")
                                obj.has("endTime") -> obj.optString("endTime")
                                else -> ""
                            }
                            if (eTime.length >= 5 && eTime.contains(":")) {
                                eTime = eTime.substring(0, 5)
                            }

                            var sDate = when {
                                obj.has("show_date") -> obj.optString("show_date")
                                obj.has("showDate") -> obj.optString("showDate")
                                obj.has("date") -> obj.optString("date")
                                else -> ""
                            }
                            // Nếu show_date là YYYY-MM-DD hoặc ISO timestamp chuyển thành dd/MM để khớp với UI
                            if (sDate.contains("-")) {
                                val dateOnly = sDate.substringBefore("T").substringBefore(" ").trim()
                                val parts = dateOnly.split("-")
                                if (parts.size >= 3) {
                                    val day = parts[2].padStart(2, '0')
                                    val month = parts[1].padStart(2, '0')
                                    sDate = "$day/$month"
                                }
                            }
                            val rId = when {
                                obj.has("room_id") -> obj.optString("room_id")
                                obj.has("roomId") -> obj.optString("roomId")
                                else -> ""
                            }
                            val regPrice = if (obj.has("regular_price") && !obj.isNull("regular_price")) obj.optDouble("regular_price") else null
                            val vPrice = if (obj.has("vip_price") && !obj.isNull("vip_price")) obj.optDouble("vip_price") else null
                            val vPercent = if (obj.has("vip_percent") && !obj.isNull("vip_percent")) obj.optDouble("vip_percent") else null
                            val pr = if (obj.has("price") && !obj.isNull("price")) obj.optInt("price") else (regPrice?.toInt() ?: 0)

                            list.add(
                                Showtime(
                                    id = obj.optInt("id", i + 1),
                                    movieId = mId,
                                    movieStringId = mIdStr,
                                    startTime = sTime,
                                    endTime = eTime,
                                    price = pr,
                                    regularPrice = regPrice,
                                    vipPrice = vPrice,
                                    vipPercent = vPercent,
                                    date = sDate,
                                    roomId = rId
                                )
                            )
                        }
                        onResult(list)
                        Log.d(TAG, "Fetch thành công ${list.size} showtimes từ Supabase!")
                    } catch (ex: Exception) {
                        Log.e(TAG, "Lỗi parse showtimes: ${ex.message}")
                        onResult(emptyList())
                    }
                }
            }
        })
    }

    /**
     * Đồng bộ danh sách đặt chỗ (bookings) từ Supabase table 'bookings'
     */
    fun fetchBookingsFromSupabase(onResult: (List<Booking>) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id")) {
            onResult(emptyList())
            return
        }

        val url = "$SUPABASE_URL/rest/v1/bookings?select=*"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Lỗi fetch bookings từ Supabase: ${e.message}")
                onResult(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Supabase bookings phản hồi lỗi: ${response.code}")
                        onResult(emptyList())
                        return
                    }
                    val bodyString = response.body?.string() ?: ""
                    val list = mutableListOf<Booking>()
                    try {
                        val jsonArray = JSONArray(bodyString)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val seatsVal = obj.optString("seats", "")
                            val idVal = obj.opt("id")
                            val intId = when (idVal) {
                                is Int -> idVal
                                is Number -> idVal.toInt()
                                is String -> idVal.toIntOrNull() ?: kotlin.math.abs(idVal.hashCode())
                                else -> i + 1
                            }
                            val mIdVal = obj.opt("movie_id")
                            val intMId = when (mIdVal) {
                                is Int -> mIdVal
                                is Number -> mIdVal.toInt()
                                is String -> mIdVal.toIntOrNull() ?: kotlin.math.abs(mIdVal.hashCode())
                                else -> 0
                            }
                            list.add(
                                Booking(
                                    id = intId,
                                    movieId = intMId,
                                    showtimeId = obj.optInt("showtime_id", 0),
                                    seats = seatsVal,
                                    totalPrice = obj.optInt("total_price", 0),
                                    userEmail = obj.optString("user_email", "")
                                )
                            )
                        }
                        onResult(list)
                        Log.d(TAG, "Fetch thành công ${list.size} bookings từ Supabase!")
                    } catch (ex: Exception) {
                        Log.e(TAG, "Lỗi parse bookings: ${ex.message}")
                        onResult(emptyList())
                    }
                }
            }
        })
    }
}
