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

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.*;

import cr.libre.firmador.connections.Connection;
import cr.libre.firmador.connections.ConnectionUtils;
import cr.libre.firmador.connections.GaudiIntegration;
import cr.libre.firmador.connections.RemoteIntegration;
import cr.libre.firmador.gui.swing.*;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.formdev.flatlaf.util.SystemInfo;
import cr.libre.firmador.remote.FirmadorRemoteDocument;
import cr.libre.firmador.services.ConnectionService;
import cr.libre.firmador.services.DocumentService;
import cr.libre.firmador.services.UtilsService;
import cr.libre.firmador.signers.FirmadorUtils;
import cr.libre.firmador.signers.VirtualSigner;
import eu.europa.esig.dss.enumerations.MimeType;
import eu.europa.esig.dss.enumerations.MimeTypeEnum;
import cr.libre.firmador.ConfigListener;
import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.Settings;
import cr.libre.firmador.SettingsManager;
import cr.libre.firmador.SingleInstanceManager;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.pdfbox.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cr.libre.firmador.cards.CardSignInfo;
import cr.libre.firmador.cards.SmartCardDetector;
import cr.libre.firmador.documents.Document;
import cr.libre.firmador.documents.DocumentManager;
import cr.libre.firmador.remote.RemoteHttpWorker;
import cr.libre.firmador.plugins.PluginManager;

public class GUISwing implements GUIInterface, ConfigListener {
    final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Boolean isRemote;
    public JTabbedPane frameTabbedPane;
    private String documenttosign = null;
    private String documenttosave = null;
    private DocumentSelectionGroupLayout docSelector;
    private String fileName;
    private RemoteHttpWorker<Void, byte[]> remote;
    private Settings settings;
    private DSSDocument toSignDocument;
    private SwingMainWindowFrame mainFrame;
    @SuppressWarnings("unused")
    private GaudiIntegration<Void, byte[]> gaudi;
    @SuppressWarnings("unused")
    private RemoteIntegration<Void, byte[]> ucr;
    private SignPanel signPanel;
    private ValidatePanel validatePanel;
    private DirectoryPanel directoryPanel;
    private GUIInterface gui;
    private JScrollPane loggingPane;
    private int tabPosition;
    private ListDocumentPanel listdocumentpanel;
    private DocumentManager docmanager;
    private RemoteDocInformation docinfo;
    private ConnectionPanel connectionPanel;
    private ConfigPanel configPanel;
    private boolean needNavigateToPreviewPanel = false;
    private boolean background = false;
    private SingleInstanceManager instanceManager = new SingleInstanceManager();
    private List<FirmadorRemoteDocument> remoteDocuments = new ArrayList<>();
    private List<Document> virtualDocumentsToSign = new ArrayList<>();
    private DocumentService documentService;
    private ConnectionService connectionService;
    private UtilsService utilsService;

    private Document document;
    private Document simplifiedDocument;
    private LoadProgressDialogWorker loadDialogWorker;
    private boolean forcePreview = false;
    private PluginManager pluginManager;
    private SmartCardDetector cardDetector;
    private TrayIcon trayIcon;
    private volatile boolean windowHidden = false;
    private volatile boolean justShownFromTray = false;
    private String[] args;

    private PopupMenu createPopupMenu() {
        PopupMenu popupMenu = new PopupMenu();
        MenuItem exitItem = new MenuItem(MessageUtils.t("guiswing_exit"));
        exitItem.addActionListener(e -> {
            close();
        });
        popupMenu.add(exitItem);
        return popupMenu;
    }

    public void configureSystray() {
        // Asegurarse de que no estamos en modo headless
        System.setProperty("java.awt.headless", "false");

        if (!SystemTray.isSupported()) {
            LOG.info("SystemTray no está soportado en este sistema.");
            LOG.info("Sistema: " + System.getProperty("os.name"));
            LOG.info("Desktop: " + System.getenv("XDG_CURRENT_DESKTOP"));
            LOG.info("Session: " + System.getenv("XDG_SESSION_TYPE"));
            return;
        }

        SystemTray tray;
        try {
            tray = SystemTray.getSystemTray();
        } catch (UnsupportedOperationException e) {
            LOG.warn("SystemTray.getSystemTray() no disponible: " + e.getMessage());
            return;
        }

        if (trayIcon != null) {
            tray.remove(trayIcon);
            LOG.info("TrayIcon anterior removido.");
        }

        Image image = new ImageIcon(this.getClass().getClassLoader().getResource("firmadorsystray.png")).getImage();

        if (image != null) {
            LOG.info("Icono de bandeja del sistema cargado correctamente.");
        }

        trayIcon = new TrayIcon(image, "Firmador Libre");
        trayIcon.setImageAutoSize(true);
        trayIcon.setPopupMenu(createPopupMenu());

        trayIcon.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                mainFrame.setVisible(true);
                mainFrame.setState(JFrame.NORMAL);
                mainFrame.revalidate();
                mainFrame.repaint();
                mainFrame.setAlwaysOnTop(true);
                mainFrame.toFront();
                WindowAdapter[] focusListener = new WindowAdapter[1];
                focusListener[0] = new WindowAdapter() {
                    @Override
                    public void windowGainedFocus(WindowEvent we) {
                        mainFrame.setAlwaysOnTop(false);
                        mainFrame.removeWindowFocusListener(focusListener[0]);
                    }
                };
                mainFrame.addWindowFocusListener(focusListener[0]);
                javax.swing.Timer fallback = new javax.swing.Timer(2000, evt -> {
                    mainFrame.setAlwaysOnTop(false);
                    mainFrame.removeWindowFocusListener(focusListener[0]);
                });
                fallback.setRepeats(false);
                fallback.start();
            });
        });

        try {
            tray.add(trayIcon);
            LOG.info("Icono añadido a la bandeja del sistema exitosamente.");
        } catch (AWTException e) {
            LOG.error(MessageUtils.t("guiswing_error_loading_systray"), e);
        }
    }

    public void removeSystray() {
        if (trayIcon != null) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
            } catch (UnsupportedOperationException e) {
                LOG.warn("SystemTray.getSystemTray() no disponible al remover: " + e.getMessage());
            }
            trayIcon = null;
            LOG.info("TrayIcon removido correctamente.");
        }
    }

    public void loadGUI() {
        settings = SettingsManager.getInstance().getAndCreateSettings();
        isRemote = settings.isRemote();

        try {
            try {
                if (SystemInfo.isMacOS) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(Runtime.getRuntime()
                            .exec(new String[] { "/usr/bin/defaults", "read", "-g", "AppleInterfaceStyle" })
                            .getInputStream()));
                    String line = "";
                    if (reader != null)
                        line = reader.readLine();
                    if (line != null && line.equals("Dark"))
                        UIManager.setLookAndFeel(new FlatMacDarkLaf());
                    else
                        UIManager.setLookAndFeel(new FlatMacLightLaf());
                } else
                    UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
            } catch (UnsupportedLookAndFeelException | ClassNotFoundException e) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        } catch (Exception e) {
            LOG.error(MessageUtils.t("guiswing_error_loading_gui"), e);
            showError(FirmadorUtils.getRootCause(e));
        }

        if (settings.isSimplifiedMode() == null && !isRemote) {
            SelectSimpleModePanel modePanel = new SelectSimpleModePanel();

            int result = JOptionPane.showConfirmDialog(
                    null,
                    modePanel,
                    MessageUtils.t("select_mode_title"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                boolean simplifiedMode = modePanel.isSimplifiedModeSelected();
                settings.setSimplifiedMode(simplifiedMode);
            } else {
                settings.setSimplifiedMode(true);
            }
            SettingsManager.getInstance().setSettings(settings, true);
        }
        configureSystray();
        List<String> fileArgs = getFileArgs(args);
        String initialCommand;
        if (fileArgs != null && !fileArgs.isEmpty()) {
            initialCommand = fileArgs.stream()
                    .map(f -> "OPEN_FILE:" + f)
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("SHOW_WINDOW");
        } else if (isRemote) {
            initialCommand = "START_FIRMADOR_REMOTE";
        } else {
            initialCommand = "SHOW_WINDOW";
        }
        boolean isMain = instanceManager.tryLockOrRecover(cmd -> {
            if ("SHOW_WINDOW".equals(cmd)) {
                SwingUtilities.invokeLater(() -> {
                    mainFrame.setVisible(true);
                    mainFrame.setExtendedState(JFrame.NORMAL);
                    mainFrame.revalidate();
                    mainFrame.repaint();
                    mainFrame.setAlwaysOnTop(true);
                    mainFrame.toFront();
                    mainFrame.requestFocus();
                    mainFrame.setAlwaysOnTop(false);
                });
            } else if (cmd.startsWith("OPEN_FILE:")) {
                String pathArchivo = cmd.substring("OPEN_FILE:".length()).trim();
                addDocuments(new File[] { new File(pathArchivo) }, true);
            } else if (cmd.startsWith("START_FIRMADOR_REMOTE")) {
                connectionPanel.displayAndStartConnection();
                displayFunctionality("connection");
            }
        },
                initialCommand);
        if (!isMain) {
            System.exit(0);
        }
        LoggingFrame loggingFrame = new LoggingFrame();
        LogHandler handler = LogHandler.getInstance();
        handler.setWritter(loggingFrame);
        handler.register();
        loggingPane = loggingFrame.getLogScrollPane();
        gui = this;
        settings.addListener(this);

        try {
            mainFrame = new SwingMainWindowFrame(isRemote ? "Firmador remoto" : "Firmador");
            if (settings.isMinimizeGui()) {
                settings.startwindowstate = "ICONIFIED";
            }
            ;
        } catch (HeadlessException e) {
            LOG.error(MessageUtils.t("guiswing_log_error_headless"));
            throw e;
        }
        mainFrame.setGUIInterface(this, settings.isSimplifiedMode());

        signPanel = new SignPanel();
        signPanel.setGUI(this);
        signPanel.initializeActions();
        signPanel.hideButtons();
        signPanel.setSmartCardDetector(cardDetector);

        GroupLayout signLayout = new GroupLayout(signPanel);
        signPanel.createLayout(signLayout, signPanel);
        settings.addListener(signPanel);
        if (!isRemote && settings.isSimplifiedMode()) {// TODO add setting for toggling validation tab
            validatePanel = new ValidatePanel();
            validatePanel.setGUI(this);
            validatePanel.initializeActions();
            validatePanel.hideButtons();
        }

        if (!settings.isSimplifiedMode()) {
            listdocumentpanel = new ListDocumentPanel();
            listdocumentpanel.setGUI(gui);
            directoryPanel = new DirectoryPanel(mainFrame);
            directoryPanel.setGUI(gui);
        }

        JPanel aboutPanel = new JPanel();
        GroupLayout aboutLayout = new AboutLayout(aboutPanel);
        ((AboutLayout) aboutLayout).setInterface(this);

        aboutPanel.setLayout(aboutLayout);
        aboutPanel.setOpaque(false);

        this.configPanel = new ConfigPanel();
        configPanel.setOpaque(false);
        configPanel.setGui(this);
        frameTabbedPane = new JTabbedPane();
        tabPosition = 0;

        if (settings.isSimplifiedMode() && !isRemote) {
            frameTabbedPane.addTab(MessageUtils.t("guiswing_tab_sign"), signPanel);
            frameTabbedPane.setToolTipTextAt(tabPosition, MessageUtils.t("guiswing_tab_sign_tooltip"));
            frameTabbedPane.getAccessibleContext().getAccessibleChild(tabPosition).getAccessibleContext()
                    .setAccessibleDescription(MessageUtils.t("guiswing_tab_sign_tooltip_accessible"));
            frameTabbedPane.setMnemonicAt(tabPosition, '1');
            tabPosition++;
            JScrollPane validateScrollPane = validatePanel.getValidateScrollPane();
            frameTabbedPane.addTab(MessageUtils.t("guiswing_tab_validate"), validateScrollPane);
            frameTabbedPane.setToolTipTextAt(tabPosition, MessageUtils.t("guiswing_tab_validate_tooltip"));
            frameTabbedPane.getAccessibleContext().getAccessibleChild(tabPosition).getAccessibleContext()
                    .setAccessibleDescription(MessageUtils.t("guiswing_tab_validate_tooltip_accessible"));
            frameTabbedPane.setMnemonicAt(tabPosition, '2');
            tabPosition++;
            frameTabbedPane.addTab(MessageUtils.t("guiswing_tab_settings"), configPanel);
            frameTabbedPane.setToolTipTextAt(tabPosition, MessageUtils.t("guiswing_tab_settings_tooltip"));
            frameTabbedPane.getAccessibleContext().getAccessibleChild(tabPosition).getAccessibleContext()
                    .setAccessibleDescription(MessageUtils.t("guiswing_tab_settings_tooltip_accessible"));
            frameTabbedPane.setMnemonicAt(tabPosition, '3');
            tabPosition++;
            frameTabbedPane.addTab(MessageUtils.t("guiswing_tab_about"), aboutPanel);
            frameTabbedPane.setToolTipTextAt(tabPosition, MessageUtils.t("guiswing_tab_about_tooltip"));
            frameTabbedPane.getAccessibleContext().getAccessibleChild(tabPosition).getAccessibleContext()
                    .setAccessibleDescription(MessageUtils.t("guiswing_tab_about_tooltip_accessible"));
            frameTabbedPane.setMnemonicAt(tabPosition, '4');
        } else {
            frameTabbedPane.add(MessageUtils.t("guiswing_tab_documents"),
                    listdocumentpanel);
            frameTabbedPane.setToolTipTextAt(tabPosition, MessageUtils.t("guiswing_tab_documents_tooltip"));
            frameTabbedPane.getAccessibleContext().getAccessibleChild(tabPosition).getAccessibleContext()
                    .setAccessibleDescription(MessageUtils.t("guiswing_tab_documents_tooltip_accessible"));
            frameTabbedPane.setMnemonicAt(tabPosition, '1');
            tabPosition++;

            frameTabbedPane.add(MessageUtils.t("guiswing_tab_directories"), directoryPanel);
            frameTabbedPane.setToolTipTextAt(tabPosition, MessageUtils.t("guiswing_tab_directories_tooltip"));
            frameTabbedPane.getAccessibleContext().getAccessibleChild(tabPosition).getAccessibleContext()
                    .setAccessibleDescription(MessageUtils.t("guiswing_tab_directories_tooltip_accessible"));
            frameTabbedPane.setMnemonicAt(tabPosition, '2');

            tabPosition++;
            frameTabbedPane.addTab(MessageUtils.t("guiswing_tab_sign"), signPanel);
            frameTabbedPane.setToolTipTextAt(tabPosition, MessageUtils.t("guiswing_tab_sign_tooltip"));
            frameTabbedPane.getAccessibleContext().getAccessibleChild(tabPosition).getAccessibleContext()
                    .setAccessibleDescription(MessageUtils.t("guiswing_tab_sign_tooltip_accessible"));
            frameTabbedPane.setMnemonicAt(tabPosition, '3');

            boolean remote = settings.isRemote();
            if (!remote) {
                remote = settings.getStartFirmadorRemote();
            }
            connectionPanel = new ConnectionPanel(gui, remote, mainFrame);

            tabPosition++;
            frameTabbedPane.addTab(MessageUtils.t("guiswing_tab_connection"), connectionPanel);
            frameTabbedPane.setToolTipTextAt(tabPosition, MessageUtils.t("guiswing_tab_connection_tooltip"));
            frameTabbedPane.getAccessibleContext().getAccessibleChild(tabPosition).getAccessibleContext()
                    .setAccessibleDescription(MessageUtils.t("guiswing_tab_connection_tooltip_accessible"));
            frameTabbedPane.setMnemonicAt(tabPosition, '4');
            listdocumentpanel.setConnectionPanel(connectionPanel);

            tabPosition++;
            frameTabbedPane.addTab(MessageUtils.t("guiswing_tab_settings"), configPanel);
            frameTabbedPane.setToolTipTextAt(tabPosition, MessageUtils.t("guiswing_tab_settings_tooltip"));
            frameTabbedPane.getAccessibleContext().getAccessibleChild(tabPosition).getAccessibleContext()
                    .setAccessibleDescription(MessageUtils.t("guiswing_tab_settings_tooltip_accessible"));
            frameTabbedPane.setMnemonicAt(tabPosition, '5');

            tabPosition++;
            frameTabbedPane.addTab(MessageUtils.t("guiswing_tab_about"), aboutPanel);
            frameTabbedPane.setToolTipTextAt(tabPosition, MessageUtils.t("guiswing_tab_about_tooltip"));
            frameTabbedPane.getAccessibleContext().getAccessibleChild(tabPosition).getAccessibleContext()
                    .setAccessibleDescription(MessageUtils.t("guiswing_tab_about_tooltip_accessible"));
            frameTabbedPane.setMnemonicAt(tabPosition, '6');

            tabPosition++;
        }
        if (settings.showLogs)
            showLogs(frameTabbedPane);
        docSelector = new DocumentSelectionGroupLayout(mainFrame.getContentPane(), frameTabbedPane, mainFrame);
        docSelector.setGUI(this);
        docSelector.initializeActions();
        if (!isRemote)
            mainFrame.getContentPane().setLayout(docSelector);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.pack();
        mainFrame.setMinimumSize(mainFrame.getSize());
        mainFrame.setLocationByPlatform(true);

        if (background) {
            mainFrame.setVisible(false);
        } else {
            mainFrame.setVisible(true);
        }

        docmanager = new DocumentManager(gui);

        loadDialogWorker = new LoadProgressDialogWorker(gui);
        loadDialogWorker.execute();

        documentService = new DocumentService(pluginManager, docmanager, gui, settings);
        connectionService = new ConnectionService(connectionPanel, gui);
        utilsService = new UtilsService();

        this.mainFrame.setExtendedState(settings.getExtendedState());
        if (documenttosign != null) {
            File[] files = new File[1];
            File doc = new File(documenttosign);
            files[0] = doc;
            this.addDocuments(files);
        }

    }

    /*
     * =============================================================================
     * ==
     * Documents
     * =============================================================================
     * ==
     */

    @Override
    public List<Document> addDocuments(File[] files) {
        return addDocuments(files, true);
    }

    @Override
    public List<Document> addDocumentsSynchronous(File[] files) {
        return documentService.addDocumentsSynchronous(files);
    }

    public List<Document> addDocuments(File[] files, boolean dopreview) {
        if (dopreview)
            loadDialogWorker.setVisible(true);
        return documentService.addDocuments(files);
    }

    @Override
    public void loadDocuments(List<Document> documents, boolean dopreview) {
        List<Document> docs = documents;
        docmanager.processDocument(docs, settings.max_number_process_doc);
        if (settings.isSimplifiedMode()) {
            this.simplifiedDocument = docs.get(0);
            needNavigateToPreviewPanel = true;
        } else {
            if (docs.size() == 1) {
                needNavigateToPreviewPanel = true;
            } else {
                gui.displayFunctionality("document");
            }
            if (!dopreview) {
                needNavigateToPreviewPanel = false;
            }
            listdocumentpanel.setOnlyVirtual(false);
            listdocumentpanel.getLocalDocumentsButton().setSelected(true);
            listdocumentpanel.toggleToLocals();
            SwingUtilities.invokeLater(() -> listdocumentpanel.addDocuments(docs));
        }
    }

    public void loadRemoteDocumentInList(List<FirmadorRemoteDocument> remoteDocument, String service) {
        this.remoteDocuments = remoteDocument;
        VirtualSigner virtualSigner = new VirtualSigner(gui);
        try {
            loadDialogWorker.setTitle(MessageUtils.t("guiswing_completing_documents"));
            boolean signed = virtualSigner.sign(remoteDocument, service);
            desativateLoadDialog();
            if (signed) {
                for (Document doc : virtualDocumentsToSign) {
                    listdocumentpanel.removeDocument(doc);
                }
            }
            this.remoteDocuments.clear();
        } catch (Throwable e) {
            LOG.error("Error al iniciar el virtualSigner", e);
            gui.showError(FirmadorUtils.getRootCause(e));
        }
    }

    public String getDocumentToSign() {
        return docSelector.getLastFile();
    }

    @Override
    public void extendDocument() {
        if (fileName == null)
            fileName = getDocumentToSign();
        if (fileName != null)
            documentService.extendDocument(new FileDocument(fileName), false, null);
    }

    public void extendsDone(Document document) {
        setActiveDocument();
    }

    @Override
    public void doPreview(Document document) {
        forcePreview = true;
        docmanager.schedulePreview(document);
    }

    public void previewDone(Document document) {
        showNotification(MessageUtils.t("guiswing_success_preview_document"), NotificationType.SUCCESS);
        if (document.getShowPreview()) {
            setActiveDocument();
            if (document.getIsReady()) {
                loadActiveDocument(document);
            }
            if (forcePreview) {
                forcePreview = false;
                desativateLoadDialog();
                gui.displayFunctionality("sign");
            }
        }
    }

    @Override
    public void previewAllDone() {
        LOG.info("previewAllDone");
        showNotification(MessageUtils.t("guiswing_success_preview_documents"), NotificationType.SUCCESS);
        if (needNavigateToPreviewPanel) {
            Document document = null;
            if (settings.isSimplifiedMode()) {
                if (!simplifiedDocument.getIsReady()) {
                    gui.doPreview(simplifiedDocument);
                }
                document = simplifiedDocument;
            } else {
                List<Document> docs = listdocumentpanel.getDocuments();
                if (!docs.get(docs.size() - 1).getIsReady()) {
                    gui.doPreview(docs.get(0));
                }
                document = docs.get(0);
            }
            GUISwing.this.loadActiveDocument(document);
            gui.displayFunctionality("sign");
            needNavigateToPreviewPanel = false;
        }
        desativateLoadDialog();
        if (!settings.isSimplifiedMode()) {
            listdocumentpanel.previewAllDone();
        }
    }

    public void signDocument(Document document) {
        if (!validateConnectedCard()) {
            return;
        }
        if (document.isVirtual()) {
            if (connectionService.validateConnection(document.getService())) {
                virtualDocumentsToSign.add(document);
                List<Document> readyToSign = new ArrayList<>();
                readyToSign.add(document);
                VirtualSigner virtualSigner = new VirtualSigner(gui);
                try {
                    loadDialogWorker.setVisible(true);
                    loadDialogWorker.setTitle(MessageUtils.t("guiswing_obtaining_data_documents"));
                    boolean signed = virtualSigner.getHashToSign(readyToSign, document.getSettings());
                    if (!signed) {
                        desativateLoadDialog();
                        showNotification(MessageUtils.t("guiswing_show_error_getHashToSign_title"),
                                NotificationType.WARNING);
                    }
                } catch (Throwable e) {
                    LOG.error("Error al iniciar el virtualSigner", e);
                    gui.showError(FirmadorUtils.getRootCause(e));
                }
            }
        } else {
            List<Document> documents = new ArrayList<>();
            documents.add(document);
            docmanager.scheduleListofDocuments(documents);
        }

    }

    public void signDone(Document document) {
        LOG.info("Sign done");
        if (document.getSignwithErrors()) {
            showNotification(MessageUtils.t("guiswing_success_sing_document_error"), NotificationType.ERROR);
        } else {
            showNotification(MessageUtils.t("guiswing_success_sing_document"), NotificationType.SUCCESS);
        }
        if (document.getIsremote()) {
            try {
                DSSDocument signedDocument = document.getSignedDocument();
                signedDocument.writeTo(docinfo.getData());
                docinfo.setStatus(HttpStatus.SC_OK);
            } catch (IOException e) {
                LOG.error(MessageUtils.t("guiswing_error_writing_document"), e);
            }
        }
        if (settings.isSimplifiedMode()) {
            LOG.info("Removing document from simplified list");
            simplifiedDocument = null;
            File[] files = new File[1];
            File doc = new File(document.getPathToSave());
            files[0] = doc;
            addDocuments(files, false);
            getLoadDialogWorker().setVisible(true);
        }
    }

    public void signAllDocuments() {
        if (!validateConnectedCard()) {
            return;
        }
        if (!settings.isSimplifiedMode()) {
            List<Document> documents = listdocumentpanel.getDocuments();
            if (documents.isEmpty() || !documents.get(0).isVirtual()) {
                docmanager.scheduleListofDocuments(documents);
                return;
            }
            virtualDocumentsToSign = connectionService.validateVirtualDocumentsConnections(documents);
            if (!virtualDocumentsToSign.isEmpty()) {
                VirtualSigner virtualSigner = new VirtualSigner(gui);
                try {
                    loadDialogWorker.setVisible(true);
                    loadDialogWorker.setTitle(MessageUtils.t("guiswing_obtaining_data_documents"));
                    boolean signed = virtualSigner.getHashToSign(documents, null);

                    if (!signed) {
                        showNotification(MessageUtils.t("guiswing_show_error_getHashToSign_title"),
                                NotificationType.WARNING);
                    }
                } catch (Throwable e) {
                    LOG.error("Error al iniciar el virtualSigner", e);
                    gui.showError(FirmadorUtils.getRootCause(e));
                }
            }
        }
    }

    @Override
    public void signAllDone() {
        // showNotification(MessageUtils.t("guiswing_success_sign_documents"),
        // NotificationType.SUCCESS);
        // TODO Auto-generated method stub
    }

    public void signSelectedDocuments() {
        if (listdocumentpanel.getSelectedDocuments().get(0).isVirtual()) {
            virtualDocumentsToSign = connectionService
                    .validateVirtualDocumentsConnections(listdocumentpanel.getSelectedDocuments());
            if (!virtualDocumentsToSign.isEmpty()) {
                VirtualSigner virtualSigner = new VirtualSigner(gui);
                try {
                    loadDialogWorker.setVisible(true);
                    loadDialogWorker.setTitle(MessageUtils.t("guiswing_obtaining_data_documents"));
                    if (!virtualSigner.getHashToSign(listdocumentpanel.getSelectedDocuments(), null)) {
                        showNotification(MessageUtils.t("guiswing_show_error_getHashToSign"), NotificationType.WARNING);
                    }
                } catch (Throwable e) {
                    LOG.error("Error al iniciar el virtualSigner", e);
                    gui.showError(FirmadorUtils.getRootCause(e));
                }
            }
        } else {
            docmanager.scheduleListofDocuments(listdocumentpanel.getSelectedDocuments());
        }
    }

    public void processAllDocuments() {
        loadDialogWorker.setVisible(true);
        List<Document> docs = listdocumentpanel.getDocuments();

        if (listdocumentpanel.getOnlyVirtual()) {
            AtomicInteger pendingValidations = new AtomicInteger(docs.size());

            for (Document doc : docs) {
                validateVirtualDocument(doc, () -> {
                    if (pendingValidations.decrementAndGet() == 0) {
                        loadDialogWorker.setVisible(false);
                    }
                });
            }
        } else {
            docmanager.processDocument(docs, 0);
        }
    }

    public void validateVirtualDocument(Document document, Runnable onComplete) {
        Connection connection = connectionPanel.getConnection(document.getService());
        if (!connection.isLogged() || !connection.isRunning()) {
            showNotification(MessageUtils.t("guiswing_show_error_not_logged") + " " + document.getService()
                    + " " + MessageUtils.t("guiswing_show_error_not_logged6"), NotificationType.WARNING);
            onComplete.run();
        } else {
            if (!document.getValidating()) {
                document.setValidating(true);
                if (processVirtualDocument(document)) {
                    showNotification(MessageUtils.t("guiswing_success_validate_document"),
                            NotificationType.SUCCESS);
                } else {
                    showNotification(MessageUtils.t("guiswing_error_validate_document"),
                            NotificationType.ERROR);
                }
                onComplete.run();
            } else {
                onComplete.run(); // Ya estaba validando
            }
        }
    }

    public void processDocument(Document doc) {
        loadDialogWorker.setVisible(true);
        List<Document> docs = new ArrayList<Document>();
        docs.add(doc);
        docmanager.processDocument(docs, 0);
    }

    public Boolean processVirtualDocument(Document document) {
        return ConnectionUtils.validateVirtualDocument(document, cardDetector, gui);
    }

    public void loadReportDocument(Document document, boolean needProcess) {
        if (document.isValid()) {
            if (settings.isSimplifiedMode()) {
                validatePanel.reportLabel.setText(document.getReport());
                validatePanel.extendButton.setEnabled(true);
                validatePanel.reportLabel.requestFocusInWindow();
                validatePanel.reportLabel.getAccessibleContext()
                        .setAccessibleDescription(document.getPlainReport());
            } else {
                if (document.getReport() == null || document.getReport().isEmpty()) {
                    listdocumentpanel.validatePanel.reportLabel
                            .setText(MessageUtils.t("guiswing_dialog_document_not_signed_analyzed"));
                    listdocumentpanel.validatePanel.extendButton.setEnabled(false);
                } else {
                    listdocumentpanel.validatePanel.reportLabel.setText(document.getReport());
                    listdocumentpanel.validatePanel.extendButton.setEnabled(true);
                    listdocumentpanel.validatePanel.reportLabel.requestFocusInWindow();
                    listdocumentpanel.validatePanel.reportLabel.getAccessibleContext()
                            .setAccessibleDescription(document.getPlainReport());
                    listdocumentpanel.reloadView();
                }
            }
        } else {
            if (needProcess) {
                if (settings.isSimplifiedMode()) {
                    validatePanel.reportLabel.setText(document.getReport());
                    validatePanel.extendButton.setEnabled(true);
                    validatePanel.reportLabel.requestFocusInWindow();
                    validatePanel.reportLabel.getAccessibleContext()
                            .setAccessibleDescription(document.getPlainReport());
                } else {
                    listdocumentpanel.validatePanel.reportLabel
                            .setText(MessageUtils.t("guiswing_dialog_document_not_signed_analyzed"));
                    listdocumentpanel.validatePanel.extendButton.setEnabled(false);
                }
                if (document.isVirtual()) {
                    if (document.getReport() != null) {
                        listdocumentpanel.validatePanel.reportLabel.setText(document.getReport());
                        listdocumentpanel.validatePanel.extendButton.setEnabled(true);
                        listdocumentpanel.validatePanel.reportLabel.requestFocusInWindow();
                        listdocumentpanel.validatePanel.reportLabel.getAccessibleContext()
                                .setAccessibleDescription(document.getPlainReport());
                        listdocumentpanel.reloadView();
                    } else {
                        validateVirtualDocument(document, () -> {
                        });
                    }
                } else {
                    processDocument(document);
                }
            } else {
                listdocumentpanel.validatePanel.reportLabel
                        .setText(MessageUtils.t("guiswing_dialog_document_not_signed"));
                listdocumentpanel.validatePanel.extendButton.setEnabled(false);
            }
        }
    }

    public void notifyReportDocument(UUID documentId, String report) {
        List<Document> documents = listdocumentpanel.getVirtualDocuments();
        Document doc = documentService.notifyReportDocument(documentId, report, documents);
        if (doc == null) {
            LOG.error("No se encontró el documento con ID: " + documentId);
            return;
        }
        listdocumentpanel.validatePanel.reportLabel.setText(doc.getReport());
        listdocumentpanel.validatePanel.extendButton.setEnabled(true);
        listdocumentpanel.validatePanel.reportLabel.requestFocusInWindow();
        listdocumentpanel.validatePanel.reportLabel.getAccessibleContext()
                .setAccessibleDescription(doc.getPlainReport());
        listdocumentpanel.reloadView();
    }

    public void cancelDocument(UUID documentId) {
        List<Document> documents = listdocumentpanel.getRealDocuments();
        documentService.cancelDocument(documentId, documents);
        listdocumentpanel.reloadView();
    }

    public void loadActiveDocument(Document document) {
        LOG.info("Loading active document");
        try {
            if (document.isValid()) {
                if (settings.isSimplifiedMode()) {
                    validatePanel.reportLabel.setText(document.getReport());
                    validatePanel.extendButton.setEnabled(true);
                    validatePanel.reportLabel.requestFocusInWindow();
                    validatePanel.reportLabel.getAccessibleContext()
                            .setAccessibleDescription(document.getPlainReport());
                } else {
                    listdocumentpanel.validatePanel.reportLabel.setText(document.getReport());
                    listdocumentpanel.validatePanel.extendButton.setEnabled(true);
                    listdocumentpanel.validatePanel.reportLabel.requestFocusInWindow();
                    listdocumentpanel.validatePanel.reportLabel.getAccessibleContext()
                            .setAccessibleDescription(document.getPlainReport());
                }
            } else {
                if (settings.isSimplifiedMode()) {
                    validatePanel.reportLabel.setText(MessageUtils.t("guiswing_dialog_document_not_signed"));
                    validatePanel.extendButton.setEnabled(false);
                }
            }
            gui.displayFunctionality("sign");
            signPanel.setDocument(document);
            signPanel.setPreview(document.getPreviewManager());
            signPanel.paintPDFViewer();
            docSelector.setLastFile(document);
            document.getMimeType();
            signPanel.getSignButton().setEnabled(true);
        } catch (Throwable e) {
            LOG.error(MessageUtils.t("guiswing_error_loading_documents_with_mimetype"), e);
            gui.showError(FirmadorUtils.getRootCause(e));
        }
        try {
            mainFrame.setMinimumSize(mainFrame.getSize());
        } catch (Exception e) {
            LOG.error(MessageUtils.t("guiswing_error_loading_documents_with_mimetype"), e);
            gui.showError(FirmadorUtils.getRootCause(e));
        }
    }

    public void validateDone(Document document) {
        setActiveDocument();
        showNotification(MessageUtils.t("guiswing_success_validate_document"), NotificationType.SUCCESS);
        if (document.getIsReady()) {
            loadActiveDocument(document);
            desativateLoadDialog();
        }
    }

    @Override
    public void validateAllDone() {
        showNotification(MessageUtils.t("guiswing_success_validate_documents"), NotificationType.SUCCESS);
        // TODO Auto-generated method stub
    }

    @Override
    public void loadRemoteDocument(String fileName) {
        HashMap<String, RemoteDocInformation> docmap = remote.getDocInformation();
        docinfo = docmap.get(fileName);
        try {
            byte[] data = IOUtils.toByteArray(docinfo.getInputdata());
            toSignDocument = new InMemoryDocument(data, fileName);
            MimeType mimeType = toSignDocument.getMimeType();
            if (MimeTypeEnum.PDF == mimeType) {
                frameTabbedPane.setSelectedIndex(2);
                Document remoteDocument = new Document(this, data, docinfo.getName(), docinfo.getStatus());
                remoteDocument.loadPreviewRemote(data);
                signPanel.setDocument(remoteDocument);
                signPanel.setPreview(remoteDocument.getPreviewManager());
                signPanel.paintPDFViewer();
                docSelector.setLastFile(remoteDocument);
                remoteDocument.getMimeType();
                signPanel.getSignButton().setEnabled(true);
                try {
                    mainFrame.setMinimumSize(mainFrame.getSize());
                } catch (Exception e) {
                    LOG.error(MessageUtils.t("guiswing_error_loading_documents_with_mimetype"), e);
                    gui.showError(FirmadorUtils.getRootCause(e));
                }
            }
        } catch (IOException e) {
            LOG.error(MessageUtils.t("guiswing_error_loading_document_remote_with_mimetype"), e);
            throw new RuntimeException(e);
        }
    }

    public void setActiveDocument() {
        Document currentActiveDocument;
        if (settings.isSimplifiedMode()) {
            currentActiveDocument = simplifiedDocument;
        } else {
            currentActiveDocument = listdocumentpanel.getActiveDocument();
        }
        if (currentActiveDocument != document) {
            document = currentActiveDocument;
            setActiveDocument(document);
        }
    }

    public void setActiveDocument(Document document) {
        docSelector.setLastFile(document);
    }

    public String getPathToSave(String extension) {
        if (settings.overwriteSourceFile)
            return getDocumentToSign();
        if (documenttosave != null)
            return documenttosave;
        String pathToSave = showSaveDialog("-firmado", extension);
        return pathToSave;
    }

    public String getPathToSaveExtended(String extension) {
        if (settings.overwriteSourceFile)
            return getDocumentToSign();
        String pathToExtend = showSaveDialog("-sellado", extension);
        return pathToExtend;
    }

    public void validateDocumentByPath(File file) {
        // TODO: Hacerlo pero solo incluyendo este path
    }

    /*
     * =============================================================================
     * ==
     * Virtual Documents
     * =============================================================================
     * ==
     */

    public void loadVirtualDocument(List<Document> docs) {
        List<Document> virtualDocs = listdocumentpanel.getVirtualDocuments();
        List<Document> newDocs = documentService.loadVirtualDocuments(docs, virtualDocs);
        if (!newDocs.isEmpty()) {
            listdocumentpanel.addDocuments(newDocs);
        }
    }

    public boolean deleteDocument(Document document, SmartCardDetector smartCardDetector) throws Exception {
        if (!connectionService.validateConnection(document.getService())) {
            return false;
        }
        return ConnectionUtils.deleteDocument(document, smartCardDetector, gui);
    }

    public BufferedImage getPageImageFromApi(Document document, int page) {
        if (!connectionService.validateConnection(document.getService())) {
            return null;
        }
        BufferedImage image = ConnectionUtils.getPageImageFromApi(document, page, gui);
        desativateLoadDialog();
        return image;
    }

    public void disconnect(Document document, String service) {
        connectionService.disconnect(document, service);
        showNotification(MessageUtils.t("ucr_integration_lost_connection"), NotificationType.INFO);
        desativateLoadDialog();
    }

    public void notifyLoginComplete(Connection connection) {
        connectionPanel.loginComplete(connection);
        showNotification(MessageUtils.t("guiswing_login_complete"), NotificationType.INFO);
    }

    /*
     * =============================================================================
     * ==
     * Utils
     * =============================================================================
     * ==
     */

    public void cleanVirtualDocumentsInList() {
        virtualDocumentsToSign = new ArrayList<>();
    }

    public Settings getCurrentSettings() {
        Settings collectedSettings = new Settings(this.settings);
        collectedSettings.reason = signPanel.getReasonField().getText().trim().replaceAll("\t", " ");
        collectedSettings.place = signPanel.getLocationField().getText().trim().replaceAll("\t", " ");
        collectedSettings.contact = signPanel.getContactInfoField().getText().trim().replaceAll("\t", " ");
        collectedSettings.image = System.getProperty("jnlp.signatureImage");
        collectedSettings.signY = signPanel.getPDFVisibleSignatureY();
        collectedSettings.signX = signPanel.getPDFVisibleSignatureX();
        collectedSettings.pageNumber = (int) signPanel.getPageSpinner().getValue();
        if (collectedSettings.image == null)
            collectedSettings.image = this.settings.getImage();
        collectedSettings.hideSignatureAdvice = Boolean.getBoolean("jnlp.hideSignatureAdvice");
        collectedSettings.isVisibleSignature = !signPanel.getSignatureVisibleCheckBox().isSelected();
        collectedSettings.signASiC = signPanel.isASiC();
        collectedSettings.forceCades = signPanel.isCAdES();

        return collectedSettings;
    }

    public static List<String> getFileArgs(String[] args) {
        return UtilsService.getFileArgs(args);
    }

    public void setArgs(String[] args) {
        List<String> arguments = new ArrayList<>();
        for (String params : args) {
            if (params.startsWith("--background")) {
                background = true;
            }
            if (!params.startsWith("-"))
                arguments.add(params);
        }
        this.args = args;
        if (arguments.size() > 1)
            documenttosign = Paths.get(arguments.get(0)).toAbsolutePath().toString();
        if (arguments.size() > 2)
            documenttosave = Paths.get(arguments.get(1)).toAbsolutePath().toString();
    }

    public void displayFunctionality(String functionality) {
        if (settings.isSimplifiedMode()) {
            if (functionality.equalsIgnoreCase("sign"))
                frameTabbedPane.setSelectedIndex(0);
            else if (functionality.equalsIgnoreCase("validate"))
                frameTabbedPane.setSelectedIndex(1);
        } else {
            if (functionality.equalsIgnoreCase("sign"))
                frameTabbedPane.setSelectedIndex(2);
            else if (functionality.equalsIgnoreCase("document"))
                frameTabbedPane.setSelectedIndex(0);
            else if (functionality.equalsIgnoreCase("directory"))
                frameTabbedPane.setSelectedIndex(1);
            else if (functionality.equalsIgnoreCase("connection"))
                frameTabbedPane.setSelectedIndex(3);
        }

    }

    public void updateConfig() {
        if (settings.showLogs)
            showLogs(frameTabbedPane);
        else
            hideLogs(frameTabbedPane);
    }

    public void close() {
        instanceManager.release();
        removeSystray();
        mainFrame.dispatchEvent(new WindowEvent(mainFrame, WindowEvent.WINDOW_CLOSING));
    }

    public SwingMainWindowFrame getMainFrame() {
        return mainFrame;
    }

    public String getExtension() {
        return utilsService.getExtension(toSignDocument);
    }

    public void configurePluginManager() {
        this.pluginManager.startLogging();
        this.mainFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent arg0) {
                instanceManager.release();
                pluginManager.stop();
            }
        });
    }

    public void setPluginManager(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public void nextStep(String msg) {
        docmanager.progressDialogWorker.setNote(msg);
    }

    public ConnectionPanel getConnectionPanel() {
        return connectionPanel;
    }

    public void setGaudiSpeaker(GaudiIntegration<Void, byte[]> remoteWorker) {
        this.gaudi = remoteWorker; // FIXME is this actually used?
    }

    public void setUCRSpeaker(RemoteIntegration<Void, byte[]> remoteWorker) {
        this.ucr = remoteWorker; // FIXME is this actually used?
    }

    public DocumentManager getDocmanager() {
        return docmanager;
    }

    public void setRemoteWorker(RemoteHttpWorker<Void, byte[]> remoteWorker) {
        this.remote = remoteWorker;
    }

    public void reloadConfig() {
        configPanel.refreshAdvancedConfigPanel();
        connectionPanel.showConnectionDetails();
    }

    public SmartCardDetector getSmartCardDetector() {
        return cardDetector;
    }

    @Override
    public void clearDone() {
        signPanel.clean();
        validatePanel.clean();
        document = null;
        docSelector.clean();
    }

    @Override
    public void setSmartCardDetector(SmartCardDetector detector) {
        cardDetector = detector;
    }

    /*
     * =============================================================================
     * ==
     * Directories
     * =============================================================================
     * ==
     */

    public void addDirectories(File[] files) {
        directoryPanel.addFiles(files);
        displayFunctionality("directory");
    }

    public void signDirectory() {
        if (!validateConnectedCard()) {
            return;
        }
        List<Document> docs = directoryPanel.getDocuments();
        docmanager.scheduleListofDocuments(docs);
    }

    /*
     * =============================================================================
     * ==
     * Alerts and Dialogs
     * =============================================================================
     * ==
     */

    public boolean validateConnectedCard() {
        try {
            List<CardSignInfo> cards = cardDetector.readListSmartCard();
            if (cards == null || cards.isEmpty()) {
                LOG.warn(MessageUtils.t("guiswing_show_error_providerexception"));
                showErrorAlert(MessageUtils.t("guiswing_log_closefile"),
                        MessageUtils.t("guiswing_show_error_providerexception"));
                desativateLoadDialog();
                return false;
            }
            return true;
        } catch (Throwable e) {
            showError(e);
            return false;
        }
    }

    public String showSaveDialog(String suffix, String extension) {
        gui.nextStep(MessageUtils.t("guiswing_getting_save_path"));
        String lastDirectory = docSelector.getLastDirectory();
        String lastFile = docSelector.getLastFile();
        String fileName = null;
        FileDialog saveDialog = null;
        saveDialog = new FileDialog(mainFrame, MessageUtils.t("guiswing_save_document"), FileDialog.SAVE);
        saveDialog.setDirectory(lastDirectory);
        String dotExtension = "";
        int lastDot = lastFile.lastIndexOf(".");
        if (extension != "") {
            suffix = ""; // XMLs could reuse same files, however
            dotExtension = extension;
        } else if (lastDot >= 0)
            dotExtension = lastFile.substring(lastDot);

        Path path = Paths.get(lastFile);
        lastFile = path.getFileName().toString();
        String savestrinfilename = lastFile.substring(0, lastFile.lastIndexOf(".")) + suffix + dotExtension;
        saveDialog.setFile(savestrinfilename);
        // saveDialog.setFilenameFilter(docSelector.getLoadDialog().getFilenameFilter());
        // // FIXME use filter based on file type containing the signature
        saveDialog.setLocationRelativeTo(null);
        saveDialog.setVisible(true);
        saveDialog.dispose();
        if (saveDialog.getFile() != null) {
            fileName = saveDialog.getDirectory() + saveDialog.getFile();
            lastDirectory = saveDialog.getDirectory();
            lastFile = saveDialog.getFile();
        }
        return fileName;
    }

    public String showSaveDialog(String filepath, String suffix, String extension) {
        gui.nextStep(MessageUtils.t("guiswing_nextstep_save"));
        String lastDirectory = docSelector.getLastDirectory();
        String lastFile = filepath;
        String fileName = null;
        FileDialog saveDialog = null;
        saveDialog = new FileDialog(mainFrame, MessageUtils.t("guiswing_dialog_document_save"),
                FileDialog.SAVE);
        saveDialog.setDirectory(lastDirectory);
        String dotExtension = "";
        int lastDot = lastFile.lastIndexOf(".");
        if (extension.isEmpty()) {
            dotExtension = extension;
        } else if (lastDot >= 0)
            dotExtension = lastFile.substring(lastDot);

        Path path = Paths.get(lastFile);
        lastFile = path.getFileName().toString();
        String savestringfilename = lastFile.substring(0, lastFile.lastIndexOf(".")) + suffix + dotExtension;
        saveDialog.setFile(savestringfilename);
        // saveDialog.setFilenameFilter(docSelector.getLoadDialog().getFilenameFilter());
        // // FIXME use filter based on file type containing the signature
        saveDialog.setLocationRelativeTo(null);
        saveDialog.setVisible(true);
        saveDialog.dispose();
        if (saveDialog.getFile() != null) {
            fileName = saveDialog.getDirectory() + saveDialog.getFile();
            lastDirectory = saveDialog.getDirectory();
            lastFile = saveDialog.getFile();
        }
        return fileName;
    }

    public void showMessage(String message) {
        LOG.info(MessageUtils.t("guiswing_show_message") + message);
        JOptionPane.showMessageDialog(null, new CopyableJLabel(message),
                MessageUtils.t("guiswing_document_success_joptionpane_title"),
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void showError(Throwable error) {
        utilsService.showError(error, false);
    }

    public void showNotification(String message, NotificationType type) {
        docSelector.showNotification(message, type);
    }

    public enum NotificationType {
        SUCCESS, ERROR, WARNING, INFO
    }

    public void showErrorAlert(String title, String message) {
        JLabel messageLabel = new JLabel("<html><body style='width: 400px'>" + message + "</body></html>");
        messageLabel.getAccessibleContext().setAccessibleName(title);
        messageLabel.getAccessibleContext().setAccessibleDescription(message);

        JOptionPane.showMessageDialog(
                this.mainFrame,
                messageLabel,
                title,
                JOptionPane.ERROR_MESSAGE);
    }

    public LoadProgressDialogWorker getLoadDialogWorker() {
        return loadDialogWorker;
    }

    public void desativateLoadDialog() {
        loadDialogWorker.setVisible(false);
    }

    public void showErrorVirtual(String id) {
        Document doc = virtualDocumentsToSign.stream()
                .filter(document -> document.getDocumentID().toString().equals(id)).findFirst().orElse(null);
        if (doc == null) {
            LOG.error("Error al mostrar el documento virtual");
            return;
        }
        virtualDocumentsToSign.remove(doc);
        if (virtualDocumentsToSign.isEmpty()) {
            desativateLoadDialog();
        }
        ;
        showNotification(MessageUtils.t("guiswing_show_error_expire_document") + " " + doc.getName(),
                NotificationType.ERROR);
        listdocumentpanel.removeDocument(doc);
    }

    /*
     * =============================================================================
     * ==
     * Interfaces
     * =============================================================================
     * ==
     */

    public CardSignInfo getPin() {
        try {
            if (cardDetector.getListCardInfo() == null || cardDetector.getListCardInfo().isEmpty()) {
                LOG.warn("Lista de tarjetas vacía, forzando lectura...");
                cardDetector.readSaveListSmartCard();
            }
            if (cardDetector.getListCardInfo() == null || cardDetector.getListCardInfo().isEmpty()) {
                LOG.error("No se detectaron tarjetas conectadas");
                showErrorAlert(MessageUtils.t("guiswing_log_closefile"),
                        MessageUtils.t("guiswing_show_error_providerexception"));
                return null;
            }

            RequestPinWindow requestPinWindow = new RequestPinWindow();
            requestPinWindow.setSmartCardDetector(cardDetector);
            cardDetector.restoreSessions();
            int action = requestPinWindow.showandwait();
            cardDetector.login(requestPinWindow.getCardInfo());
            if (action == 0)
                return requestPinWindow.getCardInfo();
            else
                return null;
        } catch (Throwable e) {
            LOG.error("Error al obtener PIN", e);
            // showError(MessageUtils.t("error_reading_card"));
            return null;
        }
    }

    protected void showLogs(JTabbedPane frameTabbedPane) {
        frameTabbedPane.addTab(MessageUtils.t("guiswing_tab_logs"), loggingPane);
        frameTabbedPane.setToolTipTextAt(tabPosition, MessageUtils.t("guiswing_tab_logs_tooltip"));
        frameTabbedPane.getAccessibleContext().getAccessibleChild(tabPosition).getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("guiswing_tab_logs_tooltip_accessible"));
        frameTabbedPane.setMnemonicAt(tabPosition, '7');

    }

    protected void hideLogs(JTabbedPane frameTabbedPane) {
        frameTabbedPane.remove(loggingPane);
    }

    public ListDocumentPanel getListDocumentPanel() {
        return listdocumentpanel;
    }

}
