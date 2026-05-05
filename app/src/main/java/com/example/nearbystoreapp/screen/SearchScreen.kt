package com.example.nearbystoreapp.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nearbystoreapp.R
import com.google.firebase.database.FirebaseDatabase

data class StoreResult(
    val firebaseKey: String = "",
    val name: String = "",
    val category: String = "",
    val address: String = "",
    val rating: Double = 0.0,
    val imagePath: String = ""
)

data class ItemResult(
    val itemName: String = "",
    val price: String = "",
    val unit: String = "",
    val storeName: String = "",
    val storeKey: String = "",
    val imagePath: String = ""
)

@Composable
fun SearchScreen(onBack: () -> Unit = {}) {
    var query        by remember { mutableStateOf("") }
    var selectedTab  by remember { mutableStateOf(0) } // 0 = Stores, 1 = Items
    var storeResults by remember { mutableStateOf<List<StoreResult>>(emptyList()) }
    var itemResults  by remember { mutableStateOf<List<ItemResult>>(emptyList()) }
    var isLoading    by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // ── Search logic ──
    LaunchedEffect(query, selectedTab) {
        if (query.length < 2) {
            storeResults = emptyList()
            itemResults  = emptyList()
            return@LaunchedEffect
        }

        isLoading = true
        val queryLower = query.lowercase()
        val db = FirebaseDatabase.getInstance().reference

        db.child("Stores").get().addOnSuccessListener { snapshot ->
            val stores  = mutableListOf<StoreResult>()
            val items   = mutableListOf<ItemResult>()

            for (store in snapshot.children) {
                val storeName = store.child("Title").value?.toString()    ?: ""
                val category  = store.child("Category").value?.toString() ?: ""
                val address   = store.child("Address").value?.toString()  ?: ""
                val rating    = store.child("rating").value?.toString()?.toDoubleOrNull() ?: 0.0
                val imagePath = store.child("ImagePath").value?.toString() ?: ""
                val storeKey  = store.key ?: ""

                // ── Store search ──
                if (storeName.lowercase().contains(queryLower) ||
                    category.lowercase().contains(queryLower) ||
                    address.lowercase().contains(queryLower)
                ) {
                    stores.add(StoreResult(
                        firebaseKey = storeKey,
                        name        = storeName,
                        category    = category,
                        address     = address,
                        rating      = rating,
                        imagePath   = imagePath
                    ))
                }

                // ── Items search ──
                for (item in store.child("items").children) {
                    val itemName = item.child("name").value?.toString() ?: ""
                    val price    = item.child("price").value?.toString() ?: ""
                    val unit     = item.child("unit").value?.toString()  ?: ""
                    val itemImg  = item.child("imagePath").value?.toString() ?: ""

                    if (itemName.lowercase().contains(queryLower)) {
                        items.add(ItemResult(
                            itemName  = itemName,
                            price     = price,
                            unit      = unit,
                            storeName = storeName,
                            storeKey  = storeKey,
                            imagePath = itemImg
                        ))
                    }
                }
            }

            storeResults = stores
            itemResults  = items
            isLoading    = false
        }.addOnFailureListener { isLoading = false }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.black2))
    ) {
        // ── Search Bar ──
        Row(
            modifier          = Modifier.fillMaxWidth().background(colorResource(R.color.black2)).padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colorResource(R.color.gold))
            }
            Row(
                modifier          = Modifier.weight(1f).background(colorResource(R.color.black3), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🔍", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value         = query,
                    onValueChange = { query = it },
                    modifier      = Modifier.weight(1f).focusRequester(focusRequester),
                    textStyle     = TextStyle(color = Color.White, fontSize = 14.sp),
                    singleLine    = true,
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) Text("Search stores, items...", fontSize = 14.sp, color = Color.Gray)
                        innerTextField()
                    }
                )
                if (query.isNotEmpty()) {
                    Text(
                        text     = "✕",
                        color    = Color.Gray,
                        fontSize = 16.sp,
                        modifier = Modifier.clickable { query = "" }
                    )
                }
            }
        }

        // ── Tabs — Stores | Items ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("🏪 Stores", "📦 Items").forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) colorResource(R.color.gold) else colorResource(R.color.black3),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = label,
                        color      = if (isSelected) Color.Black else Color.Gray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize   = 14.sp
                    )
                }
            }
        }

        // ── Results ──
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorResource(R.color.gold))
                }
            }
            query.length < 2 -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🔍", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text     = if (selectedTab == 0) "Search stores..." else "Search items...",
                            color    = Color.Gray,
                            fontSize = 15.sp
                        )
                    }
                }
            }
            selectedTab == 0 -> {
                // ── Stores Tab ──
                if (storeResults.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "😕", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("\"$query\" store nahi mila", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier       = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                "${storeResults.size} store mile",
                                color    = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(storeResults) { store ->
                            SearchStoreCard(store = store)
                        }
                    }
                }
            }
            else -> {
                // ── Items Tab ──
                if (itemResults.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "😕", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("\"$query\" item nahi mila", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier       = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                "${itemResults.size} item mile",
                                color    = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(itemResults) { item ->
                            SearchItemCard(item = item)
                        }
                    }
                }
            }
        }
    }
}

// ── Store Card ────────────────────────────────────────────
@Composable
fun SearchStoreCard(store: StoreResult) {
    Row(
        modifier          = Modifier.fillMaxWidth().background(colorResource(R.color.black3), RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(store.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (store.category.isNotEmpty()) {
                Text(store.category, color = colorResource(R.color.gold), fontSize = 12.sp)
            }
            if (store.address.isNotEmpty()) {
                Text("📍 ${store.address}", color = Color.Gray, fontSize = 11.sp, maxLines = 1)
            }
        }
        if (store.rating > 0) {
            Column(horizontalAlignment = Alignment.End) {
                Text("⭐ ${String.format("%.1f", store.rating)}", color = colorResource(R.color.gold), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Item Card ─────────────────────────────────────────────
@Composable
fun SearchItemCard(item: ItemResult) {
    Row(
        modifier          = Modifier.fillMaxWidth().background(colorResource(R.color.black3), RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.itemName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (item.unit.isNotEmpty()) {
                Text(item.unit, color = Color.Gray, fontSize = 12.sp)
            }
            Text("🏪 ${item.storeName}", color = colorResource(R.color.gold), fontSize = 12.sp)
        }
        if (item.price.isNotEmpty()) {
            Text("₹${item.price}", color = colorResource(R.color.gold), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}