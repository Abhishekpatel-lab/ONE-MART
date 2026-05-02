package com.example.nearbystoreapp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nearbystoreapp.screens.ReportData
import com.example.nearbystoreapp.screens.StoreData
import com.example.nearbystoreapp.screens.UserData
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadUsers() {
        viewModelScope.launch {
            try {
                val snapshot = database.child("users").get().await()
                val list = mutableListOf<UserData>()
                snapshot.children.forEach { child ->
                    val user = UserData(
                        uid      = child.key ?: "",
                        name     = child.child("name").value?.toString() ?: "",
                        email    = child.child("email").value?.toString() ?: "",
                        userType = child.child("userType").value?.toString() ?: "user",
                        isBanned = child.child("isBanned").getValue(Boolean::class.java) ?: false
                    )
                    list.add(user)
                }
                _users.value = list
            } catch (e: Exception) {
                _error.value = "Users load failed: ${e.message}"
            }
        }
    }

    fun toggleBan(uid: String, currentlyBanned: Boolean) {
        viewModelScope.launch {
            try {
                database.child("users").child(uid)
                    .child("isBanned").setValue(!currentlyBanned).await()
                _users.value = _users.value.map {
                    if (it.uid == uid) it.copy(isBanned = !currentlyBanned) else it
                }
            } catch (e: Exception) {
                _error.value = "Ban toggle failed: ${e.message}"
            }
        }
    }

    fun loadStores() {
        viewModelScope.launch {
            try {
                _isLoadingStores.value = true
                val snapshot = database.child("Stores").get().await()
                val list = mutableListOf<StoreData>()
                snapshot.children.forEach { child ->
                    val store = StoreData(
                        storeId    = child.key ?: "",
                        name       = child.child("Title").value?.toString() ?: "Unknown Store",
                        ownerEmail = child.child("Address").value?.toString() ?: "",
                        status     = child.child("status").value?.toString() ?: "approved",
                        isBlocked  = child.child("isBlocked").getValue(Boolean::class.java) ?: false
                    )
                    list.add(store)
                }
                _stores.value = list
            } catch (e: Exception) {
                _error.value = "Stores load failed: ${e.message}"
            } finally {
                _isLoadingStores.value = false
            }
        }
    }

    fun toggleStoreBlock(storeId: String, currentlyBlocked: Boolean) {
        viewModelScope.launch {
            try {
                val newBlockedValue = !currentlyBlocked
                database.child("Stores").child(storeId)
                    .child("isBlocked").setValue(newBlockedValue).await()
                val storeSnapshot = database.child("Stores").child(storeId).get().await()
                val storeTitle = storeSnapshot.child("Title").value?.toString() ?: ""
                if (storeTitle.isNotEmpty()) {
                    val nearestSnapshot = database.child("Nearest").get().await()
                    nearestSnapshot.children.forEach { child ->
                        val nearestTitle = child.child("Title").value?.toString() ?: ""
                        if (nearestTitle == storeTitle) {
                            child.ref.child("isBlocked").setValue(newBlockedValue).await()
                        }
                    }
                }
                _stores.value = _stores.value.map {
                    if (it.storeId == storeId) it.copy(isBlocked = newBlockedValue) else it
                }
            } catch (e: Exception) {
                _error.value = "Store block toggle failed: ${e.message}"
            }
        }
    }

    fun updateStoreStatus(storeId: String, status: String) {
        viewModelScope.launch {
            try {
                database.child("Stores").child(storeId)
                    .child("status").setValue(status).await()
                _stores.value = _stores.value.map {
                    if (it.storeId == storeId) it.copy(status = status) else it
                }
            } catch (e: Exception) {
                _error.value = "Store status update failed: ${e.message}"
            }
        }
    }

    fun loadReports() {
        viewModelScope.launch {
            try {
                val snapshot = database.child("reports").get().await()
                val list = mutableListOf<ReportData>()
                snapshot.children.forEach { child ->
                    val report = ReportData(
                        reportId     = child.key ?: "",
                        reason       = child.child("reason").value?.toString() ?: "",
                        reportedBy   = child.child("reportedBy").value?.toString() ?: "",
                        reportedUser = child.child("reportedUser").value?.toString() ?: "",
                        status       = child.child("status").value?.toString() ?: "open",
                        adminReply   = child.child("adminReply").value?.toString() ?: ""
                    )
                    list.add(report)
                }
                _reports.value = list
            } catch (e: Exception) {
                _error.value = "Reports load failed: ${e.message}"
            }
        }
    }

    fun resolveReport(reportId: String) {
        viewModelScope.launch {
            try {
                database.child("reports").child(reportId)
                    .child("status").setValue("resolved").await()
                _reports.value = _reports.value.map {
                    if (it.reportId == reportId) it.copy(status = "resolved") else it
                }
            } catch (e: Exception) {
                _error.value = "Report resolve failed: ${e.message}"
            }
        }
    }

    // ✅ Admin reply function
    fun replyToReport(reportId: String, reply: String) {
        viewModelScope.launch {
            try {
                database.child("reports").child(reportId)
                    .child("adminReply").setValue(reply).await()
                database.child("reports").child(reportId)
                    .child("status").setValue("resolved").await()
                _reports.value = _reports.value.map {
                    if (it.reportId == reportId)
                        it.copy(adminReply = reply, status = "resolved")
                    else it
                }
            } catch (e: Exception) {
                _error.value = "Reply failed: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}