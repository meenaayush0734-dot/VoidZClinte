package com.voidx.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voidx.client.ui.AppViewModel
import com.voidx.client.ui.VoidXNavHost
import com.voidx.client.ui.theme.VoidXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { VoidXRoot() }
    }
}

@Composable
private fun VoidXRoot() {
    val vm: AppViewModel = viewModel()
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    VoidXTheme(mode = prefs.themeMode) {
        VoidXNavHost(vm)
    }
}
