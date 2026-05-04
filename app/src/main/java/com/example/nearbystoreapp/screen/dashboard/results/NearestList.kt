package com.example.nearbystoreapp.screen.dashboard.results

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nearbystoreapp.R
import com.example.nearbystoreapp.model.StoreModel
import com.google.firebase.database.FirebaseDatabase
import kotlin.math.*

// ─── Distance calculate karo (km mein) ───────────────────
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}

// ─── Average Rating Firebase se load karo ────────────────
@Composable
fun rememberStoreRating(storeKey: String): Pair<Float, Int> {
    var avgRating by remember { mutableStateOf(0f) }
    var count by remember { mutableStateOf(0) }

    LaunchedEffect(storeKey) {
        FirebaseDatabase.getInstance().reference
            .child("reviews").child("store").child(storeKey)
            .get()
            .addOnSuccessListener { snapshot ->
                val ratings = mutableListOf<Int>()
                for (child in snapshot.children) {
                    val r = child.child("rating").value?.toString()?.toIntOrNull()
                    if (r != null) ratings.add(r)
                }
                count = ratings.size
                avgRating = if (ratings.isEmpty()) 0f else ratings.average().toFloat()
            }
    }
    return Pair(avgRating, count)
}

// ─── Nearest Stores List ──────────────────────────────────
@Composable
fun NearestLisst(
    list: SnapshotStateList<StoreModel>,
    showNearestLoading: Boolean,
    onStoreClick: (StoreModel) -> Unit,
    userLat: Double = 0.0,
    userLon: Double = 0.0,
    searchQuery: String = ""
) {
    val filtered = if (searchQuery.isEmpty()) list
    else list.filter { it.title.contains(searchQuery, ignoreCase = true) || it.address.contains(searchQuery, ignoreCase = true) }

    Column {
        // ─── Nearest Stores Header ─────────────────────
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nearest Stores",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { }
            ) {
                Text(
                    text = "See all",
                    color = colorResource(R.color.gold),
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.Underline
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = colorResource(R.color.gold),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (showNearestLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colorResource(R.color.gold))
            }
        } else if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Koi store nahi mila 🏪", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 2000.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(filtered, key = { it.firebaseKey }) { store ->
                    val distanceKm = if (userLat != 0.0 && userLon != 0.0 && store.latitude != 0.0 && store.longitude != 0.0) {
                        calculateDistance(userLat, userLon, store.latitude, store.longitude)
                    } else null

                    val isFirst = filtered.indexOf(store) == 0

                    StoreCard(
                        store = store,
                        distanceKm = distanceKm,
                        isBestMatch = isFirst,
                        onClick = { onStoreClick(store) }
                    )
                }
            }
        }
    }
}

// ─── Beautiful Store Card ─────────────────────────────────
@Composable
fun StoreCard(
    store: StoreModel,
    distanceKm: Double?,
    isBestMatch: Boolean,
    onClick: () -> Unit
) {
    val (avgRating, ratingCount) = rememberStoreRating(store.firebaseKey)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // ─── Left: Store Image ─────────────────────
            Box(modifier = Modifier.width(130.dp).height(150.dp)) {
                AsyncImage(
                    model = store.imagePath,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)),
                    contentScale = ContentScale.Crop
                )
                // "Best Match" badge — sirf pehle store pe
                if (isBestMatch) {
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 10.dp),
                        color = colorResource(R.color.gold),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "Best Match",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // ─── Right: Store Info ─────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Store Name
                Text(
                    text = store.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // Rating + Distance
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (avgRating > 0) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = colorResource(R.color.gold),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format("%.1f", avgRating),
                            color = colorResource(R.color.gold),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (ratingCount > 0) {
                            Text(
                                text = " ($ratingCount)",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                        if (distanceKm != null) {
                            Text(text = "  •  ", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                    if (distanceKm != null) {
                        Text(
                            text = if (distanceKm < 1.0) "${(distanceKm * 1000).toInt()} m"
                            else String.format("%.1f km", distanceKm),
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                // Address
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(14.dp).padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = store.address,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                // Open Now badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1B5E20)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(Color.Green, RoundedCornerShape(50))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = store.activity,
                            color = Color.Green,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Hours
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🕐", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = store.hours,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            // ─── Arrow button ──────────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 10.dp)
                    .size(32.dp)
                    .background(Color(0xFF2A2A2A), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = colorResource(R.color.gold),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}