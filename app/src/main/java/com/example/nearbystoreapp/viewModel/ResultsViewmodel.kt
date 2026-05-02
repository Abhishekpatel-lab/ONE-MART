package com.example.nearbystoreapp.viewModel

import androidx.lifecycle.LiveData
import com.example.nearbystoreapp.domain.CategoryModel
import com.example.nearbystoreapp.model.StoreModel
import com.example.nearbystoreapp.repository.ResultsRepository

class ResultsViewmodel {
    private val repository = ResultsRepository()

    fun loadSubCategory(id: String): LiveData<MutableList<CategoryModel>> {
        return repository.loadSubCategory(id)
    }

    fun loadPopular(id: String): LiveData<MutableList<StoreModel>> {
        return repository.loadPopular(id)
    }

    // ✅ userLat, userLon pass karo
    fun loadNearest(
        id: String,
        userLat: Double = 0.0,
        userLon: Double = 0.0
    ): LiveData<MutableList<StoreModel>> {
        return repository.loadNearest(id, userLat, userLon)
    }
}