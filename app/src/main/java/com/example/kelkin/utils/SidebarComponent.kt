package com.example.kelkin

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    val isInMovieDetail = currentId == R.id.movieDetailFragment

    // Determine which item should be selected
    val selectedItem = remember(currentId) {
        when (currentId) {
            R.id.homeFragment, R.id.movieDetailFragment -> homeFR
            R.id.moviesFragment -> movieFR
            R.id.tvFragment -> tvFR
            R.id.searchFragment -> searchFR
            R.id.settingsFragment -> settingsFR
            else -> homeFR
        }
    }

    // Request focus when expanded - BUT NOT in MovieDetailFragment
    LaunchedEffect(isExpanded, currentId) {
        // CRITICAL: Only request focus if we're NOT in MovieDetailFragment
        // OR if we're in MovieDetailFragment and user manually expanded it
        if (isExpanded && !isInMovieDetail) {
            focusManager.clearFocus(force = true)
            selectedItem.requestFocus()
        }
    }

    // Collapse sidebar when navigating TO MovieDetailFragment
    LaunchedEffect(currentId) {
        if (isInMovieDetail) {
            // Immediately collapse when entering MovieDetailFragment
            onExpandedChange(false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(if (isExpanded) 140.dp else 60.dp)
            .padding(top = 40.dp, start = 8.dp, end = 8.dp)
            .animateContentSize(animationSpec = tween(durationMillis = 200))
            .onFocusChanged {
                if (!it.hasFocus && isExpanded) {
                    onExpandedChange(false)
                }
            },
        horizontalAlignment = Alignment.End
    ) {
        // Home
        SidebarItem(
            icon = R.drawable.ic_menu_home,
            text = "خانه",
            isExpanded = isExpanded,
            isSelected = currentId == R.id.homeFragment || currentId == R.id.movieDetailFragment,
            focusRequester = homeFR,
            isDefaultItem = currentId == R.id.homeFragment || currentId == R.id.movieDetailFragment,
            isInMovieDetail = isInMovieDetail,
            onFocus = {
                onExpandedChange(true)
            },
            onClick = {
                onExpandedChange(false)
                onMenuSelected("home")
            }
        )

        // Movies
        SidebarItem(
            icon = R.drawable.ic_menu_movie,
            text = "فیلم ها",
            isExpanded = isExpanded,
            isSelected = currentId == R.id.moviesFragment,
            focusRequester = movieFR,
            isDefaultItem = currentId == R.id.moviesFragment,
            isInMovieDetail = isInMovieDetail,
            onFocus = {
                onExpandedChange(true)
            },
            onClick = {
                onExpandedChange(false)
                onMenuSelected("movies")
            }
        )

        // Series
        SidebarItem(
            icon = R.drawable.ic_series,
            text = "سریال ها",
            isExpanded = isExpanded,
            isSelected = false,
            focusRequester = seriesFR,
            isDefaultItem = false,
            isInMovieDetail = isInMovieDetail,
            onFocus = {
                onExpandedChange(true)
            },
            onClick = {
                onExpandedChange(false)
                onMenuSelected("series")
            }
        )

        // TV
        SidebarItem(
            icon = R.drawable.ic_menu_tv,
            text = "تلویزیون",
            isExpanded = isExpanded,
            isSelected = currentId == R.id.tvFragment,
            focusRequester = tvFR,
            isDefaultItem = currentId == R.id.tvFragment,
            isInMovieDetail = isInMovieDetail,
            onFocus = {
                onExpandedChange(true)
            },
            onClick = {
                onExpandedChange(false)
                onMenuSelected("tv")
            }
        )

        // Search
        SidebarItem(
            icon = R.drawable.ic_menu_search,
            text = "جستجو",
            isExpanded = isExpanded,
            isSelected = currentId == R.id.searchFragment,
            focusRequester = searchFR,
            isDefaultItem = currentId == R.id.searchFragment,
            isInMovieDetail = isInMovieDetail,
            onFocus = {
                onExpandedChange(true)
            },
            onClick = {
                onExpandedChange(false)
                onMenuSelected("search")
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Settings
        SidebarItem(
            icon = R.drawable.ic_menu_settings,
            text = "تنظیمات",
            isExpanded = isExpanded,
            isSelected = currentId == R.id.settingsFragment,
            focusRequester = settingsFR,
            isDefaultItem = currentId == R.id.settingsFragment,
            isInMovieDetail = isInMovieDetail,
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
    isInMovieDetail: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    // Only show yellow border when actually focused
    val shouldShowBorder = isFocused

    // Show orange color if selected OR if it's the default item when not focused
    // BUT NOT in MovieDetailFragment (to prevent any visual flash)
    val shouldShowOrange = if (isInMovieDetail) {
        false  // Don't show any orange in MovieDetailFragment to prevent flash
    } else {
        isSelected || (isDefaultItem && !isFocused)
    }

    val iconColor = if (shouldShowBorder || shouldShowOrange) Color(0xFFFF9800) else Color.White
    val textColor = if (shouldShowOrange) Color(0xFFFF9800) else Color.White

    val borderModifier = if (shouldShowBorder) {
        Modifier.border(
            width = 1.5.dp,
            color = Color(0xFFFF9800),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        )
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .clickable { onClick() }
            .then(borderModifier)
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
                    color = textColor,
                    fontSize = 13.sp,
                    fontFamily = VazirBold
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}