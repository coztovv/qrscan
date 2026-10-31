package com.abusegpt.qrscan

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.abusegpt.qrscan.ui.CreateScreen
import com.abusegpt.qrscan.ui.HomeScreen
import com.abusegpt.qrscan.ui.ResultScreen
import com.abusegpt.qrscan.ui.ScanScreen
import com.abusegpt.qrscan.ui.theme.QRScanTheme
import com.abusegpt.qrscan.util.BarcodeDecoder
import com.abusegpt.qrscan.util.HistoryItem
import com.abusegpt.qrscan.util.HistoryStore

sealed interface Screen {
    data object Home : Screen
    data object Scan : Screen
    data object Create : Screen
    data class Result(val value: String) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QRScanTheme {
                QRScanApp()
            }
        }
    }
}

@Composable
fun QRScanApp() {
    val context = LocalContext.current
    val store = remember { HistoryStore(context) }
    var history by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var sharedImage by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        history = store.all()
        val activity = context as? ComponentActivity
        val intent = activity?.intent
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            @Suppress("DEPRECATION")
            val stream = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
            }
            if (stream != null) sharedImage = stream
        }
    }

    LaunchedEffect(sharedImage) {
        val uri = sharedImage ?: return@LaunchedEffect
        BarcodeDecoder.decode(context, uri) { values ->
            val value = values.firstOrNull()
            if (!value.isNullOrBlank()) {
                history = store.add(value)
                screen = Screen.Result(value)
            }
        }
        sharedImage = null
    }

    val openResult: (String) -> Unit = { value ->
        history = store.add(value)
        screen = Screen.Result(value)
    }

    BackHandler(enabled = screen !is Screen.Home) {
        screen = Screen.Home
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            (
                fadeIn(tween(260)) +
                    slideInHorizontally(tween(300)) { it / 14 } +
                    scaleIn(tween(300), initialScale = 0.97f)
                ) togetherWith fadeOut(tween(160))
        },
        label = "screens"
    ) { target ->
        when (target) {
            Screen.Home -> HomeScreen(
                history = history,
                onScan = { screen = Screen.Scan },
                onCreate = { screen = Screen.Create },
                onOpen = openResult,
                onClear = { history = store.clear() }
            )

            Screen.Scan -> ScanScreen(
                onBack = { screen = Screen.Home },
                onDetected = openResult
            )

            Screen.Create -> CreateScreen(
                onBack = { screen = Screen.Home }
            )

            is Screen.Result -> ResultScreen(
                value = target.value,
                onBack = { screen = Screen.Home },
                onScanAgain = { screen = Screen.Scan }
            )
        }
    }
}
