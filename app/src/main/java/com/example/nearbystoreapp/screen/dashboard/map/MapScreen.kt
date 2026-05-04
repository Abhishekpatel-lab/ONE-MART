package com.example.nearbystoreapp.screen.dashboard.map

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.nearbystoreapp.R
import com.example.nearbystoreapp.model.StoreModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@Composable
fun MapScreen(store: StoreModel) {
    val latlng = LatLng(store.latitude, store.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(latlng, 15f)
    }
    val context = LocalContext.current

    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (map, detail) = createRefs()

        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .constrainAs(map) { centerTo(parent) },
            cameraPositionState = cameraPositionState
        ) {
            Marker(
                state = rememberMarkerState(position = latlng),
                title = store.title
            )
        }

        LazyColumn(
            modifier = Modifier
                .wrapContentHeight()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .fillMaxWidth()
                .background(colorResource(R.color.black3), RoundedCornerShape(10.dp))
                .padding(16.dp)
                .constrainAs(detail) {
                    centerHorizontallyTo(parent)
                    bottom.linkTo(parent.bottom)
                }
        ) {
            // ✅ ItemsNearest ki jagah seedha store info
            item {
                Text(
                    text = store.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📍 ${store.address}",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🕐 ${store.hours}",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🟢 ${store.activity}",
                    color = Color.Green,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                Button(
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.gold)
                    ),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth(),
                    onClick = {
                        val dialIntent = Intent(
                            Intent.ACTION_DIAL,
                            Uri.parse("tel:${store.call}")
                        )
                        context.startActivity(dialIntent)
                    }
                ) {
                    Text(
                        text = "📞 Call Now",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}