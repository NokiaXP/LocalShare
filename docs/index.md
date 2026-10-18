# LocalShare: documentación

## Uso general

Para compartir archivos desde tu teléfono Android con otro dispositivo de la misma red local, puedes seleccionarlos dentro de LocalShare o compartirlos directamente desde otras aplicaciones mediante el menú para compartir de Android.

Los archivos compartidos aparecerán en la [pantalla principal](#pantalla-principal) y en la [pantalla de archivos compartidos](#pantalla-de-archivos-compartidos).

También puedes compartir fragmentos de texto. Usa el menú para compartir de cualquier aplicación y selecciona LocalShare, o pega texto del portapapeles en la [pantalla de texto compartido](#pantalla-de-texto-compartido).

Al pulsar el botón «Iniciar uso compartido», los archivos y textos quedarán disponibles. Para acceder al contenido compartido, los demás dispositivos deben conocer la dirección URL que se muestra en la [pantalla principal](#pantalla-principal) mientras el uso compartido está activo.

## Detalles técnicos

LocalShare incorpora un servidor web que permite acceder a los archivos compartidos desde un navegador. Así, cualquier dispositivo de la misma red puede acceder al contenido sin instalar programas adicionales.

Para que los archivos nuevos aparezcan inmediatamente en la interfaz web, la aplicación utiliza **SSE** (*Server-Sent Events*, eventos enviados por el servidor), que permite actualizar la página en tiempo real sin tener que recargarla.

LocalShare está diseñada principalmente para redes locales y utiliza **HTTP** (*Hypertext Transfer Protocol*, protocolo de transferencia de hipertexto) sin cifrado, en lugar de **HTTPS** (*Hypertext Transfer Protocol Secure*, protocolo seguro de transferencia de hipertexto). En redes locales suele ser difícil utilizar HTTPS porque normalmente no se dispone de certificados firmados válidos para direcciones IP locales.

El servidor HTTP puede vincularse a una dirección IP específica, de modo que solo acepte conexiones dirigidas a esa dirección. En general, cualquier persona que pueda acceder a tu dispositivo a través de la red podrá ver el contenido compartido si conoce la dirección IP correcta y el token secreto correspondiente.

## Pantallas

### Pantalla principal

#### Tarjeta principal

En la parte superior se muestra si el uso compartido está activo. Desde allí puedes acceder a la dirección URL o al código QR que permite llegar a tus archivos.

#### Archivos compartidos

Aquí aparece la lista de archivos compartidos. Puedes añadir archivos con el botón «Añadir archivos» o quitarlos manteniendo pulsado el icono de un archivo y seleccionando «Quitar». El pequeño botón «X» de la derecha vacía toda la lista.

#### Detalles técnicos

Puedes establecer manualmente el token de seguridad. De forma predeterminada, se genera uno aleatorio al iniciar la aplicación; puedes desactivar esta opción en la [pantalla de configuración](#pantalla-de-configuración). Esto puede resultar útil si quieres acceder al dispositivo mediante la misma dirección URL cada vez.

También puedes seleccionar la dirección IP o la interfaz de red a la que se vinculará el servidor HTTP. Esto resulta útil si el teléfono está conectado a varias redes y quieres limitar el acceso a una de ellas. Al vincular el servidor a una IP específica, solo aceptará conexiones dirigidas a esa dirección.

#### Actividad reciente

Aquí aparecen las últimas entradas del registro. Consulta la [pantalla de registros](#pantalla-de-registros) para obtener más información.

#### Botón «Iniciar/Detener uso compartido»

Pulsa este botón para iniciar o detener el uso compartido. Mientras esté activo, aparecerá una notificación en Android.

### Pantalla de archivos compartidos

Muestra una lista ordenable de todos los archivos compartidos. Puedes añadir archivos con el botón «+» o quitar uno manteniéndolo pulsado y seleccionando «Quitar».

### Pantalla de texto compartido

Muestra los fragmentos de texto compartidos. Pulsa el botón para pegar texto desde el portapapeles. Mantén pulsado un fragmento para seleccionarlo y eliminarlo.

### Pantalla de registros

Aquí se muestran las conexiones establecidas con el dispositivo. Las conexiones más recientes y las que siguen abiertas aparecen arriba. Cuando otro dispositivo abre la interfaz web para acceder a tus archivos, normalmente habrá una conexión activa correspondiente a SSE, que mantiene actualizada la interfaz.

La lista tiene tres columnas: estado de la conexión, ruta a la que se accede y dispositivo que inició la conexión.

La dirección IP del cliente puede aparecer en gris si la conexión está pendiente de aprobación, en verde si el cliente está en la lista de permitidos o en rojo si está en la lista de bloqueados.

Mantén pulsada una entrada para añadir un cliente a la lista de bloqueados o quitarlo de la lista de permitidos o de bloqueados. No puedes añadirlo a la lista de permitidos de esta manera: cuando un cliente intenta conectarse, aparece una notificación para solicitar aprobación. Si lo apruebas, se añade a la lista de permitidos.

Las listas de permitidos y bloqueados se restablecen al iniciar la aplicación.

#### Códigos de estado y símbolos

- **Indicador giratorio:** la conexión sigue abierta.
- **200 en verde — Correcto:** el contenido se entregó correctamente.
- **204 en verde — Sin contenido:** evita que el navegador solicite un icono de sitio en la ubicación predeterminada de la interfaz web.
- **206 en verde — Contenido parcial:** el servidor entrega solo una parte del recurso debido a una solicitud de rango del cliente. Se utiliza para avanzar dentro de un vídeo y reanudar descargas.
- **403 en rojo — Prohibido:** se denegó el acceso por un token o una ruta no válidos.
- **404 en rojo — No encontrado:** no se encontró un archivo que debería existir.
- **410 en rojo — Ya no disponible:** un archivo que antes estaba disponible dejó de estarlo. Esto puede ocurrir si se compartió mediante el menú de Android y su dirección temporal dejó de ser válida.
- **500 en rojo — Error interno del servidor:** ocurrió un problema en el servidor, por ejemplo, al generar una plantilla. No debería suceder; si puedes reproducirlo, informa del problema.

Los siguientes códigos solo aparecen en la conexión SSE:

- **Símbolo de detener, en verde:** se detuvo el uso compartido y se interrumpió la conexión.
- **Símbolo de cierre, en verde:** el cliente cerró la conexión, por ejemplo, al cerrar la ventana del navegador.
- **Escudo, en rojo:** el cliente fue bloqueado.
- **Signo de interrogación, en rojo:** ocurrió un problema desconocido.

### Pantalla de configuración

#### Configuración del servidor

- **Puerto:** puerto TCP (*Transmission Control Protocol*, protocolo de control de transmisión) en el que debe escuchar el servidor HTTP. Forma parte de la dirección URL de la interfaz web. El valor predeterminado es `8080`; `8000` también es habitual y suele funcionar en redes locales.
- **Tiempo de inactividad:** cantidad de segundos sin conexiones activas antes de detener el uso compartido. Mientras un cliente mantenga abierta la interfaz web, habrá una conexión activa y el servidor no se detendrá automáticamente.

#### Seguridad

- **Generar un nuevo token al iniciar la aplicación:** crea un token de seguridad nuevo cada vez que se inicia la aplicación.
- **Solicitar aprobación manual:** si se activa, cada nuevo cliente que intente conectarse generará una notificación para solicitar aprobación. Es muy recomendable en redes que no sean completamente confiables.
- **Duración de las entradas de la lista de permitidos:** cantidad de segundos tras la cual el cliente tendrá que volver a recibir aprobación.

#### Interfaz web

- **Intervalo de señal de actividad de SSE:** el servidor envía una señal periódica para comprobar si la conexión SSE con la interfaz web sigue activa. Si el intervalo es más largo, tardará más en detectar que una conexión se cerró, por ejemplo, cuando el cliente cierra el navegador.

#### Otros ajustes

- **Vaciar la lista de archivos al compartir:** si se activa, la lista se vacía cada vez que se comparten archivos nuevos mediante el menú para compartir de Android.

### Pantalla de información

Aquí se muestra la versión de la aplicación y, especialmente, el enlace a la página de incidencias del proyecto en GitHub.

## Preguntas frecuentes

### ¿Por qué aparece un indicador giratorio al principio de la lista de registros?

Hay una conexión abierta, probablemente una conexión SSE con una interfaz web.

### ¿Puedo reproducir vídeos directamente en un reproductor multimedia?

Sí. Copia desde la interfaz web el enlace del archivo y ábrelo con el reproductor de vídeo que prefieras para poder avanzar y retroceder dentro del vídeo. Tanto el formato del vídeo como el reproductor deben ser compatibles con la reproducción en continuo.
