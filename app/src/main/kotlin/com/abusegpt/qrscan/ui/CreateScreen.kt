package com.abusegpt.qrscan.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abusegpt.qrscan.util.QrUtils
import com.abusegpt.qrscan.util.ShareUtils
import kotlinx.coroutines.delay

private enum class CreateMode(val label: String) {
    TEXT("Текст"),
    LINK("Ссылка"),
    WIFI("Wi-Fi")
}

@Composable
fun CreateScreen(onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    var mode by remember { mutableStateOf(CreateMode.TEXT) }
    var text by remember { mutableStateOf("") }
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }

    val payload = remember(mode, text, ssid, password) {
        when (mode) {
            CreateMode.TEXT, CreateMode.LINK -> text.trim()
            CreateMode.WIFI -> if (ssid.isBlank()) "" else QrUtils.wifiPayload(ssid, password)
        }
    }

    val bitmap = remember(payload) {
        if (payload.isEmpty()) null else QrUtils.encode(payload, 960)
    }

    LaunchedEffect(notice) {
        if (notice != null) {
            delay(1800)
            notice = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "назад",
                        tint = scheme.onSurface
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "создание",
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onSurface
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CreateMode.entries.forEach { item ->
                    ModePill(
                        label = item.label,
                        selected = mode == item,
                        onClick = { mode = item }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            when (mode) {
                CreateMode.TEXT, CreateMode.LINK -> {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        label = {
                            Text(if (mode == CreateMode.LINK) "ссылка" else "текст")
                        },
                        placeholder = {
                            Text(
                                if (mode == CreateMode.LINK) "https://t.me/…" else "что угодно",
                                color = scheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        minLines = 1,
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (mode == CreateMode.LINK) KeyboardType.Uri else KeyboardType.Text
                        )
                    )
                }

                CreateMode.WIFI -> {
                    OutlinedTextField(
                        value = ssid,
                        onValueChange = { ssid = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        label = { Text("название сети") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        label = { Text("пароль") },
                        singleLine = true
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Fit,
                            filterQuality = FilterQuality.None
                        )
                    } else {
                        Text(
                            text = if (mode == CreateMode.WIFI) "введите название сети" else "здесь появится код",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9A9AA5),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (bitmap != null) {
                            val ok = ShareUtils.saveToGallery(context, bitmap)
                            notice = if (ok) "сохранено в галерею" else "не удалось сохранить"
                        }
                    },
                    enabled = bitmap != null,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Сохранить")
                }

                OutlinedButton(
                    onClick = {
                        if (bitmap != null) ShareUtils.shareBitmap(context, bitmap)
                    },
                    enabled = bitmap != null,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Поделиться")
                }
            }
        }

        AnimatedVisibility(
            visible = notice != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = scheme.inverseSurface,
                contentColor = scheme.inverseOnSurface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(notice.orEmpty(), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ModePill(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) scheme.primary else scheme.surfaceContainerHigh,
        contentColor = if (selected) scheme.onPrimary else scheme.onSurfaceVariant
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}
