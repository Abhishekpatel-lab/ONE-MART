package com.example.nearbystoreapp.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nearbystoreapp.viewModel.AdminViewModel

val DarkBg = Color(0xFF2D2D2D)
val YellowAccent = Color(0xFFFFC107)
val CardBg = Color(0xFF3A3A3A)
val RedColor = Color(0xFFE53935)
val GreenColor = Color(0xFF43A047)
val OrangeColor = Color(0xFFFF6D00)

@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit,
    adminViewModel: AdminViewModel = viewModel()
) {
    val selectedTab = remember { mutableStateOf(0) }
    val tabs = listOf("Analytics", "Users", "Stores", "Reports")
    val tabIcons = listOf(
        Icons.Default.BarChart,
        Icons.Default.People,
        Icons.Default.Store,
        Icons.Default.Flag
    )

    Scaffold(
        containerColor = DarkBg,
        topBar = { AdminTopBar(onLogout = onLogout) },
        bottomBar = {
            NavigationBar(containerColor = CardBg) {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab.value == index,
                        onClick = { selectedTab.value = index },
                        icon = { Icon(tabIcons[index], contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = YellowAccent,
                            selectedTextColor = YellowAccent,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = DarkBg
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DarkBg)
        ) {
            when (selectedTab.value) {
                0 -> AnalyticsTab(adminViewModel)
                1 -> UsersTab(adminViewModel)
                2 -> StoresTab(adminViewModel)
                3 -> ReportsTab(adminViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTopBar(onLogout: () -> Unit) {
    TopAppBar(
        title = {
            Text("Admin Dashboard", color = YellowAccent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        actions = {
            IconButton(onClick = onLogout) {
                Icon(Icons.Default.Logout, contentDescription = "Logout", tint = YellowAccent)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
    )
}

@Composable
fun AnalyticsTab(vm: AdminViewModel) {
    val users by vm.users.collectAsState()
    val stores by vm.stores.collectAsState()
    val reports by vm.reports.collectAsState()

    LaunchedEffect(Unit) {
        vm.loadUsers()
        vm.loadStores()
        vm.loadReports()
    }

    val totalUsers = users.count { it.userType == "user" }
    val totalStoreOwners = users.count { it.userType == "store_owner" }
    val bannedUsers = users.count { it.isBanned }
    val totalStores = stores.size
    val blockedStores = stores.count { it.isBlocked }
    val totalReports = reports.size
    val openReports = reports.count { it.status == "open" }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Overview", color = YellowAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Users", totalUsers.toString(), Icons.Default.People, Modifier.weight(1f))
                StatCard("Store Owners", totalStoreOwners.toString(), Icons.Default.Store, Modifier.weight(1f), YellowAccent)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Banned Users", bannedUsers.toString(), Icons.Default.Block, Modifier.weight(1f), RedColor)
                StatCard("Blocked Stores", blockedStores.toString(), Icons.Default.Lock, Modifier.weight(1f), OrangeColor)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Total Stores", totalStores.toString(), Icons.Default.Store, Modifier.weight(1f))
                StatCard("Open Reports", openReports.toString(), Icons.Default.ErrorOutline, Modifier.weight(1f), RedColor)
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color = GreenColor
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = YellowAccent, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text(label, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun UsersTab(vm: AdminViewModel) {
    val users by vm.users.collectAsState()

    LaunchedEffect(Unit) { vm.loadUsers() }

    val regularUsers = users.filter { it.userType == "user" }
    val storeOwners = users.filter { it.userType == "store_owner" }

    if (users.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = YellowAccent)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("👤 Regular Users (${regularUsers.size})", color = YellowAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        if (regularUsers.isEmpty()) {
            item { Text("Koi regular user nahi hai", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(8.dp)) }
        } else {
            items(regularUsers, key = { it.uid }) { user ->
                UserCard(user = user, onBanToggle = { vm.toggleBan(user.uid, user.isBanned) })
            }
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.4f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            Text("🏪 Store Owners (${storeOwners.size})", color = YellowAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        if (storeOwners.isEmpty()) {
            item { Text("Koi store owner nahi hai", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(8.dp)) }
        } else {
            items(storeOwners, key = { it.uid }) { user ->
                UserCard(user = user, onBanToggle = { vm.toggleBan(user.uid, user.isBanned) })
            }
        }
    }
}

@Composable
fun UserCard(user: UserData, onBanToggle: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                tint = if (user.isBanned) RedColor else YellowAccent,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(user.email, color = Color.Gray, fontSize = 12.sp)
                Text(
                    if (user.userType == "store_owner") "🏪 Store Owner" else "👤 User",
                    color = if (user.userType == "store_owner") YellowAccent else Color.Gray,
                    fontSize = 11.sp
                )
                Text(
                    if (user.isBanned) "🔴 Banned" else "🟢 Active",
                    color = if (user.isBanned) RedColor else GreenColor,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = onBanToggle,
                colors = ButtonDefaults.buttonColors(containerColor = if (user.isBanned) GreenColor else RedColor),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(if (user.isBanned) "Unban" else "Ban", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun StoresTab(vm: AdminViewModel) {
    val stores by vm.stores.collectAsState()
    val isLoading by vm.isLoadingStores.collectAsState()

    LaunchedEffect(Unit) { vm.loadStores() }

    when {
        isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = YellowAccent)
            }
        }
        stores.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Store, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(60.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Koi store nahi mila", color = Color.Gray, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { vm.loadStores() },
                        colors = ButtonDefaults.buttonColors(containerColor = YellowAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("All Stores (${stores.size})", color = YellowAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                items(stores, key = { it.storeId }) { store ->
                    StoreCard(
                        store = store,
                        onApprove = { vm.updateStoreStatus(store.storeId, "approved") },
                        onReject = { vm.updateStoreStatus(store.storeId, "rejected") },
                        onBlockToggle = { vm.toggleStoreBlock(store.storeId, store.isBlocked) }
                    )
                }
            }
        }
    }
}

@Composable
fun StoreCard(
    store: StoreData,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onBlockToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Store,
                    contentDescription = null,
                    tint = if (store.isBlocked) OrangeColor else YellowAccent,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(store.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(store.ownerEmail, color = Color.Gray, fontSize = 11.sp, maxLines = 2)
                    if (store.isBlocked) {
                        Text("🔒 Blocked", color = OrangeColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                StatusChip(store.status)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onBlockToggle,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (store.isBlocked) GreenColor else OrangeColor
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    if (store.isBlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (store.isBlocked) "Unblock Store" else "Block Store",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            if (store.status == "pending") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenColor),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Approve", color = Color.White) }
                    Button(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RedColor),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Reject", color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (bg, text) = when (status) {
        "approved" -> GreenColor to "Approved"
        "rejected" -> RedColor to "Rejected"
        else -> YellowAccent to "Pending"
    }
    Surface(shape = RoundedCornerShape(20.dp), color = bg.copy(alpha = 0.2f)) {
        Text(text, color = bg, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
fun ReportsTab(vm: AdminViewModel) {
    val reports by vm.reports.collectAsState()

    LaunchedEffect(Unit) { vm.loadReports() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Reports & Complaints", color = YellowAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        if (reports.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = YellowAccent)
                }
            }
        } else {
            items(reports, key = { it.reportId }) { report ->
                ReportCard(
                    report = report,
                    onResolve = { vm.resolveReport(report.reportId) },
                    onReply = { reply -> vm.replyToReport(report.reportId, reply) }
                )
            }
        }
    }
}

@Composable
fun ReportCard(
    report: ReportData,
    onResolve: () -> Unit,
    onReply: (String) -> Unit
) {
    var showReplyBox by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Flag, contentDescription = null, tint = RedColor, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(report.reason, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("By: ${report.reportedBy}", color = Color.Gray, fontSize = 12.sp)
                    Text("Against: ${report.reportedUser}", color = Color.Gray, fontSize = 12.sp)
                }
                StatusChip(report.status)
            }

            // ✅ Pehle se reply hai toh dikhao
            if (report.adminReply.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = DarkBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Admin Reply:", color = YellowAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(report.adminReply, color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ✅ Reply button — hamesha dikhega
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showReplyBox = !showReplyBox },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = YellowAccent)
                ) {
                    Text(if (showReplyBox) "Cancel" else "✏️ Reply", color = YellowAccent)
                }
                // ✅ Resolve button — sirf open reports mein
                if (report.status == "open") {
                    Button(
                        onClick = onResolve,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("✅ Resolve", color = Color.White)
                    }
                }
            }

            // ✅ Reply box — animated
            AnimatedVisibility(visible = showReplyBox) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("User ko reply likho...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YellowAccent,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = YellowAccent
                        ),
                        maxLines = 4
                    )
                    Button(
                        onClick = {
                            if (replyText.isNotEmpty()) {
                                onReply(replyText)
                                showReplyBox = false
                                replyText = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = replyText.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YellowAccent,
                            disabledContainerColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("📨 Reply Bhejo", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─── Data Classes ─────────────────────────────────────────
data class UserData(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val userType: String = "user",
    val isBanned: Boolean = false
)

data class StoreData(
    val storeId: String = "",
    val name: String = "",
    val ownerEmail: String = "",
    val status: String = "pending",
    val isBlocked: Boolean = false
)

data class ReportData(
    val reportId: String = "",
    val reason: String = "",
    val reportedBy: String = "",
    val reportedUser: String = "",
    val status: String = "open",
    val adminReply: String = ""
)