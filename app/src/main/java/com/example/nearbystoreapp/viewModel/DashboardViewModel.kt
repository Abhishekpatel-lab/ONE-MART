package com.example.nearbystoreapp.viewModel

import androidx.lifecycle.LiveData
import com.example.nearbystoreapp.domain.BannerModel
import com.example.nearbystoreapp.domain.CategoryModel

class DashboardViewModel {

    private val repository= DashboardViewModel()
    fun loadCategory(): LiveData<MutableList<CategoryModel>>{
        return repository.loadCategory()
    }

    fun loadBanner(): LiveData<MutableList<BannerModel>>{
        return repository.loadBanner()
    }
}