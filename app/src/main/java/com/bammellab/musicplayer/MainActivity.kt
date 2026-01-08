package com.bammellab.musicplayer

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.bammellab.musicplayer.ui.screens.MusicPlayerScreen
import com.bammellab.musicplayer.ui.theme.MusicPlayerTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicPlayerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MusicPlayerScreen()
                }
            }
        }
    }
}
