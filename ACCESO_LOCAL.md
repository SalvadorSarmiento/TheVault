# Acceso y recuperación local

Al abrir la app por primera vez puedes crear tu cuenta, sin Google ni API. El correo solo identifica la cuenta en ese dispositivo. Se retiraron el acceso de prueba y el token compartido de desarrollo.

1. Escribe tu correo y una contraseña de al menos 12 caracteres; repítela.
2. Anota la clave de recuperación que aparece. Todavía no se guarda la cuenta.
3. Marca que la anotaste y escribe sus últimos 8 caracteres. Al confirmar se guarda la copia cifrada y se abre la bóveda.
4. Para volver a entrar usa tu correo y contraseña. En «Olvidé mi contraseña» puedes usar la clave completa y elegir una nueva contraseña.

Es una clave de recuperación inspirada en ese tipo de experiencia; no integra Microsoft BitLocker. Cambiar la contraseña con ella conserva las credenciales. La clave también queda cifrada localmente, pero esa copia no evita tener que recordar la contraseña o conservar la clave anotada.

Si aparece «Conservar mi bóveda anterior», escribe también su contraseña anterior. La importación mantiene los identificadores locales y se puede continuar al iniciar sesión si la app se cerró durante el guardado. No borres los datos de la app si aparece un error.

## Protección y límites

- Cada cuenta tiene una clave aleatoria propia para cifrar la bóveda. La contraseña y la clave de recuperación protegen dos formas de abrirla.
- La copia local usa AES-256-GCM y Android Keystore. La contraseña se transforma con PBKDF2-HMAC-SHA256, sal aleatoria y 600.000 iteraciones; no se guarda la contraseña.
- Los archivos de acceso están en el almacenamiento privado excluido de copias automáticas. No se envían credenciales a la API desde este acceso local.
- La recuperación requiere conservar los datos y las claves del dispositivo. No restaura una app desinstalada, un teléfono perdido ni datos borrados. Sin contraseña ni clave anotada no hay recuperación.
- Se requiere Android 8 o posterior: el cifrado con PBKDF2-HMAC-SHA256 no está disponible de forma nativa en las versiones anteriores.

Referencias: [cifrado de Android](https://developer.android.com/privacy-and-security/cryptography), [almacenamiento sin copia automática](https://developer.android.com/identity/data/autobackup), [compatibilidad de PBKDF2](https://developer.android.com/reference/javax/crypto/SecretKeyFactory), [coste de PBKDF2 en OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html).

## Cómo verificar

Compila con JDK 25 y Android SDK 37:

```powershell
./gradlew.bat :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.example.thevault.AccesoLocalTest,com.example.thevault.RegistroLocalUiTest com.example.thevault.test/androidx.test.runner.AndroidJUnitRunner
adb shell am start -n com.example.thevault/.MainActivity
```

Prueba sin conexión: crea una cuenta, anota la clave, guarda una credencial, cierra sesión y vuelve a entrar. Después recupera el acceso con otra contraseña: la misma credencial debe seguir ahí y la contraseña anterior debe rechazarse. Las pruebas automáticas usan archivos y bases temporales, sin registrar cuentas reales.

Verificado en emulador API 37.2: compilación de depuración y distribución, instalación sin borrar datos y pruebas de registro, recuperación, importación interrumpida y guardado. La revisión automática no encontró errores; quedan avisos de dependencias y recursos. Falta comprobarlo en un teléfono físico.

## Pendientes

Faltan copia exportable cifrada, bloqueo automático al salir de la app y cambio de clave de recuperación. El acceso local no verifica la propiedad del correo. La integración futura con Google/API debe usar identidad propia por usuario; el problema pendiente de respuesta perdida de la API sigue documentado en `BUG_DUPLICADOS.md`.
