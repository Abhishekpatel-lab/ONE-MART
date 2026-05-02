package com.example.nearbystoreapp.screen.wishlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
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
import com.example.nearbystoreapp.model.StoreModel
import com.example.nearbystoreapp.viewModel.WishlistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    wishlistViewModel: WishlistViewModel,
    onStoreClick: (StoreModel) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val stores by wishlistViewModel.wishlistStores.collectAsState()

    LaunchedEffect(Unit) { wishlistViewModel.loadWishlist() }

    Scaffold(
        containerColor = colorResource(R.color.black2),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "❤️ Wishlist",
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
        if (stores.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Wishlist khali hai", color = Color.Gray, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Stores pe heart button dabao",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                item {
                    Text(
                        "${stores.size} Saved Stores",
                        color = colorResource(R.color.gold),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                items(stores, key = { it.firebaseKey }) { store ->
                    WishlistStoreCard(
                        store = store,
                        onStoreClick = { onStoreClick(store) },
                        onRemove = { wishlistViewModel.toggleWishlist(store) }
                    )
                }
            }
        }
    }
}

@Composable
fun WishlistStoreCard(
    store: StoreModel,
    onStoreClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.black3), RoundedCornerShape(14.dp))
            .clickable { onStoreClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (store.imagePath.isNotEmpty()) {
            AsyncImage(
                model = store.imagePath,
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                store.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("📍 ${store.shortAddress}", color = Color.Gray, fontSize = 12.sp)
            Text("🕐 ${store.hours}", color = Color.Gray, fontSize = 12.sp)
            Text(store.activity, color = colorResource(R.color.gold), fontSize = 11.sp)
        }
        // ✅ Remove button
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = "Remove",
                tint = Color.Red,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}