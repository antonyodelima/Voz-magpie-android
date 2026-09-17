package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.theme.VozoDarkBg
import com.example.theme.VozoMagpieTheme
import com.example.ui.VozoStudioScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VozoMagpieTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VozoDarkBg
                ) {
                    VozoStudioScreen()
                }
            }
        }
    }
}
