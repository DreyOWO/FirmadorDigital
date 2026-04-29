package cr.libre.firmador.services;

import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.Settings;
import cr.libre.firmador.SettingsManager;
import cr.libre.firmador.connections.Connection;
import cr.libre.firmador.connections.ConnectionUtils;
import cr.libre.firmador.connections.ServicesUrlsIO;
import cr.libre.firmador.gui.GUISwing;
import cr.libre.firmador.gui.swing.ConnectionPanel;
import cr.libre.firmador.gui.swing.RequestHostAuthorizationRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

public class ConnectionPanelService {

    private ConnectionPanel panel;
    Settings settings;
    static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public ConnectionPanelService(ConnectionPanel panel, Settings settings) {
        this.settings = settings;
        this.panel = panel;
    }

    public ActionListener getAddButtonAction() {
        return new AddButtonAction();
    }

    public ActionListener getCloseButtonAction(Connection connection) {
        return new CloseButtonAction(connection);
    }

    public ActionListener getPortButtonAction(Connection connection, JTextField portField, Boolean isRestart) {
        return new PortButtonAction(connection, portField, isRestart);
    }

    public ActionListener getConnectButtonAction(Connection connection, JButton connectButton) {
        return new ConnectButtonAction(connection, connectButton);
    }

    public ActionListener getRequestDocsButtonAction(Connection connection, JButton requestDocsButton) {
        return new RequestDocsButtonAction(connection, requestDocsButton);
    }

    public ActionListener getDeleteButtonAction(Connection connection, String host) {
        return new DeleteButtonAction(connection, host);
    }

    public ActionListener getAuthorizeButtonAction(Connection connection, String domain) {
        return new AuthorizeButtonAction(connection, domain);
    }

    public ActionListener getClearButtonAction(Connection connection) {
        return new ClearButtonAction(connection);
    }

    private class AddButtonAction implements ActionListener {

        public AddButtonAction() {
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            panel.showLoadDialog();
            panel.getFrame().pack();
            panel.getFrame().setMinimumSize(panel.getFrame().getSize());
        }
    }

    private class CloseButtonAction implements ActionListener {

        Connection connection;

        public CloseButtonAction(Connection connection) {
            this.connection = connection;
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (connection.isRunning()) {
                ((GUISwing) panel.getGUIInterface()).showNotification(MessageUtils.t("connection_panel_delete_error"),
                        GUISwing.NotificationType.ERROR);
            } else {
                int option = JOptionPane.showConfirmDialog(
                        panel,
                        MessageUtils.t("connection_panel_confirm_delete") + " \"" + connection.getName() + "\"?",
                        MessageUtils.t("connection_panel_confirm_delete_title"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if (option == JOptionPane.YES_OPTION) {
                    panel.getListConnections().remove(connection);
                    try {
                        ServicesUrlsIO.save(panel.getListConnections());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    panel.showConnectionDetails(null, false);
                    panel.reloadView();
                }
            }
        }
    }

    private class PortButtonAction implements ActionListener {

        Connection connection;
        JTextField portField;
        Boolean isRestart;

        public PortButtonAction(Connection connection, JTextField portField, Boolean isRestart) {
            this.connection = connection;
            this.portField = portField;
            this.isRestart = isRestart;
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                int port = Integer.parseInt(portField.getText());
                connection.setPort(port);
                settings.portNumber = port;
                panel.saveSettings();
                panel.showConnectionDetails(connection, isRestart);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel.getInfoPanel(), MessageUtils.t("connection_panel_port_invalid"));
            }
        }
    }

    private class ConnectButtonAction implements ActionListener {

        Connection connection;
        JButton connectButton;

        public ConnectButtonAction(Connection connection, JButton connectButton) {
            this.connection = connection;
            this.connectButton = connectButton;
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (connection.isRunning()) {
                connectButton.setText(MessageUtils.t("connection_panel_disconnecting"));
                connectButton.setEnabled(false);

                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        connection.stop(panel.getGUIInterface());
                        // Espera para simular el tiempo de desconexión
                        Thread.sleep(2000);
                        return null;
                    }

                    @Override
                    protected void done() {
                        connectButton.setText(MessageUtils.t("connection_panel_connect"));
                        connectButton.setEnabled(true);
                        panel.showConnectionDetails(connection, false);
                        ((GUISwing) panel.getGUIInterface()).showNotification(
                                MessageUtils.t("connection_panel_connection_end"), GUISwing.NotificationType.SUCCESS);

                    }
                }.execute();

            } else {
                connectButton.setText(MessageUtils.t("connection_panel_connecting"));
                connectButton.setEnabled(false);

                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        connection.setErrorList(new ArrayList<>());
                        connection.start(panel.getGUIInterface(), connection.getName());
                        Thread.sleep(2000);
                        return null;
                    }

                    @Override
                    protected void done() {
                        connectButton.setText(MessageUtils.t("connection_panel_disconnect"));
                        connectButton.setEnabled(true);
                        panel.showConnectionDetails(connection, false);
                        if (connection.isRunning()) {
                            ((GUISwing) panel.getGUIInterface()).showNotification(
                                    MessageUtils.t("connection_panel_connection_done"),
                                    GUISwing.NotificationType.SUCCESS);
                        } else {
                            ((GUISwing) panel.getGUIInterface()).showNotification(
                                    MessageUtils.t("connection_panel_connection_failed"),
                                    GUISwing.NotificationType.ERROR);
                        }
                    }
                }.execute();
            }
        }
    }

    private class RequestDocsButtonAction implements ActionListener {

        Connection connection;
        JButton requestDocsButton;

        public RequestDocsButtonAction(Connection connection, JButton requestDocsButton) {
            this.connection = connection;
            this.requestDocsButton = requestDocsButton;
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            requestDocsButton.setText("Solicitando...");
            requestDocsButton.setEnabled(false);

            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return ConnectionUtils.reloadVirtualDocuments(connection);
                }

                @Override
                protected void done() {
                    requestDocsButton.setText("Solicitar documentos");
                    requestDocsButton.setEnabled(true);
                    try {
                        Boolean success = get();
                        if (Boolean.TRUE.equals(success)) {
                            ((GUISwing) panel.getGUIInterface()).showNotification(
                                    MessageUtils.t("connection_panel_success_get_virtual_documents"),
                                    GUISwing.NotificationType.SUCCESS);
                        } else {
                            ((GUISwing) panel.getGUIInterface()).showNotification(
                                    MessageUtils.t("connection_panel_error_get_virtual_documents"),
                                    GUISwing.NotificationType.ERROR);
                        }
                    } catch (Exception ex) {
                        LOG.error(MessageUtils.t("connection_panel_internal_error"), ex);
                        ((GUISwing) panel.getGUIInterface()).showNotification(
                                MessageUtils.t("connection_panel_internal_error"), GUISwing.NotificationType.ERROR);
                    }
                }
            }.execute();
        }
    }

    private class DeleteButtonAction implements ActionListener {

        Connection connection;
        String host;

        public DeleteButtonAction(Connection connection, String host) {
            this.host = host;
            this.connection = connection;
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int confirm = panel.showAlertDeleteHost(host);
            if (confirm == JOptionPane.YES_OPTION) {
                settings.removeAllowedHost(host);
                SettingsManager settingsManager = SettingsManager.getInstance();
                settingsManager.setSettings(settings, true);
                ((GUISwing) panel.getGUIInterface()).reloadConfig();
                panel.showConnectionDetails(connection, false);
                ((GUISwing) panel.getGUIInterface()).showNotification(
                        MessageUtils.t("connection_panel_delete_connection_done"), GUISwing.NotificationType.SUCCESS);
            }
        }
    }

    private class AuthorizeButtonAction implements ActionListener {

        Connection connection;
        String domain;

        public AuthorizeButtonAction(Connection connection, String domain) {
            this.connection = connection;
            this.domain = domain;
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            RequestHostAuthorizationRemote pinrequest = new RequestHostAuthorizationRemote();
            List<String> allowedOrigins = settings.getAllowedHosts();
            int ok = pinrequest.showAndWait(domain);
            if (ok == 1) {
                allowedOrigins.add(domain);
                settings.setRegisteredAllowedOrigins(allowedOrigins);
                SettingsManager settingsManager = SettingsManager.getInstance();
                settingsManager.setSettings(settings, true);
            } else if (ok == 2) {
                settings.addTempAllowedHost(domain);
            }
            settings.removeNoAuthorizedHost(domain);
            ((GUISwing) panel.getGUIInterface()).reloadConfig();
            panel.showConnectionDetails(connection, false);
        }
    }

    private class ClearButtonAction implements ActionListener {

        Connection connection;

        public ClearButtonAction(Connection connection) {
            this.connection = connection;
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            connection.setErrorList(new ArrayList<>());
            panel.showConnectionDetails(connection, false);
        }
    }

}
