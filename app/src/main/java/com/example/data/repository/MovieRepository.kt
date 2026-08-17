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

class MovieRepository(
    private val cinemaDao: CinemaDao,
    private val prefs: android.content.SharedPreferences? = null
) {

    val allTickets: Flow<List<Ticket>> = cinemaDao.getAllTickets()
    val allPromoCodes: Flow<List<PromoCode>> = cinemaDao.getAllPromoCodes()

    // Danh sách phim tĩnh chất lượng cao
    val movies = listOf(
        Movie(
            id = 1,
            title = "Lật Mặt 7: Một Điều Ước",
            genre = "Gia đình, Tâm lý",
            duration = 138,
            rating = 4.9f,
            ageRating = "P",
            releaseDate = "26/04/2024",
            synopsis = "Câu chuyện xoay quanh bà Hai và 5 người con đã khôn lớn. Mỗi người một nơi, một gia cảnh, nhưng khi bà Hai gặp tai nạn, câu hỏi đặt ra là ai sẽ là người chăm sóc mẹ? Một bộ phim đầy tính nhân văn, lấy đi nước mắt của triệu khán giả Việt.",
            posterUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&auto=format&fit=crop",
            director = "Lý Hải",
            cast = "Thanh Hiền, Trương Minh Cường, Đinh Y Nhung, Quách Ngọc Tuyên, Thanh Thức",
            price = 0
        ),
        Movie(
            id = 2,
            title = "Mai",
            genre = "Tâm lý, Tình cảm",
            duration = 131,
            rating = 4.8f,
            ageRating = "T18",
            releaseDate = "10/02/2024",
            synopsis = "Mai là một người phụ nữ gần 30 tuổi, làm nghề massage trị liệu và luôn gánh chịu định kiến của xã hội. Cuộc đời cô thay đổi khi cô gặp Dương, một chàng trai trẻ trung, đào hoa. Liệu tình yêu có đủ lớn để vượt qua mọi rào cản quá khứ?",
            posterUrl = "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=500&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=800&auto=format&fit=crop",
            director = "Trấn Thành",
            cast = "Phương Anh Đào, Tuấn Trần, Hồng Đào, Uyển Ân, Quốc Khánh",
            price = 0
        ),
        Movie(
            id = 3,
            title = "Doraemon: Bản Tình Ca Đất Nước",
            genre = "Hoạt hình, Phiêu lưu, Âm nhạc",
            duration = 115,
            rating = 4.7f,
            ageRating = "P",
            releaseDate = "24/05/2024",
            synopsis = "Nobita và các bạn tham gia vào một cuộc phiêu lưu âm nhạc kỳ thú để giải cứu Trái Đất khỏi một thế lực đen tối đe dọa biến mất toàn bộ âm thanh và âm nhạc toàn cầu. Phim tôn vinh tình bạn và sức mạnh kỳ diệu của âm nhạc.",
            posterUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=500&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&auto=format&fit=crop",
            director = "Imai Kazuaki",
            cast = "Wasabi Mizuta, Megumi Ohara, Yumi Kakazu, Subaru Kimura",
            price = 0
        ),
        Movie(
            id = 4,
            title = "Inside Out 2 (Những Mảnh Ghép Cảm Xúc 2)",
            genre = "Hoạt hình, Hài hước, Gia đình",
            duration = 96,
            rating = 4.8f,
            ageRating = "P",
            releaseDate = "14/06/2024",
            synopsis = "Riley bước vào tuổi dậy thì, và trung tâm chỉ huy của các cảm xúc bất ngờ trải qua một đợt nâng cấp lớn để nhường chỗ cho những cảm xúc mới cực kỳ phức tạp: Lo Âu (Anxiety), Ghen Tị (Envy), Xấu Hổ (Embarrassment) và Chán Nản (Ennui).",
            posterUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop",
            director = "Kelsey Mann",
            cast = "Amy Poehler, Maya Hawke, Kensington Tallman, Liza Lapira",
            price = 0
        ),
        Movie(
            id = 5,
            title = "Deadpool & Wolverine",
            genre = "Hành động, Viễn tưởng, Hài hước",
            duration = 127,
            rating = 4.9f,
            ageRating = "T18",
            releaseDate = "26/07/2026",
            synopsis = "Sau nhiều năm sống ẩn dật, Wade Wilson buộc phải tái xuất giang hồ và hợp tác cùng Wolverine - một biến thể siêu anh hùng cáu kỉnh đến từ vũ trụ khác - để thực hiện một nhiệm vụ tối mật giải cứu dòng thời gian của mình.",
            posterUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&auto=format&fit=crop",
            isNowShowing = false,
            director = "Shawn Levy",
            cast = "Ryan Reynolds, Hugh Jackman, Emma Corrin, Matthew Macfadyen",
            price = 0
        ),
        Movie(
            id = 6,
            title = "Despicable Me 4 (Kẻ Trộm Mặt Trăng 4)",
            genre = "Hoạt hình, Hài hước, Gia đình",
            duration = 94,
            rating = 4.7f,
            ageRating = "P",
            releaseDate = "15/08/2026",
            synopsis = "Gia đình Gru chào đón một thành viên mới cực kỳ đáng yêu nhưng cũng tinh nghịch - Gru Jr. Lần này, Gru cùng cả gia đình và biệt đội Minions nhí nhố buộc phải chạy trốn khi kẻ thù truyền kiếp Maxime Le Mal vượt ngục tìm cách trả thù.",
            posterUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=500&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=800&auto=format&fit=crop",
            isNowShowing = false,
            director = "Chris Renaud",
            cast = "Steve Carell, Kristen Wiig, Will Ferrell, Joey King",
            price = 0
        ),
        Movie(
            id = 7,
            title = "Godzilla x Kong: Đế Chế Mới",
            genre = "Hành động, Viễn tưởng, Phiêu lưu",
            duration = 115,
            rating = 4.8f,
            ageRating = "T16",
            releaseDate = "29/03/2024",
            synopsis = "Một cuộc phiêu lưu hoàn toàn mới đưa hai siêu quái vật Godzilla và Kong cùng nhau đối đầu với một mối đe dọa sinh tử tiềm ẩn trong thế giới của chúng ta, thách thức sự tồn vong của nhân loại.",
            posterUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop",
            director = "Adam Wingard",
            cast = "Rebecca Hall, Brian Tyree Henry, Dan Stevens, Kaylee Hottle",
            price = 0
        ),
        Movie(
            id = 8,
            title = "Bộ Tứ Báo Thủ",
            genre = "Hài hước, Gia đình, Hành động",
            duration = 110,
            rating = 4.7f,
            ageRating = "P",
            releaseDate = "29/01/2025",
            synopsis = "Câu chuyện hài hước dở khóc dở cười về bộ bốn bạn trẻ bá đạo với những tình huống 'báo thủ' tràn ngập tiếng cười dành cho cả gia đình trong dịp Tết.",
            posterUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=500&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&auto=format&fit=crop",
            director = "Trấn Thành",
            cast = "Lê Dương Bảo Lâm, Uyển Ân, Quốc Khánh, Lê Giang",
            price = 0
        ),
        Movie(
            id = 9,
            title = "Cám",
            genre = "Kinh dị, Cổ trang, Tâm lý",
            duration = 122,
            rating = 4.6f,
            ageRating = "T18",
            releaseDate = "20/09/2024",
            synopsis = "Dựa trên dị bản kinh dị bí ẩn từ truyện cổ tích Tấm Cám. Bóc tách từng góc khuất u tăm và sự tàn khốc phía sau gia tộc làng Hương với những nghi thức tế lễ đẫm máu.",
            posterUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&auto=format&fit=crop",
            director = "Trần Hữu Tấn",
            cast = "Rima Thanh Vy, Lâm Thanh Mỹ, Thúy Diễm, Quốc Cường",
            price = 0
        ),
        Movie(
            id = 10,
            title = "Hành Tinh Khỉ: Vương Quốc Mới",
            genre = "Viễn tưởng, Hành động, Phiêu lưu",
            duration = 145,
            rating = 4.8f,
            ageRating = "T13",
            releaseDate = "10/05/2024",
            synopsis = "Nhiều thế hệ sau triều đại của Caesar, loài khỉ là loài thống trị sống hòa hợp trong khi con người bị đẩy vào thế giới bóng tối. Một thủ lĩnh khỉ mới xây dựng đế chế độc tài, buộc chú khỉ trẻ Noa phải dấn thân vào hành trình định hình lại tương lai.",
            posterUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800&auto=format&fit=crop",
            director = "Wes Ball",
            cast = "Owen Teague, Freya Allan, Kevin Durand, Peter Macon",
            price = 0
        ),
        Movie(
            id = 11,
            title = "Mufasa: Vua Sư Tử",
            genre = "Hoạt hình, Phiêu lưu, Gia đình",
            duration = 118,
            rating = 4.9f,
            ageRating = "P",
            releaseDate = "20/12/2026",
            synopsis = "Câu chuyện huyền thoại về thời niên thiếu của Mufasa - từ một chú sư tử con mồ côi đi lạc đến khi trở thành Vua Sư Tử vĩ đại nhất của vùng đất Kiêu Hãnh.",
            posterUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=500&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=800&auto=format&fit=crop",
            isNowShowing = false,
            director = "Barry Jenkins",
            cast = "Aaron Pierre, Kelvin Harrison Jr., Seth Rogen, Billy Eichner",
            price = 0
        ),
        Movie(
            id = 12,
            title = "Sonic the Hedgehog 3",
            genre = "Hành động, Hài hước, Phiêu lưu",
            duration = 106,
            rating = 4.8f,
            ageRating = "P",
            releaseDate = "25/12/2026",
            synopsis = "Nhím Sonic, Tails và Knuckles tái hợp để đối đầu với một đối thủ mới cực kỳ bí ẩn và hùng mạnh mang tên Shadow - kẻ sở hữu sức mạnh vượt xa mọi tưởng tượng.",
            posterUrl = "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=500&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=800&auto=format&fit=crop",
            isNowShowing = false,
            director = "Jeff Fowler",
            cast = "Ben Schwartz, Keanu Reeves, Jim Carrey, James Marsden",
            price = 0
        )
    )

    // Lịch chiếu mặc định riêng biệt theo từng phim (khởi tạo 0đ, chờ đồng bộ chính xác từ Database)
    val defaultShowtimes = emptyList<com.example.data.model.Showtime>()

    // Đánh giá mặc định nếu DB trống
    private val defaultReviews = listOf(
        // Lật Mặt 7 (id = 1)
        Review(movieId = 1, author = "Minh Tuấn", rating = 5, content = "Phim xúc động quá, cả rạp ai cũng sụt sịt khóc. Lý Hải làm phim ngày càng lên tay, nội dung nhân văn sâu sắc về tình mẫu tử."),
        Review(movieId = 1, author = "Khánh Vy", rating = 5, content = "Diễn xuất của bà Hai xuất sắc thật sự. Câu chuyện rất đời thường, phản ánh đúng thực trạng gia đình Việt Nam hiện đại, ai cũng có một phần bản thân trong đó."),
        Review(movieId = 1, author = "Hoàng Long", rating = 4, content = "Kịch bản hơi dễ đoán nhưng cảm xúc mang lại rất trọn vẹn. Thích hợp đi xem cùng gia đình dịp cuối tuần để trân trọng mẹ hơn."),
        Review(movieId = 1, author = "Thu Trang", rating = 4, content = "Hơi nhiều tuyến nhân vật nên đoạn giữa cảm giác hơi loãng một chút, tuy nhiên cái kết rất cảm động và ý nghĩa."),

        // Mai (id = 2)
        Review(movieId = 2, author = "Quốc Anh", rating = 5, content = "Phương Anh Đào diễn đỉnh của chóp, ánh mắt nói lên tất cả nỗi đau khổ của nhân vật Mai. Phim buồn nhưng rất thấm và đáng suy ngẫm."),
        Review(movieId = 2, author = "Bích Phương", rating = 4, content = "Màu phim đẹp, nhạc phim hay da diết. Kết thúc thực tế nhưng hơi nghẹn lòng cho số phận của Mai. Đúng là tình cảm không thắng nổi định kiến."),
        Review(movieId = 2, author = "Duy Khánh", rating = 4, content = "Một bộ phim chỉn chu của Trấn Thành. Tuy nhiên một vài phân đoạn thoại vẫn còn mang tính triết lý hơi giáo điều, dài dòng."),
        Review(movieId = 2, author = "Thanh Thảo", rating = 5, content = "Cái kết làm mình buồn mất mấy ngày liền. Mong Mai ngoài đời thực sẽ có được hạnh phúc thực sự xứng đáng với cô ấy."),

        // Doraemon (id = 3)
        Review(movieId = 3, author = "Phúc Lâm", rating = 5, content = "Các bé nhà mình thích mê luôn, âm nhạc trong phim rất hoành tráng và tai nghe đã lắm. Phù hợp cho cả gia đình."),
        Review(movieId = 3, author = "Nhã Phương", rating = 5, content = "Phim nhẹ nhàng, thông điệp về âm nhạc kết nối mọi người rất ý nghĩa. Doraemon vẫn luôn là cả bầu trời tuổi thơ của mình."),
        Review(movieId = 3, author = "Anh Đức", rating = 4, content = "Đồ họa đẹp mắt, cốt truyện hơi đơn giản so với các phần thám hiểm vũ trụ trước nhưng bù lại mảng âm nhạc cứu cánh cực kỳ xuất sắc."),

        // Inside Out 2 (id = 4)
        Review(movieId = 4, author = "Hương Giang", rating = 5, content = "Quá xuất sắc! Nhân vật Lo Âu (Anxiety) được xây dựng cực kỳ thực tế, ai đã và đang trải qua rồi mới thấy đồng cảm sâu sắc."),
        Review(movieId = 4, author = "Minh Triết", rating = 5, content = "Một bộ phim hoạt hình tuyệt vời không chỉ cho trẻ em mà còn cả người lớn. Giúp chúng ta hiểu, ôm ấp và yêu thương mọi cảm xúc của chính mình."),
        Review(movieId = 4, author = "Thùy Dương", rating = 5, content = "Màu sắc rực rỡ, các nhân vật cảm xúc mới siêu đáng yêu và chân thực. Xem xong cảm thấy nhẹ lòng và bớt áp lực hơn nhiều."),
        Review(movieId = 4, author = "Tuấn Đạt", rating = 5, content = "Hay hơn cả phần 1 luôn ấy, Disney Pixar chưa bao giờ làm người hâm mộ thất vọng về mặt ý tưởng sáng tạo và chiều sâu tâm lý.")
    )

    fun getReviewsForMovie(movieId: Int): Flow<List<Review>> {
        // Đồng bộ hóa dữ liệu từ Supabase về Room khi người dùng xem đánh giá
        com.example.data.supabase.SupabaseSyncService.fetchReviewsFromSupabase(movieId) { remoteReviews ->
            if (remoteReviews.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    cinemaDao.insertReviews(remoteReviews)
                }
            }
        }
        return cinemaDao.getReviewsForMovie(movieId)
    }

    suspend fun addReview(review: Review) = withContext(Dispatchers.IO) {
        cinemaDao.insertReview(review)
        com.example.data.supabase.SupabaseSyncService.pushReviewToSupabase(review) { success ->
            // Đã đồng bộ với Supabase backend thành công
        }
    }

    suspend fun bookTicket(ticket: Ticket) = withContext(Dispatchers.IO) {
        cinemaDao.insertTicket(ticket)
        com.example.data.supabase.SupabaseSyncService.pushTicketToSupabase(ticket) { success ->
            // Đã lưu vé lên Supabase backend thành công
        }
    }

    suspend fun deleteTicket(ticketId: Int, barcode: String = "", bookingCode: String = "") = withContext(Dispatchers.IO) {
        cinemaDao.deleteTicket(ticketId)
        if (barcode.isNotBlank() || bookingCode.isNotBlank()) {
            prefs?.let { p ->
                val deleted = p.getStringSet("deleted_ticket_barcodes", emptySet())?.toMutableSet() ?: mutableSetOf()
                if (barcode.isNotBlank()) deleted.add(barcode)
                if (bookingCode.isNotBlank()) deleted.add(bookingCode)
                p.edit().putStringSet("deleted_ticket_barcodes", deleted).apply()
            }
            com.example.data.supabase.SupabaseSyncService.deleteTicketFromSupabase(barcode, bookingCode)
        }
    }

    suspend fun deleteAllTickets(userEmail: String = "") = withContext(Dispatchers.IO) {
        cinemaDao.deleteAllTickets()
        prefs?.edit()?.putLong("tickets_cleared_timestamp", System.currentTimeMillis())?.apply()
        com.example.data.supabase.SupabaseSyncService.deleteAllTicketsFromSupabase(userEmail)
    }

    suspend fun syncTicketsFromSupabase() = withContext(Dispatchers.IO) {
        com.example.data.supabase.SupabaseSyncService.fetchTicketsFromSupabase { remoteTickets ->
            if (remoteTickets.isNotEmpty()) {
                val deletedBarcodes = prefs?.getStringSet("deleted_ticket_barcodes", emptySet()) ?: emptySet()
                val clearedTimestamp = prefs?.getLong("tickets_cleared_timestamp", 0L) ?: 0L

                // Lọc bỏ tất cả các vé đã từng bị người dùng bấm xóa, vé cũ trước thời điểm xóa, hoặc vé lỗi/rác
                val validTickets = remoteTickets.filter { ticket ->
                    val isDeleted = deletedBarcodes.contains(ticket.barcode) || 
                                    (ticket.bookingCode.isNotBlank() && deletedBarcodes.contains(ticket.bookingCode))
                    val isOldCleared = clearedTimestamp > 0L && ticket.timestamp <= clearedTimestamp
                    val isInvalid = ticket.movieTitle.isBlank() || ticket.movieTitle == "Phim Chiếu Rạp" || ticket.movieTitle == "Phim"
                    !isDeleted && !isOldCleared && !isInvalid
                }

                if (validTickets.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        for (ticket in validTickets) {
                            if (ticket.barcode.isNotBlank()) {
                                val existing = cinemaDao.getTicketByBarcode(ticket.barcode)
                                if (existing == null) {
                                    cinemaDao.insertTicket(ticket)
                                }
                            }
                        }
                    }
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

    suspend fun initDefaultDataIfNeeded() = withContext(Dispatchers.IO) {
        for (movie in movies) {
            val count = cinemaDao.getReviewCount(movie.id)
            if (count == 0) {
                val movieReviews = defaultReviews.filter { it.movieId == movie.id }
                cinemaDao.insertReviews(movieReviews)
            }
        }
        val promoCount = cinemaDao.getPromoCodeCount()
        if (promoCount == 0) {
            val defaultPromos = listOf(
                PromoCode("NEON30", 30000, "Giảm ngay 30.000đ cho đơn hàng của bạn"),
                PromoCode("HE2026", 50000, "Chào hè 2026 rực rỡ giảm giá cực sốc 50.000đ"),
                PromoCode("BAPNUOC", 20000, "Mã giảm giá 20.000đ khi mua combo bắp nước"),
                PromoCode("PREMIUM", 100000, "Mã giảm giá hạng VIP trị giá 100.000đ")
            )
            cinemaDao.insertPromoCodes(defaultPromos)
            // Đẩy các mã giảm giá mặc định lên Supabase
            for (promo in defaultPromos) {
                com.example.data.supabase.SupabaseSyncService.pushPromoCodeToSupabase(promo) {}
            }
        }
    }

    suspend fun getPromoCode(code: String): PromoCode? = withContext(Dispatchers.IO) {
        cinemaDao.getPromoCode(code)
    }

    suspend fun updatePromoCodeUsage(code: String, isUsed: Boolean) = withContext(Dispatchers.IO) {
        cinemaDao.updatePromoCodeUsage(code, isUsed)
        com.example.data.supabase.SupabaseSyncService.updatePromoCodeUsageInSupabase(code, isUsed) { success ->
            // Đã đồng bộ với Supabase thành công
        }
    }

    suspend fun analyzeReviewsWithAI(movieId: Int, movieTitle: String, reviews: List<Review>): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        // Nếu không có API Key thực tế hoặc là key giả định, sử dụng kết quả phân tích chất lượng cao offline tức thì
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineAIAnalysis(movieId)
        }

        val reviewsText = reviews.joinToString("\n") { "- Tác giả ${it.author} (${it.rating} sao): ${it.content}" }

        val systemPrompt = "Bạn là Trợ lý AI phân tích đánh giá phim chuyên nghiệp của Momo Cinema. Trả lời bằng tiếng Việt lịch sự, thân thiện và khách quan."

        val prompt = """
            Dưới đây là danh sách các đánh giá thực tế từ khách hàng cho bộ phim: "$movieTitle".

            Các đánh giá:
            $reviewsText

            Nhiệm vụ của bạn là phân tích tổng hợp các đánh giá trên và cung cấp một báo cáo đánh giá chi tiết bằng tiếng Việt định dạng Markdown sạch sẽ gồm các nội dung chính xác sau:
            
            ### 📝 Tóm tắt ý kiến khán giả
            (Tóm tắt chung 2-3 câu thể hiện góc nhìn của khán giả)

            ### ✨ Điểm mạnh nổi bật
            - (Gạch đầu dòng 1)
            - (Gạch đầu dòng 2)
            - (Gạch đầu dòng 3)

            ### ⚠️ Điểm hạn chế cần lưu ý
            - (Gạch đầu dòng 1)
            - (Gạch đầu dòng 2)

            ### 🎯 Lời khuyên người xem
            - Phù hợp nhất cho: ...
            - Gợi ý trải nghiệm: ...

            ### ⭐ Điểm số AI Đánh Giá: X/10
            (Hãy đưa ra điểm số thông minh dựa trên tỷ lệ hài lòng từ đánh giá)
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
            // Fallback khi lỗi kết nối mạng hoặc lỗi API
            getOfflineAIAnalysis(movieId)
        }
    }

    private fun getOfflineAIAnalysis(movieId: Int): String {
        return when (movieId) {
            1 -> """
                ### 📝 Tóm tắt ý kiến khán giả
                Bộ phim **Lật Mặt 7: Một Điều Ước** nhận được cơn mưa lời khen từ khán giả Việt Nam nhờ chủ đề gia đình xúc động và nhân văn. Phim mang lại những cảm xúc trọn vẹn, chân thực, đặc biệt là hình ảnh người mẹ tảo tần lấy đi nước mắt của hàng triệu người xem.

                ### ✨ Điểm mạnh nổi bật
                - **Nội dung chạm đến trái tim**: Chủ đề tình mẫu tử thiêng liêng được Lý Hải khai thác vô cùng sâu sắc, mộc mạc và chân thực.
                - **Diễn xuất đỉnh cao**: Diễn xuất của NSƯT Thanh Hiền trong vai bà Hai xuất sắc vượt trội, dẫn dắt cảm xúc cực kỳ tốt.
                - **Kịch bản đời thường và ý nghĩa**: Phản ánh chân thực thực trạng các gia đình Việt hiện đại khi con cái mải mê lo toan cuộc sống riêng mà quên mất cha mẹ mỏi mòn đợi chờ.

                ### ⚠️ Điểm hạn chế cần lưu ý
                - **Cốt truyện tương đối dễ đoán**: Đi theo mô-típ gia đình truyền thống nên kết cục không có quá nhiều bất ngờ kịch tính.
                - **Quá nhiều tuyến nhân vật**: Với 5 người con, nhịp phim ở giữa đôi lúc bị loãng do phải phân bổ thời lượng giải quyết từng gia cảnh.

                ### 🎯 Lời khuyên người xem
                - **Phù hợp nhất cho**: Gia đình nhiều thế hệ đi xem cùng nhau, đặc biệt là con cái đi xem cùng bố mẹ.
                - **Gợi ý trải nghiệm**: Chuẩn bị sẵn khăn giấy vì các phân đoạn cuối cực kỳ xúc động.

                ### ⭐ Điểm số AI Đánh Giá: 9.2/10
            """.trimIndent()
            2 -> """
                ### 📝 Tóm tắt ý kiến khán giả
                **Mai** là một tác phẩm điện ảnh xuất sắc và chỉn chu về mặt hình ảnh cũng như chiều sâu tâm lý của đạo diễn Trấn Thành. Khán giả đánh giá cao màn thể hiện đỉnh cao của Phương Anh Đào cùng cái kết vô cùng thực tế nhưng đượm buồn.

                ### ✨ Điểm mạnh nổi bật
                - **Diễn xuất xuất thần của nữ chính**: Phương Anh Đào thể hiện hoàn hảo tâm lý phức tạp, nỗi khổ đau và khao khát hạnh phúc của nhân vật Mai.
                - **Hình ảnh và Âm nhạc tuyệt vời**: Góc quay nghệ thuật, màu phim trầm ấm kết hợp nhạc phim da diết tạo hiệu ứng cảm xúc mạnh mẽ.
                - **Sức nặng tâm lý**: Khai thác sâu sắc định kiến xã hội khắt khe đè nặng lên người phụ nữ làm nghề nhạy cảm.

                ### ⚠️ Điểm hạn chế cần lưu ý
                - **Cái kết gây hụt hẫng và buồn**: Kết thúc hiện thực phũ phàng khiến người xem có cảm giác nặng nề sau khi ra rạp.
                - **Lời thoại đôi chỗ giáo điều**: Một số phân đoạn nhân vật Dương nói chuyện vẫn mang tính thuyết giảng, triết lý sống hơi khiên cưỡng.

                ### 🎯 Lời khuyên người xem
                - **Phù hợp nhất cho**: Các cặp đôi trưởng thành, nhóm bạn trẻ yêu thích thể loại tâm lý - tình cảm sâu sắc.
                - **Gợi ý trải nghiệm**: Phim dán nhãn **T18** nên hãy chắc chắn mang theo giấy tờ tùy thân khi đặt vé.

                ### ⭐ Điểm số AI Đánh Giá: 8.8/10
            """.trimIndent()
            3 -> """
                ### 📝 Tóm tắt ý kiến khán giả
                **Doraemon: Bản Tình Ca Đất Nước** tiếp tục là sự lựa chọn tuyệt vời cho các gia đình và bạn trẻ yêu mến chú mèo máy. Phim ghi điểm tuyệt đối nhờ mảng âm thanh, âm nhạc hoành tráng được đầu tư công phu, mang thông điệp ý nghĩa về sự gắn kết.

                ### ✨ Điểm mạnh nổi bật
                - **Đại tiệc âm thanh hoành tráng**: Sự kết hợp xuất sắc giữa nhạc giao hưởng và cốt truyện phiêu lưu kỳ thú giúp kích thích thính giác người xem.
                - **Thông điệp nhân văn**: Đề cao giá trị của âm nhạc trong cuộc sống và sức mạnh vô song của tình bạn quen thuộc.
                - **Đồ họa mượt mà, tươi sáng**: Tạo hình các sinh vật âm nhạc đáng yêu, chuyển động nhân vật mềm mại.

                ### ⚠️ Điểm hạn chế cần lưu ý
                - **Cốt truyện khá đơn giản**: Nội dung tương đối an toàn, dễ đoán hơn so với các phần thám hiểm không gian bí ẩn trước đây.
                - **Nhịp phim đầu hơi chậm**: Phần giới thiệu câu chuyện và luyện tập nhạc cụ có thể khiến một số khán giả nhỏ tuổi thiếu kiên nhẫn.

                ### 🎯 Lời khuyên người xem
                - **Phù hợp nhất cho**: Các gia đình có con nhỏ, nhóm bạn trẻ muốn tìm kiếm một vé đi tuổi thơ thư giãn.
                - **Gợi ý trải nghiệm**: Nên chọn các rạp có hệ thống âm thanh chất lượng cao (Dolby Atmos) để thưởng thức trọn vẹn bản nhạc giao hưởng.

                ### ⭐ Điểm số AI Đánh Giá: 8.5/10
            """.trimIndent()
            4 -> """
                ### 📝 Tóm tắt ý kiến khán giả
                **Inside Out 2** là sự trở lại bùng nổ vượt ngoài mong đợi từ nhà Disney Pixar. Phim nhận được đánh giá tuyệt đối từ khán giả nhờ việc khắc họa cực kỳ chân thực cảm xúc **Lo Âu (Anxiety)** của tuổi dậy thì, tạo sự đồng cảm sâu sắc cho cả người lớn.

                ### ✨ Điểm mạnh nổi bật
                - **Sáng tạo vượt bậc**: Cách hình hóa các khái niệm tâm lý phức tạp của tuổi dậy thì như dòng ý thức, hệ thống niềm tin cực kỳ trực quan và thông minh.
                - **Nhân vật mới xuất sắc**: Nhân vật Lo Âu (Anxiety) được xây dựng quá thực tế và đáng yêu, chạm đến nỗi lòng của hầu hết người xem hiện đại.
                - **Bài học sâu sắc**: Thông điệp giá trị về việc chấp nhận tất cả các cảm xúc, kể cả những mảnh ghép tiêu cực để trưởng thành tốt hơn.

                ### ⚠️ Điểm hạn chế cần lưu ý
                - **Nhịp độ phim dồn dập**: Việc giải quyết mâu thuẫn ở cuối phim có cảm giác hơi nhanh để kịp thời lượng phim hoạt hình.

                ### 🎯 Lời khuyên người xem
                - **Phù hợp nhất cho**: Bất cứ ai đang cảm thấy áp lực cuộc sống, học sinh sinh viên, cha mẹ có con sắp bước vào tuổi dậy thì.
                - **Gợi ý trải nghiệm**: Thích hợp xem cả bản phụ đề tiếng Anh lẫn lồng tiếng Việt chất lượng cao.

                ### ⭐ Điểm số AI Đánh Giá: 9.5/10
            """.trimIndent()
            else -> """
                ### 📝 Tóm tắt ý kiến khán giả
                Khán giả đánh giá cao tính giải trí của bộ phim. Phim có dàn diễn viên chất lượng cùng kỹ xảo âm thanh hình ảnh chỉn chu, mang lại những phút giây thư giãn thoải mái.

                ### ✨ Điểm mạnh nổi bật
                - Giải trí tốt, nhịp phim nhanh gọn.
                - Dàn diễn viên hợp vai, ngoại hình sáng.
                - Nhạc phim bắt tai, hợp thị hiếu.

                ### ⚠️ Điểm hạn chế cần lưu ý
                - Kịch bản còn một số điểm phi logic nhỏ.
                - Chiều sâu nhân vật chưa được khai thác triệt để.

                ### 🎯 Lời khuyên người xem
                - Phù hợp nhất cho nhóm bạn giải trí cuối tuần.

                ### ⭐ Điểm số AI Đánh Giá: 8.0/10
            """.trimIndent()
        }
    }
}
