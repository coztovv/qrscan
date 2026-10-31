package com.abusegpt.qrscan.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

/**
 * Открывает содержимое QR-кода напрямую в приложении, которое его обрабатывает
 * (Telegram, WhatsApp, YouTube, Spotify...), а не в браузере.
 */
object LinkLauncher {

    private val browserPackages = setOf(
        "com.android.chrome",
        "com.android.chrome.beta",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
        "org.chromium.chrome",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "org.mozilla.focus",
        "com.microsoft.emmx",
        "com.microsoft.edgemobile",
        "com.sec.android.app.sbrowser",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.brave.browser",
        "com.duckduckgo.mobile.android",
        "com.vivaldi.browser",
        "com.yandex.browser",
        "com.android.browser",
        "com.google.android.apps.search.googleapp.customtabs",
        "com.google.android.googlequicksearchbox"
    )

    /** Явные deep-link схемы для популярных сервисов. */
    private fun deepCandidates(uri: Uri): List<Intent> {
        val out = mutableListOf<Intent>()
        val scheme = uri.scheme?.lowercase() ?: return out
        val host = uri.host?.lowercase().orEmpty().removePrefix("www.")
        val path = uri.path.orEmpty()

        if (scheme == "http" || scheme == "https") {
            when {
                host == "t.me" || host == "telegram.me" -> {
                    val parts = path.trim('/').split('/')
                    when {
                        parts.firstOrNull() == "joinchat" && parts.size >= 2 ->
                            out += Intent(Intent.ACTION_VIEW, Uri.parse("tg://join?invite=${parts[1]}"))
                        parts.firstOrNull() == "+" ->
                            out += Intent(Intent.ACTION_VIEW, Uri.parse("tg://join?invite=${path.trim('/').removePrefix("+")}"))
                        parts.firstOrNull() == "s" ->
                            out += Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=${parts.getOrNull(1)}&post=${parts.getOrNull(2)}"))
                        parts.firstOrNull() == "addstickers" ->
                            out += Intent(Intent.ACTION_VIEW, Uri.parse("tg://addstickers?set=${parts.getOrNull(1)}"))
                        parts.firstOrNull() == "share" && parts.size >= 2 ->
                            out += Intent(Intent.ACTION_VIEW, Uri.parse("tg://msg_url?url=${Uri.encode("https://t.me/$path")}"))
                        parts.firstOrNull()?.isNotBlank() == true ->
                            out += Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=${parts[0]}"))
                    }
                }
                host == "wa.me" || host == "api.whatsapp.com" -> {
                    val phone = path.trim('/')
                    if (phone.isNotBlank()) {
                        out += Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://send?phone=${phone.filter { it.isDigit() }}"))
                    }
                }
                host == "chat.whatsapp.com" -> {
                    val code = path.trim('/')
                    if (code.isNotBlank()) {
                        out += Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://chat?code=$code"))
                    }
                }
                host == "instagram.com" || host == "instagr.am" -> {
                    val user = path.trim('/').substringBefore('/')
                    if (user.isNotBlank()) {
                        out += Intent(Intent.ACTION_VIEW, Uri.parse("instagram://user?username=$user"))
                    }
                }
                host == "youtube.com" || host == "m.youtube.com" || host == "youtu.be" -> {
                    val id = if (host == "youtu.be") path.trim('/') else uri.getQueryParameter("v")
                    if (!id.isNullOrBlank()) {
                        out += Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$id"))
                    }
                }
                host == "open.spotify.com" -> {
                    val parts = path.trim('/').split('/')
                    if (parts.size >= 2) {
                        out += Intent(Intent.ACTION_VIEW, Uri.parse("spotify:${parts[0]}:${parts[1]}"))
                    }
                }
                host == "twitter.com" || host == "x.com" -> {
                    val user = path.trim('/').substringBefore('/')
                    if (user.isNotBlank()) {
                        out += Intent(Intent.ACTION_VIEW, Uri.parse("twitter://user?screen_name=$user"))
                        out += Intent(Intent.ACTION_VIEW, Uri.parse("snssdk1233://user/profile/$user"))
                    }
                }
                host == "vk.com" || host == "m.vk.com" -> {
                    val user = path.trim('/').substringBefore('/')
                    if (user.isNotBlank()) {
                        out += Intent(Intent.ACTION_VIEW, Uri.parse("vk://vk.com/$user"))
                    }
                }
                host == "github.com" -> {
                    val user = path.trim('/').substringBefore('/')
                    if (user.isNotBlank()) {
                        out += Intent(Intent.ACTION_VIEW, Uri.parse("github://$user"))
                    }
                }
                host == "discord.com" || host == "discord.gg" -> {
                    out += Intent(Intent.ACTION_VIEW, Uri.parse("discord://"))
                }
                host == "yandex.ru" || host == "ya.ru" -> {
                    out += Intent(Intent.ACTION_VIEW, Uri.parse("yandexmaps://"))
                }
            }
        }

        return out
    }

    private fun queryActivities(context: Context, intent: Intent): List<android.content.pm.ResolveInfo> =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
        }.getOrElse { emptyList() }

    private fun fire(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Пытается открыть значение в приложении-обработчике.
     * Возвращает true, если удалось запустить что-то кроме браузера или намерение обработано.
     */
    fun open(context: Context, raw: String): Boolean {
        val text = raw.trim()
        if (text.isEmpty()) return false

        val uri = runCatching { Uri.parse(text) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false

        // 1. Явные схемы приложений (tg://, whatsapp://, spotify:...)
        for (candidate in deepCandidates(uri)) {
            if (queryActivities(context, candidate).isNotEmpty() && fire(context, candidate)) return true
        }

        // 2. Не-http схемы (mailto:, tel:, sms:, geo:, ftp:) — открываем как есть
        if (scheme != "http" && scheme != "https") {
            return fire(context, Intent(Intent.ACTION_VIEW, uri))
        }

        val webIntent = Intent(Intent.ACTION_VIEW, uri)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // 3. Ищем не-браузерное приложение, зарегистрированное на этот домен (App Links)
        val handlers = queryActivities(context, webIntent)
        val appHandler = handlers.firstOrNull {
            val pkg = it.activityInfo.packageName
            pkg !in browserPackages && !looksLikeBrowser(pkg)
        }
        if (appHandler != null) {
            val explicit = Intent(webIntent).setPackage(appHandler.activityInfo.packageName)
            if (fire(context, explicit)) return true
        }

        // 4. Иначе — браузер
        val browser = handlers.firstOrNull { it.activityInfo.packageName in browserPackages }
            ?: handlers.firstOrNull()
        if (browser != null) {
            val explicit = Intent(webIntent).setPackage(browser.activityInfo.packageName)
            if (fire(context, explicit)) return true
        }

        return fire(context, webIntent)
    }

    private fun looksLikeBrowser(pkg: String): Boolean {
        if (pkg.contains("browser", true)) return true
        if (pkg.contains("chrome", true) && !pkg.contains("chromeos", true)) return true
        return false
    }

    /** Проверяет, есть ли вообще обработчик (для подсветки кнопки). */
    fun canOpen(context: Context, raw: String): Boolean {
        val uri = runCatching { Uri.parse(raw.trim()) }.getOrNull() ?: return false
        if (uri.scheme.isNullOrBlank()) return false
        val intent = Intent(Intent.ACTION_VIEW, uri)
        return queryActivities(context, intent).isNotEmpty() ||
            deepCandidates(uri).any { queryActivities(context, it).isNotEmpty() }
    }

    fun prefersApp(context: Context, raw: String): Boolean {
        val uri = runCatching { Uri.parse(raw.trim()) }.getOrNull() ?: return false
        if (uri.scheme != "http" && uri.scheme != "https") return true
        if (deepCandidates(uri).any { queryActivities(context, it).isNotEmpty() }) return true
        val handlers = queryActivities(context, Intent(Intent.ACTION_VIEW, uri))
        return handlers.any {
            val pkg = it.activityInfo.packageName
            pkg !in browserPackages && !looksLikeBrowser(pkg)
        }
    }

    fun packageNameFor(context: Context, raw: String): String? {
        val uri = runCatching { Uri.parse(raw.trim()) }.getOrNull() ?: return null
        if (uri.scheme.isNullOrBlank()) return null
        deepCandidates(uri).forEach { candidate ->
            val info = queryActivities(context, candidate).firstOrNull()
            if (info != null) return info.activityInfo.packageName
        }
        if (uri.scheme == "http" || uri.scheme == "https") {
            return queryActivities(context, Intent(Intent.ACTION_VIEW, uri))
                .firstOrNull { it.activityInfo.packageName !in browserPackages && !looksLikeBrowser(it.activityInfo.packageName) }
                ?.activityInfo?.packageName
        }
        return null
    }
}
