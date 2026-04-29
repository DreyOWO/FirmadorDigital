/* Firmador is a program to sign documents using AdES standards.

Copyright (C) Firmador authors.

This file is part of Firmador.

Firmador is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Firmador is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Firmador.  If not, see <http://www.gnu.org/licenses/>.  */

package cr.libre.firmador.connections;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.Settings;
import cr.libre.firmador.SettingsManager;
import cr.libre.firmador.cards.CardSignInfo;
import cr.libre.firmador.cards.SmartCardDetector;
import cr.libre.firmador.documents.Document;
import cr.libre.firmador.gui.GUIInterface;

import cr.libre.firmador.gui.GUISwing;
import cr.libre.firmador.remote.FirmadorRemoteDocument;
import eu.europa.esig.dss.ws.dto.ToBeSignedDTO;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.List;

import sun.security.pkcs11.wrapper.PKCS11Exception;

import static cr.libre.firmador.connections.ConnectionUtils.getIdentification;

public class RemoteIntegration<T, V> extends SwingWorker<T, V> {
    static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    //private String baseUrl;
    //private Map<String, String> params;
    protected GUIInterface gui;
    private Connection connection;
    private Speaker speaker;

    public RemoteIntegration(GUIInterface gui, Connection connection) {
        super();
        this.gui = gui;
        this.connection = connection;
    }

    public void stop() {
        if (speaker != null)
            speaker.cancel();
        assert speaker != null;
        this.cancel(true);
        speaker.close_session();
    }

    @Override
    protected T doInBackground() throws Exception {
        try {
            this.speaker = new Speaker(gui, connection);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        speaker.start();
        return null;
    }

    static class Speaker {

        private volatile boolean cancelled = false;
        protected GUIInterface gui;
        private Connection connection;
        private SmartCardDetector smartCardDetector;
        private String User_Agent = "HttpClient (lang=Java; os=linux; version=2.0)";
        private final List<String> errorList = new ArrayList<>();
        private SSLContext sslContext;

        public Speaker(GUIInterface gui, Connection connection) throws Throwable {
            super();
            this.gui = gui;
            smartCardDetector = new SmartCardDetector();
            this.connection = connection;
        }

        public void cancel() {
            LOG.info("Cancelando");
            this.cancelled = true;
        }

        public Map<String, Object> start() throws Exception {
            LOG.info("Iniciando servicio de UCR");
            Map<String, Object> negotiationData = requestStartNegotiation(this.connection.getNegotiationUrl(true));
            ((GUISwing) gui).getListDocumentPanel().reloadView();
            @SuppressWarnings("unused")
            Map<String, Object> response = startComunication(negotiationData); //FIXME is response actually used?
            return null;
        }

        public Map<String, Object> requestStartNegotiation(String negotiationUrlComplete) throws Exception {
            try {
                HttpGet request = new HttpGet(negotiationUrlComplete);
                request.setHeader(HttpHeaders.USER_AGENT, this.User_Agent);

                this.sslContext = ConnectionManager.configureTrustStore(this.connection);

                PoolingHttpClientConnectionManager cm = ConnectionManager.buildConnectionManager(this.sslContext);

                try (CloseableHttpClient client = HttpClients.custom()
                        .setConnectionManager(cm)
                        .build()) {

                    return client.execute(request, response -> {
                        int status = response.getCode();
                        ObjectMapper mapper = new ObjectMapper();
                        if (status != 201) {
                            String reason = response.getReasonPhrase();
                            LOG.error("Negotiation unsuccessful: " + reason);
                            return null;
                        }
                        return mapper.readValue(response.getEntity().getContent(),
                                new TypeReference<Map<String, Object>>() {
                                });
                    });
                } catch (Throwable e) {
                    errorList.add(MessageUtils.t("ucr_integration_not_connected") + " Code:20");
                    notifyErrorConnection();
                    LOG.error("Error in requestStartNegotiation", e);
                    throw e;
                }
            } catch (Throwable e) {
                errorList.add(MessageUtils.t("ucr_internal_error") + " Code:21");
                notifyErrorConnection();
                LOG.error("Error in requestStartNegotiation", e);
                throw e;
            }
        }

        public Map<String, Object> startComunication(Map<String, Object> data) throws IOException {
            Map<String, Object> response = null;
            try {
                response = _startComunication(data);
            } catch (Throwable e) {
                if (!this.cancelled) {
                    // Detectar error de integridad PKCS12
                    if (KeystoreManager.isPKCS12IntegrityError(e)) {
                        LOG.error("Error de integridad PKCS12 detectado. Eliminando keystore corrupto...", e);

                        // Obtener el directorio del keystore (ajusta según tu configuración)
                        Path keystoreDir = SettingsManager.getInstance().getConfigDir();

                        if (KeystoreManager.deleteCorruptedKeystore(keystoreDir)) {
                            LOG.info("Keystore corrupto eliminado. Por favor, reinicie la aplicación.");

                        } else {
                            LOG.error("No se pudo eliminar el keystore corrupto");
                        }
                    } else {
                        LOG.error("Error al conectar con el servidor code:16", e);
                    }
                } else {
                    LOG.debug("Conexión cerrada por stop(), ignorando excepción: {}", e.toString());
                }
            }
            return response;
        }

        private Map<String, Object> _startComunication(Map<String, Object> data) throws Throwable {
            try {
                smartCardDetector = new SmartCardDetector();
                Map<String, X509Certificate> certs = smartCardDetector.getAuthenticationAndSignCertificates();
                if (certs.isEmpty()) {
                    errorList.add(
                            "No se encontraron certificados de firma, por favor asegurese de que su tarjeta de firma este conectada");
                    notifyErrorConnection();
                    return null;
                }
                String certAutenticacion = pemToBase64(certs.get("authentication"));
                String certFirmante = pemToBase64(certs.get("sign"));
                String hostname = java.net.InetAddress.getLocalHost().getHostName();
                String connection_token = (String) data.get("connection_token");
                String url = this.connection.getBaseUrl() + data.get("url");
                Map<String, Object> body = new HashMap<>();
                body.put("cert_auth", certAutenticacion);
                body.put("cert_sign", certFirmante);
                body.put("host_name", hostname);
                ObjectMapper mapper = new ObjectMapper();
                String jsonBody = mapper.writeValueAsString(body);
                HttpPost httpPost = new HttpPost(url);
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("X-Connection-Token", connection_token);
                httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

                PoolingHttpClientConnectionManager cm = ConnectionManager.buildConnectionManager(this.sslContext);
                try (CloseableHttpClient client = HttpClients.custom()
                        .setConnectionManager(cm)
                        .build();
                        ClassicHttpResponse response = client.executeOpen(null, httpPost, null)) {
                    int status = response.getCode();
                    mapper = new ObjectMapper();
                    if (status != 201) {
                        String responseBody = EntityUtils.toString(response.getEntity());
                        //String reason = response.getReasonPhrase();
                        if (status == 403) {
                            LOG.error("User not authorized: " + responseBody);
                            showAlert(
                                    "Atención: usted no se encuentra registrado en los servicios de firma digital de la UCR",
                                    "Acceso denegado");
                            return null;
                        }
                        LOG.error("Start Comunication unsuccessful: " + responseBody);
                        return null;
                    } else {
                        OpenBrowser(connection.getLoginUrl(true));
                        String responseBodyString = EntityUtils.toString(response.getEntity());
                        Map<String, Object> responseBody = mapper.readValue(responseBodyString,
                                new TypeReference<Map<String, Object>>() {
                                });
                        String sseUrl = (String) responseBody.get("sse_url");
                        String firmadorId = (String) responseBody.get("firmador_id");
                        String alias = (String) responseBody.get("alias");
                        try {
                            SettingsManager settingsManager = SettingsManager.getInstance();
                            KeystoreManager.saveToken(settingsManager.getConfigDir(), alias, "firmador_id", firmadorId);
                        } catch (Exception e) {
                            LOG.error("Error al guardar los tokens", e);
                            throw new RuntimeException(e);
                        }
                        HttpGet request = new HttpGet(sseUrl);
                        try (CloseableHttpClient clientBody = HttpClients.custom()
                                .setConnectionManager(cm)
                                .build()) {

                            HttpGet requestBody = new HttpGet(sseUrl);
                            RequestConfig requestConfig = RequestConfig.custom()
                                    .setConnectionRequestTimeout(Timeout.ofSeconds(30))
                                    .setResponseTimeout(Timeout.DISABLED) // SSE necesita conexión infinita
                                    .build();
                            request.setConfig(requestConfig);
                            clientBody.execute(requestBody, responseStrem -> {
                                int statusBody = responseStrem.getCode();
                                if (statusBody != 200) {

                                    String responseBodyStrem = EntityUtils.toString(responseStrem.getEntity());
                                    LOG.error("SSE connection failed: " + responseBodyStrem);
                                    return null;
                                } else {
                                    try {
                                        listenToServerSentEvents(responseStrem);
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                    return null;
                                }
                            });
                        } catch (Exception e) {
                            LOG.error("Error al conectar con el servidor code:18", e);
                            throw e;
                        }
                        return mapper.readValue(response.getEntity().getContent(),
                                new TypeReference<Map<String, Object>>() {
                                });
                    }
                }
            } catch (PKCS11Exception e) {
                LOG.error("Error al conectar con el servidor code:20", e);
                errorList.add(MessageUtils.t("gaudi_pkcs11_error") + " Code:20");
                throw new RuntimeException();
            } catch (Exception e) {
                LOG.error("Error al conectar con el servidor code:19", e);
                errorList.add(MessageUtils.t("gaudi_integration_internal_error") + " Code:19");
                throw new RuntimeException(e);
            }
        }

        private void listenToServerSentEvents(ClassicHttpResponse response) throws Exception {
            try (InputStream inputStream = response.getEntity().getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                String line;

                while (!this.cancelled && (line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("data: ")) {
                        String text = line.substring(6);
                        if (!"{}".equals(text)) {
                            processEvent(text);
                        }
                    }
                }
                if (!this.cancelled) {
                    errorList.add(MessageUtils.t("ucr_integration_lost_connection"));
                    notifyErrorConnection();
                    connection.stop(gui);
                    ((GUISwing) gui).getConnectionPanel().refreshConnectionDetails(connection);
                }
            } catch (Exception e) {
                if (!this.cancelled) {
                    errorList.add(MessageUtils.t("gaudi_integration_internal_error") + " Code:13");
                    LOG.error("Error en SSE code:13", e);
                    notifyErrorConnection();
                    connection.stop(gui);
                    ((GUISwing) gui).getConnectionPanel().refreshConnectionDetails(connection);
                    throw e;
                } else {
                    LOG.debug("SSE cerrado por stop(), ignorando excepción: {}", e.toString());
                }
            }
        }

        private void OpenBrowser(String url) throws IOException {
            SettingsManager settingsManager = SettingsManager.getInstance();
            Settings settings = settingsManager.getSettings();
            String preferredBrowser = settings.preferredBrowser;

            try {
                if (preferredBrowser != null && !preferredBrowser.isEmpty()) {
                    Runtime.getRuntime().exec(new String[] { preferredBrowser, url });
                } else {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.BROWSE)) {
                        desktop.browse(new URI(url));
                    } else {
                        LOG.error("No se puede abrir el navegador porque no se soporta la acción de BROWSE");
                    }
                }
            } catch (Exception e) {
                LOG.error("Error al abrir el navegador", e);
                throw new IOException("No se pudo abrir el navegador", e);
            }
        }
/* FIXME private and unused locally
        private boolean isConnectionReset(Throwable e) {
            while (e != null) {
                if (e instanceof java.net.SocketException) {
                    String msg = e.getMessage();
                    if (msg != null && msg.toLowerCase().contains("connection reset")) {
                        return true;
                    }
                }
                e = e.getCause();
            }
            return false;
        }
*/
        @SuppressWarnings("unchecked")
        public void processEvent(String message) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                Map<String, Object> data = mapper.readValue(message, Map.class);
                if (data.containsKey("accion") && "firmar".equals(data.get("accion"))) {
                    if (!sign(data)) {
                        errorList.add(MessageUtils.t("gaudi_integration_not_signed"));
                        notifyErrorConnection();
                    }
                }
                if (data.containsKey("accion") && "alert".equals(data.get("accion"))) {
                    Object rawMessage = data.get("message");
                    if (rawMessage instanceof Map) {
                        Map<String, Object> alert = (Map<String, Object>) rawMessage;
                        if (alert.containsKey("event")) {
                            String event = String.valueOf(alert.get("event"));
                            if ("expired".equals(event)) {
                                String id = String.valueOf(alert.get("id"));
                                ((GUISwing) gui).showErrorVirtual(id);
                            }
                            if ("desactivate".equals(event)) {
                                LOG.info("Desactivando evento");
                                ((GUISwing) gui).desativateLoadDialog();
                                showAlert((String) alert.get("message"), "Alerta");
                            }
                        }
                    } else if (rawMessage != null) {
                        String alertMessage = rawMessage.toString();
                        showAlert(alertMessage, "Alerta");
                    }
                }
                if (data.containsKey("accion") && "load".equals(data.get("accion"))) {
                    LOG.info("Cargando documentos");
                    load(data);
                }
                if (data.containsKey("accion") && "notification".equals(data.get("accion"))) {
                    String notification = data.get("message").toString();
                    showNotification(notification, "Notificación");
                }
                if (data.containsKey("accion") && "login".equals(data.get("accion"))) {
                    complete_login((Map<String, Object>) data.get("login_info"));
                }
                if (data.containsKey("accion") && "validation".equals(data.get("accion"))) {
                    String report = data.get("report").toString();
                    UUID document_id = UUID.fromString(data.get("documentid").toString());
                    ((GUISwing) gui).notifyReportDocument(document_id, report);
                }
                if (data.containsKey("accion") && "cancelled".equals(data.get("accion"))) {
                    LOG.info("Cancelando documentos");
                    UUID document_id = UUID.fromString(data.get("documentid").toString());
                    ((GUISwing) gui).cancelDocument(document_id);
                }
            } catch (Exception e) {
                errorList.add(MessageUtils.t("gaudi_integration_internal_error") + " Code:11");
                LOG.error("Error al procesar evento CODE:11", e);
                notifyErrorConnection();
            } catch (Throwable e) {
                errorList.add(MessageUtils.t("gaudi_integration_internal_error") + " Code:12");
                LOG.error("Error al procesar evento CODE:12", e);
                notifyErrorConnection();
                throw new RuntimeException(e);
            }
        }

        private void complete_login(Map<String, Object> data) throws IOException {
            LOG.info("Guardando Tokens");
            String access_token = (String) data.get("access_token");
            String refresh_token = (String) data.get("refresh_token");
            String id_token = (String) data.get("id_token");
            String alias = (String) data.get("alias");
            String user = (String) data.get("user_logged");
            try {
                SettingsManager settingsManager = SettingsManager.getInstance();
                KeystoreManager.saveToken(settingsManager.getConfigDir(), alias, "access", access_token);
                KeystoreManager.saveToken(settingsManager.getConfigDir(), alias, "refresh", refresh_token);
                KeystoreManager.saveToken(settingsManager.getConfigDir(), alias, "id", id_token);
                connection.setLogged(true);
                connection.setUserLogged(user);
                ((GUISwing) gui).notifyLoginComplete(connection);
            } catch (Exception e) {
                LOG.error("Error al guardar los tokens", e);
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("unchecked")
        private void load(Map<String, Object> data) throws IOException {
            //ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> mList = (List<Map<String, Object>>) data.get("documents");
            List<Document> docs = new ArrayList<>();
            for (Map<String, Object> m : mList) {
                Document docrequest = new Document(
                        gui,
                        UUID.fromString(m.get("documentid").toString()),
                        m.get("documentName").toString(),
                        m.get("mimetype").toString(),
                        connection.getService(),
                        Integer.parseInt(m.get("pages").toString()),
                        String.valueOf(m.get("serial")),
                        String.valueOf(m.get("origin")),
                        String.valueOf(m.get("expirationDate")),
                        String.valueOf(m.get("createdAt")));
                docs.add(docrequest);
            }
            ((GUISwing) gui).loadVirtualDocument(docs);
        }

        @SuppressWarnings("unchecked")
        private Boolean sign(Map<String, Object> data) throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> mList = (List<Map<String, Object>>) data.get("documents");
            List<FirmadorRemoteDocument> docs = new ArrayList<>();
            for (Map<String, Object> m : mList) {
                Map<String, Object> toBeSignedMap = (Map<String, Object>) m.get("tobesigned");
                String toBeSignedStr = (String) toBeSignedMap.get("bytes");
                byte[] toBeSignedBytes = Base64.getDecoder().decode(toBeSignedStr);
                ToBeSignedDTO tobesigned = new ToBeSignedDTO();
                tobesigned.setBytes(toBeSignedBytes);
                FirmadorRemoteDocument remoteDoc = mapper.convertValue(m, FirmadorRemoteDocument.class);
                docs.add(remoteDoc);
            }
            ((GUISwing) gui).loadRemoteDocumentInList(docs, connection.getService());
            return true;
        }

        public KeyStore loadCABundle(InputStream is) throws Exception {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null); // Inicializar KeyStore vacío

            Collection<? extends Certificate> certs = (Collection<? extends Certificate>) cf.generateCertificates(is);
            int i = 1;
            for (Certificate cert : certs) {
                trustStore.setCertificateEntry("ca" + i, cert);
                i++;
            }
            return trustStore;
        }

        public String pemToBase64(X509Certificate cert) throws Exception {
            return Base64.getEncoder().encodeToString(cert.getEncoded());
        }

        private void notifyErrorConnection() {
            SwingUtilities.invokeLater(() -> {
                ((GUISwing) gui).getConnectionPanel().updateErrors(new ArrayList<>(errorList), connection);
                LOG.error("Error al conectar con el servidor code:17");
                errorList.clear();
            });
        }

        private void close_session() {
            try {
                List<CardSignInfo> cards = smartCardDetector.readListSmartCard();
                if (cards.isEmpty())
                    return;
                CardSignInfo cardInfo = cards.get(0);
                String identification = getIdentification(cardInfo.getIdentification());
                HttpPost request = new HttpPost(this.connection.getEndSessionUrl(true));
                String firmadorId = KeystoreManager.loadToken(SettingsManager.getInstance().getConfigDir(),
                        identification + connection.getService(), "firmador_id");
                String json = "{ \"firmador_id\": \"" + firmadorId + "\" }";
                request.setHeader(HttpHeaders.USER_AGENT, this.User_Agent);
                request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
                LOG.info("Estado de la conexion: " + connection.isLogged());
                if (connection.isLogged()) {
                    String token = KeystoreManager.loadToken(SettingsManager.getInstance().getConfigDir(),
                            identification + connection.getService(), "access");
                    String id_token = KeystoreManager.loadToken(SettingsManager.getInstance().getConfigDir(),
                            identification + connection.getService(), "id");
                    request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                    json = "{ \"id_token_hint\": \"" + id_token + "\", \"firmador_id\": \"" + firmadorId + "\" }";
                }
                request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));
                PoolingHttpClientConnectionManager cm = ConnectionManager.buildConnectionManager(this.sslContext);
                try (CloseableHttpClient client = HttpClients.custom()
                        .setConnectionManager(cm)
                        .build();
                        ClassicHttpResponse apiResponse = client.executeOpen(null, request, null)) {

                    int statusCode = apiResponse.getCode();
                    String responseText = EntityUtils.toString(apiResponse.getEntity());

                    System.out.println("API status: " + statusCode);
                    System.out.println("API response: " + responseText);

                }
                connection.setLogged(false);
            } catch (Throwable e) {
                LOG.error("Error al cerrar sesión", e);
                throw new RuntimeException(e);
            }
        }
/* FIXME private and unused locally
        private void restartConnection() {
            SwingUtilities.invokeLater(() -> {
                ((GUISwing) gui).getConnectionPanel().restartConnection(connection);
            });
        }
*/
        private void showNotification(String message, String title) {
            ((GUISwing) gui).cleanVirtualDocumentsInList();
            ((GUISwing) gui).showNotification(message, GUISwing.NotificationType.SUCCESS);
        }

        private void showAlert(String message, String title) {
            ((GUISwing) gui).showNotification(message, GUISwing.NotificationType.WARNING);
        }
    }
}
