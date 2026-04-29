package cr.libre.firmador.connections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class KeystoreManager {
    static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static PasswordProvider passwordProvider = PasswordProvider.getInstance();

    /**
     * Elimina el keystore corrupto y crea un backup si es posible
     *
     * @param keystoreDir directorio donde se encuentra el keystore
     * @return true si se eliminó exitosamente
     */
    public static boolean deleteCorruptedKeystore(Path keystoreDir) {
        try {
            Path keystorePath = keystoreDir.resolve("keystore.p12");

            if (!Files.exists(keystorePath)) {
                LOG.info("No existe keystore para eliminar en: {}", keystorePath);
                return false;
            }

            // Crear backup antes de eliminar
            Path backupPath = keystoreDir.resolve("keystore.p12.backup." + System.currentTimeMillis());
            try {
                Files.copy(keystorePath, backupPath);
                LOG.info("Backup del keystore corrupto creado en: {}", backupPath);
            } catch (IOException e) {
                LOG.warn("No se pudo crear backup, procediendo a eliminar: {}", e.getMessage());
            }

            // Eliminar el keystore corrupto
            Files.delete(keystorePath);
            LOG.info("Keystore corrupto eliminado exitosamente: {}", keystorePath);
            return true;

        } catch (Exception e) {
            LOG.error("Error al intentar eliminar keystore corrupto", e);
            return false;
        }
    }

    /**
     * Detecta si la excepción es un error de integridad PKCS12
     */
    public static boolean isPKCS12IntegrityError(Throwable throwable) {
        if (throwable == null) {
            return false;
        }

        // Revisar el mensaje de la excepción
        String message = throwable.getMessage();
        if (message != null && (message.contains("Integrity check failed") ||
                message.contains("Failed PKCS12 integrity checking") ||
                message.contains("UnrecoverableKeyException"))) {
            return true;
        }

        // Revisar la causa recursivamente
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            return isPKCS12IntegrityError(cause);
        }

        return false;
    }

    private static KeyStore loadOrCreateKeyStore(Path keystorePath, char[] password) throws Exception {
        KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE);
        if (Files.exists(keystorePath)) {
            try (InputStream fis = Files.newInputStream(keystorePath)) {
                ks.load(fis, password);
            } catch (Exception e) {
                if (e.getMessage() != null &&
                    (e.getMessage().contains("integrity check failed") ||
                        e.getMessage().contains("password") ||
                        e.getMessage().contains("Failed PKCS12"))) {

                    LOG.error("El keystore no se puede abrir con la contraseña actual. Eliminando keystore corrupto.");
                    try {
                        Files.delete(keystorePath);
                        LOG.info("Keystore corrupto eliminado: {}", keystorePath);
                    } catch (Exception deleteEx) {
                        LOG.error("No se pudo eliminar el keystore corrupto: {}", deleteEx.getMessage());
                    }
                    ks.load(null, null);
                    LOG.info("Nuevo keystore vacío creado");
                } else {
                    throw e;
                }
            }
        } else {
            ks.load(null, null); // nuevo keystore vacío
        }
        return ks;
    }

    public static void saveToken(Path keystoreDir, String alias, String type, String token) throws Exception {
        Files.createDirectories(keystoreDir);
        Path keystorePath = keystoreDir.resolve("keystore.p12");
        // Obtener contraseña usando el PasswordProvider (con fallback automático)
        String password = passwordProvider.getKeystorePassword();

        if (password == null || password.isEmpty()) {
            throw new IllegalStateException("No se pudo obtener la contraseña del keystore");
        }
        KeyStore ks = loadOrCreateKeyStore(keystorePath, password.toCharArray());
        String fullAlias = alias + "." + type;
        LOG.info("ALIAS: " + fullAlias);
        SecretKey secretKey = new SecretKeySpec(token.getBytes(StandardCharsets.UTF_8), "AES");
        KeyStore.SecretKeyEntry entry = new KeyStore.SecretKeyEntry(secretKey);
        KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(password.toCharArray());
        ks.setEntry(fullAlias, entry, protParam);
        try (OutputStream fos = Files.newOutputStream(keystorePath)) {
            ks.store(fos, password.toCharArray());
        }
        LOG.info("Token guardado exitosamente. Almacenamiento usado: {}",
            passwordProvider.getStorageInfo());
    }

    public static String loadToken(Path keystoreDir, String alias, String type) throws Exception {
        Path keystorePath = keystoreDir.resolve("keystore.p12");
        if (!Files.exists(keystorePath)) {
            throw new IllegalStateException("No existe el keystore en: " + keystorePath);
        }
        // Obtener contraseña usando el PasswordProvider (con fallback automático)
        String password = passwordProvider.getKeystorePassword();

        if (password == null || password.isEmpty()) {
            throw new IllegalStateException("No se pudo obtener la contraseña del keystore");
        }

        KeyStore ks = loadOrCreateKeyStore(keystorePath, password.toCharArray());
        String fullAlias = alias + "." + type;
        LOG.info("Cargando token con ALIAS: {}", fullAlias);

        KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) ks.getEntry(fullAlias,
            new KeyStore.PasswordProtection(password.toCharArray()));

        if (entry == null) {
            throw new IllegalStateException("No se encontró token con alias: " + fullAlias);
        }

        LOG.info("Token cargado exitosamente. Almacenamiento usado: {}",
            passwordProvider.getStorageInfo());
        return new String(entry.getSecretKey().getEncoded(), StandardCharsets.UTF_8);
    }


    /**
     * Permite establecer/actualizar la contraseña del keystore
     */
    public static void setKeystorePassword(String password) {
        passwordProvider.saveKeystorePassword(password);
        LOG.info("Contraseña del keystore actualizada");
    }

    /**
     * Verifica si existe una contraseña configurada
     */
    public static boolean hasKeystorePassword() {
        return passwordProvider.hasStoredPassword();
    }

    /**
     * Obtiene información sobre dónde está almacenada la contraseña
     */
    public static String getPasswordStorageInfo() {
        return passwordProvider.getStorageInfo();
    }
}
