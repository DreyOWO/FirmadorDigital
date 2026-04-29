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

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import cr.libre.firmador.ConfigListener;
import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.Settings;
import cr.libre.firmador.SettingsManager;
import cr.libre.firmador.connections.Connection;
import cr.libre.firmador.connections.ServicesUrlsIO;
import cr.libre.firmador.connections.StartConnection;
import cr.libre.firmador.gui.GUIInterface;
import cr.libre.firmador.gui.GUISwing;
import cr.libre.firmador.services.ConnectionPanelService;
import cr.libre.firmador.validators.GeneralValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("serial")
public class ConnectionPanel extends JPanel implements ConfigListener {
    final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private JButton buttonAdd;
    private FileDialog loadDialog;
    private JPanel rightPanel;
    private JPanel leftPanel;
    private JPanel listPanel;
    private JPanel infoPanel;
    private GUIInterface gui;
    private List<Connection> listConnections = new ArrayList<>();
    private StartConnection startConnection;
    private SettingsManager manager;
    private Settings settings;
    private boolean isRemote;
    private int selectedConnection = 0;
    private SwingMainWindowFrame frame;
    private ConnectionPanelService service;

    @SuppressWarnings("this-escape")
    public ConnectionPanel(GUIInterface gui, boolean isRemote, SwingMainWindowFrame frame) {
        this.gui = gui;
        this.isRemote = isRemote;
        this.frame = frame;
        manager = SettingsManager.getInstance();
        loadConnectionPanel();
    }

    public void setListConnections(List<Connection> listConnections) {
        this.listConnections = listConnections;
    }

    public void displayAndStartConnection() {
        if (!listConnections.get(0).isRunning()) {
            startConnection();
        }
        showConnectionDetails(listConnections.get(0), false);
    }

    public void loadConnectionPanel() {

        try {
            List<Connection> connections = ServicesUrlsIO.load();
            if (connections == null || connections.isEmpty() ||
                    connections.stream().noneMatch(conn -> conn.getService().contains("Firmador Remoto")) ||
                    connections.stream().noneMatch(conn -> conn.getService().contains("Gaudi"))) {

                startConnection = new StartConnection(gui);
                List<Connection> defaultConnections = startConnection.startConnection();

                if (connections == null) {
                    connections = new ArrayList<>();
                }

                // Agregar solo conexiones que no existen
                if (defaultConnections != null) {
                    for (Connection defaultConn : defaultConnections) {
                        String service = defaultConn.getService();
                        if (connections.stream().noneMatch(conn -> conn.getService().equals(service))) {
                            connections.add(defaultConn);
                        }
                    }
                }
            }

            if (listConnections == null) {
                listConnections = new ArrayList<>();
            }
            if (connections != null) {
                listConnections.addAll(connections);
            }
        } catch (Exception e) {
            LOG.error("Error al cargar servicios de firma digital de la UCR", e);
        }
        for (Connection connection : listConnections) {
            if (connection.getStartOn()) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        connection.start(gui, connection.getName());
                        return null;
                    }
                };

                worker.execute();

                try {
                    worker.get(); // Bloquea hasta que termine
                    Thread.sleep(2000); // Delay adicional por seguridad
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Error iniciando conexión: " + connection.getName(), e);
                }
            }
        }
        settings = manager.getAndCreateSettings();
        settings.addListener(this);
        service = new ConnectionPanelService(this, settings);
        //JPanel principalPanel = new JPanel(new GridLayout(1, 2)); //FIXME remove if unused
        setLayout(new BorderLayout());

        // Panel Izquierdo
        leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createTitledBorder(MessageUtils.t("connection_panel_connections")));
        leftPanel.getAccessibleContext().setAccessibleName(MessageUtils.t("connection_panel_left_panel_accessible"));

        buttonAdd = new JButton(MessageUtils.t("connection_panel_add"));
        buttonAdd.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonAdd.getAccessibleContext().setAccessibleName(MessageUtils.t("connection_panel_add_button_accessible"));
        buttonAdd.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("connection_panel_add_button_accessible_description"));
        leftPanel.add(buttonAdd, BorderLayout.NORTH);

        buttonAdd.addActionListener(service.getAddButtonAction());

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        leftPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(listPanel, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.getAccessibleContext().setAccessibleName(MessageUtils.t("connection_panel_list_scroll_accessible"));
        leftPanel.add(scrollPane);

        // Panel Derecho
        rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createTitledBorder(MessageUtils.t("connection_panel_info")));
        rightPanel.getAccessibleContext().setAccessibleName(MessageUtils.t("connection_panel_right_panel_accessible"));

        infoPanel = new JPanel();
        infoPanel.getAccessibleContext().setAccessibleName(MessageUtils.t("connection_panel_info_panel_accessible"));
        JScrollPane scrollPaneInfo = new JScrollPane(infoPanel);
        scrollPaneInfo.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("connection_panel_info_scroll_accessible"));
        rightPanel.add(scrollPaneInfo);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.getAccessibleContext().setAccessibleName(null);
        splitPane.setResizeWeight(0.3);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);

        add(splitPane, BorderLayout.CENTER);
        if (isRemote) {
            int port = settings.getRemotePort();
            String origin = settings.getOrigin();
            List<String> allowedOrigins = settings.getAllowedHosts();
            boolean isAllowed = false;
            for (String allowedOrigin : allowedOrigins) {
                if (origin.contains(allowedOrigin)) {
                    isAllowed = true;
                    break;
                }
            }
            if (!isAllowed) {
                settings.addTempAllowedHost(origin);
            }
            if (port != settings.portNumber) {
                settings.portNumber = port;
                manager.setSettings(settings, true);
            }
            startConnection();
        }
        reloadView();
    }

    private void startConnection() {
        new SwingWorker<Void, Void>() {
            Connection connection = listConnections.get(0);

            @Override
            protected Void doInBackground() {
                connection.setErrorList(new ArrayList<>());
                connection.start(gui, connection.getService());
                return null;
            }
        }.execute();
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

    public void reloadView() {
        listPanel.removeAll();
        for (Connection connection : listConnections) {
            JPanel connectionPanel = new JPanel(new BorderLayout());

            if (connectionPanel.toString().equals(connection.getName())) { //FIXME why is this comparing a JPanel with a String?
                connectionPanel.setBackground(getColor());
            }

            connectionPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(8, 10, 8, 10),
                    BorderFactory.createLineBorder(Color.GRAY, 1)));

            connectionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            connectionPanel.setMinimumSize(new Dimension(Integer.MAX_VALUE, 60));

            connectionPanel.setLayout(new BoxLayout(connectionPanel, BoxLayout.X_AXIS));
            connectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            connectionPanel.setFocusable(true);

            connectionPanel.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    connectionPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createEmptyBorder(8, 10, 8, 10),
                            BorderFactory.createLineBorder(Color.BLUE, 2)));
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    connectionPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createEmptyBorder(8, 10, 8, 10),
                            BorderFactory.createLineBorder(Color.GRAY, 1)));
                }
            });

            JLabel nameLabel = new JLabel(connection.getName());
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 18f));
            nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
            nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            nameLabel.getAccessibleContext().setAccessibleName(
                    MessageUtils.t("connection_panel_name_label_accessible") + " " + connection.getName());
            connectionPanel.add(nameLabel);

            connectionPanel.add(Box.createHorizontalGlue());

            if (!connection.getService().contains("Firmador Remoto") && !connection.getService().contains("Gaudi")) {
                JButton closeButton = new JButton("X");
                closeButton.setMargin(new Insets(2, 8, 2, 8));
                closeButton.setFocusPainted(false);
                closeButton.setBorderPainted(false);
                closeButton.setContentAreaFilled(false);
                closeButton.setOpaque(false);
                closeButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
                closeButton.setFont(closeButton.getFont().deriveFont(Font.BOLD, 14f));
                closeButton.addActionListener(service.getCloseButtonAction(connection));
                connectionPanel.add(closeButton);

            }
            connectionPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showConnectionDetails(connection, false);
                    reloadView();
                    SwingUtilities.invokeLater(() -> infoPanel.requestFocusInWindow());
                }
            });

            connectionPanel.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER ||
                            e.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
                        showConnectionDetails(connection, false);
                        reloadView();
                        SwingUtilities.invokeLater(() -> infoPanel.requestFocusInWindow());
                    } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN ||
                            e.getKeyCode() == java.awt.event.KeyEvent.VK_UP) {
                        Component[] components = listPanel.getComponents();
                        int currentIndex = -1;

                        for (int i = 0; i < components.length; i++) {
                            if (components[i] == connectionPanel) {
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
                            JScrollPane scrollPane = (JScrollPane) listPanel.getParent().getParent();
                            scrollPane.getViewport().scrollRectToVisible(components[newIndex].getBounds());
                        }
                    }
                }
            });

            listPanel.add(connectionPanel);
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    public void showConnectionDetails() {
        showConnectionDetails(listConnections.get(0), false);
    }

    public void refreshConnectionDetails(Connection connection) {
        showConnectionDetails(connection, false);
    }

    public void showConnectionDetails(Connection connection, boolean isRestart) {
        selectedConnection = listConnections.indexOf(connection);
        infoPanel.removeAll();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        if (connection == null) {
            // No mostramos nada si la conexión es null
            infoPanel.revalidate();
            infoPanel.repaint();
            return;
        }

        // Nombre
        JLabel name = new JLabel(connection.getName());
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoPanel.add(name);

        // Usuario loggeado
        if (connection.getUserLogged() != null && !connection.getUserLogged().isEmpty()) {
            JPanel userPanel = new JPanel();
            userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.Y_AXIS));
            userPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel userLabel = new JLabel(MessageUtils.t("connection_panel_logged_user"));
            userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            userLabel.setFont(userLabel.getFont().deriveFont(Font.PLAIN, 12f));

            JLabel userName = new JLabel(connection.getUserLogged());
            userName.setAlignmentX(Component.CENTER_ALIGNMENT);
            userName.setFont(userName.getFont().deriveFont(Font.BOLD, 14f));
            userName.setForeground(new Color(100, 100, 90));

            userPanel.add(userLabel);
            userPanel.add(userName);
            infoPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            infoPanel.add(userPanel);
            infoPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        // Iniciar al arrancar
        if (!connection.getName().equals("Firmador Remoto")) {
            JPanel startOnPanel = new JPanel();
            startOnPanel.setLayout(new BoxLayout(startOnPanel, BoxLayout.Y_AXIS));
            JLabel startOnLabel = new JLabel(MessageUtils.t("connection_panel_start_on"));
            startOnLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            JCheckBox startOnCheckBox = new JCheckBox();
            startOnCheckBox.setAlignmentX(Component.CENTER_ALIGNMENT);
            startOnCheckBox.setSelected(connection.getStartOn());
            startOnCheckBox.addActionListener(e -> {
                connection.setStartOn(!connection.getStartOn());
                try {
                    ServicesUrlsIO.save(this.listConnections);
                } catch (Exception ex) {
                    LOG.error("Error al guardar la configuración de la conexión", ex);
                }
                showConnectionDetails(connection, false);
            });
            startOnPanel.add(startOnLabel);
            startOnPanel.add(startOnCheckBox);
            infoPanel.add(startOnPanel);
        }

        // Puerto
        if (connection.getPort() != 0) {
            JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            JLabel label = new JLabel(MessageUtils.t("connection_panel_port"));
            JTextField portField = new JTextField(settings.portNumber.toString(), 6);
            portField.getAccessibleContext()
                    .setAccessibleDescription(MessageUtils.t("connection_panel_port_description"));
            JButton portButton = new JButton(MessageUtils.t("connection_panel_edit"));
            portButton.getAccessibleContext()
                    .setAccessibleDescription(MessageUtils.t("connection_panel_edit_description"));
            portButton.addActionListener(service.getPortButtonAction(connection, portField, isRestart));
            portPanel.add(label);
            portPanel.add(portField);
            portPanel.add(portButton);
            portPanel.setMaximumSize(portPanel.getPreferredSize());
            portPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            infoPanel.add(portPanel);
        }
        // Estado
        JLabel state = new JLabel();
        if (isRestart) {
            state = new JLabel(MessageUtils.t("connection_panel_restart"));
        } else {
            if (connection.isExternal() && !connection.isLogged()) {
                state = new JLabel(connection.isRunning() ? MessageUtils.t("connection_panel_connect_external")
                        : MessageUtils.t("connection_panel_disconnected"));
            } else {
                state = new JLabel(connection.isRunning() ? MessageUtils.t("connection_panel_connected")
                        : MessageUtils.t("connection_panel_disconnected"));
            }
        }
        state.setAlignmentX(Component.CENTER_ALIGNMENT);
        if (connection.isRunning()) {
            state.setForeground(Color.GREEN);
        }
        infoPanel.add(state);
        // Boton de conexión
        JButton connectButton = new JButton(connection.isRunning() ? MessageUtils.t("connection_panel_disconnect")
                : MessageUtils.t("connection_panel_connect"));
        connectButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        connectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        connectButton.addActionListener(service.getConnectButtonAction(connection, connectButton));
        connectButton.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("connection_panel_connection_button_des"));
        infoPanel.add(connectButton);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        if (connection.isExternal() && connection.isLogged()) {
            JButton requestDocsButton = new JButton(MessageUtils.t("connection_panel_get_documents_title"));
            requestDocsButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            requestDocsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            requestDocsButton.setToolTipText(MessageUtils.t("coonection_panel_get_documents"));
            requestDocsButton.addActionListener(service.getRequestDocsButtonAction(connection, requestDocsButton));
            infoPanel.add(requestDocsButton);
        }

        if (connection.getPort() != 0) {
            JPanel domainsPanel = new JPanel();
            domainsPanel.setLayout(new BoxLayout(domainsPanel, BoxLayout.Y_AXIS));
            domainsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            domainsPanel.setMaximumSize(new Dimension(350, Integer.MAX_VALUE));
            JLabel domainsLabel = new JLabel(MessageUtils.t("connection_panel_authorized_domains"));
            domainsLabel.setFont(domainsLabel.getFont().deriveFont(Font.BOLD, 18f));
            domainsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            domainsPanel.add(domainsLabel);

            List<String> domains = settings.getAllowedHosts();

            for (String host : domains) {
                JPanel domainPanel = new JPanel(new GridBagLayout());
                domainPanel.setMaximumSize(new Dimension(300, 30));
                JLabel domainText = new JLabel(host);
                domainText.setFont(domainText.getFont().deriveFont(Font.BOLD, 16f));
                JButton deleteButton = new JButton(MessageUtils.t("connection_panel_delete"));
                deleteButton.setMaximumSize(deleteButton.getPreferredSize());
                deleteButton.addActionListener(service.getDeleteButtonAction(connection, host));
                deleteButton.getAccessibleContext()
                        .setAccessibleDescription(MessageUtils.t("connection_panel_delete_button_des"));
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.weightx = 1;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                domainPanel.add(domainText, gbc);
                gbc.gridx = 1;
                gbc.weightx = 0;
                gbc.insets = new Insets(0, 10, 0, 0);
                gbc.anchor = GridBagConstraints.EAST;
                gbc.fill = GridBagConstraints.NONE;
                domainPanel.add(deleteButton, gbc);
                domainsPanel.add(domainPanel);
            }
            infoPanel.add(domainsPanel);

            JPanel noAuthorizedDomainsPanel = new JPanel();
            noAuthorizedDomainsPanel.setLayout(new BoxLayout(noAuthorizedDomainsPanel, BoxLayout.Y_AXIS));
            noAuthorizedDomainsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            noAuthorizedDomainsPanel.setMaximumSize(new Dimension(350, Integer.MAX_VALUE));

            JLabel noAuthorizedDomainsLabel = new JLabel(MessageUtils.t("connection_panel_no_authorized_domains"));
            noAuthorizedDomainsLabel.setFont(noAuthorizedDomainsLabel.getFont().deriveFont(Font.BOLD, 18f));
            noAuthorizedDomainsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            noAuthorizedDomainsPanel.add(noAuthorizedDomainsLabel);

            List<String> noAuthorizedDomains = settings.getNoAuthorizedHosts();

            if (!noAuthorizedDomains.isEmpty()) {
                for (String domain : noAuthorizedDomains) {
                    JPanel domainPanel = new JPanel(new GridBagLayout());
                    domainPanel.setMaximumSize(new Dimension(300, 30));
                    JLabel domainText = new JLabel(domain);
                    domainText.setFont(domainText.getFont().deriveFont(Font.BOLD, 16f));
                    JButton authorizeButton = new JButton(MessageUtils.t("connection_panel_authorize"));
                    authorizeButton.getAccessibleContext()
                            .setAccessibleDescription(MessageUtils.t("connection_panel_authorize_description"));
                    authorizeButton.setMaximumSize(authorizeButton.getPreferredSize());
                    authorizeButton.addActionListener(service.getAuthorizeButtonAction(connection, domain));
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.gridx = 0;
                    gbc.gridy = 0;
                    gbc.weightx = 1;
                    gbc.anchor = GridBagConstraints.WEST;
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    domainPanel.add(domainText, gbc);
                    gbc.gridx = 1;
                    gbc.weightx = 0;
                    gbc.insets = new Insets(0, 10, 0, 0);
                    gbc.anchor = GridBagConstraints.EAST;
                    gbc.fill = GridBagConstraints.NONE;
                    domainPanel.add(authorizeButton, gbc);
                    noAuthorizedDomainsPanel.add(domainPanel);
                }
                infoPanel.add(noAuthorizedDomainsPanel);
            }
        }
        // Errores
        if (!connection.getErrorList().isEmpty()) {
            JPanel errorPanel = new JPanel();
            errorPanel.setLayout(new BoxLayout(errorPanel, BoxLayout.Y_AXIS));
            errorPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            errorPanel.getAccessibleContext()
                    .setAccessibleDescription(MessageUtils.t("connection_panel_error_description"));

            JButton clearButton = new JButton(MessageUtils.t("connection_panel_clear"));
            clearButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            clearButton.addActionListener(service.getClearButtonAction(connection));
            clearButton.getAccessibleContext()
                    .setAccessibleDescription(MessageUtils.t("connection_panel_clear_description"));
            errorPanel.add(clearButton);

            JLabel errorLabel = new JLabel(MessageUtils.t("connection_panel_errors"));
            errorLabel.setFont(errorLabel.getFont().deriveFont(Font.BOLD, 18f));
            errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            errorPanel.add(errorLabel);

            JPanel errorsContainer = new JPanel();
            errorsContainer.setLayout(new BoxLayout(errorsContainer, BoxLayout.Y_AXIS));
            errorsContainer.setAlignmentX(Component.CENTER_ALIGNMENT);

            for (String error : connection.getErrorList()) {
                JLabel errorLabelHtml = new JLabel("<html><b style='color:red;'>" + error + "</b></html>");
                errorLabelHtml.setFont(errorLabelHtml.getFont().deriveFont(14f));
                errorLabelHtml.setAlignmentX(Component.CENTER_ALIGNMENT);
                errorLabelHtml.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                errorLabelHtml.setPreferredSize(new Dimension(300, 32));
                errorsContainer.add(errorLabelHtml);
            }

            JScrollPane scrollPane = new JScrollPane(errorsContainer,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            scrollPane.setMinimumSize(new Dimension(300, 80));
            scrollPane.setPreferredSize(new Dimension(350, 120));
            scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

            errorPanel.add(scrollPane);

            infoPanel.add(errorPanel);
        }

        infoPanel.revalidate();
        infoPanel.repaint();
    }

    public int showAlertDeleteHost(String host) {
        return JOptionPane.showConfirmDialog(
                infoPanel, // Componente padre, puede ser null o tu panel actual
                "¿Está seguro que desea eliminar el host autorizado:\n\"" + host + "\"?",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
    }

    public void restartConnection(Connection connection) {
        //int idx = listConnections.indexOf(connection);
        LOG.info("restartConnection");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                connection.stop(gui);
                for (int i = 0; i < 10; i++) {
                    SwingUtilities.invokeLater(() -> showConnectionDetails(connection, true));
                    connection.start(gui, connection.getService());
                    if (connection.isRunning()) {
                        break;
                    }
                    try {
                        Thread.sleep(3000);
                        connection.setErrorList(new ArrayList<>());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                SwingUtilities.invokeLater(() -> connection.setErrorList(new ArrayList<>()));
                SwingUtilities.invokeLater(() -> showConnectionDetails(connection, false));
                return null;
            }
        }.execute();
    }

    public void updateErrors(List<String> errors, Connection connection) {
        LOG.info(MessageUtils.t("connection_panel_errors_found") + ": " + errors.toString());
        for (String error : errors) {
            connection.addError(error);
        }
        showConnectionDetails(connection, false);
    }

    public void saveSettings() {
        manager.setSettings(settings, true);
    }

    @Override
    public void updateConfig() {
        LOG.info("Updating configuration in ConnectionPanel");
        if (listConnections != null) {
            Connection connection = listConnections.get(selectedConnection);
            showConnectionDetails(connection, false);
        } else {
            LOG.info("Connections are null");
        }
    }

    public void showLoadDialog() {
        loadDialog = new FileDialog(frame, MessageUtils.t("document_selection_filedialog_title"));
        loadDialog.getAccessibleContext().setAccessibleName(MessageUtils.t("connection_panel_load_dialog_accessible"));
        loadDialog.setMultipleMode(true);

        loadDialog.setFilenameFilter((dir, name) -> name.toLowerCase().endsWith(".firmadorconn"));

        loadDialog.setLocationRelativeTo(null);
        loadDialog.setVisible(true);
        loadDialog.dispose();

        File[] files = loadDialog.getFiles();
        if (files.length >= 1) {
            for (File file : files) {
                try {
                    String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                    String json = content.split("-----BEGIN JSON-----")[1]
                            .split("-----END JSON-----")[0].trim();
                    String xml = content.split("-----BEGIN SIGNED-XML-----")[1]
                            .split("-----END SIGNED-XML-----")[0].trim();

                    Connection connection = new Connection(json);
                    boolean found = false;
                    for (Connection c : listConnections) {
                        if (c.getName().equals(connection.getService())
                                || c.getService().equals(connection.getService())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        if (AddConnection(xml, connection)) {
                            ServicesUrlsIO.save(listConnections);
                            ((GUISwing) gui).showNotification(MessageUtils.t("connection_panel_add_connection_done"),
                                    GUISwing.NotificationType.SUCCESS);
                        }
                    } else {
                        ((GUISwing) gui).showNotification(MessageUtils.t("connection_panel_add_existing"),
                                GUISwing.NotificationType.WARNING);
                    }
                    reloadView();
                } catch (Exception ex) {
                    ((GUISwing) gui).showNotification(MessageUtils.t("connection_panel_error_add_connection"),
                            GUISwing.NotificationType.ERROR);
                    LOG.info("Sucedió un error leyendo el archivo");
                }
            }
        }
    }

    private boolean AddConnection(String xml, Connection connection)
            throws IOException, ParserConfigurationException, SAXException {
        Path tempFile = Files.createTempFile("signed-", ".xml");
        Files.write(tempFile, xml.getBytes(StandardCharsets.UTF_8));

        GeneralValidator validator = new GeneralValidator();
        validator.loadDocumentPath(tempFile.toString());
        Reports reports = validator.getReports();
        String simpleReport = reports.getXmlSimpleReport();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(simpleReport.getBytes(StandardCharsets.UTF_8)));

        NodeList qNames = doc.getElementsByTagNameNS("*", "QualifiedName");
        String qualifiedName = (qNames.getLength() > 0) ? qNames.item(0).getTextContent() : "Desconocido";

        NodeList indications = doc.getElementsByTagNameNS("*", "Indication");
        String indication = (indications.getLength() > 0) ? indications.item(0).getTextContent() : "UNKNOWN";
        Files.deleteIfExists(tempFile);
        if ("TOTAL_PASSED".equalsIgnoreCase(indication) || "PASSED".equalsIgnoreCase(indication)) {
            int option = JOptionPane.showOptionDialog(
                    frame,
                    qualifiedName + MessageUtils.t("connection_panel_register_new_connection") + connection.getName()
                            + "\n" + MessageUtils.t("connection_panel_accept_new_connection"),
                    MessageUtils.t("connection_panel_register_new_connection_title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    new Object[] { MessageUtils.t("connection_panel_accept"),
                            MessageUtils.t("connection_panel_reject") },
                    MessageUtils.t("connection_panel_accept"));
            if (option == JOptionPane.YES_OPTION) {
                listConnections.add(connection);
                return true;
            } else {
                ((GUISwing) gui).showNotification(MessageUtils.t("connection_panel_error_add_connection"),
                        GUISwing.NotificationType.ERROR);
                LOG.info("Conexión rechazada por el usuario");
                return false;
            }
        } else {
            ((GUISwing) gui).showNotification(
                    MessageUtils.t("connection_panel_authorization_error") + "\nMessage: " + indication,
                    GUISwing.NotificationType.ERROR);
            return false;
        }
    }

    public void loginComplete(Connection connection) {
        connection.setLogged(true);
        showConnectionDetails(connection, false);
    }

    public Connection getConnection(String service) {
        for (Connection connection : listConnections) {
            if (connection.getService().equals(service)) {
                return connection;
            }
        }
        return null;
    }

    public void Disconnecte(Connection connection) {
        connection.stop(gui);
        showConnectionDetails(connection, false);
    }

    public List<Connection> getListConnections() {
        return listConnections;
    }

    public JPanel getInfoPanel() {
        return infoPanel;
    }

    public GUIInterface getGUIInterface() {
        return gui;
    }

    public SwingMainWindowFrame getFrame() {
        return frame;
    }
}
