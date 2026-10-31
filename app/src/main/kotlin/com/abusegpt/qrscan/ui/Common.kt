package com.abusegpt.qrscan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abusegpt.qrscan.util.QrKind
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun QrKind.icon(): ImageVector = when (this) {
    QrKind.TELEGRAM -> Icons.Rounded.Send
    QrKind.WHATSAPP -> Icons.Rounded.Chat
    QrKind.YOUTUBE -> Icons.Rounded.PlayCircle
    QrKind.SPOTIFY -> Icons.Rounded.MusicNote
    QrKind.SOCIAL -> Icons.Rounded.Language
    QrKind.LINK -> Icons.Rounded.Link
    QrKind.WIFI -> Icons.Rounded.Wifi
    QrKind.MAIL -> Icons.Rounded.MailOutline
    QrKind.PHONE -> Icons.Rounded.Call
    QrKind.SMS -> Icons.Rounded.Sms
    QrKind.GEO -> Icons.Rounded.Place
    QrKind.CONTACT -> Icons.Rounded.Person
    QrKind.TEXT -> Icons.Rounded.Notes
}

@Composable
fun KindBadge(
    kind: QrKind,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = kind.icon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    val yesterday = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR) == 1

    return when {
        sameDay -> "сегодня, " + SimpleDateFormat("HH:mm", Locale("ru")).format(Date(timestamp))
        yesterday -> "вчера, " + SimpleDateFormat("HH:mm", Locale("ru")).format(Date(timestamp))
        else -> SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(timestamp))
    }
}

fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

fun shortValue(value: String, max: Int = 64): String {
    val single = value.replace("\n", " ").trim()
    return if (single.length <= max) single else single.take(max - 1).trimEnd() + "…"
}
