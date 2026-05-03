package com.example.nearbystoreapp.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nearbystoreapp.R
import com.example.nearbystoreapp.domain.CategoryModel
import com.example.nearbystoreapp.model.StoreModel
import com.example.nearbystoreapp.repository.DashboardRepository
import com.example.nearbystoreapp.viewModel.WishlistViewModel
import com.google.firebase.database.FirebaseDatabase

// ─── Data class: category + uske stores ──────────────────
data class CategoryWithStores(
    val category: CategoryModel,
    val stores: List<StoreModel>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCategoriesScreen(
    wishlistViewModel: WishlistViewModel,
    onBack: () -> Unit = {},
    onStoreClick: (StoreModel) -> Unit = {}
) {
    val dashboardRepository = remember { DashboardRepository() }

    // Categories aur stores ka combined data
    var categoryWithStoresList by remember { mutableStateOf<List<CategoryWithStores>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val wishlistKeys by wishlistViewModel.wishlistKeys.collectAsState()
    LaunchedEffect(Unit) { wishlistViewModel.loadWishlist() }

    // ─── Pehle categories load karo, phir har category ke stores ───
    LaunchedEffect(Unit) {
        dashboardRepository.loadCategory().observeForever { categories ->
            if (categories.isEmpty()) {
                isLoading = false
                return@observeForever
            }

            // Sabhi stores ek baar mein load karo
            FirebaseDatabase.getInstance().reference
                .child("Stores")
                .get()
                .addOnSuccessListener { snapshot ->

                    // Sabhi stores parse karo
                    val allStores = mutableListOf<StoreModel>()
                    for (store in snapshot.children) {
                        val parsed = parseStore(store)
                        if (!parsed.isBlocked) allStores.add(parsed)
                    }

                    // Har category ke liye uske stores filter karo — sequence maintain karo
                    val result = categories.map { category ->
                        val categoryStores = allStores.filter { store ->
                            store.categoryId == category.Id.toString()
                        }
                        CategoryWithStores(category = category, stores = categoryStores)
                    }.filter { it.stores.isNotEmpty() } // sirf woh categories dikhao jisme stores hain

                    categoryWithStoresList = result
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
        }
    }

    Scaffold(
        containerColor = colorResource(R.color.black2),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "All Categories",
                        color = colorResource(R.color.gold),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colorResource(R.color.gold)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(R.color.black2)
                )
            )
        }
    ) { padding ->

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colorResource(R.color.gold))
            }
        } else if (categoryWithStoresList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏪", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Koi store nahi mila", color = Color.Gray, fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // ─── Har category ek section ───────────────
                categoryWithStoresList.forEach { categoryWithStores ->

                    // Category Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Category image (chhota icon)
                                if (categoryWithStores.category.ImagePath.isNotEmpty()) {
                                    AsyncImage(
                                        model = categoryWithStores.category.ImagePath,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = categoryWithStores.category.Name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorResource(R.color.gold)
                                )
                            }
                            // Store count badge
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = colorResource(R.color.gold).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${categoryWithStores.stores.size} stores",
                                    color = colorResource(R.color.gold),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Divider line
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color(0xFF2A2A2A),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Is category ke sabhi stores
                    items(categoryWithStores.stores, key = { it.firebaseKey }) { store ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            StoreListCard(
                                store = store,
                                isWishlisted = wishlistKeys.contains(store.firebaseKey),
                                onWishlistToggle = { wishlistViewModel.toggleWishlist(store) },
                                onClick = { onStoreClick(store) }
                            )
                        }
                    }

                    // Section ke baad thoda space
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}