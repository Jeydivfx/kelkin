package com.example.kelkin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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

val VazirBold = FontFamily(Font(R.font.vazir_bold, FontWeight.Bold))

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
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
    val currentId = navBackStackEntry?.destination?.id ?: navController.currentDestination?.id

    val isInDetail = currentId == R.id.movieDetailFragment || currentId == R.id.seriesDetailsFragment

    val selectedItem = remember(currentId) {
        when (currentId) {
            R.id.moviesFragment, R.id.movieDetailFragment -> movieFR
            R.id.seriesFragment, R.id.seriesDetailsFragment -> seriesFR
            R.id.tvFragment -> tvFR
            R.id.searchFragment -> searchFR
            R.id.settingsFragment, R.id.adminDashboardFragment, R.id.movieListFragment,
            R.id.seriesListFragment, R.id.channelListFragment, R.id.adminHeroFragment -> settingsFR
            else -> homeFR
        }
    }

    var isStabilized by remember { mutableStateOf(false) }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            isStabilized = false
            delay(250)
            isStabilized = true
        } else if (!isExpanded) {
            isStabilized = false
        }
    }

    // اگر کاربر وارد صفحه جزئیات شد، به طور پیش‌فرض منو بسته باشد تا تداخلی ایجاد نکند
    LaunchedEffect(currentId) {
        if (isInDetail) {
            onExpandedChange(false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(if (isExpanded) 140.dp else 60.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(
                Color(0xD91E242D), Color(0xE612171F), Color(0xED0C1016)
            )))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(top = 40.dp, start = 8.dp, end = 8.dp)
            .onFocusChanged { focusState ->
                if (!focusState.hasFocus && isExpanded) {
                    onExpandedChange(false)
                }
            }
            .focusProperties { enter = { selectedItem } }
            .focusGroup(),
        horizontalAlignment = Alignment.End
    ) {
        SidebarItem(
            icon = R.drawable.ic_menu_home,
            text = "خانه",
            isExpanded = isExpanded,
            isSelected = selectedItem === homeFR,
            focusRequester = homeFR,
            isDefaultItem = currentId == R.id.homeFragment,
            isStabilized = isStabilized,
            onFocus = {
                onExpandedChange(true)
            },
            onClick = {
                onExpandedChange(false)
                onMenuSelected("home")
            }
        )

        SidebarItem(
            icon = R.drawable.ic_menu_movie,
            text = "فیلم ها",
            isExpanded = isExpanded,
            isSelected = selectedItem === movieFR,
            focusRequester = movieFR,
            isDefaultItem = currentId == R.id.moviesFragment || currentId == R.id.movieDetailFragment,
            isStabilized = isStabilized,
            onFocus = {
                onExpandedChange(true)
            },
            onClick = {
                onExpandedChange(false)
                onMenuSelected("movies")
            }
        )

        SidebarItem(
            icon = R.drawable.ic_series,
            text = "سریال ها",
            isExpanded = isExpanded,
            isSelected = selectedItem === seriesFR,
            focusRequester = seriesFR,
            isDefaultItem = currentId == R.id.seriesFragment || currentId == R.id.seriesDetailsFragment,
            isStabilized = isStabilized,
            onFocus = {
                onExpandedChange(true)
            },
            onClick = {
                onExpandedChange(false)
                onMenuSelected("series")
            }
        )

        SidebarItem(
            icon = R.drawable.ic_menu_tv,
            text = "تلویزیون",
            isExpanded = isExpanded,
            isSelected = selectedItem === tvFR,
            focusRequester = tvFR,
            isDefaultItem = currentId == R.id.tvFragment,
            isStabilized = isStabilized,
            onFocus = {
                onExpandedChange(true)
            },
            onClick = {
                onExpandedChange(false)
                onMenuSelected("tv")
            }
        )

        SidebarItem(
            icon = R.drawable.ic_menu_search,
            text = "جستجو",
            isExpanded = isExpanded,
            isSelected = selectedItem === searchFR,
            focusRequester = searchFR,
            isDefaultItem = currentId == R.id.searchFragment,
            isStabilized = isStabilized,
            onFocus = {
                onExpandedChange(true)
            },
            onClick = {
                onExpandedChange(false)
                onMenuSelected("search")
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        SidebarItem(
            icon = R.drawable.ic_menu_settings,
            text = "تنظیمات",
            isExpanded = isExpanded,
            isSelected = selectedItem === settingsFR,
            focusRequester = settingsFR,
            isDefaultItem = currentId == R.id.settingsFragment,
            isStabilized = isStabilized,
            onFocus = {
                onExpandedChange(true)
            },
            onClick = {
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
    isStabilized: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }

    val effectiveFocused = isFocused && isExpanded

    val shouldShowSelected = !isExpanded && isSelected

    val backgroundColor = if (effectiveFocused) Color.White else Color.Transparent

    val contentColor = when {
        effectiveFocused -> Color.Black
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
            .semantics { selected = if (isExpanded) isFocused else isSelected }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onClick() }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (isExpanded) {
                Text(
                    text = text,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 32.dp),
                    color = contentColor,
                    fontSize = 13.sp,
                    fontFamily = VazirBold,
                    maxLines = 1, softWrap = false
                )
            }

            Icon(
                painter = painterResource(id = icon),
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier.align(Alignment.CenterEnd).size(20.dp)
            )
        }
    }
}
