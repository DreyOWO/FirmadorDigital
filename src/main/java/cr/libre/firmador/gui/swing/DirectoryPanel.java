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
import cr.libre.firmador.documents.Document;
import cr.libre.firmador.gui.GUIInterface;
import cr.libre.firmador.gui.GUISwing;
import cr.libre.firmador.signers.FirmadorASiC;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
//import javax.accessibility.AccessibleContext;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class DirectoryPanel extends JPanel {
    final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private JButton selectDirectoryButton;
    private JButton signALlButton;
    private JButton signAllDocumentButton;
    private JButton signAsicButton;
    private JButton saveAsButton;
    private JButton saveAsButtonName;
    private JButton clearButton;
    private JPanel detailPanel;
    private JPanel listContainer;
    private List<Document> documents;
    private GUISwing swing;
    private GUIInterface gui;
    private List<String> directories;
    private JScrollPane scrollPaneDetail;
    private String selectedDirectory;
    private SwingMainWindowFrame frame;
    //private boolean isDocumentName = false; //FIXME defined later locally, better remove this to prevent confusion

    @SuppressWarnings("this-escape")
    public DirectoryPanel(SwingMainWindowFrame frame) {
        this.frame = frame;
        loadDirectoryPanel();
    }

    public void loadDirectoryPanel() {
        // Panel principal con BorderLayout (tendrá el botón arriba y el resto del contenido en el centro)
        directories = new ArrayList<>();
        documents = new ArrayList<>();
        setLayout(new BorderLayout());

        // Panel superior
        selectDirectoryButton = new JButton(MessageUtils.t("directory_panel_select_directory"));
        selectDirectoryButton.addActionListener(new SelectDirectoryActionListener(this));
        // Accesibilidad: Botón seleccionar directorio
        selectDirectoryButton.setMnemonic(MessageUtils.k('S'));
        selectDirectoryButton.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_select_directory_button"));
        selectDirectoryButton.getAccessibleContext().setAccessibleDescription(MessageUtils.t("accessibility_select_directory_description"));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(selectDirectoryButton);

        clearButton = new JButton(MessageUtils.t("directoty_empty_action"));
        clearButton.addActionListener(new ClearActionListener(this));
        // Accesibilidad: Botón limpiar directorios
        clearButton.setMnemonic(MessageUtils.k('C'));
        clearButton.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_clear_directories_button"));
        clearButton.getAccessibleContext().setAccessibleDescription(MessageUtils.t("accessibility_clear_directories_description"));

        topPanel.add(clearButton);

        // Panel central dividido en izquierda y derecha
        //JPanel centerPanel = new JPanel(new GridLayout(1, 2)); //FIXME used a splitter instead, likely unused now

        // Panel izquierdo
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createTitledBorder(MessageUtils.t("directory_list_panel")));
        // Accesibilidad: Panel de lista de directorios
        leftPanel.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_directory_list_panel"));
        leftPanel.getAccessibleContext().setAccessibleDescription(MessageUtils.t("accessibility_directory_list_panel_description"));

        listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        leftPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(listContainer, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(listContainer);
        // Accesibilidad: Área de scroll de directorios
        scrollPane.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_directory_scroll_area"));
        scrollPane.getAccessibleContext().setAccessibleDescription(MessageUtils.t("accessibility_directory_scroll_description"));

        leftPanel.add(scrollPane);

        // Panel derecho
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createTitledBorder("Detalles"));
        // Accesibilidad: Panel de detalles
        rightPanel.getAccessibleContext().setAccessibleName(null);

        detailPanel = new JPanel();

        scrollPaneDetail = new JScrollPane(detailPanel);
        // Accesibilidad: Área de scroll de detalles
        scrollPaneDetail.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_details_scroll_area"));
        scrollPaneDetail.getAccessibleContext().setAccessibleDescription(MessageUtils.t("accessibility_details_scroll_description"));

        rightPanel.add(scrollPaneDetail, BorderLayout.CENTER);

        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.6); // 60% para el panel izquierdo
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true); // Botón de expansión opcional
        // Accesibilidad: Panel dividido
        splitPane.getAccessibleContext().setAccessibleName(null);

        // Añadir al panel principal
        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private void showDirectoryDetails(String directory) {
        detailPanel.removeAll();
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));

        signALlButton = new JButton(MessageUtils.t("directory_panel_sign"));

        signAllDocumentButton = new JButton(MessageUtils.t("directory_panel_sign_document"));
        signAsicButton = new JButton(MessageUtils.t("directory_panel_sign_asic"));
        saveAsButtonName = new JButton(MessageUtils.t("directory_panel_save_as_name"));
        saveAsButton = new JButton(MessageUtils.t("directory_panel_save_as"));

        signALlButton.addActionListener(new SignAllActionListener(this, directory, false));
        signAllDocumentButton.addActionListener(new SignAllActionListener(this, directory, true));
        saveAsButton.addActionListener(new SignAndSaveToActionListener(this, directory, false));
        saveAsButtonName.addActionListener(new SignAndSaveToActionListener(this, directory, true));
        signAsicButton.addActionListener(new signWithAsic(this, directory));

        // Accesibilidad: Botón firmar todos los archivos
        signALlButton.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_sign_all_files_button"));
        signALlButton.getAccessibleContext().setAccessibleDescription(MessageUtils.t("accessibility_sign_all_files_description"));
        signALlButton.setMnemonic(MessageUtils.k('F'));

        // Accesibilidad: Botón firmar todos los documentos
        signAllDocumentButton.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_sign_all_documents_button"));
        signAllDocumentButton.getAccessibleContext().setAccessibleDescription(MessageUtils.t("accessibility_sign_all_documents_description"));
        signAllDocumentButton.setMnemonic(MessageUtils.k('D'));

        // Accesibilidad: Botón firmar con ASIC
        signAsicButton.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_sign_asic_button"));
        signAsicButton.getAccessibleContext().setAccessibleDescription(MessageUtils.t("accessibility_sign_asic_description"));
        signAsicButton.setMnemonic(MessageUtils.k('A'));

        // Accesibilidad: Botón guardar como con nombre
        saveAsButtonName.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_save_as_name_button"));
        saveAsButtonName.getAccessibleContext().setAccessibleDescription(MessageUtils.t("accessibility_save_as_name_description"));
        saveAsButton.setMnemonic(MessageUtils.k('W'));

        // Accesibilidad: Botón guardar como
        saveAsButton.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_save_as_button"));
        saveAsButton.getAccessibleContext().setAccessibleDescription(MessageUtils.t("accessibility_save_as_description"));
        saveAsButton.setMnemonic(MessageUtils.k('E'));

        JButton[] buttons = {
            signALlButton,
            signAllDocumentButton,
            signAsicButton,
            saveAsButton,
            saveAsButtonName
        };

        int maxWidth = 0;
        int maxHeight = 0;
        for (JButton btn : buttons) {
            Dimension pref = btn.getPreferredSize();
            if (pref.width > maxWidth) maxWidth = pref.width;
            if (pref.height > maxHeight) maxHeight = pref.height;
        }

        for (JButton btn : buttons) {
            btn.setMaximumSize(new Dimension(maxWidth, maxHeight));
            btn.setMinimumSize(new Dimension(maxWidth, maxHeight));
            btn.setPreferredSize(new Dimension(maxWidth, maxHeight));
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            detailPanel.add(btn);
            detailPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }

        detailPanel.add(signALlButton);
        detailPanel.add(signAllDocumentButton);
        detailPanel.add(signAsicButton);
        detailPanel.add(saveAsButton);
        detailPanel.add(saveAsButtonName);

        JLabel directoryLabel = new JLabel(directory);
        directoryLabel.setFont(directoryLabel.getFont().deriveFont(Font.BOLD, 18f)); // Negrita y tamaño opcional
        directoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Accesibilidad: Etiqueta de directorio seleccionado
        directoryLabel.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_selected_directory_label"));
        directoryLabel.getAccessibleContext().setAccessibleDescription(MessageUtils.t("accessibility_selected_directory_label_description") + " " + directory);

        detailPanel.add(directoryLabel);

        File dir = new File(directory);
        File[] allFiles = dir.listFiles();

        int fileCount = 0;
        int dirCount = 0;

        if (allFiles != null) {
            for (File f : allFiles) {
                if (f.isDirectory()) {
                    dirCount++;
                } else {
                    fileCount++;
                }
            }
        }

        String pattern = MessageUtils.t("directory_info_label");
        String text = String.format(pattern, fileCount, dirCount);
        JLabel infoLabel = new JLabel(text);
        infoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Accesibilidad: Etiqueta de información del directorio
        infoLabel.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_directory_info_label"));
        infoLabel.getAccessibleContext().setAccessibleDescription(text);

        detailPanel.add(infoLabel);

        if (allFiles != null) {
            for (File f : allFiles) {
                if (f.isDirectory()) {
                    File[] contents = f.listFiles();
                    int subFileCount = 0;
                    if (contents != null) {
                        for (File sub : contents) {
                            if (sub.isFile()) subFileCount++;
                        }
                    }

                    JLabel subdirLabel = new JLabel("📁 " + f.getName() + " - " + subFileCount + " " + MessageUtils.t("directory_archives_label"));
                    subdirLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                    // Accesibilidad: Etiqueta de subdirectorio
                    subdirLabel.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_subdirectory_label"));
                    subdirLabel.getAccessibleContext().setAccessibleDescription(MessageUtils.t("accessibility_subdirectory_description") + " " + f.getName() + " " + MessageUtils.t("accessibility_contains") + " " + subFileCount + " " + MessageUtils.t("directory_archives_label"));

                    detailPanel.add(subdirLabel);
                }
            }
        }

        detailPanel.revalidate();
        detailPanel.repaint();
    }

    public void reloadView() {
        listContainer.removeAll();
        for (String directory : directories) {
            JPanel directoryPanel = getJPanel(directory);

            JLabel directoryLabel = new JLabel(getNameDirectory(directory));
            directoryLabel.setFont(directoryLabel.getFont().deriveFont(Font.BOLD, 18f)); // Negrita y tamaño opcional
            directoryLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
            directoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            // Accesibilidad: Etiqueta de directorio en la lista
            directoryLabel.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_directory_item_label") + " " + getNameDirectory(directory));


            directoryPanel.add(directoryLabel);

            directoryPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showDirectoryDetails(directory);
                    selectedDirectory = directory;
                    reloadView();
                    SwingUtilities.invokeLater(() -> scrollPaneDetail.requestFocusInWindow());
                }
            });

            directoryPanel.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER ||
                        e.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
                        showDirectoryDetails(directory);
                        selectedDirectory = directory;
                        reloadView();
                        SwingUtilities.invokeLater(() -> scrollPaneDetail.requestFocusInWindow());
                    } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN ||
                        e.getKeyCode() == java.awt.event.KeyEvent.VK_UP) {
                        Component[] components = listContainer.getComponents();
                        int currentIndex = -1;

                        for (int i = 0; i < components.length; i++) {
                            if (components[i] == directoryPanel) {
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
                            JScrollPane scrollPane = (JScrollPane) listContainer.getParent().getParent();
                            scrollPane.getViewport().scrollRectToVisible(components[newIndex].getBounds());
                        }
                    }
                }
            });

            listContainer.add(directoryPanel);

            JPanel deleteButtonWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            JButton deleteButton = new JButton("X");
            deleteButton.setFocusPainted(false);
            deleteButton.setContentAreaFilled(false);
            deleteButton.setBorderPainted(false);
            deleteButton.setOpaque(false);
            deleteButton.setHorizontalAlignment(SwingConstants.CENTER);
            deleteButton.setVerticalAlignment(SwingConstants.CENTER);
            // Accesibilidad: Botón eliminar directorio
            deleteButton.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_delete_directory_button"));

            deleteButtonWrapper.add(deleteButton);
            deleteButtonWrapper.setOpaque(false);
            directoryPanel.add(deleteButtonWrapper, BorderLayout.NORTH);

            deleteButton.addActionListener(new DeleteActionListener(this, directory));

        }
        listContainer.revalidate();
        listContainer.repaint();
    }

    @NotNull
    private JPanel getJPanel(String directory) {
        JPanel directoryPanel = new JPanel(new BorderLayout());

        if (directory.equals(selectedDirectory)) {
            directoryPanel.setBackground(getColor());
        }

        directoryPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(8, 10, 8, 10),
            BorderFactory.createLineBorder(Color.GRAY, 1)
        ));

        directoryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        directoryPanel.setMinimumSize(new Dimension(Integer.MAX_VALUE, 60));

        directoryPanel.setLayout(new BoxLayout(directoryPanel, BoxLayout.X_AXIS));
        directoryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        directoryPanel.setFocusable(true);

        directoryPanel.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                directoryPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(8, 10, 8, 10),
                    BorderFactory.createLineBorder(Color.BLUE, 2)
                ));
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                directoryPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(8, 10, 8, 10),
                    BorderFactory.createLineBorder(Color.GRAY, 1)
                ));
            }
        });

        return directoryPanel;
    }

    public void setGUI(GUIInterface gui) {
        this.gui = gui;
        this.swing = (GUISwing) gui;
    }

    private String getNameDirectory(String directory) {
        return new File(directory).getName();
    }

    public List<Document> getDocuments() {
        return documents;
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

    //Acciones de botones

    private class DeleteActionListener implements ActionListener {
        private DirectoryPanel panel;
        private String directory;

        public DeleteActionListener(DirectoryPanel panel, String directory) {
            this.panel = panel;
            this.directory = directory;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            panel.directories.remove(directory);
            panel.reloadView();
            detailPanel.removeAll();
            detailPanel.revalidate();
            detailPanel.repaint();
        }
    }

    private class ClearActionListener implements ActionListener {
        private DirectoryPanel panel;

        public ClearActionListener(DirectoryPanel panel) {
            this.panel = panel;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            panel.directories.clear();
            panel.reloadView();
            detailPanel.removeAll();
            detailPanel.revalidate();
            detailPanel.repaint();
            ((GUISwing) gui).showNotification(MessageUtils.t("directoty_empty_action_done"), GUISwing.NotificationType.SUCCESS);
        }
    }

    public void addFiles(File[] files) {
        boolean addedAny = false;
        for (File dir : files) {
            String path = dir.getAbsolutePath();
            if (directories.contains(path)) {
                continue;
            }
            directories.add(path);
            addedAny = true;
        }
        if (addedAny) {
            reloadView();
            ((GUISwing) gui).showNotification(MessageUtils.t("directory_add_directory_done"), GUISwing.NotificationType.SUCCESS);
        } else {
            ((GUISwing) gui).showNotification(MessageUtils.t("directory_add_directory_error"), GUISwing.NotificationType.WARNING);
        }
    }

    private class SelectDirectoryActionListener implements ActionListener {
        @SuppressWarnings("unused")
        private DirectoryPanel panel;

        public SelectDirectoryActionListener(DirectoryPanel directoryPanel) {
            this.panel = directoryPanel; //FIXME check if actually used
            }

        public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Seleccionar directorios");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setMultiSelectionEnabled(true);
            chooser.setAcceptAllFileFilterUsed(false);

            int result = chooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File[] selectedDirs = chooser.getSelectedFiles();
                if (selectedDirs.length > 0) {
                    gui.addDirectories(selectedDirs);
                }
            }
        }
    }

    private class SignAllActionListener implements ActionListener {
        private DirectoryPanel panel;
        private String directory;
        private boolean isDocumentName;

        public SignAllActionListener(DirectoryPanel panel, String directory, boolean isDocumentName) {
            this.directory = directory;
            this.panel = panel;
            this.isDocumentName = isDocumentName;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            File dir = new File(directory);
            String parent = dir.getParent();
            String name = dir.getName();
            File signedDir = new File(parent, name + "-firmado");
            if (!signedDir.exists() && !isDocumentName) {
                signedDir.mkdirs();
            }
            processDirectory(panel, directory, signedDir, false, isDocumentName);
        }
    }

    private class SignAndSaveToActionListener implements ActionListener {
        private DirectoryPanel panel;
        private String directory;
        private boolean isDocumentName;

        public SignAndSaveToActionListener(DirectoryPanel panel, String directory, boolean isDocumentName) {
            this.panel = panel;
            this.directory = directory;
            this.isDocumentName = isDocumentName;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Seleccionar carpeta de destino para los documentos firmados");
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int result = fc.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedDir = fc.getSelectedFile();
                processDirectory(panel, directory, selectedDir, false, isDocumentName);
            }
        }
    }

    private class signWithAsic implements ActionListener {
        private DirectoryPanel panel;
        private String directory;

        public signWithAsic(DirectoryPanel panel, String directory) {
            this.panel = panel;
            this.directory = directory;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            File dir = new File(directory);
            String parent = dir.getParent();
            String name = dir.getName();
            File signedDir = new File(parent, name + "-firmado");
            if (!signedDir.exists()) {
                signedDir.mkdirs();
            }
            processDirectory(panel, directory, signedDir, true, false);
        }

    }

    private void processDirectory(DirectoryPanel panel, String directory, File destinationDir, boolean isAsic, boolean isDocumentName) {
        try {
            documents.clear();
            File dir = new File(directory);
            if (!dir.exists() || !dir.isDirectory()) {
                String errorMsg = "El directorio no existe o no es válido: " + directory;
                LOG.error(errorMsg);
                SwingUtilities.invokeLater(() -> panel.gui.showMessage(errorMsg));
                return;
            }

            Path basePath = dir.toPath();

            List<File> fileList = Files.walk(basePath)
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .collect(Collectors.toList());

            if (fileList.isEmpty()) {
                String msg = "No se encontraron documentos en el directorio.";
                LOG.info(msg);
                SwingUtilities.invokeLater(() -> panel.gui.showMessage(msg));
                return;
            }

            processFiles(fileList, panel, basePath, destinationDir, isDocumentName);
            goToSign(panel, isAsic, dir);

        } catch (IOException ex) {
            LOG.error("Error al recorrer el directorio: " + ex.getMessage(), ex);
            SwingUtilities.invokeLater(() -> panel.gui.showMessage("Error al procesar el directorio."));
        }
    }

    //En este metodo proceso los archivos para asignarles su ruta de guardado para poder generar la estructura de carpetas dentro del directorio destino
    private void processFiles(List<File> fileList, DirectoryPanel panel, Path basePath, File destinationDir, boolean isDocumentName) {
        for (File file : fileList) {
            String pathname = file.getAbsolutePath();
            int lastSlash = Math.max(pathname.lastIndexOf("/"), pathname.lastIndexOf("\\"));
            int dotIndex = pathname.lastIndexOf(".");
            if (dotIndex <= lastSlash) {
                String errorMsg = MessageUtils.t("guiswing_dialog_document_not_valid_extension") + pathname + " " + MessageUtils.t("guiswing_dialog_document_not_valid_extension2");
                LOG.error(errorMsg);
                SwingUtilities.invokeLater(() -> panel.gui.showMessage(errorMsg));
            } else {
                Document document = new Document(gui, file.getAbsolutePath());
                Path relativePath = basePath.relativize(file.toPath());
                String name = document.getName();
                String signedFilename = "";
                if (isDocumentName) {
                    signedFilename = name.substring(0, name.lastIndexOf(".")) + "-Firmado" + document.getExtension();
                } else {
                    signedFilename = name.substring(0, name.lastIndexOf(".")) + document.getExtension();

                }
                File baseDir;
                if (isDocumentName) {
                    baseDir = file.getParentFile();
                } else {
                    baseDir = destinationDir;
                }
                File signedFile = new File(baseDir,
                    (relativePath.getParent() != null && !isDocumentName ? relativePath.getParent().toString() + File.separator : "")
                        + signedFilename);
                signedFile.getParentFile().mkdirs();

                String baseName = signedFilename.substring(0, signedFilename.lastIndexOf('.'));

                String extension = signedFilename.substring(signedFilename.lastIndexOf('.'));
                File adjustedSignedFile = signedFile;
                document.setAbsolutePathToSave(adjustedSignedFile.getAbsolutePath()); // Guardamos la ruta original para verificar si se repite despues
                int index = 0;
                File finalAdjustedSignedFile = adjustedSignedFile;
                // Aqui verifico si la ruta de guardado ya existe para otro documento
                for (Document actualDocument : documents) {
                    if (actualDocument == document) break;
                    if (actualDocument.getAbsolutePathToSave().equalsIgnoreCase(finalAdjustedSignedFile.getAbsolutePath())) {
                        index++;
                    }
                }
                if (index != 0) {
                    String newFilename = baseName + "(" + index + ")" + extension;
                    adjustedSignedFile = new File(destinationDir,
                        (relativePath.getParent() != null ? relativePath.getParent().toString() + File.separator : "")
                            + newFilename);
                }
                document.setPathToSave(adjustedSignedFile.getAbsolutePath());
                document.registerListener(panel.swing.getDocmanager());
                documents.add(document);
            }
        }
    }

    private void goToSign(DirectoryPanel panel, boolean isAsic, File directory) throws IOException {
        if (isAsic) {
            Document firstDocument = documents.get(0);
            documents.remove(0);
            firstDocument.setPathToSave(directory.getParent() + "/" + directory.getName() + ".ascie");
            List<DSSDocument> detacheddocs = new ArrayList<>();
            List<Document> copyList = new ArrayList<>(documents);
            Path basePath = directory.toPath();
            for (Document doc : copyList) {
                Path fullPath = new File(doc.getPathName()).toPath();
                Path relativePath = basePath.relativize(fullPath);
                byte[] content = Files.readAllBytes(fullPath);

                DSSDocument dssdoc = new InMemoryDocument(content, relativePath.toString());
                detacheddocs.add(dssdoc);
                documents.remove(doc);
            }
            documents.add(firstDocument);
            firstDocument.setSigner(new FirmadorASiC(panel.gui));
            firstDocument.getSigner().setDetached(detacheddocs);
        }
        panel.swing.signDirectory();
    }
}
