package com.example.data.supabase

import android.util.Log
import com.example.data.model.Movie
import com.example.data.model.Review
import com.example.data.model.Ticket
import com.example.data.model.UserProfile
import com.example.data.model.PromoCode
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * SupabaseSyncService - Bộ đồng bộ hóa dữ liệu thời gian thực giữa ứng dụng Kotlin và Supabase PostgreSQL.
 * Sử dụng Supabase REST API (PostgREST) qua OkHttp để đảm bảo hiệu năng cao, nhẹ và độc lập.
 */
object SupabaseSyncService {
    private const val TAG = "SupabaseSync"

    // Điền thông tin Supabase của bạn ở đây để kết nối với cơ sở dữ liệu thật!
    // Bạn cũng có thể thiết lập các khóa này trong Secrets Panel của AI Studio.
    private const val SUPABASE_URL = "https://rdfnidhtqoyshjawnxqt.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJkZm5pZGh0cW95c2hqYXdueHF0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODM0MjUwNTcsImV4cCI6MjA5OTAwMTA1N30.nVMPKkGdcy_pSGseK2mdxBkg9skOzB69GMpyK-iSApw"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Đồng bộ danh sách đánh giá của phim từ Supabase table 'reviews'
     */
    fun fetchReviewsFromSupabase(movieId: Int, onResult: (List<Review>) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id")) {
            Log.d(TAG, "Chưa cấu hình Supabase URL. Đang dùng dữ liệu Local cache.")
            return
        }

        val url = "$SUPABASE_URL/rest/v1/reviews?movie_id=eq.$movieId&select=*"
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
                        Log.e(TAG, "Supabase phản hồi lỗi: ${response.code}")
                        return
                    }
                    val bodyString = response.body?.string() ?: return
                    try {
                        val type = Types.newParameterizedType(List::class.java, Review::class.java)
                        val adapter = moshi.adapter<List<Review>>(type)
                        val reviews = adapter.fromJson(bodyString) ?: emptyList()
                        onResult(reviews)
                        Log.d(TAG, "Fetch thành công ${reviews.size} reviews từ Supabase!")
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi phân tích JSON Supabase: ${e.message}")
                    }
                }
            }
        })
    }

    /**
     * Đẩy đánh giá mới lên Supabase table 'reviews'
     */
    fun pushReviewToSupabase(review: Review, onComplete: (Boolean) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id")) {
            Log.d(TAG, "Supabase chưa cấu hình. Đã lưu review vào cơ sở dữ liệu Room Local.")
            onComplete(true)
            return
        }

        val url = "$SUPABASE_URL/rest/v1/reviews"
        val adapter = moshi.adapter(Review::class.java)
        val json = adapter.toJson(review)

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
            Log.d(TAG, "Chưa cấu hình Supabase URL. Đang dùng dữ liệu Local cache.")
            return
        }

        val url = "$SUPABASE_URL/rest/v1/tickets?select=*&order=timestamp.desc"
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
                        Log.e(TAG, "Supabase phản hồi lỗi: ${response.code}")
                        return
                    }
                    val bodyString = response.body?.string() ?: return
                    try {
                        val type = Types.newParameterizedType(List::class.java, Ticket::class.java)
                        val adapter = moshi.adapter<List<Ticket>>(type)
                        val tickets = adapter.fromJson(bodyString) ?: emptyList()
                        onResult(tickets)
                        Log.d(TAG, "Fetch thành công ${tickets.size} tickets từ Supabase!")
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi phân tích JSON Supabase tickets: ${e.message}")
                    }
                }
            }
        })
    }

    /**
     * Đẩy vé đã thanh toán lên Supabase table 'tickets'
     */
    fun pushTicketToSupabase(ticket: Ticket, onComplete: (Boolean) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id")) {
            Log.d(TAG, "Supabase chưa cấu hình. Đã lưu vé vào Room Local và hiển thị lịch sử.")
            onComplete(true)
            return
        }

        val url = "$SUPABASE_URL/rest/v1/tickets"
        val adapter = moshi.adapter(Ticket::class.java)
        val json = adapter.toJson(ticket)

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
        if (SUPABASE_URL.contains("your-project-id")) {
            onComplete(true)
            return
        }

        val url = "$SUPABASE_URL/rest/v1/profiles"
        val adapter = moshi.adapter(UserProfile::class.java)
        val json = adapter.toJson(profile)

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
        if (SUPABASE_URL.contains("your-project-id")) {
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
                        val type = Types.newParameterizedType(List::class.java, UserProfile::class.java)
                        val adapter = moshi.adapter<List<UserProfile>>(type)
                        val profiles = adapter.fromJson(bodyString)
                        if (!profiles.isNullOrEmpty()) {
                            onResult(profiles[0])
                        } else {
                            onResult(null)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi phân tích JSON Supabase profile: ${e.message}")
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
                    try {
                        val type = Types.newParameterizedType(List::class.java, Movie::class.java)
                        val adapter = moshi.adapter<List<Movie>>(type)
                        val moviesList = adapter.fromJson(bodyString)
                        onResult(moviesList)
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi phân tích JSON Supabase movies: ${e.message}")
                        onResult(null)
                    }
                }
            }
        })
    }

    /**
     * Đồng bộ danh sách mã giảm giá từ Supabase table 'promo_codes'
     */
    fun fetchPromoCodesFromSupabase(onResult: (List<PromoCode>) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id")) {
            Log.d(TAG, "Chưa cấu hình Supabase URL. Đang dùng dữ liệu Local cache.")
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
                        Log.e(TAG, "Supabase phản hồi lỗi: ${response.code}")
                        return
                    }
                    val bodyString = response.body?.string() ?: return
                    try {
                        val type = Types.newParameterizedType(List::class.java, PromoCode::class.java)
                        val adapter = moshi.adapter<List<PromoCode>>(type)
                        val promoCodes = adapter.fromJson(bodyString) ?: emptyList()
                        onResult(promoCodes)
                        Log.d(TAG, "Fetch thành công ${promoCodes.size} promo_codes từ Supabase!")
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi phân tích JSON Supabase promo_codes: ${e.message}")
                    }
                }
            }
        })
    }

    /**
     * Đẩy mã giảm giá lên Supabase table 'promo_codes'
     */
    fun pushPromoCodeToSupabase(promoCode: PromoCode, onComplete: (Boolean) -> Unit) {
        if (SUPABASE_URL.contains("your-project-id")) {
            onComplete(true)
            return
        }

        val url = "$SUPABASE_URL/rest/v1/promo_codes"
        val adapter = moshi.adapter(PromoCode::class.java)
        val json = adapter.toJson(promoCode)

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
        if (SUPABASE_URL.contains("your-project-id")) {
            onComplete(true)
            return
        }

        val url = "$SUPABASE_URL/rest/v1/promo_codes?code=eq.$code"
        val payload = "{\"is_used\": $isUsed}"

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
}
