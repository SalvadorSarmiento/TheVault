# Conexión entre TheVault Android y la API

La aplicación ya consume el backend de la carpeta hermana `API_theVault`. El emulador Android usa `10.0.2.2` para acceder al puerto 8000 del equipo anfitrión.

## Prueba local sin Firebase

En una terminal, inicia el backend en modo de desarrollo:

```powershell
cd C:\Users\dyarm\Documents\Archivosdedayana\Programacion\kotlin\API_theVault
Copy-Item .env.example .env -ErrorAction SilentlyContinue
uv run uvicorn thevault.main:app --host 0.0.0.0 --port 8000
```

La configuración de `.env.example` usa `THEVAULT_AUTH_MODE=development`. En este modo la app envía `Bearer dev:demo` y permite entrar con las credenciales de demostración mostradas en la pantalla de acceso.

Comprueba el backend desde el equipo:

```powershell
Invoke-RestMethod http://127.0.0.1:8000/api/v1/health
```

Después ejecuta la app en un emulador Android. Al iniciar sesión, la bóveda descarga las credenciales remotas. Las operaciones de crear, editar, marcar como favorita y eliminar se guardan primero en Room y luego se sincronizan con la API.

## Activación de Firebase

1. Registra en Firebase una aplicación Android con el paquete `com.example.thevault`.
2. Activa el proveedor Email y contraseña en Firebase Authentication.
3. Descarga `google-services.json` y colócalo en `TheVault/app/google-services.json`.
4. Crea al menos un usuario de prueba en Firebase Authentication.
5. Descarga una cuenta de servicio para el backend.
6. Configura la API:

```dotenv
THEVAULT_AUTH_MODE=firebase
GOOGLE_APPLICATION_CREDENTIALS=C:/ruta/firebase-service-account.json
```

7. Sincroniza Gradle y vuelve a compilar la app.

Cuando existe `app/google-services.json`, la compilación activa automáticamente Google Services. La pantalla de acceso usa Firebase Authentication, obtiene el ID token y lo envía en cada petición como `Authorization: Bearer <token>`.

## Seguridad de los datos

Antes de salir del dispositivo, cada credencial se serializa y cifra con AES-256-GCM. La clave de cifrado se deriva con PBKDF2-HMAC-SHA256 y 310000 iteraciones; el secreto maestro local queda protegido por Android Keystore. El backend recibe únicamente `saltB64`, `nonceB64` y `ciphertextB64`, nunca la contraseña en texto plano.

Esta implementación permite recuperar los datos cifrados en la misma instalación. Para sincronización entre varios dispositivos se debe añadir una contraseña maestra o un mecanismo seguro de transferencia de la clave de la bóveda.
