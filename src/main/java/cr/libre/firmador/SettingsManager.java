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

package cr.libre.firmador;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import cr.libre.firmador.connections.PasswordProvider;

public class SettingsManager {
    private static SettingsManager cm = new SettingsManager();
    private Path path;
    private Properties props;
    private Settings settings = null;
    private static final String __OBFUSCATE = "OBF:";

    private SettingsManager() {
        super();
        this.path = null;
        this.props = new Properties();
    }

    public Path getConfigDir() throws IOException {
        String osName = System.getProperty("os.name").toLowerCase();
        String homepath = System.getProperty("user.home");
        String suffixpath = ".config/firmadorlibre";
        if (osName.contains("windows")) {
            homepath = System.getenv("APPDATA");
            suffixpath = "firmadorlibre";
        }
        // Se asegura que siempre exista el directorio de configuracion
        Path dirpath = FileSystems.getDefault().getPath(homepath, suffixpath);

        if (!Files.isDirectory(dirpath)) {
            Files.createDirectories(dirpath);
            if (osName.contains("windows"))
                Files.setAttribute(dirpath, "dos:hidden", true);
        }
        return dirpath;
    }

    public Path getPathConfigFile(String name) throws IOException {
        if (this.path == null) {
            this.path = this.getConfigDir();
            this.path = this.path.getFileSystem().getPath(this.path.toString(), name);
        }
        return this.path;
    }

    public String getConfigFile(String name) throws IOException {
        return this.getPathConfigFile(name).toString();
    }

    public Path getPath() {
        return this.path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public void setPath(String path) {
        this.path = FileSystems.getDefault().getPath(path);
    }

    public static SettingsManager getInstance() {
        return cm; // it always returns the same unique instance
    }

    public String getProperty(String key) {
        return this.props.getProperty(key, "");
    }

    public void setProperty(String key, String value) {
        this.props.setProperty(key, value);
    }

    private String getConfigFile() throws IOException {
        String configFile = "";
        // Returns the configuration file
        if (this.path == null) {
            if (Boolean.parseBoolean(System.getenv("FIRMADORINFLATPAK"))) {
                configFile = this.getConfigFile("config-flatpak-properties");
            } else {
                configFile = this.getConfigFile("config.properties");
            }
        } else
            configFile = this.path.toString();
        return configFile;
    }

    public boolean loadConfig() {
        // Carga las configuraciones desde un archivo de texto
        File configFile;
        boolean loaded = false;
        try {
            configFile = new File(this.getConfigFile());
            if (configFile.exists()) {
                InputStream inputStream = new FileInputStream(configFile);
                Reader reader = new InputStreamReader(inputStream, "UTF-8");
                this.props.load(reader);
                reader.close();
                inputStream.close();
                loaded = true;
            }
        } catch (IOException ex) {
            Logger.getLogger(SettingsManager.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
        return loaded;
    }

    public void saveConfig() {
        // Guarda las configuraciones en un archivo de texto
        // File configFile = null;
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(this.getConfigFile()), StandardCharsets.UTF_8);
            // writer = new FileWriter(this.getConfigFile());
            this.props.store(writer, "Firmador Libre settings");
        } catch (IOException ex) {
            Logger.getLogger(SettingsManager.class.getName()).log(Level.SEVERE, null, ex); // FIXME using JUL instead of
                                                                                           // SLF4j, could use just a
                                                                                           // single logger
            ex.printStackTrace();
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException ex) {
                Logger.getLogger(SettingsManager.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
            }
        }
    }

    private List<String> getListFromString(String data, List<String> defaultdata) {
        if (data == null && defaultdata != null && !defaultdata.isEmpty())
            return defaultdata;

        if (data == null || data.isEmpty())
            return new ArrayList<String>();

        List<String> plugins = new ArrayList<String>();
        for (String item : Arrays.asList(data.split("\\|")))
            if (!item.isEmpty())
                plugins.add(item);
        return plugins;
    }

    public static String generateKeyPassword() {
        final char[] chars = ("ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                "abcdefghijklmnopqrstuvwxyz" +
                "0123456789" +
                "!@#$%^&*()-_=+[]{};:,.<>?").toCharArray();

        SecureRandom random = new SecureRandom();
        char[] password = new char[32];

        for (int i = 0; i < password.length; i++) {
            password[i] = chars[random.nextInt(chars.length)];
        }

        return new String(password);
    }

    public static String obfuscate(String s) {
        StringBuilder buf = new StringBuilder();
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        buf.append(__OBFUSCATE);

        for (int i = 0; i < b.length; i++) {
            byte b1 = b[i];
            byte b2 = b[s.length() - (i + 1)];
            int i1 = 127 + b1 + b2;
            int i2 = 127 + b1 - b2;
            int i0 = i1 * 256 + i2;
            String x = Integer.toString(i0, 36);

            switch (x.length()) {
                case 1:
                    buf.append("000").append(x);
                    break;
                case 2:
                    buf.append("00").append(x);
                    break;
                case 3:
                    buf.append('0').append(x);
                    break;
                default:
                    buf.append(x);
                    break;
            }
        }
        return buf.toString();
    }

    public static String deobfuscate(String s) {
        if (s.startsWith(__OBFUSCATE)) {
            s = s.substring(4);
        }

        byte[] b = new byte[s.length() / 2];
        int l = 0;

        for (int i = 0; i < s.length(); i += 4) {
            String x = s.substring(i, i + 4);
            int i0 = Integer.parseInt(x, 36);
            int i1 = (i0 / 256);
            int i2 = (i0 % 256);
            b[l++] = (byte) ((i1 + i2 - 254) / 2);
        }

        return new String(b, 0, l, StandardCharsets.UTF_8);
    }

    public Settings getSettings() {
        Settings conf = new Settings();
        boolean loaded = this.loadConfig();
        if (loaded) {
            conf.withoutVisibleSign = Boolean
                    .parseBoolean(props.getProperty("withoutvisiblesign", String.valueOf(conf.withoutVisibleSign)));
            // conf.useLTA = Boolean.parseBoolean(props.getProperty("uselta",
            // String.valueOf(conf.useLTA)));
            conf.showLogs = Boolean.parseBoolean(props.getProperty("showlogs", String.valueOf(conf.showLogs)));
            conf.overwriteSourceFile = Boolean
                    .parseBoolean(props.getProperty("overwritesourcefile", String.valueOf(conf.overwriteSourceFile)));
            conf.isImgWithDpi = Boolean
                    .parseBoolean(props.getProperty("isimgwithdpi", String.valueOf(conf.isImgWithDpi)));
            conf.reason = props.getProperty("reason", conf.reason);
            conf.place = props.getProperty("place", conf.place);
            conf.contact = props.getProperty("contact", conf.contact);
            conf.dateFormat = props.getProperty("dateformat", conf.dateFormat);
            conf.defaultSignMessage = props.getProperty("defaultsignmessage", conf.defaultSignMessage);
            conf.pageNumber = Integer.parseInt(props.getProperty("pagenumber", conf.pageNumber.toString()));
            conf.signWidth = Integer.parseInt(props.getProperty("signwidth", conf.signWidth.toString()));
            conf.signHeight = Integer.parseInt(props.getProperty("signheight", conf.signHeight.toString()));
            conf.fontSize = Integer.parseInt(props.getProperty("fontsize", conf.fontSize.toString()));
            conf.font = props.getProperty("font", conf.font);
            conf.fontColor = props.getProperty("fontcolor", conf.fontColor);
            conf.backgroundColor = props.getProperty("backgroundcolor", conf.backgroundColor);
            conf.signX = Integer.parseInt(props.getProperty("singx", conf.signX.toString()));
            conf.signY = Integer.parseInt(props.getProperty("singy", conf.signY.toString()));
            conf.image = props.getProperty("image");
            PasswordProvider passwordProvider = PasswordProvider.getInstance();
            if (passwordProvider.canUseSecureStore()) {
                String existingPassword = passwordProvider.getKeystorePassword();
                if (existingPassword != null && !existingPassword.isEmpty()) {
                    // Ya existe una contraseña (en credential manager o Settings)
                    conf.keyPassword = existingPassword;
                    System.out
                            .println("Contraseña del keystore recuperada desde: " + passwordProvider.getStorageInfo());
                } else {
                    // No existe contraseña, generar una nueva
                    conf.keyPassword = generateKeyPassword();
                    System.out.println("Generando nueva contraseña del keystore");

                    // Guardar la contraseña recién generada
                    passwordProvider.saveKeystorePassword(conf.keyPassword);
                    System.out.println("Nueva contraseña guardada en: " + passwordProvider.getStorageInfo());
                }
            } else {
                System.out.println("Utilizando contraseña del keystore guardada en Settings");
                conf.keyPassword = deobfuscate(props.getProperty("keyPassword"));
                if (props.getProperty("keyPassword") == null || Objects.equals(props.getProperty("keyPassword"), "")) {
                    conf.keyPassword = generateKeyPassword();
                    setSettings(conf, true);
                } else {
                    conf.keyPassword = deobfuscate(props.getProperty("keyPassword"));
                }
            }
            // conf.startServer = Boolean.parseBoolean(props.getProperty("startserver",
            // String.valueOf(conf.startServer)));
            conf.fontAlignment = props.getProperty("fontalignment", conf.fontAlignment);
            conf.portNumber = Integer.parseInt(props.getProperty("portnumber", conf.portNumber.toString()));
            conf.pAdESLevel = props.getProperty("padesLevel", conf.pAdESLevel);
            conf.xAdESLevel = props.getProperty("xadesLevel", conf.xAdESLevel);
            conf.cAdESLevel = props.getProperty("cadesLevel", conf.cAdESLevel);
            conf.jAdESLevel = props.getProperty("jadesLevel", conf.jAdESLevel);
            conf.sofficePath = props.getProperty("sofficePath", conf.getSofficePath());
            conf.extraPKCS11Lib = props.getProperty("extrapkcs11Lib");
            conf.pKCS12File = getListFromString(props.getProperty("pkcs12file"), conf.pKCS12File);
            conf.activePlugins = getListFromString(props.getProperty("plugins"), conf.activePlugins);
            conf.pDFImgScaleFactor = getFloatFromString(
                    props.getProperty("pdfimgscalefactor", String.format("%.2f", conf.pDFImgScaleFactor)));
            conf.language = props.getProperty("language", conf.language);
            conf.country = props.getProperty("country", conf.country);
            conf.startwindowstate = props.getProperty("startwindowstate", conf.startwindowstate);
            conf.max_number_process_doc = Integer
                    .parseInt(props.getProperty("max_number_process_doc",
                            String.format("%d", conf.max_number_process_doc)));
            conf.registeredAllowedOrigins = props.getProperty("registeredAllowedOrigins", "");
            conf.signImageWidth = Integer
                    .parseInt(props.getProperty("signImageWidth", String.format("%d", conf.signImageWidth)));
            conf.signImageHeight = Integer
                    .parseInt(props.getProperty("signImageHeight", String.format("%d", conf.signImageHeight)));
            conf.startFimadorRemote = Boolean
                    .parseBoolean(props.getProperty("startFimadorRemote", String.valueOf(conf.startFimadorRemote)));
            conf.preferredBrowser = props.getProperty("preferredBrowser", conf.preferredBrowser);
            String simplifiedModeStr = props.getProperty("simplifiedMode");
            if (simplifiedModeStr != null) {
                conf.simplified_mode = Boolean.parseBoolean(simplifiedModeStr);
            } else {
                conf.simplified_mode = null;
            }
        }
        return conf;
    }

    private float getFloatFromString(String value) {
        String valueTmp = value.replace(",", ".");
        float fValue = 1;
        try {
            fValue = Float.parseFloat(valueTmp);
        } catch (Exception e) {
            Logger.getLogger(SettingsManager.class.getName()).log(Level.SEVERE, null, e);
            e.printStackTrace();
        }
        return fValue;
    }

    private String getListRepr(List<String> items) {
        return String.join("|", items);
    }

    public void setSettings(Settings conf, boolean save) {
        setProperty("withoutvisiblesign", String.valueOf(conf.withoutVisibleSign));
        // setProperty("uselta", String.valueOf(conf.useLTA));
        setProperty("overwritesourcefile", String.valueOf(conf.overwriteSourceFile));
        setProperty("isimgwithdpi", String.valueOf(conf.isImgWithDpi));
        setProperty("reason", conf.reason);
        setProperty("place", conf.place);
        setProperty("contact", conf.contact);
        setProperty("dateformat", conf.dateFormat);
        setProperty("defaultsignmessage", conf.defaultSignMessage);
        setProperty("pagenumber", conf.pageNumber.toString());
        setProperty("signwidth", conf.signWidth.toString());
        setProperty("signheight", conf.signHeight.toString());
        setProperty("fontsize", conf.fontSize.toString());
        setProperty("font", conf.font);
        setProperty("fontcolor", conf.fontColor);
        setProperty("backgroundcolor", conf.backgroundColor);
        setProperty("singx", conf.signX.toString());
        setProperty("singy", conf.signY.toString());
        // setProperty("startserver", String.valueOf(conf.startServer));
        setProperty("fontalignment", conf.fontAlignment.toString());
        setProperty("portnumber", conf.portNumber.toString());
        setProperty("showlogs", String.valueOf(conf.showLogs));
        setProperty("pdfimgscalefactor", String.format("%.2f", conf.pDFImgScaleFactor));
        setProperty("padesLevel", conf.pAdESLevel);
        setProperty("xadesLevel", conf.xAdESLevel);
        setProperty("cadesLevel", conf.cAdESLevel);
        setProperty("jadesLevel", conf.jAdESLevel);
        setProperty("sofficePath", conf.getSofficePath());
        setProperty("language", conf.language);
        setProperty("country", conf.country);
        setProperty("startwindowstate", conf.startwindowstate);
        setProperty("max_number_process_doc", conf.max_number_process_doc.toString());
        setProperty("signImageWidth", conf.signImageWidth.toString());
        setProperty("signImageHeight", conf.signImageHeight.toString());
        setProperty("startFimadorRemote", String.valueOf(conf.startFimadorRemote));
        setProperty("keyPassword", obfuscate(conf.keyPassword));
        setProperty("preferredBrowser", conf.preferredBrowser);
        setProperty("simplifiedMode", String.valueOf(conf.isSimplifiedMode()));

        setProperty("plugins", getListRepr(conf.activePlugins));
        if (conf.extraPKCS11Lib != null && conf.extraPKCS11Lib != "")
            setProperty("extrapkcs11Lib", conf.extraPKCS11Lib);
        else if (this.props.get("extrapkcs11Lib") != null)
            this.props.remove("extrapkcs11Lib");
        setProperty("pkcs12file", getListRepr(conf.pKCS12File));
        if (conf.image != null)
            setProperty("image", conf.image);
        else if (this.props.get("image") != null)
            this.props.remove("image");
        setProperty("registeredAllowedOrigins", conf.registeredAllowedOrigins);
        if (save)
            saveConfig();
    }

    public Settings getAndCreateSettings() {
        if (this.settings != null)
            return this.settings;
        Settings dev = new Settings();
        try {
            if (this.path != null)
                if (!Files.exists(this.path)) {
                    Logger.getLogger(SettingsManager.class.getName()).log(Level.SEVERE, null,
                            "Config File does not exist");
                    return dev;
                }
            dev = getSettings();
        } catch (Exception e) {
            Logger.getLogger(SettingsManager.class.getName()).log(Level.SEVERE, null, e);
            e.printStackTrace();
            setSettings(dev, true);
        }
        this.settings = dev;
        return dev;
    }

    public Properties getProps() {
        return this.props;
    }

    public void setProps(Properties props) {
        this.props = props;
    }

    public void nullifySettingsVariable() {
        this.settings = null;
    }

    public void setSettingsVariable(Settings settings) {
        this.settings = settings;
    }

    public Settings getSettingsVariable() {
        return this.settings;
    }

    public String saveDocumentSettings(Settings settings, String documentName) {
        String documentConfigFilePath = "";
        OutputStreamWriter writer = null;
        try {
            Path documentConfigDirPath = FileSystems.getDefault().getPath(getConfigDir().toString(), "docSettings");
            Files.createDirectories(documentConfigDirPath); // create the dirs if they don't exist
            documentConfigFilePath = FileSystems.getDefault()
                    .getPath(documentConfigDirPath.toString(), documentName + ".config").toString();

            Properties props = docSettingsToProperties(settings);
            writer = new OutputStreamWriter(Files.newOutputStream(Paths.get(documentConfigFilePath)),
                    StandardCharsets.UTF_8);
            props.store(writer, "Firmador Libre settings for " + documentName);
        } catch (Exception e) {
            Logger.getLogger(SettingsManager.class.getName()).log(Level.SEVERE, null, e); // FIXME using JUL instead of
                                                                                          // SLF4j, could use just a
                                                                                          // single logger
            e.printStackTrace();
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException e) {
                Logger.getLogger(SettingsManager.class.getName()).log(Level.SEVERE, null, e);
                e.printStackTrace();
            }
        }
        return documentConfigFilePath;
    }

    public Settings loadDocumentSettings(String documentSettingsPath) {
        InputStream inputStream = null;
        Settings settings = null;
        try {
            inputStream = Files.newInputStream(Paths.get(documentSettingsPath));
            Properties props = new Properties();
            props.load(inputStream);
            settings = propertiesToDocSettings(props);
        } catch (Exception e) {
            Logger.getLogger(SettingsManager.class.getName()).log(Level.SEVERE, null, e); // FIXME using JUL instead of
                                                                                          // SLF4j, could use just a
                                                                                          // single logger
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                Logger.getLogger(SettingsManager.class.getName()).log(Level.SEVERE, null, e);
                e.printStackTrace();
            }
        }
        return settings;
    }

    private Properties docSettingsToProperties(Settings settings) throws Exception {
        Properties props = new Properties();
        ArrayList<String> fieldsToSave = new ArrayList<>(Arrays.asList("country", "reason", "pDFImgScaleFactor",
                "signWidth", "fontSize", "language", "cAdESLevel", "signX", "pageNumber", "backgroundColor", "signY",
                "fontAlignment", "contact", "fontColor", "place", "image", "signHeight", "pAdESLevel",
                "overwriteSourceFile",
                "xAdESLevel", "dateFormat", "withoutVisibleSign", "defaultSignMessage", "font", "jAdESLevel"));
        for (String fieldName : fieldsToSave) {
            Object fieldValue = settings.getClass().getDeclaredField(fieldName).get(settings);
            if (fieldValue != null) {
                props.put(fieldName, fieldValue.toString());
            }
        }
        return props;
    }

    private Settings propertiesToDocSettings(Properties props) throws Exception {
        Settings settings = new Settings(getAndCreateSettings()); // the other settings not loaded will have the value
                                                                  // of current app settings
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            Field field = settings.getClass().getDeclaredField(entry.getKey().toString());
            String value = entry.getValue().toString();
            if (field.getType() == boolean.class) {
                field.setBoolean(settings, Boolean.parseBoolean(value));
            } else if (field.getType() == Integer.class) {
                field.set(settings, Integer.parseInt(value));
            } else if (field.getType() == float.class) {
                field.setFloat(settings, getFloatFromString(value));
            } else {
                field.set(settings, value);
            }
        }
        return settings;
    }
}
