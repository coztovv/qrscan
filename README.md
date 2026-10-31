# qrscan

минималистичное приложение для сканирования и создания QR-кодов в стиле **Material 3 You**
(динамические цвета с обоев, Android 16, edge-to-edge, тёмная тема).

## что умеет

**сканирование**
- камера (CameraX) + ML Kit barcode — QR, Data Matrix, Aztec, PDF417, Code 128 и остальные форматы
- импорт кода из галереи (на случай, если код уже есть картинкой)
- фонарик, вибро-отклик, живая анимация сканирующей линии

**открытие напрямую в приложении**
ссылка из кода не улетает в браузер, а открывается в том приложении, которое её обрабатывает:
- `t.me`, `telegram.me` → `tg://resolve` / `tg://join` (Telegram)
- `wa.me`, `chat.whatsapp.com` → WhatsApp
- `instagram.com`, `x.com`, `vk.com`, `youtube.com`, `open.spotify.com`, `github.com`, `discord.gg`
- любой домен с App Links: выбирается не-браузерный обработчик, браузер — только если приложения нет
- `mailto:`, `tel:`, `sms:`, `geo:` — системные приложения

**создание**
- текст, ссылка, Wi-Fi (`WIFI:T:WPA;S:...;P:...;;`)
- 960 px, коррекция ошибок Q, живое превью
- сохранение в `Pictures/qrscan` и «поделиться» картинкой

**ещё**
- история последних 60 кодов с типом и временем (SharedPreferences)
- приём картинки через «поделиться» из галереи (`ACTION_SEND image/*`)
- предиктивный back, все строки на русском

## сборка

нужен JDK 17+ и Android SDK (compileSdk 36).

```bash
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
```

готовый APK лежит в `dist/qrscan-1.0.apk`.

## стек

Kotlin · Jetpack Compose (Material 3, dynamic color) · CameraX 1.5 · ML Kit barcode 17.3 · ZXing 3.5 (генерация)
minSdk 29 · targetSdk 36
