package com.example.nearbystoreapp.screen.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nearbystoreapp.R
import com.example.nearbystoreapp.viewModel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val cloudName = stringResource(R.string.cloudinary_cloud_name)
    val uploadPreset = stringResource(R.string.cloudinary_upload_preset)

    var userName by remember { mutableStateOf("") }
    var profilePicUrl by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var uploadingImage by remember { mutableStateOf(false) }

    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val database = FirebaseDatabase.getInstance().reference

    // Load user data
    LaunchedEffect(uid) {
        uid?.let {
            database.child("users").child(it).get()
                .addOnSuccessListener { snapshot ->
                    userName = snapshot.child("name").value?.toString() ?: "User"
                    profilePicUrl = snapshot.child("profilePic").value?.toString() ?: ""
                    editName = userName
                }
        }
    }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            uploadingImage = true
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes() ?: return@launch
                    inputStream.close()

                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                            "file",
                            "profile.jpg",
                            bytes.toRequestBody("image/*".toMediaTypeOrNull())
                        )
                        .addFormDataPart("upload_preset", uploadPreset)
                        .build()

                    val request = Request.Builder()
                        .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                        .post(requestBody)
                        .build()

                    val response = OkHttpClient().newCall(request).execute()
                    val responseBody = response.body?.string()
                    val json = JSONObject(responseBody ?: "{}")
                    val imageUrl = json.getString("secure_url")

                    // Save to Firebase
                    uid?.let { userId ->
                        database.child("users").child(userId)
                            .child("profilePic").setValue(imageUrl)
                    }

                    withContext(Dispatchers.Main) {
                        profilePicUrl = imageUrl
                        uploadingImage = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        uploadingImage = false
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.black3))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Profile Picture
            Box(
                contentAlignment = Alignment.BottomEnd
            ) {
                if (uploadingImage) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color.DarkGray, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = colorResource(R.color.gold),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                } else if (profilePicUrl.isNotEmpty()) {
                    AsyncImage(
                        model = profilePicUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(colorResource(R.color.gold), CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "👤", fontSize = 48.sp)
                    }
                }

                // Camera icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(colorResource(R.color.gold), CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "📷", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap to change photo",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Name Section
            if (isEditing) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Name", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorResource(R.color.gold),
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        isLoading = true
                        uid?.let { userId ->
                            database.child("users").child(userId)
                                .child("name").setValue(editName)
                                .addOnSuccessListener {
                                    userName = editName
                                    isEditing = false
                                    isLoading = false
                                }
                                .addOnFailureListener {
                                    isLoading = false
                                }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.gold)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "Save",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { isEditing = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Cancel", color = Color.White)
                }

            } else {
                Text(
                    text = userName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.gold)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "✏️ Edit Name",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.clickable { isEditing = true }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Back Button
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colorResource(R.color.gold)
                )
            ) {
                Text(text = "Back", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Logout Button
            Button(
                onClick = {
                    authViewModel.logout()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                )
            ) {
                Text(
                    text = "Logout",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}