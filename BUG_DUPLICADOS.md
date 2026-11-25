# Credenciales duplicadas

La app podía guardar y sincronizar la misma cuenta al mismo tiempo. Al editar
antes de recibir su identificador, también volvía a pedir que se creara.

Se reprodujo con respuestas lentas: un solo toque generaba dos creaciones.
Ahora las operaciones esperan su turno y Guardar se bloquea mientras termina.
La edición conserva el identificador de la cuenta ya guardada.

Para cubrir la pérdida de respuesta, la app conserva un identificador por
creación y lo envía como `Idempotency-Key`. La API reconoce el reintento y devuelve
la misma cuenta. Si hubo una edición pendiente, se actualiza ese registro.
Al reconectar, se resuelven los guardados pendientes antes de descargar la lista.

Requiere también la API de `dayanaArmasB/API_TheVault`, rama
`prueba/evitar-duplicados`. Los clientes antiguos sin esta cabecera todavía pueden
generar copias al reintentar. No se borran duplicados existentes.

La app sigue usando acceso local; esta corrección no activa la sincronización.
Las pruebas con API usan cuentas ficticias y una base temporal.

Para repetirlo, consulta [las pruebas de conexión](PRUEBAS_CORTES.md).
