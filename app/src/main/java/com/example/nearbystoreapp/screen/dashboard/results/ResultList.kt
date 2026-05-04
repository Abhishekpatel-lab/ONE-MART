package com.example.nearbystoreapp.screen.dashboard.results

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.example.nearbystoreapp.R
import com.example.nearbystoreapp.model.StoreModel
import com.example.nearbystoreapp.viewModel.ResultsViewmodel
import com.google.android.gms.location.LocationServices
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultList(
    id: String,
    title: String,
    onBackClick: () -> Unit,
    onStoreClick: (StoreModel) -> Unit,
) {
    val context = LocalContext.current
    val viewModel = remember { ResultsViewmodel() }
    val nearest = remember { mutableStateListOf<StoreModel>() }
    var showNearestLoading by remember { mutableStateOf(true) }
    var userLat by remember { mutableStateOf(0.0) }
    var userLon by remember { mutableStateOf(0.0) }
    var searchQuery by remember { mutableStateOf("") }
    var cityName by remember { mutableStateOf("📍 Location detect ho rahi hai...") }

    // ─── Location fetch ───────────────────────────────
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                LocationServices.getFusedLocationProviderClient(context)
                    .lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            userLat = location.latitude
                            userLon = location.longitude

                            try {
                                val geocoder = Geocoder(context, Locale.getDefault())
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                                        if (addresses.isNotEmpty()) {
                                            val city = addresses[0].locality ?: ""
                                            val state = addresses[0].adminArea ?: ""
                                            cityName = if (city.isNotEmpty()) "$city, $state" else state
                                        }
                                    }
                                } else {
                                    @Suppress("DEPRECATION")
                                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                    if (!addresses.isNullOrEmpty()) {
                                        val city = addresses[0].locality ?: ""
                                        val state = addresses[0].adminArea ?: ""
                                        cityName = if (city.isNotEmpty()) "$city, $state" else state
                                    }
                                }
                            } catch (_: Exception) {
                                cityName = "Current Location"
                            }
                        } else {
                            cityName = "Bareilly, Uttar Pradesh"
                        }
                    }
            } catch (_: Exception) {
                cityName = "Bareilly, Uttar Pradesh"
            }
        } else {
            cityName = "Bareilly, Uttar Pradesh"
        }
    }

    DisposableEffect(id, userLat, userLon) {
        val liveData = viewModel.loadNearest(id, userLat, userLon)
        val observer = Observer<MutableList<StoreModel>> {
            nearest.clear()
            nearest.addAll(it)
            showNearestLoading = false
        }
        liveData.observeForever(observer)
        onDispose { liveData.removeObserver(observer) }
    }

    Scaffold(
        containerColor = Color(0xFF0A0A0A),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        color = colorResource(R.color.gold),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colorResource(R.color.gold)
                        )
                    }
                },
                // ✅ Search icon hata diya
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0A))
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ─── Location Bar ──────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = colorResource(R.color.gold),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = cityName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    // ✅ Loading indicator jab tak city detect na ho
                    if (cityName.contains("detect")) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = colorResource(R.color.gold),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = "▼", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            // ─── Search Bar ────────────────────────────
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Store ya address search karo...", color = Color.Gray, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorResource(R.color.gold),
                        unfocusedBorderColor = Color(0xFF2A2A2A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = colorResource(R.color.gold),
                        unfocusedContainerColor = Color(0xFF1A1A1A),
                        focusedContainerColor = Color(0xFF1A1A1A)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // ─── Stores List ───────────────────────────
            item {
                NearestLisst(
                    list = nearest,
                    showNearestLoading = showNearestLoading,
                    onStoreClick = onStoreClick,
                    userLat = userLat,
                    userLon = userLon,
                    searchQuery = searchQuery
                )
            }
        }
    }
}