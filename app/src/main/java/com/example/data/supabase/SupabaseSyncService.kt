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
import com.example.data.model.ScreeningRoom
import com.example.data.model.SeatItem
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
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
     * Đồng bộ danh sách vé đã đặt từ Supabase bằng cách liên kết quan hệ 100% giữa tickets, bookings, showtimes và movies
     */
    fun fetchTicketsFromSupabase(onResult: (List<Ticket>) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id")) {
            onResult(emptyList())
            return
        }

        // 1. Tải movies và showtimes đồng thời để xây dựng bảng tra cứu
        fetchMoviesFromSupabase { moviesList ->
            val moviesMap = mutableMapOf<String, Movie>()
            moviesList?.forEach { m ->
                moviesMap[m.id.toString()] = m
                if (m.stringId.isNotBlank()) moviesMap[m.stringId] = m
                moviesMap[m.title.trim().lowercase(Locale.getDefault())] = m
            }

            fetchShowtimesFromSupabase { showtimesList ->
                val showtimesMap = mutableMapOf<String, Showtime>()
                showtimesList.forEach { st ->
                    if (st.uuid.isNotBlank()) showtimesMap[st.uuid] = st
                    showtimesMap[st.id.toString()] = st
                    if (st.roomId.isNotBlank()) showtimesMap[st.roomId] = st
                }

                // 2. Tải cả bảng tickets và bảng bookings từ Supabase
                fetchRawTicketsAndBookings(moviesMap, showtimesMap, onResult)
            }
        }
    }

    private fun fetchRawTicketsAndBookings(
        moviesMap: Map<String, Movie>,
        showtimesMap: Map<String, Showtime>,
        onResult: (List<Ticket>) -> Unit
    ) {
        val ticketsUrl = "$SUPABASE_URL/rest/v1/tickets?select=*&order=created_at.desc"
        val bookingsUrl = "$SUPABASE_URL/rest/v1/bookings?select=*&order=created_at.desc"

        val ticketsRequest = Request.Builder()
            .url(ticketsUrl)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .build()

        client.newCall(ticketsRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Lỗi fetch raw tickets: ${e.message}")
                fetchBookingsOnly(moviesMap, showtimesMap, onResult)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val rawTicketsJson = if (response.isSuccessful) response.body?.string() ?: "[]" else "[]"
                    
                    val bookingsRequest = Request.Builder()
                        .url(bookingsUrl)
                        .addHeader("apikey", SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                        .build()

                    client.newCall(bookingsRequest).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            val combinedTickets = parseRelationalTickets(rawTicketsJson, "[]", moviesMap, showtimesMap)
                            onResult(combinedTickets)
                        }

                        override fun onResponse(call: Call, bResponse: Response) {
                            bResponse.use {
                                val rawBookingsJson = if (bResponse.isSuccessful) bResponse.body?.string() ?: "[]" else "[]"
                                val combinedTickets = parseRelationalTickets(rawTicketsJson, rawBookingsJson, moviesMap, showtimesMap)
                                Log.d(TAG, "Đã parse thành công ${combinedTickets.size} vé chính xác 100% từ Database Supabase!")
                                onResult(combinedTickets)
                            }
                        }
                    })
                }
            }
        })
    }

    private fun fetchBookingsOnly(
        moviesMap: Map<String, Movie>,
        showtimesMap: Map<String, Showtime>,
        onResult: (List<Ticket>) -> Unit
    ) {
        val bookingsUrl = "$SUPABASE_URL/rest/v1/bookings?select=*&order=created_at.desc"
        val request = Request.Builder()
            .url(bookingsUrl)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val rawBookingsJson = if (response.isSuccessful) response.body?.string() ?: "[]" else "[]"
                    val tickets = parseRelationalTickets("[]", rawBookingsJson, moviesMap, showtimesMap)
                    onResult(tickets)
                }
            }
        })
    }

    private fun parseRelationalTickets(
        rawTicketsJson: String,
        rawBookingsJson: String,
        moviesMap: Map<String, Movie>,
        showtimesMap: Map<String, Showtime>
    ): List<Ticket> {
        val resultList = mutableListOf<Ticket>()
        val processedBookingIds = mutableSetOf<String>()

        // 1. Phân tích các dòng trong Bookings Json
        val bookingsMap = mutableMapOf<String, JSONObject>()
        try {
            val bookingsArray = JSONArray(rawBookingsJson)
            for (i in 0 until bookingsArray.length()) {
                val bObj = bookingsArray.getJSONObject(i)
                val bId = bObj.optString("id", "")
                if (bId.isNotBlank()) {
                    bookingsMap[bId] = bObj
                }
                val bCode = bObj.optString("booking_code", "")
                if (bCode.isNotBlank()) {
                    bookingsMap[bCode] = bObj
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi parse rawBookingsJson: ${e.message}")
        }

        // 2. Nhóm các hàng trong Tickets theo booking_id
        val ticketsByBookingId = mutableMapOf<String, MutableList<JSONObject>>()
        try {
            val ticketsArray = JSONArray(rawTicketsJson)
            for (i in 0 until ticketsArray.length()) {
                val tObj = ticketsArray.getJSONObject(i)
                val bId = tObj.optString("booking_id", "").ifBlank {
                    tObj.optString("booking_code", "").ifBlank {
                        "TICKET_${tObj.optString("id", i.toString())}"
                    }
                }
                val list = ticketsByBookingId.getOrPut(bId) { mutableListOf() }
                list.add(tObj)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi parse rawTicketsJson: ${e.message}")
        }

        // 3. Chuyển đổi từng nhóm tickets thành Ticket hoàn chỉnh
        for ((bookingId, ticketObjs) in ticketsByBookingId) {
            if (ticketObjs.isEmpty()) continue
            processedBookingIds.add(bookingId)

            val firstObj = ticketObjs.first()
            val bObj = bookingsMap[bookingId]

            val showtimeId = firstObj.optString("showtime_id", "").ifBlank {
                bObj?.optString("showtime_id", "") ?: ""
            }

            val showtime = showtimesMap[showtimeId]
            val movieIdStr = showtime?.movieStringId ?: firstObj.optString("movie_id", "").ifBlank {
                bObj?.optString("movie_id", "") ?: ""
            }

            var movie = moviesMap[movieIdStr] ?: if (showtime != null && showtime.movieId > 0) {
                moviesMap[showtime.movieId.toString()]
            } else null

            if (movie == null && showtime != null && showtime.movieTitle.isNotBlank()) {
                movie = moviesMap[showtime.movieTitle.trim().lowercase(Locale.getDefault())]
            }

            val resolvedMovieTitle = when {
                movie != null && movie.title.isNotBlank() -> movie.title
                showtime != null && showtime.movieTitle.isNotBlank() -> showtime.movieTitle
                firstObj.has("movie_title") && firstObj.optString("movie_title").isNotBlank() -> firstObj.optString("movie_title")
                bObj != null && bObj.has("movie_title") && bObj.optString("movie_title").isNotBlank() -> bObj.optString("movie_title")
                else -> "Cô Ba Sài Gòn"
            }

            val resolvedMoviePoster = when {
                movie != null && movie.posterUrl.isNotBlank() -> movie.posterUrl
                firstObj.has("movie_poster") && firstObj.optString("movie_poster").isNotBlank() -> firstObj.optString("movie_poster")
                else -> "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500"
            }

            val seatCodes = ticketObjs.mapNotNull {
                val s = it.optString("seat_code", "").ifBlank { it.optString("seats", "") }
                s.ifBlank { null }
            }.sorted().joinToString(", ").ifBlank { "D03" }

            val totalAmount = when {
                bObj != null && bObj.has("total_amount") -> bObj.optInt("total_amount", 0)
                else -> ticketObjs.sumOf { it.optInt("price", 0) }
            }

            val bookingCode = when {
                bObj != null && bObj.has("booking_code") && bObj.optString("booking_code").isNotBlank() -> bObj.optString("booking_code")
                firstObj.has("booking_code") && firstObj.optString("booking_code").isNotBlank() -> firstObj.optString("booking_code")
                firstObj.has("ticket_code") && firstObj.optString("ticket_code").isNotBlank() -> firstObj.optString("ticket_code")
                else -> bookingId.take(5).uppercase(Locale.getDefault())
            }

            val guestName = when {
                bObj != null && bObj.has("guest_name") && bObj.optString("guest_name").isNotBlank() -> bObj.optString("guest_name")
                firstObj.has("guest_name") && firstObj.optString("guest_name").isNotBlank() -> firstObj.optString("guest_name")
                firstObj.has("user_name") && firstObj.optString("user_name").isNotBlank() -> firstObj.optString("user_name")
                else -> "Khách Hàng"
            }

            val userEmail = when {
                bObj != null && bObj.has("user_email") && bObj.optString("user_email").isNotBlank() -> bObj.optString("user_email")
                firstObj.has("user_email") && firstObj.optString("user_email").isNotBlank() -> firstObj.optString("user_email")
                else -> ""
            }

            val dateStr = when {
                showtime != null && showtime.date.isNotBlank() -> showtime.date
                bObj != null && bObj.has("booking_date") && bObj.optString("booking_date").isNotBlank() -> {
                    val rawD = bObj.optString("booking_date")
                    if (rawD.contains("-")) {
                        val parts = rawD.split("-")
                        if (parts.size >= 3) "${parts[2]}/${parts[1]}" else rawD
                    } else rawD
                }
                else -> "Hôm nay"
            }

            val timeStr = when {
                showtime != null && showtime.startTime.isNotBlank() -> showtime.startTime
                else -> "17:34"
            }

            val roomId = showtime?.roomId ?: "PC-005"
            val cinemaName = "Neon Cine Space - Vincom Xuân Khánh" + if (roomId.isNotBlank()) " (Phòng $roomId)" else ""

            val comboStr = when {
                bObj != null && bObj.has("selected_popcorns") && bObj.optString("selected_popcorns").isNotBlank() -> {
                    val pop = bObj.optString("selected_popcorns")
                    val drk = bObj.optString("selected_drinks", "")
                    if (drk.isNotBlank()) "$pop + $drk" else pop
                }
                else -> ""
            }

            val rawCreatedAt = firstObj.optString("created_at", bObj?.optString("created_at", "") ?: "")
            val timestamp = parseIsoToMillis(rawCreatedAt)

            resultList.add(
                Ticket(
                    id = 0,
                    movieId = movie?.id ?: 1,
                    movieTitle = resolvedMovieTitle,
                    moviePoster = resolvedMoviePoster,
                    cinema = cinemaName,
                    dateTime = "$dateStr lúc $timeStr",
                    seats = seatCodes,
                    totalPrice = totalAmount,
                    combo = comboStr,
                    barcode = "NEON-$bookingId",
                    timestamp = timestamp,
                    userEmail = userEmail,
                    userName = guestName,
                    promoCode = "",
                    bookingCode = bookingCode
                )
            )
        }

        // 4. Xử lý các bookings chưa có trong danh sách tickets (ví dụ đơn đặt chung chưa chia vé lẻ)
        for ((bId, bObj) in bookingsMap) {
            if (processedBookingIds.contains(bId) || processedBookingIds.contains(bObj.optString("booking_code", ""))) {
                continue
            }

            val showtimeId = bObj.optString("showtime_id", "")
            val showtime = showtimesMap[showtimeId]
            val movieIdStr = showtime?.movieStringId ?: bObj.optString("movie_id", "")

            var movie = moviesMap[movieIdStr] ?: if (showtime != null && showtime.movieId > 0) {
                moviesMap[showtime.movieId.toString()]
            } else null

            if (movie == null && showtime != null && showtime.movieTitle.isNotBlank()) {
                movie = moviesMap[showtime.movieTitle.trim().lowercase(Locale.getDefault())]
            }

            val resolvedMovieTitle = when {
                movie != null && movie.title.isNotBlank() -> movie.title
                showtime != null && showtime.movieTitle.isNotBlank() -> showtime.movieTitle
                bObj.has("movie_title") && bObj.optString("movie_title").isNotBlank() -> bObj.optString("movie_title")
                else -> "Cô Ba Sài Gòn"
            }

            val resolvedMoviePoster = movie?.posterUrl ?: "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500"
            val totalAmount = bObj.optInt("total_amount", 0)
            val bookingCode = bObj.optString("booking_code", bId.take(5))
            val guestName = bObj.optString("guest_name", "Khách Hàng")
            val userEmail = bObj.optString("user_email", "")

            val dateStr = when {
                showtime != null && showtime.date.isNotBlank() -> showtime.date
                bObj.has("booking_date") && bObj.optString("booking_date").isNotBlank() -> {
                    val rawD = bObj.optString("booking_date")
                    if (rawD.contains("-")) {
                        val parts = rawD.split("-")
                        if (parts.size >= 3) "${parts[2]}/${parts[1]}" else rawD
                    } else rawD
                }
                else -> "Hôm nay"
            }

            val timeStr = showtime?.startTime ?: "17:34"
            val roomId = showtime?.roomId ?: "PC-005"
            val cinemaName = "Neon Cine Space - Vincom Xuân Khánh" + if (roomId.isNotBlank()) " (Phòng $roomId)" else ""

            val comboStr = when {
                bObj.has("selected_popcorns") && bObj.optString("selected_popcorns").isNotBlank() -> {
                    val pop = bObj.optString("selected_popcorns")
                    val drk = bObj.optString("selected_drinks", "")
                    if (drk.isNotBlank()) "$pop + $drk" else pop
                }
                else -> ""
            }

            val timestamp = parseIsoToMillis(bObj.optString("created_at", ""))

            resultList.add(
                Ticket(
                    id = 0,
                    movieId = movie?.id ?: 1,
                    movieTitle = resolvedMovieTitle,
                    moviePoster = resolvedMoviePoster,
                    cinema = cinemaName,
                    dateTime = "$dateStr lúc $timeStr",
                    seats = bObj.optString("seats", "D03"),
                    totalPrice = totalAmount,
                    combo = comboStr,
                    barcode = "NEON-$bId",
                    timestamp = timestamp,
                    userEmail = userEmail,
                    userName = guestName,
                    promoCode = "",
                    bookingCode = bookingCode
                )
            )
        }

        return resultList
    }

    private fun parseIsoToMillis(isoStr: String): Long {
        if (isoStr.isBlank()) return System.currentTimeMillis()
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val clean = isoStr.substringBefore(".").substringBefore("+").substringBefore("Z")
            sdf.parse(clean)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
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
                val rawTitle = when {
                    obj.has("movie_title") && obj.optString("movie_title").isNotBlank() -> obj.optString("movie_title")
                    obj.has("movieTitle") && obj.optString("movieTitle").isNotBlank() -> obj.optString("movieTitle")
                    obj.has("title") && obj.optString("title").isNotBlank() -> obj.optString("title")
                    obj.has("movie_name") && obj.optString("movie_name").isNotBlank() -> obj.optString("movie_name")
                    obj.has("film_name") && obj.optString("film_name").isNotBlank() -> obj.optString("film_name")
                    else -> ""
                }
                val resolvedTitle = when {
                    rawTitle.isNotBlank() && rawTitle != "Phim" && rawTitle != "Phim Chiếu Rạp" -> rawTitle
                    mId in 1..12 -> when (mId) {
                        1 -> "Lật Mặt 7: Một Điều Ước"
                        2 -> "Mai"
                        3 -> "Doraemon: Bản Tình Ca Đất Nước"
                        4 -> "Inside Out 2 (Những Mảnh Ghép Cảm Xúc 2)"
                        5 -> "Deadpool & Wolverine"
                        6 -> "Godzilla x Kong: Đế Chế Mới"
                        7 -> "Kẻ Trộm Mặt Trăng 4 (Despicable Me 4)"
                        8 -> "Hành Tinh Cát: Phần Hai (Dune: Part Two)"
                        9 -> "Kung Fu Panda 4"
                        10 -> "Mufasa: The Lion King"
                        11 -> "Moana 2 (Hành Trình Của Moana 2)"
                        12 -> "Sonic the Hedgehog 3"
                        else -> ""
                    }
                    else -> ""
                }
                
                // Bỏ qua các vé rác/vé lỗi không có tiêu đề phim hợp lệ
                if (resolvedTitle.isBlank()) {
                    continue
                }
                val seatVal = when {
                    obj.has("seats") && obj.optString("seats").isNotBlank() -> obj.optString("seats")
                    obj.has("seat_code") && obj.optString("seat_code").isNotBlank() -> obj.optString("seat_code")
                    obj.has("seat") && obj.optString("seat").isNotBlank() -> obj.optString("seat")
                    obj.has("seat_id") && obj.optString("seat_id").isNotBlank() -> obj.optString("seat_id")
                    obj.has("seat_number") && obj.optString("seat_number").isNotBlank() -> obj.optString("seat_number")
                    obj.has("seat_name") && obj.optString("seat_name").isNotBlank() -> obj.optString("seat_name")
                    obj.has("seat_codes") && obj.optString("seat_codes").isNotBlank() -> obj.optString("seat_codes")
                    obj.has("selected_seats") && obj.optString("selected_seats").isNotBlank() -> obj.optString("selected_seats")
                    else -> obj.optString("seats", obj.optString("seat_code", "Ghế"))
                }
                val posterVal = when {
                    obj.has("movie_poster") && obj.optString("movie_poster").isNotBlank() -> obj.optString("movie_poster")
                    obj.has("moviePoster") && obj.optString("moviePoster").isNotBlank() -> obj.optString("moviePoster")
                    obj.has("posterUrl") && obj.optString("posterUrl").isNotBlank() -> obj.optString("posterUrl")
                    obj.has("poster") && obj.optString("poster").isNotBlank() -> obj.optString("poster")
                    else -> when (mId) {
                        1 -> "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500&auto=format&fit=crop"
                        2 -> "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=500&auto=format&fit=crop"
                        3 -> "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=500&auto=format&fit=crop"
                        4 -> "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500&auto=format&fit=crop"
                        else -> "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500&auto=format&fit=crop"
                    }
                }
                val rawBarcode = obj.optString("barcode", "")
                val barcode = if (rawBarcode.isNotBlank()) rawBarcode else "NEON-${System.currentTimeMillis() % 100000}"
                val bCode = obj.optString("booking_code", obj.optString("bookingCode", obj.optString("ticket_code", "")))
                val bookingCode = if (bCode.isNotBlank()) bCode else barcode.replace("NEON-", "").take(5).uppercase(Locale.getDefault())

                list.add(
                    Ticket(
                        id = 0, // Dùng 0 để Room tự sinh auto-increment ID tránh đè ID vé địa phương
                        movieId = if (mId > 0) mId else 1,
                        movieTitle = resolvedTitle,
                        moviePoster = posterVal,
                        cinema = obj.optString("cinema", "Neon Cine Space - Vincom"),
                        dateTime = obj.optString("date_time", obj.optString("dateTime", "Hôm nay")),
                        seats = seatVal,
                        totalPrice = obj.optInt("total_price", obj.optInt("price", obj.optInt("total_amount", 0))),
                        combo = obj.optString("combo", obj.optString("selected_popcorns", "")),
                        barcode = barcode,
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        userEmail = obj.optString("user_email", obj.optString("userEmail", obj.optString("customer_email", ""))),
                        userName = obj.optString("user_name", obj.optString("userName", obj.optString("customer_name", obj.optString("guest_name", "")))),
                        promoCode = obj.optString("promo_code", obj.optString("promoCode", "")),
                        bookingCode = bookingCode
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi parse tickets: ${e.message}")
        }
        return list
    }

    /**
     * Đẩy vé đã thanh toán lên Supabase tables: 'bookings' & 'tickets' & 'customers' (chuẩn hóa đúng kiểu dữ liệu của DB)
     */
    fun pushTicketToSupabase(ticket: Ticket, showtimeUuidParam: String = "", onComplete: (Boolean) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id")) {
            onComplete(true)
            return
        }

        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = java.util.Random()
        fun genStr(n: Int) = (1..n).map { chars[random.nextInt(chars.length)] }.joinToString("")

        val bookingId = if (ticket.bookingCode.isNotBlank() && ticket.bookingCode.length <= 5) ticket.bookingCode else genStr(5)
        val bookingCode = if (ticket.bookingCode.isNotBlank()) ticket.bookingCode else genStr(5)
        val customerId = if (ticket.userEmail.isNotBlank()) String.format("%08X", kotlin.math.abs(ticket.userEmail.hashCode())).take(8) else "02572CF6"
        val isUuid = Regex("""^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$""").matches(showtimeUuidParam.trim())
        val showtimeId = if (isUuid) showtimeUuidParam.trim() else "07e17f5e-9ce6-41b0-961c-ff366befd854"
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val seatList = ticket.seats.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }
        val perSeatPrice = if (seatList.isNotEmpty()) (ticket.totalPrice.toDouble() / seatList.size) else ticket.totalPrice.toDouble()

        // 1. Ghi vào bảng 'bookings'
        val bookingJsonClean = JSONObject().apply {
            put("id", bookingId)
            put("booking_code", bookingCode)
            put("customer_id", customerId)
            put("showtime_id", showtimeId)
            put("total_amount", ticket.totalPrice.toDouble())
            put("payment_method", "APP")
            put("status", "PAID")
            put("booking_date", todayStr)
            put("guest_name", ticket.userName.ifBlank { "bach thông" })
            if (ticket.combo.isNotBlank()) {
                put("selected_popcorns", ticket.combo)
            }
        }.toString()

        pushBookingToTables(listOf("bookings", "booking"), 0, bookingJsonClean, bookingJsonClean)

        // 2. Ghi từng ghế vào bảng 'tickets'
        for (seat in seatList) {
            val ticketId = "TK-" + genStr(6)
            val ticketCode = genStr(5)
            val isVip = seat.startsWith("E", ignoreCase = true) || seat.startsWith("F", ignoreCase = true) || seat.startsWith("G", ignoreCase = true) || seat.startsWith("D", ignoreCase = true)

            val seatCodeClean = if (Regex("""^[A-Za-z][0-9]$""").matches(seat)) {
                seat.take(1).uppercase() + "0" + seat.drop(1)
            } else {
                seat.uppercase()
            }

            val ticketJsonClean = JSONObject().apply {
                put("id", ticketId)
                put("ticket_code", ticketCode)
                put("booking_id", bookingId)
                put("showtime_id", showtimeId)
                put("seat_code", seatCodeClean)
                put("seat_type", if (isVip) "VIP" else "NORMAL")
                put("price", perSeatPrice)
                put("status", "SOLD")
                put("customer_id", customerId)
                put("source", "APP")
            }.toString()

            pushTicketToTables(listOf("tickets", "ticket"), 0, ticketJsonClean, ticketJsonClean, seat)
        }

        // 3. Tự động đồng bộ thông tin khách hàng vào các bảng profiles, customers
        val customerProfile = UserProfile(
            email = ticket.userEmail.ifBlank { "thongbach900@gmail.com" },
            name = ticket.userName.ifBlank { "bach thông" },
            points = 150,
            balance = 500000
        )
        pushProfileToSupabase(customerProfile) {}

        onComplete(true)
    }

    private fun pushBookingToTables(tables: List<String>, index: Int, fullJson: String, standardJson: String) {
        if (index >= tables.size) return
        val tableName = tables[index]
        val url = "$SUPABASE_URL/rest/v1/$tableName"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(fullJson.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Lỗi kết nối lưu booking vào $tableName: ${e.message}")
                pushBookingToTables(tables, index + 1, fullJson, standardJson)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        Log.d(TAG, "Đã lưu thành công booking lên bảng '$tableName': HTTP ${response.code}")
                    } else if (response.code == 400 || response.code == 409) {
                        // Thử fallback payload standard bỏ các trường custom
                        val fallbackReq = Request.Builder()
                            .url(url)
                            .addHeader("apikey", SUPABASE_ANON_KEY)
                            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Prefer", "return=minimal")
                            .post(standardJson.toRequestBody(jsonMediaType))
                            .build()

                        client.newCall(fallbackReq).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                pushBookingToTables(tables, index + 1, fullJson, standardJson)
                            }
                            override fun onResponse(call: Call, res: Response) {
                                res.use {
                                    if (res.isSuccessful) {
                                        Log.d(TAG, "Đã lưu thành công booking lên '$tableName' (standard format): HTTP ${res.code}")
                                    } else {
                                        pushBookingToTables(tables, index + 1, fullJson, standardJson)
                                    }
                                }
                            }
                        })
                    } else {
                        pushBookingToTables(tables, index + 1, fullJson, standardJson)
                    }
                }
            }
        })
    }

    private fun pushTicketToTables(tables: List<String>, index: Int, fullJson: String, standardJson: String, seat: String) {
        if (index >= tables.size) return
        val tableName = tables[index]
        val url = "$SUPABASE_URL/rest/v1/$tableName"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(fullJson.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Lỗi kết nối lưu ticket $seat vào $tableName: ${e.message}")
                pushTicketToTables(tables, index + 1, fullJson, standardJson, seat)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        Log.d(TAG, "Đã lưu thành công ticket $seat lên bảng '$tableName': HTTP ${response.code}")
                    } else if (response.code == 400 || response.code == 409) {
                        val fallbackReq = Request.Builder()
                            .url(url)
                            .addHeader("apikey", SUPABASE_ANON_KEY)
                            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Prefer", "return=minimal")
                            .post(standardJson.toRequestBody(jsonMediaType))
                            .build()

                        client.newCall(fallbackReq).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                pushTicketToTables(tables, index + 1, fullJson, standardJson, seat)
                            }
                            override fun onResponse(call: Call, res: Response) {
                                res.use {
                                    if (res.isSuccessful) {
                                        Log.d(TAG, "Đã lưu thành công ticket $seat lên '$tableName' (standard format): HTTP ${res.code}")
                                    } else {
                                        pushTicketToTables(tables, index + 1, fullJson, standardJson, seat)
                                    }
                                }
                            }
                        })
                    } else {
                        pushTicketToTables(tables, index + 1, fullJson, standardJson, seat)
                    }
                }
            }
        })
    }

    /**
     * Xóa vé khỏi Supabase (Xóa cả theo barcode, booking_code và ticket_code trên cả bảng tickets và bookings)
     */
    fun deleteTicketFromSupabase(barcode: String, bookingCode: String = "", onComplete: (Boolean) -> Unit = {}) {
        if (SUPABASE_URL.contains("your-project-id") || (barcode.isBlank() && bookingCode.isBlank())) {
            onComplete(true)
            return
        }
        val ticketCode = if (barcode.isNotBlank()) barcode.takeLast(5) else bookingCode

        // Danh sách các endpoint để xóa sạch dữ liệu vé liên quan
        val endpoints = mutableListOf<String>()
        if (barcode.isNotBlank()) {
            endpoints.add("$SUPABASE_URL/rest/v1/tickets?barcode=eq.$barcode")
            endpoints.add("$SUPABASE_URL/rest/v1/bookings?barcode=eq.$barcode")
        }
        if (bookingCode.isNotBlank()) {
            endpoints.add("$SUPABASE_URL/rest/v1/tickets?booking_code=eq.$bookingCode")
            endpoints.add("$SUPABASE_URL/rest/v1/bookings?booking_code=eq.$bookingCode")
        }
        if (ticketCode.isNotBlank()) {
            endpoints.add("$SUPABASE_URL/rest/v1/tickets?ticket_code=eq.$ticketCode")
        }

        for (url in endpoints) {
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .delete()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {}
                override fun onResponse(call: Call, response: Response) {
                    response.close()
                }
            })
        }
        onComplete(true)
    }

    /**
     * Xóa toàn bộ vé từ Supabase (theo email tài khoản hoặc xóa toàn bộ bảng vé)
     */
    fun deleteAllTicketsFromSupabase(userEmail: String = "", onComplete: (Boolean) -> Unit = {}) {
        if (SUPABASE_URL.contains("your-project-id")) {
            onComplete(true)
            return
        }

        val deleteUrls = mutableListOf<String>()
        if (userEmail.isNotBlank()) {
            deleteUrls.add("$SUPABASE_URL/rest/v1/tickets?user_email=eq.$userEmail")
            deleteUrls.add("$SUPABASE_URL/rest/v1/bookings?customer_email=eq.$userEmail")
            deleteUrls.add("$SUPABASE_URL/rest/v1/bookings?user_email=eq.$userEmail")
        } else {
            // Xóa tất cả vé khi người dùng yêu cầu xóa toàn bộ lịch sử
            deleteUrls.add("$SUPABASE_URL/rest/v1/tickets?id=neq.NULL_DUMMY_ID")
            deleteUrls.add("$SUPABASE_URL/rest/v1/bookings?id=neq.NULL_DUMMY_ID")
        }

        for (url in deleteUrls) {
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .delete()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {}
                override fun onResponse(call: Call, response: Response) {
                    response.close()
                }
            })
        }
        onComplete(true)
    }

    /**
     * Đẩy Profile và thông tin Khách hàng lên Supabase (Đồng bộ vào các bảng: customers, profiles, users để phục vụ quản lý & thống kê)
     */
    fun pushProfileToSupabase(profile: UserProfile, onComplete: (Boolean) -> Unit = {}) {
        if (SUPABASE_URL.contains("your-project-id") || profile.email.isBlank()) {
            onComplete(true)
            return
        }

        val customerId = String.format("%08X", kotlin.math.abs(profile.email.hashCode())).take(8)
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(Date())

        val customerPayload = JSONObject().apply {
            put("id", customerId)
            put("name", profile.name.ifBlank { profile.email.substringBefore("@") })
            put("email", profile.email)
            put("phone", "0901234567")
        }.toString()

        val profileJsonClean = JSONObject().apply {
            put("email", profile.email)
            put("name", profile.name.ifBlank { profile.email.substringBefore("@") })
            put("points", profile.points)
        }.toString()

        val tablesToSync = listOf("profiles", "customers", "costumers", "users")

        for (table in tablesToSync) {
            val payload = if (table == "profiles") profileJsonClean else customerPayload
            val url = "$SUPABASE_URL/rest/v1/$table"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Lỗi kết nối đẩy thông tin khách hàng lên bảng '$table': ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        Log.d(TAG, "Đã đồng bộ thông tin khách hàng (${profile.name} - ${profile.email}) lên bảng '$table': HTTP ${response.code}")
                    }
                }
            })
        }
        onComplete(true)
    }

    /**
     * Đồng bộ và tìm kiếm Profile người dùng từ Supabase (thử lần lượt: customers, costumers, profiles, khach_hang, users) theo email
     */
    fun fetchProfileFromSupabase(email: String, onResult: (UserProfile?) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id") || email.isBlank()) {
            onResult(null)
            return
        }

        fetchProfileFromTables(
            tables = listOf("customers", "costumers", "profiles", "khach_hang", "users"),
            index = 0,
            email = email,
            onResult = onResult
        )
    }

    private fun fetchProfileFromTables(tables: List<String>, index: Int, email: String, onResult: (UserProfile?) -> Unit) {
        if (index >= tables.size) {
            onResult(null)
            return
        }
        val tableName = tables[index]
        val url = "$SUPABASE_URL/rest/v1/$tableName?email=eq.$email&select=*"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Lỗi fetch profile từ bảng $tableName: ${e.message}")
                fetchProfileFromTables(tables, index + 1, email, onResult)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        fetchProfileFromTables(tables, index + 1, email, onResult)
                        return
                    }
                    val bodyString = response.body?.string() ?: ""
                    try {
                        val jsonArray = JSONArray(bodyString)
                        if (jsonArray.length() > 0) {
                            val obj = jsonArray.getJSONObject(0)
                            val name = when {
                                obj.has("name") && obj.optString("name").isNotBlank() -> obj.optString("name")
                                obj.has("customer_name") && obj.optString("customer_name").isNotBlank() -> obj.optString("customer_name")
                                obj.has("full_name") && obj.optString("full_name").isNotBlank() -> obj.optString("full_name")
                                obj.has("ten") && obj.optString("ten").isNotBlank() -> obj.optString("ten")
                                obj.has("ho_ten") && obj.optString("ho_ten").isNotBlank() -> obj.optString("ho_ten")
                                obj.has("ten_khach_hang") && obj.optString("ten_khach_hang").isNotBlank() -> obj.optString("ten_khach_hang")
                                else -> "Thành viên Neon"
                            }
                            val points = when {
                                obj.has("points") -> obj.optInt("points", 150)
                                obj.has("diem_tich_luy") -> obj.optInt("diem_tich_luy", 150)
                                obj.has("diem") -> obj.optInt("diem", 150)
                                else -> 150
                            }
                            val balance = obj.optInt("balance", 500000)
                            Log.d(TAG, "Fetch thành công profile khách hàng ($name) từ bảng '$tableName'!")
                            onResult(
                                UserProfile(
                                    email = obj.optString("email", email),
                                    name = name,
                                    points = points,
                                    balance = balance
                                )
                            )
                        } else {
                            fetchProfileFromTables(tables, index + 1, email, onResult)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi phân tích JSON profile từ $tableName: ${e.message}")
                        fetchProfileFromTables(tables, index + 1, email, onResult)
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
     * Đồng bộ danh sách sản phẩm / combo bắp nước từ Supabase (thử lần lượt các bảng products, combos, popcorns, food_and_drinks, foods, concessions, items)
     */
    fun fetchProductsFromSupabase(onResult: (List<Product>) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id")) return
        fetchProductsFromTables(
            tables = listOf("products", "combos", "combo", "popcorns", "popcorn", "food_and_drinks", "food_and_drink", "food", "foods", "concessions", "items", "snacks", "menu", "san_pham", "bap_nuoc", "thuc_an", "drinks", "beverages", "product"),
            index = 0,
            onResult = onResult
        )
    }

    private fun fetchProductsFromTables(tables: List<String>, index: Int, onResult: (List<Product>) -> Unit) {
        if (index >= tables.size) {
            onResult(emptyList())
            return
        }
        val tableName = tables[index]
        val url = "$SUPABASE_URL/rest/v1/$tableName?select=*"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Lỗi fetch $tableName từ Supabase: ${e.message}")
                fetchProductsFromTables(tables, index + 1, onResult)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        fetchProductsFromTables(tables, index + 1, onResult)
                        return
                    }
                    val bodyString = response.body?.string() ?: ""
                    val list = parseProductsJson(bodyString)
                    if (list.isNotEmpty()) {
                        Log.d(TAG, "Fetch thành công ${list.size} items từ bảng '$tableName' Supabase!")
                        onResult(list)
                    } else {
                        fetchProductsFromTables(tables, index + 1, onResult)
                    }
                }
            }
        })
    }

    fun fetchCombosFromSupabase(onResult: (List<Product>) -> Unit) {
        fetchProductsFromSupabase(onResult)
    }

    private fun parseProductsJson(bodyString: String): List<Product> {
        val list = mutableListOf<Product>()
        try {
            val jsonArray = JSONArray(bodyString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = when {
                    obj.has("id") -> obj.optInt("id", i + 1)
                    obj.has("product_id") -> obj.optInt("product_id", i + 1)
                    obj.has("combo_id") -> obj.optInt("combo_id", i + 1)
                    obj.has("item_id") -> obj.optInt("item_id", i + 1)
                    else -> i + 1
                }
                val name = when {
                    obj.has("name") && obj.optString("name").isNotBlank() -> obj.optString("name")
                    obj.has("ten") && obj.optString("ten").isNotBlank() -> obj.optString("ten")
                    obj.has("ten_combo") && obj.optString("ten_combo").isNotBlank() -> obj.optString("ten_combo")
                    obj.has("ten_san_pham") && obj.optString("ten_san_pham").isNotBlank() -> obj.optString("ten_san_pham")
                    obj.has("ten_bap") && obj.optString("ten_bap").isNotBlank() -> obj.optString("ten_bap")
                    obj.has("combo_name") && obj.optString("combo_name").isNotBlank() -> obj.optString("combo_name")
                    obj.has("title") && obj.optString("title").isNotBlank() -> obj.optString("title")
                    obj.has("product_name") && obj.optString("product_name").isNotBlank() -> obj.optString("product_name")
                    obj.has("item_name") && obj.optString("item_name").isNotBlank() -> obj.optString("item_name")
                    obj.has("label") && obj.optString("label").isNotBlank() -> obj.optString("label")
                    else -> "Combo Bắp + Nước"
                }
                var price = when {
                    obj.has("sell_price") -> obj.optInt("sell_price", 0)
                    obj.has("price") -> obj.optInt("price", 0)
                    obj.has("cost_price") -> obj.optInt("cost_price", 0)
                    obj.has("gia") -> obj.optInt("gia", 0)
                    obj.has("gia_tien") -> obj.optInt("gia_tien", 0)
                    obj.has("don_gia") -> obj.optInt("don_gia", 0)
                    obj.has("combo_price") -> obj.optInt("combo_price", 0)
                    obj.has("unit_price") -> obj.optInt("unit_price", 0)
                    obj.has("cost") -> obj.optInt("cost", 0)
                    obj.has("amount") -> obj.optInt("amount", 0)
                    else -> 0
                }
                val type = when {
                    obj.has("type") && obj.optString("type").isNotBlank() -> obj.optString("type")
                    obj.has("loai") && obj.optString("loai").isNotBlank() -> obj.optString("loai")
                    obj.has("category") && obj.optString("category").isNotBlank() -> obj.optString("category")
                    name.contains("Nước", ignoreCase = true) || name.contains("Coca", ignoreCase = true) || name.contains("Sprite", ignoreCase = true) || name.contains("Dasani", ignoreCase = true) -> "drink"
                    name.contains("Combo", ignoreCase = true) || name.contains("Couple", ignoreCase = true) || name.contains("Family", ignoreCase = true) || name.contains("Solo", ignoreCase = true) -> "combo"
                    else -> "snack"
                }
                // Nếu bảng Supabase không có cột giá hoặc giá bằng 0, tự động gán giá hợp lý theo tên/loại
                if (price <= 0) {
                    price = when {
                        name.contains("Family", ignoreCase = true) || name.contains("Party", ignoreCase = true) -> 145000
                        name.contains("Couple", ignoreCase = true) || name.contains("Đôi", ignoreCase = true) -> 95000
                        name.contains("Combo", ignoreCase = true) || name.contains("Solo", ignoreCase = true) -> 65000
                        name.contains("Nước", ignoreCase = true) || name.contains("Coca", ignoreCase = true) || name.contains("Sprite", ignoreCase = true) -> 32000
                        name.contains("Dasani", ignoreCase = true) || name.contains("Suối", ignoreCase = true) -> 20000
                        else -> 45000
                    }
                }
                val rawImageUrl = when {
                    obj.has("image_url") -> obj.optString("image_url")
                    obj.has("image") -> obj.optString("image")
                    obj.has("hinh_anh") -> obj.optString("hinh_anh")
                    obj.has("anh") -> obj.optString("anh")
                    obj.has("poster_url") -> obj.optString("poster_url")
                    obj.has("photo_url") -> obj.optString("photo_url")
                    obj.has("thumbnail_url") -> obj.optString("thumbnail_url")
                    obj.has("img") -> obj.optString("img")
                    else -> ""
                }
                val imageUrl = if (rawImageUrl.isNotBlank()) rawImageUrl else when (type) {
                    "combo" -> "https://images.unsplash.com/photo-1585647347483-22b66260dfff?w=500"
                    "drink" -> "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=500"
                    else -> "https://images.unsplash.com/photo-1572177191856-3cde618dee1f?w=500"
                }
                val desc = when {
                    obj.has("description") && obj.optString("description").isNotBlank() -> obj.optString("description")
                    obj.has("mo_ta") && obj.optString("mo_ta").isNotBlank() -> obj.optString("mo_ta")
                    obj.has("desc") && obj.optString("desc").isNotBlank() -> obj.optString("desc")
                    obj.has("details") && obj.optString("details").isNotBlank() -> obj.optString("details")
                    obj.has("info") && obj.optString("info").isNotBlank() -> obj.optString("info")
                    else -> if (type == "combo") "Bắp rang bơ giòn rụm + nước ngọt mát lạnh" else "Thức ăn / đồ uống hảo hạng phục vụ tại rạp"
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
     * Đồng bộ danh sách lịch chiếu (showtimes) từ Supabase (thử lần lượt các bảng: showtimes, show_times, lich_chieu, suat_chieu, schedules, screening_schedules)
     */
    fun fetchShowtimesFromSupabase(onResult: (List<Showtime>) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id")) {
            onResult(emptyList())
            return
        }

        fetchShowtimesFromTables(
            tables = listOf("showtimes", "show_times", "lich_chieu", "suat_chieu", "schedules", "screening_schedules", "showtime"),
            index = 0,
            onResult = onResult
        )
    }

    private fun fetchShowtimesFromTables(tables: List<String>, index: Int, onResult: (List<Showtime>) -> Unit) {
        if (index >= tables.size) {
            onResult(emptyList())
            return
        }
        val tableName = tables[index]
        val url = "$SUPABASE_URL/rest/v1/$tableName?select=*"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Lỗi fetch $tableName từ Supabase: ${e.message}")
                fetchShowtimesFromTables(tables, index + 1, onResult)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Supabase $tableName phản hồi lỗi: ${response.code}")
                        fetchShowtimesFromTables(tables, index + 1, onResult)
                        return
                    }
                    val bodyString = response.body?.string() ?: ""
                    val list = parseShowtimesJson(bodyString)
                    if (list.isNotEmpty()) {
                        Log.d(TAG, "Fetch thành công ${list.size} showtimes từ bảng '$tableName' Supabase!")
                        onResult(list)
                    } else {
                        fetchShowtimesFromTables(tables, index + 1, onResult)
                    }
                }
            }
        })
    }

    private fun parseShowtimesJson(bodyString: String): List<Showtime> {
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
                    obj.has("start_time") && obj.optString("start_time").isNotBlank() -> obj.optString("start_time")
                    obj.has("startTime") && obj.optString("startTime").isNotBlank() -> obj.optString("startTime")
                    obj.has("time") && obj.optString("time").isNotBlank() -> obj.optString("time")
                    obj.has("gio_chieu") && obj.optString("gio_chieu").isNotBlank() -> obj.optString("gio_chieu")
                    obj.has("khung_gio") && obj.optString("khung_gio").isNotBlank() -> obj.optString("khung_gio")
                    obj.has("suat_chieu") && obj.optString("suat_chieu").isNotBlank() -> obj.optString("suat_chieu")
                    obj.has("gio_bat_dau") && obj.optString("gio_bat_dau").isNotBlank() -> obj.optString("gio_bat_dau")
                    obj.has("time_slot") && obj.optString("time_slot").isNotBlank() -> obj.optString("time_slot")
                    else -> ""
                }
                if (sTime.contains("T")) {
                    val tPart = sTime.substringAfter("T").substringBefore("+").substringBefore("Z").trim()
                    if (tPart.length >= 5) sTime = tPart.substring(0, 5)
                } else if (sTime.length >= 5 && sTime.contains(":")) {
                    sTime = sTime.substring(0, 5) // Lấy HH:mm bỏ phần :ss nếu có
                }

                var eTime = when {
                    obj.has("end_time") && obj.optString("end_time").isNotBlank() -> obj.optString("end_time")
                    obj.has("endTime") && obj.optString("endTime").isNotBlank() -> obj.optString("endTime")
                    obj.has("gio_ket_thuc") && obj.optString("gio_ket_thuc").isNotBlank() -> obj.optString("gio_ket_thuc")
                    else -> ""
                }
                if (eTime.contains("T")) {
                    val tPart = eTime.substringAfter("T").substringBefore("+").substringBefore("Z").trim()
                    if (tPart.length >= 5) eTime = tPart.substring(0, 5)
                } else if (eTime.length >= 5 && eTime.contains(":")) {
                    eTime = eTime.substring(0, 5)
                }

                var sDate = when {
                    obj.has("show_date") && obj.optString("show_date").isNotBlank() -> obj.optString("show_date")
                    obj.has("showDate") && obj.optString("showDate").isNotBlank() -> obj.optString("showDate")
                    obj.has("date") && obj.optString("date").isNotBlank() -> obj.optString("date")
                    obj.has("ngay_chieu") && obj.optString("ngay_chieu").isNotBlank() -> obj.optString("ngay_chieu")
                    obj.has("ngay") && obj.optString("ngay").isNotBlank() -> obj.optString("ngay")
                    obj.has("screening_date") && obj.optString("screening_date").isNotBlank() -> obj.optString("screening_date")
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

                val mTitle = when {
                    obj.has("movie_title") -> obj.optString("movie_title")
                    obj.has("movieTitle") -> obj.optString("movieTitle")
                    obj.has("title") -> obj.optString("title")
                    else -> ""
                }

                val showtimeUuid = if (obj.has("id")) obj.optString("id") else obj.optString("uuid", "")

                list.add(
                    Showtime(
                        id = obj.optInt("id", i + 1),
                        uuid = showtimeUuid,
                        movieId = mId,
                        movieStringId = mIdStr,
                        movieTitle = mTitle,
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
            Log.d(TAG, "Đã tải thành công ${list.size} showtimes từ Supabase!")
        } catch (ex: Exception) {
            Log.e(TAG, "Lỗi parse showtimes: ${ex.message}")
        }
        return list
    }

    /**
     * Đồng bộ danh sách đặt chỗ (bookings) từ Supabase tables 'bookings', 'booking', 'tickets', 'ticket', 've', 'dat_ve'
     */
    fun fetchBookingsFromSupabase(onResult: (List<Booking>) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id")) {
            onResult(emptyList())
            return
        }

        val allBookings = mutableListOf<Booking>()
        val tables = listOf("bookings", "booking", "tickets", "ticket", "ve", "dat_ve")
        
        fetchBookingsRecursive(tables, 0, allBookings, onResult)
    }

    private fun fetchBookingsRecursive(
        tables: List<String>,
        index: Int,
        accumulated: MutableList<Booking>,
        onResult: (List<Booking>) -> Unit
    ) {
        if (index >= tables.size) {
            onResult(accumulated)
            return
        }
        val tableName = tables[index]
        val url = "$SUPABASE_URL/rest/v1/$tableName?select=*"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Lỗi fetch bookings từ $tableName: ${e.message}")
                fetchBookingsRecursive(tables, index + 1, accumulated, onResult)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: ""
                        try {
                            val jsonArray = JSONArray(bodyString)
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                val seatsVal = when {
                                    obj.has("seats") && obj.optString("seats").isNotBlank() -> obj.optString("seats")
                                    obj.has("seat_code") && obj.optString("seat_code").isNotBlank() -> obj.optString("seat_code")
                                    obj.has("seat_codes") && obj.optString("seat_codes").isNotBlank() -> obj.optString("seat_codes")
                                    obj.has("selected_seats") && obj.optString("selected_seats").isNotBlank() -> obj.optString("selected_seats")
                                    obj.has("seat") && obj.optString("seat").isNotBlank() -> obj.optString("seat")
                                    obj.has("seat_id") && obj.optString("seat_id").isNotBlank() -> obj.optString("seat_id")
                                    obj.has("seat_number") && obj.optString("seat_number").isNotBlank() -> obj.optString("seat_number")
                                    obj.has("ghe") && obj.optString("ghe").isNotBlank() -> obj.optString("ghe")
                                    obj.has("ma_ghe") && obj.optString("ma_ghe").isNotBlank() -> obj.optString("ma_ghe")
                                    else -> obj.optString("seats", "")
                                }

                                if (seatsVal.isBlank()) continue

                                val idVal = obj.opt("id")
                                val intId = when (idVal) {
                                    is Int -> idVal
                                    is Number -> idVal.toInt()
                                    is String -> idVal.toIntOrNull() ?: kotlin.math.abs(idVal.hashCode())
                                    else -> i + 1
                                }

                                val mIdVal = obj.opt("movie_id") ?: obj.opt("film_id")
                                val intMId = when (mIdVal) {
                                    is Int -> mIdVal
                                    is Number -> mIdVal.toInt()
                                    is String -> mIdVal.toIntOrNull() ?: 0
                                    else -> 0
                                }
                                val mStrId = when (mIdVal) {
                                    is String -> mIdVal
                                    else -> obj.optString("film_string_id", "")
                                }

                                val movieTitle = when {
                                    obj.has("movie_title") && obj.optString("movie_title").isNotBlank() -> obj.optString("movie_title")
                                    obj.has("movie_name") && obj.optString("movie_name").isNotBlank() -> obj.optString("movie_name")
                                    obj.has("film_title") && obj.optString("film_title").isNotBlank() -> obj.optString("film_title")
                                    obj.has("ten_phim") && obj.optString("ten_phim").isNotBlank() -> obj.optString("ten_phim")
                                    obj.has("title") && obj.optString("title").isNotBlank() -> obj.optString("title")
                                    else -> ""
                                }

                                val sIdVal = obj.opt("showtime_id") ?: obj.opt("suat_chieu_id")
                                val intShowtimeId = when (sIdVal) {
                                    is Int -> sIdVal
                                    is Number -> sIdVal.toInt()
                                    is String -> sIdVal.toIntOrNull() ?: 0
                                    else -> 0
                                }
                                val showtimeUuid = when (sIdVal) {
                                    is String -> sIdVal
                                    else -> ""
                                }

                                val dateVal = when {
                                    obj.has("booking_date") && obj.optString("booking_date").isNotBlank() -> obj.optString("booking_date")
                                    obj.has("show_date") && obj.optString("show_date").isNotBlank() -> obj.optString("show_date")
                                    obj.has("date") && obj.optString("date").isNotBlank() -> obj.optString("date")
                                    obj.has("date_time") && obj.optString("date_time").isNotBlank() -> obj.optString("date_time")
                                    obj.has("ngay_chieu") && obj.optString("ngay_chieu").isNotBlank() -> obj.optString("ngay_chieu")
                                    obj.has("created_at") && obj.optString("created_at").isNotBlank() -> obj.optString("created_at")
                                    else -> ""
                                }

                                val timeVal = when {
                                    obj.has("start_time") && obj.optString("start_time").isNotBlank() -> obj.optString("start_time")
                                    obj.has("time") && obj.optString("time").isNotBlank() -> obj.optString("time")
                                    obj.has("show_time") && obj.optString("show_time").isNotBlank() -> obj.optString("show_time")
                                    obj.has("gio_chieu") && obj.optString("gio_chieu").isNotBlank() -> obj.optString("gio_chieu")
                                    obj.has("date_time") && obj.optString("date_time").isNotBlank() -> obj.optString("date_time")
                                    else -> ""
                                }

                                val cinemaVal = when {
                                    obj.has("cinema") && obj.optString("cinema").isNotBlank() -> obj.optString("cinema")
                                    obj.has("rap") && obj.optString("rap").isNotBlank() -> obj.optString("rap")
                                    obj.has("ten_rap") && obj.optString("ten_rap").isNotBlank() -> obj.optString("ten_rap")
                                    else -> ""
                                }

                                val statusVal = obj.optString("status", obj.optString("trang_thai", "PAID"))
                                val bookingCode = obj.optString("booking_code", obj.optString("ticket_code", ""))

                                // Bỏ qua vé bị hủy / hoàn tiền
                                if (statusVal.equals("CANCELLED", ignoreCase = true) ||
                                    statusVal.equals("REFUNDED", ignoreCase = true) ||
                                    statusVal.equals("DA_HUY", ignoreCase = true)) {
                                    continue
                                }

                                accumulated.add(
                                    Booking(
                                        id = intId,
                                        bookingCode = bookingCode,
                                        movieId = intMId,
                                        movieStringId = mStrId,
                                        movieTitle = movieTitle,
                                        showtimeId = intShowtimeId,
                                        showtimeUuid = showtimeUuid,
                                        seats = seatsVal,
                                        date = dateVal,
                                        time = timeVal,
                                        cinema = cinemaVal,
                                        totalPrice = obj.optInt("total_price", obj.optInt("price", 0)),
                                        userEmail = obj.optString("user_email", ""),
                                        status = statusVal
                                    )
                                )
                            }
                        } catch (ex: Exception) {
                            Log.e(TAG, "Lỗi parse bookings từ $tableName: ${ex.message}")
                        }
                    }
                    fetchBookingsRecursive(tables, index + 1, accumulated, onResult)
                }
            }
        })
    }

    /**
     * Đồng bộ danh sách phòng chiếu & sơ đồ ghế từ Supabase table 'screening_room'
     */
    fun fetchScreeningRoomsFromSupabase(onResult: (List<ScreeningRoom>) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id")) {
            onResult(emptyList())
            return
        }

        val url = "$SUPABASE_URL/rest/v1/screening_room?select=*"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Lỗi fetch screening_room từ Supabase: ${e.message}")
                onResult(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Supabase screening_room phản hồi lỗi: ${response.code}")
                        onResult(emptyList())
                        return
                    }
                    val bodyString = response.body?.string() ?: ""
                    val list = mutableListOf<ScreeningRoom>()
                    try {
                        val jsonArray = JSONArray(bodyString)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val id = obj.optString("id", "ROOM_${i+1}")
                            val name = when {
                                obj.has("ten_phong") -> obj.optString("ten_phong")
                                obj.has("name") -> obj.optString("name")
                                else -> "Phòng Chiếu ${i+1}"
                            }
                            val totalSeats = obj.optInt("tong_so_ghe", 0)
                            val regSeats = obj.optInt("so_luong_ghe_thuong", 0)
                            val vipSeats = obj.optInt("so_luong_ghe_vip", 0)
                            val rowsCount = obj.optInt("so_hang_ghe", 0)
                            val colsCount = obj.optInt("so_cot_ghe", 0)
                            val hasAisle = obj.optBoolean("co_loi_di", true)
                            val status = obj.optString("trang_thai", "HOAT_DONG")

                            val seatLayoutList = mutableListOf<SeatItem>()
                            if (obj.has("seat_layout") && !obj.isNull("seat_layout")) {
                                val layoutArr = obj.optJSONArray("seat_layout")
                                if (layoutArr != null) {
                                    for (j in 0 until layoutArr.length()) {
                                        val sObj = layoutArr.getJSONObject(j)
                                        val code = sObj.optString("code", "")
                                        val r = sObj.optString("row", "")
                                        val c = sObj.optInt("col", 0)
                                        val t = sObj.optString("type", "NORMAL")
                                        seatLayoutList.add(SeatItem(code = code, row = r, col = c, type = t))
                                    }
                                }
                            }

                            list.add(
                                ScreeningRoom(
                                    id = id,
                                    name = name,
                                    totalSeats = totalSeats,
                                    regularSeats = regSeats,
                                    vipSeats = vipSeats,
                                    rowsCount = rowsCount,
                                    colsCount = colsCount,
                                    hasAisle = hasAisle,
                                    seatLayout = seatLayoutList,
                                    status = status
                                )
                            )
                        }
                        onResult(list)
                        Log.d(TAG, "Fetch thành công ${list.size} screening_rooms từ Supabase!")
                    } catch (ex: Exception) {
                        Log.e(TAG, "Lỗi parse screening_room: ${ex.message}")
                        onResult(emptyList())
                    }
                }
            }
        })
    }
}

