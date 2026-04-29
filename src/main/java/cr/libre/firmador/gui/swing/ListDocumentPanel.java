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

import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.connections.Connection;
import cr.libre.firmador.documents.Document;
import cr.libre.firmador.documents.DocumentChangeListener;
import cr.libre.firmador.gui.GUIInterface;
import cr.libre.firmador.gui.GUISwing;
import cr.libre.firmador.services.ListDocumentPanelService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class ListDocumentPanel extends JPanel implements DocumentChangeListener {
    private JPanel listContainer;
    private JToggleButton localDocumentsButton;
    private JToggleButton virtualDocumentsButton;
    private JScrollPane scrollPane;
    private List<Document> documents;
    private List<Document> selectedDocuments = new ArrayList<>();
    private JPanel actionButtonsPanel;
    private JPanel actionSearchButtons;
    private JPanel actionSortingButtons;
    private JPanel rightPanel;
    private GUIInterface gui;
    public ValidatePanel validatePanel;
    private ButtonActionsController controller;
    private CardLayout cardLayout;
    private String lastAction = "na";
    static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private int listIndexSelected = 0;
    private Document selectedDocument;
    private boolean onlyVirtual = false;
    private ListDocumentPanelService service;
    private ConnectionPanel connectionPanel;

    @SuppressWarnings("this-escape")
    public ListDocumentPanel() {
        service = new ListDocumentPanelService(this);
        documents = new ArrayList<>();
        setLayout(new BorderLayout());

        actionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionButtonsPanel.setLayout(new BoxLayout(actionButtonsPanel, BoxLayout.Y_AXIS));

        actionSearchButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));

        actionSortingButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        actionSortingButtons.setLayout(new BoxLayout(actionSortingButtons, BoxLayout.Y_AXIS));

        controller = new ButtonActionsController(this, (GUISwing) gui, actionButtonsPanel, actionSortingButtons,
                actionSearchButtons);

        controller.loadSearchButtons();
        controller.loadActionButtons();
        controller.loadSortingButtons();

        add(actionSearchButtons, BorderLayout.NORTH);

        JSplitPane buttonsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, actionButtonsPanel,
                actionSortingButtons);
        buttonsSplitPane.getAccessibleContext().setAccessibleName(
                MessageUtils.t("list_document_buttons_split_pane_accessible"));
        buttonsSplitPane.setResizeWeight(0.5);
        buttonsSplitPane.setDividerLocation(0.5);
        buttonsSplitPane.setPreferredSize(new Dimension(50, Integer.MAX_VALUE));

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());

        JPanel buttonsPanel = new JPanel();
        localDocumentsButton = new JToggleButton(MessageUtils.t("list_document_button_local"));
        localDocumentsButton.setMnemonic(MessageUtils.k('L'));
        localDocumentsButton.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("list_document_panel_local_button_accessible"));
        localDocumentsButton.addActionListener(e -> {
            toggleToLocals();
        });
        localDocumentsButton.setVisible(false);
        localDocumentsButton.setEnabled(false);
        virtualDocumentsButton = new JToggleButton(MessageUtils.t("list_document_button_virtual"));
        virtualDocumentsButton.setMnemonic(MessageUtils.k('V'));
        virtualDocumentsButton.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("list_document_panel_virtual_button_accessible"));
        virtualDocumentsButton.addActionListener(e -> {
            onlyVirtual = true;
            reloadView();
            if (!selectedDocuments.isEmpty() && selectedDocuments.size() == getDocuments().size()) {
                controller.getSelectAllButton().setText(MessageUtils.t("list_document_deselectall"));
            } else {
                controller.getSelectAllButton().setText(MessageUtils.t("list_document_selectall"));
            }
            controller.getChangefolderbtn().setVisible(false);
            controller.getSavedoclistbtn().setVisible(false);
            controller.getLoaddoclistbtn().setVisible(false);
            controller.setShowConnectionButtons(true);

        });
        virtualDocumentsButton.setVisible(false);
        virtualDocumentsButton.setEnabled(false);
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(localDocumentsButton);
        buttonGroup.add(virtualDocumentsButton);
        localDocumentsButton.setSelected(true);
        buttonsPanel.add(localDocumentsButton);
        buttonsPanel.add(virtualDocumentsButton);
        leftPanel.add(buttonsPanel, BorderLayout.NORTH);

        listContainer = new JPanel();
        listContainer.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("list_document_list_container_accessible"));
        listContainer.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("list_document_list_container_accessible_description"));
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        scrollPane = new JScrollPane(listContainer);
        scrollPane.getAccessibleContext().setAccessibleName(MessageUtils.t("list_document_scroll_pane_accessible"));
        leftPanel.add(scrollPane, BorderLayout.CENTER);

        rightPanel = new JPanel();
        cardLayout = new CardLayout();
        rightPanel.setLayout(cardLayout);
        validatePanel = new ValidatePanel();
        rightPanel.add(validatePanel.getValidateScrollPane(), "validatePanel");
        rightPanel.add(buttonsSplitPane, "actionButtonsPanel");

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.getAccessibleContext().setAccessibleName(null);
        splitPane.setResizeWeight(0.6);
        splitPane.setDividerLocation(0.6);

        add(splitPane, BorderLayout.CENTER);
    }

    // Método para agregar documents
    public void addDocuments(List<Document> docs) {
        if (docs.isEmpty())
            return;
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            if (docs.size() == 1) {
                documents.add(0, doc);
            } else {
                documents.add(doc);
            }
            doc.registerListener(this);
        }
        reloadView();
    }

    public void setGUI(GUIInterface gui) {
        this.gui = gui;
        this.controller.setGui((GUISwing) gui);
    }

    private Color getColor() {
        Color selectedColor;
        if (UIManager.getColor("Panel.background").getRed() < 100) {
            // Tema oscuro
            selectedColor = new Color(70, 80, 100);
        } else {
            // Tema claro
            selectedColor = new Color(200, 220, 255);
        }
        return selectedColor;
    }

    // Refresca la lista de documents en pantalla
    public void reloadView() {
        if (connectionPanel != null) {
            List<Connection> connections = connectionPanel.getListConnections();
            for (Connection connection : connections) {
                if (connection.getName().equals("Gaudi") || connection.getName().equals("Firmador Remoto")) {
                    continue;
                }
                if (connection.isRunning()) {
                    localDocumentsButton.setVisible(true);
                    localDocumentsButton.setEnabled(true);
                    virtualDocumentsButton.setVisible(true);
                    virtualDocumentsButton.setEnabled(true);
                    break;
                }
                localDocumentsButton.setVisible(false);
                localDocumentsButton.setEnabled(false);
                virtualDocumentsButton.setVisible(false);
                virtualDocumentsButton.setEnabled(false);
            }
        }

        listContainer.removeAll();
        List<Document> documents = this.documents;
        if (onlyVirtual) {
            documents = documents.stream()
                    .filter(Document::isVirtual)
                    .collect(Collectors.toCollection(ArrayList::new));
        } else {
            documents = documents.stream()
                    .filter(d -> !d.isVirtual())
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        for (Document doc : documents) {
            JPanel docPanel = getJPanel(doc);
            docPanel.getAccessibleContext()
                    .setAccessibleName(MessageUtils.t("list_document_docPanel_accessible") + doc.getName());
            docPanel.getAccessibleContext()
                    .setAccessibleDescription(MessageUtils.t("list_document_docPanel_accessible_description")
                            + (doc.isValid() ? MessageUtils.t("list_document_docPanel_accessible_description2")
                                    : MessageUtils.t("list_document_docPanel_accessible_description3")));

            // Panel del Icono
            JPanel iconPanel = new JPanel(new BorderLayout());
            JCheckBox checkBox = new JCheckBox();
            checkBox.getAccessibleContext().setAccessibleName(MessageUtils.t("list_document_checkbox_accessible"));
            checkBox.getAccessibleContext()
                    .setAccessibleDescription(MessageUtils.t("list_document_checkbox_accessible_description"));
            checkBox.setSelected(selectedDocuments.contains(doc));
            checkBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            iconPanel.add(checkBox, BorderLayout.NORTH);
            Icon fileIcon;
            // Archivo del cual quieres obtener el icono del sistema
            if (!doc.isVirtual()) {
                File file = new File(doc.getPathName());
                fileIcon = FileSystemView.getFileSystemView().getSystemIcon(file);
            } else {
                // Usar un icono genérico de PDF
                fileIcon = UIManager.getIcon("FileView.fileIcon");
                // o si tienes un icono propio para PDF en tus recursos:
                // fileIcon = new ImageIcon(getClass().getResource("/icons/pdf.png"));
            }
            // fallback por si no encuentra ningún icono
            if (fileIcon == null) {
                fileIcon = UIManager.getIcon("FileView.fileIcon");
            }

            // Convertir el Icon a ImageIcon (para escalarlo)
            BufferedImage bufferedImage = new BufferedImage(
                    fileIcon.getIconWidth(),
                    fileIcon.getIconHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics g = bufferedImage.createGraphics();
            fileIcon.paintIcon(null, g, 0, 0);
            g.dispose();

            // Escalar el icono
            Image scaledImage = getHighQualityScaledImage(bufferedImage, 32, 32);
            ImageIcon scaledIcon = new ImageIcon(scaledImage);

            // Crear el botón con el icono del archivo
            JButton iconButton = new JButton(scaledIcon);
            iconButton.getAccessibleContext().setAccessibleName(
                    MessageUtils.t("list_document_icon_button_accessible") + " " + doc.getName());
            iconButton.getAccessibleContext().setAccessibleDescription(
                    MessageUtils.t("list_document_icon_button_accessible_description"));
            iconButton.setPreferredSize(new Dimension(48, 48));
            iconButton.setBorder(BorderFactory.createEmptyBorder(5, 0, 20, 10));
            iconButton.setContentAreaFilled(false);
            iconButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Añadir al panel
            iconPanel.add(iconButton, BorderLayout.CENTER);
            docPanel.add(iconPanel, BorderLayout.WEST);

            // Panel de información
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
            infoPanel.getAccessibleContext().setAccessibleName(null);
            infoPanel.getAccessibleContext().setAccessibleDescription(null);

            JLabel pathLabel = new JLabel(doc.getName());
            pathLabel.getAccessibleContext().setAccessibleName(
                    MessageUtils.t("list_document_path_label_accessible"));
            pathLabel.getAccessibleContext().setAccessibleDescription(doc.getName());
            pathLabel.setFont(pathLabel.getFont().deriveFont(Font.BOLD, 18f)); // Negrita y tamaño opcional
            pathLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            infoPanel.add(pathLabel);
            String pathToSave = doc.getOrigin();
            if (!doc.isVirtual()) {
                pathToSave = doc.getPathToSave();
            }
            JButton saveButton = null;
            if (doc.isVirtual()) {
                saveButton = new JButton(MessageUtils.t("list_document_panel_origin") + pathToSave);
                saveButton.getAccessibleContext().setAccessibleName(
                        MessageUtils.t("list_document_save_button_accessible_virtual"));
                saveButton.getAccessibleContext().setAccessibleDescription(
                        MessageUtils.t("list_document_save_button_accessible_description_virtual") + pathToSave);
            } else {
                saveButton = new JButton(MessageUtils.t("list_document_panel_exit_path") + pathToSave);
                saveButton.getAccessibleContext().setAccessibleName(
                        MessageUtils.t("list_document_save_button_accessible_local"));
                saveButton.getAccessibleContext().setAccessibleDescription(
                        MessageUtils.t("list_document_save_button_accessible_description_local") + pathToSave);
            }
            infoPanel.add(saveButton);
            JPanel firmaPaginaPanel = new JPanel();
            firmaPaginaPanel.setLayout(new BoxLayout(firmaPaginaPanel, BoxLayout.X_AXIS));
            firmaPaginaPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            firmaPaginaPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            firmaPaginaPanel.getAccessibleContext().setAccessibleName(null);
            firmaPaginaPanel.getAccessibleContext().setAccessibleDescription(null);
            int amountOfSignatures = 0;
            if (doc.isVirtual()) {
                amountOfSignatures = doc.getAmountOfSignatures();
            } else {
                amountOfSignatures = doc.amountOfSignatures();
            }
            JLabel amountOfSignaturesLabel = new JLabel(
                    "✍️ " + MessageUtils.t("list_document_panel_sign") + amountOfSignatures);
            amountOfSignaturesLabel.getAccessibleContext().setAccessibleName(
                    MessageUtils.t("list_document_panel_amount_of_signatures_label_accessible"));
            amountOfSignaturesLabel.getAccessibleContext().setAccessibleDescription(
                    MessageUtils.t("list_document_panel_amount_of_signatures_label_accessible_description")
                            + amountOfSignatures);
            amountOfSignaturesLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));

            JButton signButton = new JButton(MessageUtils.t("list_document_panel_sign_action"));
            signButton.getAccessibleContext().setAccessibleName(
                    MessageUtils.t("list_document_sign_button_accessible"));
            signButton.getAccessibleContext().setAccessibleDescription(
                    MessageUtils.t("list_document_sign_button_accessible_description") + amountOfSignatures);
            String pages = "0";
            if (doc.isVirtual()) {
                pages = doc.getPages() + "";
            } else {
                pages = doc.getNumberOfPages() + "";
            }

            JLabel pagesLabel = new JLabel("📄 " + MessageUtils.t("list_document_panel_pages") + pages);
            pagesLabel.getAccessibleContext().setAccessibleName(
                    MessageUtils.t("list_document_panel_pages_label_accessible") + pages);
            pagesLabel.getAccessibleContext().setAccessibleDescription(
                    MessageUtils.t("list_document_panel_pages_label_accessible_description") + pages);
            pagesLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));

            JButton pageButton = new JButton(MessageUtils.t("list_document_panel_page_action"));
            pageButton.getAccessibleContext().setAccessibleName(
                    MessageUtils.t("list_document_page_button_accessible") + pages);

            firmaPaginaPanel.add(amountOfSignaturesLabel);
            firmaPaginaPanel.add(signButton);
            firmaPaginaPanel.add(Box.createRigidArea(new Dimension(5, 0)));
            firmaPaginaPanel.add(pagesLabel);
            firmaPaginaPanel.add(pageButton);
            infoPanel.add(firmaPaginaPanel);

            docPanel.add(infoPanel, BorderLayout.CENTER);

            // Panel de acciones
            JPanel actionPanel = new JPanel();
            actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
            actionPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
            actionPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
            actionPanel.getAccessibleContext().setAccessibleName(null);
            actionPanel.getAccessibleContext().setAccessibleDescription(null);

            JPanel deleteButtonWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            JButton deleteButton = new JButton("X");
            deleteButton.getAccessibleContext().setAccessibleName(
                    MessageUtils.t("list_document_delete_button_accessible") + doc.getName());

            deleteButton.setFocusPainted(false);
            deleteButton.setContentAreaFilled(false);
            deleteButton.setBorderPainted(false);
            deleteButton.setOpaque(false);
            deleteButton.setHorizontalAlignment(SwingConstants.CENTER);
            deleteButton.setVerticalAlignment(SwingConstants.CENTER);
            deleteButtonWrapper.add(deleteButton);
            deleteButtonWrapper.setOpaque(false);
            deleteButtonWrapper.getAccessibleContext().setAccessibleName(null);
            deleteButtonWrapper.getAccessibleContext().setAccessibleDescription(null);
            actionPanel.add(deleteButtonWrapper);

            // Agregar espacio flexible para empujar el contenido hacia arriba y abajo
            actionPanel.add(Box.createVerticalGlue());

            if (doc.isVirtual()) {
                // Agregar fecha de expiración para documentos virtuales
                JLabel expirationLabel = new JLabel();
                String expirationDate = doc.getExpirationDate();
                if (expirationDate != null && !expirationDate.isEmpty()) {
                    expirationLabel.setText(MessageUtils.t("list_document_panel_expiration") + " " + expirationDate);
                    expirationLabel.getAccessibleContext().setAccessibleName(
                            MessageUtils.t("list_document_expiration_label_accessible"));
                    expirationLabel.getAccessibleContext().setAccessibleDescription(
                            MessageUtils.t("list_document_expiration_label_accessible_description") + expirationDate);
                    expirationLabel.setFont(expirationLabel.getFont().deriveFont(Font.PLAIN, 11f));
                    expirationLabel.setForeground(Color.GRAY);
                    expirationLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
                    actionPanel.add(expirationLabel);
                }
            } else {
                JLabel wasValidated = getValidated(doc);
                actionPanel.add(wasValidated);
            }

            docPanel.add(actionPanel, BorderLayout.EAST);

            // Acciones
            docPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showValidatePanel();
                    ActionListener validateAction = service.getGoToValidateActionListener(doc, true);
                    validateAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
                    selectedDocument = doc;

                }
            });

            docPanel.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER ||
                            e.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
                        showValidatePanel();
                        ActionListener validateAction = service.getGoToValidateActionListener(doc, true);
                        validateAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
                        selectedDocument = doc;
                    } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN ||
                            e.getKeyCode() == java.awt.event.KeyEvent.VK_UP) {
                        Component[] components = listContainer.getComponents();
                        int currentIndex = -1;

                        for (int i = 0; i < components.length; i++) {
                            if (components[i] == docPanel) {
                                currentIndex = i;
                                break;
                            }
                        }

                        int newIndex = currentIndex;
                        if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN) {
                            newIndex = Math.min(currentIndex + 1, components.length - 1);
                        } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_UP) {
                            newIndex = Math.max(currentIndex - 1, 0);
                        }

                        if (newIndex != currentIndex && newIndex >= 0 && newIndex < components.length) {
                            components[newIndex].requestFocusInWindow();
                            scrollPane.getViewport().scrollRectToVisible(components[newIndex].getBounds());
                        }
                    }
                }
            });

            if (!doc.isVirtual()) {
                saveButton.addActionListener(service.getChooseSaveFileActionListener(doc));
                iconButton.addActionListener(service.getChangeFormatActionListener(doc));
            }
            pageButton.addActionListener(service.getGoToSignActionListener(doc));
            deleteButton.addActionListener(service.getCleanDocumentAction(doc));
            signButton.addActionListener(service.getSignActionListener(doc));
            checkBox.addActionListener(service.getSelectDocumentActionListener(doc, checkBox));

            listContainer.add(docPanel);
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    @NotNull
    private JPanel getJPanel(Document doc) {
        JPanel docPanel = new JPanel(new BorderLayout());
        //Color originalColor = docPanel.getBackground();

        if (selectedDocument == doc) {
            docPanel.setBackground(getColor());
        }

        docPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5),
                BorderFactory.createLineBorder(Color.GRAY, 1)));

        docPanel.setFocusable(true);

        docPanel.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                docPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(5, 5, 5, 5),
                        BorderFactory.createLineBorder(Color.BLUE, 2)));
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                docPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(5, 5, 5, 5),
                        BorderFactory.createLineBorder(Color.GRAY, 1)));
            }
        });

        docPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));
        return docPanel;
    }

    @NotNull
    private static JLabel getValidated(Document doc) {
        JLabel wasValidated = new JLabel(MessageUtils.t("list_document_panel_validated"));
        boolean isvalid = false;
        if (doc.getValidator() != null) {
            if (doc.isValid()) {
                isvalid = doc.isValid();
            } else {
                isvalid = doc.getDocumentIsValidate();
            }
        } else {
            isvalid = doc.getDocumentIsValidate();
        }
        wasValidated.setVisible(false);
        if (isvalid) {
            wasValidated.setVisible(true);
        }
        return wasValidated;
    }

    private Image getHighQualityScaledImage(BufferedImage originalImage, int width, int height) {
        BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(originalImage, 0, 0, width, height, null);
        g2d.dispose();
        return scaledImage;
    }

    public void setSelectedDocument(Document document) {
        selectedDocument = document;
        reloadView();
    }

    public List<Document> getRealDocuments() {
        return documents;
    }

    public List<Document> getDocuments() {
        if (onlyVirtual) {
            return documents.stream()
                    .filter(Document::isVirtual)
                    .collect(Collectors.toCollection(ArrayList::new));
        } else {
            return documents.stream()
                    .filter(d -> !d.isVirtual())
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }

    public List<Document> getVirtualDocuments() {
        return documents.stream()
                .filter(Document::isVirtual)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public void removeDocument(Document document) {
        documents.remove(document);
        reloadView();
    }

    public void removeDocuments(List<Document> documents) {
        this.documents.removeAll(documents);
        ((GUISwing) gui).showNotification(MessageUtils.t("list_document_delete_documents"),
                GUISwing.NotificationType.INFO);
        reloadView();
    }

    public List<Document> getAllSelectedDocuments() {
        return selectedDocuments;
    }

    public List<Document> getSelectedDocuments() {
        if (onlyVirtual) {
            return selectedDocuments.stream()
                    .filter(Document::isVirtual)
                    .collect(Collectors.toCollection(ArrayList::new));
        } else {
            return selectedDocuments.stream()
                    .filter(d -> !d.isVirtual())
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }

    public void setSelectedDocuments(List<Document> selectedDocuments) {
        this.selectedDocuments = selectedDocuments;
    }

    public void cleanDocuments() {
        List<Document> clearDocuments;

        if (onlyVirtual) {
            clearDocuments = this.documents.stream()
                    .filter(d -> !d.isVirtual())
                    .collect(Collectors.toCollection(ArrayList::new));
        } else {
            clearDocuments = this.documents.stream()
                    .filter(Document::isVirtual)
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        ((GUISwing) gui).showNotification(MessageUtils.t("list_document_clear_documents"),
                GUISwing.NotificationType.SUCCESS);
        this.documents = clearDocuments;
    }

    public void updateDocument(Document document) {
        for (int i = 0; i < documents.size(); i++) {
            if (documents.get(i).equals(document)) {
                documents.set(i, document);
                break;
            }
        }
        reloadView();
    }

    @Override
    public void previewDone(Document document) {
        ((GUISwing) gui).showNotification(MessageUtils.t("guiswing_success_preview_document"),
                GUISwing.NotificationType.SUCCESS);
        reloadView();
    }

    @Override
    public void previewAllDone() {
        ((GUISwing) gui).showNotification(MessageUtils.t("guiswing_success_preview_documents"),
                GUISwing.NotificationType.SUCCESS);

        if (documents.isEmpty())
            return;
        Document document = documents.get(listIndexSelected);
        GUISwing gui = (GUISwing) document.getGUI();
        if (lastAction.equals("preview")) {
            service.getGoToSignActionListener(document).actionPerformed(null);
        } else if (lastAction.equals("sign")) {
            listIndexSelected = 0;
            gui.signDocument(document);
        }
        lastAction = "na";
        gui.loadReportDocument(document, false);
        reloadView();
    }

    @Override
    public void validateDone(Document document) {
        ((GUISwing) gui).showNotification(MessageUtils.t("guiswing_success_validate_document"),
                GUISwing.NotificationType.SUCCESS);
        reloadView();
    }

    @Override
    public void validateAllDone() {
        ((GUISwing) gui).showNotification(MessageUtils.t("guiswing_success_validate_documents"),
                GUISwing.NotificationType.SUCCESS);
        reloadView();
    }

    @Override
    public void signDone(Document document) {
        if (document.getSignwithErrors()) {
            ((GUISwing) gui).showNotification(MessageUtils.t("guiswing_success_sing_document_error"),
                    GUISwing.NotificationType.ERROR);
        } else {
            ((GUISwing) gui).showNotification(MessageUtils.t("guiswing_success_sing_document"),
                    GUISwing.NotificationType.SUCCESS);
        }
        if (!document.getIsremote()) {
            if (documents.contains(document)) {
                if (!document.getSignwithErrors()) {
                    // remove the non-signed document and add the signed one to the list of
                    // documents
                    documents.remove(document);
                    File[] files = new File[1];
                    File doc = new File(document.getPathToSave());
                    files[0] = doc;
                    ((GUISwing) gui).addDocuments(files, false);
                    ((GUISwing) gui).getLoadDialogWorker().setVisible(true);
                }
            }
        }
    }

    @Override
    public void signAllDone() {

    }

    @Override
    public void extendsDone(Document document) {

    }

    @Override
    public void clearDone() {

    }

    public GUIInterface getGui() {
        return this.gui;
    }

    public void showValidatePanel() {
        cardLayout.show(rightPanel, "validatePanel");
    }

    public void showActionButtonsPanel() {
        // SwingUtilities.invokeLater(() -> actionButtonsPanel.requestFocusInWindow());
        cardLayout.show(rightPanel, "actionButtonsPanel");
    }

    public Document getActiveDocument() {
        Document returnedDocument = null;
        if (!documents.isEmpty()) {
            returnedDocument = documents.get(0);
        }
        return returnedDocument;
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public boolean getOnlyVirtual() {
        return onlyVirtual;
    }

    public void setOnlyVirtual(boolean onlyVirtual) {
        this.onlyVirtual = onlyVirtual;
    }

    public String getLastAction() {
        return lastAction;
    }

    public void setLastAction(String lastAction) {
        this.lastAction = lastAction;
    }

    public void setListIndexSelected(int index) {
        this.listIndexSelected = index;
    }

    public JToggleButton getLocalDocumentsButton() {
        return localDocumentsButton;
    }

    public void setConnectionPanel(ConnectionPanel connectionPanel) {
        this.connectionPanel = connectionPanel;
    }

    public void toggleToLocals() {
        onlyVirtual = false;
        reloadView();
        if (!selectedDocuments.isEmpty() && selectedDocuments.size() == getDocuments().size()) {
            controller.getSelectAllButton().setText(MessageUtils.t("list_document_deselectall"));
        } else {
            controller.getSelectAllButton().setText(MessageUtils.t("list_document_selectall"));
        }
        controller.getChangefolderbtn().setVisible(true);
        controller.getSavedoclistbtn().setVisible(true);
        controller.getLoaddoclistbtn().setVisible(true);
        controller.setShowConnectionButtons(false);
    }
}
