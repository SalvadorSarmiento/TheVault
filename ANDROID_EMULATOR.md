# Verificar TheVault en el emulador Android

Este procedimiento corresponde a la rama `salvador`. `main` contiene una demo
anterior con Room v1; `salvador` usa Room v4 y registra las migraciones 1→2→3→4.
Instalar la demo de `main` sobre una base v3/v4 provoca un downgrade sin migración.
Para continuar con datos existentes, usa `salvador`; no borres la base ni agregues
una migración destructiva para ocultar el error.

## Requisitos

- JDK 25, requerido por `gradle/gradle-daemon-jvm.properties`.
- Android SDK Platform 37 para compilar y Platform Tools con `adb`.
- Un emulador iniciado, API 24 o superior. La verificación se realizó en API 37,
  x86_64, con páginas de 16 KB.
- `JAVA_HOME` y `ANDROID_HOME` configurados. El wrapper fija Gradle 9.6.0 y su SHA-256.

## Compilar, instalar y probar

Desde la raíz del repositorio, en PowerShell:

```powershell
& "$env:ANDROID_HOME/platform-tools/adb.exe" devices -l
./scripts/verify-android.ps1 -Serial emulator-5554
```

El script selecciona explícitamente el dispositivo, espera hasta 120 segundos a
que termine de arrancar, verifica `minSdk`, compila los dos APK, instala con
`adb install -r`, abre `MainActivity`, comprueba que el proceso sigue vivo y ejecuta
la suite instrumentada. No desinstala la aplicación ni limpia sus datos. Un error
de instalación se conserva en la salida; un fallo de ejecución muestra AndroidRuntime.
Un código de salida cero de `adb am instrument` no basta: el script también exige
el resultado JUnit `OK (...)` y rechaza fallos de instrumentación.

La suite incluye apertura del login, acceso demo a la bóveda local, cifrado con
contraseña maestra y migración de Room v3 a v4 que conserva el secreto y elimina
las columnas en texto plano. Las pruebas de almacenamiento usan una base temporal.
El login demo se omite si se ha configurado Firebase; en ese caso debe verificarse
manualmente con una cuenta de pruebas de ese proyecto.

Comprobaciones adicionales:

```powershell
./gradlew.bat :app:testDebugUnitTest :app:lintDebug
```

Los informes quedan en `app/build/reports/tests/testDebugUnitTest/` y
`app/build/reports/lint-results-debug.html`. Las pruebas instrumentadas del script
se muestran en la terminal. El task Gradle `connectedDebugAndroidTest` puede retirar
los APK al terminar: usa un emulador de pruebas o el script anterior para conservar
la instalación entre ejecuciones.

## Verificación manual

1. Abre TheVault y espera a que desaparezca la pantalla inicial.
2. Sin Firebase, usa `ana@ejemplo.com` / `123456`; debe aparecer “Mi bóveda”.
3. Crea una credencial de prueba, cierra la aplicación, vuelve a abrirla e inicia
   sesión con la misma contraseña maestra. La credencial debe seguir disponible.
4. Para probar sincronización, inicia el backend y comprueba su health endpoint.
   El debug usa `http://10.0.2.2:8000` por defecto. La verificación de instalación
   y almacenamiento local no demuestra que AWS o Firebase estén operativos.

Consulta `CONEXION_API.md` para configurar el backend y Firebase. Un backend
inaccesible debe mostrar el mensaje de modo local, no confundirse con un fallo
de instalación del APK. `release` exige una URL HTTPS configurada y no es el
artefacto usado por este procedimiento.

## Diagnóstico de fallos observados

- `NoSuchMethodException: android.hardware.input.InputManager.getInstance`:
  Espresso 3.5.1 falla en este emulador. Se actualiza a Espresso 3.7.0 y AndroidX
  Test JUnit 1.3.0. La corrección de InputManager figura en las
  [notas oficiales de AndroidX Test](https://developer.android.com/jetpack/androidx/releases/test).
- `Method ... should be void`: una prueba devolvía el Boolean de `deleteDatabase`
  mediante `runBlocking`. `runBlocking<Unit>` restablece la firma válida para JUnit.
- `Queries can be performed using SQLiteDatabase query or rawQuery methods only`:
  la migración v3→v4 ejecutaba un PRAGMA que devuelve filas mediante `execSQL`.
  Ahora usa `query` y comprueba que `secure_delete` está activado antes de migrar.
- `A migration from 3 to 1 was required but not found`: se instaló la demo antigua
  de `main` sobre datos más nuevos. Usa `salvador` con su ruta de migración existente.
- `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: hay una firma distinta para el mismo paquete.
  Usa el certificado de la instalación existente o un AVD separado. No desinstales
  una bóveda con datos que necesites conservar.
- `offline`, ausencia del dispositivo o arranque incompleto: comprueba el serial
  en `adb devices -l` y el estado del AVD antes de atribuir el fallo al código.

## Protecciones adicionales

La actividad usa `FLAG_SECURE`: las capturas de pantalla y la proyección en
pantallas no seguras quedan bloqueadas. El generador crea contraseñas nuevas con
`SecureRandom`, incluido su valor inicial. Las respuestas HTTP se limitan a 8 MiB
y no se siguen redirecciones autenticadas automáticamente. Los registros cifrados
se limitan a 1 MiB y PBKDF2 admite de 310000 a 1000000 iteraciones; el formato actual
genera 310000. Un protocolo que requiera valores superiores necesita una revisión
explícita de estos límites. Los buffers temporales de claves y texto plano se limpian
cuando es posible, aunque los `String` de JVM no se pueden borrar de forma fiable.

Una compilación `release` exige además `app/google-services.json` y una URL HTTPS
sin credenciales, query ni fragmento. Debug conserva el acceso demo para desarrollo.
