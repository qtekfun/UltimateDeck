# UltimateDeck — Especificación (SPEC)

Versión del documento: 0.1 · Estado: borrador para implementación del MVP

## 1. Objetivo

Cliente Android para **Nextcloud Deck** con una experiencia moderna al estilo de Trello/Jira móvil, que funcione **offline desde el primer día**. Debe ser útil para otras personas (no solo uso personal) y publicarse en F-Droid más adelante.

### Problemas que resuelve (apps actuales)
1. No se pueden deslizar y mover las tarjetas cómodamente.
2. Ver y editar tareas es lento, vago y anticuado.
3. Estéticamente poco amigable.

## 2. Alcance y supuestos

| Tema | Decisión |
|---|---|
| Backend | Nextcloud Deck vía API REST **v1.1** (`/index.php/apps/deck/api/v1.1/`) y OCS donde haga falta. v1.1 es v1.0 más adjuntos con tipo (`deck_file` / `file`) |
| Versión de referencia | Nextcloud 35. Versión mínima de Deck: **≥ 1.3** (primera con API v1.1), provisional hasta probar contra servidores reales |
| Plataforma | Android, `minSdk` 26; móvil en vertical primero, layouts adaptables preparados para tablet/apaisado después |
| Cuentas | Modelo de datos **indexado por cuenta** desde el inicio; la UI del MVP muestra una cuenta activa. Multicuenta completo en la UI, después |
| Volumen de referencia | ~1 tablero y ≤15 tarjetas en curso, pero el diseño debe escalar (índices por cuenta/tablero, paginación, listas perezosas) |
| Licencia | GPL-3.0-or-later |
| Idiomas | Inglés y español en el MVP; arquitectura preparada para más |
| Distribución | F-Droid (no en el MVP, pero todo se diseña para cumplir sus requisitos desde ya) |

## 3. Requisitos funcionales (MVP)

### RF-01 Autenticación
- Login mediante **Login Flow v2** de Nextcloud (contraseña de aplicación; nunca se almacena la contraseña real).
- Credenciales cifradas con Android Keystore.
- Cierre de sesión: elimina credenciales y datos locales de esa cuenta.
- **Criterios de aceptación:**
  - Dada una URL válida de servidor, el usuario completa el flujo en el navegador y vuelve a la app autenticado.
  - Dada una URL inválida o sin Deck instalado, se muestra un error claro y accionable.
  - Las credenciales no aparecen en logs, copias de seguridad ni preferencias en texto plano.

### RF-02 Lista de tableros
- Muestra tableros no archivados de la cuenta activa, con color y título.
- **Criterio:** funciona sin conexión mostrando lo último sincronizado.

### RF-03 Vista de tablero (estilo Jira móvil)
- Una columna ocupa casi toda la pantalla y **la siguiente asoma por el borde** (~columna y media).
- Swipe lateral con ajuste (snap) a la columna.
- Tarjeta visible de un vistazo: título, etiquetas de color, avatares de asignados, fecha de vencimiento, indicador de adjuntos y progreso de checklist.
  - La fecha de vencimiento se muestra **siempre con el año** (formato de fecha media del idioma, p. ej. "4 oct 2026" / "Oct 4, 2026"), también cuando es del año en curso.
- **Criterios:**
  - Desplazamiento fluido (sin saltos perceptibles) con el volumen de referencia.
  - El estado de columna y scroll se conserva al rotar o volver atrás.

### RF-04 Mover y reordenar tarjetas
- Arrastrar con **pulsación larga**, entre columnas y dentro de la misma columna.
- **Autoscroll horizontal** al acercar la tarjeta al borde de la pantalla.
- Sin menús intermedios para mover.
- **Criterios:**
  - El movimiento se refleja al instante en local y se encola para sincronizar.
  - Si falla la sincronización, el estado local se mantiene y se reintenta; nunca se pierde el movimiento.

### RF-05 Ver y editar tarjeta
- Detalle en pantalla completa o bottom sheet.
- **Edición en línea de título y descripción** (tocar y editar en el sitio, sin pantallas intermedias).
- **Descripción WYSIWYG** con almacenamiento en Markdown (formato nativo de Deck). El viaje de ida y vuelta Markdown → editor → Markdown **no debe corromper ni alterar** el contenido existente.
- Checklists interactivas dentro de la descripción.
- Panel secundario para fecha de vencimiento, etiquetas y asignados (menos inmediato).
- **Criterios:**
  - Editar título o descripción requiere como máximo un toque para entrar en edición y se guarda automáticamente.
  - Una descripción con Markdown no soportado por el editor se conserva intacta al guardar (no se pierde información).

### RF-06 Crear y gestionar tarjetas
- Crear tarjeta rápida en una columna (solo título, el resto después).
- Archivar y eliminar tarjetas.
- Crear/renombrar columnas: **fuera del MVP** salvo que sea trivial (preguntar).

### RF-07 Adjuntos
- Añadir desde cámara, galería y selector de archivos. Cualquier tipo; el límite lo impone el servidor.
- **Subida mediante cola con reintentos** (WorkManager) que sobrevive a cierres de app.
- **Descarga bajo demanda** al abrir el adjunto (no se descargan todos de golpe).
- Eliminar adjuntos desde el móvil, también sin conexión (se borran en el servidor al sincronizar).
- Indicador de estado: pendiente, subiendo, error, completado.
- **Criterios:**
  - Un adjunto añadido sin conexión se sube automáticamente al volver la red.
  - Un fallo permanente (p. ej. archivo demasiado grande) se muestra al usuario con opción de reintentar o descartar.

### RF-08 Offline-first y sincronización
- **Room es la fuente de verdad.** La UI lee solo de Room.
- Se almacenan localmente todos los tableros no archivados con sus tarjetas.
- Los cambios locales se guardan en una **cola de operaciones pendientes** y se envían en cuanto hay red.
- Sincronización: al abrir la app, con pull-to-refresh y periódica cada ~15 min en segundo plano (WorkManager).
- **Indicador discreto de "pendiente de sincronizar" por tarjeta.**
- Detección de cambios en servidor mediante ETag/`lastModified` cuando la API lo permita.
- **Criterios:** ver sección 5 (conflictos) y 7 (tests de sync).

### RF-09 Ajustes e internacionalización
- Inglés y español; sigue el idioma del sistema.
- Tema claro/oscuro/sistema y colores dinámicos (Material You).

### RF-10 Recordatorios de vencimiento
- Notificaciones **locales** cuando vence una tarjeta: sin servidor de push ni servicios de Google, funcionan sin conexión.
- En Ajustes: activarlos (desactivados por defecto), antelación (a la hora, 1 h antes, 1 día antes) y de qué tarjetas (solo las asignadas a mí, o todas).
- A la hora exacta cuando el sistema lo permite (permiso de alarmas exactas); si no, con unos minutos de margen.
- No avisan de tarjetas archivadas, borradas ni hechas; se reprograman tras cada sincronización o cambio y al reiniciar el móvil.
- Tocar la notificación abre la tarjeta.

## 4. Fuera de alcance (MVP)
- Comentarios, notificaciones push de servidor (los recordatorios locales son RF-10), widgets.
- Épicas, sprints, campos personalizados, informes (Deck no los almacena).
- Websockets o tiempo real (se usa polling/sync periódica).
- Multicuenta completo en la interfaz.
- Tablet/apaisado optimizado (solo arquitectura preparada).
- Gestión avanzada de tableros (crear, compartir, permisos).
- Cualquier servicio de Google, telemetría o analíticas.

## 5. Política de conflictos de sincronización

Cada tarjeta guarda, por campo editable, el valor local, el último valor conocido del servidor y la marca "modificado localmente".

1. **Título y descripción:** si se editaron offline **y** el servidor cambió ese mismo campo desde la última sincronización, **no se pisa nada**. Se muestra un diálogo con "tu versión / la del servidor" y opción de copiar tu texto antes de decidir.
2. **Resto de campos** (fecha, etiquetas, asignados, posición/columna): gana el **último cambio por campo** (por marca de tiempo del servidor frente a la local).
3. **Tarjeta borrada en servidor** mientras se editaba localmente: se avisa y se ofrece conservar una copia local o descartar.
4. **Orden/posición:** se recalcula contra el estado del servidor tras cada sync; las operaciones de movimiento pendientes se reaplican en orden.
5. Las operaciones de la cola son **idempotentes** y se reintentan con backoff exponencial.

**Requisito de test:** el resolutor y la cola tienen **100% de cobertura** con casos para cada regla anterior más fallos de red a mitad de operación.

## 6. Requisitos no funcionales

- **Rendimiento:** arranque en frío con datos locales < 1,5 s en un dispositivo de gama media; scroll a 60 fps con el volumen de referencia.
- **Privacidad:** sin telemetría, sin servicios de terceros; los datos solo viajan entre el dispositivo y el servidor Nextcloud del usuario. HTTPS obligatorio (permitir certificados de CA propias del usuario instaladas en el sistema; no desactivar la validación).
- **Seguridad:** credenciales cifradas; `allowBackup` desactivado o excluyendo credenciales; sin logs de datos sensibles.
- **Accesibilidad:** TalkBack, tamaños táctiles ≥ 48dp, escalado de fuente, contraste suficiente.
- **Robustez:** ninguna pérdida de datos del usuario ante cierres, falta de red o errores del servidor.
- **Escalabilidad de datos:** esquema Room versionado con migraciones probadas; claves compuestas `(accountId, id)`.

## 7. Calidad y CI

- **GitHub Actions:** en cada PR, build + detekt + ktlint + Android Lint + tests unitarios + Kover.
- **Cobertura (Kover):** ≥85% global en `domain`/`data`/`sync`; 100% en resolutor de conflictos y cola de sync; excluidos código generado, `@Preview` y UI Compose pura. Aspiracional: 100% global sin tests tautológicos.
- **Tests de sync offline:** simular caídas de red, respuestas 4xx/5xx, timeouts y cambios concurrentes con MockWebServer.
- **Tests de UI (Compose):** login, mover tarjeta, editar título/descripción offline.
- **Dependabot:** actualizaciones semanales agrupadas (Gradle y GitHub Actions).
- **Comprobación de licencias:** la CI falla si aparece una dependencia no libre o de Google Play Services.
- **Verificación de dependencias de Gradle** activada.
- **Versionado:** SemVer; Conventional Commits.

## 8. Riesgos conocidos
1. **Editor WYSIWYG en Compose**: no existe una solución madura y libre lista para usar. Hay que evaluar opciones (librería libre existente o editor propio sobre estado enriquecido) y garantizar el viaje de ida y vuelta con Markdown. Es la tarea de mayor riesgo del MVP y debe prototiparse pronto (ver `PLAN.md`, Fase 0).
2. **Diferencias entre versiones de Deck** en la API: fijar versión mínima y cubrirlo con tests de contrato.
3. **Drag & drop con autoscroll** en Compose con columnas anidadas: validar el enfoque en un prototipo antes de construir el resto.
4. **Reordenación y conflictos de posición** en la API de Deck: comprobar comportamiento real del servidor.

## 9. Decisiones abiertas (a confirmar durante la implementación)
- ~~Librería/estrategia para el editor WYSIWYG (ver riesgo 1).~~ Decidido en T03; ver "Decisiones tomadas".
- ~~Versión mínima de Deck soportada.~~ Provisional: Deck ≥ 1.3 (API v1.1), fijada en T05; se confirma al probar contra servidores reales (T06).
- Si crear/renombrar columnas entra en el MVP.
- Estrategia de builds reproducibles (firma, versión de Gradle/AGP fijadas).

### Decisiones tomadas
- **Drag & drop del tablero (T02, prototipo validado en dispositivo):** implementación propia sobre Compose Foundation, sin librerías externas.
  - Columnas en un `LazyRow` con *snap fling*. Cada columna ocupa el 82 % del ancho (máximo 400 dp), así que la siguiente asoma por el borde.
  - La gesture de pulsación larga + arrastre se detecta en el tablero, no en la tarjeta. Así el arrastre sobrevive a que la tarjeta cambie de columna y su composable original desaparezca. Tras la pulsación larga, los eventos se consumen en la fase `Initial` y el scroll de las listas se desactiva.
  - La tarjeta arrastrada se dibuja flotando por encima. El destino se calcula con la geometría de las tarjetas visibles y se muestra un hueco en vivo.
  - Autoscroll horizontal (y vertical en la columna destino) proporcional a la cercanía al borde.
  - Acciones de accesibilidad "Mover a <columna>" para mover tarjetas sin arrastrar (TalkBack).
  - La lógica de movimiento es pura y está en `domain/board` (`moveItem`), con tests; T13 la reutiliza con datos reales.
- **Editor de descripción (T03, prototipo validado en dispositivo):** editor propio "WYSIWYG por bloques" sobre el Markdown original, sin librería de texto enriquecido.
  - **Evaluación:** viaje Markdown → editor → Markdown sobre un corpus de 14 archivos (`app/src/test/resources/markdown-corpus/`).

    | Opción | Archivos idénticos |
    |---|---|
    | Editor propio | 14 / 14 |
    | compose-rich-editor 1.2.1 | 1 / 14 |

  - **Por qué se descarta compose-rich-editor:**
    - Borra los checkboxes (`- [ ] x` → `- x`).
    - Aplana tablas, bloques de código y citas a texto.
    - Vacía autoenlaces (`<https://…>` → `<>`) y quita títulos de enlaces y HTML.
    - Normaliza la sintaxis (`_x_` → `*x*`, `__x__` → `**x**`, `*` → `-`) y quita los espacios finales que fuerzan un salto de línea.
    - Además obliga a usar material3 en alpha.
  - **Diseño:**
    - La descripción guardada es siempre el Markdown original. Se analiza con JetBrains markdown (`org.jetbrains:markdown`, Apache-2.0) sin reescribirla.
    - Solo se reescribe el segmento que el usuario edita. Lo demás se conserva byte a byte; al abrir y cerrar el editor sin cambios, el texto queda idéntico.
  - **Lectura:** Markdown renderizado sin ningún símbolo, al estilo de Jira.
    - Los checkboxes se pueden marcar directamente.
    - Tocar el texto entra en edición.
  - **Edición:** sin símbolos tampoco. La descripción se divide en segmentos:
    - **Texto** (párrafos, títulos, listas, checklists, citas): se edita la fuente con los marcadores ocultos.
      - Intro continúa o termina listas y checklists.
      - Barra con negrita, cursiva, título, lista, checklist y tabla.
    - **Tablas:** en cuadrícula, con añadir o quitar filas y columnas.
      - Sin editar se guardan idénticas.
      - Editadas se regeneran con formato limpio, conservando la alineación, los `\|`, CRLF y el salto de línea final.
    - **Código:** solo el contenido, con las vallas y el lenguaje conservados.
    - **HTML:** tal cual.
  - **Pendiente para T14:** autoguardado, diálogo de conflictos, edición de enlaces e imágenes.
