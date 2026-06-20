package com.example.kelkin

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
fun Sidebar(navController: NavController, onMenuSelected: (String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }

    val homeFR = remember { FocusRequester() }
    val movieFR = remember { FocusRequester() }
    val seriesFR = remember { FocusRequester() }
    val tvFR = remember { FocusRequester() }
    val searchFR = remember { FocusRequester() }
    val settingsFR = remember { FocusRequester() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentId = navBackStackEntry?.destination?.id

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            when (currentId) {
                R.id.homeFragment -> homeFR.requestFocus()
                R.id.moviesFragment -> movieFR.requestFocus()
                R.id.tvFragment -> tvFR.requestFocus()
                R.id.searchFragment -> searchFR.requestFocus()
                R.id.settingsFragment -> settingsFR.requestFocus()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(if (isExpanded) 140.dp else 60.dp)
            .padding(top = 40.dp, start = 8.dp, end = 8.dp)
            .animateContentSize(animationSpec = tween(durationMillis = 200))
            .onFocusChanged { if (!it.hasFocus) isExpanded = false },
        horizontalAlignment = Alignment.End
    ) {
        SidebarItem(R.drawable.ic_menu_home, "خانه", isExpanded, currentId == R.id.homeFragment, homeFR, { isExpanded = true }) { onMenuSelected("home") }
        SidebarItem(R.drawable.ic_menu_movie, "فیلم ها", isExpanded, currentId == R.id.moviesFragment, movieFR, { isExpanded = true }) { onMenuSelected("movies") }
        SidebarItem(R.drawable.ic_series, "سریال ها", isExpanded, false, seriesFR, { isExpanded = true }) { onMenuSelected("series") }
        SidebarItem(R.drawable.ic_menu_tv, "تلویزیون", isExpanded, currentId == R.id.tvFragment, tvFR, { isExpanded = true }) { onMenuSelected("tv") }
        SidebarItem(R.drawable.ic_menu_search, "جستجو", isExpanded, currentId == R.id.searchFragment, searchFR, { isExpanded = true }) { onMenuSelected("search") }

        Spacer(modifier = Modifier.weight(1f))

        SidebarItem(R.drawable.ic_menu_settings, "تنظیمات", isExpanded, currentId == R.id.settingsFragment, settingsFR, { isExpanded = true }) { onMenuSelected("settings") }
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
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        val activeColor = if (isFocused || isSelected) Color(0xFFFF9800) else Color.White

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {

            if (isExpanded) {
                Text(
                    text = text,
                    color = activeColor,
                    fontSize = 13.sp,
                    fontFamily = VazirBold
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = activeColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}