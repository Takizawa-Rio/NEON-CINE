package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.CinemaDatabase
import com.example.data.model.Movie
import com.example.data.model.Review
import com.example.data.model.Ticket
import com.example.data.model.UserProfile
import com.example.data.model.PromoCode
import com.example.data.model.UserNotification
import com.example.data.repository.MovieRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

sealed class AIAnalysisState {
    object Idle : AIAnalysisState()
    object Loading : AIAnalysisState()
    data class Success(val analysis: String) : AIAnalysisState()
    data class Error(val message: String) : AIAnalysisState()
}

class MovieViewModel(application: Application) : AndroidViewModel(application) {
    private val database = CinemaDatabase.getDatabase(application)
    private val repository = MovieRepository(database.cinemaDao())
    private val prefs = application.getSharedPreferences("neon_cine_prefs", android.content.Context.MODE_PRIVATE)

    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    private val _customMovieDatesTrigger = MutableStateFlow(0)

    val rawMoviesList: StateFlow<List<Movie>> = _movies.asStateFlow()

    val movies: StateFlow<List<Movie>> = combine(_movies, _customMovieDatesTrigger) { rawMovies, _ ->
        val currentDate = Date()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        rawMovies.mapNotNull { movie ->
            val customStart = prefs.getString("movie_${movie.id}_start_date", null)
            val customEnd = prefs.getString("movie_${movie.id}_end_date", null)
            
            val startDate = if (!customStart.isNullOrBlank()) {
                try { sdf.parse(customStart) } catch (e: Exception) { null }
            } else null
            
            val endDate = if (!customEnd.isNullOrBlank()) {
                try { sdf.parse(customEnd) } catch (e: Exception) { null }
            } else null
            
            // Expiry Check: If end date is set and current date is past the end date (end of that day)
            if (endDate != null) {
                val calEnd = Calendar.getInstance().apply {
                    time = endDate
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                if (currentDate.after(calEnd.time)) {
                    // This movie is expired and should be automatically removed (gỡ bỏ)!
                    return@mapNotNull null
                }
            }
            
            // Status determination: Now Showing vs Coming Soon
            val isNowShowingNew = if (startDate != null) {
                val calStart = Calendar.getInstance().apply {
                    time = startDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                currentDate.after(calStart.time) || currentDate.equals(calStart.time)
            } else {
                movie.isNowShowing
            }
            
            movie.copy(isNowShowing = isNowShowingNew)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = repository.movies
    )

    // Lịch sử vé đã đặt
    val tickets: StateFlow<List<Ticket>> = repository.allTickets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Danh sách mã giảm giá trong CSDL
    val promoCodes: StateFlow<List<PromoCode>> = repository.allPromoCodes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Phim đang được chọn để xem chi tiết
    private val _selectedMovie = MutableStateFlow<Movie?>(null)
    val selectedMovie: StateFlow<Movie?> = _selectedMovie.asStateFlow()

    // Đánh giá của bộ phim đang chọn
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMovieReviews: StateFlow<List<Review>> = _selectedMovie
        .flatMapLatest { movie ->
            if (movie != null) {
                repository.getReviewsForMovie(movie.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Trạng thái AI đánh giá
    private val _aiAnalysisState = MutableStateFlow<AIAnalysisState>(AIAnalysisState.Idle)
    val aiAnalysisState: StateFlow<AIAnalysisState> = _aiAnalysisState.asStateFlow()

    // Trình chọn tab chính (0: Mua vé, 1: Lịch chiếu, 2: Vé của tôi, 3: Tôi)
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // --- State cho Splash Screen ---
    private val _showSplashScreen = MutableStateFlow(true)
    val showSplashScreen: StateFlow<Boolean> = _showSplashScreen.asStateFlow()

    // --- State cho Vị Trí Hiện Tại ---
    private val _userLocationAddress = MutableStateFlow("Quận 1, TP. Hồ Chí Minh")
    val userLocationAddress: StateFlow<String> = _userLocationAddress.asStateFlow()

    fun updateUserLocation(address: String) {
        _userLocationAddress.value = address
    }

    // --- State cho Đăng Nhập & Đăng Ký ---
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _showLoginRequiredDialog = MutableStateFlow(false)
    val showLoginRequiredDialog: StateFlow<Boolean> = _showLoginRequiredDialog.asStateFlow()

    fun showLoginRequired() {
        _showLoginRequiredDialog.value = true
    }

    fun dismissLoginRequiredDialog() {
        _showLoginRequiredDialog.value = false
    }

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userPoints = MutableStateFlow(150) // Điểm tích lũy thành viên Neon Club
    val userPoints: StateFlow<Int> = _userPoints.asStateFlow()

    private val _userBalance = MutableStateFlow(500000) // Ví Neon Pay tích hợp 500.000đ mặc định
    val userBalance: StateFlow<Int> = _userBalance.asStateFlow()

    // --- State cho luồng Đặt Vé ---
    private val _isBookingFlowActive = MutableStateFlow(false)
    val isBookingFlowActive: StateFlow<Boolean> = _isBookingFlowActive.asStateFlow()

    // Cấu hình rạp chiếu cố định (Vincom Xuân Khánh, Cần Thơ)
    private val _cinemaName = MutableStateFlow("Neon Cine Space - Vincom Xuân Khánh")
    val cinemaName: StateFlow<String> = _cinemaName.asStateFlow()

    private val _cinemaAddress = MutableStateFlow("Tầng 4, TTTM Vincom Plaza Xuân Khánh, 209 Đường 30 Tháng 4, Xuân Khánh, Ninh Kiều, Cần Thơ")
    val cinemaAddress: StateFlow<String> = _cinemaAddress.asStateFlow()

    private val _cinemaMapQuery = MutableStateFlow("Vincom Plaza Xuan Khanh Can Tho")
    val cinemaMapQuery: StateFlow<String> = _cinemaMapQuery.asStateFlow()

    // Chi tiết luồng đặt vé
    private val _selectedCinema = MutableStateFlow("Neon Cine Space - Vincom Xuân Khánh")
    val selectedCinema: StateFlow<String> = _selectedCinema.asStateFlow()

    private val _selectedDate = MutableStateFlow("")
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedTime = MutableStateFlow("19:00")
    val selectedTime: StateFlow<String> = _selectedTime.asStateFlow()

    private val _selectedSeats = MutableStateFlow<Set<String>>(emptySet())
    val selectedSeats: StateFlow<Set<String>> = _selectedSeats.asStateFlow()

    // Lịch chiếu & đặt chỗ đồng bộ từ Supabase DB
    private val _showtimes = MutableStateFlow<List<com.example.data.model.Showtime>>(emptyList())
    val showtimes: StateFlow<List<com.example.data.model.Showtime>> = _showtimes.asStateFlow()

    private val _bookings = MutableStateFlow<List<com.example.data.model.Booking>>(emptyList())
    val bookings: StateFlow<List<com.example.data.model.Booking>> = _bookings.asStateFlow()

    // Danh sách suất chiếu theo thời gian động từ cơ sở dữ liệu Supabase
    val availableTimeSlots: StateFlow<List<String>> = combine(_selectedMovie, _showtimes) { movie, list ->
        if (movie != null) {
            val movieShowtimes = list.filter { it.movieId == movie.id }.map { it.startTime }.filter { it.isNotBlank() }
            if (movieShowtimes.isNotEmpty()) {
                return@combine movieShowtimes.distinct()
            }
        }
        listOf("10:00", "13:30", "16:15", "19:00", "21:30")
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("10:00", "13:30", "16:15", "19:00", "21:30")
    )

    // Danh sách ghế đã được đặt cho suất chiếu hiện tại (đồng bộ từ cả tickets & bookings trong Supabase)
    val bookedSeats: StateFlow<Set<String>> = combine(
        _selectedMovie,
        _selectedCinema,
        _selectedDate,
        _selectedTime,
        tickets,
        _bookings
    ) { flowArray ->
        val movie = flowArray[0] as? Movie
        val cinema = flowArray[1] as? String ?: ""
        val date = flowArray[2] as? String ?: ""
        val time = flowArray[3] as? String ?: ""
        @Suppress("UNCHECKED_CAST")
        val allTickets = flowArray[4] as? List<Ticket> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val allBookings = flowArray[5] as? List<com.example.data.model.Booking> ?: emptyList()

        if (movie == null) return@combine emptySet<String>()
        val dateTimeTarget = "$date lúc $time"
        val seatsFromTickets = allTickets
            .filter { ticket ->
                ticket.movieId == movie.id &&
                (ticket.cinema.isEmpty() || ticket.cinema == cinema) &&
                (ticket.dateTime.isEmpty() || ticket.dateTime.contains(time) || ticket.dateTime == dateTimeTarget)
            }
            .flatMap { ticket ->
                ticket.seats.split(",").map { it.trim() }
            }

        val seatsFromBookings = allBookings
            .filter { booking -> booking.movieId == movie.id }
            .flatMap { booking -> booking.seats.split(",").map { it.trim() } }

        (seatsFromTickets + seatsFromBookings)
            .filter { it.isNotEmpty() }
            .toSet()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )


    // Số lượng combo bắp nước
    private val _comboCount = MutableStateFlow(0) // 1 Combo = 1 Bắp + 1 Nước (75,000đ)
    val comboCount: StateFlow<Int> = _comboCount.asStateFlow()

    // Số lượng combo được đổi bằng điểm tích lũy (mỗi combo = 100 điểm)
    private val _redeemedComboCount = MutableStateFlow(0)
    val redeemedComboCount: StateFlow<Int> = _redeemedComboCount.asStateFlow()

    // Mã khuyến mãi
    private val _promoDiscount = MutableStateFlow(0) // Số tiền giảm
    val promoDiscount: StateFlow<Int> = _promoDiscount.asStateFlow()

    private val _appliedPromoCode = MutableStateFlow<PromoCode?>(null)
    val appliedPromoCode: StateFlow<PromoCode?> = _appliedPromoCode.asStateFlow()

    // Kiểm tra xem mã giảm giá đã dùng chưa
    private val _isPromoCodeUsed = MutableStateFlow(false)
    val isPromoCodeUsed: StateFlow<Boolean> = _isPromoCodeUsed.asStateFlow()

    // Số dư ví giả định của Momo
    val momoBalance get() = _userBalance.value // Liên kết ví Momo của app cũ với Neon Pay mới

    // Thông báo tin nhắn từng tài khoản
    private val _userNotifications = MutableStateFlow<List<UserNotification>>(emptyList())
    val userNotifications: StateFlow<List<UserNotification>> = _userNotifications.asStateFlow()

    init {
        // Tắt splash screen sau 2.2 giây
        viewModelScope.launch {
            kotlinx.coroutines.delay(2200)
            _showSplashScreen.value = false
        }

        // Tải thông tin tài khoản đã đăng nhập từ bộ nhớ tạm local
        _isLoggedIn.value = prefs.getBoolean("is_logged_in", false)
        _userEmail.value = prefs.getString("user_email", "") ?: ""
        _userName.value = prefs.getString("user_name", "") ?: ""
        _userPoints.value = prefs.getInt("user_points", 150)
        _userBalance.value = prefs.getInt("user_balance", 500000)
        _isPromoCodeUsed.value = false

        if (_userEmail.value.isNotBlank()) {
            loadNotificationsForUser(_userEmail.value)
        }

        // Khởi tạo đánh giá mặc định
        viewModelScope.launch {
            repository.initDefaultDataIfNeeded()
        }

        // Đồng bộ vé đã đặt và mã giảm giá từ Supabase về Room Local khi mở app
        viewModelScope.launch {
            try {
                repository.syncTicketsFromSupabase()
                repository.syncPromoCodesFromSupabase()
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "Lỗi đồng bộ dữ liệu khi khởi tạo: ${e.message}")
            }
        }

        // Chọn ngày mặc định là hôm nay
        val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
        _selectedDate.value = sdf.format(Date())

        // Đồng bộ phim, suất chiếu & chỗ ngồi đã đặt từ Supabase
        loadMoviesFromSupabase()
        loadShowtimesAndBookingsFromSupabase()
    }

    fun loadShowtimesAndBookingsFromSupabase() {
        com.example.data.supabase.SupabaseSyncService.fetchShowtimesFromSupabase { list ->
            if (list.isNotEmpty()) {
                _showtimes.value = list
            }
        }
        com.example.data.supabase.SupabaseSyncService.fetchBookingsFromSupabase { list ->
            if (list.isNotEmpty()) {
                _bookings.value = list
            }
        }
    }

    fun loadMoviesFromSupabase() {
        com.example.data.supabase.SupabaseSyncService.fetchMoviesFromSupabase { remoteMovies ->
            if (!remoteMovies.isNullOrEmpty()) {
                _movies.value = remoteMovies
                // Cập nhật selectedMovie nếu người dùng đang chủ động xem chi tiết một bộ phim
                val currentSelected = _selectedMovie.value
                if (currentSelected != null) {
                    val updatedMovie = remoteMovies.find { it.id == currentSelected.id }
                    if (updatedMovie != null) {
                        _selectedMovie.value = updatedMovie
                    }
                }
            } else if (_movies.value.isEmpty()) {
                _movies.value = repository.movies
            }
        }
    }

    fun refreshDataFromSupabase() {
        // Đồng bộ danh sách phim, suất chiếu & chỗ ngồi từ Supabase
        loadMoviesFromSupabase()
        loadShowtimesAndBookingsFromSupabase()


        viewModelScope.launch {
            try {
                repository.syncTicketsFromSupabase()
                repository.syncPromoCodesFromSupabase()
                _selectedMovie.value?.id?.let { movieId ->
                    // Kích hoạt đồng bộ đánh giá cho phim đang xem
                    repository.getReviewsForMovie(movieId)
                }
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "Lỗi đồng bộ thủ công: ${e.message}")
            }
        }
    }

    fun selectTab(tab: Int) {
        _currentTab.value = tab
    }

    // --- Thao tác Đăng Nhập & Đăng Ký ---
    fun login(email: String, name: String) {
        _isLoggedIn.value = true
        _userEmail.value = email
        val defaultName = if (name.isNotBlank()) name else email.substringBefore("@")
        _userName.value = defaultName
        _userPoints.value = 150
        _userBalance.value = 500000
        _isPromoCodeUsed.value = false

        // Lưu tạm local ngay lập tức
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_email", email)
            .putString("user_name", defaultName)
            .putInt("user_points", 150)
            .putInt("user_balance", 500000)
            .apply()

        loadNotificationsForUser(email)

        // Đồng bộ nâng cao từ Supabase profiles table
        com.example.data.supabase.SupabaseSyncService.fetchProfileFromSupabase(email) { remoteProfile ->
            if (remoteProfile != null) {
                _userName.value = remoteProfile.name
                _userPoints.value = remoteProfile.points
                _userBalance.value = remoteProfile.balance

                prefs.edit()
                    .putString("user_name", remoteProfile.name)
                    .putInt("user_points", remoteProfile.points)
                    .putInt("user_balance", remoteProfile.balance)
                    .apply()
            } else {
                // Nếu chưa có profile trên Supabase, tạo mới để lưu giữ thông tin
                val profile = UserProfile(email = email, name = defaultName, points = 150, balance = 500000)
                com.example.data.supabase.SupabaseSyncService.pushProfileToSupabase(profile) { }
            }
        }
    }

    fun register(name: String, email: String) {
        _isLoggedIn.value = true
        _userName.value = name
        _userEmail.value = email
        _userPoints.value = 250 // Tặng 250 điểm chào mừng
        _userBalance.value = 500000 // Ví ban đầu
        _isPromoCodeUsed.value = false

        // Lưu local
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_email", email)
            .putString("user_name", name)
            .putInt("user_points", 250)
            .putInt("user_balance", 500000)
            .apply()

        loadNotificationsForUser(email)
        addNotification("🎉 Đăng ký thành công", "Chào mừng $name đến với ứng dụng vé xem phim Neon Cine!", "system")

        // Đồng bộ lên Supabase profiles table
        val profile = UserProfile(email = email, name = name, points = 250, balance = 500000)
        com.example.data.supabase.SupabaseSyncService.pushProfileToSupabase(profile) { }
    }

    fun logout() {
        _isLoggedIn.value = false
        _userEmail.value = ""
        _userName.value = ""
        _userPoints.value = 150
        _userBalance.value = 500000
        _isPromoCodeUsed.value = false
        _userNotifications.value = emptyList()
        prefs.edit().clear().apply()
    }

    // --- Quản lý Thông báo tin nhắn từng Tài khoản (Acc) ---
    fun loadNotificationsForUser(email: String) {
        if (email.isBlank()) {
            _userNotifications.value = emptyList()
            return
        }
        val rawStr = prefs.getString("notifications_$email", null)
        if (rawStr.isNullOrBlank()) {
            val sdf = SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault())
            val now = sdf.format(Date())
            val defaultList = listOf(
                UserNotification(
                    title = "🎉 Chào mừng đến với Neon Cine",
                    message = "Tài khoản $email đã sẵn sàng. Bạn có thể đặt vé phim và thưởng thức bắp nước hấp dẫn ngay hôm nay!",
                    timestamp = now,
                    type = "system"
                ),
                UserNotification(
                    title = "🎟️ Mã giảm giá vé cực hot",
                    message = "Sử dụng mã HE2026 hoặc NEON30 tại màn hình thanh toán để nhận ngay ưu đãi giảm giá vé xem phim.",
                    timestamp = "09:00 - Hôm nay",
                    type = "promo"
                ),
                UserNotification(
                    title = "🍿 Combo Bắp Nước Ưu Đãi",
                    message = "Đặt kèm Combo Bắp lớn + Nước ngọt mát lạnh khi mua vé để thưởng thức trọn vẹn bộ phim.",
                    timestamp = "08:30 - Hôm nay",
                    type = "promo"
                )
            )
            saveNotificationsForUser(email, defaultList)
            _userNotifications.value = defaultList
        } else {
            _userNotifications.value = parseNotifications(rawStr)
        }
    }

    private fun saveNotificationsForUser(email: String, list: List<UserNotification>) {
        if (email.isBlank()) return
        val encoded = serializeNotifications(list)
        prefs.edit().putString("notifications_$email", encoded).apply()
    }

    fun deleteNotification(notificationId: String) {
        val email = _userEmail.value
        val updated = _userNotifications.value.filter { it.id != notificationId }
        _userNotifications.value = updated
        saveNotificationsForUser(email, updated)
    }

    fun clearAllNotifications() {
        val email = _userEmail.value
        _userNotifications.value = emptyList()
        saveNotificationsForUser(email, emptyList())
    }

    fun addNotification(title: String, message: String, type: String = "system") {
        val email = _userEmail.value
        if (email.isBlank()) return
        val sdf = SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault())
        val newNotif = UserNotification(
            title = title,
            message = message,
            timestamp = sdf.format(Date()),
            type = type
        )
        val updated = listOf(newNotif) + _userNotifications.value
        _userNotifications.value = updated
        saveNotificationsForUser(email, updated)
    }

    private fun serializeNotifications(list: List<UserNotification>): String {
        val sb = StringBuilder()
        for (item in list) {
            val cleanTitle = item.title.replace("||", " ").replace("\n", " ")
            val cleanMsg = item.message.replace("||", " ").replace("\n", " ")
            val cleanTime = item.timestamp.replace("||", " ").replace("\n", " ")
            sb.append("${item.id}||${cleanTitle}||${cleanMsg}||${cleanTime}||${item.type}\n")
        }
        return sb.toString()
    }

    private fun parseNotifications(str: String): List<UserNotification> {
        val result = mutableListOf<UserNotification>()
        val lines = str.split("\n")
        for (line in lines) {
            if (line.isBlank()) continue
            val parts = line.split("||")
            if (parts.size >= 5) {
                result.add(
                    UserNotification(
                        id = parts[0],
                        title = parts[1],
                        message = parts[2],
                        timestamp = parts[3],
                        type = parts[4]
                    )
                )
            }
        }
        return result
    }

    fun selectMovie(movie: Movie?) {
        _selectedMovie.value = movie
        _aiAnalysisState.value = AIAnalysisState.Idle // Reset trạng thái AI
        _selectedSeats.value = emptySet() // Reset ghế chọn
        _comboCount.value = 0 // Reset combo
    }

    fun startBookingFlow() {
        if (!_isLoggedIn.value) {
            _showLoginRequiredDialog.value = true
            return
        }
        _isBookingFlowActive.value = true
        // Đồng bộ vé mới nhất từ Supabase khi mở luồng đặt vé
        viewModelScope.launch {
            try {
                repository.syncTicketsFromSupabase()
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "Lỗi đồng bộ vé khi mở luồng đặt: ${e.message}")
            }
        }
    }

    fun stopBookingFlow() {
        _isBookingFlowActive.value = false
    }

    fun selectCinema(cinema: String) {
        _selectedCinema.value = cinema
        _selectedSeats.value = emptySet() // Reset selected seats when cinema changes
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
        _selectedSeats.value = emptySet() // Reset selected seats when date changes
    }

    fun selectTime(time: String) {
        _selectedTime.value = time
        _selectedSeats.value = emptySet() // Reset selected seats when time changes
    }

    fun toggleSeat(seat: String) {
        if (bookedSeats.value.contains(seat)) return // Ngăn chọn ghế đã được đặt trước đó
        val current = _selectedSeats.value.toMutableSet()
        if (current.contains(seat)) {
            current.remove(seat)
        } else {
            current.add(seat)
        }
        _selectedSeats.value = current
    }

    fun incrementCombo() {
        _comboCount.value += 1
    }

    fun decrementCombo() {
        if (_comboCount.value > 0) {
            _comboCount.value -= 1
            if (_redeemedComboCount.value > _comboCount.value) {
                _redeemedComboCount.value = _comboCount.value
            }
        }
    }

    fun incrementRedeemedCombo() {
        val maxRedeemableByPoints = _userPoints.value / 100
        val maxPossible = minOf(_comboCount.value, maxRedeemableByPoints)
        if (_redeemedComboCount.value < maxPossible) {
            _redeemedComboCount.value += 1
        }
    }

    fun decrementRedeemedCombo() {
        if (_redeemedComboCount.value > 0) {
            _redeemedComboCount.value -= 1
        }
    }

    fun dailyCheckIn(onResult: (String) -> Unit) {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val lastCheckIn = prefs.getString("last_check_in_date", "")
        if (lastCheckIn == today) {
            onResult("Bạn đã điểm danh hôm nay rồi! Hãy quay lại vào ngày mai.")
            return
        }
        _userPoints.value += 20
        prefs.edit()
            .putString("last_check_in_date", today)
            .putInt("user_points", _userPoints.value)
            .apply()
        
        val profile = UserProfile(email = _userEmail.value, name = _userName.value, points = _userPoints.value, balance = _userBalance.value)
        com.example.data.supabase.SupabaseSyncService.pushProfileToSupabase(profile) {}
        addNotification("💎 Điểm danh thành công", "Bạn đã nhận được +20 Neon Points điểm danh ngày hôm nay!", "system")
        onResult("Điểm danh thành công! Nhận ngay +20 Neon Points 💎")
    }

    fun redeemComboFromProfile(onResult: (String?) -> Unit) {
        if (_userPoints.value < 100) {
            onResult(null)
            return
        }
        _userPoints.value -= 100
        prefs.edit()
            .putInt("user_points", _userPoints.value)
            .apply()
        
        val profile = UserProfile(email = _userEmail.value, name = _userName.value, points = _userPoints.value, balance = _userBalance.value)
        com.example.data.supabase.SupabaseSyncService.pushProfileToSupabase(profile) {}
        
        val voucherCode = "REDEEM-" + UUID.randomUUID().toString().substring(0, 8).uppercase(Locale.getDefault())
        addNotification("🍿 Đổi bắp nước thành công", "Mã voucher đổi Combo Bắp Nước của bạn: $voucherCode. Đưa mã này cho nhân viên tại quầy.", "promo")
        onResult(voucherCode)
    }

    fun checkAndApplyPromoCode(code: String, onResult: (String?) -> Unit) {
        val trimmedCode = code.uppercase().trim()
        val currentUserEmail = _userEmail.value
        viewModelScope.launch {
            val promo = repository.getPromoCode(trimmedCode)
            if (promo != null) {
                val alreadyUsed = prefs.getBoolean("promo_used_${currentUserEmail}_${trimmedCode}", false) ||
                        tickets.value.any { it.userEmail == currentUserEmail && it.promoCode == trimmedCode }
                if (alreadyUsed) {
                    onResult("Tài khoản của bạn đã sử dụng mã này rồi")
                } else {
                    _promoDiscount.value = promo.discountAmount
                    _appliedPromoCode.value = promo
                    onResult(null) // Thành công
                }
            } else {
                onResult("Mã giảm giá không hợp lệ hoặc không tồn tại")
            }
        }
    }

    fun removePromoCode() {
        _promoDiscount.value = 0
        _appliedPromoCode.value = null
    }

    fun topUpWallet(amount: Int) {
        _userBalance.value += amount
        prefs.edit().putInt("user_balance", _userBalance.value).apply()
        addNotification("💵 Nạp tiền thành công", "Bạn đã nạp +${formatCurrency(amount)} vào ví Neon Pay. Số dư mới: ${formatCurrency(_userBalance.value)}", "system")
        val profile = UserProfile(email = _userEmail.value, name = _userName.value, points = _userPoints.value, balance = _userBalance.value)
        com.example.data.supabase.SupabaseSyncService.pushProfileToSupabase(profile) { }
    }

    fun getMovieBasePrice(movie: Movie? = _selectedMovie.value): Int {
        if (movie == null) return 95000
        val showtime = _showtimes.value.find { it.movieId == movie.id }
        if (showtime != null && showtime.price > 0) return showtime.price
        if (movie.price > 0) return movie.price
        return 95000
    }

    fun getSeatPrice(seat: String): Int {
        val basePrice = getMovieBasePrice()
        val row = seat.take(1).uppercase(Locale.getDefault())
        return when (row) {
            "E", "F", "G" -> basePrice + 20000 // VIP
            "H" -> basePrice + 55000            // Ghế đôi
            else -> basePrice                  // Ghế thường (A, B, C, D)
        }
    }


    fun calculateSeatsTotal(): Int {
        return _selectedSeats.value.sumOf { getSeatPrice(it) }
    }

    fun calculateTotalPrice(): Int {
        val comboPrice = 75000 // Giá combo bắp nước chuẩn 75.000đ
        val seatsCost = calculateSeatsTotal()
        val comboCost = _comboCount.value * comboPrice
        val total = seatsCost + comboCost - _promoDiscount.value
        return if (total < 0) 0 else total
    }

    fun formatCurrency(amount: Int): String {
        val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        return format.format(amount).replace("₫", "đ")
    }

    // Đăng bài đánh giá mới
    fun submitReview(author: String, rating: Int, content: String) {
        val movie = _selectedMovie.value ?: return
        if (author.isBlank() || content.isBlank()) return

        viewModelScope.launch {
            val newReview = Review(
                movieId = movie.id,
                author = author,
                rating = rating,
                content = content
            )
            repository.addReview(newReview)
        }
    }

    // Phân tích đánh giá bằng AI
    fun analyzeReviewsWithAI() {
        val movie = _selectedMovie.value ?: return
        val reviews = currentMovieReviews.value
        
        _aiAnalysisState.value = AIAnalysisState.Loading

        viewModelScope.launch {
            try {
                val analysis = repository.analyzeReviewsWithAI(movie.id, movie.title, reviews)
                _aiAnalysisState.value = AIAnalysisState.Success(analysis)
            } catch (e: Exception) {
                _aiAnalysisState.value = AIAnalysisState.Error(e.message ?: "Lỗi kết nối AI")
            }
        }
    }

    // Thanh toán và ghi nhận vé
    fun purchaseTicket(onSuccess: () -> Unit) {
        val movie = _selectedMovie.value ?: return
        val seatsStr = _selectedSeats.value.joinToString(", ")
        if (seatsStr.isBlank()) return

        val cost = calculateTotalPrice()
        val promoToMarkUsed = _appliedPromoCode.value
        if (promoToMarkUsed != null) {
            _isPromoCodeUsed.value = false
            prefs.edit().putBoolean("promo_used_${_userEmail.value}_${promoToMarkUsed.code}", true).apply()
        }

        // Khấu trừ điểm tích lũy dùng để đổi combo bắp nước (100 điểm / 1 combo)
        val pointsToDeduct = _redeemedComboCount.value * 100
        _userPoints.value = maxOf(0, _userPoints.value - pointsToDeduct)
        
        // Cộng điểm thưởng Neon Club khi đặt vé thành công (+50 điểm)
        _userPoints.value += 50

        // Lưu local
        prefs.edit()
            .putInt("user_points", _userPoints.value)
            .apply()

        // Đồng bộ điểm mới lên Supabase table profiles
        val profile = UserProfile(email = _userEmail.value, name = _userName.value, points = _userPoints.value, balance = _userBalance.value)
        com.example.data.supabase.SupabaseSyncService.pushProfileToSupabase(profile) { }

        // Mô tả bắp nước
        val comboStr = if (_comboCount.value > 0) "${_comboCount.value} Combo (Bắp + Nước)" else "Không có"
        val barcode = "NEON-" + UUID.randomUUID().toString().substring(0, 8).uppercase(Locale.getDefault())

        val ticket = Ticket(
            movieId = movie.id,
            movieTitle = movie.title,
            moviePoster = movie.posterUrl,
            cinema = _selectedCinema.value,
            dateTime = "${_selectedDate.value} lúc ${_selectedTime.value}",
            seats = seatsStr,
            totalPrice = cost,
            combo = comboStr,
            barcode = barcode,
            userEmail = _userEmail.value,
            userName = _userName.value,
            promoCode = promoToMarkUsed?.code ?: ""
        )

        viewModelScope.launch {
            repository.bookTicket(ticket)
            addNotification(
                title = "🎟️ Đặt vé thành công",
                message = "Vé phim ${movie.title} tại ${_selectedCinema.value}, suất ${_selectedDate.value} lúc ${_selectedTime.value}. Ghế chọn: $seatsStr. Mã vé: $barcode",
                type = "booking"
            )
            onSuccess()
            // Reset trạng thái đặt vé
            _selectedSeats.value = emptySet()
            _comboCount.value = 0
            _redeemedComboCount.value = 0
            _promoDiscount.value = 0
            _appliedPromoCode.value = null
            _isBookingFlowActive.value = false
            _selectedMovie.value = null
            _currentTab.value = 3 // Chuyển sang tab "Vé của tôi" (tab số 3) để xem vé vừa đặt!
        }
    }

    fun setMovieShowingDates(movieId: Int, startDate: String?, endDate: String?) {
        prefs.edit().apply {
            if (startDate != null) putString("movie_${movieId}_start_date", startDate)
            else remove("movie_${movieId}_start_date")
            
            if (endDate != null) putString("movie_${movieId}_end_date", endDate)
            else remove("movie_${movieId}_end_date")
        }.apply()
        
        // Trigger flow update
        _customMovieDatesTrigger.value += 1
    }
    
    fun getMovieCustomStartDate(movieId: Int): String {
        return prefs.getString("movie_${movieId}_start_date", "") ?: ""
    }
    
    fun getMovieCustomEndDate(movieId: Int): String {
        return prefs.getString("movie_${movieId}_end_date", "") ?: ""
    }
}
