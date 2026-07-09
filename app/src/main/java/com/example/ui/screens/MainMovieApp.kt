package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.data.model.Movie
import com.example.data.model.Review
import com.example.data.model.Ticket
import com.example.ui.AIAnalysisState
import com.example.ui.MovieViewModel
import com.example.ui.theme.MomoPrimary
import com.example.ui.theme.MomoSecondary
import com.example.ui.theme.MomoTertiary
import com.example.ui.theme.NeonPrimary
import com.example.ui.theme.NeonSecondary
import com.example.ui.theme.NeonTertiary
import com.example.ui.theme.NeonAccent

@Composable
fun MainMovieApp(
    viewModel: MovieViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedMovie by viewModel.selectedMovie.collectAsStateWithLifecycle()
    val isBookingActive by viewModel.isBookingFlowActive.collectAsStateWithLifecycle()
    val showSplashScreen by viewModel.showSplashScreen.collectAsStateWithLifecycle()
    val showLoginRequiredDialog by viewModel.showLoginRequiredDialog.collectAsStateWithLifecycle()

    if (showLoginRequiredDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLoginRequiredDialog() },
            title = {
                Text(
                    text = "Yêu Cầu Đăng Nhập 🔒",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Bạn vui lòng đăng nhập hoặc đăng ký tài khoản Neon Club để thực hiện đặt vé và trải nghiệm các tính năng thành viên cực hấp dẫn nhé!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissLoginRequiredDialog()
                        viewModel.selectMovie(null)
                        viewModel.selectTab(4)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MomoPrimary)
                ) {
                    Text("Đăng Nhập Ngay", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissLoginRequiredDialog() }
                ) {
                    Text("Bỏ qua", color = Color.Gray)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showSplashScreen) {
        SplashScreen()
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                if (selectedMovie == null && !isBookingActive) {
                    NeonBottomNavigationBar(
                        selectedTab = currentTab,
                        onTabSelected = { viewModel.selectTab(it) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when {
                    isBookingActive -> {
                        BookingFlowScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.stopBookingFlow() }
                        )
                    }
                    selectedMovie != null -> {
                        MovieDetailScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.selectMovie(null) }
                        )
                    }
                    else -> {
                        when (currentTab) {
                            0 -> HomeScreen(viewModel = viewModel)
                            1 -> MoviesScreen(viewModel = viewModel)
                            2 -> ShowtimesScreen(viewModel = viewModel)
                            3 -> TicketsScreen(viewModel = viewModel)
                            4 -> ProfileScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NeonBottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("neon_bottom_nav")
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            label = { 
                Text(
                    text = "Mua vé", 
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false
                ) 
            },
            icon = {
                Icon(
                    imageVector = if (selectedTab == 0) Icons.Rounded.Movie else Icons.Outlined.Movie,
                    contentDescription = "Mua vé xem phim"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonPrimary,
                selectedTextColor = NeonPrimary,
                indicatorColor = NeonSecondary.copy(alpha = 0.2f)
            ),
            modifier = Modifier.testTag("nav_tab_buy")
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            label = { 
                Text(
                    text = "Phim", 
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false
                ) 
            },
            icon = {
                Icon(
                    imageVector = if (selectedTab == 1) Icons.Rounded.LocalPlay else Icons.Outlined.LocalPlay,
                    contentDescription = "Danh sách phim"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonPrimary,
                selectedTextColor = NeonPrimary,
                indicatorColor = NeonSecondary.copy(alpha = 0.2f)
            ),
            modifier = Modifier.testTag("nav_tab_movies")
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            label = { 
                Text(
                    text = "Lịch chiếu", 
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false
                ) 
            },
            icon = {
                Icon(
                    imageVector = if (selectedTab == 2) Icons.Rounded.Schedule else Icons.Outlined.Schedule,
                    contentDescription = "Lịch chiếu phim"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonPrimary,
                selectedTextColor = NeonPrimary,
                indicatorColor = NeonSecondary.copy(alpha = 0.2f)
            ),
            modifier = Modifier.testTag("nav_tab_schedule")
        )
        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            label = { 
                Text(
                    text = "Vé", 
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false
                ) 
            },
            icon = {
                Icon(
                    imageVector = if (selectedTab == 3) Icons.Rounded.ConfirmationNumber else Icons.Outlined.ConfirmationNumber,
                    contentDescription = "Vé của tôi"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonPrimary,
                selectedTextColor = NeonPrimary,
                indicatorColor = NeonSecondary.copy(alpha = 0.2f)
            ),
            modifier = Modifier.testTag("nav_tab_tickets")
        )
        NavigationBarItem(
            selected = selectedTab == 4,
            onClick = { onTabSelected(4) },
            label = { 
                Text(
                    text = "Tôi", 
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false
                ) 
            },
            icon = {
                Icon(
                    imageVector = if (selectedTab == 4) Icons.Rounded.Person else Icons.Outlined.Person,
                    contentDescription = "Hồ sơ cá nhân"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonPrimary,
                selectedTextColor = NeonPrimary,
                indicatorColor = NeonSecondary.copy(alpha = 0.2f)
            ),
            modifier = Modifier.testTag("nav_tab_profile")
        )
    }
}

@Composable
fun HomeScreen(viewModel: MovieViewModel) {
    val movies by viewModel.movies.collectAsStateWithLifecycle()
    val promoCodes by viewModel.promoCodes.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        item {
            MomoHomeHeader(viewModel)
        }

        // Promo Banner Carousel
        item {
            MomoPromoBanner(viewModel = viewModel)
        }

        // Partner Cinema Ribbon
        item {
            MomoCinemaPartners()
        }

        // Section Title: Phim đang chiếu
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Phim Đang Chiếu Hot 🔥",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Tất cả",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MomoPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { }
                )
            }
        }

        // Movies List
        items(movies) { movie ->
            MovieCardItem(
                movie = movie,
                onSelect = { viewModel.selectMovie(movie) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun MomoHomeHeader(viewModel: MovieViewModel) {
    val context = LocalContext.current
    val userAddress by viewModel.userLocationAddress.collectAsStateWithLifecycle()
    var showNotificationDialog by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchCurrentLocation(context, viewModel)
        } else {
            Toast.makeText(context, "Quyền truy cập vị trí bị từ chối. Vui lòng cấp quyền trong cài đặt!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(NeonPrimary, NeonSecondary)
                )
            )
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Clickable Location Box
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (hasFine || hasCoarse) {
                            fetchCurrentLocation(context, viewModel)
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = "Vị trí",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Vị trí hiện tại",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = userAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = "Định vị trực tiếp",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Notification Bell replacing Wallet Balance Info
            Box {
                IconButton(
                    onClick = { showNotificationDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = "Thông báo",
                        tint = Color.White
                    )
                }
                // Notification red dot badge
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Color.Red)
                        .border(1.5.dp, NeonPrimary, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar Mock
        OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = { Text("Tìm kiếm phim đang chiếu...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Tìm kiếm", tint = Color.Gray) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            enabled = false
        )
    }

    // Dialog for Notification Bell Click
    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = "Thông báo mới",
                        tint = NeonPrimary
                    )
                    Text("Thông báo & Khuyến mãi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    NotificationItem(
                        title = "🎁 Quà tặng Neon Club",
                        desc = "Nhận ngay voucher giảm giá 50.000đ khi đặt vé lần đầu tiên qua cổng Supabase Realtime!",
                        time = "10 phút trước"
                    )
                    NotificationItem(
                        title = "🎬 Độc Quyền Phim Bom Tấn",
                        desc = "Phim 'Lật Mặt 7: Một Điều Ước' chính thức mở bán vé sớm với combo bắp nước siêu hời!",
                        time = "1 giờ trước"
                    )
                    NotificationItem(
                        title = "⚡ Nhân đôi tích điểm",
                        desc = "Đặc quyền thành viên: Tích lũy x2 điểm thưởng khi giao dịch trong khung giờ vàng 12h - 14h.",
                        time = "Hôm qua"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showNotificationDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPrimary)
                ) {
                    Text("Đóng")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun NotificationItem(title: String, desc: String, time: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = time, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

// Android Location Helpers
fun fetchCurrentLocation(context: Context, viewModel: MovieViewModel) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        return
    }
    
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (locationManager == null) {
        Toast.makeText(context, "Không thể khởi tạo Location Manager!", Toast.LENGTH_SHORT).show()
        return
    }
    
    val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    
    if (!isGpsEnabled && !isNetworkEnabled) {
        Toast.makeText(context, "Vui lòng bật GPS hoặc định vị trên thiết bị!", Toast.LENGTH_SHORT).show()
        return
    }
    
    val provider = if (isNetworkEnabled) LocationManager.NETWORK_PROVIDER else LocationManager.GPS_PROVIDER
    
    try {
        // Query last known location first to be responsive
        val lastKnownLocation = locationManager.getLastKnownLocation(provider)
        if (lastKnownLocation != null) {
            updateAddressFromLocation(context, lastKnownLocation, viewModel)
        }
        
        // Request a fresh update to ensure accuracy
        locationManager.requestSingleUpdate(provider, object : LocationListener {
            override fun onLocationChanged(location: Location) {
                updateAddressFromLocation(context, location, viewModel)
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }, context.mainLooper)
        
    } catch (e: SecurityException) {
        Toast.makeText(context, "Lỗi bảo mật khi định vị: ${e.message}", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Đang truy xuất vị trí GPS...", Toast.LENGTH_SHORT).show()
    }
}

fun updateAddressFromLocation(context: Context, location: Location, viewModel: MovieViewModel) {
    val geocoder = Geocoder(context, Locale.getDefault())
    try {
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            val district = address.subLocality ?: address.locality ?: address.subAdminArea ?: "Vị trí của bạn"
            val city = address.adminArea ?: address.countryName ?: ""
            val fullAddress = if (city.isNotEmpty()) "$district, $city" else district
            viewModel.updateUserLocation(fullAddress)
            Toast.makeText(context, "Đã định vị: $fullAddress", Toast.LENGTH_SHORT).show()
        } else {
            val coords = String.format(Locale.US, "%.4f, %.4f", location.latitude, location.longitude)
            viewModel.updateUserLocation(coords)
            Toast.makeText(context, "Tọa độ GPS: $coords", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        val coords = String.format(Locale.US, "%.4f, %.4f", location.latitude, location.longitude)
        viewModel.updateUserLocation(coords)
        Toast.makeText(context, "GPS định vị tọa độ: $coords", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MomoPromoBanner(viewModel: MovieViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val promoCodes by viewModel.promoCodes.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val tickets by viewModel.tickets.collectAsStateWithLifecycle()
    val prefs = remember(context) { context.getSharedPreferences("neon_cine_prefs", android.content.Context.MODE_PRIVATE) }

    if (promoCodes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(NeonPrimary, NeonTertiary)
                    )
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    radius = 120.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width, 0f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "HỆ THỐNG KHUYẾN MÃI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Đang cập nhật các ưu đãi mới nhất...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    } else {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Khuyến Mãi & Ưu Đãi Độc Quyền 🎁",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                items(promoCodes) { promo ->
                    val isUsed = tickets.any { it.userEmail == userEmail && it.promoCode == promo.code } ||
                            prefs.getBoolean("promo_used_${userEmail}_${promo.code}", false)
                    val gradientColors = if (isUsed) {
                        listOf(Color.Gray.copy(alpha = 0.6f), Color.DarkGray.copy(alpha = 0.6f))
                    } else {
                        listOf(NeonPrimary, NeonTertiary)
                    }

                    Box(
                        modifier = Modifier
                            .width(300.dp)
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(brush = Brush.linearGradient(colors = gradientColors))
                            .clickable {
                                if (!isUsed) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Promo Code", promo.code)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Đã sao chép mã: ${promo.code}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Mã này đã được sử dụng trước đó", Toast.LENGTH_SHORT).show()
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.08f),
                                radius = 90.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(size.width, 0f)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = if (isUsed) 0.15f else 0.25f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isUsed) "ĐÃ DÙNG" else "SỬ DỤNG NGAY",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = promo.code,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = promo.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(27.dp))
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (promo.discountAmount >= 1000) {
                                            "-${promo.discountAmount / 1000}K"
                                        } else {
                                            "-${promo.discountAmount}đ"
                                        },
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isUsed) "Hết hạn" else "Chạm để Copy",
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MomoCinemaPartners() {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "Trải Nghiệm Đẳng Cấp Tại Rạp ⚡",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val features = listOf(
                Triple("IMAX Laser", "Hình ảnh siêu thực sắc nét", Icons.Rounded.Tv),
                Triple("Dolby Atmos", "Âm thanh vòm 360 cực đã", Icons.Rounded.VolumeUp),
                Triple("VIP Gold Class", "Ghế da chỉnh điện cao cấp", Icons.Rounded.Star),
                Triple("Neon Cafe", "Bắp nước nóng hổi thơm ngon", Icons.Rounded.LocalDrink)
            )
            items(features) { (title, desc, icon) ->
                Card(
                    modifier = Modifier
                        .width(180.dp)
                        .height(68.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, NeonSecondary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonSecondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = NeonSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = desc,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MovieCardItem(
    movie: Movie,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onSelect() }
            .testTag("movie_card_${movie.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Poster Image
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = "Poster ${movie.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 85.dp, height = 120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Info Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Age Rating Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (movie.ageRating.startsWith("T")) Color(0xFFD32F2F) else Color(0xFF388E3C)
                                )
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = movie.ageRating,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = movie.genre,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = movie.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${movie.rating}/5.0",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• ${movie.duration} phút",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Book Button
                Button(
                    onClick = { onSelect() },
                    colors = ButtonDefaults.buttonColors(containerColor = MomoPrimary),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.End)
                        .height(34.dp)
                        .testTag("book_button_${movie.id}")
                ) {
                    Text(
                        text = "Mua Vé",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// Custom Markdown Text Renderer inside Compose for AI feedback
@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val lines = text.split("\n")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("###") -> {
                    val content = trimmed.substring(3).trim()
                    Text(
                        text = content,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MomoPrimary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                trimmed.startsWith("##") -> {
                    val content = trimmed.substring(2).trim()
                    Text(
                        text = content,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MomoPrimary,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                trimmed.startsWith("-") || trimmed.startsWith("*") -> {
                    val bulletContent = if (trimmed.startsWith("-")) trimmed.substring(1).trim() else trimmed.substring(1).trim()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MomoPrimary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseBoldMarkdown(bulletContent),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                trimmed.isNotBlank() -> {
                    Text(
                        text = parseBoldMarkdown(trimmed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

// Simple parser for **bold** text in markdown
@Composable
fun parseBoldMarkdown(input: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val regex = "\\*\\*(.*?)\\*\\*".toRegex()
        val matches = regex.findAll(input)
        
        for (match in matches) {
            // Append text before bold
            if (match.range.first > cursor) {
                append(input.substring(cursor, match.range.first))
            }
            // Append bold text
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                append(match.groupValues[1])
            }
            cursor = match.range.last + 1
        }
        // Append remaining text
        if (cursor < input.length) {
            append(input.substring(cursor))
        }
    }
}

@Composable
fun MovieDetailScreen(
    viewModel: MovieViewModel,
    onBack: () -> Unit
) {
    val movie by viewModel.selectedMovie.collectAsStateWithLifecycle()
    val reviews by viewModel.currentMovieReviews.collectAsStateWithLifecycle()
    val aiState by viewModel.aiAnalysisState.collectAsStateWithLifecycle()

    var authorName by remember { mutableStateOf("") }
    var reviewRating by remember { mutableStateOf(5) }
    var reviewContent by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    if (movie == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Back Button & Poster Banner Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            AsyncImage(
                model = movie!!.bannerUrl,
                contentDescription = "Movie Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Small Movie poster inside header overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                AsyncImage(
                    model = movie!!.posterUrl,
                    contentDescription = "Poster",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 60.dp, height = 85.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, Color.White, RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = movie!!.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${movie!!.genre} • ${movie!!.duration} phút",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Scrollable Body
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General specifications
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Đạo diễn: ") }
                            append(movie!!.director)
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Diễn viên: ") }
                            append(movie!!.cast)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Khởi chiếu: ") }
                            append(movie!!.releaseDate)
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Synopsis
            Column {
                Text(
                    text = "Nội dung phim",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = movie!!.synopsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }

            // 🤖 AI REVIEW ANALYSIS CARD (THE CORES OF USER REQUEST)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(
                            1.5.dp,
                            Brush.linearGradient(listOf(MomoPrimary, Color(0xFFFF529D)))
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("ai_analysis_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSystemInDarkTheme()) Color(0xFF26121C) else Color(0xFFFFF0F5)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MomoPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "AI",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Neon AI Đánh Giá Tổng Hợp",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MomoPrimary
                            )
                            Text(
                                text = "Tự động phân tích từ phản hồi khách hàng thực tế",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (aiState) {
                        is AIAnalysisState.Idle -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Nhấp để AI tổng hợp các điểm cộng, điểm trừ và đánh giá khách quan nhất về phim này.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { viewModel.analyzeReviewsWithAI() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MomoPrimary),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.testTag("ai_analyze_button")
                                ) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Bắt Đầu AI Phân Tích", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        is AIAnalysisState.Loading -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = MomoPrimary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Neon AI đang đọc phản hồi và tổng hợp...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MomoPrimary
                                )
                            }
                        }
                        is AIAnalysisState.Success -> {
                            MarkdownText(
                                text = (aiState as AIAnalysisState.Success).analysis,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            // Optional analysis repeat button
                            OutlinedButton(
                                onClick = { viewModel.analyzeReviewsWithAI() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MomoPrimary),
                                border = BorderStroke(1.dp, MomoPrimary),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cập nhật phân tích", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        is AIAnalysisState.Error -> {
                            Text(
                                text = "Lỗi phân tích: ${(aiState as AIAnalysisState.Error).message}",
                                color = Color.Red,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.analyzeReviewsWithAI() },
                                colors = ButtonDefaults.buttonColors(containerColor = MomoPrimary),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Thử Lại")
                            }
                        }
                    }
                }
            }

            // Customer Reviews List Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Đánh Giá Khách Hàng (${reviews.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Reviews List
            if (reviews.isEmpty()) {
                Text(
                    text = "Chưa có đánh giá nào cho phim này. Hãy là người đầu tiên đánh giá!",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (review in reviews) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = review.author,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Row {
                                        repeat(5) { star ->
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (star < review.rating) Color(0xFFFFC107) else Color.LightGray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = review.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Form to Add Custom Review
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Viết Đánh Giá Của Bạn",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MomoPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Star Select
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Điểm số: ", style = MaterialTheme.typography.bodyMedium)
                        repeat(5) { index ->
                            val starValue = index + 1
                            Icon(
                                imageVector = if (starValue <= reviewRating) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable { reviewRating = starValue }
                                    .testTag("star_$starValue")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Author input
                    OutlinedTextField(
                        value = authorName,
                        onValueChange = { authorName = it },
                        label = { Text("Tên của bạn") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("review_author_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MomoPrimary,
                            focusedLabelColor = MomoPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Content input
                    OutlinedTextField(
                        value = reviewContent,
                        onValueChange = { reviewContent = it },
                        label = { Text("Nhận xét về bộ phim...") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("review_content_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MomoPrimary,
                            focusedLabelColor = MomoPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (authorName.isNotBlank() && reviewContent.isNotBlank()) {
                                viewModel.submitReview(authorName, reviewRating, reviewContent)
                                authorName = ""
                                reviewContent = ""
                                reviewRating = 5
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MomoPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("submit_review_button")
                    ) {
                        Text("Gửi Đánh Giá", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        // Bottom CTA Section: Book Ticket
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Giá vé từ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "95.000đ",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MomoPrimary
                    )
                }

                Button(
                    onClick = { viewModel.startBookingFlow() },
                    colors = ButtonDefaults.buttonColors(containerColor = MomoPrimary),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.testTag("start_booking_button")
                ) {
                    Text("Đặt Vé Ngay", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun BookingFlowScreen(
    viewModel: MovieViewModel,
    onBack: () -> Unit
) {
    val movie by viewModel.selectedMovie.collectAsStateWithLifecycle()
    val cinemaName by viewModel.cinemaName.collectAsStateWithLifecycle()
    val cinema by viewModel.selectedCinema.collectAsStateWithLifecycle()
    val date by viewModel.selectedDate.collectAsStateWithLifecycle()
    val time by viewModel.selectedTime.collectAsStateWithLifecycle()
    val selectedSeats by viewModel.selectedSeats.collectAsStateWithLifecycle()
    val bookedSeats by viewModel.bookedSeats.collectAsStateWithLifecycle()
    val combos by viewModel.comboCount.collectAsStateWithLifecycle()
    val redeemedCombos by viewModel.redeemedComboCount.collectAsStateWithLifecycle()
    val userPoints by viewModel.userPoints.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val discount by viewModel.promoDiscount.collectAsStateWithLifecycle()
    val isPromoCodeUsed by viewModel.isPromoCodeUsed.collectAsStateWithLifecycle()

    val promoCodes by viewModel.promoCodes.collectAsStateWithLifecycle()
    var promoInputText by remember { mutableStateOf("") }
    var promoErrorMsg by remember { mutableStateOf<String?>(null) }

    var showPaymentConfirm by remember { mutableStateOf(false) }
    var isProcessingPayment by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }

    if (movie == null) return

    val totalAmount = viewModel.calculateTotalPrice()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Simple Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Trở về")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Mua Vé: ${movie!!.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Chọn rạp, suất chiếu & ghế ngồi",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Horizontal Step flow / Scrollable Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // STEP 1: CHỌN RẠP & SUẤT CHIẾU
            Column {
                Text(
                    text = "1. Chọn Rạp Chiếu & Ngày Giờ",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MomoPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Cinema list
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val cinemas = listOf(cinemaName)
                    items(cinemas) { item ->
                        val isSelected = cinema == item
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MomoPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.selectCinema(item) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("cinema_$item")
                        ) {
                            Text(
                                text = item,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Date selectors (Next 4 days)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
                    val dayFormat = SimpleDateFormat("EEE", Locale("vi", "VN"))
                    val now = Calendar.getInstance()

                    repeat(4) { offset ->
                        val tempCal = now.clone() as Calendar
                        tempCal.add(Calendar.DAY_OF_YEAR, offset)
                        val formattedDate = sdf.format(tempCal.time)
                        val formattedDay = if (offset == 0) "Hôm nay" else dayFormat.format(tempCal.time)

                        val isSelected = date == formattedDate
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.selectDate(formattedDate) }
                                .testTag("date_$formattedDate"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MomoPrimary else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, if (isSelected) MomoPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = formattedDay,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formattedDate,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Showtime options grid
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val showtimes = listOf("10:00", "13:30", "16:15", "19:00", "21:30")
                    for (item in showtimes) {
                        val isSelected = time == item
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, if (isSelected) MomoPrimary else Color.LightGray, RoundedCornerShape(6.dp))
                                .background(if (isSelected) MomoPrimary else Color.Transparent)
                                .clickable { viewModel.selectTime(item) }
                                .padding(vertical = 8.dp)
                                .testTag("time_$item"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // STEP 2: CHỌN GHẾ NGỒI (INTERACTIVE MAP)
            Column {
                Text(
                    text = "2. Chọn Ghế Ngồi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MomoPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Custom Canvas Screen indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth(0.8f).height(15.dp)) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, size.height)
                            quadraticTo(size.width / 2, 0f, size.width, size.height)
                        }
                        drawPath(
                            path = path,
                            color = MomoPrimary.copy(alpha = 0.5f),
                            style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                        )
                    }
                    Text(
                        text = "MÀN HÌNH CHÍNH",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MomoPrimary,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive 6x8 seat grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val rows = listOf("A", "B", "C", "D", "E", "F")
                    val cols = 8

                    for (row in rows) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = row,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.width(14.dp)
                            )
                            for (col in 1..cols) {
                                val seatId = "$row$col"
                                val isSelected = selectedSeats.contains(seatId)
                                val isBooked = bookedSeats.contains(seatId)
                                val isVIP = row == "D" || row == "E" // VIP rows

                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            when {
                                                isBooked -> Color(0xFF555555) // Màu xám đậm cho ghế đã đặt
                                                isSelected -> MomoPrimary
                                                isVIP -> Color(0xFFBA68C8) // VIP color purple
                                                else -> Color.LightGray.copy(alpha = 0.6f)
                                            }
                                        )
                                        .then(
                                            if (isBooked) Modifier else Modifier.clickable { viewModel.toggleSeat(seatId) }
                                        )
                                        .testTag("seat_$seatId"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isBooked) "X" else "$col",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBooked) Color.White.copy(alpha = 0.4f) else if (isSelected) Color.White else Color.Black
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Seat Legend Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(Color.LightGray))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Thường (95k)", fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFBA68C8)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("VIP (95k)", fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(MomoPrimary))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Đang chọn", fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF555555)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Đã bán (X)", fontSize = 10.sp)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // STEP 3: COMBO BẮP NƯỚC & VOUCHER
            Column {
                Text(
                    text = "3. Thêm Bắp Nước & Mã Giảm Giá",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MomoPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Combo selector card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MomoPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fastfood,
                                contentDescription = "Combo",
                                tint = MomoPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Combo Bắp + Nước Neon", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "1 bắp lớn vị ngọt + 1 nước ngọt 22oz mát lạnh", fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "75.000đ", fontWeight = FontWeight.ExtraBold, color = MomoPrimary, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        // Quantity counters
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    width = 1.2.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(30.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.decrementCombo() },
                                enabled = combos > 0,
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = if (combos > 0) MomoPrimary.copy(alpha = 0.1f) else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .testTag("decrement_combo")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Bớt",
                                    tint = if (combos > 0) MomoPrimary else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "$combos",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            IconButton(
                                onClick = { viewModel.incrementCombo() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MomoPrimary, CircleShape)
                                    .testTag("increment_combo")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Thêm",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                if (combos > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MomoPrimary.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.CardGiftcard,
                                        contentDescription = "Redeem points",
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Đổi bắp nước bằng Neon Points",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Dùng 100 Pts / combo. Bạn có: $userPoints Pts",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(30.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                val canIncrement = redeemedCombos < combos && (redeemedCombos + 1) * 100 <= userPoints
                                IconButton(
                                    onClick = { viewModel.decrementRedeemedCombo() },
                                    enabled = redeemedCombos > 0,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            color = if (redeemedCombos > 0) Color(0xFFFFB300).copy(alpha = 0.15f) else Color.Transparent,
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Bớt",
                                        tint = if (redeemedCombos > 0) Color(0xFFFFB300) else Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = "$redeemedCombos",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                )
                                IconButton(
                                    onClick = { viewModel.incrementRedeemedCombo() },
                                    enabled = canIncrement,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            color = if (canIncrement) Color(0xFFFFB300) else Color.LightGray.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Thêm",
                                        tint = if (canIncrement) Color.White else Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // MANUAL PROMO CODE ENTRY CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ConfirmationNumber,
                                contentDescription = "Promo Code",
                                tint = MomoPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Nhập mã giảm giá thủ công",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = promoInputText,
                                onValueChange = { 
                                    promoInputText = it
                                    promoErrorMsg = null 
                                },
                                placeholder = { Text("Nhập mã KM (ví dụ: NEON30, HE2026)", fontSize = 12.sp) },
                                singleLine = true,
                                enabled = !isPromoCodeUsed && discount == 0,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MomoPrimary,
                                    unfocusedBorderColor = Color.LightGray,
                                    disabledBorderColor = Color.LightGray.copy(alpha = 0.5f)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            if (discount > 0) {
                                Button(
                                    onClick = { 
                                        viewModel.removePromoCode()
                                        promoInputText = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("HỦY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (promoInputText.isBlank()) {
                                            promoErrorMsg = "Vui lòng nhập mã"
                                        } else {
                                            viewModel.checkAndApplyPromoCode(promoInputText) { error ->
                                                promoErrorMsg = error
                                            }
                                        }
                                    },
                                    enabled = !isPromoCodeUsed,
                                    colors = ButtonDefaults.buttonColors(containerColor = MomoPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("ÁP DỤNG", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                        
                        // Error message
                        if (promoErrorMsg != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = promoErrorMsg!!,
                                color = Color.Red,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // Success message
                        if (discount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "🎉 Áp dụng thành công! Giảm ${viewModel.formatCurrency(discount)}",
                                color = MomoPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (isPromoCodeUsed) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "🔒 Bạn đã sử dụng mã giảm giá cho tài khoản này.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        // STEP 4: BOTTOM INVOICE CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Booking Summary Text Info
                if (selectedSeats.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Ghế đã chọn: ${selectedSeats.joinToString(", ")}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Rạp: $cinema",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Tổng tiền thanh toán",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = viewModel.formatCurrency(totalAmount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MomoPrimary
                        )
                    }

                    Button(
                        onClick = { showPaymentConfirm = true },
                        enabled = selectedSeats.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MomoPrimary),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.testTag("pay_invoice_button")
                    ) {
                        Text("Tiếp Tục", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }
    }

    // PAYMENT CONFIRMATION DIALOG (CARD OR QR INTEGRATION)
    if (showPaymentConfirm) {
        var paymentMethod by remember { mutableStateOf("card") } // "card" or "qr"
        var cardNumber by remember { mutableStateOf("4111 2222 3333 4444") }
        var cardHolder by remember { mutableStateOf(userName.ifBlank { "NGUYEN VAN A" }.uppercase()) }
        var expiryDate by remember { mutableStateOf("12/28") }
        var cvvCode by remember { mutableStateOf("123") }

        Dialog(
            onDismissRequest = { if (!isProcessingPayment && !showSuccessAnimation) showPaymentConfirm = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp)
                        .testTag("payment_sheet"),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        if (!showSuccessAnimation) {
                            // Invoice header with logo
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Thanh toán an toàn qua Thẻ/QR",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                IconButton(onClick = { showPaymentConfirm = false }, enabled = !isProcessingPayment) {
                                    Icon(Icons.Default.Close, contentDescription = "Đóng")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Payment receipt breakdown
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = movie!!.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(text = "Rạp: $cinema", fontSize = 12.sp, color = Color.Gray)
                                    Text(text = "Suất chiếu: $date | $time", fontSize = 12.sp, color = Color.Gray)
                                    Text(text = "Ghế ngồi: ${selectedSeats.joinToString(", ")}", fontSize = 12.sp, color = Color.Gray)
                                    
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Giá vé (${selectedSeats.size}x)", fontSize = 12.sp)
                                        Text(viewModel.formatCurrency(selectedSeats.size * 95000), fontSize = 12.sp)
                                    }
                                    if (combos > 0) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Bắp nước combo (${combos}x)", fontSize = 12.sp)
                                            Text(viewModel.formatCurrency(combos * 75000), fontSize = 12.sp)
                                        }
                                        if (redeemedCombos > 0) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Đổi điểm Neon (${redeemedCombos}x combo)", fontSize = 12.sp, color = Color(0xFFFFB300))
                                                Text("-" + viewModel.formatCurrency(redeemedCombos * 75000), fontSize = 12.sp, color = Color(0xFFFFB300))
                                            }
                                        }
                                    }
                                    if (discount > 0) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Giảm giá voucher", fontSize = 12.sp, color = MomoPrimary)
                                            Text("-" + viewModel.formatCurrency(discount), fontSize = 12.sp, color = MomoPrimary)
                                        }
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("TỔNG TIỀN", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = viewModel.formatCurrency(totalAmount),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MomoPrimary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Chọn phương thức thanh toán",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // Tabs for Payment Methods
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (paymentMethod == "card") MomoPrimary else Color.Transparent)
                                        .clickable { paymentMethod = "card" }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.CreditCard,
                                            contentDescription = "Card",
                                            tint = if (paymentMethod == "card") Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Thẻ Quốc Tế/ATM",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (paymentMethod == "card") Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (paymentMethod == "qr") MomoPrimary else Color.Transparent)
                                        .clickable { paymentMethod = "qr" }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.QrCode,
                                            contentDescription = "QR Code",
                                            tint = if (paymentMethod == "qr") Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Quét Mã QR",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (paymentMethod == "qr") Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Payment method details
                            if (paymentMethod == "card") {
                                // Credit/Debit Card Form
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Thông tin thẻ của bạn", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = "Visa Test",
                                                    color = Color(0xFF1A73E8),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFF1A73E8).copy(alpha = 0.1f))
                                                        .clickable {
                                                            cardNumber = "4111 2222 3333 4444"
                                                            cardHolder = "NGUYEN VAN A"
                                                            expiryDate = "12/28"
                                                            cvvCode = "123"
                                                        }
                                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                                )
                                                Text(
                                                    text = "MasterCard Test",
                                                    color = Color(0xFFEA4335),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFFEA4335).copy(alpha = 0.1f))
                                                        .clickable {
                                                            cardNumber = "5412 7512 3412 3456"
                                                            cardHolder = "TRAN THI B"
                                                            expiryDate = "09/27"
                                                            cvvCode = "456"
                                                        }
                                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        OutlinedTextField(
                                            value = cardNumber,
                                            onValueChange = { cardNumber = it },
                                            label = { Text("Số thẻ", fontSize = 11.sp) },
                                            placeholder = { Text("4111 2222 3333 4444") },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MomoPrimary,
                                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedTextField(
                                            value = cardHolder,
                                            onValueChange = { cardHolder = it.uppercase() },
                                            label = { Text("Tên chủ thẻ (không dấu)", fontSize = 11.sp) },
                                            placeholder = { Text("NGUYEN VAN A") },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MomoPrimary,
                                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = expiryDate,
                                                onValueChange = { expiryDate = it },
                                                label = { Text("Hạn dùng", fontSize = 11.sp) },
                                                placeholder = { Text("MM/YY") },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = MomoPrimary,
                                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )

                                            OutlinedTextField(
                                                value = cvvCode,
                                                onValueChange = { cvvCode = it },
                                                label = { Text("Mã CVV", fontSize = 11.sp) },
                                                placeholder = { Text("123") },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = MomoPrimary,
                                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            } else {
                                // QR Code display
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "QUÉT MÃ QR ĐỂ THANH TOÁN",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MomoPrimary,
                                                letterSpacing = 1.sp
                                            )
                                            Text(
                                                text = "Giả Lập Quét QR",
                                                color = Color(0xFF1A73E8),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFF1A73E8).copy(alpha = 0.1f))
                                                    .clickable {
                                                        isProcessingPayment = true
                                                        val timer = Timer()
                                                        timer.schedule(object : TimerTask() {
                                                            override fun run() {
                                                                isProcessingPayment = false
                                                                showSuccessAnimation = true
                                                            }
                                                        }, 1200)
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        // QR Canvas drawing
                                        Box(
                                            modifier = Modifier
                                                .size(130.dp)
                                                .background(Color.White, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    isProcessingPayment = true
                                                    val timer = Timer()
                                                    timer.schedule(object : TimerTask() {
                                                        override fun run() {
                                                            isProcessingPayment = false
                                                            showSuccessAnimation = true
                                                        }
                                                    }, 1200)
                                                }
                                                .padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                val qrSize = 13
                                                val blockWidth = size.width / qrSize
                                                val blockHeight = size.height / qrSize

                                                for (i in 0 until qrSize) {
                                                    for (j in 0 until qrSize) {
                                                        // Finder patterns corners
                                                        val isTopLeftFinder = i < 4 && j < 4
                                                        val isTopRightFinder = i >= 9 && j < 4
                                                        val isBottomLeftFinder = i < 4 && j >= 9
                                                        val isFinderPattern = isTopLeftFinder || isTopRightFinder || isBottomLeftFinder
                                                        
                                                        // Standard inner ring and outer ring check for high fidelity
                                                        val isFinderBorder = (i == 0 || i == 3 || j == 0 || j == 3) && isTopLeftFinder ||
                                                                             (i == 9 || i == 12 || j == 0 || j == 3) && isTopRightFinder ||
                                                                             (i == 0 || i == 3 || j == 9 || j == 12) && isBottomLeftFinder
                                                        
                                                        val isRandomPixel = !isFinderPattern && ((i * 3 + j * 5) % 3 == 0 || (i + j) % 2 == 0 || (i - j) % 5 == 1)
                                                        
                                                        val drawBlock = isFinderBorder || isRandomPixel || (i in 1..2 && j in 1..2) || (i in 10..11 && j in 1..2) || (i in 1..2 && j in 10..11)
                                                        if (drawBlock) {
                                                            drawRect(
                                                                color = Color(0xFF1A1A1A),
                                                                topLeft = androidx.compose.ui.geometry.Offset(i * blockWidth, j * blockHeight),
                                                                size = androidx.compose.ui.geometry.Size(blockWidth, blockHeight)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Mở app ngân hàng hoặc Momo quét mã để chuyển khoản. (Dành cho Dev: Bấm trực tiếp vào mã QR hoặc nút phía trên để mô phỏng quét thành công)",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 10.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            if (isProcessingPayment) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MomoPrimary)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        isProcessingPayment = true
                                        // Simulate mock payment processing of 1.5s
                                        val timer = Timer()
                                        timer.schedule(object : TimerTask() {
                                            override fun run() {
                                                isProcessingPayment = false
                                                showSuccessAnimation = true
                                            }
                                        }, 1500)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MomoPrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("confirm_payment_button"),
                                    shape = RoundedCornerShape(26.dp)
                                ) {
                                    Text(
                                        text = "Xác Nhận Thanh Toán (${viewModel.formatCurrency(totalAmount)})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        } else {
                            // SUCCESS PAYMENT STATE
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8F5E9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Thành công",
                                        tint = Color(0xFF388E3C),
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "Thanh Toán Thành Công!",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF388E3C)
                                )
                                Text(
                                    text = "Mã vé đã được ghi nhận trong lịch sử giao dịch",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        viewModel.purchaseTicket {
                                            showPaymentConfirm = false
                                            showSuccessAnimation = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MomoPrimary),
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .height(44.dp)
                                        .testTag("go_to_tickets_button"),
                                    shape = RoundedCornerShape(22.dp)
                                ) {
                                    Text("Xem Vé Của Tôi", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TicketsScreen(viewModel: MovieViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val tickets by viewModel.tickets.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()

    val myTickets = remember(tickets, userEmail) {
        tickets.filter { it.userEmail.isBlank() || it.userEmail == userEmail }
    }

    var selectedTicketForEnlarge by remember { mutableStateOf<Ticket?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Ticket screen header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MomoPrimary)
                .padding(vertical = 12.dp, horizontal = 16.dp),
        ) {
            Text(
                text = "Lịch Sử Vé Đã Đặt 🎟️",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )

            IconButton(
                onClick = { viewModel.refreshDataFromSupabase() },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Đồng bộ Supabase",
                    tint = Color.White
                )
            }
        }

        if (!isLoggedIn) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Yêu cầu đăng nhập",
                    tint = MomoPrimary,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Yêu Cầu Đăng Nhập",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Vui lòng đăng nhập tài khoản thành viên để quản lý và xem lại các vé xem phim đã mua của bạn.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.selectTab(4) },
                    colors = ButtonDefaults.buttonColors(containerColor = MomoPrimary)
                ) {
                    Text("Đăng Nhập Ngay ⚡", color = Color.White)
                }
            }
        } else if (myTickets.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ConfirmationNumber,
                    contentDescription = "Chưa có vé",
                    tint = Color.Gray,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Bạn Chưa Có Giao Dịch Nào",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Hãy chọn những bộ phim cực hot bên tab Mua Vé và trải nghiệm đặt vé nhanh chóng tại Neon Cine nhé!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { viewModel.selectTab(0) },
                    colors = ButtonDefaults.buttonColors(containerColor = MomoPrimary)
                ) {
                    Text("Đặt Vé Ngay")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(myTickets) { ticket ->
                    TicketHistoryItem(
                        ticket = ticket,
                        viewModel = viewModel,
                        onTicketClick = { selectedTicketForEnlarge = it }
                    )
                }
            }
        }
    }

    // Dialog phóng to vé cho dễ quét
    if (selectedTicketForEnlarge != null) {
        val ticket = selectedTicketForEnlarge!!
        Dialog(
            onDismissRequest = { selectedTicketForEnlarge = null }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "VÉ XEM PHIM CỦA BẠN 🎟️",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MomoPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Movie details row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ticket.moviePoster,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 50.dp, height = 70.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ticket.movieTitle,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = ticket.cinema,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MomoPrimary
                            )
                            Text(
                                text = ticket.dateTime,
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Seat details
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Ghế ngồi", fontSize = 11.sp, color = Color.Gray)
                            Text(ticket.seats, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        if (!ticket.combo.isNullOrBlank()) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Combo", fontSize = 11.sp, color = Color.Gray)
                                Text(ticket.combo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Large scan area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // QR-like Canvas or big barcode
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val sizePx = size.width
                                val numBlocks = 15
                                val blockSize = sizePx / numBlocks
                                
                                // Standard QR anchor squares (Top-Left, Top-Right, Bottom-Left)
                                // Top-Left
                                drawRect(color = Color.Black, size = androidx.compose.ui.geometry.Size(blockSize * 5, blockSize * 5), topLeft = androidx.compose.ui.geometry.Offset(0f, 0f))
                                drawRect(color = Color.White, size = androidx.compose.ui.geometry.Size(blockSize * 3, blockSize * 3), topLeft = androidx.compose.ui.geometry.Offset(blockSize, blockSize))
                                drawRect(color = Color.Black, size = androidx.compose.ui.geometry.Size(blockSize, blockSize), topLeft = androidx.compose.ui.geometry.Offset(blockSize * 2, blockSize * 2))
                                
                                // Top-Right
                                drawRect(color = Color.Black, size = androidx.compose.ui.geometry.Size(blockSize * 5, blockSize * 5), topLeft = androidx.compose.ui.geometry.Offset(blockSize * 10, 0f))
                                drawRect(color = Color.White, size = androidx.compose.ui.geometry.Size(blockSize * 3, blockSize * 3), topLeft = androidx.compose.ui.geometry.Offset(blockSize * 11, blockSize))
                                drawRect(color = Color.Black, size = androidx.compose.ui.geometry.Size(blockSize, blockSize), topLeft = androidx.compose.ui.geometry.Offset(blockSize * 12, blockSize * 2))
                                
                                // Bottom-Left
                                drawRect(color = Color.Black, size = androidx.compose.ui.geometry.Size(blockSize * 5, blockSize * 5), topLeft = androidx.compose.ui.geometry.Offset(0f, blockSize * 10))
                                drawRect(color = Color.White, size = androidx.compose.ui.geometry.Size(blockSize * 3, blockSize * 3), topLeft = androidx.compose.ui.geometry.Offset(blockSize, blockSize * 11))
                                drawRect(color = Color.Black, size = androidx.compose.ui.geometry.Size(blockSize, blockSize), topLeft = androidx.compose.ui.geometry.Offset(blockSize * 2, blockSize * 12))
                                
                                // Pseudo random QR dots
                                val random = java.util.Random(ticket.id.hashCode().toLong())
                                for (r in 0 until numBlocks) {
                                    for (c in 0 until numBlocks) {
                                        if ((r < 5 && c < 5) || (r < 5 && c >= 10) || (r >= 10 && c < 5)) {
                                            continue
                                        }
                                        if (random.nextBoolean()) {
                                            drawRect(
                                                color = Color.Black,
                                                size = androidx.compose.ui.geometry.Size(blockSize, blockSize),
                                                topLeft = androidx.compose.ui.geometry.Offset(c * blockSize, r * blockSize)
                                            )
                                        }
                                    }
                                }
                                
                                // Center logo placeholder
                                drawRect(
                                    color = Color.White,
                                    size = androidx.compose.ui.geometry.Size(blockSize * 3, blockSize * 3),
                                    topLeft = androidx.compose.ui.geometry.Offset(blockSize * 6, blockSize * 6)
                                )
                            }
                            
                            // Center icon
                            Icon(
                                imageVector = Icons.Rounded.ConfirmationNumber,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color.White, CircleShape)
                                    .padding(4.dp),
                                tint = MomoPrimary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Large simulated barcode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .height(44.dp)
                                .background(Color.White)
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val barcodeBarWidths = listOf(2, 4, 1, 3, 2, 4, 1, 2, 3, 1, 4, 2, 1, 3, 2, 4, 1, 3, 2, 1, 3, 4, 2, 1)
                            for (width in barcodeBarWidths) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(width.dp)
                                        .background(Color.Black)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = ticket.barcode,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            letterSpacing = 2.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { selectedTicketForEnlarge = null },
                        colors = ButtonDefaults.buttonColors(containerColor = MomoPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Đóng", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun TicketHistoryItem(ticket: Ticket, viewModel: MovieViewModel, onTicketClick: (Ticket) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTicketClick(ticket) }
            .testTag("ticket_history_${ticket.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ticket Poster image
                AsyncImage(
                    model = ticket.moviePoster,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 60.dp, height = 85.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ticket.movieTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ticket.cinema,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MomoPrimary
                    )
                    Text(
                        text = ticket.dateTime,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dotted border separator
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
            ) {
                drawLine(
                    color = Color.LightGray,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Ghế ngồi: ${ticket.seats}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(text = "Combo: ${ticket.combo}", fontSize = 11.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Tổng thanh toán", fontSize = 10.sp, color = Color.Gray)
                    Text(text = viewModel.formatCurrency(ticket.totalPrice), fontWeight = FontWeight.ExtraBold, color = MomoPrimary, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Simulated Barcode with Ticket Code
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Barcode simulation lines
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(30.dp)
                        .background(Color.White)
                        .border(1.dp, Color.LightGray)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val barcodeBarWidths = listOf(2, 4, 1, 3, 2, 4, 1, 2, 3, 1, 4, 2, 1, 3, 2, 4, 1, 3, 2)
                    for (width in barcodeBarWidths) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(width.dp)
                                .background(Color.Black)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = ticket.barcode,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCode,
                        contentDescription = "Zoom",
                        tint = MomoPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "BẤM ĐỂ PHÓNG TO MÃ VÉ/QR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MomoPrimary,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
