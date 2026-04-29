API para acceso remoto
=======================

Firmador Libre posee un pequeño servidor web que permite registrar documentos en la aplicación de forma remota. Para eso se dispone de la siguiente API:

Creación de documento
---------------------

Registra un documento para firmar.

.. code:: html

    POST /create/<nombre_de_archivo>

- El cuerpo de la petición debe ser el archivo binario (content-type: application/octet-stream).
- Para cargar varios documentos a la vez, usar `multipart/form-data` y cada campo debe tener el nombre del documento.
- Si es multipart, se ignora el nombre de la URL y se toma el nombre del campo.
- Retorna HTTP 200 si el documento fue recibido correctamente.

Detalle y descarga de documento
-------------------------------

Permite descargar el documento firmado, usando el mismo nombre utilizado en la carga.

.. code:: html

    GET /detail/<nombre_de_archivo>

- Devuelve HTTP 204 si el documento aún no está firmado.
- Devuelve HTTP 200 y el archivo firmado si ya está disponible.
- Devuelve HTTP 404 si no existe el documento.

Eliminar documento
------------------

Elimina el documento cargado en el servidor.

.. code:: html

    DELETE /<nombre_de_archivo>

- Devuelve HTTP 200 si fue eliminado.
- Devuelve HTTP 404 si no existe.

Limpiar lista de documentos
---------------------------

Elimina todos los documentos en la lista del servidor.

.. code:: html

    GET /clean

Cerrar la aplicación cliente
----------------------------

Permite cerrar la aplicación, útil para integración con applets o tests automáticos.

.. code:: html

    GET /close

Firmar un documento
-------------------

Solicita la firma de un documento de forma remota utilizando un dispositivo conectado.

.. code:: html

    POST /sign

- El cuerpo debe ser un JSON con la información del documento y el número de serie del certificado a usar.
- El servidor solicita el PIN al usuario.
- Respuesta: JSON con la firma en base64 y metadatos del proceso.
- Ejemplo de cuerpo (simplificado):

.. code-block:: json

    {
        "documentName": "test.pdf",
        "documentid": "91e88f73-7862-40a6-98df-4a7ac69bcda2",
        "serialnumber": "446022984826570188563016292465943406351728476",
        "tobesigned": {"bytes": "<base64>"},
        "hostname": "firmador",
        "certificate": "MIIFuz..."
    }

Firma múltiple de documentos
----------------------------

Permite firmar varios documentos en una sola operación.

.. code:: html

    POST /multipleSign

.. code-block:: json

    [
        {
            "documentName": "test.pdf",
            "documentid": "91e88f73-7862-40a6-98df-4a7ac69bcda2",
            "serialnumber": "446022984826570188563016292465943406351728476",
            "tobesigned": {"bytes": "<base64>"},
            "hostname": "firmador",
            "certificate": "MIIFuz..."
        }
    ]

- El cuerpo debe ser un array JSON con la información de cada documento.
- Respuesta: Array de firmas para cada documento.

Obtener certificados disponibles
-------------------------------

Devuelve una lista de las tarjetas/certificados disponibles.

.. code:: html

    GET /certificates

- Respuesta: JSON con información de cada certificado conectado.

Autenticación remota
--------------------

Realiza una autenticación remota usando la tarjeta.

.. code:: html

    POST /authenticate

- El cuerpo debe ser un JSON con los datos de autenticación requeridos.
- Respuesta: JSON con el resultado.

Registro/autorización de host
-----------------------------

Permite registrar (autorizar) un nuevo origen (host) para poder interactuar con la API.

.. code:: html

    GET /doRegister

- Si el origen no está autorizado, el usuario deberá autorizarlo manualmente en la interfaz.
- Puede ser autorizado de forma permanente o temporal.

Control de CORS y orígenes permitidos
-------------------------------------

- Todos los endpoints validan el header `Origin`.
- Solo orígenes autorizados pueden interactuar con la API. Si el origen no está permitido, la respuesta será 403 y el error aparecerá en la interfaz gráfica.

Manejo de errores
-----------------

- Todos los endpoints pueden devolver códigos de error HTTP (`400`, `403`, `404`, `406`, `500`).
- Los errores se notifican automáticamente en el panel de conexiones.

