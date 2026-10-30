package com.keval.fieldpintask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.keval.fieldpintask.ui.AppRoot
import com.keval.fieldpintask.ui.theme.FieldPinTaskTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FieldPinTaskTheme {
                AppRoot()
            }
        }
    }
}
