package com.abusegpt.qrscan.util

import android.net.Uri

enum class QrKind(val label: String) {
    TELEGRAM("Telegram"),
    WHATSAPP("WhatsApp"),
    YOUTUBE("YouTube"),
    SPOTIFY("Spotify"),
    SOCIAL("Соцсеть"),
    LINK("Ссылка"),
    WIFI("Wi-Fi"),
    MAIL("Почта"),
    PHONE("Телефон"),
    SMS("SMS"),
    GEO("Место"),
    CONTACT("Контакт"),
    TEXT("Текст")
}

object QrClassifier {

    fun classify(value: String): QrKind {
        val text = value.trim()
        val lower = text.lowercase()

        if (lower.startsWith("wifi:")) return QrKind.WIFI
        if (lower.startsWith("begin:vcard")) return QrKind.CONTACT
        if (lower.startsWith("mailto:")) return QrKind.MAIL
        if (lower.startsWith("tel:")) return QrKind.PHONE
        if (lower.startsWith("smsto:") || lower.startsWith("sms:")) return QrKind.SMS
        if (lower.startsWith("geo:") || lower.startsWith("yandexmaps:")) return QrKind.GEO

        if (isEmail(text)) return QrKind.MAIL
        if (isPhone(text)) return QrKind.PHONE

        val uri = runCatching { Uri.parse(text) }.getOrNull()
        val host = uri?.host?.lowercase()?.removePrefix("www.").orEmpty()
        val scheme = uri?.scheme?.lowercase()

        if (scheme == "tg" || host == "t.me" || host == "telegram.me") return QrKind.TELEGRAM
        if (scheme == "whatsapp" || host == "wa.me" || host == "chat.whatsapp.com") return QrKind.WHATSAPP
        if (host == "youtube.com" || host == "youtu.be" || host == "m.youtube.com") return QrKind.YOUTUBE
        if (host == "open.spotify.com" || scheme == "spotify") return QrKind.SPOTIFY
        if (host in socialHosts) return QrKind.SOCIAL

        if (scheme == "http" || scheme == "https" || scheme == "ftp") return QrKind.LINK

        return QrKind.TEXT
    }

    private val socialHosts = setOf(
        "instagram.com", "instagr.am", "x.com", "twitter.com",
        "vk.com", "m.vk.com", "facebook.com", "tiktok.com",
        "github.com", "discord.gg", "discord.com", "threads.net"
    )

    private fun isEmail(text: String): Boolean =
        !text.contains(' ') && Regex("^[^@\\s]+@[^@\\s]+\\.[a-z]{2,}$", RegexOption.IGNORE_CASE).matches(text)

    private fun isPhone(text: String): Boolean {
        val compact = text.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
        return compact.startsWith("+") && compact.drop(1).all { it.isDigit() } && compact.length in 8..16
    }

    fun isOpenable(kind: QrKind): Boolean = kind != QrKind.TEXT && kind != QrKind.CONTACT && kind != QrKind.WIFI

    fun openLabel(kind: QrKind): String = when (kind) {
        QrKind.TELEGRAM -> "Открыть в Telegram"
        QrKind.WHATSAPP -> "Открыть в WhatsApp"
        QrKind.YOUTUBE -> "Смотреть на YouTube"
        QrKind.SPOTIFY -> "Слушать в Spotify"
        QrKind.LINK -> "Открыть ссылку"
        QrKind.MAIL -> "Написать письмо"
        QrKind.PHONE -> "Позвонить"
        QrKind.SMS -> "Отправить SMS"
        QrKind.GEO -> "Показать на карте"
        QrKind.SOCIAL -> "Открыть приложение"
        else -> "Открыть"
    }
}
