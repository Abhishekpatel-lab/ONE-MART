package com.example.nearbystoreapp.screen.store

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.nearbystoreapp.R
import com.example.nearbystoreapp.viewModel.AuthViewModel
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

data class StoreItem(
    val id: String = "",
    val name: String = "",
    val price: String = "",
    val unit: String = "",
    val imagePath: String = ""
)

data class OwnerStore(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val address: String = "",
    val imagePath: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val items: List<StoreItem> = emptyList()
)

val storeCategories = listOf(
    "Foods", "Grocery", "Electronics", "Fashion",
    "Home Goods", "Health", "Dairy", "Bakery",
    "Pharmacy", "Stationery", "Hardware"
)

val categoryIdMap = mapOf(
    "Foods" to "0", "Grocery" to "1", "Electronics" to "2",
    "Fashion" to "3", "Home Goods" to "4", "Health" to "5",
    "Dairy" to "6", "Bakery" to "7", "Pharmacy" to "8",
    "Stationery" to "9", "Hardware" to "10"
)

fun getSectionTitle(category: String): String {
    return if (category.equals("Foods", ignoreCase = true)) "🍽️ Menu" else "📦 Items"
}

fun uploadToCloudinary(
    context: Context,
    imageUri: Uri,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    val cloudName = "dlkfpznqk"
    val uploadPreset = "Nearby"
    val stream = context.contentResolver.openInputStream(imageUri) ?: run {
        onError("Image open failed"); return
    }
    val bytes = stream.readBytes()
    stream.close()

    // ✅ Fixed — toRequestBody use karo
    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart(
            "file", "image.jpg",
            bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        )
        .addFormDataPart("upload_preset", uploadPreset)
        .build()

    val request = Request.Builder()
        .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
        .post(requestBody).build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) { onError(e.message ?: "Upload failed") }
        override fun onResponse(call: Call, response: Response) {
            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                onSuccess(JSONObject(body).getString("secure_url"))
            } else { onError("Upload failed: ${response.code}") }
        }
    })
}

@Composable
fun StoreDashboardPlaceholder(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    var currentScreen by remember { mutableStateOf("dashboard") }
    var editingStore by remember { mutableStateOf<OwnerStore?>(null) }

    when (currentScreen) {
        "dashboard" -> StoreOwnerHome(
            authViewModel = authViewModel,
            onLogout = onLogout,
            onAddStore = { currentScreen = "addStore" },
            onEditStore = { store -> editingStore = store; currentScreen = "editStore" },
            onSupportClick = { currentScreen = "support" }
        )
        "addStore" -> AddEditStoreScreen(
            existingStore = null,
            onBack = { currentScreen = "dashboard" },
            onSaved = { currentScreen = "dashboard" }
        )
        "editStore" -> AddEditStoreScreen(
            existingStore = editingStore,
            onBack = { currentScreen = "dashboard" },
            onSaved = { currentScreen = "dashboard" }
        )
        "support" -> StoreOwnerSupportScreen(
            onBack = { currentScreen = "dashboard" }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreOwnerSupportScreen(onBack: () -> Unit) {
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var myReports by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var isLoadingReports by remember { mutableStateOf(true) }

    fun loadReports() {
        FirebaseDatabase.getInstance().reference
            .child("reports").get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<Map<String, String>>()
                for (child in snapshot.children) {
                    val reportedBy = child.child("reportedBy").value?.toString() ?: ""
                    if (reportedBy == uid) {
                        list.add(mapOf(
                            "reason" to (child.child("reason").value?.toString() ?: ""),
                            "status" to (child.child("status").value?.toString() ?: "open"),
                            "adminReply" to (child.child("adminReply").value?.toString() ?: "")
                        ))
                    }
                }
                myReports = list.reversed()
                isLoadingReports = false
            }
            .addOnFailureListener { isLoadingReports = false }
    }

    LaunchedEffect(Unit) { loadReports() }

    Scaffold(
        containerColor = colorResource(R.color.black2),
        topBar = {
            TopAppBar(
                title = { Text("Help & Support", color = colorResource(R.color.gold), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colorResource(R.color.gold))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorResource(R.color.black2))
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3A3A3A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📞 Contact Admin", color = colorResource(R.color.gold), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Koi bhi problem ho toh admin ko message karo.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }

            item {
                Text("✏️ Admin ko message bhejo:", color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = { Text("Apni problem ya feedback likhein...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorResource(R.color.gold),
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = colorResource(R.color.gold)
                    ),
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (sent) {
                    Text("✅ Message bhej diya! Admin jald reply karega.", color = Color.Green, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Button(
                    onClick = {
                        if (message.isNotEmpty()) {
                            isSending = true
                            val reportData = mapOf(
                                "reportedBy" to uid, "reportedUser" to "admin",
                                "reason" to message, "status" to "open",
                                "userType" to "store_owner", "adminReply" to ""
                            )
                            FirebaseDatabase.getInstance().reference.child("reports").push()
                                .setValue(reportData)
                                .addOnSuccessListener { isSending = false; sent = true; message = ""; loadReports() }
                                .addOnFailureListener { isSending = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = message.isNotEmpty() && !isSending,
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.gold), disabledContainerColor = Color.Gray),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isSending) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                    else Text("📨 Message Bhejo", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("📋 Mere Reports", color = colorResource(R.color.gold), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TextButton(onClick = { loadReports() }) {
                        Text("Refresh", color = colorResource(R.color.gold), fontSize = 12.sp)
                    }
                }
            }

            if (isLoadingReports) {
                item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colorResource(R.color.gold), modifier = Modifier.size(24.dp))
                    }
                }
            } else if (myReports.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFF3A3A3A), RoundedCornerShape(12.dp)).padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Koi report nahi hai abhi 😊", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                items(myReports) { report ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3A3A3A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Tumhara Message:", color = Color.Gray, fontSize = 12.sp)
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (report["status"] == "resolved") Color(0xFF43A047).copy(alpha = 0.2f) else Color(0xFFE53935).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (report["status"] == "resolved") "✅ Resolved" else "🔴 Open",
                                        color = if (report["status"] == "resolved") Color(0xFF43A047) else Color(0xFFE53935),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(report["reason"] ?: "", color = Color.White, fontSize = 14.sp)
                            val adminReply = report["adminReply"] ?: ""
                            if (adminReply.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(color = Color(0xFF2D2D2D), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("💬 Admin Reply:", color = colorResource(R.color.gold), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(adminReply, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("⏳ Admin ke reply ka wait kar rahe hain...", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreOwnerHome(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onAddStore: () -> Unit,
    onEditStore: (OwnerStore) -> Unit,
    onSupportClick: () -> Unit
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var ownerStores by remember { mutableStateOf<List<OwnerStore>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        FirebaseDatabase.getInstance().reference.child("Stores").get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<OwnerStore>()
                for (store in snapshot.children) {
                    val ownerId = store.child("ownerId").value?.toString() ?: ""
                    if (ownerId == uid) {
                        val itemsList = mutableListOf<StoreItem>()
                        for (item in store.child("items").children) {
                            itemsList.add(StoreItem(
                                id = item.key ?: "",
                                name = item.child("name").value?.toString() ?: "",
                                price = item.child("price").value?.toString() ?: "",
                                unit = item.child("unit").value?.toString() ?: "",
                                imagePath = item.child("imagePath").value?.toString() ?: ""
                            ))
                        }
                        list.add(OwnerStore(
                            id = store.key ?: "",
                            name = store.child("Title").value?.toString() ?: "",
                            category = store.child("Category").value?.toString() ?: "",
                            description = store.child("Description").value?.toString() ?: "",
                            address = store.child("Address").value?.toString() ?: "",
                            imagePath = store.child("ImagePath").value?.toString() ?: "",
                            latitude = store.child("Latitude").value?.toString()?.toDoubleOrNull() ?: 0.0,
                            longitude = store.child("Longitude").value?.toString()?.toDoubleOrNull() ?: 0.0,
                            items = itemsList
                        ))
                    }
                }
                ownerStores = list
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    Scaffold(
        containerColor = colorResource(R.color.black2),
        topBar = {
            TopAppBar(
                title = { Text("My Stores 🏪", color = colorResource(R.color.gold), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSupportClick) {
                        Icon(Icons.Default.HeadsetMic, contentDescription = "Support", tint = colorResource(R.color.gold))
                    }
                    TextButton(onClick = { authViewModel.logout(); onLogout() }) {
                        Text("Logout", color = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorResource(R.color.black2))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF3A3A3A)) {
                NavigationBarItem(
                    selected = true, onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colorResource(R.color.gold),
                        selectedTextColor = colorResource(R.color.gold),
                        indicatorColor = Color(0xFF2D2D2D)
                    )
                )
                NavigationBarItem(
                    selected = false, onClick = onSupportClick,
                    icon = { Icon(Icons.Default.HeadsetMic, contentDescription = "Support") },
                    label = { Text("Support", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF2D2D2D)
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddStore, containerColor = colorResource(R.color.gold)) {
                Icon(Icons.Default.Add, contentDescription = "Add Store", tint = Color.Black)
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colorResource(R.color.gold))
            }
        } else if (ownerStores.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏪", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Koi store nahi hai abhi", color = Color.Gray, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Neeche + button se store add karo", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            val groupedStores = ownerStores.groupBy { it.category }.toSortedMap()
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                groupedStores.forEach { (category, storesInCategory) ->
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("📂 $category  (${storesInCategory.size})", color = colorResource(R.color.gold), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = colorResource(R.color.gold).copy(alpha = 0.3f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    itemsIndexed(storesInCategory) { _, store ->
                        OwnerStoreCard(
                            store = store,
                            onEdit = { onEditStore(store) },
                            onDelete = {
                                FirebaseDatabase.getInstance().reference.child("Stores").child(store.id).removeValue()
                                ownerStores = ownerStores.filter { it.id != store.id }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OwnerStoreCard(store: OwnerStore, onEdit: () -> Unit, onDelete: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(colorResource(R.color.black3), RoundedCornerShape(12.dp))) {
        if (store.imagePath.isNotEmpty()) {
            AsyncImage(
                model = store.imagePath, contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(store.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(store.category, color = colorResource(R.color.gold), fontSize = 13.sp)
                if (store.address.isNotEmpty()) Text("📍 ${store.address}", color = Color.Gray, fontSize = 12.sp)
                if (store.items.isNotEmpty()) Text("${store.items.size} ${getSectionTitle(store.category)}", color = Color.Gray, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onEdit) { Text("Edit", color = colorResource(R.color.gold)) }
                TextButton(onClick = onDelete) { Text("Delete", color = Color.Red) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditStoreScreen(existingStore: OwnerStore?, onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val isEditing = existingStore != null

    var storeName by remember { mutableStateOf(existingStore?.name ?: "") }
    var description by remember { mutableStateOf(existingStore?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(existingStore?.category ?: "") }
    var address by remember { mutableStateOf(existingStore?.address ?: "") }
    var latitude by remember { mutableStateOf(existingStore?.latitude ?: 0.0) }
    var longitude by remember { mutableStateOf(existingStore?.longitude ?: 0.0) }
    var manualLat by remember { mutableStateOf(if (existingStore?.latitude != 0.0) existingStore?.latitude?.toString() ?: "" else "") }
    var manualLng by remember { mutableStateOf(if (existingStore?.longitude != 0.0) existingStore?.longitude?.toString() ?: "" else "") }
    var locationFetched by remember { mutableStateOf(existingStore != null && existingStore.latitude != 0.0) }
    var isSaving by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var uploadedImageUrl by remember { mutableStateOf(existingStore?.imagePath ?: "") }
    var isUploading by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf(if (existingStore?.items?.isNotEmpty() == true) existingStore.items else listOf(StoreItem(id = "0"))) }
    var itemUploadingIndex by remember { mutableStateOf(-1) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { isUploading = true; uploadToCloudinary(context, it, onSuccess = { url -> uploadedImageUrl = url; isUploading = false }, onError = { isUploading = false }) }
    }

    var pendingItemIndex by remember { mutableStateOf(-1) }
    val itemImagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imageUri ->
            val idx = pendingItemIndex
            if (idx >= 0) {
                itemUploadingIndex = idx
                uploadToCloudinary(context, imageUri,
                    onSuccess = { url -> items = items.toMutableList().also { list -> list[idx] = list[idx].copy(imagePath = url) }; itemUploadingIndex = -1 },
                    onError = { itemUploadingIndex = -1 })
            }
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { loc ->
                if (loc != null) { latitude = loc.latitude; longitude = loc.longitude; manualLat = loc.latitude.toString(); manualLng = loc.longitude.toString(); locationFetched = true }
            }
        }
    }

    fun fetchLocation() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { loc ->
                if (loc != null) { latitude = loc.latitude; longitude = loc.longitude; manualLat = loc.latitude.toString(); manualLng = loc.longitude.toString(); locationFetched = true }
            }
        } else { locationLauncher.launch(permission) }
    }

    fun saveStore() {
        if (storeName.isEmpty() || selectedCategory.isEmpty()) return
        val finalLat = manualLat.toDoubleOrNull() ?: latitude
        val finalLng = manualLng.toDoubleOrNull() ?: longitude
        isSaving = true
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseDatabase.getInstance().reference
        val categoryId = categoryIdMap[selectedCategory] ?: "0"
        val itemsMap = mutableMapOf<String, Any>()
        items.forEachIndexed { index, item ->
            if (item.name.isNotEmpty()) {
                itemsMap["item_$index"] = mapOf("name" to item.name, "price" to item.price, "unit" to item.unit, "available" to true, "imagePath" to item.imagePath)
            }
        }
        val storeData = mutableMapOf<String, Any>(
            "ownerId" to uid, "Title" to storeName, "Category" to selectedCategory,
            "CategoryId" to categoryId, "Description" to description, "Address" to address,
            "ShortAddress" to address.take(30), "ImagePath" to uploadedImageUrl,
            "Latitude" to finalLat, "Longitude" to finalLng,
            "Activity" to "Open", "Hours" to "9 am - 9 pm",
            "Call" to "", "rating" to 0.0, "orderCount" to 0, "items" to itemsMap
        )
        if (isEditing && existingStore != null) {
            db.child("Stores").child(existingStore.id).updateChildren(storeData)
                .addOnSuccessListener { isSaving = false; onSaved() }
                .addOnFailureListener { isSaving = false }
        } else {
            val storeId = db.child("Stores").push().key ?: return
            db.child("Stores").child(storeId).setValue(storeData)
                .addOnSuccessListener { isSaving = false; onSaved() }
                .addOnFailureListener { isSaving = false }
        }
    }

    Scaffold(
        containerColor = colorResource(R.color.black2),
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Store" else "Add Store", color = colorResource(R.color.gold), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colorResource(R.color.gold))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorResource(R.color.black2))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Text("🖼️ Store Photo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorResource(R.color.gold))
            Box(
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp))
                    .background(colorResource(R.color.black3))
                    .border(1.dp, if (uploadedImageUrl.isNotEmpty()) colorResource(R.color.gold) else Color.Gray, RoundedCornerShape(12.dp))
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (isUploading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colorResource(R.color.gold))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Uploading...", color = Color.Gray, fontSize = 13.sp)
                    }
                } else if (uploadedImageUrl.isNotEmpty()) {
                    AsyncImage(model = uploadedImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📷", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap to add store photo", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }

            StoreTextField(value = storeName, onValueChange = { storeName = it }, label = "Store Name")
            StoreTextField(value = description, onValueChange = { description = it }, label = "Description")

            ExposedDropdownMenuBox(expanded = showCategoryDropdown, onExpandedChange = { showCategoryDropdown = it }) {
                OutlinedTextField(
                    value = selectedCategory, onValueChange = {}, readOnly = true,
                    label = { Text("Category", color = Color.Gray) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = storeTextFieldColors()
                )
                ExposedDropdownMenu(expanded = showCategoryDropdown, onDismissRequest = { showCategoryDropdown = false }, modifier = Modifier.background(colorResource(R.color.black3))) {
                    storeCategories.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat, color = Color.White) }, onClick = { selectedCategory = cat; showCategoryDropdown = false })
                    }
                }
            }

            StoreTextField(value = address, onValueChange = { address = it }, label = "Address")

            Text("📍 Location", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorResource(R.color.gold))
            Button(
                onClick = { fetchLocation() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = if (locationFetched) Color(0xFF2E7D32) else colorResource(R.color.gold)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (locationFetched) "✅ GPS Location Mili" else "📍 GPS Se Live Location Lo", color = Color.White, fontSize = 13.sp)
            }

            Text("Ya manually type karo:", color = Color.Gray, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = manualLat,
                    onValueChange = { manualLat = it; it.toDoubleOrNull()?.let { v -> latitude = v; locationFetched = true } },
                    label = { Text("Latitude", color = Color.Gray, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = storeTextFieldColors(), singleLine = true,
                    placeholder = { Text("28.6139", color = Color.Gray, fontSize = 11.sp) }
                )
                OutlinedTextField(
                    value = manualLng,
                    onValueChange = { manualLng = it; it.toDoubleOrNull()?.let { v -> longitude = v; locationFetched = true } },
                    label = { Text("Longitude", color = Color.Gray, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = storeTextFieldColors(), singleLine = true,
                    placeholder = { Text("77.2090", color = Color.Gray, fontSize = 11.sp) }
                )
            }

            Box(modifier = Modifier.fillMaxWidth().background(colorResource(R.color.black3), RoundedCornerShape(8.dp)).padding(10.dp)) {
                Text("💡 Google Maps se coordinates kaise lein:\n1. Google Maps kholo\n2. Apni location par long press karo\n3. Upar coordinates copy karo\n4. Yahan paste karo", color = Color.Gray, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(getSectionTitle(selectedCategory), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorResource(R.color.gold))

            items.forEachIndexed { index, item ->
                ItemRowWithImage(
                    item = item,
                    isUploading = itemUploadingIndex == index,
                    onItemChange = { updated -> items = items.toMutableList().also { it[index] = updated } },
                    onDelete = { if (items.size > 1) items = items.toMutableList().also { it.removeAt(index) } },
                    onPickImage = { pendingItemIndex = index; itemImagePickerLauncher.launch("image/*") }
                )
            }

            TextButton(onClick = { items = items + StoreItem(id = items.size.toString()) }) {
                Icon(Icons.Default.Add, contentDescription = null, tint = colorResource(R.color.gold))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (selectedCategory == "Foods") "Item Add Karo (Menu)" else "Item Add Karo", color = colorResource(R.color.gold))
            }

            Button(
                onClick = { saveStore() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isSaving && !isUploading && itemUploadingIndex == -1 && storeName.isNotEmpty() && selectedCategory.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.gold), disabledContainerColor = Color.Gray),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                else Text(if (isEditing) "Update Store" else "Store Save Karo", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ItemRowWithImage(item: StoreItem, isUploading: Boolean, onItemChange: (StoreItem) -> Unit, onDelete: () -> Unit, onPickImage: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(colorResource(R.color.black3), RoundedCornerShape(10.dp)).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray).clickable { onPickImage() },
                contentAlignment = Alignment.Center
            ) {
                if (isUploading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colorResource(R.color.gold))
                else if (item.imagePath.isNotEmpty()) AsyncImage(model = item.imagePath, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Text("📷", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedTextField(value = item.name, onValueChange = { onItemChange(item.copy(name = it)) }, label = { Text("Item naam", color = Color.Gray, fontSize = 12.sp) }, modifier = Modifier.weight(1f), colors = storeTextFieldColors(), singleLine = true)
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = item.price, onValueChange = { onItemChange(item.copy(price = it)) }, label = { Text("₹ Price", color = Color.Gray, fontSize = 12.sp) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = storeTextFieldColors(), singleLine = true)
            OutlinedTextField(value = item.unit, onValueChange = { onItemChange(item.copy(unit = it)) }, label = { Text("Unit", color = Color.Gray, fontSize = 12.sp) }, modifier = Modifier.weight(1f), placeholder = { Text("1kg, 1L..", color = Color.Gray, fontSize = 11.sp) }, colors = storeTextFieldColors(), singleLine = true)
        }
    }
}

@Composable
fun StoreTextField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label, color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), colors = storeTextFieldColors(), singleLine = true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun storeTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = colorResource(R.color.gold),
    unfocusedBorderColor = Color.Gray,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = colorResource(R.color.gold)
)