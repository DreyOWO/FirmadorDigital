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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import javax.accessibility.AccessibleContext;
import javax.swing.*;

import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.documents.Document;
import cr.libre.firmador.gui.GUIInterface;
import cr.libre.firmador.gui.GUISwing;

public class DocumentSelectionGroupLayout extends GroupLayout {
    private JLabel fileLabel;
    public JTextField fileField;
    public JButton fileButton;
    private static FileDialog loadDialog;
    private SwingMainWindowFrame frame;
    public GUIInterface gui;
    private String lastDirectory = null;
    private String lastFile = null;

    private JPanel notificationBar;
    private JLabel notificationLabel;
    private Timer notificationTimer;

    public void setGUI(GUIInterface gui) {
        this.gui = gui;
    }

    @SuppressWarnings("this-escape")
    public DocumentSelectionGroupLayout(Container host, JTabbedPane frameTabbedPane, SwingMainWindowFrame frame) {
        super(host);
        this.frame = frame;
        fileLabel = new JLabel(MessageUtils.t("document_selection_label"));
        fileField = new JTextField(MessageUtils.t("document_selection_filefield"));
        fileField.setToolTipText(MessageUtils.t("document_selection_filefield_tooltip"));
        fileField.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("document_selection_filefield_tooltip_accessible"));

        fileField.setEditable(false);
        fileButton = new JButton(MessageUtils.t("document_selection_btn"));
        fileButton.setToolTipText(MessageUtils.t("document_selection_btn_tooltip"));
        fileButton.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("document_selection_btn_tooltip_accessible"));
        fileButton.setMnemonic('N');
        fileButton.setOpaque(false);

        notificationBar = new JPanel();
        notificationBar.setLayout(new BorderLayout());
        notificationBar.setVisible(false);
        notificationBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        notificationLabel = new JLabel();
        notificationLabel.setHorizontalAlignment(SwingConstants.CENTER);
        notificationLabel.setFont(notificationLabel.getFont().deriveFont(Font.BOLD, 14f));
        notificationLabel.getAccessibleContext().setAccessibleName("Notificación");
        notificationLabel.getAccessibleContext().setAccessibleDescription("Mensaje de notificación del sistema");

        notificationBar.add(notificationLabel, BorderLayout.CENTER);

        this.setAutoCreateGaps(true);
        this.setAutoCreateContainerGaps(true);
        this.setHorizontalGroup(this.createParallelGroup()
            .addGroup(this.createSequentialGroup()
                .addComponent(fileLabel)
                .addComponent(fileField)
                .addComponent(fileButton))
            .addComponent(frameTabbedPane)
            .addComponent(notificationBar)); // Agregar notificationBar horizontalmente

        this.setVerticalGroup(this.createSequentialGroup()
            .addGroup(this.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(fileLabel)
                .addComponent(fileField)
                .addComponent(fileButton))
            .addComponent(frameTabbedPane)
            .addComponent(notificationBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)); // Agregar notificationBar al final verticalmente

    }

    public void initializeActions() {
        fileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                showLoadDialog();
                frame.pack();
                frame.setMinimumSize(frame.getSize());
            }
        });

    }

    private void showLoadDialog() {
        loadDialog = new FileDialog(frame, MessageUtils.t("document_selection_filedialog_title"));
        loadDialog.setMultipleMode(true);
        loadDialog.setLocationRelativeTo(null);
        loadDialog.setVisible(true);
        loadDialog.dispose();
        File[] files = loadDialog.getFiles();
		if (files.length >= 1) {
			gui.addDocuments(files);
			lastDirectory = loadDialog.getDirectory();
        }
    }

    public String getLastDirectory() {
        return lastDirectory;
    }

    public String getLastFile() {
        return lastFile;
    }

    public void setLastFile(Document document) {
        if (!document.isVirtual()){
            this.lastFile = document.getPathName();
            Path path= FileSystems.getDefault().getPath(this.lastFile);
            this.lastDirectory = path.getParent().toString();
            if(document.getIsremote()) {
                fileField.setText(document.getName());
                fileField.getAccessibleContext().setAccessibleDescription(String.format(
                    MessageUtils.t("document_selection_filefield_load_tooltip_accessible"), document.getName()));
            } else {
                fileField.setText(path.getFileName().toString());
                fileField.getAccessibleContext().setAccessibleDescription(
                    String.format(MessageUtils.t("document_selection_filefield_load_tooltip_accessible"),
                        path.getFileName().toString()));
            }
        }
        fileField.requestFocus(true);
        fileField.requestFocus();
    }

    public FileDialog getLoadDialog() {
        return loadDialog;
    }

    public void clean() {
        fileField.setText("");
    }

    public void showNotification(String message, GUISwing.NotificationType type) {
        SwingUtilities.invokeLater(() -> {
            String typeText = "";
            switch (type) {
                case SUCCESS:
                    notificationBar.setBackground(new Color(212, 237, 218));
                    notificationLabel.setForeground(new Color(21, 87, 36));
                    break;
                case ERROR:
                    notificationBar.setBackground(new Color(248, 215, 218));
                    notificationLabel.setForeground(new Color(114, 28, 36));
                    break;
                case WARNING:
                    notificationBar.setBackground(new Color(255, 243, 205));
                    notificationLabel.setForeground(new Color(133, 100, 4));
                    break;
                case INFO:
                    notificationBar.setBackground(new Color(217, 237, 247));
                    notificationLabel.setForeground(new Color(12, 84, 96));
                    break;
            }

            notificationLabel.setText(message);
            notificationBar.setVisible(true);
            notificationBar.setOpaque(true);

            notificationLabel.requestFocusInWindow();

            AccessibleContext ac = notificationLabel.getAccessibleContext();
            String fullMessage = typeText + ": " + message;
            ac.setAccessibleName(fullMessage);
            ac.setAccessibleDescription(fullMessage);

            notificationLabel.requestFocusInWindow();

            ac.firePropertyChange(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, null, fullMessage);
            ac.firePropertyChange(AccessibleContext.ACCESSIBLE_TEXT_PROPERTY, null, message);
            ac.firePropertyChange(AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY, null, fullMessage);

            if (notificationTimer != null) {
                notificationTimer.stop();
            }

            notificationTimer = new Timer(5000, e -> {
                notificationBar.setVisible(false);
                ((Timer)e.getSource()).stop();
            });
            notificationTimer.setRepeats(false);
            notificationTimer.start();
        });
    }
}
