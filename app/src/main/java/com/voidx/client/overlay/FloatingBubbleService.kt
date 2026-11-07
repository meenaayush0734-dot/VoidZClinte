package com.voidx.client.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.voidx.client.MainActivity
import com.voidx.client.VoidXApp
import com.voidx.client.domain.model.ThemeMode
import com.voidx.client.ui.components.VoidXLogo
import com.voidx.client.ui.theme.VoidSurface
import com.voidx.client.ui.theme.VoidViolet
import com.voidx.client.ui.theme.VoidXTheme
import kotlin.math.roundToInt

/**
 * A VoidX-only floating bubble drawn over other apps. Its menu only opens VoidX
 * or launches Minecraft through the normal launch intent. It does not modify the game.
 */
class FloatingBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow 'Display over other apps' for VoidX first.", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt("x", 0)
            y = prefs.getInt("y", 300)
        }
        layoutParams = params

        val owner = OverlayLifecycleOwner()
        owner.start()
        lifecycleOwner = owner

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setContent {
                BubbleContent(
                    onDrag = { dx, dy -> moveBy(dx, dy) },
                    onDragEnd = { savePosition() },
                    onOpenVoidX = { openVoidX() },
                    onLaunchMinecraft = { launchMinecraft() },
                    onClose = { stopSelf() },
                )
            }
        }
        composeView = view
        wm.addView(view, params)
        owner.resume()
    }

    override fun onDestroy() {
        composeView?.let { view -> runCatching { windowManager?.removeView(view) } }
        lifecycleOwner?.destroy()
        composeView = null
        lifecycleOwner = null
        super.onDestroy()
    }

    private fun moveBy(dx: Float, dy: Float) {
        val view = composeView ?: return
        val params = layoutParams ?: return
        params.x += dx.roundToInt()
        params.y += dy.roundToInt()
        windowManager?.updateViewLayout(view, params)
    }

    private fun savePosition() {
        val params = layoutParams ?: return
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit()
            .putInt("x", params.x)
            .putInt("y", params.y)
            .apply()
    }

    private fun openVoidX() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    }

    private fun launchMinecraft() {
        val container = (application as VoidXApp).container
        val intent = container.minecraft.launchIntent()
        if (intent == null) {
            Toast.makeText(this, "Minecraft Bedrock is not installed.", Toast.LENGTH_LONG).show()
            return
        }
        startActivity(intent)
    }

    companion object {
        private const val PREFS = "voidx_overlay"
    }
}

@Composable
private fun BubbleContent(
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onOpenVoidX: () -> Unit,
    onLaunchMinecraft: () -> Unit,
    onClose: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    VoidXTheme(mode = ThemeMode.DARK) {
        Column(horizontalAlignment = Alignment.Start) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(VoidSurface)
                    .pointerInput(Unit) {
                        detectDragGestures(onDragEnd = { onDragEnd() }) { change, drag ->
                            change.consume()
                            onDrag(drag.x, drag.y)
                        }
                    }
                    .clickable { expanded = !expanded },
                contentAlignment = Alignment.Center,
            ) {
                VoidXLogo(size = 40.dp)
            }

            if (expanded) {
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(VoidSurface)
                        .padding(10.dp)
                        .width(180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MenuButton("Launch Minecraft", onLaunchMinecraft)
                    MenuButton("Open VoidX", onOpenVoidX)
                    MenuButton("Hide menu") { expanded = false }
                    MenuButton("Close bubble", onClose)
                }
            }
        }
    }
}

@Composable
private fun MenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = VoidViolet),
    ) {
        Text(text)
    }
}

/** Gives the overlay ComposeView the lifecycle and saved state it needs outside an Activity. */
private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    init {
        savedState.performAttach()
        savedState.performRestore(null)
    }

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun start() { registry.currentState = Lifecycle.State.CREATED }
    fun resume() { registry.currentState = Lifecycle.State.RESUMED }
    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }
}
