# Conexión entre TheVault Android y la API

La aplicación ya consume el backend de la carpeta hermana `API_theVault`. El emulador Android usa `10.0.2.2` para acceder al puerto 8000 del equipo anfitrión.

## Prueba local sin Firebase

En una terminal, inicia el backend en modo de desarrollo:

```powershell
cd C:\Users\dyarm\Documents\Archivosdedayana\Programacion\API_TheVault
Copy-Item .env.example .env -ErrorAction SilentlyContinue
uv run uvicorn thevault.main:app --host 0.0.0.0 --port 8000
```

La configuración de `.env.example` usa `THEVAULT_AUTH_MODE=development`. En este modo la app envía `Bearer dev:demo` y permite entrar con las credenciales de demostración mostradas en la pantalla de acceso.

Comprueba el backend desde el equipo:

```powershell
Invoke-RestMethod http://127.0.0.1:8000/api/v1/health
```

Después ejecuta la app en un emulador Android. Al iniciar sesión, la misma contraseña se usa como contraseña maestra para desbloquear y descargar la bóveda. Las operaciones de crear, editar y marcar como favorita se guardan cifradas en Room y luego se sincronizan con la API. Una eliminación remota se confirma antes de borrar la copia local.

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

## URL de la API y despliegue en AWS

La compilación `debug` usa `http://10.0.2.2:8000` cuando no se proporciona otra URL. Esta dirección funciona únicamente desde el emulador Android. Para AWS, configura la URL pública HTTPS mediante una de estas opciones:

```properties
# local.properties (no se versiona)
THEVAULT_API_BASE_URL=https://api.example.com
```

```powershell
$env:THEVAULT_API_BASE_URL="https://api.example.com"
.\gradlew.bat assembleRelease
```

Las compilaciones `release` fallan si la variable no está definida, si la URL no tiene host o si no usa HTTPS. El tráfico HTTP sin cifrar queda permitido solamente en `debug` para `10.0.2.2` y `localhost`.

En AWS, publica el servicio detrás de HTTPS (por ejemplo, mediante un Application Load Balancer o API Gateway) y reenvía las solicitudes al puerto configurado por `THEVAULT_PORT`. El proceso escucha en `0.0.0.0` mediante `THEVAULT_HOST` para poder recibir tráfico dentro del contenedor o instancia.

Si se despliega en ECS, App Runner u otra plataforma con disco efímero, no debe usarse el archivo SQLite local como almacenamiento definitivo. Se necesita un volumen persistente o sustituir el repositorio por una base administrada antes de producción.

## Seguridad de los datos

Antes de guardarse localmente o salir del dispositivo, cada credencial se serializa y cifra con AES-256-GCM. La clave de cada registro se deriva de la contraseña maestra mediante PBKDF2-HMAC-SHA256, una sal aleatoria y 310000 iteraciones. La contraseña maestra se conserva solamente en memoria durante la sesión. El backend recibe únicamente `saltB64`, `nonceB64` y `ciphertextB64`, nunca la contraseña ni las credenciales en texto plano.

La misma contraseña maestra permite recuperar los datos desde otra instalación. Los registros creados por la versión anterior se migran automáticamente al abrir la bóveda en el dispositivo original; después de esa migración pueden abrirse en otros dispositivos. Cambiar u olvidar la contraseña maestra impide descifrar los datos existentes, por lo que cualquier cambio de contraseña debe incluir un proceso de recifrado.
