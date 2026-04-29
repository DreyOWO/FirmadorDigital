package cr.libre.firmador.connections;

import com.microsoft.credentialstorage.SecretStore;
import com.microsoft.credentialstorage.StorageProvider;
import com.microsoft.credentialstorage.StorageProvider.SecureOption;
import com.microsoft.credentialstorage.model.StoredCredential;

import cr.libre.firmador.Settings;
import cr.libre.firmador.SettingsManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

public class PasswordProvider {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String CREDENTIAL_KEY = "firmador-keystore-password";
    private static final String CREDENTIAL_USERNAME = "keystore";

    private static PasswordProvider instance;
    private static final Object lock = new Object();

    private SecretStore<StoredCredential> credentialStorage;
    private boolean useSecureStore = false;

    private PasswordProvider() {
        try {
            this.credentialStorage = StorageProvider.getCredentialStorage(true, SecureOption.PREFERRED);

            if (this.credentialStorage != null && this.credentialStorage.isSecure()) {
                this.useSecureStore = true;
                LOG.info("Credential manager del SO disponible y seguro");
            } else if (this.credentialStorage != null) {
                LOG.warn("Credential manager disponible pero NO es seguro");
                this.useSecureStore = false;
            } else {
                LOG.warn("No se pudo inicializar credential manager del SO");
                this.useSecureStore = false;
            }
        } catch (Exception e) {
            LOG.warn("Error al inicializar credential manager del SO: {}", e.getMessage());
            this.useSecureStore = false;
            this.credentialStorage = null;
        }
    }

    public static PasswordProvider getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new PasswordProvider();
                }
            }
        }
        return instance;
    }

    /**
     * Verifica si el sistema puede usar almacenamiento seguro
     */
    public boolean canUseSecureStore() {
        boolean result = useSecureStore && credentialStorage != null;
        LOG.info("UseStore: {}", result);
        return result;
    }

    /**
     * Obtiene la contraseña del keystore desde el credential manager del SO.
     *
     * @return La contraseña si existe, null si no se encuentra o hay error
     */
    public String getKeystorePassword() {
        if (!canUseSecureStore()) {
            Settings settings = SettingsManager.getInstance().getSettings();
            return settings.getKeyPassword();
        }

        try {
            StoredCredential credential = credentialStorage.get(CREDENTIAL_KEY);
            if (credential != null) {
                String password = String.valueOf(credential.getPassword());
                credential.clear();
                LOG.debug("Contraseña recuperada desde credential manager del SO");
                return password;
            } else {
                LOG.debug("No se encontró contraseña en credential manager");
                return null;
            }
        } catch (Exception e) {
            LOG.error("Error al leer desde credential manager: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Guarda la contraseña del keystore en el credential manager del SO.
     *
     * @param password La contraseña a guardar
     * @return true si se guardó exitosamente, false si falló
     */
    public boolean saveKeystorePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede ser nula o vacía");
        }

        if (!canUseSecureStore()) {
            LOG.warn("Credential manager no disponible, no se puede guardar contraseña");
            return false;
        }

        try {
            char[] passwordChars = password.toCharArray();
            StoredCredential credential = new StoredCredential(CREDENTIAL_USERNAME, passwordChars);

            credentialStorage.add(CREDENTIAL_KEY, credential);
            LOG.info("Contraseña guardada en credential manager del SO");

            // Limpiar memoria
            credential.clear();
            Arrays.fill(passwordChars, (char) 0x00);

            return true;

        } catch (Exception e) {
            LOG.error("Error al guardar en credential manager: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Elimina la contraseña del credential manager del SO
     *
     * @return true si se eliminó exitosamente, false si falló
     */
    public boolean deleteKeystorePassword() {
        if (!canUseSecureStore()) {
            LOG.warn("Credential manager no disponible");
            return false;
        }

        try {
            credentialStorage.delete(CREDENTIAL_KEY);
            LOG.info("Contraseña eliminada del credential manager del SO");
            return true;
        } catch (Exception e) {
            LOG.error("Error al eliminar del credential manager: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si hay una contraseña almacenada en el credential manager
     *
     * @return true si existe una contraseña almacenada
     */
    public boolean hasStoredPassword() {
        if (!canUseSecureStore()) {
            return false;
        }

        try {
            StoredCredential credential = credentialStorage.get(CREDENTIAL_KEY);
            if (credential != null) {
                boolean hasPassword = credential.getPassword() != null && credential.getPassword().length > 0;
                credential.clear();
                return hasPassword;
            }
            return false;
        } catch (Exception e) {
            LOG.debug("Error al verificar credential manager: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Retorna información sobre el estado del credential manager
     *
     * @return String descriptivo del estado
     */
    public String getStorageInfo() {
        if (!canUseSecureStore()) {
            return "Credential Manager: NO DISPONIBLE";
        }

        try {
            StoredCredential credential = credentialStorage.get(CREDENTIAL_KEY);
            if (credential != null) {
                credential.clear();
                return "Credential Manager del SO: ACTIVO";
            }
            return "Credential Manager del SO: DISPONIBLE (sin contraseña guardada)";
        } catch (Exception e) {
            return "Credential Manager del SO: ERROR - " + e.getMessage();
        }
    }
}
