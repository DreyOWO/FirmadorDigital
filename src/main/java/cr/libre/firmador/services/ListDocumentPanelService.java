package cr.libre.firmador.services;

import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.documents.Document;
import cr.libre.firmador.documents.SupportedMimeTypeEnum;
import cr.libre.firmador.gui.GUISwing;
import cr.libre.firmador.gui.swing.ListDocumentPanel;
import cr.libre.firmador.gui.swing.SelectSignatureTypeDialog;
import cr.libre.firmador.signers.DocumentSigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.invoke.MethodHandles;

public class ListDocumentPanelService {

    private ListDocumentPanel panel;
    static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public ListDocumentPanelService(ListDocumentPanel panel) {
        this.panel = panel;
    }

    public ActionListener getCleanDocumentAction(Document document) {
        return new CleanDocumentAction(document);
    }

    public ActionListener getChooseSaveFileActionListener(Document document) {
        return new ChooseSaveFileActionListener(document);
    }

    public ActionListener getGoToValidateActionListener(Document document, boolean isAction) {
        return new goToValidateActionListener(document, isAction);
    }

    public ActionListener getSignActionListener(Document document) {
        return new SignActionListener(document);
    }

    public ActionListener getGoToSignActionListener(Document document) {
        return new goToSignActionListener(document);
    }

    public ActionListener getSelectDocumentActionListener(Document document, JCheckBox checkBox) {
        return new SelectDocumentActionListener(document, checkBox);
    }

    public ActionListener getChangeFormatActionListener(Document document) {
        return new ChangeFormatActionListener(document);
    }

    private class CleanDocumentAction implements ActionListener {
        private Document document;

        public CleanDocumentAction(Document document) {
            this.document = document;
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (document.isVirtual()) {
                int opcion = JOptionPane.showConfirmDialog(
                        null,
                        MessageUtils.t("list_document_panel_confirm_delete"),
                        MessageUtils.t("list_document_panel_confirm_delete_title"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (opcion == JOptionPane.YES_OPTION) {
                    GUISwing gui = (GUISwing) document.getGUI();
                    try {
                        if (gui.deleteDocument(document, gui.getSmartCardDetector())) {
                            panel.getRealDocuments().remove(document);
                            panel.getSelectedDocuments().remove(document);
                            gui.showNotification(MessageUtils.t("list_document_delete_document"),
                                    GUISwing.NotificationType.SUCCESS);
                        } else {
                            gui.showNotification(MessageUtils.t("list_document_panel_delete_error"),
                                    GUISwing.NotificationType.ERROR);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    return;
                }
            } else {
                panel.getRealDocuments().remove(document);
                panel.getSelectedDocuments().remove(document);
            }

            panel.getGui().clearDone();
            panel.reloadView();
        }

    }

    private class ChooseSaveFileActionListener implements ActionListener {

        private Document document;

        ChooseSaveFileActionListener(Document document) {
            this.document = document;
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            panel.setSelectedDocument(document);
            GUISwing gui = (GUISwing) document.getGUI();
            if (!document.getIsremote()) {
                String savefile = gui.showSaveDialog(document.getPathName(), "-firmado", document.getExtension());
                if (savefile != null) { // a path was selected
                    document.setPathToSave(savefile);
                    panel.reloadView();
                }
            }

        }
    }

    private class goToValidateActionListener implements ActionListener {

        private Document document;
        private boolean isAction = false;

        goToValidateActionListener(Document document, boolean isAction) {
            this.document = document;
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            // TODO Auto-generated method stub
            if (isAction)
                panel.setLastAction("validate");
            panel.setListIndexSelected(panel.getRealDocuments().indexOf(document));
            GUISwing gui = (GUISwing) document.getGUI();
            panel.setSelectedDocument(document);
            boolean needProcess = !document.getDocumentIsValidate();
            gui.loadReportDocument(document, needProcess);
        }
    }

    private class SignActionListener implements ActionListener {

        private Document document;

        SignActionListener(Document document) {
            this.document = document;
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            panel.setSelectedDocument(document);
            panel.reloadView();
            if (!document.getDocumentIsValidate() && !document.isVirtual()) {
                panel.setLastAction("sign");
                new goToValidateActionListener(document, false).actionPerformed(null);
            } else {
                panel.setLastAction("na");
                GUISwing gui = (GUISwing) document.getGUI();
                panel.setListIndexSelected(0);
                gui.signDocument(document);
            }
        }

    }

    private class goToSignActionListener implements ActionListener {

        private Document document;

        goToSignActionListener(Document document) {
            this.document = document;
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            // TODO Auto-generated method stub
            panel.setSelectedDocument(document);
            panel.reloadView();
            if (!document.getDocumentIsValidate() && !document.isVirtual()) {
                panel.setLastAction("preview");
                new goToValidateActionListener(document, false).actionPerformed(null);
                return;
            }
            panel.setLastAction("na");
            GUISwing gui = (GUISwing) document.getGUI();
            if (!document.getIsReady()) {
                gui.doPreview(document);
            }
            gui.loadActiveDocument(document);
            gui.displayFunctionality("sign");
        }

    }

    private class SelectDocumentActionListener implements ActionListener {
        private final Document document;
        private final JCheckBox checkBox;

        public SelectDocumentActionListener(Document document, JCheckBox checkBox) {
            this.document = document;
            this.checkBox = checkBox;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            LOG.info("Document selected: " + document.getPathName() + " Selected: " + checkBox.isSelected());
            if (checkBox.isSelected()) {
                panel.getAllSelectedDocuments().add(document);
            } else {
                panel.getAllSelectedDocuments().remove(document);
            }
            panel.setSelectedDocument(document);
        }
    }

    private class ChangeFormatActionListener implements ActionListener {

        private Document document;

        ChangeFormatActionListener(Document document) {
            this.document = document;
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            panel.setSelectedDocument(document);
            GUISwing gui = (GUISwing) document.getGUI();
            SupportedMimeTypeEnum mimeType = document.getMimeType();
            DocumentSigner newSigner = SelectSignatureTypeDialog.show(gui, mimeType, document.getSigner(), document);
            if (newSigner != null) {
                document.setSigner(newSigner);
            }
        }
    }

}
