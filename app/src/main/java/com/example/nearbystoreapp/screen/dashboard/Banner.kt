package com.example.nearbystoreapp.screen.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.nearbystoreapp.domain.BannerModel
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Sliding(
    pagerState: PagerState =remember{PagerState()},
    banners: List<BannerModel>
){
    HorizontalPager(count = banners.size,state = pagerState ) { page->
        AsyncImage(model= ImageRequest.Builder(context = LocalContext.current)
            .data(data = banners[page].image)
            .build(),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 16.dp)
                .clip(shape = RoundedCornerShape(size = 10.dp))
                .height(height = 150.dp)
        )

    }

}
@Composable
fun Banner(banners: SnapshotStateList<BannerModel>,showBannerLoading: Boolean){
    if (showBannerLoading){
        Box(
            modifier = Modifier
                .fillMaxSize()
                .height(height = 200.dp),
            contentAlignment= Alignment.Center
        ){
            CircularProgressIndicator()
        }
    }else{
        Sliding(banners = banners)
    }

}