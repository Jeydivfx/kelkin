package com.example.kelkin

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import kotlinx.coroutines.delay

private const val TAG = "SidebarDebug"

val VazirBold = FontFamily(Font(R.font.vazir_bold, FontWeight.Bold))

@Composable
fun Sidebar(
    navController: NavController,
    onMenuSelected: (String) -> Unit,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    val focusManager = LocalFocusManager.current

    val homeFR = remember { FocusRequester() }
    val movieFR = remember { FocusRequester() }
    val seriesFR = remember { FocusRequester() }
    val tvFR = remember { FocusRequester() }
    val searchFR = remember { FocusRequester() }
    val settingsFR = remember { FocusRequester() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentId = navBackStackEntry?.destination?.id

    val isInDetail = currentId == R.id.movieDetailFragment || currentId == R.id.seriesDetailsFragment

    val selectedItem = remember(currentId) {
        val item = when (currentId) {
            R.id.homeFragment, R.id.movieDetailFragment, R.id.seriesDetailsFragment -> homeFR
            R.id.moviesFragment -> movieFR
            R.id.seriesFragment -> seriesFR
            R.id.tvFragment -> tvFR
            R.id.searchFragment -> searchFR
            R.id.settingsFragment -> settingsFR
            else -> homeFR
        }
        Log.d(TAG, "Selected Item resolved for currentId: $currentId")
        item
    }

    LaunchedEffect(isExpanded) {
        Log.d(TAG, "LaunchedEffect(isExpanded) -> isExpanded: $isExpanded, isInDetail: $isInDetail")
        if (isExpanded && !isInDetail) {
            Log.d(TAG, "Requesting direct focus on selectedItem to prevent flash.")
            selectedItem.requestFocus()
        }
    }

    LaunchedEffect(currentId) {
        Log.d(TAG, "LaunchedEffect(currentId) -> currentId: $currentId, isInDetail: $isInDetail")
        if (isInDetail) {
            onExpandedChange(false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(if (isExpanded) 140.dp else 60.dp)
            .padding(top = 40.dp, start = 8.dp, end = 8.dp)
            .animateContentSize(animationSpec = tween(durationMillis = 200))
            .onFocusChanged { focusState ->
                Log.d(TAG, "Column focus changed -> hasFocus: ${focusState.hasFocus}, isExpanded: $isExpanded")
                if (!focusState.hasFocus && isExpanded) {
                    onExpandedChange(false)
                }
            },
        horizontalAlignment = Alignment.End
    ) {
        SidebarItem(
            icon = R.drawable.ic_menu_home,
            text = "خانه",
            isExpanded = isExpanded,
            isSelected = currentId == R.id.homeFragment || currentId == R.id.movieDetailFragment,
            focusRequester = homeFR,
            isDefaultItem = currentId == R.id.homeFragment || currentId == R.id.movieDetailFragment,
            isInDetail = isInDetail,
            onFocus = {
                Log.d(TAG, "Item [خانه] focused. isExpanded: $isExpanded")
                onExpandedChange(true)
            },
            onClick = {
                Log.d(TAG, "Item [خانه] clicked.")
                onExpandedChange(false)
                onMenuSelected("home")
            }
        )

        SidebarItem(
            icon = R.drawable.ic_menu_movie,
            text = "فیلم ها",
            isExpanded = isExpanded,
            isSelected = currentId == R.id.moviesFragment,
            focusRequester = movieFR,
            isDefaultItem = currentId == R.id.moviesFragment,
            isInDetail = isInDetail,
            onFocus = {
                Log.d(TAG, "Item [فیلم ها] focused. isExpanded: $isExpanded")
                onExpandedChange(true)
            },
            onClick = {
                Log.d(TAG, "Item [فیلم ها] clicked.")
                onExpandedChange(false)
                onMenuSelected("movies")
            }
        )

        SidebarItem(
            icon = R.drawable.ic_series,
            text = "سریال ها",
            isExpanded = isExpanded,
            isSelected = currentId == R.id.seriesFragment || currentId == R.id.seriesDetailsFragment,
            focusRequester = seriesFR,
            isDefaultItem = currentId == R.id.seriesFragment || currentId == R.id.seriesDetailsFragment,
            isInDetail = isInDetail,
            onFocus = {
                Log.d(TAG, "Item [سریال ها] focused. isExpanded: $isExpanded")
                onExpandedChange(true)
            },
            onClick = {
                Log.d(TAG, "Item [سریال ها] clicked.")
                onExpandedChange(false)
                onMenuSelected("series")
            }
        )

        SidebarItem(
            icon = R.drawable.ic_menu_tv,
            text = "تلویزیون",
            isExpanded = isExpanded,
            isSelected = currentId == R.id.tvFragment,
            focusRequester = tvFR,
            isDefaultItem = currentId == R.id.tvFragment,
            isInDetail = isInDetail,
            onFocus = {
                Log.d(TAG, "Item [تلویزیون] focused. isExpanded: $isExpanded")
                onExpandedChange(true)
            },
            onClick = {
                Log.d(TAG, "Item [تلویزیون] clicked.")
                onExpandedChange(false)
                onMenuSelected("tv")
            }
        )

        SidebarItem(
            icon = R.drawable.ic_menu_search,
            text = "جستجو",
            isExpanded = isExpanded,
            isSelected = currentId == R.id.searchFragment,
            focusRequester = searchFR,
            isDefaultItem = currentId == R.id.searchFragment,
            isInDetail = isInDetail,
            onFocus = {
                Log.d(TAG, "Item [جستجو] focused. isExpanded: $isExpanded")
                onExpandedChange(true)
            },
            onClick = {
                Log.d(TAG, "Item [جستجو] clicked.")
                onExpandedChange(false)
                onMenuSelected("search")
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        SidebarItem(
            icon = R.drawable.ic_menu_settings,
            text = "تنظیمات",
            isExpanded = isExpanded,
            isSelected = currentId == R.id.settingsFragment,
            focusRequester = settingsFR,
            isDefaultItem = currentId == R.id.settingsFragment,
            isInDetail = isInDetail,
            onFocus = {
                Log.d(TAG, "Item [تنظیمات] focused. isExpanded: $isExpanded")
                onExpandedChange(true)
            },
            onClick = {
                Log.d(TAG, "Item [تنظیمات] clicked.")
                onExpandedChange(false)
                onMenuSelected("settings")
            }
        )

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun SidebarItem(
    icon: Int,
    text: String,
    isExpanded: Boolean,
    isSelected: Boolean,
    focusRequester: FocusRequester,
    isDefaultItem: Boolean,
    isInDetail: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }

    val shouldShowSelected = if (isInDetail) {
        false
    } else {
        isSelected || (isDefaultItem && !isFocused)
    }

    val backgroundColor = if (isFocused) Color.White else Color.Transparent

    val contentColor = when {
        isFocused -> Color.Black
        shouldShowSelected -> Color.White
        else -> Color.Gray
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) {
                    onFocus()
                }
            }
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onClick() }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (isExpanded) {
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = 13.sp,
                    fontFamily = VazirBold
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}