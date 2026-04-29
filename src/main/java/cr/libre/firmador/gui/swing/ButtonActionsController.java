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

package cr.libre.firmador.gui.swing;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.SettingsManager;
import cr.libre.firmador.connections.Connection;
import cr.libre.firmador.connections.ConnectionUtils;
import cr.libre.firmador.connections.ServicesUrlsIO;
import cr.libre.firmador.documents.Document;
import cr.libre.firmador.gui.GUISwing;

import org.apache.commons.io.FileUtils;

import javax.swing.*;
import javax.accessibility.AccessibleContext;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static cr.libre.firmador.gui.swing.ListDocumentPanel.LOG;

public class ButtonActionsController {
    private ListDocumentPanel panel;
    private GUISwing gui;
    private JPanel actionButtonsPanel;
    private JPanel sortingButtonsPanel;
    private JPanel searchButtonsPanel;
    private Path documentListSavePath = FileSystems.getDefault()
            .getPath(SettingsManager.getInstance().getPath().getParent().toString(), "document_list.csv");
    private boolean orderAscBySignatures = true;
    private boolean orderAscByName = true;
    private boolean orderAscByNumberPages = true;
    private boolean orderAscByDate = true;
    private boolean orderAscByExpiration = true;
    private JButton selectallbtn;
    private JButton savedoclistbtn;
    private JButton loaddoclistbtn;
    private JButton changefolderbtn;
    private boolean showConnectionButtons = false;

    public JButton getSelectAllButton() {
        return selectallbtn;
    }

    public JButton getSavedoclistbtn() {
        return savedoclistbtn;
    }

    public JButton getLoaddoclistbtn() {
        return loaddoclistbtn;
    }

    public JButton getChangefolderbtn() {
        return changefolderbtn;
    }

    public ButtonActionsController(ListDocumentPanel panel, GUISwing gui, JPanel actionButtonsPanel,
            JPanel sortingButtonsPanel, JPanel searchButtonsPanel) {
        this.panel = panel;
        this.gui = gui;
        this.actionButtonsPanel = actionButtonsPanel;
        this.sortingButtonsPanel = sortingButtonsPanel;
        this.searchButtonsPanel = searchButtonsPanel;
    }

    public void setGui(GUISwing gui) {
        this.gui = gui;
    }

    public void loadSortingButtons() {
        JLabel titleLabel = new JLabel(MessageUtils.t("list_document_sorting_actions"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f)); // Negrita y tamaño opcional
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Accesibilidad: Descripción para lectores de pantalla
        AccessibleContext titleContext = titleLabel.getAccessibleContext();
        titleContext.setAccessibleName(MessageUtils.t("accessibility_sorting_section_title"));

        JButton orderByNameButton = new JButton(MessageUtils.t("list_document_order_by_name"));
        JButton orderByNumberSignButton = new JButton(MessageUtils.t("list_document_order_by_number_of_signatures"));
        JButton orderByNumberOfPagesButton = new JButton(MessageUtils.t("list_document_order_by_number_of_pages"));
        JButton orderByDateButton = new JButton(MessageUtils.t("list_document_order_by_date"));
        JButton orderByExpirationButton = new JButton(MessageUtils.t("list_document_order_by_expiration_date"));

        orderByNameButton.setToolTipText(MessageUtils.t("list_document_order_by_name_tooltip"));
        orderByNameButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        // Accesibilidad: Nombre y descripción accesible
        orderByNameButton.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_order_by_name"));
        orderByNameButton.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("accessibility_order_by_name_description"));

        orderByNumberSignButton.setToolTipText(MessageUtils.t("list_document_order_by_number_of_signatures_tooltip"));
        orderByNumberSignButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        // Accesibilidad: Nombre y descripción accesible
        orderByNumberSignButton.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("accessibility_order_by_signatures"));
        orderByNumberSignButton.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("accessibility_order_by_signatures_description"));

        orderByNumberOfPagesButton.setToolTipText(MessageUtils.t("list_document_order_by_number_of_pages_tooltip"));
        orderByNumberOfPagesButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        // Accesibilidad: Nombre y descripción accesible
        orderByNumberOfPagesButton.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("accessibility_order_by_pages"));
        orderByNumberOfPagesButton.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("accessibility_order_by_pages_description"));

        orderByDateButton.setToolTipText(MessageUtils.t("list_document_order_by_date_tooltip"));
        orderByDateButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        // Accesibilidad: Nombre y descripción accesible
        orderByDateButton.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_order_by_date"));
        orderByDateButton.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("accessibility_order_by_date_description"));

        orderByExpirationButton.setToolTipText(MessageUtils.t("list_document_order_by_expiration_date_tooltip"));
        orderByExpirationButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        // Accesibilidad: Nombre y descripción accesible
        orderByExpirationButton.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("accessibility_order_by_expiration"));
        orderByExpirationButton.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("accessibility_order_by_expiration_description"));

        orderByNameButton.addActionListener(OrderByNameAction());
        orderByNumberSignButton.addActionListener(OrderByNumberSignAction());
        orderByNumberOfPagesButton.addActionListener(OrderByNumberOfPagesAction());
        orderByDateButton.addActionListener(OrderByCreateAtAction());
        orderByExpirationButton.addActionListener(OrderByExpirationDateAction());

        sortingButtonsPanel.add(titleLabel);
        sortingButtonsPanel.add(orderByNameButton);
        sortingButtonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        sortingButtonsPanel.add(orderByNumberSignButton);
        sortingButtonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        sortingButtonsPanel.add(orderByNumberOfPagesButton);
        sortingButtonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        sortingButtonsPanel.add(orderByDateButton);
        sortingButtonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        sortingButtonsPanel.add(orderByExpirationButton);
    }

    public void loadSearchButtons() {

        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton(MessageUtils.t("list_document_search_button"));
        JButton propertiesButton = new JButton(MessageUtils.t("list_document_properties"));

        // Accesibilidad: Campo de búsqueda
        searchField.setToolTipText(MessageUtils.t("list_document_search_field_tooltip"));
        searchField.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_search_field"));
        searchField.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("accessibility_search_field_description"));

        searchButton.setToolTipText(MessageUtils.t("list_document_search_button"));
        // Accesibilidad: Botón de búsqueda
        searchButton.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_search_button"));
        searchButton.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("accessibility_search_button_description"));

        propertiesButton.setToolTipText(MessageUtils.t("list_document_properties"));
        // Accesibilidad: Botón de propiedades
        propertiesButton.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_properties_button"));
        propertiesButton.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("accessibility_properties_button_description"));

        searchButton.setMnemonic('B');
        propertiesButton.setMnemonic('P');

        ActionListener searchAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String query = searchField.getText().toLowerCase();
                List<Document> documents = panel.getDocuments();
                documents.sort(Comparator.comparingInt((Document doc) -> {
                    String path = doc.getName().toLowerCase();
                    if (path.startsWith(query))
                        return 0;
                    else if (path.contains(query))
                        return 1;
                    else
                        return 2;
                }).thenComparing(Document::getPathName));
                panel.getScrollPane().getVerticalScrollBar().setValue(0);
                panel.reloadView();
            }
        };

        searchButton.addActionListener(searchAction);
        searchField.addActionListener(searchAction);

        propertiesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.showActionButtonsPanel();
            }
        });

        searchButtonsPanel.add(searchField);
        searchButtonsPanel.add(searchButton);
        searchButtonsPanel.add(propertiesButton);

    }

    public void loadActionButtons() {
        JLabel titleLabel = new JLabel(MessageUtils.t("list_document_actions"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f)); // Negrita y tamaño opcional
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Accesibilidad: Descripción para lectores de pantalla
        AccessibleContext titleContext = titleLabel.getAccessibleContext();
        titleContext.setAccessibleName(MessageUtils.t("accessibility_actions_section_title"));

        JButton signbtn = new JButton(MessageUtils.t("list_document_signall"));
        JButton signSelectedbtn = new JButton(MessageUtils.t("list_document_signall_selected"));
        JButton cleanbtn = new JButton(MessageUtils.t("list_document_clear"));
        this.savedoclistbtn = new JButton(MessageUtils.t("list_document_save_list"));
        this.loaddoclistbtn = new JButton(MessageUtils.t("list_document_load_list"));
        JButton previewallbtn = new JButton(MessageUtils.t("list_document_previewall"));
        this.changefolderbtn = new JButton(MessageUtils.t("list_document_changefolder"));
        JButton setconfigureallbtn = new JButton(MessageUtils.t("list_document_setconfigureall"));
        this.selectallbtn = new JButton(MessageUtils.t("list_document_selectall"));

        signbtn.setToolTipText(MessageUtils.t("list_document_signall"));
        signbtn.setToolTipText(MessageUtils.t("list_document_signall_selected_tooltip"));
        signbtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        // Accesibilidad: Botón firmar todos
        signbtn.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_sign_all_button"));
        signbtn.getAccessibleContext().setAccessibleDescription(MessageUtils.t("accessibility_sign_all_description"));
        signbtn.setMnemonic(MessageUtils.k('F'));

        signSelectedbtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        // Accesibilidad: Botón firmar seleccionados
        signSelectedbtn.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_sign_selected_button"));
        signSelectedbtn.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("accessibility_sign_selected_description"));
        signSelectedbtn.setMnemonic(MessageUtils.k('S'));

        cleanbtn.setToolTipText(MessageUtils.t("list_document_clear"));
        cleanbtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        cleanbtn.setMnemonic(MessageUtils.k('C'));
        // Accesibilidad: Botón limpiar
        cleanbtn.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_clear_button"));
        cleanbtn.getAccessibleContext().setAccessibleDescription(MessageUtils.t("accessibility_clear_description"));

        savedoclistbtn.setToolTipText(MessageUtils.t("list_document_save_list"));
        savedoclistbtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        savedoclistbtn.setMnemonic(MessageUtils.k('G'));
        // Accesibilidad: Botón guardar lista
        savedoclistbtn.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_save_list_button"));
        savedoclistbtn.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("accessibility_save_list_description"));

        loaddoclistbtn.setToolTipText(MessageUtils.t("list_document_load_list"));
        loaddoclistbtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        // Accesibilidad: Botón cargar lista
        loaddoclistbtn.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_load_list_button"));
        loaddoclistbtn.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("accessibility_load_list_description"));

        previewallbtn.setToolTipText(MessageUtils.t("list_document_previewall_tooltip"));
        previewallbtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        // Accesibilidad: Botón previsualizar todos
        previewallbtn.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_preview_all_button"));
        previewallbtn.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("accessibility_preview_all_description"));

        changefolderbtn.setToolTipText(MessageUtils.t("list_document_changefolder_tooltip"));
        changefolderbtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        // Accesibilidad: Botón cambiar carpeta
        changefolderbtn.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_change_folder_button"));
        changefolderbtn.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("accessibility_change_folder_description"));

        setconfigureallbtn.setToolTipText(MessageUtils.t("list_document_setconfigureall_tooltip"));
        setconfigureallbtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        // Accesibilidad: Botón configurar todos
        setconfigureallbtn.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("accessibility_configure_all_button"));
        setconfigureallbtn.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("accessibility_configure_all_description"));

        selectallbtn.setToolTipText(MessageUtils.t("list_document_selectall_tooltip"));
        selectallbtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        // Accesibilidad: Botón seleccionar todos
        selectallbtn.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_select_all_button"));
        selectallbtn.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("accessibility_select_all_description"));

        signbtn.addActionListener(SignAllAction());
        signSelectedbtn.addActionListener(SignSelectedAction());
        cleanbtn.addActionListener(CleanDocumentsAction());
        previewallbtn.addActionListener(PreviewAllAction());
        savedoclistbtn.addActionListener(SaveDocumentListAction());
        loaddoclistbtn.addActionListener(LoadDocumentListAction());
        changefolderbtn.addActionListener(ChangeFolderAction());
        setconfigureallbtn.addActionListener(SetConfigureAllAction());
        selectallbtn.addActionListener(SelectAllAction(selectallbtn));

        actionButtonsPanel.add(titleLabel);
        actionButtonsPanel.add(selectallbtn);
        actionButtonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        actionButtonsPanel.add(signbtn);
        actionButtonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        actionButtonsPanel.add(signSelectedbtn);
        actionButtonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        actionButtonsPanel.add(previewallbtn);
        actionButtonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        actionButtonsPanel.add(cleanbtn);
        actionButtonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        actionButtonsPanel.add(setconfigureallbtn);
        actionButtonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        actionButtonsPanel.add(loaddoclistbtn);
        actionButtonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        actionButtonsPanel.add(savedoclistbtn);
        actionButtonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        actionButtonsPanel.add(changefolderbtn);

        if (showConnectionButtons) {
            try {
                List<Connection> connections = ServicesUrlsIO.load();
                actionButtonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
                JLabel label = new JLabel(MessageUtils.t("connection_panel_get_documents_title"));
                actionButtonsPanel.add(label);

                for (Connection connection : connections) {
                    if (connection.getName().equals("Gaudi") || connection.getName().equals("Firmador Remoto")) {
                        continue;
                    }
                    if (connection.isLogged()) {
                        JButton requestDocsButton = new JButton(connection.getName());
                        requestDocsButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                        requestDocsButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

                        requestDocsButton.addActionListener(reloadDocumentsAction(connection, requestDocsButton));

                        actionButtonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
                        actionButtonsPanel.add(requestDocsButton);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public ActionListener reloadDocumentsAction(Connection connection, JButton requestDocsButton) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Connection currentConnection = gui.getConnectionPanel().getListConnections().stream()
                        .filter(c -> c.getName().equals(connection.getName()))
                        .findFirst()
                        .orElse(null);

                if (currentConnection == null) {
                    gui.showNotification(
                            "Conexión no encontrada",
                            GUISwing.NotificationType.ERROR);
                    return;
                }

                if (!currentConnection.isLogged()) {
                    gui.showNotification(
                            MessageUtils.t("guiswing_show_error_not_logged"),
                            GUISwing.NotificationType.ERROR);
                    return;
                }

                requestDocsButton.setEnabled(false);

                SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                    @Override
                    protected Boolean doInBackground() throws Exception {
                        return ConnectionUtils.reloadVirtualDocuments(currentConnection);
                    }

                    @Override
                    protected void done() {
                        try {
                            Boolean success = get();
                            if (success) {
                                gui.showNotification(
                                        MessageUtils.t("connection_panel_success_get_virtual_documents"),
                                        GUISwing.NotificationType.SUCCESS);
                            } else {
                                gui.showNotification(
                                        MessageUtils.t("connection_panel_error_get_virtual_documents"),
                                        GUISwing.NotificationType.ERROR);
                            }
                        } catch (Exception ex) {
                            gui.showNotification(
                                    MessageUtils.t("connection_panel_error_get_virtual_documents"),
                                    GUISwing.NotificationType.ERROR);
                            ex.printStackTrace();
                        } finally {
                            requestDocsButton.setEnabled(true);
                        }
                    }
                };

                worker.execute();
            }
        };
    }

    public ActionListener SelectAllAction(JButton button) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (panel.getDocuments().isEmpty()) {
                    gui.showNotification(MessageUtils.t("list_document_no_documents_to_sign"),
                            GUISwing.NotificationType.INFO);
                } else {
                    if (panel.getSelectedDocuments().size() == panel.getDocuments().size()) {
                        button.setText(MessageUtils.t("list_document_selectall"));
                        // Accesibilidad: Actualizar descripción cuando cambia el estado
                        button.getAccessibleContext()
                                .setAccessibleDescription(MessageUtils.t("accessibility_select_all_description"));
                        panel.setSelectedDocuments(new ArrayList<>());
                    } else {
                        panel.setSelectedDocuments(panel.getDocuments());
                        button.setText(MessageUtils.t("list_document_deselectall"));
                        // Accesibilidad: Actualizar descripción cuando cambia el estado
                        button.getAccessibleContext()
                                .setAccessibleDescription(MessageUtils.t("accessibility_deselect_all_description"));
                    }
                    panel.reloadView();
                }
            }
        };
    }

    public ActionListener SignAllAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (panel.getDocuments().isEmpty()) {
                    gui.showNotification(MessageUtils.t("list_document_no_documents_to_sign"),
                            GUISwing.NotificationType.INFO);
                } else {
                    (gui).signAllDocuments();
                    panel.reloadView();
                }
            }
        };
    }

    public ActionListener SignSelectedAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (panel.getSelectedDocuments().isEmpty()) {
                    gui.showNotification(MessageUtils.t("list_document_no_documents_to_sign"),
                            GUISwing.NotificationType.INFO);
                } else {
                    (gui).signSelectedDocuments();
                    panel.reloadView();
                }
            }
        };
    }

    public ActionListener SaveDocumentListAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    if (panel.getOnlyVirtual()) {
                        gui.showNotification(MessageUtils.t("list_document_no_virtual_documents_to_save"),
                                GUISwing.NotificationType.INFO);
                        return;
                    }
                    if (!panel.getDocuments().isEmpty()) {
                        saveDocumentList();
                    } else {
                        gui.showNotification(MessageUtils.t("list_document_no_documents_to_save"),
                                GUISwing.NotificationType.INFO);
                    }
                } catch (Exception e) {
                    gui.showNotification(MessageUtils.t("list_document_error_during_save"),
                            GUISwing.NotificationType.ERROR);
                }
            }
        };
    }

    public void saveDocumentList() throws IOException {
        File settingsDir = new File(FileSystems.getDefault()
                .getPath(SettingsManager.getInstance().getConfigDir().toString(), "docSettings").toString());
        if (settingsDir.exists())
            FileUtils.forceDelete(settingsDir);

        CSVWriter writer = new CSVWriter(new FileWriter(this.documentListSavePath.toString()));
        writer.writeNext(new String[] { "name", "pathName", "pathToSave", "settingsPath" }); // write the header for the
                                                                                             // document
        List<Document> savedDocuments = new ArrayList<Document>();
        if (panel.getOnlyVirtual()) {
            gui.showNotification(MessageUtils.t("list_document_no_virtual_documents_to_save"),
                    GUISwing.NotificationType.INFO);
            writer.close();
            return;
        }
        if (!panel.getDocuments().isEmpty()) {
            if (panel.getSelectedDocuments().isEmpty()) {
                savedDocuments = panel.getDocuments();
            } else {
                savedDocuments = panel.getSelectedDocuments();
            }
            for (Document document : savedDocuments) {
                String settingsPath = SettingsManager.getInstance()
                        .saveDocumentSettings(document.getSettings(), document.getName());
                String[] dataToSave = {
                        document.getName(),
                        document.getPathName(),
                        document.getPathToSave(),
                        settingsPath
                };
                writer.writeNext(dataToSave);
            }
        } else {
            gui.showNotification(MessageUtils.t("list_document_no_documents_to_save"), GUISwing.NotificationType.INFO);
        }
        writer.close();
        gui.showNotification(MessageUtils.t("list_document_save_done") + " " + this.documentListSavePath.toString(),
                GUISwing.NotificationType.SUCCESS);
    }

    public ActionListener LoadDocumentListAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    if (Files.exists(documentListSavePath)) {
                        loadDocumentList();
                    } else {
                        gui.showNotification(MessageUtils.t("list_document_no_file_to_load"),
                                GUISwing.NotificationType.INFO);
                    }
                    panel.reloadView();
                } catch (Exception e) {
                    LOG.info("Error loading document list: ", e);
                    gui.showNotification(MessageUtils.t("list_document_error_during_load"),
                            GUISwing.NotificationType.ERROR);
                }
            }
        };
    }

    public void updateDocument(List<Document> documents) {

    }

    public void loadDocumentList() throws Exception {
        ArrayList<String> docsNotFound = new ArrayList<>();
        CSVReader reader = new CSVReader(new FileReader(this.documentListSavePath.toFile()));
        panel.cleanDocuments(); // clear the list before loading what is saved
        File[] files = new File[1];
        String[] fileLine;
        reader.readNext(); // to get the header, ignore it
        while ((fileLine = reader.readNext()) != null) {
            String documentPath = fileLine[1];
            File f = new File(documentPath);

            if (Files.exists(Paths.get(documentPath))) {
                // load the document only if it exists in the filesystem
                files[0] = f;
                List<Document> docs = gui.addDocumentsSynchronous(files);
                Document document = docs.get(0);
                // set the document settings, so it is as it was before saving/loading
                document.setSettings(SettingsManager.getInstance().loadDocumentSettings(fileLine[3]));

                // update the path to save in case the user selected something different from
                // default before saving
                document.setPathToSave(fileLine[2]);
                panel.updateDocument(document);
            } else {
                docsNotFound.add(documentPath);
            }
        }
        reader.close();
        if (!docsNotFound.isEmpty()) {
            gui.showNotification(
                    MessageUtils.t("list_document_files_not_found_on_load") + " " + String.join(", ", docsNotFound),
                    GUISwing.NotificationType.ERROR);
        } else {
            gui.showNotification(MessageUtils.t("list_document_files_success"), GUISwing.NotificationType.SUCCESS);
        }
    }

    public ActionListener CleanDocumentsAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                panel.cleanDocuments();
                panel.getGui().clearDone();
                panel.reloadView();
            }
        };
    }

    public ActionListener PreviewAllAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (!panel.getDocuments().isEmpty()) {
                    ((GUISwing) panel.getGui()).processAllDocuments();
                } else {
                    gui.showNotification(MessageUtils.t("list_document_previewall_empty_action"),
                            GUISwing.NotificationType.INFO);
                }
            }
        };
    }

    public ActionListener ChangeFolderAction() {
        return new ActionListener() {
            private String showLoadDialog() {
                JFileChooser f = new JFileChooser();
                f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                // f.setAcceptAllFileFilterUsed(false);
                if (f.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File currentfolder = f.getSelectedFile();
                    if (!currentfolder.exists()) {
                        gui.showNotification(MessageUtils.t("list_document_changefolder_notfound"),
                                GUISwing.NotificationType.ERROR);
                        return null;
                    }
                    return currentfolder.getPath();
                }
                return null;
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (panel.getOnlyVirtual()) {
                    gui.showNotification(MessageUtils.t("list_document_no_virtual_documents_to_save"),
                            GUISwing.NotificationType.INFO);
                    return;
                }
                if (!panel.getDocuments().isEmpty()) {
                    String directory = showLoadDialog();
                    if (directory != null) {
                        for (Document doc : panel.getDocuments()) {
                            doc.setPathToSave(directory + File.separatorChar + doc.getName());
                        }
                    }
                    gui.showNotification(MessageUtils.t("list_document_save_done") + " " + directory,
                            GUISwing.NotificationType.SUCCESS);
                    panel.reloadView();
                } else {
                    gui.showNotification(MessageUtils.t("list_document_no_documents_to_sign"),
                            GUISwing.NotificationType.INFO);
                }
            }
        };
    }

    public ActionListener SetConfigureAllAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (panel.getOnlyVirtual()) {
                    gui.showNotification(MessageUtils.t("list_document_no_virtual_documents_to_config"),
                            GUISwing.NotificationType.INFO);
                    return;
                }
                if (!panel.getDocuments().isEmpty()) {
                    for (Document doc : panel.getDocuments()) {
                        doc.setSettings(panel.getGui().getCurrentSettings());
                    }
                    gui.showNotification(MessageUtils.t("list_document_setconfigureall_success"),
                            GUISwing.NotificationType.SUCCESS);
                } else {
                    gui.showNotification(MessageUtils.t("list_document_no_documents_to_sign"),
                            GUISwing.NotificationType.ERROR);
                }
            }
        };
    }

    /// Methods for sorting
    public ActionListener OrderByNameAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (panel.getRealDocuments().isEmpty()) {
                    gui.showNotification(MessageUtils.t("list_document_no_documents_to_sign"),
                            GUISwing.NotificationType.INFO);
                } else {
                    if (orderAscByName) {
                        panel.getRealDocuments().sort(Comparator.comparing(Document::getName));
                    } else {
                        panel.getRealDocuments().sort(Comparator.comparing(Document::getName).reversed());
                    }
                    orderAscByName = !orderAscByName;
                    panel.getScrollPane().getVerticalScrollBar().setValue(0);
                    panel.reloadView();
                }
            }
        };
    }

    public ActionListener OrderByNumberSignAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (panel.getRealDocuments().isEmpty()) {
                    gui.showNotification(MessageUtils.t("list_document_no_documents_to_sign"),
                            GUISwing.NotificationType.INFO);
                } else {
                    if (orderAscBySignatures) {
                        panel.getRealDocuments().sort(Comparator.comparingInt(Document::amountOfSignatures));
                    } else {
                        panel.getRealDocuments().sort(Comparator.comparingInt(Document::amountOfSignatures).reversed());
                    }
                    orderAscBySignatures = !orderAscBySignatures;
                    panel.getScrollPane().getVerticalScrollBar().setValue(0);
                    panel.reloadView();
                }
            }
        };
    }

    public ActionListener OrderByNumberOfPagesAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (panel.getRealDocuments().isEmpty()) {
                    gui.showNotification(MessageUtils.t("list_document_no_documents_to_sign"),
                            GUISwing.NotificationType.INFO);
                } else {
                    if (orderAscByNumberPages) {
                        panel.getRealDocuments().sort(Comparator.comparingInt(Document::getNumberOfPages));
                    } else {
                        panel.getRealDocuments().sort(Comparator.comparingInt(Document::getNumberOfPages).reversed());
                    }
                    orderAscByNumberPages = !orderAscByNumberPages;
                    panel.getScrollPane().getVerticalScrollBar().setValue(0);
                    panel.reloadView();
                }
            }
        };
    }

    public ActionListener OrderByCreateAtAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (panel.getRealDocuments().isEmpty()) {
                    gui.showNotification(MessageUtils.t("list_document_no_documents_to_sign"),
                            GUISwing.NotificationType.INFO);
                } else {
                    if (orderAscByDate) {
                        panel.getRealDocuments().sort(Comparator.comparing(doc -> {
                            try {
                                return parseDate(doc.getCreatedAt());
                            } catch (Exception e) {
                                return new Date(0); // Fecha por defecto si hay error
                            }
                        }));
                    } else {
                        panel.getRealDocuments().sort(Comparator.comparing((Document doc) -> {
                            try {
                                return parseDate(doc.getCreatedAt());
                            } catch (Exception e) {
                                return new Date(0);
                            }
                        }).reversed());
                    }
                    orderAscByDate = !orderAscByDate;
                    panel.getScrollPane().getVerticalScrollBar().setValue(0);
                    panel.reloadView();
                }
            }
        };
    }

    public ActionListener OrderByExpirationDateAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (panel.getRealDocuments().isEmpty()) {
                    gui.showNotification(MessageUtils.t("list_document_no_documents_to_sign"),
                            GUISwing.NotificationType.INFO);
                } else {
                    if (orderAscByExpiration) {
                        panel.getRealDocuments().sort(Comparator.comparing(doc -> {
                            try {
                                return parseDate(doc.getExpirationDate());
                            } catch (Exception e) {
                                return new Date(Long.MAX_VALUE); // Fecha futura si hay error
                            }
                        }));
                    } else {
                        panel.getRealDocuments().sort(Comparator.comparing((Document doc) -> {
                            try {
                                return parseDate(doc.getExpirationDate());
                            } catch (Exception e) {
                                return new Date(Long.MAX_VALUE);
                            }
                        }).reversed());
                    }
                    orderAscByExpiration = !orderAscByExpiration;
                    panel.getScrollPane().getVerticalScrollBar().setValue(0);
                    panel.reloadView();
                }
            }
        };
    }

    private Date parseDate(String dateString) throws Exception {
        if (dateString == null || dateString.isEmpty()) {
            throw new Exception("Fecha vacía");
        }
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        return format.parse(dateString);
    }

    public void setShowConnectionButtons(boolean showConnectionButtons) {
        this.showConnectionButtons = showConnectionButtons;
        actionButtonsPanel.removeAll();
        loadActionButtons();
        actionButtonsPanel.revalidate();
        actionButtonsPanel.repaint();
    }

    public boolean getShowConnectionButtons() {
        return showConnectionButtons;
    }
}
