package cr.libre.firmador.services;

import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.Settings;
import cr.libre.firmador.documents.Document;
import cr.libre.firmador.documents.DocumentManager;
import cr.libre.firmador.gui.GUIInterface;
import cr.libre.firmador.plugins.PluginManager;
//import cr.libre.firmador.remote.FirmadorRemoteDocument;
import cr.libre.firmador.signers.*;
import eu.europa.esig.dss.enumerations.MimeType;
import eu.europa.esig.dss.enumerations.MimeTypeEnum;
import eu.europa.esig.dss.model.DSSDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentService {
    //private List<Document> virtualDocumentsToSign = new ArrayList<>();
    //private List<FirmadorRemoteDocument> remoteDocuments = new ArrayList<>();
    final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private PluginManager pluginManager;
    private DocumentManager docmanager;
    private GUIInterface gui;
    //private Settings settings;

    public DocumentService(PluginManager pluginManager, DocumentManager docmanager, GUIInterface gui, Settings settings) {
        this.pluginManager = pluginManager;
        this.docmanager = docmanager;
        this.gui = gui;
        //this.settings = settings;
    }

    public List<Document> addDocuments(File[] files) {
        SwingWorker<List<Document>, Void> worker = new SwingWorker<List<Document>, Void>() {
            @Override
            protected List<Document> doInBackground() throws Exception {
                List<Document> docs = new ArrayList<>();
                for (File file : files) {
                    String pathname = file.getAbsolutePath();
                    int lastSlash = Math.max(pathname.lastIndexOf("/"), pathname.lastIndexOf("\\"));
                    int dotIndex = pathname.lastIndexOf(".");

                    if (dotIndex <= lastSlash) {
                        String errorMsg = MessageUtils.t("guiswing_dialog_document_not_valid_extension") +
                            pathname + " " +
                            MessageUtils.t("guiswing_dialog_document_not_valid_extension2");
                        gui.showMessage(errorMsg);
                    } else {
                        Document document = new Document(gui, file.getAbsolutePath());
                        document.registerListener(docmanager);
                        pluginManager.registerDocument(document);
                        docs.add(document);
                    }
                }
                return docs;
            }

            @Override
            protected void done() {
                try {
                    List<Document> docs = get();
                    gui.loadDocuments(docs, true);
                } catch (Exception e) {
                    LOG.error(MessageUtils.t("guiswing_error_loading_documents"), e);
                    gui.showMessage(MessageUtils.t("guiswing_error_loading_documents") + ": " + e.getMessage());
                }
            }
        };
        worker.execute();
        return null;
    }

    public List<Document> addDocumentsSynchronous(File[] files){
        try {
            List<Document> docs = new ArrayList<>();

            for (File file : files) {
                String pathname = file.getAbsolutePath();
                int lastSlash = Math.max(pathname.lastIndexOf("/"), pathname.lastIndexOf("\\"));
                int dotIndex = pathname.lastIndexOf(".");

                if (dotIndex <= lastSlash) {
                    String errorMsg = MessageUtils.t("guiswing_dialog_document_not_valid_extension") +
                        pathname + " " +
                        MessageUtils.t("guiswing_dialog_document_not_valid_extension2");
                    gui.showMessage(errorMsg);
                } else {
                    Document document = new Document(gui, file.getAbsolutePath());
                    document.registerListener(docmanager);
                    pluginManager.registerDocument(document);
                    docs.add(document);
                }
            }

            gui.loadDocuments(docs, false);

            return docs;
        } catch (Exception e) {
            LOG.error(MessageUtils.t("guiswing_error_loading_documents"), e);
            gui.showMessage(MessageUtils.t("guiswing_error_loading_documents") + ": " + e.getMessage());
            return null;
        }
    }

    public ByteArrayOutputStream extendDocument(DSSDocument toExtendDocument, boolean asbytes, String fileName){
        if (toExtendDocument == null) return null;
        DSSDocument extendedDocument = null;
        ByteArrayOutputStream outdoc = null;
        MimeType mimeType = toExtendDocument.getMimeType();
        if (mimeType == MimeTypeEnum.PDF) {
            FirmadorPAdES firmador = new FirmadorPAdES(gui);
            extendedDocument = firmador.extend(toExtendDocument);
        } else if (mimeType == MimeTypeEnum.ODG || mimeType == MimeTypeEnum.ODP || mimeType == MimeTypeEnum.ODS || mimeType == MimeTypeEnum.ODT) {
            FirmadorOpenDocument firmador = new FirmadorOpenDocument(gui);
            extendedDocument = firmador.extend(toExtendDocument);
        } else if (mimeType == MimeTypeEnum.XML) {
            FirmadorXAdES firmador = new FirmadorXAdES(gui, true);
            extendedDocument = firmador.extend(toExtendDocument);
        } else if (mimeType == MimeTypeEnum.JSON) {
            FirmadorJAdES firmador = new FirmadorJAdES(gui);
            extendedDocument = firmador.extend(toExtendDocument);
        } else {
            FirmadorCAdES firmador = new FirmadorCAdES(gui);
            extendedDocument = firmador.extend(toExtendDocument);
        }
        if (extendedDocument != null) {
            if (asbytes) {
                outdoc = new ByteArrayOutputStream();
                try {
                    extendedDocument.writeTo(outdoc);
                } catch (IOException e) {
                    LOG.error(MessageUtils.t("guiswing_error_extending_document"), e);
                    gui.showError(FirmadorUtils.getRootCause(e));
                }
            } else {
                if (fileName == null) fileName = gui.getPathToSaveExtended("");
                else {
                    try {
                        extendedDocument.save(fileName);
                        gui.showMessage(MessageUtils.t("guiswing_document_saved_successfully") + "<br>" + fileName);

                    } catch (IOException e) {
                        LOG.error(MessageUtils.t("guiswing_error_saving_extended"), e);
                        gui.showError(FirmadorUtils.getRootCause(e));
                    }
                }
            }
        }
        return outdoc;
    }

    public List<Document> loadVirtualDocuments(List<Document> docs, List<Document> virtualDocs){
        List<Document> newDocs = new ArrayList<>();
        for (Document doc : docs) {
            boolean found = false;
            for (Document virtualDoc : virtualDocs) {
                if (virtualDoc.getDocumentID().equals(doc.getDocumentID())) {
                    found = true;
                    break;
                }
            }
            if (!found) newDocs.add(doc);
        }
        return newDocs;
    }

    public Document notifyReportDocument(UUID documentId, String report, List<Document> virtualDocs){
        Document doc = null;
        for (Document document : virtualDocs) {
            if (document.getId().equals(documentId)) {
                doc = document;
                doc.setValidating(false);
                break;
            }
        }
        if (doc == null) {return doc;}
        String cleanedReport = report.replaceAll("El documento doc_[0-9]+\\.pdf[^\\n]*", "").trim();

        int firmasCount = 0;
        Matcher matcher = Pattern.compile("Contiene\\s+(\\d+)\\s+firma").matcher(report);
        if (matcher.find()) {
            try {
                firmasCount = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {}
        }
        cleanedReport = cleanedReport.replaceAll("(\n\\s*){2,}", "\n");
        if (firmasCount <= 0) {
            cleanedReport = "Este documento no tiene ninguna firma digital.";
        }
        String htmlReport = "<html>" + cleanedReport.replace("\n", "<br>") + "</html>";
        doc.setAmountOfSignatures(firmasCount);
        doc.setReport(htmlReport);
        return doc;
    }

    public void cancelDocument(UUID documentId, List<Document> virtualDocs){
        Iterator<Document> iterator = virtualDocs.iterator();
        while (iterator.hasNext()) {
            Document document = iterator.next();
            if (document.getId().equals(documentId)) {
                iterator.remove();
                break;
            }
        }
    }
}
