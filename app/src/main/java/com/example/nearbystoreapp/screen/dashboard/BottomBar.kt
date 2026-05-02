package com.example.nearbystoreapp.screen.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.BottomAppBar
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nearbystoreapp.R

data class BottomMenuItem(
    val label: String,
    val icon: Painter
)

@Composable
fun prepareBottomMenu(): List<BottomMenuItem> {
    return listOf(
        BottomMenuItem(label = "Home", icon = painterResource(id = R.drawable.btn_1)),
        BottomMenuItem(label = "Support", icon = painterResource(id = R.drawable.btn_2)),
        BottomMenuItem(label = "Wishlist", icon = painterResource(id = R.drawable.btn_3)),
        BottomMenuItem(label = "Profile", icon = painterResource(id = R.drawable.btn_4))
    )
}

@Composable
fun BottomBar(
    selectedItem: String = "Home",
    onHomeClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onWishlistClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val bottomMenuItemList = prepareBottomMenu()
    var selected by remember { mutableStateOf(value = selectedItem) }

    BottomAppBar(
        backgroundColor = colorResource(id = R.color.black3),
        elevation = 3.dp
    ) {
        bottomMenuItemList.forEach { item ->
            BottomNavigationItem(
                selected = (selected == item.label),
                onClick = {
                    selected = item.label
                    when (item.label) {
                        "Home" -> onHomeClick()
                        "Support" -> onSupportClick()
                        "Wishlist" -> onWishlistClick()
                        "Profile" -> onProfileClick()
                    }
                },
                icon = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = item.icon,
                            contentDescription = null,
                            tint = if (selected == item.label)
                                colorResource(id = R.color.gold)
                            else
                                colorResource(id = R.color.white),
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .size(20.dp)
                        )
                        Text(
                            text = item.label,
                            fontSize = 12.sp,
                            color = if (selected == item.label)
                                colorResource(id = R.color.gold)
                            else
                                colorResource(id = R.color.white),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            )
        }
    }
}