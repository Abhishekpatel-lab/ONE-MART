package com.example.nearbystoreapp.screen.dashboard


import androidx.compose.foundation.background
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

// ---- Data classes ----
data class StoreResult(
    val id: String,
    val name: String,
    val category: String,
    val rating: Double = 0.0,
    val orderCount: Int = 0
)

data class ProductResult(
    val id: String,
    val name: String,
    val storeName: String,
    val price: String
)

// ---- Category keyword map ----
val categoryKeywords = mapOf(
    "grocery" to listOf("grocery", "groceries", "sabzi", "vegetable", "fruit", "ration"),
    "dairy" to listOf("dairy", "milk", "doodh", "paneer", "curd", "dahi", "cheese", "butter"),
    "bakery" to listOf("bakery", "bread", "cake", "biscuit", "roti", "paav"),
    "pharmacy" to listOf("pharmacy", "medicine", "dawai", "medical", "tablet", "chemist"),
    "clothing" to listOf("clothing", "clothes", "shirt", "pant", "dress", "kapda", "fashion"),
    "electronics" to listOf("electronics", "mobile", "phone", "laptop", "tv", "charger"),
    "restaurant" to listOf("restaurant", "food", "khana", "biryani", "pizza", "burger", "cafe"),
    "stationery" to listOf("stationery", "pen", "pencil", "copy", "notebook", "book"),
    "hardware" to listOf("hardware", "tools", "screw", "pipe", "cement", "paint")
)

// Query se category guess karo
fun guessCategory(query: String): String? {
    val q = query.lowercase()
    for ((category, keywords) in categoryKeywords) {
        if (keywords.any { q.contains(it) }) return category
    }
    return null
}

@Composable
fun SearchScreen(
    onBack: () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var storeResults by remember { mutableStateOf<List<StoreResult>>(emptyList()) }
    var productResults by remember { mutableStateOf<List<ProductResult>>(emptyList()) }
    var similarStores by remember { mutableStateOf<List<StoreResult>>(emptyList()) }
    var popularStores by remember { mutableStateOf<List<StoreResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var noResultFound by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(query) {
        if (query.length < 2) {
            storeResults = emptyList()
            productResults = emptyList()
            similarStores = emptyList()
            popularStores = emptyList()
            noResultFound = false
            return@LaunchedEffect
        }

        isLoading = true
        noResultFound = false
        val db = FirebaseDatabase.getInstance().reference
        val queryLower = query.lowercase()
        val guessedCategory = guessCategory(query)

        // --- Stores Search ---
        db.child("stores").get().addOnSuccessListener { snapshot ->
            val stores = mutableListOf<StoreResult>()
            val similar = mutableListOf<StoreResult>()
            val popular = mutableListOf<StoreResult>()

            for (store in snapshot.children) {
                val name = store.child("name").value?.toString() ?: continue
                val category = store.child("category").value?.toString() ?: ""
                val rating = store.child("rating").value?.toString()?.toDoubleOrNull() ?: 0.0
                val orderCount = store.child("orderCount").value?.toString()?.toIntOrNull() ?: 0

                val storeObj = StoreResult(
                    id = store.key ?: "",
                    name = name,
                    category = category,
                    rating = rating,
                    orderCount = orderCount
                )

                when {
                    // Exact match
                    name.lowercase().contains(queryLower) ||
                            category.lowercase().contains(queryLower) -> {
                        stores.add(storeObj)
                    }
                    // Same category (guessed)
                    guessedCategory != null &&
                            category.lowercase().contains(guessedCategory) -> {
                        similar.add(storeObj)
                    }
                    // Popular fallback
                    else -> {
                        if (rating >= 4.0 || orderCount >= 50) {
                            popular.add(storeObj)
                        }
                    }
                }
            }

            storeResults = stores
            similarStores = similar.sortedByDescending { it.rating }
            popularStores = popular
                .sortedWith(compareByDescending<StoreResult> { it.rating }
                    .thenByDescending { it.orderCount })
                .take(5)

            isLoading = false
        }

        // --- Products Search ---
        db.child("products").get().addOnSuccessListener { snapshot ->
            val products = mutableListOf<ProductResult>()
            for (product in snapshot.children) {
                val name = product.child("name").value?.toString() ?: continue
                val storeName = product.child("storeName").value?.toString() ?: ""
                val price = product.child("price").value?.toString() ?: ""
                if (name.lowercase().contains(queryLower)) {
                    products.add(ProductResult(product.key ?: "", name, storeName, price))
                }
            }
            productResults = products
            noResultFound = storeResults.isEmpty() && products.isEmpty()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.black2))
    ) {
        // ---- Top Search Bar ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.black2))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colorResource(R.color.gold)
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(colorResource(R.color.black3), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🔍", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Search stores, products...",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                        innerTextField()
                    },
                    singleLine = true
                )
            }
        }

        // ---- Loading ----
        if (isLoading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colorResource(R.color.gold))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {

                // ---- Exact Match: Stores ----
                if (storeResults.isNotEmpty()) {
                    item { SectionHeader(title = "🏪 Stores") }
                    items(storeResults) { store ->
                        StoreCard(store = store)
                    }
                }

                // ---- Exact Match: Products ----
                if (productResults.isNotEmpty()) {
                    item { SectionHeader(title = "📦 Products") }
                    items(productResults) { product ->
                        ProductCard(product = product)
                    }
                }

                // ---- No Exact Match ----
                if (noResultFound) {
                    item { NoResultBanner(query = query) }

                    if (similarStores.isNotEmpty()) {
                        item { SectionHeader(title = "🔎 Similar Stores") }
                        items(similarStores) { store ->
                            StoreCard(store = store, showCategory = true)
                        }
                    }

                    if (popularStores.isNotEmpty()) {
                        item { SectionHeader(title = "⭐ Popular Stores") }
                        items(popularStores) { store ->
                            StoreCard(store = store, showRating = true)
                        }
                    }

                    if (similarStores.isEmpty() && popularStores.isEmpty()) {
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Koi store available nahi hai abhi 😔",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

// ---- Reusable Composables ----

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = colorResource(R.color.gold),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
fun NoResultBanner(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(colorResource(R.color.black3), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "\"$query\" nahi mila 😕",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Yahan kuch similar options hain jo shayad kaam aayein 👇",
            color = Color.Gray,
            fontSize = 13.sp
        )
    }
}

@Composable
fun StoreCard(
    store: StoreResult,
    showCategory: Boolean = false,
    showRating: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .background(colorResource(R.color.black3), RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = store.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (showCategory && store.category.isNotEmpty()) {
                Text(
                    text = store.category,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
        if (showRating && store.rating > 0) {
            Text(
                text = "⭐ ${store.rating}",
                color = colorResource(R.color.gold),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProductCard(product: ProductResult) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .background(colorResource(R.color.black3), RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "by ${product.storeName}",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        Text(
            text = "₹${product.price}",
            color = colorResource(R.color.gold),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}