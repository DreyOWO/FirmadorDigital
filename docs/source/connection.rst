Conexiones
#################

El sistema de conexiones permite a Firmador Libre integrarse con servicios externos para recibir y procesar documentos virtuales. Este módulo gestiona la comunicación, autenticación y sincronización con servicios remotos de firma digital.

Arquitectura del sistema
=========================

El sistema de conexiones está compuesto por varios componentes que trabajan en conjunto:

* **Connection**: Representa una conexión a un servicio específico
* **ConnectionManager**: Gestiona certificados SSL y configuración de conexiones HTTP
* **ConnectionUtils**: Proporciona utilidades para operaciones comunes (autenticación, obtención de imágenes, eliminación de documentos)
* **KeystoreManager**: Administra el almacenamiento seguro de tokens de autenticación
* **ServicesUrlsIO**: Gestiona la persistencia de configuraciones de servicios
* **Integraciones específicas**: GaudiIntegration, RemoteIntegration (una por cada servicio soportado)

Connection
==========

La clase ``Connection`` representa una conexión individual a un servicio externo y contiene toda la información necesaria para comunicarse con él.

Constructores
-------------

1. ``Connection(String name, String service, Integer port, SwingWorker<?, ?> worker)``: Constructor básico para conexiones simples.
2. ``Connection(String name, String service, Integer port, SwingWorker<?, ?> worker, String baseUrl, String negotiationUrl, String completeUrl, String endSessionUrl, String previewUrl, String signUrl, String deleteUrl, String loginUrl, String validateUrl, String virtualDocumentsUrl)``: Constructor completo con todas las URLs del servicio.
3. ``Connection(String json)``: Constructor que deserializa una conexión desde JSON.

Propiedades principales
-----------------------

* ``name``: Nombre identificador de la conexión
* ``service``: Tipo de servicio ("Firmador Remoto", "Gaudi", o servicios externos)
* ``port``: Puerto de conexión
* ``baseUrl``: URL base del servicio
* ``logged``: Estado de autenticación del usuario
* ``worker``: SwingWorker encargado de la comunicación asíncrona

URLs de endpoints
-----------------

La clase mantiene URLs para diferentes operaciones:

* ``negotiationUrl``: Endpoint para iniciar negociación de conexión
* ``negotiationStartUrl``: Endpoint para comenzar la comunicación
* ``completeUrl``: Endpoint para completar operaciones de firma
* ``endSessionUrl``: Endpoint para cerrar sesión
* ``previewUrl``: Endpoint para obtener previsualizaciones de documentos
* ``signUrl``: Endpoint para solicitar hash a firmar
* ``deleteUrl``: Endpoint para eliminar documentos virtuales
* ``loginUrl``: Endpoint para autenticación
* ``validateUrl``: Endpoint para validar documentos
* ``virtualDocumentsUrl``: Endpoint para listar documentos virtuales

Métodos principales
-------------------

4. ``start(GUIInterface gui, String name)``: Inicia la conexión con el servicio. Crea e inicia el worker apropiado según el tipo de servicio (RemoteHttpWorker para Firmador Remoto, GaudiIntegration para Gaudi, RemoteIntegration para servicios externos).
5. ``stop()``: Detiene la conexión activa y cancela el worker en ejecución.
6. ``isRunning()``: Retorna verdadero si la conexión está activa (el worker está ejecutándose).
7. ``isExternal()``: Retorna verdadero si la conexión es a un servicio externo (no es Firmador Remoto ni Gaudi).
8. ``isLogged()``: Retorna el estado de autenticación del usuario.
9. ``setLogged(Boolean logged)``: Actualiza el estado de autenticación.
10. Métodos ``get*Url(Boolean complete)``: Retornan las URLs de los endpoints. Si ``complete`` es verdadero, incluyen la baseUrl completa; si es falso, retornan solo el path relativo.

Gestión de errores
------------------

11. ``addError(String error)``: Agrega un mensaje de error a la lista de errores de la conexión.
12. ``getErrorList()``: Retorna la lista completa de errores registrados.
13. ``setErrorList(List<String> errorList)``: Establece una nueva lista de errores.

ConnectionManager
=================

Clase utilitaria estática que maneja la configuración de conexiones HTTP seguras y certificados SSL.

Métodos principales
-------------------

1. ``configureTrustStore(Connection connection)``: Configura el contexto SSL para la conexión. Si no existe un truststore local, descarga automáticamente el certificado del servidor y lo almacena en ``truststore.jks``. Retorna un ``SSLContext`` configurado.

2. ``buildConnectionManager(SSLContext sslContext)``: Construye un ``PoolingHttpClientConnectionManager`` configurado con el contexto SSL proporcionado.

Funcionalidad interna
----------------------

* ``extractHost(String url)``: Extrae el hostname de una URL.
* ``downloadAndStoreCertificate(String host, int port, String trustStorePath, String password)``: Descarga el certificado SSL del servidor y lo almacena en el truststore local. Utiliza un TrustManager que acepta todos los certificados para realizar la descarga inicial.

ConnectionUtils
===============

Clase utilitaria con métodos estáticos para operaciones comunes relacionadas con conexiones y documentos virtuales.

Métodos de gestión de conexiones
---------------------------------

1. ``findConnection(String name)``: Busca y retorna una conexión por su nombre de servicio en la lista de conexiones guardadas.
2. ``getIdentification(String identification)``: Procesa y limpia el número de identificación extrayendo la parte relevante (después del guión).

Métodos de operaciones con documentos virtuales
------------------------------------------------

3. ``getPageImageFromApi(Document document, int page, GUIInterface gui)``: Obtiene la imagen de una página específica de un documento virtual desde el servicio remoto.

4. ``deleteDocument(Document document, SmartCardDetector smartCardDetector, GUIInterface gui)``: Elimina un documento virtual del servicio remoto.

5. ``validateVirtualDocument(Document document, SmartCardDetector smartCardDetector, GUIInterface gui)``: Solicita la validación de un documento virtual al servicio remoto.

6. ``reloadVirtualDocuments(Connection connection)``: Solicita al servicio remoto que recargue la lista de documentos virtuales pendientes.

Métodos de autenticación
-------------------------

7. ``getCardInfoByIdentification(String identification, SmartCardDetector smartCardDetector, GUIInterface gui)``: Busca la información de una tarjeta específica por su número de identificación entre todas las tarjetas detectadas.

KeystoreManager
===============

Clase que gestiona el almacenamiento seguro de tokens de autenticación y credenciales en un keystore PKCS12 cifrado.

Métodos principales
-------------------

1. ``saveToken(Path keystoreDir, String alias, String type, String token)``: Guarda un token de forma segura en el keystore.

   * ``alias``: Generalmente es la identificación del usuario + nombre del servicio
   * ``type``: Tipo de token ("access", "refresh", "id", "firmador_id")
   * Crea el keystore si no existe
   * Los tokens se convierten a ``SecretKey`` antes de almacenarse

2. ``loadToken(Path keystoreDir, String alias, String type)``: Recupera un token previamente guardado.


Tipos de tokens almacenados
----------------------------

* **access**: Token de acceso para autenticación Bearer
* **refresh**: Token para renovar el token de acceso
* **id**: Token de identidad del usuario
* **firmador_id**: Identificador único del firmador en el servicio remoto

ServicesUrlsIO
==============

Clase que gestiona la persistencia de configuraciones de servicios en formato XML.

Métodos principales
-------------------

1. ``save(List<Connection> connections)``: Guarda la lista de conexiones en un archivo XML (``servicesUrls.xml``).

   * Solo guarda conexiones externas (no guarda "Firmador Remoto" ni "Gaudi")
   * Cada conexión se serializa con todas sus URLs y propiedades
   * El archivo se guarda en el directorio de configuración de la aplicación

2. ``load()``: Carga la lista de conexiones desde el archivo XML.

   * Retorna una lista vacía si el archivo no existe
   * Parsea el XML y reconstruye los objetos ``Connection``
   * Las conexiones integradas (Firmador Remoto, Gaudi) se crean dinámicamente

Formato del archivo XML
------------------------

.. code-block:: xml

    <servicesUrls>
        <connection name="Mi Servicio">
            <service>Nombre del Servicio</service>
            <baseUrl>https://ejemplo.com</baseUrl>
            <negotiationUrl>/api/negotiate</negotiationUrl>
            <negotiationStartUrl>/api/start</negotiationStartUrl>
            <completeUrl>/api/complete</completeUrl>
            <endSessionUrl>/api/logout</endSessionUrl>
            <previewUrl>/api/preview</previewUrl>
            <signUrl>/api/sign</signUrl>
            <deleteUrl>/api/delete</deleteUrl>
            <loginUrl>/api/login</loginUrl>
            <validateUrl>/api/validate</validateUrl>
            <virtualDocumentsUrl>/api/documents</virtualDocumentsUrl>
        </connection>
    </servicesUrls>

Integraciones específicas
==========================

GaudiIntegration
----------------

Integración con el sistema GAUDI (Gestión Automatizada de Documentos Institucionales) del Banco Central de Costa Rica.

**Características específicas:**

* Usa certificados de autenticación y firma del usuario
* Implementa comunicación mediante Server-Sent Events (SSE)
* Utiliza certificados específicos de la CA Raíz Nacional de Costa Rica
* Solicita PIN y código de acceso al usuario
* Monitorea la presencia física de la tarjeta durante la conexión
* Realiza firma local de hash usando PKCS#11

RemoteIntegration
-----------------

Integración genérica para servicios externos que implementan el protocolo de documentos virtuales de Firmador.

**Características específicas:**

* Soporta autenticación OAuth2 con tokens Bearer
* Comunicación mediante Server-Sent Events (SSE)
* Gestión automática de tokens (access, refresh, id)
* Soporte para login externo mediante navegador
* Manejo de múltiples documentos simultáneos

**Acciones soportadas:**

* ``sign``: Recibe documentos para firmar
* ``load``: Carga lista de documentos virtuales
* ``notification``: Muestra notificaciones al usuario
* ``login``: Completa proceso de autenticación
* ``validation``: Recibe reportes de validación
* ``cancelled``: Notifica cancelación de documentos

Flujo general de una conexión
==============================

1. **Inicialización**:

   * El usuario agrega una nueva conexión en el panel de conexiones
   * Se crea un objeto ``Connection`` con las URLs del servicio
   * Se guarda en ``servicesUrls.xml`` mediante ``ServicesUrlsIO``

2. **Conexión**:

   * Se llama a ``connection.start()``
   * Se crea el worker apropiado (GaudiIntegration o RemoteIntegration)
   * El worker inicia la negociación con el servidor

3. **Autenticación**:

   * GAUDI: Usa certificados de la tarjeta
   * Servicios externos: OAuth2 mediante navegador
   * Los tokens se almacenan en el keystore mediante ``KeystoreManager``

4. **Operación normal**:

   * El servicio envía documentos virtuales mediante SSE
   * Los documentos se cargan en la interfaz
   * El usuario puede visualizar, firmar o eliminar documentos
   * Todas las operaciones usan tokens almacenados para autenticación

5. **Desconexión**:

   * Se llama a ``connection.stop()``
   * Se cierra la sesión en el servidor remoto
   * Se cancela el worker
   * Los tokens permanecen almacenados para futuras conexiones

Manejo de errores
=================

El sistema implementa un manejo robusto de errores:

* **Lista de errores**: Cada conexión mantiene una lista de errores que se muestra en la interfaz
* **Reconexión automática**: Algunos errores desencadenan intentos de reconexión
* **Notificaciones**: Los errores se notifican al usuario mediante la interfaz gráfica
* **Logging**: Todos los errores se registran en los logs con códigos de error específicos
* **Degradación elegante**: Si una operación falla, se notifica pero no se bloquea la aplicación

