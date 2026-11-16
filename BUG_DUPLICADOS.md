# Credenciales duplicadas

La app podía guardar y sincronizar la misma cuenta al mismo tiempo. Al editar
antes de recibir su identificador, también volvía a pedir que se creara.

Se reprodujo con respuestas lentas: un solo toque generaba dos creaciones.
Ahora las operaciones esperan su turno y Guardar se bloquea mientras termina.
La edición conserva el identificador de la cuenta ya guardada.

Antes, las tres pruebas del fallo generaban dos registros. Con la corrección
mantienen uno. Pasaron 10 pruebas de la app y 6 de la API en local.

Rama de prueba: `prueba/credenciales-duplicadas`. La API se comprobó en local;
no se cambió su código ni se borraron las copias existentes.

Pruebas adicionales con cortes reales de conexión, la API local y Room temporal:

- API desconectada al guardar: conserva una sola cuenta al reconectar.
- Corte antes de que la API guarde: conserva una sola cuenta al reconectar.
- Corte después de guardar, antes de responder: todavía duplica; quedan dos
  cuentas en el servidor y dos en el teléfono.

Este último fallo sigue pendiente. La API debe reconocer el reintento como la
misma creación. La primera corrección no cubre ese caso.

Para repetirlo, consulta [las pruebas de conexión](PRUEBAS_CORTES.md).
