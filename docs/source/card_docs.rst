Documentos y manejo de tarjetas
####################################


Firmador Libre es una aplicación escrita en java y mantiene una estructura de paquetes propia del lenguaje.

Los paquetes que posee son:

Cards
====================

Contiene todo el código necesario para proveer acceso a las tarjetas físicas, así como la representación de tarjetas que se usa para identificar el certificado deseado para la firma de documentos.


* ``CardManager``: es el encargado de detectar si se utiliza PKCS11 o PKCS12, posee el método estático ``getCartdManager`` que retorna el ``CardManagerInterface`` adecuado según el ``CardSignInfo`` proporcionado.
* ``SmartCardDetector``: Monitoriza cambios en los dispositivos físicos, es el encargado de identificar si el usuario tiene una tarjeta o varias conectadas.
* ``CardManagerInterface``: es el encargado de la comunicación con las tarjetas físicas o los keystore de java, se encarga de convertir la información que está en el almacenamiento en ``CardSignInfo``. Posee los métodos:

1. ``getCertificates`` que retorna la lista de certificados disponibles en el almacenamiento físico.
2. ``getKeyStore`` retorna un Keystore de java que contiene el certificado y la llave privada (en el caso de pkcs11 tiene el apuntador a la llave).
3. ``getPrivateKey`` Obtiene la llave privada para firmar.
4. ``getCertificate`` Obtiene el certificado a partir del token (Dispositivo físico) y slot (Posición del certificado en el dispositivo) proporcionados.
5. ``setSerialNumber`` en el caso de PKCS12 indica la ruta donde se debe leer la información del keystore, en PKCS11 no se utiliza.
6. ``loadTokens`` en PKCS12 busca el primer token de un keystore y lo retorna, en PKCS11 no se utiliza.

Posee 2 implementaciones ``PKCS11Manager`` y ``PKCS12Manager``.

Documents
=================

Contiene la representación de un documento para firmador libre, así como los manejadores de previsualización de documentos.

* ``SupportedMimeTypeEnum``: Es un Enum diseñado para representar los formatos soportados por la aplicación.
* ``MimeTypeDetector``: Proporciona el método estático ``detect`` el cual intenta adivinar el MimeType correspondiente al documento, si el mimetype no está soportado, retorna BINARY como mimetype para poder firmar documentos con ASIC-E.
* ``PreviewerManager``: Proporciona el método estático ``getPreviewManager`` el cual según el MimeType proporcionado retorna el manejador de Preview adecuado.  Posee las implementaciones:

1. ``NonPreviewer``: Genera una imagen que indica que la previsualización no está disponible.
2. ``PDFPreviewer``: Previsualizador de documentos PDF (Es el más exacto de todos)
3. ``SofficePreviewer``: Si se tiene instalado libreoffice genera previsualizaciones de los documentos usando esta herramienta, la cual puede dar una idea del documento a firmar pero no es una representación exacta.

* ``Document``: Representa un documento dentro de la aplicación, es el encargado de almacenar la información y las referencias necesarias para manipular el documento. Generalmente las operaciones de firmado, previsualización y validación se realizan en el documento pero llamado desde otro Hilo o Swing Worker.

Constructores
------------------------

1. ``Document(GUIInterface gui, String pathname)``: Constructor para documentos locales. Crea una instancia del documento a partir de una ruta del sistema de archivos.
2. ``Document(GUIInterface gui, byte[] data, String name, int status)``: Constructor para documentos remotos. Crea una instancia a partir de datos en memoria (bytes) con un nombre y estado específico.
3. ``Document(GUIInterface gui, UUID id, String name, String mimetype, String service, int pages, String serial, String origin, String expirationDate, String createdAt)``: Constructor para documentos virtuales. Crea una representación de documento que no requiere el archivo completo en memoria.

Métodos principales
------------------------

1. ``getSettings`` y ``setSettings``: Los settings son los mismos ``Settings`` que se usan a lo largo de la aplicación, pero se almacena una copia de los mismos por cada documento, de forma que se puedan firmar multiples documentos con configuraciones diferentes.
2. ``validate``: Ejecuta el proceso de validación del documento bloqueando el hilo que lo llame. Retorna verdadero si el documento es válido.
3. ``sign``: Firma el documento con el ``CardSignInfo`` proporcionado bloqueando el hilo que lo llame.
4. ``extend``: Extiende la firma de un documento agregando la información de estampa de tiempo necesaria, bloqueando el hilo que lo llame.
5. ``loadPreview``: Genera una previsualización el documento (para documentos locales), bloqueando el hilo que lo llame.
6. ``loadPreviewRemote``: Genera una previsualización a partir de datos en bytes (para documentos remotos), bloqueando el hilo que lo llame.
7. ``setPrincipal``: Puede ser llamado cuando se quiere que el documento sea el documento de trabajo del usuario, llama a las validaciones y al preview si no se tienen en cache.

Métodos de información
------------------------

8. ``getPathName``: Retorna la ruta completa de donde se lee el documento.
9. ``getName``: Retorna solo el nombre del documento.
10. ``getMimeType``: Retorna el MimeType del documento según se haya detectado.
11. ``getExtension``: Retorna la extensión del documento, acá la extensión del documento de entrada puede variar si por ejemplo se firma CAdES o ASIC-E.
12. ``getPathToSave``: Retorna la ruta de guardado del documento.
13. ``getPathToSaveName``: Retorna el nombre del documento a guardar, incluye ya la extensión.
14. ``setPathToSaveName``: Permite guardar el nombre del documento a guardar.
15. ``setPathToSave``: Permite guardar la ruta de guardado del documento.
16. ``getAbsolutePathToSave`` y ``setAbsolutePathToSave``: Maneja la ruta absoluta de guardado del documento.
17. ``getReport``: Retorna el reporte de validación del documento en HTML.
18. ``setReport``: Permite establecer el reporte de validación manualmente.
19. ``getPlainReport``: Retorna el reporte de validación sin los tags de HTML, el cual se usa para llenar los contextos de accesibilidad.
20. ``isValid``: Retorna si el documento tiene firmas válidas, en caso de que no haya sido validado retorna ``False``.
21. ``getPreviewManager``: Retorna el manejador de previsualizaciones para ser utilizado en el panel de firmado.
22. ``setPreview``: Permite establecer un manejador de previsualizaciones personalizado.
23. ``amountOfSignatures``: Retorna la cantidad de firmas que tiene el documento, si no está validado retorna 0.
24. ``getAmountOfSignatures`` y ``setAmountOfSignatures``: Obtiene o establece la cantidad de firmas del documento.

Métodos de documento firmado
--------------------------------

25. ``getSignedDocument``: Retorna el documento firmado o extendido.
26. ``setSignedDocument``: Guarda el documento firmado, los ``signers`` después del proceso de firma actualizan el documento proporcionando el documento ya firmado.

Métodos de estado
------------------------

27. ``getIsReady``: Retorna verdadero si ya el proceso de validación y generación de previsualizaciones terminó.
28. ``getNumberOfPages``: Retorna el número de páginas que posee la previsualización. **Nota:** la previsualización puede ser inexacta y diferenciarse de lo que se vería en un programa de edición de documentos, por lo que se debe tomar la previsualización como una forma de referencia.
29. ``getPages`` y ``setPages``: Obtiene o establece el número de páginas del documento.
30. ``checkIsReady``: Verifica si la validación y el preview ya se realizaron (método privado).
31. ``getStatus`` y ``setStatus``: Maneja el estado del documento. Los estados disponibles son:
    - ``STATUS_TOSIGN`` (0): Documento pendiente de firma
    - ``STATUS_SIGNED`` (1): Documento firmado
    - ``STATUS_ERROR_SIGNING`` (2): Error al firmar el documento
32. ``getSignwithErrors`` y ``setSignwithErrors``: Indica si el documento fue firmado con errores.
33. ``getDocumentIsValidate``: Indica si el documento ya ha sido validado.
34. ``getValidating`` y ``setValidating``: Indica si el documento está en proceso de validación.

Métodos para firma remota
--------------------------------

35. ``getIsremote``: Indica si el documento es remoto (proviene de un servicio web).
36. ``isVirtual``: Indica si el documento es virtual (solo metadatos, sin contenido completo).
37. ``getTobeSignedRemote``: Retorna un FirmadorRemoteDocument para completar procesos de firmado.
38. ``getRemoteDocument``: Retorna una representación remota del documento para ser enviada a servicios de firma.
39. ``signRemoteDocument``: Firma un documento remoto usando la firma proporcionada.
40. ``getRemoteParameters`` y ``setRemoteParameters``: Maneja los parámetros de firma remota.

Métodos de configuración de firmadores
------------------------------------------

41. ``forcesignASiC``: Fuerza a utilizar el ``FirmadorASIC`` en lugar del detectado según el tipo de documento.
42. ``forceCades``: Fuerza a utilizar el ``FirmadorCAdES`` en lugar del detectado según el tipo de documento.
43. ``getSigner`` y ``setSigner``: Obtiene o establece el firmador de documentos a utilizar.
44. ``getValidator``: Retorna el validador asociado al documento.
45. ``setMimeType``: Permite establecer manualmente el tipo MIME del documento.

Métodos de identificación
--------------------------------

46. ``getDocumentID`` / ``getId``: Retorna el UUID único del documento.
47. ``getSerial``: Retorna el número de serie asociado al documento.
48. ``getService`` y ``setService``: Retorna el nombre del servicio del que fue enviado el documento virtual

Métodos de visualización
--------------------------------

49. ``getShowPreview`` y ``setShowPreview``: Controla si se debe mostrar la previsualización del documento.
50. ``getUsedCard``: Retorna la información de la tarjeta usada para firmar el documento.

Métodos de firma masiva
--------------------------------

52. ``isIsmasivesign`` y ``setIsmasivesign``: Indica si el documento forma parte de un proceso de firma masiva.

Métodos de metadatos adicionales
------------------------------------

53. ``getOrigin`` y ``setOrigin``: Maneja el nombre del origen del que proviene el documento virtual.
54. ``getExpirationDate`` y ``setExpirationDate``: Maneja la fecha de expiración del documento.
55. ``getCreatedAt`` y ``setCreatedAt``: Maneja la fecha de creación del documento.

Métodos de interfaz
--------------------------------

56. ``getGUI``: Retorna la interfaz gráfica asociada al documento.

Eventos del documento
------------------------

El documento proporciona varios eventos que pueden ser escuchados por cualquier clase que lo requiera, para escucharlos se debe registrar usando el método ``registerListener`` y ser una clase que implemente ``DocumentChangeListener``.

``DocumentChangeListener`` proporciona los siguientes métodos:

1. ``previewDone``: Se llama cuando la previsualización ha sido cargada en memoria.
2. ``validateDone``: Se llama cuando la validación se ha completado.
3. ``signDone``: Se llama cuando la firma de un documento se ha completado.
4. ``extendsDone``: Se llama cuando el documento ha sido extendido.

Es importante mencionar que todos los métodos pasan como parámetro el documento que generó el evento, además que el orden de llamado no es necesariamente ``signDone`` y luego ``extendDone`` si no que podría darse que la extensión del documento se llame primero.
Adicionalmente indicar que se realiza por documento, así que se llama multiples veces cuando se realizan firmas multiples.
