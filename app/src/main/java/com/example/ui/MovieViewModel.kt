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

    // Danh sách phim kết nối trực tiếp từ DB / Supabase
    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    val movies: StateFlow<List<Movie>> = _movies.asStateFlow()
    val rawMoviesList: StateFlow<List<Movie>> = _movies.asStateFlow()

    // Lịch sử vé đã đặt từ DB
    val tickets: StateFlow<List<Ticket>> = repository.allTickets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Danh sách mã giảm giá từ DB
    val promoCodes: StateFlow<List<PromoCode>> = repository.allPromoCodes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Phim đang chọn xem chi tiết
    private val _selectedMovie = MutableStateFlow<Movie?>(null)
    val selectedMovie: StateFlow<Movie?> = _selectedMovie.asStateFlow()

    // Đánh giá của bộ phim lấy từ DB
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

    // Trạng thái AI
    private val _aiAnalysisState = MutableStateFlow<AIAnalysisState>(AIAnalysisState.Idle)
    val aiAnalysisState: StateFlow<AIAnalysisState> = _aiAnalysisState.asStateFlow()

    // Tab chính
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Splash Screen
    private val _showSplashScreen = MutableStateFlow(true)
    val showSplashScreen: StateFlow<Boolean> = _showSplashScreen.asStateFlow()

    // Vị trí người dùng
    private val _userLocationAddress = MutableStateFlow("Quận 1, TP. Hồ Chí Minh")
    val userLocationAddress: StateFlow<String> = _userLocationAddress.asStateFlow()

    fun updateUserLocation(address: String) {
        _userLocationAddress.value = address
    }

    // Đăng Nhập & Tài Khoản
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

    private val _userPoints = MutableStateFlow(0)
    val userPoints: StateFlow<Int> = _userPoints.asStateFlow()

    private val _userBalance = MutableStateFlow(0)
    val userBalance: StateFlow<Int> = _userBalance.asStateFlow()

    // Luồng Đặt Vé
    private val _isBookingFlowActive = MutableStateFlow(false)
    val isBookingFlowActive: StateFlow<Boolean> = _isBookingFlowActive.asStateFlow()

    private val _cinemaName = MutableStateFlow("Neon Cine Space")
    val cinemaName: StateFlow<String> = _cinemaName.asStateFlow()

    private val _cinemaAddress = MutableStateFlow("209 Đường 30 Tháng 4, Xuân Khánh, Ninh Kiều, Cần Thơ")
    val cinemaAddress: StateFlow<String> = _cinemaAddress.asStateFlow()

    private val _selectedCinema = MutableStateFlow("Neon Cine Space")
    val selectedCinema: StateFlow<String> = _selectedCinema.asStateFlow()

    private val _selectedDate = MutableStateFlow("")
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedTime = MutableStateFlow("19:00")
    val selectedTime: StateFlow<String> = _selectedTime.asStateFlow()

    private val _selectedSeats = MutableStateFlow<Set<String>>(emptySet())
    val selectedSeats: StateFlow<Set<String>> = _selectedSeats.asStateFlow()

    val bookedSeats: StateFlow<Set<String>> = combine(
        _selectedMovie,
        _selectedCinema,
        _selectedDate,
        _selectedTime,
        tickets
    ) { movie, cinema, date, time, allTickets ->
        if (movie == null) return@combine emptySet<String>()
        val dateTimeTarget = "$date lúc $time"
        allTickets
            .filter { ticket ->
                ticket.movieId == movie.id &&
                        ticket.cinema == cinema &&
                        ticket.dateTime == dateTimeTarget
            }
            .flatMap { ticket ->
                ticket.seats.split(",").map { it.trim() }
            }
            .filter { it.isNotEmpty() }
            .toSet()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    private val _comboCount = MutableStateFlow(0)
    val comboCount: StateFlow<Int> = _comboCount.asStateFlow()

    private val _redeemedComboCount = MutableStateFlow(0)
    val redeemedComboCount: StateFlow<Int> = _redeemedComboCount.asStateFlow()

    private val _promoDiscount = MutableStateFlow(0)
    val promoDiscount: StateFlow<Int> = _promoDiscount.asStateFlow()

    private val _appliedPromoCode = MutableStateFlow<PromoCode?>(null)
    val appliedPromoCode: StateFlow<PromoCode?> = _appliedPromoCode.asStateFlow()

    private val _isPromoCodeUsed = MutableStateFlow(false)
    val isPromoCodeUsed: StateFlow<Boolean> = _isPromoCodeUsed.asStateFlow()

    val momoBalance get() = _userBalance.value

    private val _userNotifications = MutableStateFlow<List<UserNotification>>(emptyList())
    val userNotifications: StateFlow<List<UserNotification>> = _userNotifications.asStateFlow()

    init {
        viewModelScope.launch {
            kotlinx.coroutines.delay(2200)
            _showSplashScreen.value = false
        }

        _isLoggedIn.value = prefs.getBoolean("is_logged_in", false)
        _userEmail.value = prefs.getString("user_email", "") ?: ""
        _userName.value = prefs.getString("user_name", "") ?: ""
        _userPoints.value = prefs.getInt("user_points", 0)
        _userBalance.value = prefs.getInt("user_balance", 0)
        _isPromoCodeUsed.value = false

        if (_userEmail.value.isNotBlank()) {
            loadNotificationsForUser(_userEmail.value)
        }

        // Đồng bộ dữ liệu từ Database / Supabase khi khởi tạo
        viewModelScope.launch {
            try {
                repository.syncTicketsFromSupabase()
                repository.syncPromoCodesFromSupabase()
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "Lỗi đồng bộ dữ liệu khi khởi tạo: ${e.message}")
            }
        }

        val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
        _selectedDate.value = sdf.format(Date())

        loadMoviesFromSupabase()
    }

    fun loadMoviesFromSupabase() {
        com.example.data.supabase.SupabaseSyncService.fetchMoviesFromSupabase { remoteMovies ->
            if (!remoteMovies.isNullOrEmpty()) {
                _movies.value = remoteMovies
                val currentSelected = _selectedMovie.value
                if (currentSelected != null) {
                    val updatedMovie = remoteMovies.find { it.id == currentSelected.id }
                    if (updatedMovie != null) {
                        _selectedMovie.value = updatedMovie
                    }
                }
            }
        }
    }

    fun refreshDataFromSupabase() {
        loadMoviesFromSupabase()

        viewModelScope.launch {
            try {
                repository.syncTicketsFromSupabase()
                repository.syncPromoCodesFromSupabase()
                _selectedMovie.value?.id?.let { movieId ->
                    repository.getReviewsForMovie(movieId)
                }
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "Lỗi đồng bộ: ${e.message}")
            }
        }
    }

    fun selectTab(tab: Int) {
        _currentTab.value = tab
    }

    fun login(email: String, name: String) {
        _isLoggedIn.value = true
        _userEmail.value = email
        val defaultName = if (name.isNotBlank()) name else email.substringBefore("@")
        _userName.value = defaultName

        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_email", email)
            .putString("user_name", defaultName)
            .apply()

        loadNotificationsForUser(email)

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
                val profile = UserProfile(email = email, name = defaultName, points = 0, balance = 0)
                com.example.data.supabase.SupabaseSyncService.pushProfileToSupabase(profile) { }
            }
        }
    }

    fun register(name: String, email: String) {
        _isLoggedIn.value = true
        _userName.value = name
        _userEmail.value = email
        _userPoints.value = 0
        _userBalance.value = 0

        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_email", email)
            .putString("user_name", name)
            .putInt("user_points", 0)
            .putInt("user_balance", 0)
            .apply()

        loadNotificationsForUser(email)

        val profile = UserProfile(email = email, name = name, points = 0, balance = 0)
        com.example.data.supabase.SupabaseSyncService.pushProfileToSupabase(profile) { }
    }

    fun logout() {
        _isLoggedIn.value = false
        _userEmail.value = ""
        _userName.value = ""
        _userPoints.value = 0
        _userBalance.value = 0
        _isPromoCodeUsed.value = false
        _userNotifications.value = emptyList()
        prefs.edit().clear().apply()
    }

    // Quản lý thông báo người dùng (Đã xóa danh sách mẫu tĩnh)
    fun loadNotificationsForUser(email: String) {
        if (email.isBlank()) {
            _userNotifications.value = emptyList()
            return
        }
        val rawStr = prefs.getString("notifications_$email", null)
        if (rawStr.isNullOrBlank()) {
            _userNotifications.value = emptyList()
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
        _aiAnalysisState.value = AIAnalysisState.Idle
        _selectedSeats.value = emptySet()
        _comboCount.value = 0
    }

    fun startBookingFlow() {
        if (!_isLoggedIn.value) {
            _showLoginRequiredDialog.value = true
            return
        }
        _isBookingFlowActive.value = true
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
        _selectedSeats.value = emptySet()
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
        _selectedSeats.value = emptySet()
    }

    fun selectTime(time: String) {
        _selectedTime.value = time
        _selectedSeats.value = emptySet()
    }

    fun toggleSeat(seat: String) {
        if (bookedSeats.value.contains(seat)) return
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
                    onResult(null)
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

    fun calculateTotalPrice(): Int {
        val ticketPrice = 95000
        val comboPrice = 75000

        val seatsCost = _selectedSeats.value.size * ticketPrice
        val nonRedeemedCombos = maxOf(0, _comboCount.value - _redeemedComboCount.value)
        val comboCost = nonRedeemedCombos * comboPrice
        val total = seatsCost + comboCost - _promoDiscount.value
        return if (total < 0) 0 else total
    }

    fun formatCurrency(amount: Int): String {
        val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        return format.format(amount).replace("₫", "đ")
    }

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

        val pointsToDeduct = _redeemedComboCount.value * 100
        _userPoints.value = maxOf(0, _userPoints.value - pointsToDeduct)

        prefs.edit()
            .putInt("user_points", _userPoints.value)
            .apply()

        val profile = UserProfile(email = _userEmail.value, name = _userName.value, points = _userPoints.value, balance = _userBalance.value)
        com.example.data.supabase.SupabaseSyncService.pushProfileToSupabase(profile) { }

        val comboStr = if (_comboCount.value > 0) {
            if (_redeemedComboCount.value > 0) {
                "${_comboCount.value} Combo (${_redeemedComboCount.value} miễn phí bằng điểm)"
            } else {
                "${_comboCount.value} Combo (Bắp + Nước)"
            }
        } else {
            "Không có"
        }
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
            _selectedSeats.value = emptySet()
            _comboCount.value = 0
            _redeemedComboCount.value = 0
            _promoDiscount.value = 0
            _appliedPromoCode.value = null
            _isBookingFlowActive.value = false
            _selectedMovie.value = null
            _currentTab.value = 2
        }
    }
}