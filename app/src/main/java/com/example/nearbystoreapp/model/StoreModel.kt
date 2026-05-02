package com.example.nearbystoreapp.model

import com.google.firebase.database.PropertyName

data class StoreModel(
    var id: String = "",
    var firebaseKey: String = "",

    @get:PropertyName("CategoryId")
    @set:PropertyName("CategoryId")
    var categoryId: String = "",

    @get:PropertyName("Category")
    @set:PropertyName("Category")
    var category: String = "",

    @get:PropertyName("Title")
    @set:PropertyName("Title")
    var title: String = "",

    @get:PropertyName("Description")
    @set:PropertyName("Description")
    var description: String = "",

    @get:PropertyName("Latitude")
    @set:PropertyName("Latitude")
    var latitude: Double = 0.0,

    @get:PropertyName("Longitude")
    @set:PropertyName("Longitude")
    var longitude: Double = 0.0,

    @get:PropertyName("Address")
    @set:PropertyName("Address")
    var address: String = "",

    @get:PropertyName("ShortAddress")
    @set:PropertyName("ShortAddress")
    var shortAddress: String = "",

    @get:PropertyName("Call")
    @set:PropertyName("Call")
    var call: String = "",

    @get:PropertyName("Activity")
    @set:PropertyName("Activity")
    var activity: String = "",

    @get:PropertyName("Hours")
    @set:PropertyName("Hours")
    var hours: String = "",

    @get:PropertyName("ImagePath")
    @set:PropertyName("ImagePath")
    var imagePath: String = "",

    var rating: Double = 0.0,
    var orderCount: Int = 0,

    @get:PropertyName("isBlocked")
    @set:PropertyName("isBlocked")
    var isBlocked: Boolean = false
)