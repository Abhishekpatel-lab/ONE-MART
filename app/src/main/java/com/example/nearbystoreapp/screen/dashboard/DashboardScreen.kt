package com.example.nearbystoreapp.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nearbystoreapp.R
import com.example.nearbystoreapp.domain.BannerModel
import com.example.nearbystoreapp.domain.CategoryModel
import com.example.nearbystoreapp.model.StoreModel
import com.example.nearbystoreapp.repository.DashboardRepository
import com.example.nearbystoreapp.repository.ResultsRepository
import com.example.nearbystoreapp.viewModel.AuthViewModel
import com.example.nearbystoreapp.viewModel.WishlistViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

data class DealItem(
    val name: String = "",
    val price: String = "",
    val unit: String = "",
    val imagePath: String = "",
    val storeName: String = ""
)

@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel,
    wishlistViewModel: WishlistViewModel,
    onCategoryClick: (String, String) -> Unit,
    onProfileClick: () -> Unit,
    onStoreClick: (StoreModel) -> Unit,
    onWishlistClick: () -> Unit,
    onSupportClick: () -> Unit,
    onCategoriesTabClick: () -> Unit
) {
    val dashboardRepository = remember { DashboardRepository() }
    val resultsRepository = remember { ResultsRepository() }

    val banners = remember { mutableStateListOf<BannerModel>() }
    val categories = remember { mutableStateListOf<CategoryModel>() }
    val dealItems = remember { mutableStateListOf<DealItem>() }

    var showBannerLoading by remember { mutableStateOf(true) }
    var showCategoryLoading by remember { mutableStateOf(true) }
    var showDealsLoading by remember { mutableStateOf(true) }
    var userName by remember { mutableStateOf("User") }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        uid?.let {
            FirebaseDatabase.getInstance().reference
                .child("users").child(it).get()
                .addOnSuccessListener { snapshot ->
                    userName = snapshot.child("name").value?.toString() ?: "User"
                }
        }
    }

    LaunchedEffect(Unit) {
        dashboardRepository.loadBanner().observeForever {
            banners.clear()
            banners.addAll(it)
            showBannerLoading = false
        }
    }

    LaunchedEffect(Unit) {
        dashboardRepository.loadCategory().observeForever {
            categories.clear()
            categories.addAll(it)
            showCategoryLoading = false
        }
    }

    LaunchedEffect(Unit) {
        FirebaseDatabase.getInstance().reference
            .child("Stores").get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<DealItem>()
                for (store in snapshot.children) {
                    val isBlocked = store.child("isBlocked").getValue(Boolean::class.java) ?: false
                    if (isBlocked) continue
                    val storeName = store.child("Title").value?.toString() ?: ""
                    for (item in store.child("items").children) {
                        val name = item.child("name").value?.toString() ?: ""
                        val price = item.child("price").value?.toString() ?: ""
                        if (name.isNotEmpty() && price.isNotEmpty()) {
                            list.add(DealItem(
                                name = name,
                                price = price,
                                unit = item.child("unit").value?.toString() ?: "",
                                imagePath = item.child("imagePath").value?.toString() ?: "",
                                storeName = storeName
                            ))
                        }
                    }
                }
                dealItems.clear()
                dealItems.addAll(list.take(10))
                showDealsLoading = false
            }
            .addOnFailureListener { showDealsLoading = false }
    }

    Scaffold(
        containerColor = Color(0xFF0A0A0A),
        bottomBar = {
            NewBottomBar(
                onHomeClick = {},
                onCategoryClick = onCategoriesTabClick,
                onWishlistClick = onWishlistClick,
                onSupportClick = onSupportClick,
                onProfileClick = onProfileClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
                .padding(paddingValues)
        ) {
            item {
                NewTopBar(
                    userName = userName,
                    onSearchClick = {},
                    onProfileClick = onProfileClick
                )
            }

            item {
                if (showBannerLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(160.dp)
                            .background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colorResource(R.color.gold))
                    }
                } else if (banners.isNotEmpty()) {
                    AsyncImage(
                        model = banners[0].image,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    PromoCard()
                }
            }

            item {
                NewCategorySection(
                    categories = categories,
                    showLoading = showCategoryLoading,
                    onCategoryClick = onCategoryClick
                )
            }

            item {
                BestDealsSection(
                    items = dealItems,
                    showLoading = showDealsLoading
                )
            }

            item {
                WhyChooseSection()
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ─── New TopBar ───────────────────────────────────────────
@Composable
fun NewTopBar(
    userName: String,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0A))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = greeting, color = Color.Gray, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = userName,
                    color = colorResource(R.color.gold),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "👋", fontSize = 16.sp)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF1A1A1A), CircleShape)
                    .clickable { onSearchClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Text("ONE MART", color = Color(0xFF4CAF50), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colorResource(R.color.gold), CircleShape)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.Black, modifier = Modifier.size(22.dp))
            }
        }
    }
}

// ─── Promo Card ───────────────────────────────────────────
@Composable
fun PromoCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))
                )
            )
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text("On First 3 Orders", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))
            ) {
                Text(
                    "Use Code: FIRST50",
                    color = Color(0xFF4CAF50),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = Color.White) {
                Text(
                    "Shop Now →",
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
        Text(text = "🛒", fontSize = 64.sp, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

// ─── New Category Section ─────────────────────────────────
@Composable
fun NewCategorySection(
    categories: List<CategoryModel>,
    showLoading: Boolean,
    onCategoryClick: (String, String) -> Unit
) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Categories", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("See all →", color = Color(0xFF4CAF50), fontSize = 13.sp, textDecoration = TextDecoration.Underline)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (showLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colorResource(R.color.gold))
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categories) { category ->
                    NewCategoryItem(
                        category = category,
                        onClick = { onCategoryClick(category.Id.toString(), category.Name) }
                    )
                }
            }
        }
    }
}

@Composable
fun NewCategoryItem(category: CategoryModel, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(90.dp)
            .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(model = category.ImagePath, contentDescription = null, modifier = Modifier.size(54.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = category.Name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ─── Best Deals Section ───────────────────────────────────
@Composable
fun BestDealsSection(items: List<DealItem>, showLoading: Boolean) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Best Deals for You", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("See all →", color = Color(0xFF4CAF50), fontSize = 13.sp, textDecoration = TextDecoration.Underline)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (showLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colorResource(R.color.gold))
            }
        } else if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp)).padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Koi deals available nahi hain", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items) { deal -> DealItemCard(deal = deal) }
            }
        }
    }
}

@Composable
fun DealItemCard(deal: DealItem) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        if (deal.imagePath.isNotEmpty()) {
            AsyncImage(
                model = deal.imagePath,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(90.dp)
                    .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) { Text("🛍️", fontSize = 32.sp) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(deal.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (deal.unit.isNotEmpty()) Text(deal.unit, color = Color.Gray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text("₹${deal.price}", color = colorResource(R.color.gold), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (deal.storeName.isNotEmpty()) Text(deal.storeName, color = Color.Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ─── Why Choose Section ───────────────────────────────────
@Composable
fun WhyChooseSection() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text("Why Choose One Mart?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WhyChooseItem(emoji = "💰", label = "Best\nPrices", modifier = Modifier.weight(1f))
            WhyChooseItem(emoji = "🔄", label = "Easy\nReturns", modifier = Modifier.weight(1f))
            WhyChooseItem(emoji = "🔒", label = "100%\nSecure", modifier = Modifier.weight(1f))
            WhyChooseItem(emoji = "⭐", label = "Quality\nProducts", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun WhyChooseItem(emoji: String, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Text(text = emoji, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ─── New Bottom Bar ───────────────────────────────────────
@Composable
fun NewBottomBar(
    onHomeClick: () -> Unit,
    onCategoryClick: () -> Unit,
    onWishlistClick: () -> Unit,
    onSupportClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    var selected by remember { mutableStateOf("Home") }

    NavigationBar(containerColor = Color(0xFF111111), tonalElevation = 0.dp) {
        NavigationBarItem(
            selected = selected == "Home",
            onClick = { selected = "Home"; onHomeClick() },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorResource(R.color.gold),
                selectedTextColor = colorResource(R.color.gold),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color(0xFF1A1A1A)
            )
        )
        NavigationBarItem(
            selected = selected == "Categories",
            onClick = { selected = "Categories"; onCategoryClick() },
            icon = { Icon(Icons.Default.GridView, contentDescription = "Categories") },
            label = { Text("Categories", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorResource(R.color.gold),
                selectedTextColor = colorResource(R.color.gold),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color(0xFF1A1A1A)
            )
        )
        NavigationBarItem(
            selected = selected == "Wishlist",
            onClick = { selected = "Wishlist"; onWishlistClick() },
            icon = { Icon(Icons.Default.FavoriteBorder, contentDescription = "Wishlist") },
            label = { Text("Wishlist", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorResource(R.color.gold),
                selectedTextColor = colorResource(R.color.gold),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color(0xFF1A1A1A)
            )
        )
        NavigationBarItem(
            selected = selected == "Support",
            onClick = { selected = "Support"; onSupportClick() },
            icon = { Icon(Icons.Default.HeadsetMic, contentDescription = "Support") },
            label = { Text("Support", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorResource(R.color.gold),
                selectedTextColor = colorResource(R.color.gold),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color(0xFF1A1A1A)
            )
        )
        NavigationBarItem(
            selected = selected == "Profile",
            onClick = { selected = "Profile"; onProfileClick() },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorResource(R.color.gold),
                selectedTextColor = colorResource(R.color.gold),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color(0xFF1A1A1A)
            )
        )
    }
}