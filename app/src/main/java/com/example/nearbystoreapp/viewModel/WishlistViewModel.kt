package com.example.nearbystoreapp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nearbystoreapp.model.StoreModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WishlistViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    private val _wishlistKeys = MutableStateFlow<Set<String>>(emptySet())
    val wishlistKeys: StateFlow<Set<String>> = _wishlistKeys

    private val _wishlistStores = MutableStateFlow<List<StoreModel>>(emptyList())
    val wishlistStores: StateFlow<List<StoreModel>> = _wishlistStores

    // ✅ App start pe wishlist load karo
    fun loadWishlist() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            try {
                val snapshot = database.child("wishlists").child(uid).get().await()
                val keys = mutableSetOf<String>()
                val stores = mutableListOf<StoreModel>()
                snapshot.children.forEach { child ->
                    val firebaseKey = child.key ?: return@forEach
                    keys.add(firebaseKey)
                    val store = StoreModel(
                        firebaseKey  = firebaseKey,
                        title        = child.child("title").value?.toString() ?: "",
                        address      = child.child("address").value?.toString() ?: "",
                        shortAddress = child.child("shortAddress").value?.toString() ?: "",
                        imagePath    = child.child("imagePath").value?.toString() ?: "",
                        hours        = child.child("hours").value?.toString() ?: "",
                        call         = child.child("call").value?.toString() ?: "",
                        activity     = child.child("activity").value?.toString() ?: "",
                        categoryId   = child.child("categoryId").value?.toString() ?: "",
                        latitude     = child.child("latitude").value?.toString()?.toDoubleOrNull() ?: 0.0,
                        longitude    = child.child("longitude").value?.toString()?.toDoubleOrNull() ?: 0.0,
                    )
                    stores.add(store)
                }
                _wishlistKeys.value = keys
                _wishlistStores.value = stores
            } catch (e: Exception) {
                // silent fail
            }
        }
    }

    // ✅ Wishlist mein hai ya nahi check karo
    fun isWishlisted(firebaseKey: String): Boolean {
        return _wishlistKeys.value.contains(firebaseKey)
    }

    // ✅ Toggle — add ya remove
    fun toggleWishlist(store: StoreModel) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val key = store.firebaseKey.ifEmpty { store.id }
            if (key.isEmpty()) return@launch

            try {
                if (isWishlisted(key)) {
                    // ✅ Remove from wishlist
                    database.child("wishlists").child(uid).child(key).removeValue().await()
                    _wishlistKeys.value = _wishlistKeys.value - key
                    _wishlistStores.value = _wishlistStores.value.filter { it.firebaseKey != key }
                } else {
                    // ✅ Add to wishlist
                    val storeMap = mapOf(
                        "title"        to store.title,
                        "address"      to store.address,
                        "shortAddress" to store.shortAddress,
                        "imagePath"    to store.imagePath,
                        "hours"        to store.hours,
                        "call"         to store.call,
                        "activity"     to store.activity,
                        "categoryId"   to store.categoryId,
                        "latitude"     to store.latitude,
                        "longitude"    to store.longitude
                    )
                    database.child("wishlists").child(uid).child(key).setValue(storeMap).await()
                    _wishlistKeys.value = _wishlistKeys.value + key
                    _wishlistStores.value = _wishlistStores.value + store
                }
            } catch (e: Exception) {
                // silent fail
            }
        }
    }
}