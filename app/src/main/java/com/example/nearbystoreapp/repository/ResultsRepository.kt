package com.example.nearbystoreapp.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.nearbystoreapp.domain.CategoryModel
import com.example.nearbystoreapp.model.StoreModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import kotlin.math.*

class ResultsRepository {
    private val firebaseDatabase = FirebaseDatabase.getInstance()

    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun loadSubCategory(id: String): LiveData<MutableList<CategoryModel>> {
        val listData = MutableLiveData<MutableList<CategoryModel>>()
        val ref = firebaseDatabase.getReference("SubCategory")
        val query: Query = ref.orderByChild("CategoryId").equalTo(id)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<CategoryModel>()
                for (child in snapshot.children) {
                    val model = child.getValue(CategoryModel::class.java)
                    if (model != null) list.add(model)
                }
                listData.value = list
            }
            override fun onCancelled(error: DatabaseError) {
                listData.value = mutableListOf()
            }
        })
        return listData
    }

    fun loadPopular(id: String): LiveData<MutableList<StoreModel>> {
        val listData = MutableLiveData<MutableList<StoreModel>>()
        val ref = firebaseDatabase.getReference("Stores")
        val query: Query = ref.orderByChild("CategoryId").equalTo(id)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<StoreModel>()
                for (child in snapshot.children) {
                    val model = child.getValue(StoreModel::class.java)
                    if (model != null && model.isBlocked != true) {
                        model.firebaseKey = child.key ?: "" // ✅ Fix
                        list.add(model)
                    }
                }
                listData.value = list
            }
            override fun onCancelled(error: DatabaseError) {
                listData.value = mutableListOf()
            }
        })
        return listData
    }

    fun loadNearest(
        id: String,
        userLat: Double,
        userLon: Double
    ): LiveData<MutableList<StoreModel>> {
        val listData = MutableLiveData<MutableList<StoreModel>>()
        val ref = firebaseDatabase.getReference("Stores")
        val query: Query = ref.orderByChild("CategoryId").equalTo(id)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<StoreModel>()
                for (child in snapshot.children) {
                    val model = child.getValue(StoreModel::class.java)
                    if (model != null && model.isBlocked != true) {
                        model.firebaseKey = child.key ?: "" // ✅ Fix
                        list.add(model)
                    }
                }
                val sorted = if (userLat != 0.0 && userLon != 0.0) {
                    list.sortedBy { store ->
                        calculateDistance(userLat, userLon, store.latitude, store.longitude)
                    }.toMutableList()
                } else {
                    list
                }
                listData.value = sorted
            }
            override fun onCancelled(error: DatabaseError) {
                listData.value = mutableListOf()
            }
        })
        return listData
    }
}