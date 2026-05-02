package com.example.nearbystoreapp.util

import com.google.firebase.database.FirebaseDatabase

object FirebaseMigrationUtil {

    /**
     * Sabhi stores mein rating aur orderCount add karo
     * Sirf tab jab field already exist nahi karti
     */
    fun addMissingStoreFields() {
        val storesRef = FirebaseDatabase.getInstance().reference.child("stores")

        storesRef.get().addOnSuccessListener { snapshot ->
            for (store in snapshot.children) {
                val updates = mutableMapOf<String, Any>()

                // Rating field nahi hai toh add karo (default 0.0)
                if (store.child("rating").value == null) {
                    updates["rating"] = 0.0
                }

                // orderCount field nahi hai toh add karo (default 0)
                if (store.child("orderCount").value == null) {
                    updates["orderCount"] = 0
                }

                // Sirf tab update karo jab kuch missing ho
                if (updates.isNotEmpty()) {
                    store.ref.updateChildren(updates)
                        .addOnSuccessListener {
                            println("✅ Store '${store.key}' updated: $updates")
                        }
                        .addOnFailureListener {
                            println("❌ Store '${store.key}' update failed: ${it.message}")
                        }
                }
            }
        }.addOnFailureListener {
            println("❌ Stores fetch failed: ${it.message}")
        }
    }
}