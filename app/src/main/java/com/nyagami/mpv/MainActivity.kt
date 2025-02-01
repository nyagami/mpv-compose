package com.nyagami.mpv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nyagami.mpv.ui.theme.MPVComposeTheme

class MainActivity : ComponentActivity(), MPV.Companion.EventObserver{
    private val videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    override fun onCreate(savedInstanceState: Bundle?) {
        MPV.prepare(this)
        MPV.addObserver(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LaunchedEffect(Unit) {
                MPV.command(arrayOf("loadfile", videoUrl))
            }
            MPVComposeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)){
                        MPVView(modifier = Modifier.fillMaxWidth().height(200.dp))
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        MPV.destroy()
        super.onDestroy()
    }

    override fun efEvent(err: String) {
        
    }

    override fun event(eventId: Int) {
        
    }

    override fun eventProperty(property: String) {
        
    }

    override fun eventProperty(property: String, value: Long) {
        
    }

    override fun eventProperty(property: String, value: Boolean) {
        
    }

    override fun eventProperty(property: String, value: String) {
        
    }

    override fun eventProperty(property: String, value: Double) {
        
    }
}