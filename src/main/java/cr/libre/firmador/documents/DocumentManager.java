package cr.libre.firmador.documents;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.gui.GUIInterface;
import cr.libre.firmador.gui.swing.SignProgressDialogWorker;
import cr.libre.firmador.previewers.PreviewScheduler;
import cr.libre.firmador.signers.FirmadorUtils;
import cr.libre.firmador.signers.SignerScheduler;
import cr.libre.firmador.validators.ValidateScheduler;
import eu.europa.esig.dss.model.DSSDocument;

public class DocumentManager implements DocumentChangeListener {
	final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	public SignProgressDialogWorker progressDialogWorker;
	private ValidateScheduler validatescheduler;
	private PreviewScheduler previewScheduler;
	private SignerScheduler signerScheduler;
	private List<String> currentSavedFilePath = new ArrayList<String>();
	private GUIInterface gui;
	private Integer preview_per_doc = 1;

	public DocumentManager(GUIInterface gui) {
		this.gui = gui;
		previewScheduler = new PreviewScheduler(gui);
		previewScheduler.start();
		validatescheduler = new ValidateScheduler(gui);
		validatescheduler.start();
		progressDialogWorker = new SignProgressDialogWorker();
		progressDialogWorker.execute();
		signerScheduler = new SignerScheduler(gui, progressDialogWorker);
		signerScheduler.start();
	}

	public void processDocument(List<Document> docs, int limit_to) {
		preview_per_doc = 0;
		if (docs.size() < limit_to || limit_to == 0) {
			ProcessDocumentWorker procesor = new ProcessDocumentWorker(docs, this, gui);
			procesor.execute();
		} else {
			gui.previewAllDone();
			gui.showMessage(String.format("Por razones de rendimiento no se procesan mas de %s documentos", limit_to));

		}
	}

	public void endProcessDocument() {
		preview_per_doc = 1;
		gui.previewAllDone();
	}

	public void scheduleListofDocuments(List<Document> docs) {
		signerScheduler.addDocuments(docs);
	}

	public void schedulePreview(Document doc) {
		previewScheduler.addDocument(doc);
	}

	@Override
	public void previewDone(Document document) {
		// TODO Auto-generated method stub

	}

	@Override
	public void previewAllDone() {
		if (preview_per_doc > 0)
			gui.previewAllDone();

	}

	@Override
	public void validateDone(Document document) {
		// TODO Auto-generated method stub

	}

	@Override
	public void validateAllDone() {
		// TODO Auto-generated method stub

	}

	@Override
	public void signDone(Document document) {
		DSSDocument signedDocument = document.getSignedDocument();
		// String fileName = document.getPathToSaveName(); //
		// addSuffixToFilePath(document.getPathName(), "-firmado");
		String pathToSave = document.getPathToSave();
		if (!document.getSignwithErrors() && signedDocument != null) {
			try {
				signedDocument.save(pathToSave);
				currentSavedFilePath.add(pathToSave);
				gui.signDone(document);
			} catch (IOException e) {
				LOG.error("Error Firmando documentos", e);
				gui.showError(FirmadorUtils.getRootCause(e));
			}
		}

	}

	@Override
	public void signAllDone() {
		String paths = "";
		File pfile;
		for (String path : currentSavedFilePath) {
			pfile = new File(path);
			paths += "<a href=\"" + pfile.toURI().normalize() + "\">" + path + "</a><br>";
		}
		currentSavedFilePath.clear();

		if (!paths.isEmpty())
			gui.showMessage(MessageUtils.t("guiswing_dialog_document_success") + paths);

	}

	@Override
	public void extendsDone(Document document) {
		// TODO Auto-generated method stub

	}

	@Override
	public void clearDone() {
		gui.clearDone();

	}
}
