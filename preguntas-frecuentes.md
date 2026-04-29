# Preguntas frecuentes


## ¿Cómo utilizar Firmador por línea de comandos o de forma automatizada?

Estos comandos son para utilizar con el fichero firmador.jar y el comando java.
Esta sintaxis se planea reemplazar en futuras actualizaciones.

Firmador cuenta actualmente con 3 tipos de interfaces de usuario:
- `swing` (predeterminada)
- `shell` (interactiva por consola)
- `args` (no interactiva, con parámetros)

Para seleccionar la interfaz, utilizar -d seguido de la interfaz sin espacios.

Ejemplo para seleccionar la interfaz interactiva por consola:

    java -jar firmador.jar -dshell

Ejemplo para la interfaz por parámetros:

    java -jar firmador.jar -dargs original.pdf firmado.pdf

En este caso solicita el pin de forma interactiva pero podría suministrarse
por ejemplo de la siguiente manera para que quede completamente automatizado:

    echo 0000 | java -jar firmador.jar -dargs original.pdf firmado.pdf

Donde `0000` es un pin de ejemplo, así como las rutas de carga y guardado, que
pueden ser absolutas o relativas. Se recomienda entrecomillar las rutas si
contienen caracteres especiales, como por ejemplo espacios.

En el caso de ejecutarse en Windows, no debe dejar espacio antes del símbolo
`|` o no reconocerá el PIN como válido. Ejemplo: `echo 0000| java(...)`

El parámetro `-timestamp` permite agregar sellos de tiempo a documentos. Cuando
se define, no firmará, solo sellará, por lo que no requiere suministrar PIN.

El parámetro `-visible-timestamp` es similar a `-timestamp` pero muestra una
representación visual dentro de los documentos PDF en la esquina superior
izquierda de la primera página.

Es posible utilizar un tercer parámetro en la interfaz args para firmar con
un fichero de almacén de certificados. Se puede utilizar de la siguiente forma:

    echo contraseña | java -jar firmador.jar -dargs original.pdf firmado.pdf almacen.p12

En el caso de ejecutarse en Windows, no debe dejar espacio antes del símbolo
`|` o no reconocerá la contraseña como válida.
Ejemplo: `echo contraseña| java(...)`


## ¿Cómo integrar firmador en un sitio web para que se lance la app, cargue un documento en la app y suba el documento firmado automáticamente?

Para poder utilizar esta funcionalidad, requerirá tener instalado primero el
firmador. En el caso de Windows, instalado desde el instalador para Windows.
En el caso de macOS, moviendo la aplicación Firmador desde la carpeta donde se
descargó a otra carpeta, por ejemplo Aplicaciones u otra carpeta, este
movimiento entre ubicaciones registra los eventos a los que atiende la
aplicación. Para el caso de Linux, se puede leer en otra pregunta más abajo en
este mismo documento.

Puede accederse a una demostración de firma web en la siguiente dirección:
- https://firmador.libre.cr/demo-firma-web/

En esta ruta resultan útiles el código fuente de index.html y de él demo.js y
firmador.js. Por conveniencia, se enlazan desde aquí también:
- https://firmador.libre.cr/demo-firma-web/demo.js
- https://firmador.libre.cr/demo-firma-web/firmador.js

El instalador de la aplicación del firmador registra un protocolo
personalizado, `firmador:` que permite cargar el firmador cuando se hace clic
en un enlace de un sitio web. En este modo, el firmador se abre en un "modo
remoto", donde un servicio local escuchando por defecto en
`http://localhost:3516` atenderá a consultas únicamente desde la máquina local
(binding restringido). El navegador, mediante JavaScript y su `XMLHttpRequest`
o su equivalente más moderno `fetch`, tratarán de conectar al servidor local.
Para prevenir que otros sitios web maliciosos, por ejemplo abiertos en otras
pestañas, puedan enviar documentos al firmador que está abierto en local,
deberá pasarse el protocolo y dominio del sitio web como parámetro del
protocolo `firmador:`. Por ejemplo, en la demo del sitio web genera
internamente `firmador:https://firmador.libre.cr`. Otros scripts desde otros
dominios no podrán ni siquiera saber si el firmador está escuchando en ese
puerto, evitando escaneos no autorizados, gracias a la protección CORS del
navegador web. La obtención del dominio que usa el sitio web es automática. El
fichero firmador.js conforma la URI del protocolo personalizado así:
```
"firmador:" + window.location.protocol + "//" + window.location.host;
```
De esta manera también se previene que haya otros sitios web que intenten
cargar documentos para firmar sin que el usuario se dé cuenta.

Los navegadores puede comunicarse con servicios locales que estén en la
interfaz loopback o en localhost sin que se bloquee la comunicación por
contenido mixto (entre HTTPS y HTTP) al considerarse en un estándar W3C como
zona segura. Para que este mecanismo evite que sitios web maliciosos examinen
servicios locales no autorizados, este tipo de servicios, como es el caso de
Firmador, usan explícitamente el encabezado `Access-Control-Allow-Origin`.
Firefox, Edge y Chrome lo soportan, con la excepción notable de Safari, que
todavía no soporta esta implementación por HTTPS pero está siendo desarrollada.

El servicio HTTP de Firmador utiliza `Vary: Origin` para prevenir que el
navegador no memorice el valor del encabezado `Orign` para así evitar
reutilizarlo de manera cacheada y forzar a utilizar el de las nuevas consultas.


## ¿Por qué Firmador utiliza el puerto 3516 para el mecanismo de firma remota?

Porque ese número de puerto está registrado en IANA con el nombre
[smartcard-port](https://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xhtml?search=smartcard-port)
y descrito como "Smartcard Port", por lo que por contexto resulta conveniente.
Algunas herramientas como `netstat` o `ss` muestran este nombre que identifica
de forma más intuitiva el tipo de servicio que está funcionando en esa
conexión, facilitando la auditoría de red.

## ¿Es posible utilizar un puerto diferente al 3516 para la firma remota?

Sí, a partir de la versión 1.9.8 se puede mandar el número de puerto al final
de la URL utilizando el símbolo # seguido del número de puerto a la sintaxis de
protocolo personalizado. Esto se puede pasar como quinto parámetro a la función
`firmadorFirmar` del fichero firmador.js. La documentación está en el fichero
demo.js en la ruta indicada algunas preguntas más arriba.

# ¿Cómo registrar el protocolo personalizado `firmador:` en escritorios Linux?

Mientras no existan instaladores de paquete para distribuciones GNU/Linux, se
indica cómo crear un lanzador manualmente. Estas instrucciones son para uso
local, para paquetería se deberán usar rutas equivalentes globales en /usr.

Crear, si no existen, la siguientes rutas:

    mkdir -p $HOME/.local/share/icons/
    mkdir -p $HOME/.local/share/firmador/
    mkdir -p $HOME/.local/share/applications/

Descargar el fichero https://firmador.libre.cr/firmador.svg y colocarlo en
`$HOME/.local/share/icons/`

Descargar el fichero https://firmador.libre.cr/firmador.jar y colocarlo en
`$HOME/.local/share/firmador/`

Crear un fichero `firmador.sh` en `$HOME/.local/share/firmador/` y editarlo con
un editor de texto plano con el siguiente contenido:

```
#!/bin/sh
case $1 in "firmador:"*)
remoteOrigin=${1#"firmador:"}
shift
java -Djnlp.remoteOrigin="$remoteOrigin" -jar .local/share/firmador/firmador.jar "$@"
;;
*)
java -jar .local/share/firmador/firmador.jar "$@"
;;
esac
```

Crear un fichero `firmador.desktop` en `$HOME/.local/share/applications/` y
editarlo con un editor de texto plano con el siguiente contenido:

```
[Desktop Entry]
Type=Application
Name=Firmador
Exec=sh .local/share/firmador/firmador.sh %U
Icon=firmador
Terminal=false
SingleMainWindow=true
MimeType=x-scheme-handler/firmador
```

Refrescar para que aparezca el icono si no lo hace automáticamente:

    update-desktop-database ~/.local/share/applications


## ¿Se pueden firmar varios documentos PDF en lote?

Sí se puede. La firma debe quedar exactamente en el mismo lugar en la misma
página de cada documento.

- Cargar un archivo del lote, para poder previsualizarlo.
- Entrar a la pestaña 'Configuración' y cambiar el campo 'Página inicial' al
  número de página deseado.
- Clic en 'Guardar'.
- Devolverse a la pestaña 'Firmar'.
- Mover la firma al lugar deseado.
- Arrastrar todos los documentos que quiere firmar de esta forma a la ventana
  del Firmador.


## ¿Cómo se pueden detectar anotaciones e imports que no se utilizan?

Se puede ejecutar el compilador de Eclipse para Java (ecj):

```
mvn -Dmaven.compiler.compilerId=eclipse clean package
```

No es necesario instalar el editor de Eclipse para que funcione.

Está configurada una versión antigua que no soporta algunas anotaciones nuevas.


## ¿Cómo se pueden actualizar las dependencias del proyecto?

Ejecutar los siguientes comandos para actualizar el fichero `pom.xml`:

```
mvn -U versions:use-latest-releases
mvn versions:commit
```

Las versiones de los plugins no se actualizan automáticamente. Ejecutar:

```
mvn -U versions:display-plugin-updates
```

y editar manualmente el fichero `pom.xml` según corresponda.


## ¿Cómo se puede saber si una dependencia tiene una vulnerabilidad?

Para generar el reporte, ejecutar:

```
mvn clean verify
```


## ¿Cómo se puede ajustar el nivel de información de depuración mostrada por el firmador mediante línea de comandos?

Se puede utilizar el siguiente comando donde el nivel máximo es `trace`:

    java -jar -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog -Dorg.apache.commons.logging.simplelog.defaultlog=trace -jar firmador.jar

Los valores posibles son: `off`, `error`, `warn`, `info` (predeterminado),
`debug` y `trace`.
