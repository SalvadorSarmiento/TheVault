# Pruebas de conexión en local

Usan la API del repositorio y una base temporal, con cuentas inventadas.
La tercera prueba falla actualmente porque reproduce el duplicado pendiente.
No se ejecutan por defecto: hay que activar `api_cortes=true`.

Abre el emulador. En una terminal, sustituye las rutas e inicia la API de prueba:

```powershell
uv run --project "RUTA_API" --python 3.12 --frozen python "RUTA_APP/scripts/probar-cortes-api.py"
```

En otra terminal, desde la carpeta de la app:

```powershell
./gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
$adb = "$env:ANDROID_HOME/platform-tools/adb.exe"
& $adb reverse tcp:18082 tcp:18082
& $adb reverse tcp:18083 tcp:18083
& $adb install -r app/build/outputs/apk/debug/app-debug.apk
& $adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $adb shell am instrument -w -e api_cortes true -e class com.example.thevault.CortesConexionTest com.example.thevault.test/androidx.test.runner.AndroidJUnitRunner
```

Al terminar:

```powershell
Invoke-RestMethod http://localhost:18083/terminar
& $adb reverse --remove tcp:18082
& $adb reverse --remove tcp:18083
```
