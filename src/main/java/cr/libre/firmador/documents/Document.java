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

package cr.libre.firmador.documents;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cr.libre.firmador.signers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.Settings;
import cr.libre.firmador.SettingsManager;
import cr.libre.firmador.cards.CardSignInfo;
import cr.libre.firmador.gui.GUIInterface;
import cr.libre.firmador.previewers.PreviewerInterface;
import cr.libre.firmador.previewers.PreviewerManager;
import cr.libre.firmador.remote.FirmadorRemoteDocument;
import cr.libre.firmador.remote.RemoteSignatureValueDTO;
import cr.libre.firmador.validators.Validator;
import cr.libre.firmador.validators.ValidatorFactory;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.ws.dto.RemoteDocument;
import eu.europa.esig.dss.ws.signature.dto.parameters.RemoteSignatureParameters;

public class Document {
    final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final int STATUS_TOSIGN = 0;
    public static final int STATUS_SIGNED = 1;
    public static final int STATUS_ERROR_SIGNING = 2;
    private List<DocumentChangeListener> listeners = new ArrayList<DocumentChangeListener>();
    private SupportedMimeTypeEnum mimeType;
    private String pathname;
    private String name;
    private DSSDocument document = null;
    private DSSDocument signedDocument = null;
    private Validator validator;
    private PreviewerInterface preview;
    private Settings settings;
    private DocumentSigner signer;
    private GUIInterface gui;
    private String pathToSave = null;
    private String pathToSaveName = null;
    private String absolutePathToSave = null;
    private boolean isvalid = false;
    private boolean documentIsValidate = false;
    private boolean hasPreviewLoaded = false;
    private boolean isReady = false;
    private String report;
    private boolean signwithErrors = false;
    private CardSignInfo usedcard;
    private boolean showPreview = true;
    private boolean ismasivesign = false;
    private boolean isremote = false;
    private boolean isVirtual = false;
    private String service;
    @SuppressWarnings("unused")
    private VirtualSigner virtualSigner; //FIXME is virtualSigner variable actually used here? and in JSON?
    private int status = 0;
    private byte[] data;
    private final UUID documentID;
    @JsonIgnore
    private RemoteDocument cacheremote = null;
    @JsonIgnore
    private RemoteSignatureParameters remoteParameters = null;
    FirmadorRemoteDocument remoteDoc;
    private String serial;
    private int pages;
    private int amountOfSignatures;
    private boolean validating = false;
    private String expirationDate = "";
    private String origin = "";
    private String createdAt = "";

    public Document(GUIInterface gui, String pathname) {
        LOG.info("Se creó una nueva instancia de Document");
        this.pathname = pathname;
        this.gui = gui;
        File file = new File(pathname);
        name = file.getName();
        mimeType = MimeTypeDetector.detect(pathname);
        validator = ValidatorFactory.getValidator(pathname);
        preview = PreviewerManager.getPreviewManager(mimeType);
        settings = SettingsManager.getInstance().getAndCreateSettings();
        signer = DocumentSignerDetector.getDocumentSigner(gui, settings, mimeType);
        data = null; // I will check it if remote document
        this.documentID = UUID.randomUUID();
    }

    public Document(GUIInterface gui, byte[] data, String name, int status) {
        this.documentID = UUID.randomUUID();
        this.name = name;
        this.gui = gui;
        this.status = status;
        if (status == 0) {
            this.data = data;
        }
        mimeType = MimeTypeDetector.detect(data, name);
        try {
            Path tempFile = Files.createTempFile("doc_", "." + mimeType.getExtension());
            Files.write(tempFile, data, StandardOpenOption.WRITE);
            this.pathname = tempFile.toString();
        } catch (IOException e) {
            LOG.error(name + " -- " + e.getMessage());
            e.printStackTrace();
        }

        Path savedtempFile;
        try {
            savedtempFile = Files.createTempFile("doc_saved_", "." + mimeType.getExtension());
            pathToSaveName = savedtempFile.toString();
        } catch (IOException e) {
            LOG.error(name + " -- " + e.getMessage());
            e.printStackTrace();
        }

        validator = ValidatorFactory.getValidator(this.pathname);
        preview = PreviewerManager.getPreviewManager(mimeType);
        settings = SettingsManager.getInstance().getAndCreateSettings();
        signer = DocumentSignerDetector.getDocumentSigner(gui, settings, mimeType);
        isremote = true;
    }

    public Document(GUIInterface gui, UUID id, String name, String mimetype, String service, int pages, String serial, String origin, String expirationDate, String createdAt) {
        this.documentID = id;
        this.name = name;
        this.gui = gui;
        this.isVirtual = true;
        this.isremote = true;
        this.mimeType = MimeTypeDetector.fromMimeType(mimetype);
        this.pages = pages;
        preview = PreviewerManager.getPreviewManager(this.mimeType);
        settings = SettingsManager.getInstance().getAndCreateSettings();
        this.virtualSigner = new VirtualSigner(gui);
        this.service = service;
        this.serial = serial;
        this.origin = origin;
        this.expirationDate = expirationDate;
        this.createdAt = createdAt;
    }

    public GUIInterface getGUI() {
        return this.gui;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
        // destroy old signer first
        signer = DocumentSignerDetector.getDocumentSigner(gui, settings, mimeType);
    }

    public Settings getSettings() {
        return settings;
    }

    public boolean validate() throws Throwable {
        if (validator != null) {
            if (!documentIsValidate) {
                isvalid = false;
                document = validator.loadDocumentPath(pathname);
                isvalid = validator.isSigned();
                report = validator.getStringReport();
                this.validateDone();
            }
        } else {
            documentIsValidate = true;
        }
        return isvalid;
    }

    public void sign(CardSignInfo card) {
        usedcard = card;
        if (settings != null && settings.signASiC) {
            this.forcesignASiC();
        }
        if (document == null) {
            document = new FileDocument(this.pathname);
        }
        signedDocument = signer.sign(this, card);
        if (signedDocument == null) {
            signwithErrors = true;
            status = STATUS_ERROR_SIGNING;
        }
        if (settings.extendDocument && signedDocument != null) {
            this.extend();
        }
        signDone();
    }

    public void extend() {

        if (mimeType == SupportedMimeTypeEnum.BINARY) {
            ArrayList<DSSDocument> detacheddocs = new ArrayList<DSSDocument>();
            detacheddocs.add(document);
            signer.setDetached(detacheddocs);
        }
        DSSDocument extendDocument = signer.extend(signedDocument);
        if (extendDocument != null) {
            signedDocument = extendDocument;
        }

        extendsDone();
        if (mimeType == SupportedMimeTypeEnum.BINARY) {
            signer.setDetached(null);
        }
    }

    public void setPrincipal() throws Throwable {
        if (!documentIsValidate)
            validate();
        if (!hasPreviewLoaded)
            loadPreview();
    }

    public String getPathName() {
        return this.pathname;
    }

    public String getName() {
        return name;
    }

    public SupportedMimeTypeEnum getMimeType() {
        return mimeType;
    }

    public String getExtension() {
        String extension = "";
        if (mimeType.isXML())
            extension = ".xml";
        else if (mimeType.isPDF() || mimeType.isOpenDocument() || mimeType.isOpenxmlformats()) {
            if (settings.signASiC) {
                extension = ".asice";
            } else {
                extension = "." + mimeType.getExtension().toLowerCase();
            }
        } else {
            extension = ".asice";
        }

        return extension;
    }

    public String getPathToSave() {
        if (pathToSave == null) {
            String extension = getExtension();
            String suffix = "-firmado";

            int lastSlash = pathname.lastIndexOf(File.separator);
            int dotIndex = pathname.lastIndexOf(".");

            if (dotIndex > lastSlash) {
                pathToSave = pathname.substring(0, dotIndex) + suffix + extension;
            } else {
                pathToSave = pathname + suffix + extension;
            }
        }

        return pathToSave;
    }

    public String getPathToSaveName() {
        if (pathToSaveName == null) {
            String extension = mimeType.getExtension();
            if (extension == null) extension = "asice";
            String suffix = "-firmado";
            pathToSaveName = name.substring(0, name.lastIndexOf(".")) + suffix + "." + extension;
        }
        return pathToSaveName;
    }

    public void setPathToSaveName(String pathToSaveName) {
        this.pathToSave = pathToSaveName;
        File filep = new File(pathToSaveName);
        this.pathToSaveName = filep.getName();
    }

    public void setPathToSave(String pathToSave) {
        this.pathToSave = pathToSave;
        File filep = new File(pathToSave);
        this.pathToSaveName = filep.getName();

    }

    public String getReport() {
        return report;
    }

    public String getPlainReport() {
        return MessageUtils.html2txt(report);
    }

    public DSSDocument getDSSDocument() {
        if (document != null)
            return document;
        if (data != null)
            return new InMemoryDocument(data);
        return null;
    }

    public void setDSSDocument(DSSDocument document) {
        this.document = document;
        documentIsValidate = false;
    }

    public void registerListener(DocumentChangeListener listener) {
        listeners.add(listener);
    }

    public void previewDone() {
        hasPreviewLoaded = true;
        checkIsReady();
        for (DocumentChangeListener hl : listeners)
            hl.previewDone(this);
    }

    public void validateDone() {
        documentIsValidate = true;
        checkIsReady();
        for (DocumentChangeListener hl : listeners)
            hl.validateDone(this);

    }

    ;

    public void signDone() {
        for (DocumentChangeListener hl : listeners)
            hl.signDone(this);
        status = STATUS_SIGNED;
    }

    ;

    public void extendsDone() {
        for (DocumentChangeListener hl : listeners)
            hl.extendsDone(this);
        this.status = STATUS_SIGNED;
    }

    ;

    public boolean isValid() {
        return this.isvalid;
    }

    public PreviewerInterface getPreviewManager() {
        return preview;
    }

    public void loadPreview() {
        try {
            if (!isVirtual) {
                preview.loadDocument(pathname);
                preview.getRender();
            }
        } catch (Throwable e) {
            LOG.error("Preview: " + e.getMessage(), e);
        } finally {
            this.previewDone();
        }
    }

    public void loadPreviewRemote(byte[] data) {
        try {
            preview.loadDocument(data);
            preview.getRender();
        } catch (Throwable e) {
            LOG.error("Preview: " + e.getMessage(), e);
        } finally {
            this.previewDone();
        }
    }

    public int amountOfSignatures() {
        if (validator == null)
            return 0;
        return validator.amountOfSignatures();

    }

    public DSSDocument getSignedDocument() {
        return signedDocument;
    }

    public void setSignedDocument(DSSDocument signedDocument) {
        this.signedDocument = signedDocument;
    }

    public boolean getIsReady() {
        return isReady;
    }

    public int getNumberOfPages() {
        return preview.getNumberOfPages();
    }

    private void checkIsReady() {
        if (hasPreviewLoaded && documentIsValidate) {
            isReady = true;
        }
    }

    public void forcesignASiC() {
        signer = new FirmadorASiC(this.gui);
    }

    public void forceCades() {
        signer = new FirmadorCAdES(this.gui);
    }

    public boolean getSignwithErrors() {
        return signwithErrors;
    }

    public void setSignwithErrors(boolean signwithErrors) {
        this.signwithErrors = signwithErrors;
    }

    public CardSignInfo getUsedCard() {
        return usedcard;
    }

    public boolean getShowPreview() {
        return showPreview;
    }

    public void setShowPreview(boolean showPreview) {
        this.showPreview = showPreview;
    }

    public boolean isIsmasivesign() {
        return ismasivesign;
    }

    public void setIsmasivesign(boolean ismasivesign) {
        this.ismasivesign = ismasivesign;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean getIsremote() {
        return isremote;
    }

    public FirmadorRemoteDocument getTobeSignedRemote(CardSignInfo card) {
        return signer.getTobeSignedRemote(this, card);

    }

    public RemoteDocument getRemoteDocument() {
        if (cacheremote == null) {
            cacheremote = new RemoteDocument(data, name);
        }
        return cacheremote;
    }

    public RemoteDocument signRemoteDocument(CardSignInfo card, RemoteSignatureValueDTO signature) {
        return signer.signRemoteDocument(this, card, signature);
    }

    public UUID getDocumentID() {
        return documentID;
    }

    public RemoteSignatureParameters getRemoteParameters() {
        return remoteParameters;
    }

    public void setRemoteParameters(RemoteSignatureParameters remoteParameters) {
        this.remoteParameters = remoteParameters;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public void setSigner(DocumentSigner signer) {
        this.signer = signer;
    }

    public DocumentSigner getSigner() {
        return signer;
    }

    public Validator getValidator() {
        return validator;
    }

    public boolean getDocumentIsValidate() {
        return documentIsValidate;
    }

    public String getPathTOSave() {
        return pathToSave;
    }

    public String getAbsolutePathToSave() {
        return absolutePathToSave;
    }

    public void setAbsolutePathToSave(String absolutePathToSave) {
        this.absolutePathToSave = absolutePathToSave;
    }

    public boolean isVirtual() {
        return isVirtual;
    }

    public void setPreview(PreviewerInterface preview) {
        this.preview = preview;
    }

    public void setMimeType(SupportedMimeTypeEnum mimeType) {
        this.mimeType = mimeType;
    }

    public int getPages(){
        return pages;
    }

    public void setPages(int pages){
        this.pages = pages;
    }

    public String getSerial(){
        return serial;
    }

    public UUID getId() {return documentID;}

    public void setReport(String report) {
        this.report = report;
    }

    public void setAmountOfSignatures(int amountOfSignatures) {
        this.amountOfSignatures = amountOfSignatures;
    }

    public int getAmountOfSignatures() {
        return amountOfSignatures;
    }

    public boolean getValidating(){
        return validating;
    }

    public void setValidating(boolean validating) {
        this.validating = validating;
    }

    public String getOrigin(){
        return origin;
    }

    public void setOrigin(String origin){
        this.origin = origin;
    }

    public String getExpirationDate(){
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate){
        this.expirationDate = expirationDate;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

}
