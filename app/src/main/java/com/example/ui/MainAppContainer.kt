package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.theme.VozoBorder
import com.example.theme.VozoDarkBg
import com.example.theme.VozoDarkSurface
import com.example.theme.VozoPrimary
import com.example.theme.VozoTextMuted
import com.example.theme.VozoTextPrimary
import com.example.theme.VozoTextSecondary

enum class VozoScreen {
    STUDIO,
    DASHBOARD
}

/**
 * Top-level container that manages navigation and transitions between
 * the Voice Lab WebView studio screen and the Dashboard / Settings screen.
 */
@Composable
fun MainAppContainer(
    modifier: Modifier = Modifier,
    hasMicPermission: Boolean = true,
    onRequestAudioPermission: (((Boolean) -> Unit) -> Unit)? = null,
    viewModel: VozoStudioViewModel = viewModel()
) {
    var currentScreen by rememberSaveable { mutableStateOf(VozoScreen.STUDIO) }

    // Intercept back button when on Dashboard to smoothly return to Studio
    BackHandler(enabled = currentScreen == VozoScreen.DASHBOARD) {
        currentScreen = VozoScreen.STUDIO
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(VozoDarkBg)
            .testTag("main_app_container"),
        containerColor = VozoDarkBg,
        bottomBar = {
            NavigationBar(
                containerColor = VozoDarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = VozoBorder)
                    .navigationBarsPadding()
                    .testTag("main_bottom_nav")
            ) {
                NavigationBarItem(
                    selected = currentScreen == VozoScreen.STUDIO,
                    onClick = { currentScreen = VozoScreen.STUDIO },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Voice Lab Studio",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Voice Lab",
                            fontSize = 12.sp,
                            fontWeight = if (currentScreen == VozoScreen.STUDIO) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = VozoPrimary,
                        indicatorColor = VozoPrimary,
                        unselectedIconColor = VozoTextMuted,
                        unselectedTextColor = VozoTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_item_studio")
                )

                NavigationBarItem(
                    selected = currentScreen == VozoScreen.DASHBOARD,
                    onClick = { currentScreen = VozoScreen.DASHBOARD },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Dashboard & Configurações",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Dashboard",
                            fontSize = 12.sp,
                            fontWeight = if (currentScreen == VozoScreen.DASHBOARD) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = VozoPrimary,
                        indicatorColor = VozoPrimary,
                        unselectedIconColor = VozoTextMuted,
                        unselectedTextColor = VozoTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_item_dashboard")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    if (targetState == VozoScreen.DASHBOARD) {
                        (slideInHorizontally(animationSpec = tween(280)) { width -> width } + fadeIn(animationSpec = tween(280)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { width -> -width } + fadeOut(animationSpec = tween(280)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(280)) { width -> -width } + fadeIn(animationSpec = tween(280)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { width -> width } + fadeOut(animationSpec = tween(280)))
                    }
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    VozoScreen.STUDIO -> {
                        VozoStudioScreen(
                            hasMicPermission = hasMicPermission,
                            onRequestAudioPermission = onRequestAudioPermission,
                            onNavigateToDashboard = { currentScreen = VozoScreen.DASHBOARD },
                            viewModel = viewModel
                        )
                    }
                    VozoScreen.DASHBOARD -> {
                        VozoDashboardScreen(
                            onNavigateToStudio = { currentScreen = VozoScreen.STUDIO },
                            hasMicPermission = hasMicPermission,
                            onRequestAudioPermission = {
                                onRequestAudioPermission?.invoke { }
                            },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}
