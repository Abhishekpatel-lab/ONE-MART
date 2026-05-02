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
import com.example.nearbystoreapp.model.StoreModel
import com.example.nearbystoreapp.viewModel.WishlistViewModel
import com.google.firebase.database.FirebaseDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryStoreListScreen(
    categoryId: String,
    categoryTitle: String,
    wishlistViewModel: WishlistViewModel,
    onBack: () -> Unit = {},
    onStoreClick: (StoreModel) -> Unit = {}
) {
    var stores by remember { mutableStateOf<List<StoreModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val wishlistKeys by wishlistViewModel.wishlistKeys.collectAsState()

    LaunchedEffect(Unit) { wishlistViewModel.loadWishlist() }

    LaunchedEffect(categoryId) {
        FirebaseDatabase.getInstance().reference
            .child("Stores")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<StoreModel>()
                for (store in snapshot.children) {
                    val storeCatId = store.child("CategoryId").value?.toString() ?: ""
                    if (storeCatId == categoryId) {
                        val parsedStore = parseStore(store)
                        if (!parsedStore.isBlocked) {
                            list.add(parsedStore)
                        }
                    }
                }
                stores = list
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    Scaffold(
        containerColor = colorResource(R.color.black2),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = categoryTitle,
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
        } else if (stores.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🏪", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Is category mein koi store nahi hai", color = Color.Gray, fontSize = 15.sp)
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
                items(stores, key = { it.firebaseKey }) { store ->
                    StoreListCard(
                        store = store,
                        isWishlisted = wishlistKeys.contains(store.firebaseKey),
                        onWishlistToggle = { wishlistViewModel.toggleWishlist(store) },
                        onClick = { onStoreClick(store) }
                    )
                }
            }
        }
    }
}

fun parseStore(store: com.google.firebase.database.DataSnapshot): StoreModel {
    return StoreModel(
        firebaseKey  = store.key ?: "",
        id           = store.child("Id").value?.toString() ?: "",
        title        = store.child("Title").value?.toString() ?: "",
        address      = store.child("Address").value?.toString() ?: "",
        shortAddress = store.child("ShortAddress").value?.toString() ?: "",
        imagePath    = store.child("ImagePath").value?.toString() ?: "",
        latitude     = store.child("Latitude").value?.toString()?.toDoubleOrNull() ?: 0.0,
        longitude    = store.child("Longitude").value?.toString()?.toDoubleOrNull() ?: 0.0,
        hours        = store.child("Hours").value?.toString() ?: "",
        call         = store.child("Call").value?.toString() ?: "",
        activity     = store.child("Activity").value?.toString() ?: "",
        categoryId   = store.child("CategoryId").value?.toString() ?: "",
        isBlocked    = store.child("isBlocked").getValue(Boolean::class.java) ?: false
    )
}

@Composable
fun StoreListCard(
    store: StoreModel,
    isWishlisted: Boolean,
    onWishlistToggle: () -> Unit,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.black3), RoundedCornerShape(14.dp))
                .clickable { onClick() }
        ) {
            if (store.imagePath.isNotEmpty()) {
                AsyncImage(
                    model = store.imagePath,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(store.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📍", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(store.shortAddress, color = Color.Gray, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🕐", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(store.hours, color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(store.activity, color = colorResource(R.color.gold), fontSize = 11.sp)
            }
        }

        // ✅ Heart button — top right corner
        IconButton(
            onClick = onWishlistToggle,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(36.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
        ) {
            Icon(
                imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Wishlist",
                tint = if (isWishlisted) Color.Red else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}