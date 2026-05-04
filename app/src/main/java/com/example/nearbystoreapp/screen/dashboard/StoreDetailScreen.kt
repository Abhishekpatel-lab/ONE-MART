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
import androidx.compose.material.icons.filled.Star
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

// ✅ ItemModel NAHI hai yahan — ItemModel.kt mein hai

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
    var storeReviewRefresh by remember { mutableStateOf(0) }

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
                            itemsList.add(ItemModel(
                                name      = name,
                                price     = item.child("price").value?.toString() ?: "",
                                unit      = item.child("unit").value?.toString() ?: "",
                                available = item.child("available").value as? Boolean ?: true,
                                imagePath = item.child("imagePath").value?.toString() ?: "",
                                itemKey   = item.key ?: ""
                            ))
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
        containerColor = Color(0xFF0A0A0A),
        topBar = {
            TopAppBar(
                title = {
                    Text(store.title, color = colorResource(R.color.gold), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colorResource(R.color.gold))
                    }
                },
                actions = {
                    IconButton(onClick = { wishlistViewModel.toggleWishlist(store) }) {
                        Icon(
                            imageVector        = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Wishlist",
                            tint               = if (isWishlisted) Color.Red else colorResource(R.color.gold)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0A))
            )
        }
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            item {
                if (store.imagePath.isNotEmpty()) {
                    AsyncImage(
                        model              = store.imagePath,
                        contentDescription = null,
                        modifier           = Modifier.fillMaxWidth().height(240.dp),
                        contentScale       = ContentScale.Crop
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111111))
                        .padding(16.dp)
                ) {
                    Text(store.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = colorResource(R.color.gold), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reviews", color = Color.Gray, fontSize = 13.sp)
                        }
                        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF1B5E20)) {
                            Row(
                                modifier          = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(8.dp).background(Color(0xFF4CAF50), RoundedCornerShape(50)))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(store.activity.ifEmpty { "Open" }, color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier              = Modifier.fillMaxWidth().background(Color(0xFF111111)).padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📍", fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(store.address, color = Color.Gray, fontSize = 10.sp, lineHeight = 13.sp, maxLines = 3)
                    }
                    Box(modifier = Modifier.width(1.dp).height(60.dp).background(Color.DarkGray))
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🕐", fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(store.hours, color = Color.Gray, fontSize = 10.sp, lineHeight = 13.sp)
                        Text("All Days", color = Color.Gray, fontSize = 9.sp)
                    }
                    Box(modifier = Modifier.width(1.dp).height(60.dp).background(Color.DarkGray))
                    Column(
                        modifier = Modifier.weight(1f).clickable {
                            if (store.call.isNotEmpty()) {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${store.call}")))
                            }
                        },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📞", fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(store.call.ifEmpty { "N/A" }, color = colorResource(R.color.gold), fontSize = 10.sp, lineHeight = 13.sp)
                        Text("Call Store", color = Color.Gray, fontSize = 9.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick  = {
                            if (store.call.isNotEmpty()) {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${store.call}")))
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.gold))
                    ) {
                        Text("📞", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Button(
                        onClick  = {
                            if (store.call.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://wa.me/91${store.call}?text=Hello, I need information about your store")
                                }
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Text("💬", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    RatingBarChart(
                        targetType     = "store",
                        targetId       = store.firebaseKey,
                        refreshTrigger = storeReviewRefresh
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ReviewInputSection(
                        targetType        = "store",
                        targetId          = store.firebaseKey,
                        targetName        = store.title,
                        onReviewSubmitted = { storeReviewRefresh++ }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Top Reviews", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    ReviewsSection(
                        targetType     = "store",
                        targetId       = store.firebaseKey,
                        refreshTrigger = storeReviewRefresh
                    )
                }
                HorizontalDivider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.padding(top = 16.dp))
            }

            item {
                Text(
                    text       = getDetailSectionTitle(store.category),
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color      = colorResource(R.color.gold),
                    modifier   = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            when {
                itemsLoading -> {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colorResource(R.color.gold), modifier = Modifier.size(24.dp))
                        }
                    }
                }
                items.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(Color(0xFF1A1A1A), RoundedCornerShape(10.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text     = if (store.category.equals("Foods", ignoreCase = true)) "Menu abhi available nahi hai 🔜" else "Items abhi available nahi hain 🔜",
                                color    = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                else -> {
                    items(items) { item ->
                        ItemCardWithReview(item = item, storeKey = store.firebaseKey)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.DarkGray, thickness = 1.dp)
                Text(
                    "📍 Store Location",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color      = colorResource(R.color.gold),
                    modifier   = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(200.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    GoogleMap(
                        modifier            = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings          = MapUiSettings(
                            zoomControlsEnabled   = false,
                            scrollGesturesEnabled = false,
                            zoomGesturesEnabled   = false
                        )
                    ) {
                        Marker(
                            state   = MarkerState(position = storeLatLng),
                            title   = store.title,
                            snippet = store.shortAddress
                        )
                    }
                    Box(modifier = Modifier.fillMaxSize().clickable {
                        val uri = Uri.parse("geo:${store.latitude},${store.longitude}?q=${store.latitude},${store.longitude}(${store.title})")
                        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)
                        } else {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${store.latitude},${store.longitude}")))
                        }
                    })
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("🗺️ Tap map to open in Google Maps", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun RatingBarChart(targetType: String, targetId: String, refreshTrigger: Int) {
    var reviews   by remember { mutableStateOf<List<ReviewModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(targetId, refreshTrigger) {
        FirebaseDatabase.getInstance().reference
            .child("reviews").child(targetType).child(targetId).get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<ReviewModel>()
                for (child in snapshot.children) {
                    list.add(ReviewModel(
                        id         = child.key ?: "",
                        userId     = child.child("userId").value?.toString() ?: "",
                        userName   = child.child("userName").value?.toString() ?: "User",
                        rating     = child.child("rating").value?.toString()?.toIntOrNull() ?: 0,
                        comment    = child.child("comment").value?.toString() ?: "",
                        timestamp  = child.child("timestamp").value?.toString()?.toLongOrNull() ?: 0L,
                        targetType = targetType,
                        targetId   = targetId
                    ))
                }
                reviews   = list
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    if (isLoading || reviews.isEmpty()) return

    val avgRating  = reviews.map { it.rating }.average().toFloat()
    val totalCount = reviews.size
    val starCounts = (5 downTo 1).map { star -> star to reviews.count { it.rating == star } }

    Row(
        modifier          = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A), RoundedCornerShape(14.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
            Text(String.format("%.1f", avgRating), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
            Row {
                for (i in 1..5) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = if (i <= avgRating) colorResource(R.color.gold) else Color.Gray, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("($totalCount Reviews)", color = Color.Gray, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            starCounts.forEach { (star, count) ->
                val percent = if (totalCount > 0) (count.toFloat() / totalCount) else 0f
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = colorResource(R.color.gold), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$star", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.width(10.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.weight(1f).height(8.dp).background(Color(0xFF2A2A2A), RoundedCornerShape(4.dp))) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(percent).background(colorResource(R.color.gold), RoundedCornerShape(4.dp)))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${(percent * 100).toInt()}%", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.width(30.dp))
                }
            }
        }
    }
}

@Composable
fun ItemCardWithReview(item: ItemModel, storeKey: String) {
    var showReviews       by remember { mutableStateOf(false) }
    var itemReviewRefresh by remember { mutableStateOf(0) }
    val itemId = "${storeKey}_${item.itemKey}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .background(Color(0xFF1A1A1A), RoundedCornerShape(10.dp))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (item.imagePath.isNotEmpty()) {
                AsyncImage(model = item.imagePath, contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (item.unit.isNotEmpty()) Text(text = item.unit, color = Color.Gray, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (item.price.isNotEmpty()) Text("₹${item.price}", color = colorResource(R.color.gold), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(text = if (item.available) "✅ Available" else "❌ Unavailable", color = if (item.available) Color.Green else Color.Red, fontSize = 11.sp)
            }
        }
        if (item.itemKey.isNotEmpty()) {
            TextButton(onClick = { showReviews = !showReviews }, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(text = if (showReviews) "▲ Close Reviews" else "⭐ Show Reviews / Give", color = colorResource(R.color.gold), fontSize = 12.sp)
            }
            if (showReviews) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    ReviewInputSection(targetType = "item", targetId = itemId, targetName = item.name, onReviewSubmitted = { itemReviewRefresh++ })
                    Spacer(modifier = Modifier.height(10.dp))
                    ReviewsSection(targetType = "item", targetId = itemId, refreshTrigger = itemReviewRefresh)
                }
            }
        }
    }
}

@Composable
fun InfoRow(emoji: String, text: String, isPhone: Boolean = false) {
    if (text.isEmpty()) return
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = if (isPhone) Modifier.clickable {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$text")))
        }.padding(vertical = 4.dp) else Modifier.padding(vertical = 2.dp)
    ) {
        Text(text = emoji, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text       = text,
            color      = if (isPhone) colorResource(R.color.gold) else Color.Gray,
            fontSize   = 13.sp,
            fontWeight = if (isPhone) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun ItemCard(item: ItemModel) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(10.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.imagePath.isNotEmpty()) {
            AsyncImage(model = item.imagePath, contentDescription = null, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (item.unit.isNotEmpty()) Text(text = item.unit, color = Color.Gray, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            if (item.price.isNotEmpty()) Text("₹${item.price}", color = colorResource(R.color.gold), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(text = if (item.available) "✅ Available" else "❌ Unavailable", color = if (item.available) Color.Green else Color.Red, fontSize = 11.sp)
        }
    }
}