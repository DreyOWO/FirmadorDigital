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

package cr.libre.firmador.gui;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore.PasswordProtection;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import cr.libre.firmador.Settings;
import cr.libre.firmador.cards.CardSignInfo;
import cr.libre.firmador.cards.SmartCardDetector;
import cr.libre.firmador.documents.Document;
import cr.libre.firmador.documents.DocumentChangeListener;
import cr.libre.firmador.gui.shell.RequestsShell;
import cr.libre.firmador.gui.shell.RequestsShell.SignCommand;
import cr.libre.firmador.gui.shell.RequestsShell.SignRemoteCommand;
import cr.libre.firmador.gui.shell.RequestsShell.ValidateCommand;
import cr.libre.firmador.plugins.PluginManager;
import cr.libre.firmador.remote.FirmadorRemoteDocument;
import cr.libre.firmador.remote.RemoteSignatureValueDTO;
import cr.libre.firmador.gui.shell.ResponsesShell;
import cr.libre.firmador.signers.BasicSigner;
import cr.libre.firmador.signers.FirmadorUtils;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.ws.dto.RemoteDocument;
import eu.europa.esig.dss.ws.dto.SignatureValueDTO;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;

import static cr.libre.firmador.gui.shell.UtilsShell.*;

public class GUIShell implements GUIInterface, DocumentChangeListener {

    //private Settings settings;
    private PluginManager pluginManager;
    private SmartCardDetector smartCardDetector = new SmartCardDetector();

    public void loadGUI() {
        System.out.println("Firmador Shell - Escuchando comandos");
        System.out.println("Comandos disponibles: sign|<ruta_archivo>|<pin>, exit, help");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        boolean running = true;

        while (running) {
            try {
                System.out.print("> ");
                String input = reader.readLine();

                if (input == null || input.trim().isEmpty()) {
                    continue;
                }

                String[] parts = input.trim().split("\\|", 3);

                if (parts.length == 0) {
                    continue;
                }

                String command = parts[0].toLowerCase();

                switch (command) {
                    case "sign":
                        if (parts.length < 2) {
                            System.out.println("ERROR: Uso incorrecto. Formato: sign|<ruta_archivo>|<pin>");
                        } else {
                            RequestsShell.ListSignCommand signCommand = parseSingCommand(parts[1]);
                            executeSign(signCommand, parts[2].toCharArray());
                        }
                        break;
                    case "signremote":
                        if (parts.length < 2) {
                            System.out.println("ERROR: Uso incorrecto. Formato: signremote|<ruta_archivo>|<pin>");
                        } else {
                            RequestsShell.ListSignRemoteCommand signRemoteCommand = parseSignRemoteCommand(parts[1]);
                            executeSignRemote(signRemoteCommand, parts[2].toCharArray());
                        }
                        break;
                    case "validate":
                        if (parts.length < 2) {
                            System.out.println("ERROR: Uso incorrecto. Formato: validate|<ruta_archivo>");
                        } else {
                            RequestsShell.ListValidateCommand validateCommand = parseValidate(parts[1]);
                            executeValidate(validateCommand);
                        }
                        break;
                    case "getcertificates":
                        executeGetCertificates(parts.length >= 2 ? parts[1] : null);
                        break;
                    case "preview":
                        RequestsShell.PreviewCommand previewCommand = parsePreviewCommand(parts[1]);
                        executePreview(previewCommand);
                        break;
                    case "help":
                        showHelp();
                        break;
                    case "exit":
                    case "quit":
                        running = false;
                        System.out.println("Saliendo...");
                        System.exit(0);
                        break;

                    default:
                        System.out.println("ERROR: Comando no reconocido. Escriba 'help' para ver comandos disponibles.");
                        break;
                }

            } catch (IOException e) {
                System.out.println("ERROR: Error leyendo comando - " + e.getMessage());
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void setArgs(String[] args) {

    }

    @Override
    public void showError(Throwable error) {

    }

    private void executePreview(RequestsShell.PreviewCommand command) {
        try {
            boolean isBase64 = command.getBase64Document() != null && !command.getBase64Document().isEmpty();
            Document doc = null;
            if (!isBase64) {
                if ((command.getFilePath() == null || command.getFilePath().isEmpty())) {
                    System.out.println("ERROR");
                    System.out.println("ERROR: La ruta del archivo no puede estar vacía.");
                    return;
                }
                File file = new File(command.getFilePath());
                if (!file.exists()) {
                    System.out.println("ERROR");
                    System.out.println("ERROR: El archivo no existe - " + command.getFilePath());
                    return;
                }
                if (!file.canRead()) {
                    System.out.println("ERROR");
                    System.out.println("ERROR: No se puede leer el archivo - " + command.getFilePath());
                    return;
                }
                doc = new Document(this, command.getFilePath());
            }else{
                doc = new Document(this, Base64.getDecoder().decode(command.getBase64Document()), "document.pdf", 0);
            }
            doc.loadPreview();
            PDFRenderer render = doc.getPreviewManager().getRender();
            List<String> imagesBase64 = new ArrayList<>();
            int totalPages = doc.getNumberOfPages();
            for (int i = 0; i < totalPages; i++) {
                BufferedImage image = render.renderImage(i, 1);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                imagesBase64.add(base64Image);
            }
            ResponsesShell.PreviewDocumentResponse response = new ResponsesShell.PreviewDocumentResponse(imagesBase64);
            if (command.getFileOutput() != null && !command.getFileOutput().isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                try {
                    File outputFile = new File(command.getFileOutput());

                    // Crear directorios padres si no existen
                    File parentDir = outputFile.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }

                    // Escribir JSON al archivo
                    mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, response);

                    System.out.println("SUCCESS");
                    System.out.println("Previsualización guardada en: " + command.getFileOutput());
                    return;
                } catch (IOException e) {
                    System.out.println("ERROR");
                    System.out.println("ERROR: No se pudo escribir el archivo de salida - " + e.getMessage());
                }
            }
            System.out.println("SUCCESS");
        } catch (IOException e) {
            System.out.println("ERROR");
            System.out.println("ERROR: Error de I/O - " + e.getMessage());
        } catch (Exception e) {
            System.out.println("ERROR");
            System.out.println("ERROR: " + FirmadorUtils.getRootCause(e));
        }
    }

    private void executeGetCertificates(String fileOutput) {
        try {
            SmartCardDetector detector = new SmartCardDetector();
            List<CardSignInfo> cards = detector.readListSmartCard();
            ObjectMapper certMapper = new ObjectMapper();
            if (fileOutput != null && !fileOutput.isEmpty()) {
                try {
                    File outputFile = new File(fileOutput);

                    // Crear directorios padres si no existen
                    File parentDir = outputFile.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }

                    // Escribir JSON al archivo
                    certMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, cards);

                    System.out.println("SUCCESS");
                    System.out.println("Certificados guardados en: " + fileOutput);
                    return;
                } catch (IOException e) {
                    System.out.println("ERROR: No se pudo escribir el archivo de salida - " + e.getMessage());
                }
            }

            System.out.println("SUCCESS");
        } catch (Exception e) {
            System.out.println("ERROR");
            System.out.println("ERROR: " + FirmadorUtils.getRootCause(e));
        } catch (Throwable e) {
            System.out.println("ERROR");
            throw new RuntimeException(e);
        }
    }

    private void executeSign(RequestsShell.ListSignCommand commands, char[] pin) {
        try {
            // Verificar que el archivo existe
            List<ResponsesShell.ResponseSignDocument> results = new ArrayList<>();
            for (SignCommand command : commands.getCommands()) {
                File file = new File(command.filePath);

                if (!file.exists()) {
                    System.out.println("ERROR");
                    System.out.println("ERROR: El archivo no existe - " + command.filePath);
                    return;
                }

                if (!file.canRead()) {
                    System.out.println("ERROR");
                    System.out.println("ERROR: No se puede leer el archivo - " + command.filePath);
                    return;
                }

                // Crear el documento usando el nuevo constructor
                Document doc = new Document(this, command.filePath);
                doc.setSettings(command.settings);

                // Crear CardSignInfo con el PIN proporcionado (ya es char[])
                PasswordProtection passwordProtection = new PasswordProtection(pin);
                CardSignInfo card = new CardSignInfo(passwordProtection);

                // Firmar el documento
                doc.sign(card);
                card.destroyPin();

                DSSDocument signedDocument = doc.getSignedDocument();

                if (signedDocument != null) {
                    // Guardar en archivo temporal para leer los bytes
                    Path tempOutput = Files.createTempFile("firmador_output_", ".pdf");
                    signedDocument.save(tempOutput.toString());

                    // Leer el documento firmado y convertirlo a base64
                    byte[] signedBytes = Files.readAllBytes(tempOutput);
                    String base64Signed = Base64.getEncoder().encodeToString(signedBytes);
                    results.add(new ResponsesShell.ResponseSignDocument(base64Signed, command.getExternalId()));
                } else {
                    System.out.println("ERROR");
                    System.out.println("ERROR: No se pudo generar el documento firmado");
                }
            }
            ResponsesShell.ResponseListSignDocument finalResult = new ResponsesShell.ResponseListSignDocument(results);
            // Crear JSON con el documento firmado
            ObjectMapper mapper = new ObjectMapper();
            if (commands.getFileOutput() != null && !commands.getFileOutput().isEmpty()) {
                try {
                    File outputFile = new File(commands.getFileOutput());

                    File parentDir = outputFile.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }

                    mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, finalResult);

                    System.out.println("SUCCESS");
                    System.out.println("Documento firmado guardado en: " + commands.getFileOutput());
                } catch (IOException e) {
                    System.out.println("ERROR");
                    System.out.println("ERROR: No se pudo escribir el archivo de salida - " + e.getMessage());
                }
            } else {
                System.out.println("ERROR");
                System.out.println("ERROR: No se proporcionó fileOutput para guardar el resultado.");
            }
        } catch (IOException e) {
            System.out.println("ERROR");
            System.out.println("ERROR: Error de I/O - " + e.getMessage());
        } catch (Exception e) {
            System.out.println("ERROR");
            System.out.println("ERROR: " + FirmadorUtils.getRootCause(e));
        }
    }

    private void executeSignRemote(RequestsShell.ListSignRemoteCommand commands, char[] pin) {
        try {
            PasswordProtection passwordProtection = new PasswordProtection(pin);
            CardSignInfo card = this.getCardInfoByNumberID(commands.getSerialnumber());
            card.setPin(passwordProtection);
            //if (card != null) { //FIXME does this comparison have sense? see the FIXME in the else below
                //Creamos el documento
                List<ResponsesShell.ResponseRemoteDocument> results = new ArrayList<>();
                for (SignRemoteCommand command : commands.getCommands()) {
                    byte[] bytes = Base64.getDecoder().decode(command.getBase64Document());
                    Document doc = new Document(this, bytes, command.getDocumentName(), 0);
                    doc.setSettings(command.getSettings());
                    FirmadorRemoteDocument remoteDoc = doc.getTobeSignedRemote(card);
                    BasicSigner basicSigner = new BasicSigner(this);
                    SignatureValueDTO signature = basicSigner.sign(card, remoteDoc.getTobesigned());
                    RemoteSignatureValueDTO rsignature = new RemoteSignatureValueDTO(remoteDoc, signature);
                    RemoteDocument remoteDocument = doc.signRemoteDocument(card, rsignature);
                    results.add(new ResponsesShell.ResponseRemoteDocument(remoteDocument, command.getExternalId()));
                }

                ResponsesShell.ResponseListRemoteDocumet finalResult = new ResponsesShell.ResponseListRemoteDocumet(results);

                ObjectMapper responsemapper = new ObjectMapper();
                if (commands.getFileOutput() != null && !commands.getFileOutput().isEmpty()) {
                    try {
                        File outputFile = new File(commands.getFileOutput());

                        // Crear directorios padres si no existen
                        File parentDir = outputFile.getParentFile();
                        if (parentDir != null && !parentDir.exists()) {
                            parentDir.mkdirs();
                        }

                        // Escribir JSON al archivo
                        responsemapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, finalResult);

                        System.out.println("SUCCESS");
                        System.out.println("Firma remota guardada en: " + commands.getFileOutput());
                        return;
                    } catch (IOException e) {
                        System.out.println("ERROR");
                        System.out.println("ERROR: No se pudo escribir el archivo de salida - " + e.getMessage());
                    }
                } else {
                    System.out.println("ERROR");
                    System.out.println("ERROR: No se proporcionó fileOutput para guardar el resultado.");
                }
            /*} else { // FIXME Eclipse compiler says this is dead code. Is it possible that an object setter null its own object?
                System.out.println("ERROR");
                System.out.println("ERROR: No se encontró la tarjeta con el serial proporcionado.");
            }*/
        } catch (Exception e) {
            System.out.println("ERROR");
            System.out.println("ERROR: " + FirmadorUtils.getRootCause(e));
        }
    }

    private void executeValidate(RequestsShell.ListValidateCommand commands) {
        try {
            List<ResponsesShell.ValidateDocumentResponse> results = new ArrayList<>();
            for (ValidateCommand command : commands.getCommands()) {
                // Verificar que el archivo existe
                File file = new File(command.getFilePath());

                if (!file.exists()) {
                    System.out.println("ERROR");
                    System.out.println("ERROR: El archivo no existe - " + command.getFilePath());
                    return;
                }

                if (!file.canRead()) {
                    System.out.println("ERROR");
                    System.out.println("ERROR: No se puede leer el archivo - " + command.getFilePath());
                    return;
                }

                // Crear el documento usando el nuevo constructor
                Document doc = new Document(this, command.getFilePath());
                doc.validate();

                results.add(new ResponsesShell.ValidateDocumentResponse(command.getExternalId(), doc.getPlainReport()));
            }

            ResponsesShell.ListValidateDocumentResponse response = new ResponsesShell.ListValidateDocumentResponse(results);

            ObjectMapper mapper = new ObjectMapper();
            // Si se especificó fileOutput, guardar JSON en ese archivo
            if (commands.getFileOutput() != null && !commands.getFileOutput().isEmpty()) {
                try {
                    File outputFile = new File(commands.getFileOutput());
                    // Crear directorios padres si no existen
                    File parentDir = outputFile.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }
                    // Escribir JSON al archivo - pasar el objeto directamente
                    mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, response);
                    System.out.println("SUCCESS");
                    System.out.println("Reporte guardado en: " + commands.getFileOutput());
                } catch (IOException e) {
                    System.out.println("ERROR");
                    System.out.println("ERROR: No se pudo escribir el archivo de salida - " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.out.println("ERROR");
            System.out.println("ERROR: Error de I/O - " + e.getMessage());
        } catch (Exception e) {
            System.out.println("ERROR");
            System.out.println("ERROR: " + FirmadorUtils.getRootCause(e));
        } catch (Throwable e) {
            System.out.println("ERROR");
            throw new RuntimeException(e);
        }
    }

    private void showHelp() {
        System.out.println("Comandos disponibles:");
        System.out.println("  sign|<json_file>|<pin>           - Firmar documento(s) desde JSON");
        System.out.println("  signremote|<json_file>|<pin>     - Firmar documento(s) remotamente");
        System.out.println("  validate|<json_file>              - Validar documento(s)");
        System.out.println("  getcertificates|<output_file>     - Obtener certificados disponibles");
        System.out.println("  help                              - Mostrar esta ayuda");
        System.out.println("  exit                              - Salir del programa");
        System.out.println("");
        System.out.println("Ejemplos:");
        System.out.println("  sign|config.json|1234");
        System.out.println("  signremote|remote_config.json|1234");
        System.out.println("  validate|validate_config.json");
        System.out.println("  getcertificates|certificates.json");
        System.out.println("");
        System.out.println("Salida exitosa:");
        System.out.println("  SUCCESS");
        System.out.println("  <mensaje_de_confirmacion>");
        System.out.println("");
        System.out.println("Salida con error:");
        System.out.println("  ERROR");
        System.out.println("  ERROR: <mensaje_de_error>");
    }

    private CardSignInfo getCardInfoByNumberID(String serialnumber) {
        List<CardSignInfo> cards = null;
        CardSignInfo result = null;
        String card_serialnumber;
        try {
            cards = smartCardDetector.readListSmartCard();
            smartCardDetector.setCardInfo(cards);
            for (CardSignInfo card : cards) {
                card_serialnumber = card.getCertificate().getSerialNumber().toString();
                if (card_serialnumber.equals(serialnumber)) {
                    result = card;
                    break;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public void showMessage(String message) {
        System.out.println(message);
    }

    @Override
    public void showErrorAlert(String title, String message) {

    }

    @Override
    public String getDocumentToSign() {
        return "";
    }

    @Override
    public String getPathToSave(String extension) {
        return "";
    }

    @Override
    public CardSignInfo getPin() {
        return null;
    }

    @Override
    public void configurePluginManager() {
        this.pluginManager.startLogging();
    }

    @Override
    public void setPluginManager(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Override
    public void extendDocument() {
    }

    @Override
    public String getPathToSaveExtended(String extension) {
        return null;
    }

    @Override
    public void displayFunctionality(String functionality) {
        System.out.println(functionality);

    }

    @Override
    public void nextStep(String msg) {
        System.out.println(msg);

    }

    public void previewDone(Document document) {
    }

    ;

    public void validateDone(Document document) {
    }

    ;

    public void signDone(Document document) {
    }

    ;

    public void extendsDone(Document document) {
    }

    @Override
    public void validateAllDone() {
        // TODO Auto-generated method stub

    }

    @Override
    public void signAllDone() {
        // TODO Auto-generated method stub

    }

    @Override
    public void doPreview(Document document) {
        // TODO Auto-generated method stub

    }

    @Override
    public Settings getCurrentSettings() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void signDocument(Document document) {
        // TODO Auto-generated method stub

    }

    @Override
    public void clearDone() {
        // TODO Auto-generated method stub

    }

    @Override
    public void previewAllDone() {
        // TODO Auto-generated method stub

    }


    @Override
    public List<Document> addDocuments(File[] files) {
        return null;
        // TODO Auto-generated method stub
    }

    @Override
    public void setSmartCardDetector(SmartCardDetector detector) {
        // TODO Auto-generated method stub

    }

    @Override
    public void loadRemoteDocument(String fileName) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addDirectories(File[] files) {
        //TODO Auto-generated method stub
    }

    @Override
    public void loadDocuments(List<Document> documents, boolean doPreview) {
    }

    @Override
    public List<Document> addDocumentsSynchronous(File[] files) {
        return List.of();
    }

    ;
}
