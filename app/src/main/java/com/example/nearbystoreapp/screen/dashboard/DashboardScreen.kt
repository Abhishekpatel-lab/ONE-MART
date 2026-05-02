package com.example.nearbystoreapp.screen.dashboard

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nearbystoreapp.R
import com.example.nearbystoreapp.domain.BannerModel
import com.example.nearbystoreapp.domain.CategoryModel
import com.example.nearbystoreapp.model.StoreModel
import com.example.nearbystoreapp.repository.DashboardRepository
import com.example.nearbystoreapp.screen.wishlist.WishlistScreen
import com.example.nearbystoreapp.viewModel.AuthViewModel
import com.example.nearbystoreapp.viewModel.WishlistViewModel

@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel? = null,
    onCategoryClick: (id: String, title: String) -> Unit = { _, _ -> },
    onProfileClick: () -> Unit = {}
) {
    val viewModel = remember { DashboardRepository() }
    val wishlistViewModel: WishlistViewModel = viewModel()

    val categories = remember { mutableStateListOf<CategoryModel>() }
    val banners = remember { mutableStateListOf<BannerModel>() }
    var showCategoryLoading by remember { mutableStateOf(true) }
    var showBannerLoading by remember { mutableStateOf(true) }

    var currentScreen by remember { mutableStateOf("dashboard") }
    var showSearch by remember { mutableStateOf(false) }
    var selectedStore by remember { mutableStateOf<StoreModel?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadCategory().observeForever {
            categories.clear()
            categories.addAll(it)
            showCategoryLoading = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadBanner().observeForever {
            banners.clear()
            banners.addAll(it)
            showBannerLoading = false
        }
    }

    // ─── Screen Routing ───────────────────────────────────
    when {
        showSearch -> {
            SearchScreen(onBack = { showSearch = false })
            return
        }
        currentScreen == "support" -> {
            SupportScreen(onBack = { currentScreen = "dashboard" })
            return
        }
        currentScreen == "wishlist" -> {
            WishlistScreen(
                wishlistViewModel = wishlistViewModel,
                onStoreClick = { store ->
                    selectedStore = store
                    currentScreen = "storeDetail"
                },
                onBack = { currentScreen = "dashboard" }
            )
            return
        }
        currentScreen == "storeDetail" && selectedStore != null -> {
            StoreDetailScreen(
                store = selectedStore!!,
                wishlistViewModel = wishlistViewModel,
                onBack = { currentScreen = "wishlist" }
            )
            return
        }
    }

    // ─── Main Dashboard ───────────────────────────────────
    Scaffold(
        containerColor = colorResource(id = R.color.black2),
        bottomBar = {
            BottomBar(
                onHomeClick     = { currentScreen = "dashboard" },
                onSupportClick  = { currentScreen = "support" },   // ✅ Fix
                onWishlistClick = { currentScreen = "wishlist" },  // ✅ Fix
                onProfileClick  = onProfileClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = paddingValues)
        ) {
            item {
                TopBar(
                    onProfileClick = onProfileClick,
                    onSearchClick  = { showSearch = true }
                )
            }
            item {
                CategorySection(
                    categories          = categories,
                    showCategoryLoading = showCategoryLoading,
                    onCategoryClick     = onCategoryClick
                )
            }
            item { Banner(banners, showBannerLoading) }
        }
    }
}