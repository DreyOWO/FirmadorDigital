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

import com.fasterxml.jackson.databind.ObjectMapper;
import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.cards.SmartCardDetector;
import cr.libre.firmador.gui.GUIInterface;
import cr.libre.firmador.gui.GUISwing;
import cr.libre.firmador.gui.swing.RequestPinAndCodeWindow;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Signature;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import javax.imageio.ImageIO;
import javax.net.ssl.SSLContext;
import javax.swing.*;
import java.lang.invoke.MethodHandles;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.*;

public class GaudiIntegration<T, V> extends SwingWorker<T, V> {
    static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    //private String baseUrl;
    //private Map<String, String> params;
    protected GUIInterface gui;
    private Connection connection;
    private Speaker speaker;


    public GaudiIntegration(GUIInterface gui, Connection connection) {
        super();
        this.gui = gui;
        this.connection = connection;
    }

    public void stop() {
        if (speaker != null) speaker.cancel();
        this.cancel(true);
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
        private SmartCardDetector smartCardDetector;
        private String BCCR_URL = "https://www.firmadigital.go.cr";
        private String BBCR_START_NEGOTIATION = "/wcfv2/Bccr.Firma.Fva.Hub/signalr/negotiate?clientProtocol=1.4&connectionData=%5B%7B%22name%22%3A%22administradordeclientes%22%7D%5D";
        private String BBCR_CONNECT = "/connect";
        private String User_Agent = "HttpClient (lang=Java; os=linux; version=2.0)";
        //private String code;
        //private String pin;
        private String baseUrl = "";
        Map<String, String> params;
        ImageIcon logoIcon = null;
        String nameOfEntity = "";
        String resumeOfEntity = "";
        private final List<String> errorList = new ArrayList<>();
        private Connection connection;
        //private static final int MAX_RECONNECT_ATTEMPTS = 10;
        //private static final int RECONNECT_DELAY_MS = 5000;

        public Speaker(GUIInterface gui, Connection connection) throws Throwable {
            super();
            this.gui = gui;
            if (smartCardDetector == null)
                smartCardDetector = new SmartCardDetector();
            smartCardDetector.readSaveListSmartCard();
            this.connection = connection;
        }

        public void cancel() {
            LOG.info("Cancelando");
            this.cancelled = true;
        }

        public Map<String, Object> start() throws Exception {
            LOG.info("Iniciando servicio de Gaudi");
            Map<String, Object> negotiationData = requestStartNegotiation(this.BCCR_URL, this.BBCR_START_NEGOTIATION);
            Map<String, Object> response = startComunication(negotiationData);
            LOG.info("Servicio de Gaudi iniciado");
            if (response == null) return null;
            return null;
        }

        public Map<String, Object> startComunication(Map<String, Object> data) {
            Map<String, Object> response = null;
            try {
                response = _startComunication(data);
            } catch (Throwable e) {
                errorList.add(MessageUtils.t("gaudi_integration_internal_error") + " Code:15");
                LOG.error("Error al conectar con el servidor code:15", e);
            }
            return response;
        }

        private Map<String, Object> _startComunication(Map<String, Object> data) throws Throwable {
            try {
                Map<String, X509Certificate> certs = smartCardDetector.getAuthenticationAndSignCertificates();
                if (certs.isEmpty()) {
                    errorList.add(MessageUtils.t("gaudi_integration_not_certificate_detected"));
                    notifyErrorConnection();
                    return null;
                }
                String certAutenticacion = pemToBase64(certs.get("authentication"));
                String certFirmante = pemToBase64(certs.get("sign"));
                String arch = getOSArch();
                String archHeader = arch.contains("64") ? "amd64" : "x86";
                String hostname = java.net.InetAddress.getLocalHost().getHostName();

                Map<String, String> params = new HashMap<>();
                params.put("connectionData", "[{\"name\":\"administradordeclientes\"}]");
                params.put("connectionToken", (String) data.get("ConnectionToken"));
                params.put("connectionId", (String) data.get("ConnectionId"));
                params.put("transport", "serverSentEvents");

                this.params = params;

                this.baseUrl = BCCR_URL + data.get("Url");
                String url = this.baseUrl + BBCR_CONNECT;
                URIBuilder uriBuilder = new URIBuilder(url);
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    uriBuilder.addParameter(entry.getKey(), entry.getValue());
                }

                HttpGet request = new HttpGet(uriBuilder.build());
                setCommonHeaders(request, certAutenticacion, certFirmante, archHeader, hostname);

                InputStream is = this.getClass().getClassLoader().getResourceAsStream("certs/CA RAIZ NACIONAL - COSTA RICA v2.crt");
                if (is == null) {
                    LOG.error("No se pudo cargar el certificado de firma");
                }
                PoolingHttpClientConnectionManager cm = buildConnectionManager();

                try (CloseableHttpClient client = HttpClients.custom()
                    .setConnectionManager(cm)
                    .build()) {

                    return client.execute(request, response -> {
                        int status = response.getCode();
                        ObjectMapper mapper = new ObjectMapper();
                        if (status != 200) {
                            String responseBody = EntityUtils.toString(response.getEntity());
                            /*String reason =*/ response.getReasonPhrase(); //FIXME unused reason phrase for error
                            LOG.error("Start Comunication unsuccessful: " + responseBody);
                            return null;
                        } else {
                            try {
                                startCardMonitor();
                                listenToServerSentEvents(response);
                            } catch (Exception e) {
                                LOG.error("Error al conectar con el servidor code:18", e);
                                throw new RuntimeException(e);
                            }
                            return mapper.readValue(response.getEntity().getContent(), new TypeReference<Map<String, Object>>() {});
                        }
                    });
                } catch (Exception e) {
                    LOG.error("Start Comunication unsuccessful", e);
                    throw e;
                }
            } catch (Exception e) {
                errorList.add(MessageUtils.t("gaudi_integration_internal_error") + " Code:14");
                LOG.error("Error al conectar con el servidor code:14", e);
                notifyErrorConnection();
                throw e;
            }
        }

        public Map<String, Object> requestStartNegotiation(String baseDomain, String urlNegotiation) throws Exception {
            try {
                String url = baseDomain + urlNegotiation;
                HttpGet request = new HttpGet(url);
                request.setHeader(HttpHeaders.USER_AGENT, this.User_Agent);

                PoolingHttpClientConnectionManager cm = buildConnectionManager();

                try (CloseableHttpClient client = HttpClients.custom()
                    .setConnectionManager(cm)
                    .build()) {

                    return client.execute(request, response -> {
                        int status = response.getCode();
                        ObjectMapper mapper = new ObjectMapper();
                        if (status != 200) {
                            String reason = response.getReasonPhrase();
                            LOG.error("Negotiation unsuccessful: " + reason);
                            return null;
                        }
                        return mapper.readValue(response.getEntity().getContent(),
                            new TypeReference<Map<String, Object>>() {});
                    });
                }
            } catch (Exception e) {
                LOG.error("Negotiation unsuccessful", e);
                throw e;
            }
        }

        private void listenToServerSentEvents(ClassicHttpResponse response) throws Exception {
            try (InputStream inputStream = response.getEntity().getContent();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                //String line;
                while ((/*line = */reader.readLine()) != null) {
                    processMessages(inputStream);

                }
                LOG.info("Se ha cerrado la conexión con el servidor");
                errorList.add(MessageUtils.t("gaudi_integration_lost_connection"));
                notifyErrorConnection();
                restartConnection();
            } catch (Exception e) {
                errorList.add(MessageUtils.t("gaudi_integration_internal_error") + " Code:13");
                LOG.error("Error al conectar con el servidor code:13", e);
                notifyErrorConnection();
                if (isConnectionReset(e)) {
                    restartConnection();
                }
                throw e;
            }
        }

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

        private String getOSArch() {
            String osArch = System.getProperty("os.arch").toLowerCase();
            String arch;
            if (osArch.contains("64")) {
                arch = "64bit";
            } else if (osArch.contains("86") || osArch.contains("32")) {
                arch = "32bit";
            } else {
                arch = osArch;
            }
            return arch;
        }

        public void processMessages(InputStream inputStream) {
            ObjectMapper mapper = new ObjectMapper();
            for (String message : readMessages(inputStream)) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = mapper.readValue(message, Map.class);

                    if (data.containsKey("M")) {
                        List<?> mList = (List<?>) data.get("M");
                        if (!mList.isEmpty()) {
                            Object m0 = mList.get(0);
                            if (m0 instanceof Map) {
                                Map<?, ?> mMap = (Map<?, ?>) m0;
                                Object mValue = mMap.get("M");
                                if ("Firme".equals(mValue)) {
                                    if (!sign(data)) {
                                        errorList.add(MessageUtils.t("gaudi_integration_not_signed"));
                                        notifyErrorConnection();
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    errorList.add(MessageUtils.t("gaudi_integration_internal_error") + " Code:11");
                    LOG.error("Error al conectar con el servidor CODE:11", e);
                    notifyErrorConnection();
                } catch (Throwable e) {
                    errorList.add(MessageUtils.t("gaudi_integration_internal_error") + " Code:12");
                    LOG.error("Error al conectar con el servidor CODE:12", e);
                    notifyErrorConnection();
                    throw new RuntimeException(e);
                }
            }
        }
/* FIXME private and unused
        private String getHash(Map<String, Object> data) {
            List<?> mList = (List<?>) data.get("M");
            Map<?, ?> mMap = (Map<?, ?>) mList.get(0);
            List<?> aList = (List<?>) mMap.get("A");
            Map<?, ?> a0Map = (Map<?, ?>) aList.get(0);
            return (String) a0Map.get("b");
        }
*/

        public Iterable<String> readMessages(InputStream inputStream) {
        /*
        Esto es parte de lo que viene
          @c(a="a")
          public String HashAFirmarDocumento;
          @c(a="b")
          public String HashAFirmarResumen;
          @c(a="c")
          public String ResumenDelDocumento;
          @c(a="d")
          public String NombreDeLaEntidad;
          @c(a="e")
          public String LogoDeLaEntidad;
          @c(a="f")
          public int TimeoutEnSegundos;
          @c(a="g")
          public int IdDeLaSolicitud;
          @c(a="h")
          public int TipoDeFirma;
        */
            return () -> new Iterator<String>() {
                String nextLine = null;
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                @Override
                public boolean hasNext() {
                    try {
                        while ((nextLine = reader.readLine()) != null && !cancelled) {
                            nextLine = nextLine.trim();
                            if (nextLine.isEmpty()) continue;
                            if (nextLine.startsWith("data: ")) {
                                String text = nextLine.substring(6);
                                if (!"{}".equals(text)) {
                                    nextLine = text;
                                    return true;
                                }
                            }
                        }
                        return false;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public String next() {
                    if (nextLine == null && !hasNext()) throw new NoSuchElementException();
                    String line = nextLine;
                    nextLine = null;
                    return line;
                }
            };
        }

        public Boolean sign(Map<String, Object> data) throws IOException {
            Map<String, Object> params = new HashMap<>();
            params.put("H", "administradorDeClientes");
            params.put("M", "FirmaRealizada");
            List<Object> A = new ArrayList<>();
            params.put("A", A);
            params.put("I", 0);

            // 2. Emitir evento y bloquear hasta que el PIN esté disponible
            Map<String, Object> dev = new HashMap<>();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> mList = (List<Map<String, Object>>) data.get("M");
            Map<String, Object> m0 = mList.get(0);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> aList = (List<Map<String, Object>>) m0.get("A");
            Map<String, Object> a0 = aList.get(0);
            String base64Logo = a0.get("e").toString();
            byte[] imageBytes = Base64.getDecoder().decode(base64Logo);
            BufferedImage logoImg = ImageIO.read(new ByteArrayInputStream(imageBytes));
            this.logoIcon = new ImageIcon(logoImg);
            this.nameOfEntity = a0.get("d").toString();
            this.resumeOfEntity = a0.get("c").toString();

            RequestPinAndCodeWindow pinCodeRequest = new RequestPinAndCodeWindow(this.logoIcon, this.nameOfEntity, this.resumeOfEntity, "");
            boolean ok = pinCodeRequest.showAndWait();

            dev.put("e", a0.get("g"));
            boolean rejected = !ok;
            dev.put("d", rejected ? 2 : 0);
            dev.put("c", "");

            if (!rejected) {
                try {
                    String pin = new String(pinCodeRequest.getPinPassword());
                    String code = pinCodeRequest.getCode();
                    dev.put("c", code);

                    String b = getSignedHash((String) a0.get("b"), pin);
                    String a = getSignedHash((String) a0.get("a"), pin);

                    dev.put("b", b);
                    dev.put("a", a);

                    if (b == null || a == null) {
                        LOG.error("Alguna firma incorrecta {} o {}", a, b);
                        errorList.add(MessageUtils.t("guadi_integration_no_private_key_found"));
                        notifyErrorConnection();
                        dev.put("d", 2);
                        return false;
                    }
                } catch (Throwable e) {
                    LOG.error("Error al firmar hash", e);
                    errorList.add(MessageUtils.t("guadi_integration_no_private_key_found"));
                    notifyErrorConnection();
                    dev.put("d", 2);
                }
            }
            A.add(dev);
            try {
                String url = this.baseUrl + "/send";
                String userAgent = this.User_Agent;
                String json = new ObjectMapper().writeValueAsString(params);
                String body = "data=" + URLEncoder.encode(json, StandardCharsets.UTF_8);
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", userAgent);
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                String response = sendSignedData(url, body, headers);
                smartCardDetector.restoreSessions();
                return response != null && response.trim().equals("{\"I\":\"0\"}");
            } catch (Exception e) {
                LOG.error("Error enviando datos firmados", e);
                LOG.error("Error al conectar con el servidor code:16", e);
                return false;
            }
        }

        public String sendSignedData(String url, String body, Map<String, String> headers) throws Exception {
            int count = 0;
            boolean ok = false;
            String responseData = null;
            int maxTries = 1;
            Exception lastException = null;

            while (!ok && count < maxTries && !cancelled) {
                PoolingHttpClientConnectionManager cm = buildConnectionManager();

                try (CloseableHttpClient client = HttpClients.custom()
                    .setConnectionManager(cm)
                    .build()) {

                    URIBuilder uriBuilder = new URIBuilder(url);
                    for (Map.Entry<String, String> entry : params.entrySet()) {
                        uriBuilder.addParameter(entry.getKey(), entry.getValue());
                    }
                    HttpPost request = new HttpPost(uriBuilder.build());

                    if (headers != null) {
                        for (Map.Entry<String, String> entry : headers.entrySet()) {
                            request.setHeader(entry.getKey(), entry.getValue());
                        }
                    }

                    request.setEntity(new StringEntity(body, ContentType.APPLICATION_FORM_URLENCODED));


                    responseData = client.execute(request, response -> {
                        int status = response.getCode();
                        String respBody = EntityUtils.toString(response.getEntity());
                        if (status >= 200 && status < 300) {
                            return respBody;
                        } else {
                            LOG.warn("HTTP error al enviar datos firmados, status: {}, response: {}", status, respBody);
                            return null;
                        }
                    });

                    if (responseData != null) {
                        ok = true;
                    }
                } catch (Exception e) {
                    lastException = e;
                    LOG.warn("Error enviando datos firmados: {}", e.toString());
                }
                count++;
            }

            if (!ok && lastException != null) {
                LOG.error("Fallo enviando datos firmados tras {} intentos", count, lastException);
            }

            return responseData;
        }


        public String getSignedHash(String hashBase64, String pin) throws Throwable {
            boolean ok = false;
            int count = 0;
            String response = null;
            RequestPinAndCodeWindow pinDialog = null;
            while (!ok && count < 5 && !cancelled) {
                try {
                    response = _getSignedHash(hashBase64, pin);
                    if (response != null) {
                        if (response.contains("PKCS11 not found")) {
                            errorList.add(MessageUtils.t("guadi_integration_no_private_key_found_title"));
                            notifyErrorConnection();
                            restartConnection();
                        } else if (response.contains("load failed")) {
                            errorList.add(MessageUtils.t("guiswing_show_error_pkcs11_pinincorrect"));
                            notifyErrorConnection();
                        }
                    }
                    LOG.info("Response from PKCS11 service: " + response);

                } catch (Throwable e) {
                    LOG.error(e.getMessage());
                }
                if (response != null && !response.equals("CKR_PIN_INCORRECT") && !response.contains("load failed")) {
                    ok = true;
                } else {
                    count++;
                    pinDialog = new RequestPinAndCodeWindow(this.logoIcon, this.nameOfEntity, this.resumeOfEntity, MessageUtils.t("gaudi_integration_incorrect_pin"));
                    boolean pinOk = pinDialog.showAndWait();
                    if (!pinOk) {
                        break;
                    }
                    pin = new String(pinDialog.getPinPassword());
                }
            }
            return response;
        }


        private String _getSignedHash(String hashBase64, String pin) throws Throwable {
            try {
                PrivateKey privateKey = smartCardDetector.getSignPrivateKey(pin.toCharArray());
                LOG.info("Private key: " + privateKey);
                if (privateKey == null) {
                    errorList.add(MessageUtils.t("guadi_integration_no_private_key_found"));
                    notifyErrorConnection();
                    restartConnection();
                    return null;
                }
                byte[] hashBytes = Base64.getDecoder().decode(hashBase64);
                Signature signature = Signature.getInstance("SHA256withRSA");
                signature.initSign(privateKey);
                signature.update(hashBytes);
                byte[] signedBytes = signature.sign();

                // 3. Codifica la firma en base64 y la retorna
                String signatureBase64 = Base64.getEncoder().encodeToString(signedBytes);
                return signatureBase64;
            } catch (Throwable error) {
                String message = error.getLocalizedMessage();
                String className = error.getClass().getName();
                if (className.contains("sun.security.pkcs11.wrapper.PKCS11Exception")) {
                    if (message.contains("CKR_PIN_INCORRECT")) {
                        message = MessageUtils.t("guiswing_show_error_pkcs11_pinincorrect");
                        errorList.add(MessageUtils.t("guiswing_show_error_pkcs11_pinincorrect"));
                        notifyErrorConnection();
                    }
                    if (message.contains("CKR_PIN_LOCKED")) {
                        message = MessageUtils.t("guiswing_show_error_pkcs11_pinlocked");
                        errorList.add(MessageUtils.t("guiswing_show_error_pkcs11_pinlocked"));
                        notifyErrorConnection();
                    }
                    if (message.contains("PKCS11 not found")) {
                        message = MessageUtils.t("guiswing_show_error_pkcs11_notfound");
                        errorList.add(MessageUtils.t("guiswing_show_error_pkcs11_notfound"));
                        notifyErrorConnection();
                    }
                }
                return message;
            }
        }

        private PoolingHttpClientConnectionManager buildConnectionManager() throws Exception {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("certs/CA RAIZ NACIONAL - COSTA RICA v2.crt");
            KeyStore trustStore = loadCABundle(is);
            SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(trustStore, null)
                .build();

            return PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(new DefaultClientTlsStrategy(sslContext))
                .build();
        }

        private void setCommonHeaders(HttpGet request, String certAutenticacion, String certFirmante, String archHeader, String hostname) {
            request.setHeader(HttpHeaders.ACCEPT, "text/event-stream");
            request.setHeader("CertificadoAutenticacion", certAutenticacion);
            request.setHeader("CertificadoFirmante", certFirmante);
            request.setHeader("NombreDelSistemaOperativo", System.getProperty("os.name"));
            request.setHeader("VersionDelSistemaOperativo", System.getProperty("os.version"));
            request.setHeader("IpPrivada", "127.0.0.1");
            request.setHeader("Arquitectura", archHeader);
            request.setHeader("NombreDelHost", hostname);
            request.setHeader(HttpHeaders.USER_AGENT, this.User_Agent);
            request.setHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
        }

        private void notifyErrorConnection() {
            SwingUtilities.invokeLater(() -> {
                ((GUISwing) gui).getConnectionPanel().updateErrors(new ArrayList<>(errorList), connection);
                LOG.error(MessageUtils.t("gaudi_integration_error") + errorList.toString());
                errorList.clear();
            });
        }

        private void restartConnection(){
            SwingUtilities.invokeLater(() -> {
                ((GUISwing) gui).getConnectionPanel().restartConnection(connection);
            });
        }

        public void startCardMonitor() {
            LOG.info("Iniciando monitoreo de tarjeta");
            new Thread(() -> {
                while (!cancelled) {
                    boolean cardPresent = smartCardDetector.isCardPresent();

                    if (!cardPresent) {
                        LOG.warn("¡Tarjeta extraída!");
                        errorList.add("Tarjeta extraída, cerrando conexión");
                        notifyErrorConnection();
                        restartConnection();
                        break;
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        LOG.error("Error al conectar con el servidor code:17", e);
                        break;
                    }
                }
            }).start();
        }
    }

}
