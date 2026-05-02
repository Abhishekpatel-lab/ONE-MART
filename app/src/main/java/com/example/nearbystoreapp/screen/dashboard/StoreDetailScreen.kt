package com.example.nearbystoreapp.screen.dashboard

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nearbystoreapp.R
import com.example.nearbystoreapp.model.StoreModel
import com.example.nearbystoreapp.viewModel.WishlistViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.FirebaseDatabase
import com.google.maps.android.compose.*

data class ItemModel(
    val name: String = "",
    val price: String = "",
    val unit: String = "",
    val available: Boolean = true,
    val imagePath: String = ""
)

fun getDetailSectionTitle(category: String): String =
    if (category.equals("Foods", ignoreCase = true)) "🍽️ Menu" else "📦 Items"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDetailScreen(
    store: StoreModel,
    onBack: () -> Unit = {},
    wishlistViewModel: WishlistViewModel
) {
    val context = LocalContext.current
    val storeLatLng = LatLng(store.latitude, store.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(storeLatLng, 15f)
    }

    val items = remember { mutableStateListOf<ItemModel>() }
    var itemsLoading by remember { mutableStateOf(true) }

    val wishlistKeys by wishlistViewModel.wishlistKeys.collectAsState()
    val isWishlisted = wishlistKeys.contains(store.firebaseKey)

    LaunchedEffect(store.firebaseKey) {
        if (store.firebaseKey.isNotEmpty()) {
            FirebaseDatabase.getInstance().reference
                .child("Stores")
                .child(store.firebaseKey)
                .child("items")
                .get()
                .addOnSuccessListener { snapshot ->
                    val itemsList = mutableListOf<ItemModel>()
                    for (item in snapshot.children) {
                        val name = item.child("name").value?.toString() ?: ""
                        if (name.isNotEmpty()) {
                            itemsList.add(
                                ItemModel(
                                    name      = name,
                                    price     = item.child("price").value?.toString() ?: "",
                                    unit      = item.child("unit").value?.toString() ?: "",
                                    available = item.child("available").value as? Boolean ?: true,
                                    imagePath = item.child("imagePath").value?.toString() ?: ""
                                )
                            )
                        }
                    }
                    items.clear()
                    items.addAll(itemsList)
                    itemsLoading = false
                }
                .addOnFailureListener { itemsLoading = false }
        } else {
            itemsLoading = false
        }
    }

    Scaffold(
        containerColor = colorResource(R.color.black2),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        store.title,
                        color = colorResource(R.color.gold),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
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
                actions = {
                    IconButton(onClick = { wishlistViewModel.toggleWishlist(store) }) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Wishlist",
                            tint = if (isWishlisted) Color.Red else colorResource(R.color.gold)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(R.color.black2)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ─── Store Image ──────────────────────────────
            item {
                if (store.imagePath.isNotEmpty()) {
                    AsyncImage(
                        model = store.imagePath,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // ─── Store Info ───────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        store.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(emoji = "📍", text = store.address)
                    Spacer(modifier = Modifier.height(4.dp))
                    InfoRow(emoji = "🕐", text = store.hours)
                    Spacer(modifier = Modifier.height(4.dp))

                    // ✅ Call number — clickable, dialer open hoga
                    InfoRow(
                        emoji = "📞",
                        text = store.call,
                        isPhone = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    InfoRow(emoji = "🟢", text = store.activity)
                }
                HorizontalDivider(color = Color.DarkGray, thickness = 1.dp)
            }

            // ─── Items/Menu Title ─────────────────────────
            item {
                Text(
                    text = getDetailSectionTitle(store.category),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.gold),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // ─── Items List ───────────────────────────────
            when {
                itemsLoading -> {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = colorResource(R.color.gold),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                items.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(
                                    colorResource(R.color.black3),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (store.category.equals("Foods", ignoreCase = true))
                                    "Menu abhi available nahi hai 🔜"
                                else "Items abhi available nahi hain 🔜",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                else -> {
                    items(items) { item -> ItemCard(item = item) }
                }
            }

            // ─── Map Section ──────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.DarkGray, thickness = 1.dp)
                Text(
                    text = "📍 Store Location",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.gold),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(200.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            scrollGesturesEnabled = false,
                            zoomGesturesEnabled = false
                        )
                    ) {
                        Marker(
                            state = MarkerState(position = storeLatLng),
                            title = store.title,
                            snippet = store.shortAddress
                        )
                    }
                    // ✅ Map tap → Google Maps open
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                val uri = Uri.parse(
                                    "geo:${store.latitude},${store.longitude}?" +
                                            "q=${store.latitude},${store.longitude}(${store.title})"
                                )
                                val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                    setPackage("com.google.android.apps.maps")
                                }
                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                } else {
                                    val browserUri = Uri.parse(
                                        "https://www.google.com/maps/search/?api=1" +
                                                "&query=${store.latitude},${store.longitude}"
                                    )
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, browserUri)
                                    )
                                }
                            }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "🗺️ Tap map to open in Google Maps",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// ─── InfoRow — Phone clickable ────────────────────────────
@Composable
fun InfoRow(
    emoji: String,
    text: String,
    isPhone: Boolean = false
) {
    if (text.isEmpty()) return
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (isPhone) {
            Modifier
                .clickable {
                    // ✅ Dialer open karo
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$text"))
                    context.startActivity(intent)
                }
                .padding(vertical = 4.dp)
        } else {
            Modifier.padding(vertical = 2.dp)
        }
    ) {
        Text(text = emoji, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            // ✅ Phone number gold + underline
            color = if (isPhone) colorResource(R.color.gold) else Color.Gray,
            fontSize = 13.sp,
            fontWeight = if (isPhone) FontWeight.SemiBold else FontWeight.Normal,
            textDecoration = if (isPhone) TextDecoration.Underline else TextDecoration.None
        )
        if (isPhone && text.isNotEmpty()) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "📲", fontSize = 12.sp)
        }
    }
}

// ─── Item Card ────────────────────────────────────────────
@Composable
fun ItemCard(item: ItemModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .background(colorResource(R.color.black3), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.imagePath.isNotEmpty()) {
            AsyncImage(
                model = item.imagePath,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (item.unit.isNotEmpty()) {
                Text(text = item.unit, color = Color.Gray, fontSize = 12.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            if (item.price.isNotEmpty()) {
                Text(
                    "₹${item.price}",
                    color = colorResource(R.color.gold),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = if (item.available) "✅ Available" else "❌ Unavailable",
                color = if (item.available) Color.Green else Color.Red,
                fontSize = 11.sp
            )
        }
    }
}