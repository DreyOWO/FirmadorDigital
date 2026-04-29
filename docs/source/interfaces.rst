Interfaces
#################

Firmador libre posee varias interfaces por las que puede ser llamado, todas las interfaces deben heredar de ``GUIInterface`` el cual posee los siguientes métodos:

Métodos de inicialización
---------------------------

1. ``void loadGUI()``: Es el método llamado inmediatamente después de que el programa inicia.
2. ``void setArgs(String[] args)``: Pasa a la interfaz los parámetros con los que firmador libre fue llamado.
3. ``void setPluginManager(PluginManager pluginManager)``: Asigna el plugin manager para uso de la interfaz.
4. ``void configurePluginManager()``: Registra el plugin manager para que la interfaz pueda proporcionarle eventos u obtener eventos del plugin.
5. ``void setSmartCardDetector(SmartCardDetector detector)``: Asigna el detector de tarjetas inteligentes para usarse en la interfaz.

Métodos de mensajes y diálogos
---------------------------------

6. ``void showError(Throwable error)``: Se proporciona un mensaje de error para ser mostrado, almacenado o lo que la interfaz quiera hacer con él, estos mensajes se generan durante el flujo de firmado y no tienen un orden fijo.
7. ``void showMessage(String message)``: Se proporciona un mensaje para ser mostrado al usuario o realizar lo que la interfaz disponga.
8. ``void showErrorAlert(String title, String message)``: Muestra un diálogo de error con un título y mensaje específicos.

Métodos de carga de documentos
---------------------------------

9. ``List<Document> addDocuments(File[] files)``: Se proporciona un array de archivos para ser cargados y retorna la lista de documentos creados.
10. ``void loadDocuments(List<Document> documents, boolean doPreview)``: Carga una lista de documentos previamente creados, con opción de generar vista previa.
11. ``List<Document> addDocumentsSynchronous(File[] files)``: Carga documentos de forma síncrona (bloqueante) y retorna la lista de documentos.
12. ``void addDirectories(File[] files)``: Se utiliza para cargar un array de directorios.
13. ``void loadRemoteDocument(String fileName)``: Se utiliza para cargar un documento remoto.

Métodos de interacción con el usuario
---------------------------------------

14. ``String getDocumentToSign()``: Retorna la ruta completa del documento a firmar.
15. ``String getPathToSave(String extension)``: Retorna la ruta completa donde se debe guardar el documento.
16. ``String getPathToSaveExtended(String extension)``: Retorna la ruta de guardado cuando el documento solo se extendió, cuando se firma el documento se utiliza ``getPathToSave``.
17. ``CardSignInfo getPin()``: Retorna la información del PIN de la tarjeta para usarse a la hora de firmar.

Métodos de control de flujo
-----------------------------

18. ``void displayFunctionality(String functionality)``: En el caso de ``Swing Interface`` permite cambiarse de TAB según el nombre proporcionado.
19. ``void nextStep(String msg)``: Permite indicar un mensaje para el paso que se está dando en el flujo de firmado.
20. ``Settings getCurrentSettings()``: Obtiene los settings que están seleccionados actualmente en la interfaz (no confundir con los settings de toda la aplicación ya que usan la misma clase Settings).

Métodos de operaciones de documentos
--------------------------------------

21. ``void signDocument(Document document)``: Encola un documento para ser firmado con el Agendador de firmado.
22. ``void extendDocument()``: Extiende el documento ya firmado.
23. ``void doPreview(Document document)``: Encola la generación de previsualización de un documento en el Agendador de previsualizaciones.

Eventos de documentos individuales
------------------------------------

Estos métodos son llamados cuando se completa una operación sobre un documento específico:

24. ``void previewDone(Document document)``: Es llamada cuando se termina de cargar la previsualización de un documento.
25. ``void validateDone(Document document)``: Es llamada cuando se termina de validar un documento.
26. ``void signDone(Document document)``: Es llamada cuando se termina de firmar un documento.
27. ``void extendsDone(Document document)``: Es llamada cuando se termina de extender un documento.

Eventos de operaciones masivas
---------------------------------

Estos métodos son llamados cuando se completan operaciones sobre todos los documentos encolados:

28. ``void validateAllDone()``: Es llamada cuando se termina de validar todos los documentos encolados.
29. ``void signAllDone()``: Es llamada cuando se termina de firmar todos los documentos encolados.
30. ``void previewAllDone()``: Es llamada cuando se termina de generar previsualizaciones de todos los documentos encolados.
31. ``void clearDone()``: Es llamada cuando se termina de eliminar un documento.

Implementaciones disponibles
==============================

Existen las siguientes implementaciones de esta interfaz:

* **GUIArgs**: Interfaz de línea de comandos con argumentos
* **GUIShell**: Interfaz de consola interactiva
* **GUISwing**: Interfaz gráfica usando Java Swing

Seleccionando la interfaz
---------------------------

``GUISelector`` es la clase encargada de seleccionar qué interfaz se debe utilizar, para ello proporciona el método ``GUIInterface getInterface(String[] args)`` de forma que pueda ser llamada por el firmador.

Se puede especificar la interfaz a utilizar mediante los comandos:

.. code:: bash

	java -jar firmador.jar -dswing

Las posibles opciones para la línea de comandos son ``args``, ``shell`` y ``swing`` siendo la última la interfaz por defecto.
