# LocalShare

[![Google Play](https://img.shields.io/badge/Google_Play-Descargar-green?logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.defname.localshare)
[![F-Droid](https://img.shields.io/badge/F--Droid-Descargar-blue?logo=f-droid&logoColor=white)](https://f-droid.org/packages/com.defname.localshare)

![Logotipo](raw_assets/logo.svg)

**LocalShare** es una aplicación ligera para Android que permite compartir archivos a través de la red local mediante un navegador, sin nube, cables ni aplicaciones adicionales.

## Funciones

- Compartir archivos directamente desde el menú para compartir de Android.
- Acceder a los archivos mediante una interfaz web sencilla.
- Interfaz web que se actualiza en tiempo real en el dispositivo receptor, sin necesidad de recargar la página.
- Reproducir vídeos y audio con controles de búsqueda y avance.
- Compartir varios archivos o descargarlos juntos en un archivo ZIP.
- Acceso mediante token de seguridad y aprobación opcional de dispositivos.
- Sin anuncios y de código abierto.

## Inicio rápido

1. Comparte archivos con LocalShare.
2. Pulsa **Iniciar uso compartido**.
3. Abre la dirección que aparece en otro dispositivo o escanea el código QR.
4. Descarga los archivos o reproduce el contenido multimedia.

## Seguridad

> **Importante:** los archivos se transfieren mediante HTTP sin cifrado.

LocalShare está diseñada para usarse únicamente en redes locales de confianza, como la red Wi-Fi de tu hogar. Evita utilizarla en redes públicas o que no sean de confianza, especialmente para transferir información sensible.

## Documentación

Consulta la [documentación completa](docs/index.md), disponible en la carpeta `docs/`.

## Permisos

### Notificaciones

Son necesarias porque el servidor funciona como un servicio en primer plano. También se utilizan para avisarte cuando otros dispositivos intentan conectarse.

## Compilar el proyecto

### Requisitos

- Android SDK (kit de desarrollo de software para Android) o Android Studio.
- Python 3, utilizado por un pequeño script de compilación.
- npm, el gestor de paquetes de Node.js, necesario para Tailwind CSS.

### Pasos

1. Clona el repositorio incluyendo sus submódulos para obtener también el tema de iconos.
2. Configura las variables `JAVA_HOME` y `ANDROID_SDK_ROOT` (o `ANDROID_HOME`).
3. Ejecuta `./gradlew assembleDebug`.
4. El archivo APK se generará en `app/build/outputs/apk/debug/`.

## Licencia y créditos

El proyecto se distribuye bajo la licencia **GNU GPL versión 3 o posterior**.

Los iconos proceden de Papirus Icon Theme, publicados bajo la licencia GPL-3.0 y mantenidos por el equipo de desarrollo de Papirus: https://github.com/PapirusDevelopmentTeam/papirus-icon-theme
