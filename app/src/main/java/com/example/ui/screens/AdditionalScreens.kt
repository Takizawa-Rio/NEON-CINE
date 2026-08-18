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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
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

    val dates by viewModel.allAvailableShowtimeDates.collectAsStateWithLifecycle()
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
                if (dates.isEmpty()) {
                    Text(
                        text = "Hiện chưa có lịch chiếu nào trong hệ thống CSDL.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else {
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
                    Text("Hiện chưa có phim nào trên hệ thống.", color = Color.Gray)
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
    val showtimes = remember(movie.id, movie.stringId, selectedDate, dbShowtimes) {
        viewModel?.getShowtimesForMovieAndDate(movie, selectedDate) ?: emptyList()
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

            if (showtimes.isEmpty()) {
                Text(
                    text = "Chưa có suất chiếu trên hệ thống Supabase",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Text(
                    text = "Chọn suất chiếu (đồng bộ từ Supabase):",
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
    val userAvatarUrl by viewModel.userAvatarUrl.collectAsStateWithLifecycle()
    val userBalance by viewModel.userBalance.collectAsStateWithLifecycle()
    val userNotifications by viewModel.userNotifications.collectAsStateWithLifecycle()

    var showNotificationDialog by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showPolicyDialog by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf("") }
    var showUrlInputDialog by remember { mutableStateOf(false) }

    // Launcher chọn ảnh từ thư viện máy (Photo Picker / Gallery)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.updateUserAvatar(uri.toString())
        }
    }

    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.updateUserAvatar(uri.toString())
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Avatar & Nút đổi ảnh đại diện
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clickable { showAvatarDialog = true },
                contentAlignment = Alignment.Center
            ) {
                // Viền sáng bóng bẩy cho Avatar
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(MomoPrimary, NeonSecondary, Color(0xFF00C6FF))
                            )
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (userAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = userAvatarUrl,
                            contentDescription = "Ảnh đại diện $userName",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.DarkGray)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
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
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }

                // Badge nút Camera đổi ảnh ở góc dưới
                Surface(
                    shape = CircleShape,
                    color = MomoPrimary,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .clickable { showAvatarDialog = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = "Đổi ảnh đại diện",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            TextButton(
                onClick = { showAvatarDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoCamera,
                    contentDescription = null,
                    tint = MomoPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Đổi ảnh đại diện", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MomoPrimary)
            }

            Spacer(modifier = Modifier.height(6.dp))

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
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileMenuRow(
                        icon = Icons.Rounded.HeadsetMic,
                        title = "Liên Hệ & Hỗ Trợ Khách Hàng",
                        subtitle = "Hotline 24/7, Gửi phản hồi sự cố & Câu hỏi thường gặp (FAQ)",
                        onClick = { showSupportDialog = true }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileMenuRow(
                        icon = Icons.Rounded.Policy,
                        title = "Điều Khoản & Chính Sách Rạp",
                        subtitle = "Bảo mật, Quy định đổi trả vé & Quy định độ tuổi xem phim",
                        onClick = { showPolicyDialog = true }
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

    // Dialog Tùy chọn Thay đổi Ảnh Đại Diện (Avatar)
    if (showAvatarDialog) {
        Dialog(onDismissRequest = { showAvatarDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Thay Đổi Ảnh Đại Diện",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tải ảnh từ điện thoại hoặc chọn nhân vật yêu thích",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Lựa chọn 1: Chọn từ thư viện ảnh thiết bị
                    Button(
                        onClick = {
                            showAvatarDialog = false
                            try {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } catch (e: Exception) {
                                getContentLauncher.launch("image/*")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MomoPrimary)
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tải Ảnh Từ Thư Viện Máy", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "— Hoặc chọn Avatar Neon Cine —",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bộ sưu tập avatar mẫu phong cách điện ảnh
                    val presetAvatars = listOf(
                        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop" to "Đạo diễn",
                        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop" to "Mọt phim",
                        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&auto=format&fit=crop" to "Ngôi sao",
                        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&auto=format&fit=crop" to "Cyberpunk",
                        "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400&auto=format&fit=crop" to "Anime",
                        "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400&auto=format&fit=crop" to "Siêu anh hùng"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetAvatars) { (url, label) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.updateUserAvatar(url)
                                        showAvatarDialog = false
                                    }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = label,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, if (userAvatarUrl == url) MomoPrimary else Color.Transparent, CircleShape)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showAvatarDialog = false
                                customUrlInput = userAvatarUrl
                                showUrlInputDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Dán Link URL", fontSize = 12.sp)
                        }

                        if (userAvatarUrl.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    viewModel.updateUserAvatar("")
                                    showAvatarDialog = false
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Gỡ avatar", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { showAvatarDialog = false }) {
                        Text("Đóng")
                    }
                }
            }
        }
    }

    // Dialog nhập link URL ảnh trực tiếp
    if (showUrlInputDialog) {
        AlertDialog(
            onDismissRequest = { showUrlInputDialog = false },
            title = { Text("Nhập Đường Dẫn Link Ảnh", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = customUrlInput,
                    onValueChange = { customUrlInput = it },
                    label = { Text("URL hình ảnh (https://...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customUrlInput.isNotBlank()) {
                            viewModel.updateUserAvatar(customUrlInput.trim())
                        }
                        showUrlInputDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MomoPrimary)
                ) {
                    Text("Lưu Avatar", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showUrlInputDialog = false }) {
                    Text("Hủy")
                }
            }
        )
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

    // Dialog Liên hệ & Hỗ trợ khách hàng
    if (showSupportDialog) {
        SupportContactDialog(
            onDismiss = { showSupportDialog = false },
            onSendFeedback = { topic, message ->
                viewModel.addNotification(
                    title = "📩 Đã gửi yêu cầu hỗ trợ",
                    message = "Chủ đề: $topic. Cảm ơn bạn, bộ phận CSKH Neon Cine sẽ phản hồi trong thời gian sớm nhất.",
                    type = "general"
                )
            }
        )
    }

    // Dialog Điều khoản & Chính sách rạp
    if (showPolicyDialog) {
        PoliciesTermsDialog(
            onDismiss = { showPolicyDialog = false }
        )
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
    var showSupportDialog by remember { mutableStateOf(false) }
    var showPolicyDialog by remember { mutableStateOf(false) }

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

                Spacer(modifier = Modifier.height(24.dp))

                // Liên kết hỗ trợ & chính sách cho khách truy cập
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showSupportDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.HeadsetMic,
                            contentDescription = null,
                            tint = NeonPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Liên hệ hỗ trợ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonPrimary
                        )
                    }

                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    TextButton(onClick = { showPolicyDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Policy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Chính sách & Quy định",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Dialog Liên hệ & Hỗ trợ
    if (showSupportDialog) {
        SupportContactDialog(
            onDismiss = { showSupportDialog = false },
            onSendFeedback = { topic, message ->
                viewModel.addNotification(
                    title = "📩 Đã gửi yêu cầu hỗ trợ",
                    message = "Chủ đề: $topic. Bộ phận CSKH sẽ hỗ trợ bạn sớm nhất.",
                    type = "general"
                )
            }
        )
    }

    // Dialog Chính sách & Quy định
    if (showPolicyDialog) {
        PoliciesTermsDialog(
            onDismiss = { showPolicyDialog = false }
        )
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

/**
 * 7. Dialog Liên Hệ & Hỗ Trợ Khách Hàng (Support & Contact Dialog)
 */
@Composable
fun SupportContactDialog(
    onDismiss: () -> Unit,
    onSendFeedback: (topic: String, message: String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // Form state
    var selectedTopic by remember { mutableStateOf("Sự cố đặt vé / xuất mã QR") }
    var feedbackContent by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var feedbackSent by remember { mutableStateOf(false) }

    val topics = remember {
        listOf(
            "Sự cố đặt vé / xuất mã QR",
            "Yêu cầu đổi suất chiếu",
            "Lỗi thanh toán MoMo / VNPAY",
            "Tích điểm hội viên Neon Club",
            "Góp ý chất lượng rạp & dịch vụ",
            "Vấn đề khác"
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(NeonPrimary.copy(alpha = 0.2f), NeonSecondary.copy(alpha = 0.1f))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NeonPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.HeadsetMic,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Hỗ Trợ Khách Hàng",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tổng đài 24/7 & Giải đáp tức thì",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = NeonPrimary,
                    divider = { Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Liên Hệ", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Rounded.PhoneInTalk, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Gửi Yêu Cầu", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Rounded.RateReview, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Hỏi Đáp FAQ", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Rounded.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                // Tab Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Tab 1: Liên hệ trực tiếp
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                item {
                                    // Hotline Card
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, NeonPrimary.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(NeonPrimary.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Phone,
                                                    contentDescription = null,
                                                    tint = NeonPrimary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Hotline Tổng Đài 24/7",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "1900 6868",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = NeonPrimary
                                                )
                                                Text(
                                                    text = "Cước phí: 1.000đ/phút • Hoạt động 24/7",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            Button(
                                                onClick = {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                                            data = Uri.parse("tel:19006868")
                                                        }
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        // Ignored
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = NeonPrimary),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("Gọi Ngay", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }

                                item {
                                    // Email Card
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(NeonSecondary.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Email,
                                                    contentDescription = null,
                                                    tint = NeonSecondary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Email Chăm Sóc Khách Hàng",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "hotro@neoncine.vn",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Phản hồi trong vòng 30 phút",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            Button(
                                                onClick = {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                            data = Uri.parse("mailto:hotro@neoncine.vn")
                                                            putExtra(Intent.EXTRA_SUBJECT, "Yêu cầu hỗ trợ Neon Cine")
                                                        }
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        // Ignored
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = NeonSecondary),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("Gửi Mail", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }

                                item {
                                    // Thông tin cụm rạp & trụ sở
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = MomoPrimary, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Trụ Sở & Cụm Rạp Trung Tâm",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "📍 Tòa nhà Neon Cine Tower, 123 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh\n📍 Neon Cine Center, 88 Cầu Giấy, Hà Nội\n📍 Neon Cine Plaza, 45 Nguyễn Văn Linh, Đà Nẵng",
                                                fontSize = 11.sp,
                                                lineHeight = 18.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Spacer(modifier = Modifier.height(12.dp))
                                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = NeonPrimary, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Thời Gian Phục Vụ",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "• Mở cửa rạp: 08:00 - 01:00 (Sáng hôm sau)\n• Tổng đài hỗ trợ: 24/7 (Cả ngày Lễ & Tết)",
                                                fontSize = 11.sp,
                                                lineHeight = 17.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // Tab 2: Gửi phản hồi / Yêu cầu hỗ trợ
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (feedbackSent) {
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 32.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF4CAF50)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "Gửi Yêu Cầu Thành Công!",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Mã yêu cầu: #NC-${(1000..9999).random()}\nBộ phận CSKH Neon Cine đã tiếp nhận và sẽ phản hồi qua Hộp thư thông báo trong vòng 15-30 phút.",
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                            Spacer(modifier = Modifier.height(20.dp))
                                            Button(
                                                onClick = {
                                                    feedbackSent = false
                                                    feedbackContent = ""
                                                    contactPhone = ""
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = NeonPrimary),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Gửi Yêu Cầu Khác", color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                } else {
                                    item {
                                        Text(
                                            text = "Chọn Chủ Đề Cần Hỗ Trợ",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Topic chips
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            topics.chunked(2).forEach { rowTopics ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    rowTopics.forEach { topic ->
                                                        val isSelected = selectedTopic == topic
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = if (isSelected) NeonPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                            border = BorderStroke(1.dp, if (isSelected) NeonPrimary else Color.Transparent),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clickable { selectedTopic = topic }
                                                        ) {
                                                            Text(
                                                                text = topic,
                                                                fontSize = 11.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (isSelected) NeonPrimary else MaterialTheme.colorScheme.onSurface,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                                                textAlign = TextAlign.Center,
                                                                maxLines = 2
                                                            )
                                                        }
                                                    }
                                                    if (rowTopics.size == 1) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Số Điện Thoại / Email Liên Hệ Lại",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = contactPhone,
                                            onValueChange = { contactPhone = it },
                                            placeholder = { Text("VD: 0912345678 hoặc email@example.com", fontSize = 12.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }

                                    item {
                                        Text(
                                            text = "Mô Tả Chi Tiết Vấn Đề",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = feedbackContent,
                                            onValueChange = { feedbackContent = it },
                                            placeholder = { Text("Vui lòng mô tả chi tiết sự cố bạn gặp phải, mã giao dịch, rạp chiếu hoặc góp ý của bạn...", fontSize = 12.sp) },
                                            minLines = 3,
                                            maxLines = 6,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                if (feedbackContent.isNotBlank()) {
                                                    onSendFeedback(selectedTopic, feedbackContent)
                                                    feedbackSent = true
                                                }
                                            },
                                            enabled = feedbackContent.isNotBlank(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = NeonPrimary,
                                                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(46.dp)
                                        ) {
                                            Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Gửi Yêu Cầu Hỗ Trợ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // Tab 3: Câu hỏi thường gặp FAQ
                            val faqs = remember {
                                listOf(
                                    "Tôi có thể đổi hoặc hủy vé sau khi đã thanh toán thành công không?" to
                                            "Bạn có thể đổi sang suất chiếu khác trước giờ chiếu ít nhất 60 phút bằng cách gọi Hotline 1900 6868 hoặc liên hệ trực tiếp quầy vé tại rạp. Vé đã qua giờ chiếu sẽ không thể đổi hoặc hoàn tiền.",
                                    "Làm thế nào để vào phòng chiếu khi đã đặt vé online?" to
                                            "Sau khi thanh toán, vé điện tử có mã QR sẽ xuất hiện trong mục 'Vé Của Tôi'. Bạn chỉ cần mở mã QR này và quét trực tiếp tại cổng soát vé vào phòng chiếu mà không cần in vé giấy.",
                                    "Tài khoản bị trừ tiền nhưng không nhận được vé thì xử lý sao?" to
                                            "Trường hợp nghẽn mạng cổng thanh toán, hệ thống sẽ tự động đối soát trong vòng 10-15 phút. Bạn cũng có thể liên hệ ngay Hotline 1900 6868 hoặc gửi yêu cầu ở mục Hỗ Trợ kèm mã giao dịch để nhân viên kích hoạt vé hoặc hoàn tiền ngay.",
                                    "Điểm Neon Club dùng để làm gì và có hạn sử dụng không?" to
                                            "Điểm tích lũy Neon Club có thể dùng để đổi vé xem phim miễn phí (2D/3D), đổi bắp nước và nâng cấp ghế VIP. Điểm thưởng có giá trị trong vòng 12 tháng kể từ ngày giao dịch.",
                                    "Khán giả dưới độ tuổi quy định có được vào xem phim giới hạn tuổi không?" to
                                            "Theo quy định của Cục Điện ảnh, khán giả phải đủ độ tuổi theo nhãn phim (T13, T16, T18). Nhân viên soát vé có quyền yêu cầu xuất trình giấy tờ tùy thân (CCCD/VNeID/Thẻ học sinh) trước khi vào phòng chiếu."
                                 )
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(faqs) { (question, answer) ->
                                    var isExpanded by remember { mutableStateOf(false) }
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isExpanded = !isExpanded },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isExpanded) NeonPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        ),
                                        border = BorderStroke(1.dp, if (isExpanded) NeonPrimary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Help,
                                                        contentDescription = null,
                                                        tint = NeonPrimary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = question,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Icon(
                                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            if (isExpanded) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = answer,
                                                    fontSize = 11.sp,
                                                    lineHeight = 17.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Đóng Trung Tâm Hỗ Trợ", fontWeight = FontWeight.Bold, color = NeonPrimary)
                    }
                }
            }
        }
    }
}

/**
 * 8. Dialog Điều Khoản & Chính Sách Rạp (Policies & Terms Dialog)
 */
@Composable
fun PoliciesTermsDialog(
    onDismiss: () -> Unit
) {
    var selectedPolicyTab by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(NeonSecondary.copy(alpha = 0.2f), NeonPrimary.copy(alpha = 0.1f))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NeonSecondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Policy,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Điều Khoản & Chính Sách",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Quy chế hoạt động cụm rạp Neon Cine",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Scrollable Tab Selector
                ScrollableTabRow(
                    selectedTabIndex = selectedPolicyTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = NeonSecondary,
                    edgePadding = 12.dp,
                    divider = { Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) }
                ) {
                    Tab(
                        selected = selectedPolicyTab == 0,
                        onClick = { selectedPolicyTab = 0 },
                        text = { Text("Điều Khoản Sử Dụng", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedPolicyTab == 1,
                        onClick = { selectedPolicyTab = 1 },
                        text = { Text("Chính Sách Bảo Mật", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedPolicyTab == 2,
                        onClick = { selectedPolicyTab = 2 },
                        text = { Text("Đổi Trả & Hoàn Vé", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedPolicyTab == 3,
                        onClick = { selectedPolicyTab = 3 },
                        text = { Text("Độ Tuổi & Nội Quy", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                // Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedPolicyTab) {
                        0 -> {
                            // Điều khoản sử dụng
                            item {
                                PolicySection(
                                    title = "1. Chấp Thuận Các Điều Khoản",
                                    content = "Bằng việc cài đặt, đăng ký hoặc sử dụng ứng dụng Neon Cine để đặt vé xem phim và bắp nước, bạn đồng ý tuân thủ toàn bộ các điều khoản và điều kiện hoạt động được quy định tại đây."
                                )
                            }
                            item {
                                PolicySection(
                                    title = "2. Tài Khoản & Trách Nhiệm Thành Viên",
                                    content = "• Người dùng chịu trách nhiệm bảo mật thông tin đăng nhập và mật khẩu của mình.\n• Mọi giao dịch được thực hiện qua tài khoản của bạn được coi là do bạn trực tiếp xác nhận.\n• Nghiêm cấm hành vi sử dụng công cụ tự động (bot), giả mạo thông tin hoặc gian lận voucher khuyến mãi."
                                )
                            }
                            item {
                                PolicySection(
                                    title = "3. Giá Vé & Thanh Toán Trực Tuyến",
                                    content = "• Toàn bộ giá vé, phụ thu phòng chiếu (IMAX, 3D, VIP) và combo bắp nước được niêm yết công khai bằng Việt Nam Đồng (VND), đã bao gồm thuế GTGT.\n• Giao dịch thanh toán qua MoMo, VNPAY, Thẻ nội địa và Quốc tế được bảo vệ theo giao thức an toàn SSL."
                                )
                            }
                            item {
                                PolicySection(
                                    title = "4. Quyền Sở Hữu Trí Tuệ",
                                    content = "Hình ảnh, trailer phim, poster, biểu trưng và mã nguồn ứng dụng thuộc quyền sở hữu của Neon Cine và các nhà phát hành phim đối tác. Nghiêm cấm mọi hành vi sao chép nhằm mục đích thương mại trái phép."
                                )
                            }
                        }

                        1 -> {
                            // Chính sách bảo mật
                            item {
                                PolicySection(
                                    title = "1. Thu Thập Thông Tin",
                                    content = "Chúng tôi chỉ thu thập các thông tin cần thiết phục vụ việc xuất vé điện tử và quyền lợi thành viên gồm: Họ tên, Số điện thoại, Địa chỉ Email và Lịch sử giao dịch đặt vé."
                                )
                            }
                            item {
                                PolicySection(
                                    title = "2. Bảo Mật Thanh Toán (PCI-DSS)",
                                    content = "Neon Cine không lưu trữ thông tin số thẻ tín dụng hoặc mã bảo mật CVV trên hệ thống. Toàn bộ tiến trình thanh toán được mã hóa và xử lý trực tiếp qua các đối tác cổng thanh toán được Ngân hàng Nhà nước cấp phép."
                                )
                            }
                            item {
                                PolicySection(
                                    title = "3. Cam Kết Không Chia Sẻ Dữ Liệu",
                                    content = "Thông tin cá nhân của bạn được bảo mật tuyệt đối. Chúng tôi cam đoan không bán, trao đổi hoặc cung cấp dữ liệu người dùng cho bất kỳ bên thứ ba nào vì mục đích quảng cáo rác."
                                )
                            }
                            item {
                                PolicySection(
                                    title = "4. Quyền Của Khách Hàng",
                                    content = "Bạn có quyền tra cứu, cập nhật thông tin cá nhân, thay đổi ảnh đại diện hoặc yêu cầu xóa toàn bộ lịch sử vé xem phim bất kỳ lúc nào trong ứng dụng."
                                )
                            }
                        }

                        2 -> {
                            // Đổi trả & Hoàn vé
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MomoPrimary.copy(alpha = 0.1f)),
                                    border = BorderStroke(1.dp, MomoPrimary.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.Info, contentDescription = null, tint = MomoPrimary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Quy Định Đổi Vé", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MomoPrimary)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Khách hàng được hỗ trợ đổi sang suất chiếu khác trước giờ chiếu ít nhất 60 phút. Liên hệ Hotline 1900 6868 hoặc quầy vé tại rạp.",
                                            fontSize = 11.sp,
                                            lineHeight = 17.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            item {
                                PolicySection(
                                    title = "1. Chính Sách Hoàn Tiền Vé",
                                    content = "• Vé xem phim đã mua thành công không áp dụng hoàn tiền tự động nếu khách hàng không đến xem đúng giờ.\n• Trong trường hợp xảy ra sự cố kỹ thuật từ rạp (mất điện, lỗi máy chiếu, hoãn chiếu), Neon Cine hoàn tiền 100% qua tài khoản thanh toán ban đầu hoặc tặng 02 voucher xem phim miễn phí."
                                )
                            }
                            item {
                                PolicySection(
                                    title = "2. Vé Đặt Nhầm Suất Hoặc Nhầm Phim",
                                    content = "Vui lòng kiểm tra kỹ thông tin rạp, ngày chiếu và giờ chiếu trước khi xác nhận thanh toán. Trong vòng 15 phút sau khi đặt nhầm, hãy gọi ngay hotline CSKH để được chuyên viên hỗ trợ đổi suất sớm nhất."
                                )
                            }
                        }

                        3 -> {
                            // Độ tuổi & Nội quy rạp
                            item {
                                Text(
                                    text = "Bảng Phân Loại Độ Tuổi (Theo Cục Điện Ảnh)",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AgeRatingItem(badge = "P", color = Color(0xFF4CAF50), desc = "Phổ biến: Phim được phép phổ biến đến người xem ở mọi độ tuổi.")
                                    AgeRatingItem(badge = "K", color = Color(0xFF2196F3), desc = "Dưới 13 tuổi: Được xem với điều kiện có cha, mẹ hoặc người giám hộ đi cùng.")
                                    AgeRatingItem(badge = "T13", color = Color(0xFFFF9800), desc = "13+: Phim được phép phổ biến đến khán giả từ đủ 13 tuổi trở lên.")
                                    AgeRatingItem(badge = "T16", color = Color(0xFFE91E63), desc = "16+: Phim được phép phổ biến đến khán giả từ đủ 16 tuổi trở lên.")
                                    AgeRatingItem(badge = "T18", color = Color(0xFFF44336), desc = "18+: Phim dành riêng cho khán giả từ đủ 18 tuổi trở lên (Yêu cầu xuất trình CCCD/VNeID).")
                                    AgeRatingItem(badge = "C", color = Color(0xFF9E9E9E), desc = "Cấm phổ biến: Phim không được phép phát hành.")
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                PolicySection(
                                    title = "Nội Quy Phòng Chiếu",
                                    content = "• Chuyển điện thoại sang chế độ rung/im lặng trước khi phim bắt đầu.\n• Nghiêm cấm ghi âm, chụp ảnh, quay video màn hình chiếu (Hành vi vi phạm bản quyền sẽ bị xử lý theo pháp luật).\n• Không hút thuốc (kể cả thuốc lá điện tử) trong toàn bộ khuôn viên rạp.\n• Vui lòng không mang thức ăn có mùi nặng từ ngoài vào phòng chiếu."
                                )
                            }
                        }
                    }
                }

                // Footer
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Đã Hiểu & Đóng", fontWeight = FontWeight.Bold, color = NeonSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun PolicySection(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = content,
                fontSize = 11.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AgeRatingItem(badge: String, color: Color, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badge,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = desc,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}


