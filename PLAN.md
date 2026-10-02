# UltimateDeck — Plan de tareas

Reglas: una tarea cada vez, en su rama `feat/<tarea>`, con `./gradlew check` en verde antes de cerrarla. Marca `[x]` al completar. Cada tarea debe poder verificarse (test o prueba manual descrita).

## Fase 0 — Cimientos y prototipos de riesgo
- [x] **T00 Proyecto base**: módulo Android, Gradle KTS, `libs.versions.toml`, Hilt, Compose, tema Material 3, `strings.xml` en/es, cabeceras SPDX, `LICENSE` (GPLv3).
  - *Verificación:* `./gradlew assembleDebug` compila y la app arranca con pantalla vacía.
- [x] **T01 CI y calidad**: detekt, ktlint, Lint (warnings como errores), Kover con umbrales, verificación de dependencias de Gradle, chequeo de licencias/Play Services, workflow de GitHub Actions, Dependabot.
  - *Verificación:* un PR de prueba pasa CI; una dependencia de Play Services añadida a propósito la hace fallar.
- [x] **T02 Prototipo drag & drop**: columna y media + arrastre entre columnas con autoscroll, con datos falsos.
  - *Verificación:* demo manual en dispositivo; decisión documentada en `SPEC.md` (sección 9).
- [x] **T03 Prototipo editor WYSIWYG**: evaluar opciones y demostrar el viaje Markdown → editor → Markdown sin pérdidas con un corpus de ejemplos (listas, checklists, enlaces, código, tablas, imágenes).
  - *Verificación:* tests de ida y vuelta con el corpus; decisión documentada.

## Fase 1 — Datos y red
- [x] **T04 Modelo Room**: entidades con clave `(accountId, id)` para cuenta, tablero, columna, tarjeta, etiqueta, asignado, adjunto y cola de operaciones; migraciones y tests.
- [x] **T05 Cliente API de Deck**: Retrofit + serialización, endpoints de tableros/columnas/tarjetas/etiquetas/asignados/adjuntos; tests con MockWebServer (incluye errores 4xx/5xx y timeouts).
- [x] **T06 Login Flow v2**: flujo de autenticación, almacenamiento cifrado en Keystore, cierre de sesión que limpia datos.

## Fase 2 — Sincronización (lo más crítico)
- [x] **T07 Cola de operaciones pendientes**: operaciones idempotentes, backoff exponencial, persistencia en Room. **100% de cobertura.**
- [x] **T08 Resolutor de conflictos**: reglas de la sección 5 de `SPEC.md`. **100% de cobertura**, un test por regla más fallos a mitad de operación.
- [x] **T09 Motor de sincronización**: pull (tableros → columnas → tarjetas), push de la cola, detección de cambios, WorkManager periódico (~15 min) y sync al abrir / pull-to-refresh.

## Fase 3 — Interfaz del MVP
- [x] **T11 Login y lista de tableros** (offline funcional).
- [x] **T12 Vista de tablero**: columna y media, snap, tarjetas con etiquetas/avatares/fecha/adjuntos/checklist, indicador "pendiente de sync".
- [x] **T15 Crear, archivar y eliminar tarjetas.**
- [x] **T13 Mover y reordenar tarjetas**: integrar el prototipo T02 con datos reales y la cola de sync.
- [x] **T15b Tarjetas archivadas**: ver las tarjetas archivadas de un tablero y desarchivarlas.
- [x] **T14 Detalle de tarjeta con edición en línea**: título y descripción WYSIWYG (según T03), guardado automático, diálogo de conflicto (tu versión / servidor).
- [x] **T10 (pospuesta tras T14) Tests de sync offline**: caídas de red, cambios concurrentes, reintentos, app cerrada a mitad de sync.
- [x] **T16 Panel secundario**: fecha, etiquetas y asignados.
- [x] **T18 Ajustes**: tema, colores dinámicos, idioma, cuenta y cierre de sesión.
- [ ] **T17 Adjuntos**: cámara/galería/archivos, cola de subida con reintentos, descarga bajo demanda, estados visibles.

## Fase 4 — Cierre del MVP
- [ ] **T19 Accesibilidad y rendimiento**: TalkBack, tamaños táctiles, fuente grande; medir arranque y scroll con el volumen de referencia.
- [ ] **T20 Tests de UI clave (Compose)**: login, mover tarjeta, editar título/descripción offline.
- [ ] **T21 Metadatos F-Droid**: `fastlane/metadata/android/{en-US,es-ES}/`, iconos, capturas, descripciones; revisar builds reproducibles y ausencia de dependencias no libres.
- [ ] **T22 Documentación**: `README.md`, `CONTRIBUTING.md`, política de privacidad, `CHANGELOG.md`.

## Después del MVP (backlog, no implementar aún)
- Multicuenta completo en la UI.
- Tablet y apaisado.
- Comentarios, notificaciones, widgets.
- Crear/renombrar/reordenar columnas y gestión avanzada de tableros.
- Más idiomas.
