package cr.libre.firmador.signers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.Settings;
import cr.libre.firmador.SettingsManager;
import cr.libre.firmador.cards.CardSignInfo;
import cr.libre.firmador.cards.SmartCardDetector;
import cr.libre.firmador.connections.*;
import cr.libre.firmador.documents.Document;
import cr.libre.firmador.gui.GUIInterface;
import cr.libre.firmador.gui.GUISwing;
import cr.libre.firmador.gui.swing.RequestPinWindowRemote;
import cr.libre.firmador.remote.FirmadorRemoteDocument;
import cr.libre.firmador.remote.RemoteSignatureValueDTO;
import eu.europa.esig.dss.ws.dto.SignatureValueDTO;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cr.libre.firmador.connections.ConnectionUtils.getIdentification;

public class VirtualSigner {

    final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    protected GUIInterface gui;
    private SmartCardDetector smartCardDetector;

    public VirtualSigner(GUIInterface gui) {
        this.gui = gui;
        if (smartCardDetector == null)
            smartCardDetector = new SmartCardDetector();
    }

    public boolean getHashToSign(List<Document> toSignDocuments, Settings settings) throws Exception {
        LOG.info("identificacion" + toSignDocuments.get(0).getSerial());
        CardSignInfo card = ConnectionUtils.getCardInfoByIdentification(toSignDocuments.get(0).getSerial(), smartCardDetector, gui);
        String service = toSignDocuments.get(0).getService();
        if (card == null) {
            LOG.error("Error al firmar documentos virtuales porque no se encontro la tarjeta de firma");
            return false;
        }
        List<String> ids = new ArrayList<>();
        for (Document doc : toSignDocuments) {
            ids.add(doc.getDocumentID().toString());
        }
        String identification = getIdentification(card.getIdentification());
        String firmadorId = KeystoreManager.loadToken(SettingsManager.getInstance().getConfigDir(), identification + service, "firmador_id");
        if (settings == null){
            SettingsManager settingsManager = SettingsManager.getInstance();
            settings = settingsManager.getSettings();
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode settingsNode = mapper.valueToTree(settings);
        Map<String, Object> payload = new HashMap<>();
        payload.put("ids", ids);
        payload.put("firmador_id", firmadorId);
        payload.put("settings", settingsNode);
        String json = new ObjectMapper().writeValueAsString(payload);
        LOG.info("Alias: "+ identification + service);
        try {
            String token = KeystoreManager.loadToken(SettingsManager.getInstance().getConfigDir(), identification + service, "access");
            String refresh_token = KeystoreManager.loadToken(SettingsManager.getInstance().getConfigDir(), identification + service, "refresh");
            Connection connection = ConnectionUtils.findConnection(service);
            SSLContext sslContext = ConnectionManager.configureTrustStore(connection);
            PoolingHttpClientConnectionManager cm = ConnectionManager.buildConnectionManager(sslContext);
            String url = connection.getSignUrl(true);
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
                if (statusCode == 201) {
                    return true;
                } else if (statusCode == 403) {
                    ((GUISwing) gui).disconnect(null, service);
                }
                LOG.error(apiResponse.toString());
                return false;
            } catch (Exception e) {
                LOG.error("Error al obtener hash a firmar", e);
                return false;
            }
        } catch (Exception e) {
            LOG.error("Error al obtener hash a firmar", e);
            return false;
        }
    }

    public boolean sign(List<FirmadorRemoteDocument> toSignDocuments, String service) throws Exception {
        if (toSignDocuments == null || toSignDocuments.isEmpty()) {
            LOG.error("Error al firmar documentos virtuales porque no se encontro documentos");
            return false;
        }
        CardSignInfo card = this.getCardInfoBySerial(toSignDocuments.get(0).getSerialnumber());
        if (card == null) {
            LOG.error("Error al firmar documentos virtuales porque no se encontro la tarjeta de firma");
            return false;
        }
        BasicSigner signer = new BasicSigner(gui);

        FirmadorRemoteDocument firstDoc = toSignDocuments.get(0);
        RequestPinWindowRemote pinrequest = new RequestPinWindowRemote();
        pinrequest.setCard(card);
        pinrequest.setDocumentName(firstDoc.getDocumentName());
        pinrequest.setIcon(firstDoc.getImageIcon());
        smartCardDetector.restoreSessions();
        int ok = pinrequest.showandwait();
        smartCardDetector.login(card);
        if (ok == 0) {
            List<RemoteSignatureValueDTO> signatures = new ArrayList<>();

            for (FirmadorRemoteDocument remoteDoc : toSignDocuments) {
                SignatureValueDTO signature = signer.sign(card, remoteDoc.getTobesigned());
                if (signature == null) {
                    return false;
                }
                RemoteSignatureValueDTO rsignature = new RemoteSignatureValueDTO(remoteDoc, signature);
                signatures.add(rsignature);
            }

            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(signatures);
            String identification = getIdentification(card.getIdentification());
            String token = KeystoreManager.loadToken(SettingsManager.getInstance().getConfigDir(), identification + service, "access");
            String refresh_token = KeystoreManager.loadToken(SettingsManager.getInstance().getConfigDir(), identification + service, "refresh");
            Connection connection = ConnectionUtils.findConnection(service);
            assert connection != null;
            SSLContext sslContext = ConnectionManager.configureTrustStore(connection);
            PoolingHttpClientConnectionManager cm = ConnectionManager.buildConnectionManager(sslContext);
            HttpPost post = new HttpPost(connection.getCompleteUrl(true));
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Authorization", "Bearer " + token);
            post.setHeader("X-Refresh-Token", refresh_token);
            post.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

            try (CloseableHttpClient client = HttpClients.custom()
                .setConnectionManager(cm)
                .build();
                 ClassicHttpResponse apiResponse = client.executeOpen(null, post, null)) {

                int statusCode = apiResponse.getCode();
                if (statusCode == 200) {
                    return true;
                }else if (statusCode == 403) {
                    ((GUISwing) gui).disconnect(null, service);
                }
            }
            return false;
        } else {
            ((GUISwing) gui).showNotification(MessageUtils.t("virtual_ping_panel_error"), GUISwing.NotificationType.ERROR);
        }
        return false;
    }

    private CardSignInfo getCardInfoBySerial(String serial) {
        List<CardSignInfo> cards = null;
        CardSignInfo result = null;
        String card_serialnumber;
        try {
            cards = smartCardDetector.readListSmartCard();
            LOG.info("Tarjetas detectadas: " + cards.size());
            smartCardDetector.setCardInfo(cards);
            for (CardSignInfo card : cards) {
                card_serialnumber = card.getCertificate().getSerialNumber().toString();
                LOG.info("Tarjeta detectada: " + card_serialnumber);
                if (card_serialnumber.equals(serial)) {
                    result = card;
                    break;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return result;
    }


}
