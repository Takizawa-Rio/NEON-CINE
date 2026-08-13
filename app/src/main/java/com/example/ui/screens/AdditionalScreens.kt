package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.MovieViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 1. Splash Screen
 * Hiển thị mở đầu đầy phong cách với logo Neon Cinema và hiệu ứng tải động.
 */
@Composable
fun SplashScreen() {
    var startAnimation by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1.1f else 0.8f,
        animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
        label = "Logo scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = EaseInOut),
        label = "Logo alpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E15)) // Tông màu tối đặc trưng cyberpunk
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Gradient nền trang trí phía sau
        Box(
            modifier = Modifier
                .size(400.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonPrimary.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Biểu tượng Cinema phát sáng lấp lánh
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(NeonPrimary, NeonSecondary)
                        )
                    )
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF141522)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MovieFilter,
                        contentDescription = "Neon Logo",
                        tint = NeonPrimary,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand name phát sáng
            Text(
                text = "NEON CINE",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Khẩu hiệu
            Text(
                text = "TRẢI NGHIỆM ĐIỆN ẢNH ĐƠN ĐỘC BẢN",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = NeonSecondary,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Hiệu ứng thanh tiến trình lấp lánh cực đẹp
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "Loading")
                val offset by infiniteTransition.animateFloat(
                    initialValue = -1f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "Indicator translation"
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.3f)
                        .align(Alignment.CenterStart)
                        .offset(x = 180.dp * offset)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(NeonPrimary, NeonTertiary)
                            )
                        )
                )
            }
        }
    }
}

/**
 * 2. Lịch Chiếu Screen (Showtimes)
 * Cho phép xem chi tiết tất cả các suất chiếu tại rạp Neon Cine Space, lọc theo ngày và chọn nhanh để đặt vé.
 */
@Composable
fun ShowtimesScreen(viewModel: MovieViewModel) {
    val movies by viewModel.movies.collectAsStateWithLifecycle()
    val cinemaName by viewModel.cinemaName.collectAsStateWithLifecycle()
    val cinemaAddress by viewModel.cinemaAddress.collectAsStateWithLifecycle()
    val cinemaMapQuery by viewModel.cinemaMapQuery.collectAsStateWithLifecycle()

    val dates = remember {
        val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
        val cal = Calendar.getInstance()
        (0..4).map {
            val dateStr = sdf.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            dateStr
        }
    }
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("showtimes_screen")
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(NeonPrimary.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "Lịch Chiếu Phim 📅",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Cập nhật suất chiếu mới nhất tại rạp $cinemaName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Thông tin địa chỉ rạp & Chỉ đường bản đồ
        item {
            val context = androidx.compose.ui.platform.LocalContext.current
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MomoPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = "Địa chỉ rạp",
                            tint = MomoPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cinemaName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cinemaAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            try {
                                val encodedQuery = java.net.URLEncoder.encode(cinemaMapQuery, "UTF-8")
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedQuery")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Không thể mở ứng dụng bản đồ!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MomoPrimary)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Navigation,
                            contentDescription = "Chỉ đường",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Thanh chọn ngày dạng lướt ngang (Horizontal Date Picker)
        item {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = "Chọn ngày chiếu",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(dates) { date ->
                        val isSelected = date == selectedDate
                        val displayDay = if (date == dates[0]) "Hôm nay" else "Ngày ${date.split("/")[0]}"
                        
                        Card(
                            onClick = { viewModel.selectDate(date) },
                            modifier = Modifier
                                .width(90.dp)
                                .height(60.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) NeonPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = displayDay,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = date,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Divider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Danh sách phim đang chiếu cùng suất chiếu tương ứng
        if (movies.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Hiện chưa có lịch chiếu cho ngày đã chọn.", color = Color.Gray)
                }
            }
        } else {
            items(movies) { movie ->
                ShowtimeMovieCard(
                    movie = movie,
                    selectedDate = selectedDate,
                    viewModel = viewModel,
                    onShowtimeSelected = { time ->
                        // Thiết lập bộ lọc đặt vé trực tiếp và kích hoạt luồng đặt vé
                        viewModel.selectMovie(movie)
                        viewModel.selectDate(selectedDate)
                        viewModel.selectTime(time)
                        viewModel.startBookingFlow()
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun ShowtimeMovieCard(
    movie: com.example.data.model.Movie,
    selectedDate: String,
    viewModel: MovieViewModel? = null,
    onShowtimeSelected: (String) -> Unit
) {
    val dbShowtimes = viewModel?.showtimes?.collectAsStateWithLifecycle()?.value
    val showtimes = remember(movie.id, dbShowtimes) {
        val listFromDb = dbShowtimes?.filter { it.movieId == movie.id }?.map { it.startTime }?.filter { it.isNotBlank() }
        if (!listFromDb.isNullOrEmpty()) {
            listFromDb.distinct()
        } else {
            listOf("09:30", "12:15", "15:00", "17:45", "19:30", "21:15", "23:00")
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Poster
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 64.dp, height = 90.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Metadata
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = movie.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Thời lượng: ${movie.duration} • Thể loại: ${movie.genre}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = movie.rating.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Chọn suất chiếu tối nay (2D Phụ đề Tiếng Việt):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = NeonSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Khung lưới các suất chiếu ngang
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 4.dp)
            ) {
                items(showtimes) { time ->
                    Button(
                        onClick = { onShowtimeSelected(time) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonSecondary.copy(alpha = 0.12f),
                            contentColor = NeonSecondary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, NeonSecondary.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = time,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. Hồ Sơ Tôi (Profile, Login, Register)
 * Quản lý trạng thái đăng nhập, đăng ký, tích hợp thẻ thành viên Neon Club, nạp Neon Pay và xem thông tin cá nhân.
 */
@Composable
fun ProfileScreen(viewModel: MovieViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("profile_screen")
    ) {
        if (isLoggedIn) {
            LoggedInProfileView(viewModel = viewModel)
        } else {
            AuthFormView(viewModel = viewModel)
        }
    }
}

@Composable
fun LoggedInProfileView(viewModel: MovieViewModel) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val userBalance by viewModel.userBalance.collectAsStateWithLifecycle()
    val userNotifications by viewModel.userNotifications.collectAsStateWithLifecycle()

    var showNotificationDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Avatar & Tên người dùng
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(NeonPrimary, NeonSecondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(1).uppercase(Locale.getDefault()),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = userName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = userEmail,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(20.dp))
        }



        // Menu tùy chọn
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Rounded.Notifications,
                        title = "Hộp Thư Tin Nhắn (${userNotifications.size})",
                        subtitle = "Xem tin nhắn hệ thống, vé đã đặt & xóa tin nhắn",
                        onClick = { showNotificationDialog = true }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileMenuRow(
                        icon = Icons.Rounded.History,
                        title = "Lịch Sử Đặt Vé",
                        subtitle = "Chi tiết các vé xem phim đã thanh toán của bạn",
                        onClick = { viewModel.selectTab(3) } // Di chuyển đến tab vé
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Đăng xuất
            Button(
                onClick = { viewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
            ) {
                Icon(Icons.Default.Logout, contentDescription = "Log out")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Đăng xuất tài khoản", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }



    // Dialog Quản lý Thông báo Tin Nhắn (Notification per account)
    if (showNotificationDialog) {
        Dialog(
            onDismissRequest = { showNotificationDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.82f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = "Tin nhắn",
                                tint = NeonPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Thông Báo & Tin Nhắn",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tài khoản: $userEmail",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { showNotificationDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (userNotifications.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { viewModel.clearAllNotifications() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Xóa Tất Cả Tin Nhắn",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    if (userNotifications.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.MarkEmailRead,
                                    contentDescription = "Trống",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(50.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Hộp thư thông báo trống!",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Không có tin nhắn nào cho tài khoản này.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(userNotifications, key = { it.id }) { item ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    when (item.type) {
                                                        "booking" -> NeonSecondary.copy(alpha = 0.15f)
                                                        "promo" -> NeonTertiary.copy(alpha = 0.15f)
                                                        else -> NeonPrimary.copy(alpha = 0.15f)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (item.type) {
                                                    "booking" -> Icons.Rounded.ConfirmationNumber
                                                    "promo" -> Icons.Rounded.CardGiftcard
                                                    else -> Icons.Rounded.Notifications
                                                },
                                                contentDescription = null,
                                                tint = when (item.type) {
                                                    "booking" -> NeonSecondary
                                                    "promo" -> NeonTertiary
                                                    else -> NeonPrimary
                                                },
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = item.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = item.timestamp,
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.message,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        IconButton(
                                            onClick = { viewModel.deleteNotification(item.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Xóa tin nhắn",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showNotificationDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Đóng", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(NeonPrimary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = NeonPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Go",
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun AuthFormView(viewModel: MovieViewModel) {
    var isLoginTab by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo cực ngầu
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(NeonPrimary, NeonSecondary)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MovieFilter,
                contentDescription = "Neon Cine Logo",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "CHÀO MỪNG ĐẾN VỚI NEON CINE",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Đăng nhập hoặc đăng ký tài khoản thành viên cực nhanh",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tab Selector (Đăng nhập / Đăng ký)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isLoginTab) NeonPrimary else Color.Transparent)
                    .clickable {
                        isLoginTab = true
                        errorText = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Đăng Nhập",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isLoginTab) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (!isLoginTab) NeonPrimary else Color.Transparent)
                    .clickable {
                        isLoginTab = false
                        errorText = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Đăng Ký",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (!isLoginTab) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Tên hiển thị (chỉ dùng khi đăng ký)
                AnimatedVisibility(visible = !isLoginTab) {
                    Column {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Họ và tên") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name", tint = Color.Gray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPrimary,
                                focusedLabelColor = NeonPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Địa chỉ Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPrimary,
                        focusedLabelColor = NeonPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mật khẩu
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = Color.Gray) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                                tint = Color.Gray
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPrimary,
                        focusedLabelColor = NeonPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (email.isBlank() || !email.contains("@")) {
                            errorText = "Vui lòng nhập Email hợp lệ."
                            return@Button
                        }
                        if (password.length < 4) {
                            errorText = "Mật khẩu phải từ 4 ký tự trở lên."
                            return@Button
                        }
                        if (!isLoginTab && name.isBlank()) {
                            errorText = "Vui lòng nhập họ và tên để đăng ký."
                            return@Button
                        }

                        // Thực hiện
                        if (isLoginTab) {
                            viewModel.login(email, name)
                        } else {
                            viewModel.register(name, email)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = if (isLoginTab) "Đăng Nhập Ngay ⚡" else "Đăng Ký Thành Viên 🎉",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * 6. Movies Screen (Trang Chứa Danh Sách Phim)
 * Trang hiển thị đầy đủ danh sách phim Đang chiếu & Sắp chiếu, với thanh tìm kiếm, bộ lọc thể loại, và đăng ký thông báo nhắc nhở rạp.
 */
@Composable
fun MoviesScreen(viewModel: MovieViewModel) {
    val movies by viewModel.movies.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("Tất cả") }
    var showNowShowingOnly by remember { mutableStateOf(true) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val genres = remember {
        listOf("Tất cả", "Gia đình", "Tâm lý", "Hoạt hình", "Hài hước", "Hành động")
    }

    val filteredMovies = remember(searchQuery, selectedGenre, showNowShowingOnly, movies) {
        movies.filter { movie ->
            val matchesSearch = movie.title.contains(searchQuery, ignoreCase = true) ||
                    movie.director.contains(searchQuery, ignoreCase = true) ||
                    movie.cast.contains(searchQuery, ignoreCase = true)
            val matchesGenre = selectedGenre == "Tất cả" || movie.genre.contains(selectedGenre, ignoreCase = true)
            val matchesShowing = movie.isNowShowing == showNowShowingOnly
            matchesSearch && matchesGenre && matchesShowing
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("movies_screen")
    ) {
        // Banner Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(NeonPrimary.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "Khám Phá Điện Ảnh 🎬",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Khám phá danh sách phim đang hot và đặt lịch nhận thông báo phim sắp ra mắt",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("movies_search_bar"),
                placeholder = { Text("Tìm tên phim, diễn viên, đạo diễn...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Tìm kiếm",
                        tint = NeonPrimary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Xóa tìm kiếm",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        // Tab Selector (Now Showing vs Upcoming)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showNowShowingOnly) NeonPrimary else Color.Transparent)
                        .clickable { showNowShowingOnly = true }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayCircleFilled,
                            contentDescription = null,
                            tint = if (showNowShowingOnly) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Đang Chiếu 🔥",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (showNowShowingOnly) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!showNowShowingOnly) NeonPrimary else Color.Transparent)
                        .clickable { showNowShowingOnly = false }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Upcoming,
                            contentDescription = null,
                            tint = if (!showNowShowingOnly) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Sắp Chiếu 🚀",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (!showNowShowingOnly) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Genre Filter Chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(genres) { genre ->
                    val isSelected = selectedGenre == genre
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedGenre = genre },
                        label = { Text(genre) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonSecondary.copy(alpha = 0.3f),
                            selectedLabelColor = NeonPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) NeonPrimary else Color.Transparent,
                            selectedBorderColor = NeonPrimary,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.5.dp,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }
        }

        // Search Count Label
        item {
            Text(
                text = "Tìm thấy ${filteredMovies.size} phim tương ứng",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
            )
        }

        // Render List or Empty State
        if (filteredMovies.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MovieFilter,
                        contentDescription = "Trống",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Không tìm thấy kết quả phù hợp!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Vui lòng thử từ khóa khác hoặc điều chỉnh bộ lọc thể loại.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            searchQuery = ""
                            selectedGenre = "Tất cả"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPrimary)
                    ) {
                        Text("Xóa bộ lọc 🔄", color = Color.White)
                    }
                }
            }
        } else {
            items(filteredMovies) { movie ->
                MovieDirectoryItem(
                    movie = movie,
                    onSelect = { viewModel.selectMovie(movie) },
                    onActionClick = {
                        if (movie.isNowShowing) {
                            viewModel.selectMovie(movie)
                            viewModel.startBookingFlow()
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "🔔 Đăng ký thành công! Neon Cinema sẽ gửi thông báo khuyến mãi và lịch đặt vé sớm cho phim: ${movie.title}!",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun MovieDirectoryItem(
    movie: com.example.data.model.Movie,
    onSelect: () -> Unit,
    onActionClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onSelect() }
            .testTag("movie_dir_item_${movie.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 95.dp, height = 135.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.LightGray)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = when (movie.ageRating) {
                                    "P" -> Color(0xFF4CAF50)
                                    "T16" -> Color(0xFFFF9800)
                                    "T18" -> Color(0xFFF44336)
                                    else -> NeonPrimary
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = movie.ageRating,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = if (movie.isNowShowing) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (movie.isNowShowing) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (movie.isNowShowing) "Đang Chiếu" else "Sắp Chiếu",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (movie.isNowShowing) Color(0xFF4CAF50) else Color(0xFFFF9800)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = movie.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = movie.genre,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 1.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${movie.duration} phút",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${movie.rating}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { onActionClick() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (movie.isNowShowing) NeonPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (movie.isNowShowing) Color.White else NeonPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (movie.isNowShowing) Icons.Rounded.ConfirmationNumber else Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (movie.isNowShowing) "Đặt Vé Ngay ⚡" else "Nhận Nhắc Nhở 🔔",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}


