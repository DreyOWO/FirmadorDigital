package cr.libre.firmador.connections;

import cr.libre.firmador.SettingsManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class ConnectionManager {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static SSLContext configureTrustStore(Connection connection) throws Exception {
        Path trustStorePath = SettingsManager.getInstance().getConfigDir().resolve("truststore.jks");
        String trustStorePassword = "changeit";

        Files.createDirectories(trustStorePath.getParent());

        if (!Files.exists(trustStorePath)) {
            LOG.info("Descargando certificado del servidor...");
            String host = extractHost(connection.getBaseUrl());
            downloadAndStoreCertificate(host, 443, trustStorePath.toString(), trustStorePassword);
        }

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream fis = new FileInputStream(trustStorePath.toFile())) {
            trustStore.load(fis, trustStorePassword.toCharArray());
        }

        return SSLContexts.custom()
            .loadTrustMaterial(trustStore, null)
            .build();
    }

    public static PoolingHttpClientConnectionManager buildConnectionManager(SSLContext sslContext) {
        return PoolingHttpClientConnectionManagerBuilder.create()
            .build();
    }

    private static String extractHost(String url) {
        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (Exception e) {
            return url.replaceAll("https?://", "").split("/")[0];
        }
    }

    private static void downloadAndStoreCertificate(String host, int port, String trustStorePath, String password) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        X509Certificate[] serverCerts = new X509Certificate[1];

        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    serverCerts[0] = chain[0];
                }
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }
        };

        sslContext.init(null, trustAllCerts, new SecureRandom());

        try (SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket(host, port)) {
            socket.startHandshake();
        }

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry(host, serverCerts[0]);

        try (FileOutputStream fos = new FileOutputStream(trustStorePath)) {
            trustStore.store(fos, password.toCharArray());
        }

    }
}
