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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.awt.Font;
import java.awt.Frame;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignerTextPosition;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("serial")
public class Settings {
    private List<ConfigListener> listeners = new ArrayList<ConfigListener>();

    @JsonIgnore
    public String releaseUrlCheck = "https://firmador.libre.cr/v2/version.txt";
    @JsonIgnore
    public String baseUrl = "https://firmador.libre.cr";
    @JsonIgnore
    public String releaseUrl = "https://firmador.libre.cr/v2/firmador.jar";
    @JsonIgnore
    public String releaseMacUrl = "https://firmador.libre.cr/v2/macos-notarized/firmador.jar";
    @JsonIgnore
    public String releaseMacChecksumUrl = "https://firmador.libre.cr/v2/macos-notarized/firmador.jar.sha256";
    @JsonIgnore
    public String releaseSnapshotUrl = "https://firmador.libre.cr/v2/firmador-en-pruebas.jar";
    @JsonIgnore
    public String checksumUrl = "https://firmador.libre.cr/v2/firmador.jar.sha256";
    @JsonIgnore
    public String checksumSnapshotUrl = "https://firmador.libre.cr/v2/firmador-en-pruebas.jar.sha256";

    public String defaultDevelopmentVersion = "Desarrollo";
    public boolean withoutVisibleSign = false;
    public boolean onlyimage = false;
    // public boolean useLTA = true;
    public boolean overwriteSourceFile = false;

    public String reason = "";
    public String place = "";
    public String contact = "";
    public String dateFormat = "dd/MM/yyyy hh:mm:ss a";
    public String dateFormatEn = "MM/dd/yyyy hh:mm:ss a";
    @JsonIgnore
    public Map<String, String> dateFormatByLanguage = new HashMap<String, String>() {
        {
            put("es", dateFormat);
            put("en", dateFormatEn);
        }
    };
    public String defaultSignMessage;
    public Integer signWidth = 133;
    public Integer signHeight = 33;
    public Integer fontSize = 7;
    // Attributes for size of image in sign
    public Integer signImageWidth = 60;
    public Integer signImageHeight = 50;
    // Only for remote you can pass base64:fontname:base64representation
    // Only for remote you can pass file:fontname:path
    // NOTE: On remote signature System font do not work
    public String font = Font.SANS_SERIF;
    public String fontColor = "#000000";
    public String backgroundColor = "transparente";
    @JsonIgnore
    public String extraPKCS11Lib = null;
    public Integer signX = 198;
    public Integer signY = 0;
    // Only for remote you can pass
    // data:image/png;base64,bas64data
    public String image = null;
    // public boolean startServer = false;
    public String fontAlignment = "RIGHT";
    public boolean showLogs = false;

    public Integer pageNumber = 1;
    public Integer portNumber = 3516;
    public String pAdESLevel = "LTA";
    public String xAdESLevel = "LTA";
    public String cAdESLevel = "LTA";
    public String jAdESLevel = "LTA";
    public String sofficePath = "";
    @JsonIgnore
    public List<String> pKCS12File = new ArrayList<String>();

    @JsonIgnore
    public List<String> activePlugins = new ArrayList<String>();
    @JsonIgnore
    public List<String> availablePlugins = new ArrayList<String>();

    public String registeredAllowedOrigins = "";
    public String tempAllowedHosts = "";
    public String noAuthorizedHosts = "";

    public float scaleFactorDpi = 1;
    public float pDFImgScaleFactor = 1;
    public boolean isImgWithDpi = false;

    public String language = "es";
    public String country = "CR";

    @JsonIgnore
    public String languagesList[] = new String[] { "es", "en" };

    @JsonIgnore
    public String windowSetateList[] = new String[] { "NORMAL", "MAXIMIZED_BOTH", "MAXIMIZED_HORIZ", "MAXIMIZED_VERT" };
    @JsonIgnore
    public Map<String, String> countryByLanguage = new HashMap<String, String>() {
        {
            put("es", "CR");
            put("en", "US");
        }
    };

    public boolean extendDocument = false;
    public boolean isVisibleSignature = false;
    public boolean hideSignatureAdvice = false;
    public boolean signASiC = false;
    public boolean forceCades = false;
    public String startwindowstate = "NORMAL";
    public Integer max_number_process_doc = 5;

    public boolean startFimadorRemote = false;

    public String preferredBrowser = "";

    public String keyPassword = "";

    final org.slf4j.Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public Boolean simplified_mode = null;

    @SuppressWarnings("this-escape")
    public Settings() {
        activePlugins.add("cr.libre.firmador.plugins.DummyPlugin");
        activePlugins.add("cr.libre.firmador.plugins.CheckUpdatePlugin");
        // activePlugins.add("cr.libre.firmador.plugins.DocumentSignLogs");
        availablePlugins.add("cr.libre.firmador.plugins.DummyPlugin");
        availablePlugins.add("cr.libre.firmador.plugins.CheckUpdatePlugin");
        availablePlugins.add("cr.libre.firmador.plugins.DocumentSignLogs");

        defaultSignMessage = getTranslatedDefaultSignMessage();
    }

    public Settings(Settings oldsettings) {
        this.releaseUrlCheck = oldsettings.releaseUrlCheck;
        this.baseUrl = oldsettings.baseUrl;
        this.releaseUrl = oldsettings.releaseUrl;
        this.releaseSnapshotUrl = oldsettings.releaseSnapshotUrl;
        this.checksumUrl = oldsettings.checksumUrl;
        this.checksumSnapshotUrl = oldsettings.checksumSnapshotUrl;
        this.defaultDevelopmentVersion = oldsettings.defaultDevelopmentVersion;
        this.withoutVisibleSign = oldsettings.withoutVisibleSign;
        this.hideSignatureAdvice = oldsettings.hideSignatureAdvice;
        this.overwriteSourceFile = oldsettings.overwriteSourceFile;
        this.reason = oldsettings.reason;
        this.place = oldsettings.place;
        this.contact = oldsettings.contact;
        this.dateFormat = oldsettings.dateFormat;
        this.dateFormatEn = oldsettings.dateFormatEn;
        this.dateFormatByLanguage = oldsettings.dateFormatByLanguage;
        this.defaultSignMessage = oldsettings.defaultSignMessage;
        this.signWidth = oldsettings.signWidth;
        this.signHeight = oldsettings.signHeight;
        this.fontSize = oldsettings.fontSize;
        this.font = oldsettings.font;
        this.fontColor = oldsettings.fontColor;
        this.backgroundColor = oldsettings.backgroundColor;
        this.extraPKCS11Lib = oldsettings.extraPKCS11Lib;
        this.signX = oldsettings.signX;
        this.signY = oldsettings.signY;
        this.image = oldsettings.image;
        this.fontAlignment = oldsettings.fontAlignment;
        this.showLogs = oldsettings.showLogs;
        this.pageNumber = oldsettings.pageNumber;
        this.portNumber = oldsettings.portNumber;
        this.pAdESLevel = oldsettings.pAdESLevel;
        this.xAdESLevel = oldsettings.xAdESLevel;
        this.cAdESLevel = oldsettings.cAdESLevel;
        this.jAdESLevel = oldsettings.jAdESLevel;
        this.sofficePath = oldsettings.getSofficePath();
        this.pDFImgScaleFactor = oldsettings.pDFImgScaleFactor;
        this.isVisibleSignature = oldsettings.isVisibleSignature;
        this.signASiC = oldsettings.signASiC;
        this.forceCades = oldsettings.forceCades;
        this.countryByLanguage = oldsettings.countryByLanguage;
        this.language = oldsettings.language;
        this.country = oldsettings.country;
        this.onlyimage = oldsettings.onlyimage;
        this.startwindowstate = oldsettings.startwindowstate;
        this.max_number_process_doc = oldsettings.max_number_process_doc;
        this.signImageWidth = oldsettings.signImageWidth;
        this.signImageHeight = oldsettings.signImageHeight;
        this.preferredBrowser = oldsettings.preferredBrowser;
    }

    public List<String> getAllowedHosts() {
        return Stream.concat(
                Arrays.stream(tempAllowedHosts.split("\n")),
                Arrays.stream(registeredAllowedOrigins.split("\n")))
                .map(String::trim) // Elimina espacios extra
                .filter(s -> !s.isEmpty()) // Evita elementos vacíos
                .distinct() // Elimina duplicados
                .collect(Collectors.toList());
    }

    public List<String> getNoAuthorizedHosts() {
        return Arrays.stream(noAuthorizedHosts.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public void removeAllowedHost(String host) {
        String[] hosts = this.registeredAllowedOrigins.split("\\n");
        StringBuilder sb = new StringBuilder();
        boolean removed = false;
        for (String h : hosts) {
            if (!h.trim().equals(host.trim()) && !h.trim().isEmpty()) {
                if (sb.length() > 0)
                    sb.append("\n");
                sb.append(h.trim());
            } else if (h.trim().equals(host.trim())) {
                removed = true;
            }
        }
        this.registeredAllowedOrigins = sb.toString();
        if (!removed) {
            String[] hosts2 = this.tempAllowedHosts.split("\\n");
            StringBuilder sb2 = new StringBuilder();
            for (String h : hosts2) {
                if (!h.trim().equals(host.trim()) && !h.trim().isEmpty()) {
                    if (sb2.length() > 0)
                        sb2.append("\n");
                    sb2.append(h.trim());
                }
            }
            this.tempAllowedHosts = sb2.toString();
        }
    }

    public void removeNoAuthorizedHost(String host) {
        String[] hosts = this.noAuthorizedHosts.split("\\n");
        StringBuilder sb = new StringBuilder();
        for (String h : hosts) {
            if (!h.trim().equals(host.trim()) && !h.trim().isEmpty()) {
                if (sb.length() > 0)
                    sb.append("\n");
                sb.append(h.trim());
            }
        }
        this.noAuthorizedHosts = sb.toString();
    }

    public void addNoAuthorizedHost(String host) {
        String[] hosts = this.noAuthorizedHosts.split("\\n");
        for (String h : hosts) {
            if (h.trim().equals(host.trim())) {
                return;
            }
        }
        this.noAuthorizedHosts = this.noAuthorizedHosts + "\n" + host;
    }

    public void addTempAllowedHost(String host) {
        this.tempAllowedHosts = this.tempAllowedHosts + "\n" + host;
    }

    public void setRegisteredAllowedOrigins(List<String> registeredAllowedOrigins) {
        this.registeredAllowedOrigins = String.join("\n", registeredAllowedOrigins);
    }

    public String getFormattedAllowedPorts() {
        return String.join("\n", registeredAllowedOrigins);
    }

    public void setScaleFactorDpi(float scaleFactor) {
        this.scaleFactorDpi = scaleFactor;
    }

    public String getDefaultSignMessage() {
        return this.defaultSignMessage;
    }

    public String getTranslatedDefaultSignMessage() {
        Locale locale = new Locale.Builder().setLanguage(language).setRegion(country).build();
        ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);
        return bundle.getString("configpanel_default_sign_message");
    }

    public String getDateFormat() {
        try {
            return this.dateFormat;
        } catch (Exception e) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null,
                    MessageUtils.t("settings_error_return_date_format") + ": " + e); // FIXME does try-catch make sense
                                                                                     // here?
            e.printStackTrace();
            return "dd/MM/yyyy hh:mm:ss a";
        }
    }

    public void addListener(ConfigListener toAdd) {
        LOG.info("Adding listener from " + toAdd.getClass().getName());
        listeners.add(toAdd);
    }

    public void updateConfig() {
        for (ConfigListener hl : listeners)
            hl.updateConfig();
    }

    public String getSofficePath() {
        String newofficepath = "/usr/bin/soffice";
        if (this.sofficePath.isEmpty()) {
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("mac"))
                newofficepath = "/Applications/LibreOffice.app/Contents/MacOS/soffice ";
            else if (osName.contains("linux")) {
                String envFlatpakSofficePath = System.getenv("FLATPAKSOFFICEPATH");
                newofficepath = envFlatpakSofficePath != null && !envFlatpakSofficePath.isEmpty()
                        ? envFlatpakSofficePath
                        : "/usr/bin/soffice";
            } else if (osName.contains("windows"))
                newofficepath = System.getenv("systemdrive") + "\\Program Files\\LibreOffice\\program\\soffice.exe";
        } else
            newofficepath = this.sofficePath;
        return newofficepath;
    }

    public String getFontName(String fontName, boolean isPdf) {
        String selectedFontName = "";
        switch (fontName) {
            case "Arial Regular":
            case "Arial Italic":
            case "Arial Bold":
            case "Arial Bold Italic":
                if (!isPdf)
                    selectedFontName = "Arial";
                else
                    selectedFontName = Font.SANS_SERIF;
                break;
            case "Helvetica Regular":
            case "Helvetica Oblique":
            case "Helvetica Bold":
            case "Helvetica Bold Oblique":
                if (!isPdf)
                    selectedFontName = "Helvetica";
                else
                    selectedFontName = Font.SANS_SERIF;
                break;
            case "Nimbus Sans Regular":
            case "Nimbus Sans Italic":
            case "Nimbus Sans Bold":
            case "Nimbus Sans Bold Italic":
                if (!isPdf)
                    selectedFontName = "Nimbus Sans";
                else
                    selectedFontName = Font.SANS_SERIF;
                break;
            case "Nimbus Roman Regular":
            case "Nimbus Roman Italic":
            case "Nimbus Roman Bold":
            case "Nimbus Roman Bold Italic":
                if (!isPdf)
                    selectedFontName = "Nimbus Roman";
                else
                    selectedFontName = Font.SERIF;
                break;
            case "Times New Roman Regular":
            case "Times New Roman Italic":
            case "Times New Roman Bold":
            case "Times New Roman Bold Italic":
                if (!isPdf)
                    selectedFontName = "Times New Roman";
                else
                    selectedFontName = Font.SERIF;
                break;
            case "Courier New Regular":
            case "Courier New Italic":
            case "Courier New Bold":
            case "Courier New Bold Italic":
                if (!isPdf)
                    selectedFontName = "Courier New";
                else
                    selectedFontName = Font.MONOSPACED;
                break;
            case "Nimbus Mono PS Regular":
            case "Nimbus Mono PS Italic":
            case "Nimbus Mono PS Bold":
            case "Nimbus Mono PS Bold Italic":
                if (!isPdf)
                    selectedFontName = "Nimbus Mono PS";
                else
                    selectedFontName = Font.MONOSPACED;
                break;
            default:
                selectedFontName = Font.SANS_SERIF;
                break;
        }
        return selectedFontName;
    }

    public int getFontStyle(String fontName) {
        switch (fontName) {
            case "Arial Regular":
            case "Courier New Regular":
            case "Helvetica Regular":
            case "Nimbus Roman Regular":
            case "Nimbus Sans Regular":
            case "Nimbus Mono PS Regular":
            case "Times New Roman Regular":
                return Font.PLAIN;
            case "Arial Italic":
            case "Courier New Italic":
            case "Helvetica Oblique":
            case "Nimbus Roman Italic":
            case "Nimbus Sans Italic":
            case "Nimbus Mono PS Italic":
            case "Times New Roman Italic":
                return Font.ITALIC;
            case "Arial Bold":
            case "Courier New Bold":
            case "Helvetica Bold":
            case "Nimbus Roman Bold":
            case "Nimbus Sans Bold":
            case "Nimbus Mono PS Bold":
            case "Times New Roman Bold":
                return Font.BOLD;
            case "Arial Bold Italic":
            case "Courier New Bold Italic":
            case "Helvetica Bold Oblique":
            case "Nimbus Roman Bold Italic":
            case "Nimbus Sans Bold Italic":
            case "Nimbus Mono PS Bold Italic":
            case "Times New Roman Bold Italic":
                return Font.BOLD + Font.ITALIC;
            default:
                return Font.PLAIN;
        }
    }

    public SignerTextPosition getFontAlignment() {
        SignerTextPosition position = SignerTextPosition.RIGHT;
        switch (this.fontAlignment) {
            case "RIGHT":
                position = SignerTextPosition.RIGHT;
                break;
            case "LEFT":
                position = SignerTextPosition.LEFT;
                break;
            case "BOTTOM":
                position = SignerTextPosition.BOTTOM;
                break;
            case "TOP":
                position = SignerTextPosition.TOP;
                break;
            default:
                break;
        }

        return position;
    }

    @JsonIgnore
    public Color getFontColor() {
        if (this.fontColor.equalsIgnoreCase("transparente"))
            return new Color(255, 255, 255, 0);
        try {
            return Color.decode(this.fontColor);
        } catch (Exception e) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null,
                    MessageUtils.t("settings_error_decoding_font_color") + ": " + e);
            e.printStackTrace();
            return new Color(0, 0, 0, 255);
        }
    }

    @JsonIgnore
    public Color getBackgroundColor() {
        if (this.backgroundColor.equalsIgnoreCase("transparente"))
            return new Color(255, 255, 255, 0);
        try {
            return Color.decode(this.backgroundColor);
        } catch (Exception e) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null,
                    MessageUtils.t("settings_error_decoding_background_color") + ": " + e);
            e.printStackTrace();
            return new Color(255, 255, 255, 0);
        }
    }

    @JsonProperty("fontColor")
    public String getFontColorString() {
        return this.fontColor;
    }

    @JsonProperty("fontColor")
    public void setFontColor(String fontColor) {
        this.fontColor = fontColor;
    }

    @JsonProperty("backgroundColor")
    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @JsonProperty("backgroundColor")
    public String getBackgroundColorString() {
        return this.backgroundColor;
    }

    public String getImage() {
        if (this.image == null || this.image.trim().isEmpty()) {
            return null;
        }
        File temp = new File(this.image.trim());
        if (temp.exists() && temp.isFile()) {
            return temp.toURI().toString();
        }
        return null;
    }

    @JsonIgnore
    public byte[] getByteImage() {
        byte[] decodedBytes = null;
        if (this.image == null)
            return null;
        if (this.image.startsWith("data:image/")) {
            String base64Image = image.split(",")[1];
            decodedBytes = Base64.getDecoder().decode(base64Image.getBytes());
        } else {
            File temp = new File(this.image);
            boolean exists = temp.exists();
            if (exists) {
                decodedBytes = new byte[(int) temp.length()];
                FileInputStream fis;
                try {
                    fis = new FileInputStream(temp);
                    fis.read(decodedBytes);
                    fis.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    decodedBytes = null;
                }
            }
        }
        return decodedBytes;
    }

    public void setImage(String img) {
        this.image = img;
    }

    public boolean isRemote() {
        String origin = System.getProperty("jnlp.remoteOrigin");
        boolean isRemote = (origin != null);
        return isRemote;
    }

    public boolean getStartFirmadorRemote() {
        return startFimadorRemote;
    }

    public String getOrigin() {
        String origin = System.getProperty("jnlp.remoteOrigin");
        if (origin == null) {
            origin = "http://localhost:" + portNumber;
        }

        int httpIndex = origin.indexOf("http");
        if (httpIndex > 0) {
            origin = origin.substring(httpIndex);
        }
        int hash = origin.indexOf('#');
        if (hash != -1) {
            origin = origin.substring(0, hash);
        }

        try {
            URL url = new URI(origin).toURL();
            String port = (url.getPort() == -1) ? "" : ":" + url.getPort();
            return url.getProtocol() + "://" + url.getHost() + port;
        } catch (Exception e) {
            return origin;
        }
    }

    public int getRemotePort() {
        String origin = System.getProperty("jnlp.remoteOrigin");
        String[] metadata = processOrigin(origin, 3516);
        return Integer.parseInt(metadata[1]);
    }

    public boolean isMinimizeGui() {
        String origin = System.getProperty("jnlp.remoteOrigin");
        String[] metadata = processOrigin(origin, 3516);
        return Boolean.parseBoolean(metadata[2]);
    }

    public String[] processOrigin(String origin, int defaultPort) {
        // metadata[0] = url, [1] = port, [2] = flag extra
        String[] metadata = { "http://localhost", String.valueOf(defaultPort), "false" };
        if (origin == null)
            return metadata;

        String withoutprotocol = origin;
        if (origin.startsWith("firmador:")) {
            withoutprotocol = origin.substring("firmador:".length());
        }
        String[] parts = withoutprotocol.split("#");

        // parts[0] siempre es la url
        if (parts.length > 0)
            metadata[0] = parts[0];
        // parts[1] el puerto, si existe
        if (parts.length > 1)
            metadata[1] = parts[1];
        // parts[2] el flag extra, si existe
        if (parts.length > 2)
            metadata[2] = parts[2];

        // Validar que el puerto sea número válido
        try {
            Integer.parseInt(metadata[1]);
        } catch (NumberFormatException e) {
            metadata[1] = String.valueOf(defaultPort);
        }

        return metadata;
    }

    public SignatureLevel getPAdESLevel() {
        SignatureLevel level = SignatureLevel.PAdES_BASELINE_LTA;
        switch (pAdESLevel) {
            case "T":
                level = SignatureLevel.PAdES_BASELINE_T;
                break;
            case "LT":
                level = SignatureLevel.PAdES_BASELINE_LT;
                break;
            case "LTA":
                level = SignatureLevel.PAdES_BASELINE_LTA;
                break;
            default:
                level = SignatureLevel.PAdES_BASELINE_LTA;
                break;
        }
        return level;
    }

    public SignatureLevel getXAdESLevel() {
        SignatureLevel level = SignatureLevel.XAdES_BASELINE_LTA;
        switch (xAdESLevel) {
            case "T":
                level = SignatureLevel.XAdES_BASELINE_T;
                break;
            case "LT":
                level = SignatureLevel.XAdES_BASELINE_LT;
                break;
            case "LTA":
                level = SignatureLevel.XAdES_BASELINE_LTA;
                break;
            default:
                level = SignatureLevel.XAdES_BASELINE_LTA;
                break;
        }
        return level;
    }

    public SignatureLevel getCAdESLevel() {
        SignatureLevel level = SignatureLevel.CAdES_BASELINE_LTA;
        switch (cAdESLevel) {
            case "T":
                level = SignatureLevel.CAdES_BASELINE_T;
                break;
            case "LT":
                level = SignatureLevel.CAdES_BASELINE_LT;
                break;
            case "LTA":
                level = SignatureLevel.CAdES_BASELINE_LTA;
                break;
            default:
                level = SignatureLevel.CAdES_BASELINE_LTA;
                break;
        }
        return level;
    }

    public SignatureLevel getJAdESLevel() {
        SignatureLevel level = SignatureLevel.JAdES_BASELINE_LTA;
        switch (jAdESLevel) {
            case "T":
                level = SignatureLevel.JAdES_BASELINE_T;
                break;
            case "LT":
                level = SignatureLevel.JAdES_BASELINE_LT;
                break;
            case "LTA":
                level = SignatureLevel.JAdES_BASELINE_LTA;
                break;
            default:
                level = SignatureLevel.JAdES_BASELINE_LTA;
                break;
        }
        return level;
    }

    public String getVersion() {
        String versionStr = getClass().getPackage().getImplementationVersion();
        if (versionStr == null)
            versionStr = this.defaultDevelopmentVersion;
        return versionStr.replaceAll("[\\n\\r]", "");
    }

    public String getReleaseUrl() {
        String version = getVersion();
        if (version.contains("SNAPSHOT"))
            return this.releaseSnapshotUrl;
        if (System.getProperty("os.name").toLowerCase().contains("mac"))
            return this.releaseMacUrl;
        return this.releaseUrl;
    }

    public String getReleaseCheckUrl() {
        String version = getVersion();
        if (version.contains("SNAPSHOT")) {
            return "";
        }
        return this.releaseUrlCheck;
    }

    public String getChecksumUrl() {
        String version = getVersion();
        if (version.contains("SNAPSHOT")) {
            return this.checksumSnapshotUrl;
        }
        if (System.getProperty("os.name").toLowerCase().contains("mac"))
            return this.releaseMacChecksumUrl;
        return this.checksumUrl;
    }

    public boolean isOnlyimage() {
        return onlyimage;
    }

    public void setOnlyimage(boolean onlyimage) {
        this.onlyimage = onlyimage;
    }

    public int getExtendedState() {
        int state = Frame.NORMAL;
        switch (startwindowstate) {

            case "MAXIMIZED_BOTH":
                state = Frame.MAXIMIZED_BOTH;
                break;
            case "MAXIMIZED_HORIZ":
                state = Frame.MAXIMIZED_HORIZ;
                break;
            case "MAXIMIZED_VERT":
                state = Frame.MAXIMIZED_VERT;
                break;
            case "NORMAL":
                state = Frame.NORMAL;
                break;
            case "ICONIFIED":
                state = Frame.ICONIFIED;
                break;
            default:
                state = Frame.NORMAL;
                break;
        }

        return state;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public Boolean isSimplifiedMode() {
        return simplified_mode;
    }

    public void setSimplifiedMode(Boolean simplified_mode) {
        this.simplified_mode = simplified_mode;
    }

}
