package com.example.nearbystoreapp.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nearbystoreapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

// ─── Data class ───────────────────────────────────────────
data class ReviewModel(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val timestamp: Long = 0L,
    val targetType: String = "store", // "store" ya "item"
    val targetId: String = ""
)

// ─── Star Rating Bar ──────────────────────────────────────
@Composable
fun StarRatingBar(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "$i star",
                tint = colorResource(R.color.gold),
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onRatingChange(i) }
                    .padding(2.dp)
            )
        }
    }
}

// ─── Star Display (read-only) ─────────────────────────────
@Composable
fun StarDisplay(rating: Float, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        val fullStars = rating.toInt()
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= fullStars) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = colorResource(R.color.gold),
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = String.format("%.1f", rating),
            color = colorResource(R.color.gold),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── Review Input Box ─────────────────────────────────────
@Composable
fun ReviewInputSection(
    targetType: String,   // "store" or "item"
    targetId: String,
    targetName: String,
    onReviewSubmitted: () -> Unit
) {
    var selectedRating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var userName by remember { mutableStateOf("User") }

    LaunchedEffect(Unit) {
        uid.let {
            FirebaseDatabase.getInstance().reference
                .child("users").child(it).get()
                .addOnSuccessListener { snap ->
                    userName = snap.child("name").value?.toString() ?: "User"
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        // Header — tap to expand/collapse
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "✍️ Write Review",
                color = colorResource(R.color.gold),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = if (expanded) "▲" else "▼",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))

            if (submitted) {
                Text(
                    "✅Reviwe submitted",
                    color = Color.Green,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = {
                    submitted = false
                    selectedRating = 0
                    comment = ""
                }) {
                    Text("Write another review", color = colorResource(R.color.gold))
                }
            } else {
                Text("Give a rating:", color = Color.Gray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                StarRatingBar(
                    rating = selectedRating,
                    onRatingChange = { selectedRating = it }
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = { Text("Write your review here...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().height(90.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorResource(R.color.gold),
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = colorResource(R.color.gold)
                    ),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (selectedRating > 0) {
                            isSubmitting = true
                            val review = mapOf(
                                "userId"     to uid,
                                "userName"   to userName,
                                "rating"     to selectedRating,
                                "comment"    to comment,
                                "timestamp"  to System.currentTimeMillis(),
                                "targetType" to targetType,
                                "targetId"   to targetId
                            )
                            // Firebase path: reviews/store/{storeId}/{pushId}
                            //             or reviews/item/{itemId}/{pushId}
                            FirebaseDatabase.getInstance().reference
                                .child("reviews")
                                .child(targetType)
                                .child(targetId)
                                .push()
                                .setValue(review)
                                .addOnSuccessListener {
                                    isSubmitting = false
                                    submitted = true
                                    onReviewSubmitted()
                                }
                                .addOnFailureListener { isSubmitting = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedRating > 0 && !isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.gold),
                        disabledContainerColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                    } else {
                        Text("⭐ Submit Review", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─── Reviews List ─────────────────────────────────────────
@Composable
fun ReviewsSection(
    targetType: String,  // "store" or "item"
    targetId: String,
    refreshTrigger: Int  // increment this to refresh
) {
    var reviews by remember { mutableStateOf<List<ReviewModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var avgRating by remember { mutableStateOf(0f) }

    LaunchedEffect(targetId, refreshTrigger) {
        isLoading = true
        FirebaseDatabase.getInstance().reference
            .child("reviews")
            .child(targetType)
            .child(targetId)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<ReviewModel>()
                for (child in snapshot.children) {
                    list.add(ReviewModel(
                        id        = child.key ?: "",
                        userId    = child.child("userId").value?.toString() ?: "",
                        userName  = child.child("userName").value?.toString() ?: "User",
                        rating    = child.child("rating").value?.toString()?.toIntOrNull() ?: 0,
                        comment   = child.child("comment").value?.toString() ?: "",
                        timestamp = child.child("timestamp").value?.toString()?.toLongOrNull() ?: 0L,
                        targetType = targetType,
                        targetId  = targetId
                    ))
                }
                // Newest pehle
                reviews = list.sortedByDescending { it.timestamp }
                avgRating = if (list.isEmpty()) 0f else list.map { it.rating }.average().toFloat()
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ─── Header + Average Rating ───────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⭐ Reviews",
                color = colorResource(R.color.gold),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            if (reviews.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StarDisplay(rating = avgRating)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${reviews.size})",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = colorResource(R.color.gold),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            reviews.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(10.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No reviews yet",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
            else -> {
                reviews.forEach { review ->
                    ReviewCard(review = review)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// ─── Single Review Card ───────────────────────────────────
@Composable
fun ReviewCard(review: ReviewModel) {
    val dateStr = remember(review.timestamp) {
        if (review.timestamp > 0) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(Date(review.timestamp))
        } else ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(colorResource(R.color.gold), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = review.userName.firstOrNull()?.uppercase() ?: "U",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = review.userName,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    if (dateStr.isNotEmpty()) {
                        Text(text = dateStr, color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
            // Stars
            Row {
                for (i in 1..5) {
                    Icon(
                        imageVector = if (i <= review.rating) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = colorResource(R.color.gold),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (review.comment.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = review.comment,
                color = Color.LightGray,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}