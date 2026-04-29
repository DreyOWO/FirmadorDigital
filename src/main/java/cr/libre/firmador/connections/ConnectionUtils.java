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
import cr.libre.firmador.SettingsManager;
import cr.libre.firmador.cards.CardSignInfo;
import cr.libre.firmador.cards.SmartCardDetector;
import cr.libre.firmador.documents.Document;
import cr.libre.firmador.gui.GUIInterface;
import cr.libre.firmador.gui.GUISwing;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.net.ssl.SSLContext;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionUtils {
    static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Set<String> INFLIGHT = ConcurrentHashMap.newKeySet();

    public ConnectionUtils() {
        super();
    }

    public static Connection findConnection(String name) throws Exception {
        List<Connection> connections = ServicesUrlsIO.load();
        for (Connection c : connections) {
            if (c.getService().equals(name)) return c;
        }
        return null;
    }

    public static String getIdentification(String identification) {
        if (identification != null && identification.contains("-")) {
            return identification.substring(identification.indexOf('-') + 1);
        }
        return identification;
    }

    public static BufferedImage getPageImageFromApi(Document document, int page, GUIInterface gui) {
        String key = document.getDocumentID() + "#" + page;
        if (!INFLIGHT.add(key)) {
            return null;
        }
        SmartCardDetector smartCardDetector = new SmartCardDetector();
        CardSignInfo card = getCardInfoByIdentification(document.getSerial(), smartCardDetector, gui);
        String token = "";
        String refresh_token = "";
        try {
            String identification = getIdentification(card.getIdentification());
            token = KeystoreManager.loadToken(SettingsManager.getInstance().getConfigDir(), identification + document.getService(), "access");
            refresh_token = KeystoreManager.loadToken(SettingsManager.getInstance().getConfigDir(), identification + document.getService(), "refresh");
        } catch (Exception e) {
            ((GUISwing) gui).showNotification(MessageUtils.t("connection_panel_error_token"), GUISwing.NotificationType.ERROR);
            LOG.error("Error al obtener token");
        }
        Connection connection = null;
        PoolingHttpClientConnectionManager cm = null;
        try {
            String service = document.getService();
            connection = findConnection(service);
            if (connection == null) {
                ((GUISwing) gui).showNotification(MessageUtils.t("connection_panel_not_connection1") +" "+ service + MessageUtils.t("connection_panel_not_connection2"), GUISwing.NotificationType.ERROR);
                LOG.error("Erro al obtener la connection");
                return null;
            }
            SSLContext sslContext = ConnectionManager.configureTrustStore(connection);
            cm = ConnectionManager.buildConnectionManager(sslContext);
        } catch (Throwable e) {
            ((GUISwing) gui).showNotification(MessageUtils.t("connection_panel_error_token"), GUISwing.NotificationType.ERROR);
            LOG.error("Error al obtener connection");
        }
        try (CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(cm)
            .build()) {

            Map<String, Object> body = new HashMap<>();
            body.put("document_id", document.getDocumentID());
            body.put("page_number", page);
            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(body);

            assert connection != null;
            HttpPost httpPost = new HttpPost(connection.getPreviewUrl(true));
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + token);
            httpPost.setHeader("X-Refresh-Token", refresh_token);
            httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

            try (ClassicHttpResponse response = httpClient.executeOpen(null, httpPost, null)) {
                int status = response.getCode();
                if (status == HttpStatus.SC_OK) {
                    try (InputStream is = response.getEntity().getContent()) {
                        String base64 = IOUtils.toString(is, StandardCharsets.UTF_8).trim();
                        if (base64.startsWith("\"") && base64.endsWith("\"")) {
                            base64 = base64.substring(1, base64.length() - 1);
                        }
                        byte[] imageBytes = Base64.getDecoder().decode(base64);
                        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
                            return ImageIO.read(bais);
                        }
                    }
                } else if (status == 403) {
                    ((GUISwing) gui).disconnect(document, "");
                } else {
                    ((GUISwing) gui).showNotification(MessageUtils.t("signpanel_problem_render_image"), GUISwing.NotificationType.ERROR);
                }
            }
        } catch (Throwable e) {
            ((GUISwing) gui).showNotification(MessageUtils.t("connection_panel_internal_error_image"), GUISwing.NotificationType.ERROR);
            LOG.error("Error en getPageImageFromApi", e);
        } finally {
            INFLIGHT.remove(key);
        }
        return null;
    }

    public static boolean deleteDocument(Document document, SmartCardDetector smartCardDetector, GUIInterface gui) throws Exception {
        CardSignInfo card = getCardInfoByIdentification(document.getSerial(), smartCardDetector, gui);
        String service = document.getService();
        if (card == null) {
            LOG.error("Error al firmar documentos virtuales porque no se encontro la tarjeta de firma");
            return false;
        }
        String identification = getIdentification(card.getIdentification());
        String json = "{ \"documentid\": \"" + document.getDocumentID().toString() + "\" }";
        String token = KeystoreManager.loadToken(SettingsManager.getInstance().getConfigDir(), identification + service, "access");
        String refresh_token = KeystoreManager.loadToken(SettingsManager.getInstance().getConfigDir(), identification + service, "refresh");
        Connection connection = ConnectionUtils.findConnection(service);
        assert connection != null;
        SSLContext sslContext = ConnectionManager.configureTrustStore(connection);
        PoolingHttpClientConnectionManager cm = ConnectionManager.buildConnectionManager(sslContext);
        String url = connection.getDeleteUrl(true);
        HttpPost post = new HttpPost(url);
        post.setHeader("Content-Type", "application/json");
        post.setHeader("Authorization", "Bearer " + token);
        post.setHeader("X-Refresh-Token", refresh_token);
        post.setEntity(new StringEntity(json, StandardCharsets.UTF_8));
        try (CloseableHttpClient client = HttpClients.custom()
            .setConnectionManager(cm)
            .build();
             ClassicHttpResponse apiResponse = client.executeOpen(null, post, null)) {
            int statusCode = apiResponse.getCode();
            if (statusCode == 204) {
                return true;
            } else if (statusCode == 403) {
                ((GUISwing) gui).disconnect(document, "");
            }
            ((GUISwing) gui).showNotification(MessageUtils.t("connection_panel_internal_error_image"), GUISwing.NotificationType.ERROR);
            return false;
        } catch (Exception e) {
            ((GUISwing) gui).showNotification(MessageUtils.t("connection_panel_internal_error_image"), GUISwing.NotificationType.ERROR);
            LOG.error("Error al borrar documento", e);
            return false;
        }
    }

    public static Boolean validateVirtualDocument(Document document, SmartCardDetector smartCardDetector, GUIInterface gui) {
        try {
            CardSignInfo card = getCardInfoByIdentification(document.getSerial(), smartCardDetector, gui);
            String service = document.getService();
            String identification = getIdentification(card.getIdentification());
            String token = KeystoreManager.loadToken(SettingsManager.getInstance().getConfigDir(), identification + service, "access");
            String refresh_token = KeystoreManager.loadToken(SettingsManager.getInstance().getConfigDir(), identification + service, "refresh");
            Connection connection = ConnectionUtils.findConnection(service);
            assert connection != null;
            SSLContext sslContext = ConnectionManager.configureTrustStore(connection);
            PoolingHttpClientConnectionManager cm = ConnectionManager.buildConnectionManager(sslContext);
            String url = connection.getValidateUrl(true)
                .replace("get_validate_document/", document.getId() + "/get_validate_document/");
            HttpGet get = new HttpGet(url);
            get.setHeader("Content-Type", "application/json");
            get.setHeader("Authorization", "Bearer " + token);
            get.setHeader("X-Refresh-Token", refresh_token);
            try (CloseableHttpClient client = HttpClients.custom()
                .setConnectionManager(cm)
                .build();
                 ClassicHttpResponse apiResponse = client.executeOpen(null, get, null)) {
                int statusCode = apiResponse.getCode();
                if (statusCode == 200) {
                    return true;
                } else if (statusCode == 403) {
                    ((GUISwing) gui).disconnect(document, "");
                }
                return false;
            } catch (Exception e) {
                ((GUISwing) gui).showNotification(MessageUtils.t("connection_panel_internal_error_image"), GUISwing.NotificationType.ERROR);
                LOG.error("Error al borrar documento", e);
                return false;
            }
        } catch (Exception e) {
            ((GUISwing) gui).showNotification(MessageUtils.t("connection_panel_internal_error_image"), GUISwing.NotificationType.ERROR);
            LOG.error("Error al borrar documento", e);
            return false;
        }
    }

    public static Boolean reloadVirtualDocuments(Connection connection) {
        try {
            SmartCardDetector smartCardDetector = new SmartCardDetector();
            List<CardSignInfo> cards = smartCardDetector.readListSmartCard();
            if (cards.isEmpty()) return false;
            CardSignInfo cardInfo = cards.get(0);
            String identification = getIdentification(cardInfo.getIdentification());
            String firmadorId = KeystoreManager.loadToken(
                SettingsManager.getInstance().getConfigDir(),
                identification + connection.getService(),
                "firmador_id"
            );
            String url = connection.getGetVirtualDocumentsUrl(true) + "?firmador_id=" + firmadorId;
            HttpGet request = new HttpGet(url);
            SSLContext sslContext = ConnectionManager.configureTrustStore(connection);
            PoolingHttpClientConnectionManager cm = ConnectionManager.buildConnectionManager(sslContext);
            try (CloseableHttpClient client = HttpClients.custom()
                .setConnectionManager(cm)
                .build();
                 ClassicHttpResponse apiResponse = client.executeOpen(null, request, null)) {
                return apiResponse.getCode() == 200;
            }
        } catch (Exception e) {
            LOG.error("Error al recargar documentos virtuales", e);
            LOG.error(e.getMessage(), e);
            return false;
        } catch (Throwable e) {
            LOG.error("Error al recargar documentos virtuales", e);
            return false;
        }
    }

    public static CardSignInfo getCardInfoByIdentification(String identification, SmartCardDetector smartCardDetector, GUIInterface gui) {
        List<CardSignInfo> cards = null;
        CardSignInfo result = null;
        String card_serialnumber;
        try {
            cards = smartCardDetector.readListSmartCard();
            LOG.info("Tarjetas detectadas: " + cards.size());
            smartCardDetector.setCardInfo(cards);
            for (CardSignInfo card : cards) {
                card_serialnumber = card.getIdentification();
                String serial = card_serialnumber.substring(4);
                LOG.info("Tarjeta detectada: " + serial);
                if (serial.equals(identification)) {
                    result = card;
                    break;
                }
            }
        } catch (Throwable e) {
            ((GUISwing) gui).showNotification(MessageUtils.t("gaudi_integration_not_certificate_detected"), GUISwing.NotificationType.ERROR);
            LOG.error(e.getMessage(), e);
        }
        return result;
    }

}
