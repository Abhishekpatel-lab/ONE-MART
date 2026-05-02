package com.example.nearbystoreapp.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nearbystoreapp.R
import com.example.nearbystoreapp.domain.CategoryModel

@Composable
fun CategoryItem(
    category: CategoryModel,
    modifier: Modifier = Modifier,
    onItemClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = colorResource(R.color.black3),
                shape = RoundedCornerShape(13.dp)
            )
            .clickable(onClick = onItemClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = category.ImagePath,
            contentDescription = null,
            modifier = Modifier.size(65.dp)
        )
        Text(
            text = category.Name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun CategorySection(
    categories: SnapshotStateList<CategoryModel>,
    showCategoryLoading: Boolean,
    onCategoryClick: (id: String, title: String) -> Unit
) {
    Text(
        text = "Explorer Store",
        fontSize = 20.sp,
        color = colorResource(R.color.gold),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp)
    )
    if (showCategoryLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = 200.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        val rows = categories.chunked(size = 3)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    row.forEach { categoryModel ->
                        CategoryItem(
                            category = categoryModel,
                            modifier = Modifier
                                .weight(weight = 1f)
                                .padding(horizontal = 8.dp),
                            onItemClick = {
                                onCategoryClick(
                                    categoryModel.Id.toString(),
                                    categoryModel.Name
                                )
                            }
                        )
                    }
                    if (row.size < 3) {
                        repeat(times = 3 - row.size) {
                            Spacer(Modifier.weight(weight = 1f))
                        }
                    }
                }
            }
        }
    }
}