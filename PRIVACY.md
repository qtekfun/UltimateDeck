<!--
SPDX-FileCopyrightText: 2026 UltimateDeck contributors
SPDX-License-Identifier: GPL-3.0-or-later
-->

# Privacy policy

*Español más abajo.*

UltimateDeck is a client for your own Nextcloud Deck. It has no servers of its own, no accounts of its own, no ads, no analytics and no telemetry. Nobody but you and your Nextcloud server sees your data.

## What data goes where

- **Your data only travels between your device and the Nextcloud server you sign in to**, always over HTTPS. Certificate validation is never disabled; certificates of your own CA installed on the device are accepted.
- **On the device** the app keeps a copy of your boards, columns, cards, labels, members and attachments you opened or added, so it works offline. It is deleted when you log out or uninstall the app.
- **Your app password** (created by Nextcloud's Login Flow v2, never your real password) is encrypted with a key stored in the Android Keystore. It never appears in logs or backups.
- **Backups are disabled**, so none of this is copied by Android's cloud backup.
- **Settings** (theme, language, favorite board, reminders) stay on the device.
- **Backups you export** are files you choose where to keep. They only include your signed-in sessions if you ask; then the app passwords inside are encrypted with a password you choose (AES-256-GCM, key derived with PBKDF2). Anyone with the file *and* that password could sign in as you, so keep both safe.

## Permissions and why

| Permission | Why |
|---|---|
| Internet (`INTERNET`) | To talk to your Nextcloud server. |
| Notifications (`POST_NOTIFICATIONS`) | Due date reminders. Only asked when you turn reminders on (Android 13+). |
| Exact alarms (`SCHEDULE_EXACT_ALARM`) | So reminders arrive at the time you set, not minutes later. Without it they still work, with some delay. |
| Alarm clock mode (optional setting) | Some phones delay even exact alarms to save battery. In the "aggressive" reminder mode they are set like an alarm clock, which no battery saver delays; Android then shows the alarm icon while one is pending. |
| Ignore battery optimizations (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) | Only on request, from Settings → Reminders: it opens the system dialog that lets the app run in the background so reminders are not blocked. Google Play limits which apps may ask this; UltimateDeck is distributed outside Play, and reminders are exactly the use case it exists for. You decide. |
| Run at startup (`RECEIVE_BOOT_COMPLETED`) | Alarms are lost when the phone restarts or the app is updated; this lets the app schedule your reminders again (and start robust mode if you turned it on). It does nothing else. |
| Foreground service (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`) | Only for robust mode, an optional setting that is off by default. Some phones (ColorOS, MIUI, OriginOS…) stop apps in the background and lose their alarms; robust mode keeps the app running with a small permanent notification, which explains what it is for and lets you turn it off. It never uses the network. |
| Camera, photos and files | **No permission is requested.** The app asks the system camera to take a photo, or the system picker for a photo or file you choose; it only receives what you pick. |

Attachments you open are shared with the app you choose to view them through a temporary, read-only link (Android's `FileProvider`).

## Contact

Questions or concerns: open an issue in the project repository.

---

# Política de privacidad

UltimateDeck es un cliente para tu propio Nextcloud Deck. No tiene servidores propios, ni cuentas propias, ni anuncios, ni analíticas, ni telemetría. Nadie más que tú y tu servidor Nextcloud ve tus datos.

## Qué datos van adónde

- **Tus datos solo viajan entre tu dispositivo y el servidor Nextcloud en el que inicias sesión**, siempre por HTTPS. Nunca se desactiva la validación de certificados; se aceptan los de tu propia CA instalados en el dispositivo.
- **En el dispositivo** la app guarda una copia de tus tableros, columnas, tarjetas, etiquetas, miembros y de los adjuntos que abres o añades, para funcionar sin conexión. Se borra al cerrar sesión o desinstalar la app.
- **Tu contraseña de aplicación** (la crea el Login Flow v2 de Nextcloud; nunca es tu contraseña real) se cifra con una clave guardada en el Android Keystore. Nunca aparece en registros ni copias de seguridad.
- **Las copias de seguridad están desactivadas**: nada de esto lo copia la copia en la nube de Android.
- **Los ajustes** (tema, idioma, tablero favorito, recordatorios) se quedan en el dispositivo.
- **Las copias que exportas** son archivos que guardas donde quieras. Solo incluyen tus sesiones iniciadas si lo pides; entonces las contraseñas de aplicación van cifradas con una contraseña que eliges (AES-256-GCM, clave derivada con PBKDF2). Quien tenga el archivo *y* esa contraseña podría entrar como tú: guarda bien ambos.

## Permisos y por qué

| Permiso | Por qué |
|---|---|
| Internet (`INTERNET`) | Para hablar con tu servidor Nextcloud. |
| Notificaciones (`POST_NOTIFICATIONS`) | Avisos de vencimiento. Solo se pide al activarlos (Android 13+). |
| Alarmas exactas (`SCHEDULE_EXACT_ALARM`) | Para que los avisos lleguen a la hora que pusiste y no minutos después. Sin él funcionan igual, con algo de retraso. |
| Modo despertador (ajuste opcional) | Algunos móviles retrasan incluso las alarmas exactas para ahorrar batería. En el modo «agresivo» se programan como un despertador, que ningún ahorro de batería retrasa; Android muestra entonces el icono de alarma mientras haya uno pendiente. |
| Ignorar la optimización de batería (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) | Solo si lo pides, desde Ajustes → Recordatorios: abre la ventana del sistema para dejar que la app funcione en segundo plano y no se bloqueen los avisos. Google Play limita qué apps pueden pedirlo; UltimateDeck se distribuye fuera de Play y los recordatorios son justo el caso para el que existe. Decides tú. |
| Inicio con el sistema (`RECEIVE_BOOT_COMPLETED`) | Las alarmas se pierden al reiniciar el móvil o actualizar la app; esto permite volver a programar tus avisos (y arrancar el modo robusto si lo activaste). No hace nada más. |
| Servicio en primer plano (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`) | Solo para el modo robusto, un ajuste opcional desactivado por defecto. Algunos móviles (ColorOS, MIUI, OriginOS…) detienen las apps en segundo plano y pierden sus alarmas; el modo robusto mantiene la app en marcha con una pequeña notificación fija, que explica para qué sirve y permite desactivarlo. Nunca usa la red. |
| Cámara, fotos y archivos | **No se pide ningún permiso.** La app pide a la cámara del sistema que haga una foto, o al selector del sistema una foto o archivo que tú eliges; solo recibe lo que eliges. |

Los adjuntos que abres se comparten con la app que elijas para verlos mediante un enlace temporal de solo lectura (el `FileProvider` de Android).

## Contacto

Dudas o problemas: abre una incidencia en el repositorio del proyecto.
