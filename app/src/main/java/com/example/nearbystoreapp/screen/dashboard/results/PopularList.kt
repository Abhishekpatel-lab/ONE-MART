package com.example.nearbystoreapp.screen.dashboard.results

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nearbystoreapp.R
import com.example.nearbystoreapp.model.StoreModel
import com.example.nearbystoreapp.ui.theme.NearbyStoreappTheme

// ─────────────────────────────────────────────
// Single Popular Store Card
// ─────────────────────────────────────────────

@Composable
fun ItemsPopular(
    item: StoreModel,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .wrapContentSize()
            .background(colorResource(R.color.black3), RoundedCornerShape(10.dp))
            .padding(8.dp)
            .clickable { onClick() }        // ✅ onClick() — invoke karo
    ) {
        AsyncImage(
            model              = item.imagePath,    // ✅ camelCase
            contentDescription = null,
            modifier           = Modifier
                .size(135.dp, 90.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colorResource(R.color.grey), RoundedCornerShape(10.dp)),
            contentScale       = ContentScale.Crop
        )
        Text(
            text       = item.title,                // ✅ camelCase
            color      = colorResource(R.color.white),
            modifier   = Modifier.padding(top = 8.dp),
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
            fontSize   = 14.sp
        )
        Row(modifier = Modifier.padding(top = 8.dp)) {
            Image(
                painter            = painterResource(R.drawable.location),
                contentDescription = null
            )
            Text(
                text       = item.shortAddress,     // ✅ camelCase
                color      = colorResource(R.color.white),
                maxLines   = 1,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.padding(start = 8.dp),
                overflow   = TextOverflow.Ellipsis,
                fontSize   = 12.sp
            )
        }
    }
}

// ─────────────────────────────────────────────
// Popular Stores Section
// ─────────────────────────────────────────────

@Composable
fun PopularSection(
    list: SnapshotStateList<StoreModel>,
    showPopularLoading: Boolean,
    onStoreClick: (StoreModel) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
        ) {
            Text(
                text       = "Popular Stores",
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

        if (showPopularLoading) {
            Box(
                modifier         = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colorResource(R.color.gold))
            }
        } else {
            LazyRow(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding        = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp)
            ) {
                items(list) { item ->                   // ✅ items(list) — cleaner
                    ItemsPopular(item = item, onClick = { onStoreClick(item) })
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
fun ItemsPopularPreview() {
    val item = StoreModel(
        title        = "Store Title",   // ✅ camelCase
        address      = "123 Main St",
        shortAddress = "Main St"
    )
    NearbyStoreappTheme { ItemsPopular(item = item, onClick = {}) }
}

@Preview
@Composable
fun PopularSectionPreview() {
    val list = remember {
        mutableStateListOf(
            StoreModel(title = "Store 1", shortAddress = "Address 1"),
            StoreModel(title = "Store 2", shortAddress = "Address 2")
        )
    }
    NearbyStoreappTheme {
        PopularSection(list = list, showPopularLoading = false, onStoreClick = {})
    }
}