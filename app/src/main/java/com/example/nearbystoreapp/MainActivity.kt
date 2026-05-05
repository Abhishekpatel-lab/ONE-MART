package com.example.nearbystoreapp

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nearbystoreapp.model.StoreModel
import com.example.nearbystoreapp.screens.AdminDashboardScreen
import com.example.nearbystoreapp.screen.auth.LoginScreen
import com.example.nearbystoreapp.screen.auth.RegisterScreen
import com.example.nearbystoreapp.screen.dashboard.AllCategoriesScreen
import com.example.nearbystoreapp.screen.dashboard.SearchScreen
import com.example.nearbystoreapp.screen.dashboard.StoreDetailScreen
import com.example.nearbystoreapp.screen.dashboard.SupportScreen
import com.example.nearbystoreapp.screen.dashboard.map.MapScreen
import com.example.nearbystoreapp.screen.dashboard.results.ResultList
import com.example.nearbystoreapp.screen.profile.ProfileScreen
import com.example.nearbystoreapp.screen.store.StoreDashboardPlaceholder
import com.example.nearbystoreapp.screen.wishlist.WishlistScreen
import com.example.nearbystoreapp.viewModel.AuthState
import com.example.nearbystoreapp.viewModel.AuthViewModel
import com.example.nearbystoreapp.viewModel.WishlistViewModel
import com.example.nearbystoreapp.util.FirebaseMigrationUtil
import com.google.gson.Gson
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        FirebaseMigrationUtil.addMissingStoreFields()

        setContent {
            val navController    = rememberNavController()
            val authViewModel: AuthViewModel       = viewModel()
            val wishlistViewModel: WishlistViewModel = viewModel()
            val authState by authViewModel.authState.collectAsState()
            var navReady by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) { authViewModel.checkCurrentUser() }

            NavHost(navController = navController, startDestination = "splash") {

                composable("splash") {
                    LaunchedEffect(Unit) { navReady = true }
                    Box(
                        modifier = Modifier.fillMaxSize().background(colorResource(R.color.black3)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colorResource(R.color.gold))
                    }
                }

                composable("login") {
                    LoginScreen(
                        authViewModel       = authViewModel,
                        onLoginSuccess      = { userType ->
                            when (userType) {
                                "store_owner" -> navController.navigate("store_dashboard") { popUpTo(0) { inclusive = true } }
                                "admin"       -> navController.navigate("admin_dashboard")  { popUpTo(0) { inclusive = true } }
                                else          -> navController.navigate("user_dashboard")   { popUpTo(0) { inclusive = true } }
                            }
                        },
                        onNavigateToRegister = { navController.navigate("register") }
                    )
                }

                composable("register") {
                    RegisterScreen(
                        authViewModel      = authViewModel,
                        onRegisterSuccess  = { userType ->
                            when (userType) {
                                "store_owner" -> navController.navigate("store_dashboard") { popUpTo(0) { inclusive = true } }
                                "admin"       -> navController.navigate("admin_dashboard")  { popUpTo(0) { inclusive = true } }
                                else          -> navController.navigate("user_dashboard")   { popUpTo(0) { inclusive = true } }
                            }
                        },
                        onNavigateToLogin = { navController.popBackStack() }
                    )
                }

                composable("user_dashboard") {
                    com.example.nearbystoreapp.screen.dashboard.DashboardScreen(
                        authViewModel      = authViewModel,
                        wishlistViewModel  = wishlistViewModel,
                        onCategoryClick    = { id, title -> navController.navigate("results/$id/$title") },
                        onProfileClick     = { navController.navigate("profile") },
                        onStoreClick       = { store ->
                            val storeJson = URLEncoder.encode(Gson().toJson(store), StandardCharsets.UTF_8.toString())
                            navController.navigate("store_detail/$storeJson")
                        },
                        onWishlistClick    = { navController.navigate("wishlist") },
                        onSupportClick     = { navController.navigate("support") },
                        onCategoriesTabClick = { navController.navigate("all_categories") },
                        onSearchClick      = { navController.navigate("search") }  // ✅ Add kiya
                    )
                }

                composable("store_dashboard") {
                    StoreDashboardPlaceholder(
                        authViewModel = authViewModel,
                        onLogout      = { navController.navigate("login") { popUpTo(0) { inclusive = true } } }
                    )
                }

                composable("admin_dashboard") {
                    AdminDashboardScreen(
                        onLogout = {
                            authViewModel.logout()
                            navController.navigate("login") { popUpTo(0) { inclusive = true } }
                        }
                    )
                }

                composable("profile") {
                    ProfileScreen(
                        authViewModel = authViewModel,
                        onLogout      = { navController.navigate("login") { popUpTo(0) { inclusive = true } } },
                        onBackClick   = { navController.popBackStack() }
                    )
                }

                composable("wishlist") {
                    WishlistScreen(
                        wishlistViewModel = wishlistViewModel,
                        onStoreClick      = { store ->
                            val storeJson = URLEncoder.encode(Gson().toJson(store), StandardCharsets.UTF_8.toString())
                            navController.navigate("store_detail/$storeJson")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("support") {
                    SupportScreen(onBack = { navController.popBackStack() })
                }

                // ✅ Search route add kiya
                composable("search") {
                    SearchScreen(onBack = { navController.popBackStack() })
                }

                composable("results/{id}/{title}") { backStackEntry ->
                    val id    = backStackEntry.arguments?.getString("id")    ?: ""
                    val title = backStackEntry.arguments?.getString("title") ?: ""
                    ResultList(
                        id          = id,
                        title       = title,
                        onBackClick = { navController.popBackStack() },
                        onStoreClick = { store ->
                            val storeJson = URLEncoder.encode(Gson().toJson(store), StandardCharsets.UTF_8.toString())
                            navController.navigate("store_detail/$storeJson")
                        }
                    )
                }

                composable("all_categories") {
                    AllCategoriesScreen(
                        wishlistViewModel = wishlistViewModel,
                        onBack           = { navController.popBackStack() },
                        onStoreClick     = { store ->
                            val storeJson = URLEncoder.encode(Gson().toJson(store), StandardCharsets.UTF_8.toString())
                            navController.navigate("store_detail/$storeJson")
                        }
                    )
                }

                composable("store_detail/{storeJson}") { backStackEntry ->
                    val encoded   = backStackEntry.arguments?.getString("storeJson") ?: ""
                    val storeJson = URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
                    val store     = Gson().fromJson(storeJson, StoreModel::class.java)
                    StoreDetailScreen(
                        store             = store,
                        onBack            = { navController.popBackStack() },
                        wishlistViewModel = wishlistViewModel
                    )
                }

                composable("map/{storeJson}") { backStackEntry ->
                    val encoded   = backStackEntry.arguments?.getString("storeJson") ?: ""
                    val storeJson = URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
                    val store     = Gson().fromJson(storeJson, StoreModel::class.java)
                    MapScreen(store = store)
                }
            }

            LaunchedEffect(navReady, authState) {
                if (!navReady) return@LaunchedEffect
                when (authState) {
                    is AuthState.Success -> {
                        val userType = (authState as AuthState.Success).userType
                        when (userType) {
                            "store_owner" -> navController.navigate("store_dashboard") { popUpTo(0) { inclusive = true } }
                            "admin"       -> navController.navigate("admin_dashboard")  { popUpTo(0) { inclusive = true } }
                            else          -> navController.navigate("user_dashboard")   { popUpTo(0) { inclusive = true } }
                        }
                    }
                    is AuthState.Idle -> {
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    }
                    else -> {}
                }
            }
        }
    }
}