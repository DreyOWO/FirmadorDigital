package cr.libre.firmador.services;

import com.formdev.flatlaf.util.SystemInfo;
import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.documents.MimeTypeDetector;
import cr.libre.firmador.documents.SupportedMimeTypeEnum;
import cr.libre.firmador.gui.swing.CopyableJLabel;
import eu.europa.esig.dss.model.DSSDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

public class UtilsService {
    final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public UtilsService() {
    }

    public static List<String> getFileArgs(String[] args) {
        List<String> files = new ArrayList<>();
        if (args != null) {
            for (String arg : args) {
                if (!arg.startsWith("-")) {
                    File file = new File(arg);
                    if (arg.contains(".") && !arg.endsWith(File.separator) && (!file.exists() || file.isFile())) {
                        files.add(arg);
                    }
                }
            }
        }
        return files;
    }

    public String getExtension(DSSDocument toSignDocument) {
        String extension = "";
        if (toSignDocument != null) {
            SupportedMimeTypeEnum mimeType = MimeTypeDetector.detect(toSignDocument);
            if (mimeType.isXML())
                extension = ".xml";
            else if (mimeType.isOpenDocument()) {
                extension = "." + mimeType.getExtension().toLowerCase();
            } else if (mimeType.isPDF() || mimeType.isOpenxmlformats()) {
                extension = "." + mimeType.getExtension().toLowerCase();
            } else {
                extension = ".asice";
            }
        }
        return extension;
    }

    public void showError(Throwable error, boolean closed) {
    String message = error.getLocalizedMessage();
    int messageType = JOptionPane.ERROR_MESSAGE;
    String className = error.getClass().getName();
    switch (className) {
        case "java.lang.NoSuchMethodError":
            message = MessageUtils.t("guiswing_show_error_nosuchmethoderror");
            break;
        case "java.security.ProviderException":
            message = MessageUtils.t("guiswing_show_error_providerexception");
            break;
        case "java.security.NoSuchAlgorithmException":
            message = MessageUtils.t("guiswing_show_error_nosuchalgorithmexception");
            break;
        case "sun.security.pkcs11.wrapper.PKCS11Exception":
            message = handlePKCS11Exception(message);
            if (message.contains("guiswing_show_error_pkcs11_pinincorrect")) {
                messageType = JOptionPane.WARNING_MESSAGE;
            }
            break;
        case "java.io.IOException":
            if (message.contains("asepkcs") || message.contains("libASEP11")) {
                message = MessageUtils.t("guiswing_show_error_installers");
            }
            break;
        default:
            message = String.format(MessageUtils.t("guiswing_show_error_default"), className, message);
            break;
    }
    LOG.error(MessageUtils.t("guiswing_show_error_logmessage") + message);
    error.printStackTrace();
    JOptionPane.showMessageDialog(null, new CopyableJLabel(message),
            MessageUtils.t("guiswing_show_error_dialog_title"), messageType);
    if (closed)
        if (messageType == JOptionPane.ERROR_MESSAGE)
            System.exit(0);
}

private String handlePKCS11Exception(String message) {
    switch (message) {
        case "CKR_GENERAL_ERROR":
            if (SystemInfo.isMacOS) {
                return "Error genérico del controlador de tarjetas.<br>" +
                        "Si se está ejecutando Agente GAUDI, debe hacer clic en el icono de Agente GAUDI<br>" +
                        "de la barra superior derecha. En el menú que aparece, elegir 'Salir'.<br>" +
                        "Esto permitirá que Firmador funcione correctamente.";
            } else {
                return MessageUtils.t("guiswing_show_error_pkcs11_general");
            }
        case "CKR_SLOT_ID_INVALID":
            return MessageUtils.t("guiswing_show_error_pkcs11_slotinvalid");
        case "CKR_PIN_INCORRECT":
            return MessageUtils.t("guiswing_show_error_pkcs11_pinincorrect");
        case "CKR_PIN_LOCKED":
            return MessageUtils.t("guiswing_show_error_pkcs11_pinlocked");
        case "CKR_PIN_LEN_RANGE":
            return MessageUtils.t("guiswing_show_error_pkcs11_pintooshort");
        case "0x80000066": // CKR_FUNCTION_FAILED
        case "CKR_FUNCTION_FAILED":
            return MessageUtils.t("guiswing_show_error_pkcs11_update_plugin");
        default:
            return String.format(MessageUtils.t("guiswing_show_error_pkcs11_default"), 
                    "sun.security.pkcs11.wrapper.PKCS11Exception", message);
    }
}
}
