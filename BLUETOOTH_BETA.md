# Pasar la bóveda a otro teléfono

Funciona sin internet ni cuenta de Google. Ambos teléfonos necesitan esta versión
de TheVault y deben estar vinculados en los ajustes Bluetooth de Android.

1. En el teléfono nuevo, pulsa **Recibir por Bluetooth** antes de iniciar sesión.
2. Actualiza la lista, elige el teléfono anterior y pulsa **Esperar copia**.
3. En el anterior, abre **Ajustes → Transferir por Bluetooth**, elige el nuevo
   teléfono y autoriza el envío con tu contraseña maestra.
4. En el nuevo, introduce la clave de recuperación original y elige una nueva
   contraseña. Al terminar, inicia sesión.

La copia completa se cifra con AES-256-GCM usando la clave de recuperación.
La contraseña y esa clave no se transmiten. La cuenta original se conserva.
Las copias recibidas quedan en memoria hasta restaurarlas o cerrar esta pantalla.

Una cuenta existente no se reemplaza ni se importa otra vez. Si la app se cierra
durante la restauración, inicia sesión con la nueva contraseña para terminar;
el registro de importación evita insertar las mismas filas dos veces.

Límites de esta beta: una cuenta por transferencia, hasta 1000 credenciales y
8 MiB por copia. La conexión caduca a los dos minutos. Mantén ambas apps abiertas.
No hay sincronización automática y no sirve si ya perdiste el teléfono anterior.

La base local se actualiza a la versión 5 sin borrar las credenciales. Un APK
anterior con base versión 4 no puede abrirla; conserva esta versión al probarla.

## Comprobación entre dos teléfonos

Usa una cuenta de prueba con varias credenciales y favoritos. Transfiérela y
comprueba que se puede iniciar sesión, abrir las contraseñas y recuperar el acceso
con la clave original. Repite el envío: no debe crear otra cuenta. Prueba también
cancelar y apagar Bluetooth durante el envío: la bóveda original debe conservarse.

Las pruebas automáticas cubren el cifrado, cortes de la trama, restauración con
almacenamientos independientes, reintentos y pantallas con transporte simulado.
No sustituyen la comprobación de la conexión Bluetooth entre teléfonos físicos.
