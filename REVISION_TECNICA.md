# Revisión técnica de salvador — 15 de noviembre de 2026

Base revisada: `55bcdcb2c4e9565e632037ed9659ce4f027d5a34`.
Se buscaron PR e incidencias de todo el repositorio, abiertas y cerradas: no se
encontraron. Tampoco había ejecuciones de Actions ni checks en el HEAD de main.
Los resultados siguientes proceden de comprobaciones locales, no de CI de GitHub.

## Problemas corregidos

| Prioridad | Problema observado | Corrección y evidencia |
|---|---|---|
| Alta | La migración Room v3→v4 fallaba con `Queries can be performed using ... query or rawQuery methods only`. | El PRAGMA `secure_delete` usa `query`, cierra el cursor y verifica su resultado. La prueba migra una base temporal y comprueba conservación del secreto, eliminación de columnas y ausencia del secreto en el fichero. |
| Alta | JUnit no ejecutaba la clase de pruebas de almacenamiento: la migración de prueba devolvía Boolean. | `runBlocking<Unit>`; la clase completa se ejecuta correctamente. |
| Alta | Espresso 3.5.1 fallaba en API 37 buscando `InputManager.getInstance`. | Espresso 3.7.0 y AndroidX Test JUnit 1.3.0; apertura y login probados. |
| Alta | Generador de contraseñas con PRNG general y un primer valor fijo conocido. | `SecureRandom`, generación inicial y regeneración al limpiar la sesión. |
| Alta | Una release podía compilar sin Firebase y conservar la autenticación demo. | La validación de release exige configuración Firebase y URL HTTPS sin credenciales, query ni fragmento. Se comprobó el rechazo sin Firebase. |
| Media | La ventana mostraba secretos en capturas y pantallas no seguras. | `FLAG_SECURE` en MainActivity. |
| Media | Parámetros de cifrado provenientes de almacenamiento/API sin límites de coste y tamaño. | PBKDF2 entre 310000 y 1000000 iteraciones, sal/nonce del tamaño esperado y ciphertext de hasta 1 MiB. Prueba de parámetros fuera de rango. |
| Media | Respuesta HTTP sin límite de memoria y redirecciones autenticadas automáticas. | Límite de 8 MiB y redirecciones desactivadas. Ambas rutas tienen pruebas con un servidor local de prueba. |
| Media | Buffers temporales de clave y texto plano permanecían en memoria hasta el GC. | Limpieza en bloques `finally`; esto no permite borrar los String inmutables de JVM. |

## Verificación realizada

- `assembleDebug` y `assembleDebugAndroidTest`: correctos.
- Instalación con `adb install -r`: `Success`.
- `am start -W`: `Status: ok`; proceso activo después de 5 segundos.
- Suite instrumentada: `OK (8 tests)`, emulador API 37 x86_64, páginas de 16 KB.
- `testDebugUnitTest`: correcto; la prueba unitaria preexistente es solo una prueba de ejemplo.
- `lintDebug`: correcto, con avisos de dependencias, iconos, recursos sin uso y estilo.
- Release sin `google-services.json`: rechazada como se esperaba.

Se verificó login demo y almacenamiento local. No se configuraron cuentas reales
de Firebase, certificados de publicación ni un backend AWS: su operación extremo
a extremo sigue pendiente. Ver `ANDROID_EMULATOR.md` para repetir la verificación.

## Riesgos y desarrollo pendiente

1. **Alta — conflictos y cambios offline.** `syncFromRemote` actualiza las copias
   locales desde el servidor y solo reintenta pendientes con `remoteId == null`.
   Una edición offline de un registro ya sincronizado puede ser sustituida por la
   versión remota al volver a entrar. Hace falta una cola persistente de operaciones,
   estado dirty/revisiones y una política de conflictos, con pruebas de desconexión.
   Este cambio requiere diseñar también el contrato con el backend; no está resuelto
   por las correcciones de despliegue.
2. **Alta — sesión desbloqueada en segundo plano.** Falta bloqueo por inactividad o
   al enviar la app al fondo y cancelación coordinada de trabajos de sesión. Cerrar
   sesión sí limpia el estado, pero no hay una política automática de bloqueo.
3. **Alta — backend y Firebase sin auditoría.** Este repositorio no demuestra que
   AWS valide tokens, separe usuarios, limite peticiones o mantenga copias de seguridad.
   Requiere revisar el backend y probar con cuentas reales antes de producción.
4. **Media — portapapeles.** Se siguen copiando contraseñas con la API antigua de
   Compose, sin etiquetar el contenido como sensible ni definir un borrado temporizado.
   Hace falta una política que no borre contenido posterior que copie el usuario.
5. **Media — contraseñas maestras.** Faltan cambio de contraseña con recifrado,
   exportación/importación y recuperación documentada. Debug conserva credenciales
   demo conocidas. La release requiere Firebase, pero la fortaleza de la contraseña
   y su política deben configurarse y comprobarse en ese proyecto.
6. **Media — escalabilidad de sincronización.** La API se lee como una lista completa;
   los límites nuevos hacen fallar respuestas mayores de 8 MiB. Faltan paginación,
   límites por lote, pruebas de volumen y compatibilidad explícita del protocolo.
7. **Media — automatización.** No hay CI en el repositorio. Queda por configurar
   compilación, lint y pruebas en cada push/PR, además de las reglas de revisión.
8. **Baja — mantenimiento.** Quedan avisos de ClipboardManager, icono Logout sin
   AutoMirrored, recursos duplicados/sin uso y dependencias con versiones nuevas.
   No impidieron compilar, instalar o ejecutar; no se actualizó todo el ecosistema
   a la vez sin pruebas de compatibilidad.

## Diferencia entre ramas y estado del emulador

`main` es una demo Room v1; `salvador` es la aplicación v4 con migraciones. En el
primer recorrido se reprodujo `A migration from 3 to 1 was required but not found`
al abrir main sobre una instalación con datos v3. No debe solucionarse borrando
datos o agregando fallback destructivo: para el trabajo actual corresponde salvador.

La primera ejecución con `connectedDebugAndroidTest` retiró la instalación del AVD
al terminar (se observó `run-as: unknown package`). Esto puede afectar a datos locales
de esa instalación. Las verificaciones siguientes usan instalación `-r` e
instrumentación directa para conservar la instalación. Las pruebas de migración
usan bases temporales propias y no migran la bóveda activa para fabricar el resultado.

## Fuentes técnicas

- [AndroidX Test: corrección de InputManager en Espresso 3.7.0](https://developer.android.com/jetpack/androidx/releases/test)
- [Android: proteger actividades con FLAG_SECURE](https://developer.android.com/security/fraud-prevention/activities)
- [Java: SecureRandom](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/security/SecureRandom.html)

Esta revisión corrige fallos reproducidos y añade protecciones específicas; los
puntos pendientes anteriores impiden afirmar que la aplicación está lista para
producción o que todos los riesgos de seguridad están resueltos.
