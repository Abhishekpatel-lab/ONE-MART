package com.example.nearbystoreapp.screen.dashboard.results

import android.widget.Space
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.example.nearbystoreapp.R
import com.example.nearbystoreapp.domain.CategoryModel

@Composable
fun Category(item: CategoryModel,onItemClick: ()-> Unit){
    Column(
        modifier = Modifier
            .size(width = 85.dp, height = 95.dp)
            .background(color = colorResource(R.color.black3),
                shape = RoundedCornerShape(size = 10.dp)
            )
            .clickable(onClick = onItemClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        AsyncImage(
            model = item.ImagePath,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 16.dp)
                .size(45.dp, height = 40.dp)
        )
        Spacer(modifier = Modifier.padding(top = 12.dp))
        Text(
            text = item.Name,
            color=Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview
@Composable
fun CategoryPreview(){
    val item = CategoryModel(Id = 0, ImagePath = "" , Name = "Name")
    Category(item = item, onItemClick = {})
}

@Composable
fun SubCategory(
    subCategory: SnapshotStateList<CategoryModel>,
    showSubCategoryLoading: Boolean
){
    if (showSubCategoryLoading){
        Box(Modifier
            .fillMaxWidth()
            .height(height = 100.dp),
            contentAlignment = Alignment.Center
        ){
            CircularProgressIndicator()
        }
    }else{
        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space =  12.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp)
        ){
         items(subCategory.size){index ->
             Category(item = subCategory[index], onItemClick = {})
         }
        }

        }
    }
