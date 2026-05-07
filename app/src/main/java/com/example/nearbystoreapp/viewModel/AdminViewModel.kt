package com.example.nearbystoreapp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar



class AdminViewModel : ViewModel() {

    private val database = FirebaseDatabase.getInstance().reference

    private val _users = MutableStateFlow<List<UserData>>(emptyList())
    val users: StateFlow<List<UserData>> = _users

    private val _stores = MutableStateFlow<List<StoreData>>(emptyList())
    val stores: StateFlow<List<StoreData>> = _stores

    private val _reports = MutableStateFlow<List<ReportData>>(emptyList())
    val reports: StateFlow<List<ReportData>> = _reports

    private val _isLoadingStores = MutableStateFlow(false)
    val isLoadingStores: StateFlow<Boolean> = _isLoadingStores

    private val _recentActivity = MutableStateFlow<List<ActivityItem>>(emptyList())
    val recentActivity: StateFlow<List<ActivityItem>> = _recentActivity

    private val _usersThisWeek = MutableStateFlow(0)
    val usersThisWeek: StateFlow<Int> = _usersThisWeek

    private val _storesThisWeek = MutableStateFlow(0)
    val storesThisWeek: StateFlow<Int> = _storesThisWeek

    private val _reportsThisWeek = MutableStateFlow(0)
    val reportsThisWeek: StateFlow<Int> = _reportsThisWeek

    private fun weekStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        return cal.timeInMillis
    }

    fun loadUsers() {
        viewModelScope.launch {
            try {
                val snapshot = database.child("users").get().await()
                val list = mutableListOf<UserData>()
                var weekCount = 0
                val weekStart = weekStart()
                snapshot.children.forEach { child ->
                    val createdAt = child.child("createdAt").value?.toString()?.toLongOrNull() ?: 0L
                    list.add(UserData(
                        uid       = child.key ?: "",
                        name      = child.child("name").value?.toString() ?: "",
                        email     = child.child("email").value?.toString() ?: "",
                        userType  = child.child("userType").value?.toString() ?: "user",
                        isBanned  = child.child("isBanned").getValue(Boolean::class.java) ?: false,
                        createdAt = createdAt
                    ))
                    if (createdAt > weekStart) weekCount++
                }
                _users.value = list
                _usersThisWeek.value = weekCount
                buildRecentActivity()
            } catch (_: Exception) {}
        }
    }

    fun toggleBan(uid: String, currentlyBanned: Boolean) {
        viewModelScope.launch {
            try {
                database.child("users").child(uid).child("isBanned").setValue(!currentlyBanned).await()
                _users.value = _users.value.map {
                    if (it.uid == uid) it.copy(isBanned = !currentlyBanned) else it
                }
            } catch (_: Exception) {}
        }
    }

    fun loadStores() {
        viewModelScope.launch {
            try {
                _isLoadingStores.value = true
                val snapshot = database.child("Stores").get().await()
                val list = mutableListOf<StoreData>()
                var weekCount = 0
                val weekStart = weekStart()
                snapshot.children.forEach { child ->
                    val createdAt = child.child("createdAt").value?.toString()?.toLongOrNull() ?: 0L
                    list.add(StoreData(
                        storeId    = child.key ?: "",
                        name       = child.child("Title").value?.toString() ?: "Unknown Store",
                        ownerEmail = child.child("Address").value?.toString() ?: "",
                        status     = child.child("status").value?.toString() ?: "approved",
                        isBlocked  = child.child("isBlocked").getValue(Boolean::class.java) ?: false,
                        createdAt  = createdAt
                    ))
                    if (createdAt > weekStart) weekCount++
                }
                _stores.value = list
                _storesThisWeek.value = weekCount
                buildRecentActivity()
            } catch (_: Exception) {
            } finally {
                _isLoadingStores.value = false
            }
        }
    }

    fun toggleStoreBlock(storeId: String, currentlyBlocked: Boolean) {
        viewModelScope.launch {
            try {
                val newVal = !currentlyBlocked
                database.child("Stores").child(storeId).child("isBlocked").setValue(newVal).await()
                _stores.value = _stores.value.map {
                    if (it.storeId == storeId) it.copy(isBlocked = newVal) else it
                }
            } catch (_: Exception) {}
        }
    }

    fun updateStoreStatus(storeId: String, status: String) {
        viewModelScope.launch {
            try {
                database.child("Stores").child(storeId).child("status").setValue(status).await()
                _stores.value = _stores.value.map {
                    if (it.storeId == storeId) it.copy(status = status) else it
                }
            } catch (_: Exception) {}
        }
    }

    fun loadReports() {
        viewModelScope.launch {
            try {
                val snapshot = database.child("reports").get().await()
                val list = mutableListOf<ReportData>()
                var weekCount = 0
                val weekStart = weekStart()
                snapshot.children.forEach { child ->
                    val createdAt = child.child("createdAt").value?.toString()?.toLongOrNull() ?: 0L
                    list.add(ReportData(
                        reportId     = child.key ?: "",
                        reason       = child.child("reason").value?.toString() ?: "",
                        reportedBy   = child.child("reportedBy").value?.toString() ?: "",
                        reportedUser = child.child("reportedUser").value?.toString() ?: "",
                        status       = child.child("status").value?.toString() ?: "open",
                        adminReply   = child.child("adminReply").value?.toString() ?: "",
                        createdAt    = createdAt
                    ))
                    if (createdAt > weekStart) weekCount++
                }
                _reports.value = list
                _reportsThisWeek.value = weekCount
                buildRecentActivity()
            } catch (_: Exception) {}
        }
    }

    fun resolveReport(reportId: String) {
        viewModelScope.launch {
            try {
                database.child("reports").child(reportId).child("status").setValue("resolved").await()
                _reports.value = _reports.value.map {
                    if (it.reportId == reportId) it.copy(status = "resolved") else it
                }
            } catch (_: Exception) {}
        }
    }

    fun replyToReport(reportId: String, reply: String) {
        viewModelScope.launch {
            try {
                database.child("reports").child(reportId).child("adminReply").setValue(reply).await()
                database.child("reports").child(reportId).child("status").setValue("resolved").await()
                _reports.value = _reports.value.map {
                    if (it.reportId == reportId) it.copy(adminReply = reply, status = "resolved") else it
                }
            } catch (_: Exception) {}
        }
    }

    private fun buildRecentActivity() {
        val now = System.currentTimeMillis()
        val activities = mutableListOf<ActivityItem>()
        _users.value.sortedByDescending { it.createdAt }.take(3).forEach { user ->
            if (user.createdAt > 0) activities.add(ActivityItem(
                type = "user", title = "New user registered",
                subtitle = user.name.ifEmpty { user.email },
                timestamp = user.createdAt, timeAgo = getTimeAgo(user.createdAt, now)
            ))
        }
        _stores.value.sortedByDescending { it.createdAt }.take(3).forEach { store ->
            if (store.createdAt > 0) activities.add(ActivityItem(
                type = "store", title = "New store added",
                subtitle = store.name, timestamp = store.createdAt,
                timeAgo = getTimeAgo(store.createdAt, now)
            ))
        }
        _reports.value.sortedByDescending { it.createdAt }.take(3).forEach { report ->
            if (report.createdAt > 0) activities.add(ActivityItem(
                type = "report", title = "Report submitted",
                subtitle = report.reason.take(40), timestamp = report.createdAt,
                timeAgo = getTimeAgo(report.createdAt, now)
            ))
        }
        _recentActivity.value = activities.sortedByDescending { it.timestamp }.take(10)
    }

    private fun getTimeAgo(timestamp: Long, now: Long): String {
        val diff    = now - timestamp
        val minutes = diff / 60000
        val hours   = diff / 3600000
        val days    = diff / 86400000
        return when {
            minutes < 1  -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24   -> "$hours hour${if (hours > 1) "s" else ""} ago"
            else         -> "$days day${if (days > 1) "s" else ""} ago"
        }
    }
}