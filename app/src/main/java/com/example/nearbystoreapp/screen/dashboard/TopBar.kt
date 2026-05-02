package com.example.nearbystoreapp.screen.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nearbystoreapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

// ✅ private — sirf is file mein use hoga, clash nahi hoga
private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11  -> "Good Morning 🌅"
        in 12..16 -> "Good Afternoon ☀️"
        in 17..20 -> "Good Evening 🌆"
        else      -> "Good Night 🌙"
    }
}

@Composable
fun TopBar(
    onProfileClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    var userName by remember { mutableStateOf("User") }
    var profilePicUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        uid?.let {
            FirebaseDatabase.getInstance().reference
                .child("users").child(it).get()
                .addOnSuccessListener { snapshot ->
                    userName = snapshot.child("name").value?.toString() ?: "User"
                    profilePicUrl = snapshot.child("profilePic").value?.toString() ?: ""
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.black2))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "👋", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = getGreeting(),
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = userName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.gold)
                    )
                }
            }

            if (profilePicUrl.isNotEmpty()) {
                AsyncImage(
                    model = profilePicUrl,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onProfileClick() },
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(colorResource(R.color.gold), CircleShape)
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👤", fontSize = 20.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.one_mart_logo),
                contentDescription = "One Mart Logo",
                modifier = Modifier
                    .height(110.dp)
                    .fillMaxWidth(0.6f),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your nearby stores, one tap away 🛍️",
                fontSize = 16.sp,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    colorResource(R.color.black3),
                    RoundedCornerShape(12.dp)
                )
                .clickable { onSearchClick() }
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🔍", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Search stores, products...",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}