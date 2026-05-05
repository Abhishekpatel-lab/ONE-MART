package com.example.nearbystoreapp.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextAlign
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
    val storeName: String = "",
    val badge: String = "",
    val rating: Float = 0f,
    val ratingCount: Int = 0
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
    onCategoriesTabClick: () -> Unit,
    onSearchClick: () -> Unit = {}  // ✅ Add kiya
) {
    val dashboardRepository = remember { DashboardRepository() }

    val banners    = remember { mutableStateListOf<BannerModel>() }
    val categories = remember { mutableStateListOf<CategoryModel>() }
    val dealItems  = remember { mutableStateListOf<DealItem>() }

    var showBannerLoading   by remember { mutableStateOf(true) }
    var showCategoryLoading by remember { mutableStateOf(true) }
    var showDealsLoading    by remember { mutableStateOf(true) }
    var userName            by remember { mutableStateOf("User") }

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
                val categoryItemsMap = mutableMapOf<String, MutableList<DealItem>>()
                var globalIndex = 0

                for (store in snapshot.children) {
                    val isBlocked = store.child("isBlocked").getValue(Boolean::class.java) ?: false
                    if (isBlocked) continue
                    val storeName     = store.child("Title").value?.toString()    ?: ""
                    val storeCategory = store.child("Category").value?.toString() ?: "Other"

                    for (item in store.child("items").children) {
                        val name  = item.child("name").value?.toString()  ?: ""
                        val price = item.child("price").value?.toString() ?: ""
                        if (name.isNotEmpty() && price.isNotEmpty()) {
                            val badge = when (globalIndex % 3) {
                                0    -> "Best Seller"
                                1    -> "Popular"
                                else -> ""
                            }
                            val rating = when (globalIndex % 5) {
                                0    -> 4.6f
                                1    -> 4.7f
                                2    -> 4.4f
                                3    -> 4.5f
                                else -> 4.3f
                            }
                            val ratingCount = when (globalIndex % 5) {
                                0    -> 128
                                1    -> 96
                                2    -> 72
                                3    -> 84
                                else -> 56
                            }
                            val dealItem = DealItem(
                                name        = name,
                                price       = price,
                                unit        = item.child("unit").value?.toString()      ?: "",
                                imagePath   = item.child("imagePath").value?.toString() ?: "",
                                storeName   = storeName,
                                badge       = badge,
                                rating      = rating,
                                ratingCount = ratingCount
                            )
                            val list = categoryItemsMap.getOrPut(storeCategory) { mutableListOf() }
                            if (list.size < 2) list.add(dealItem)
                            globalIndex++
                        }
                    }
                }
                val finalList = mutableListOf<DealItem>()
                categoryItemsMap.values.forEach { finalList.addAll(it) }
                dealItems.clear()
                dealItems.addAll(finalList)
                showDealsLoading = false
            }
            .addOnFailureListener { showDealsLoading = false }
    }

    Scaffold(
        containerColor = Color(0xFF0A0A0A),
        bottomBar = {
            NewBottomBar(
                onHomeClick     = {},
                onCategoryClick = onCategoriesTabClick,
                onWishlistClick = onWishlistClick,
                onSupportClick  = onSupportClick,
                onProfileClick  = onProfileClick
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
                    userName       = userName,
                    onSearchClick  = onSearchClick,  // ✅ Fix
                    onProfileClick = onProfileClick
                )
            }

            item {
                if (showBannerLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(180.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colorResource(R.color.gold))
                    }
                } else if (banners.isNotEmpty()) {
                    AsyncImage(
                        model              = banners[0].image,
                        contentDescription = null,
                        modifier           = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(180.dp).clip(RoundedCornerShape(16.dp)),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    PromoCard()
                }
            }

            item {
                NewCategorySection(
                    categories  = categories,
                    showLoading = showCategoryLoading,
                    onCategoryClick = onCategoryClick
                )
            }

            item {
                BestDealsSection(items = dealItems, showLoading = showDealsLoading)
            }

            item { WhyChooseSection() }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun NewTopBar(
    userName: String,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11  -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else      -> "Good Night"
    }

    var profilePicUrl by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        uid?.let {
            FirebaseDatabase.getInstance().reference
                .child("users").child(it).get()
                .addOnSuccessListener { snapshot ->
                    profilePicUrl = snapshot.child("profilePic").value?.toString() ?: ""
                }
        }
    }

    Row(
        modifier              = Modifier.fillMaxWidth().background(Color(0xFF0A0A0A)).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text(text = greeting, color = Color.Gray, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = userName.uppercase(), color = colorResource(R.color.gold), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "👋", fontSize = 16.sp)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ONE",  color = Color(0xFF4CAF50),          fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 11.sp)
                Text("MART", color = colorResource(R.color.gold), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 11.sp)
            }

            // ✅ Search button — onSearchClick call hoga
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0xFF1A1A1A), CircleShape)
                    .clickable { onSearchClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colorResource(R.color.gold), CircleShape)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                if (profilePicUrl.isNotEmpty()) {
                    AsyncImage(model = profilePicUrl, contentDescription = "Profile", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
fun PromoCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(colors = listOf(Color(0xFF0D2B1A), Color(0xFF1B5E20))))
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(shape = RoundedCornerShape(4.dp), color = colorResource(R.color.gold).copy(alpha = 0.2f)) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("◆", color = colorResource(R.color.gold), fontSize = 8.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BEST DEALS", color = colorResource(R.color.gold), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Fresh Groceries",   color = Color.White,        fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Best Prices!",      color = Color(0xFF4CAF50),  fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Top quality products\nat lowest prices", color = Color.Gray, fontSize = 12.sp, lineHeight = 16.sp)
                Spacer(modifier = Modifier.height(14.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = colorResource(R.color.gold)) {
                    Text("Shop Now →", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
            Text("🧺", fontSize = 80.sp, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
fun NewCategorySection(
    categories: List<CategoryModel>,
    showLoading: Boolean,
    onCategoryClick: (String, String) -> Unit
) {
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Categories", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("See all →", color = Color(0xFF4CAF50), fontSize = 13.sp, textDecoration = TextDecoration.Underline)
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (showLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colorResource(R.color.gold))
            }
        } else {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(categories) { category ->
                    NewCategoryItem(category = category, onClick = { onCategoryClick(category.Id.toString(), category.Name) })
                }
            }
        }
    }
}

@Composable
fun NewCategoryItem(category: CategoryModel, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(90.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp)).clickable { onClick() }.padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(model = category.ImagePath, contentDescription = null, modifier = Modifier.size(54.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = category.Name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
fun BestDealsSection(items: List<DealItem>, showLoading: Boolean) {
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Best Deals for You", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("See all →", color = Color(0xFF4CAF50), fontSize = 13.sp, textDecoration = TextDecoration.Underline)
        }
        Spacer(modifier = Modifier.height(12.dp))
        when {
            showLoading -> {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorResource(R.color.gold))
                }
            }
            items.isEmpty() -> {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp)).padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Koi deals available nahi hain", color = Color.Gray, fontSize = 13.sp)
                }
            }
            else -> {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(items) { _, deal -> DealItemCard(deal = deal) }
                }
            }
        }
    }
}

@Composable
fun DealItemCard(deal: DealItem) {
    Box(modifier = Modifier.width(160.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(14.dp))) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                if (deal.imagePath.isNotEmpty()) {
                    AsyncImage(model = deal.imagePath, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)), contentScale = ContentScale.Crop)
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2A2A2A), RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)), contentAlignment = Alignment.Center) {
                        Text("🛍️", fontSize = 36.sp)
                    }
                }
                if (deal.badge.isNotEmpty()) {
                    val badgeColor = if (deal.badge == "Best Seller") Color(0xFF4CAF50) else Color(0xFFFFA000)
                    Surface(shape = RoundedCornerShape(bottomEnd = 8.dp), color = badgeColor, modifier = Modifier.align(Alignment.TopStart)) {
                        Text(deal.badge, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(deal.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = colorResource(R.color.gold), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("${deal.rating} (${deal.ratingCount})", color = Color.Gray, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(deal.storeName, color = Color.Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(6.dp))
                Text("₹${deal.price}", color = colorResource(R.color.gold), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                if (deal.unit.isNotEmpty()) Text(deal.unit, color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun WhyChooseSection() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp)) {
        Text("Why Choose One Mart?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WhyChooseItem("💰", "Best Prices",       "Lowest prices always",          Modifier.weight(1f))
            WhyChooseItem("🔄", "Easy Returns",      "Hassle free returns",            Modifier.weight(1f))
            WhyChooseItem("🔒", "100% Secure",       "Secure payments guaranteed",     Modifier.weight(1f))
            WhyChooseItem("⭐", "Quality Products",  "Top quality products",           Modifier.weight(1f))
        }
    }
}

@Composable
fun WhyChooseItem(emoji: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp)).padding(vertical = 14.dp, horizontal = 6.dp)
    ) {
        Box(modifier = Modifier.size(42.dp).background(Color(0xFF1B5E20), CircleShape), contentAlignment = Alignment.Center) {
            Text(text = emoji, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(title,    color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 13.sp)
        Spacer(modifier = Modifier.height(3.dp))
        Text(subtitle, color = Color.Gray,  fontSize = 9.sp,  textAlign = TextAlign.Center, lineHeight = 12.sp)
    }
}

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
            onClick  = { selected = "Home"; onHomeClick() },
            icon     = { Icon(Icons.Default.Home, contentDescription = null) },
            label    = { Text("Home", fontSize = 10.sp) },
            colors   = NavigationBarItemDefaults.colors(selectedIconColor = colorResource(R.color.gold), selectedTextColor = colorResource(R.color.gold), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color(0xFF1A1A1A))
        )
        NavigationBarItem(
            selected = selected == "Categories",
            onClick  = { selected = "Categories"; onCategoryClick() },
            icon     = { Icon(Icons.Default.GridView, contentDescription = null) },
            label    = { Text("Categories", fontSize = 10.sp) },
            colors   = NavigationBarItemDefaults.colors(selectedIconColor = colorResource(R.color.gold), selectedTextColor = colorResource(R.color.gold), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color(0xFF1A1A1A))
        )
        NavigationBarItem(
            selected = selected == "Wishlist",
            onClick  = { selected = "Wishlist"; onWishlistClick() },
            icon     = { Icon(Icons.Default.FavoriteBorder, contentDescription = null) },
            label    = { Text("Wishlist", fontSize = 10.sp) },
            colors   = NavigationBarItemDefaults.colors(selectedIconColor = colorResource(R.color.gold), selectedTextColor = colorResource(R.color.gold), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color(0xFF1A1A1A))
        )
        NavigationBarItem(
            selected = selected == "Support",
            onClick  = { selected = "Support"; onSupportClick() },
            icon     = { Icon(Icons.Default.HeadsetMic, contentDescription = null) },
            label    = { Text("Support", fontSize = 10.sp) },
            colors   = NavigationBarItemDefaults.colors(selectedIconColor = colorResource(R.color.gold), selectedTextColor = colorResource(R.color.gold), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color(0xFF1A1A1A))
        )
        NavigationBarItem(
            selected = selected == "Profile",
            onClick  = { selected = "Profile"; onProfileClick() },
            icon     = { Icon(Icons.Default.Person, contentDescription = null) },
            label    = { Text("Profile", fontSize = 10.sp) },
            colors   = NavigationBarItemDefaults.colors(selectedIconColor = colorResource(R.color.gold), selectedTextColor = colorResource(R.color.gold), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color(0xFF1A1A1A))
        )
    }
}