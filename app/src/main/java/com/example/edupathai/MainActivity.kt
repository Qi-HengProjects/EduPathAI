package com.example.edupathai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.edupathai.ui.notes.NotesDirectoryScreen
import com.example.edupathai.ui.theme.EduPathAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EduPathAITheme {
                NotesDirectoryScreen()
            }
        }
    }
}