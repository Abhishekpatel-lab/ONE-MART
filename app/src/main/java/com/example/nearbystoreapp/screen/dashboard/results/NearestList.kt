package com.example.nearbystoreapp.screen.dashboard.results

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nearbystoreapp.R
import com.example.nearbystoreapp.model.StoreModel
import com.example.nearbystoreapp.ui.theme.NearbyStoreappTheme

// ─────────────────────────────────────────────
// Store Detail (text info)
// ─────────────────────────────────────────────

@Composable
fun StoreDetail(item: StoreModel) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(start = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text       = item.title,        // ✅
            color      = Color.White,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines   = 1
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter            = painterResource(R.drawable.location),
                contentDescription = null
            )
            Text(
                text     = item.address,    // ✅
                color    = Color.White,
                fontSize = 12.sp,
                maxLines = 1,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Text(
            text       = item.activity,     // ✅ Activty typo fix
            color      = Color.White,
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines   = 1
        )
        Text(
            text       = "Hours : ${item.hours}",  // ✅
            color      = Color.White,
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines   = 1
        )
    }
}

// ─────────────────────────────────────────────
// Store Image
// ─────────────────────────────────────────────

@Composable
fun StoreImage(item: StoreModel) {
    AsyncImage(
        model              = item.imagePath,   // ✅
        contentDescription = null,
        modifier           = Modifier
            .size(95.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colorResource(R.color.grey), RoundedCornerShape(10.dp)),
        contentScale       = ContentScale.Crop
    )
}

// ─────────────────────────────────────────────
// Single Store Row
// ─────────────────────────────────────────────

@Composable
fun ItemsNearest(
    item: StoreModel,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.black3), RoundedCornerShape(10.dp))
            .padding(8.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        StoreImage(item = item)
        StoreDetail(item = item)
    }
}

// ─────────────────────────────────────────────
// Nearest Stores List
// ─────────────────────────────────────────────

@Composable
fun NearestLisst(
    list: SnapshotStateList<StoreModel>,
    showNearestLoading: Boolean,
    onStoreClick: (StoreModel) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
        ) {
            Text(
                text       = "Nearest Stores",
                color      = colorResource(R.color.gold),
                fontSize   = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.weight(1f)
            )
            Text(
                text  = "See all",
                color = Color.White,
                fontSize = 16.sp,
                style = TextStyle(textDecoration = TextDecoration.Underline)
            )
        }

        if (showNearestLoading) {
            Box(
                modifier         = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colorResource(R.color.gold))
            }
        } else {
            LazyColumn(
                modifier            = Modifier.height(400.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp)
            ) {
                items(list) { store ->
                    ItemsNearest(
                        item    = store,
                        onClick = { onStoreClick(store) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────

@Preview
@Composable
fun StoreDetailPreview() {
    val sample = StoreModel(
        title    = "Store Title",      // ✅
        address  = "123 Main St",
        hours    = "9am - 5pm",
        activity = "Open"
    )
    NearbyStoreappTheme { StoreDetail(item = sample) }
}

@Preview
@Composable
fun ItemsNearestPreview() {
    val sample = StoreModel(
        title    = "Store Title",
        address  = "123 Main St",
        hours    = "9am - 5pm",
        activity = "Open"
    )
    NearbyStoreappTheme { ItemsNearest(item = sample) }
}

@Preview
@Composable
fun NearestLisstPreview() {
    val sampleList = remember {
        mutableStateListOf(
            StoreModel(title = "Store 1", address = "Address 1", hours = "9am - 5pm",  activity = "Open"),
            StoreModel(title = "Store 2", address = "Address 2", hours = "10am - 6pm", activity = "Open")
        )
    }
    NearbyStoreappTheme {
        NearestLisst(list = sampleList, showNearestLoading = false, onStoreClick = {})
    }
}