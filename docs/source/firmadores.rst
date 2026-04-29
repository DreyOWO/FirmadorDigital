Firmadores
==================

Son los elementos que permiten firmar los documentos, existe uno por cada tipo de documento soportado.

``DocumentSigner`` es la interfaz que proporciona las funcionalidades que tiene que tener un Firmador. Sus métodos son:

1. ``void setGui(GUIInterface gui)``: Asigna la interfaz a utilizar para enviar mensajes de error o mensajes para el usuario.
2. ``void setSettings(Settings settings)``: Asigna las settings del documento a utilizar.
3. ``DSSDocument sign(Document toSignDocument, CardSignInfo card)``: Firma el documento con la ``CardSignInfo`` proporcionada de donde puede sacar el certificado y la llave privada.
4. ``DSSDocument extend(DSSDocument document)``: Extiende el documento agregando una estampa de tiempo.
5. ``void setDetached(List<DSSDocument> detacheddocs)``:  Cuando se tienen firmas CAdES o ASIC se puede agregar los documentos que se quieran incorporar al contenedor o al validador, osea son los documentos fuente.

Existen las siguientes implementaciones: 

* FirmadorPAdES
* FirmadorOpenDocument
* FirmadorCAdES
* FirmadorOpenXmlFormat
* FirmadorASiC
* FirmadorXAdES
* FirmadorJAdES
* FirmadorVirtual


``CRSigner`` Es una clase de la cual heredan todas las implementaciones ya que proporciona implementación de varios métodos comunes.

1. ``void setGui(GUIInterface gui)``: Asigna la interfaz a utilizar para enviar mensajes de error o mensajes para el usuario.
2. ``void setSettings(Settings settings)``: Asigna las settings del documento a utilizar.
3. ``DSSPrivateKeyEntry getPrivateKey(SignatureTokenConnection signingToken)``: A partir de un `SignatureTokenConnection` obtiene la representación de la llave privada para firmar documentos con DSS.
4. ``String getPkcs11Lib``: En los contextos con PKCS11 obtiene la ruta de la biblioteca encargada de conectarse al dispositivo físico.
5. ``CertificateVerifier getCertificateVerifier``: Obtiene el manejador de verificación para DSS utilizando todas las cadenas de certificados disponibles.
6. ``CertificateVerifier getCertificateVerifier(CertificateToken subjectCertificate)``: Obtiene el manejador de verificación para DSS utilizando solamente las cadenas de certificados que tienen relación con el certificado dado.

VirtualSigner
==================

``VirtualSigner`` es una clase especializada que maneja la firma de documentos virtuales (documentos que residen en servicios remotos). A diferencia de los otros firmadores, este no firma documentos locales directamente, sino que coordina el proceso de firma con servicios remotos.

Funcionalidad principal
------------------------

El ``VirtualSigner`` actúa como intermediario entre la aplicación local y los servicios de firma remota, permitiendo:

* Obtener los hash de documentos a firmar desde servicios remotos
* Firmar los hash localmente usando las tarjetas físicas del usuario
* Enviar las firmas de vuelta al servicio remoto para completar el proceso

Métodos principales
--------------------

1. ``getHashToSign(List<Document> toSignDocuments, Settings settings)``: Solicita al servicio remoto los hash de los documentos que se van a firmar. Este método:

   * Obtiene la información de la tarjeta usando el número de serie del documento
   * Prepara el payload con los IDs de documentos y configuraciones
   * Envía la solicitud al servicio remoto usando autenticación Bearer
   * Maneja la renovación de tokens si es necesario
   * Retorna ``true`` si la solicitud fue exitosa

2. ``sign(List<FirmadorRemoteDocument> toSignDocuments, String service)``: Firma los documentos virtuales localmente y envía las firmas al servicio remoto. Este método:

   * Obtiene la tarjeta de firma usando el número de serie
   * Solicita el PIN al usuario mediante ``RequestPinWindowRemote``
   * Firma cada hash usando ``BasicSigner``
   * Serializa las firmas en JSON
   * Envía las firmas al servicio remoto
   * Maneja desconexión automática si el token es inválido (código 403)
   * Retorna ``true`` si todas las firmas fueron completadas exitosamente

3. ``getCardInfoBySerial(String serial)``: Método privado que busca y retorna la información de una tarjeta específica usando su número de serie. Itera sobre todas las tarjetas detectadas hasta encontrar una coincidencia.
