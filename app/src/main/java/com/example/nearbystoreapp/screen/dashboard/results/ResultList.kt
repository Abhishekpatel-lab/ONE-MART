package com.example.nearbystoreapp.screen.dashboard.results

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.example.nearbystoreapp.R
import com.example.nearbystoreapp.model.StoreModel
import com.example.nearbystoreapp.viewModel.ResultsViewmodel
import com.google.android.gms.location.LocationServices

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

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        userLat = location.latitude
                        userLon = location.longitude
                    }
                }
            } catch (e: Exception) {}
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.black2))
    ) {
        item { TopTitle(title, onBackClick) }
        // ✅ Search() hataya
        item { NearestLisst(nearest, showNearestLoading, onStoreClick) }
    }
}