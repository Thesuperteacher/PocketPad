# PocketPad: guía rápida

Usa tu teléfono Android como trackpad y teclado para una PC con Windows. La interfaz de la app está en inglés.

## Instalación

Descarga los archivos de la [beta oficial](https://github.com/Thesuperteacher/PocketPad/releases/tag/v0.3.1). Instala el APK en Android 7 o posterior. Extrae el paquete de Windows y conserva los archivos DLL junto a `PocketPad.exe`. Necesitas Windows 10/11 x64.

El APK usa una firma de depuración y el programa de Windows no tiene firma de editor. Son versiones experimentales. Compara las descargas con `SHA256SUMS.txt` y revisa los avisos de instalación. No desactives tu software de seguridad.

## Conexión

**Wifi:** conecta ambos dispositivos a la misma red de confianza. Selecciona el adaptador de esa red en la PC y pulsa **Start connection**. En el teléfono abre la conexión, elige **Scan PC code · Wi-Fi / USB tethering** y escanea el código. Mantenlo privado.

**Depuración USB:** instala Android Platform Tools, activa la depuración USB y acepta el aviso en el teléfono desbloqueado. Usa un cable de datos. Ejecuta `Start-USB.ps1`, elige **USB cable / this PC only** en la PC e inicia la conexión. En el teléfono selecciona **Scan PC code · USB debugging cable**. Vuelve a ejecutar el asistente después de desconectar y reconectar el cable.

**Conexión compartida por USB:** activa el anclaje USB en Android. Vuelve a abrir el programa de la PC, selecciona el adaptador USB y escanea con la opción de wifi/tethering. Esta función también puede compartir los datos móviles.

**Bluetooth:** experimental; requiere Android 9+ y firmware HID compatible. Elige **Bluetooth mouse & keyboard** y empareja la PC. No necesita el programa de Windows. Abre el teclado manualmente. El texto por Bluetooth admite ASCII imprimible con la distribución estadounidense en la PC. Para acentos y emoji, usa USB o wifi.

## Gestos

| Gesto | Acción |
|---|---|
| Deslizar un dedo | Mover el cursor |
| Tocar una o dos veces | Clic o doble clic |
| Tocar, levantar y volver a tocar rápido manteniendo el dedo | Arrastrar; levanta el dedo para soltar |
| Deslizar dos dedos | Desplazamiento vertical |
| Tocar con dos dedos | Clic derecho |
| Activar Drag | Mantener pulsado el botón; toca otra vez para soltar |

Por USB o wifi, seleccionar un campo de texto compatible en la PC puede abrir el teclado del teléfono automáticamente. Escribe y pulsa **Send text**. El cuadro del teléfono es un borrador; no se envía cada tecla al instante. También puedes abrir **Keyboard** manualmente.

## Límites y ayuda

Las pruebas físicas se han centrado en un Galaxy Z Fold5. Bluetooth y la compatibilidad con más equipos siguen pendientes de pruebas. Algunos campos personalizados no abren el teclado automáticamente. Las ventanas elevadas, UAC y la pantalla de inicio de sesión quedan fuera de los permisos normales.

Si no conecta, revisa el cable, la autorización USB, el adaptador de red seleccionado y el firewall. Las redes de invitados pueden aislar los dispositivos. Detén la conexión en la PC para invalidar el código.

La [guía en inglés](../README.md) incluye las instrucciones completas y los pasos de desarrollo. Al informar de un problema, no compartas códigos de conexión, números de serie ni texto personal.
