package cr.libre.firmador.services;

import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.connections.Connection;
import cr.libre.firmador.documents.Document;
import cr.libre.firmador.gui.GUIInterface;
import cr.libre.firmador.gui.swing.ConnectionPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConnectionService {
    private ConnectionPanel connectionPanel;
    private GUIInterface gui;
    final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public ConnectionService(ConnectionPanel connectionPanel, GUIInterface gui) {
        this.gui = gui;
        this.connectionPanel = connectionPanel;
    }

    public boolean validateConnection(String service) {
        Connection connection = connectionPanel.getConnection(service);
        if (!connection.isLogged() || !connection.isRunning()) {
            gui.showErrorAlert(
                MessageUtils.t("guiswing_show_error_not_logged_title"),
                MessageUtils.t("guiswing_show_error_not_logged") + " " + service + " " + MessageUtils.t("guiswing_show_error_not_logged4")
            );
            return false;
        }
        return  true;
    }

    public void disconnect(Document document, String service){
        if (document != null) {
            service = document.getService();
        }
        Connection connection = connectionPanel.getConnection(service);
        gui.showErrorAlert(
            MessageUtils.t("guiswing_show_403_title"),
            MessageUtils.t("guiswing_show_403") + " " + connection.getName()
        );
        connectionPanel.Disconnecte(connection);
    }

    public List<Document> validateVirtualDocumentsConnections(List<Document> virtualDocuments) {

        Set<String> connections = virtualDocuments.stream()
            .map(Document::getService)
            .collect(Collectors.toSet());

        List<String> invalidConnections = new ArrayList<>();
        List<Document> validDocuments = new ArrayList<>(virtualDocuments);

        for (String service : connections) {
            Connection connection = connectionPanel.getConnection(service);

            if (connection == null || !connection.isLogged() || !connection.isRunning()) {
                LOG.warn("Conexión inválida o no disponible: {}", service);
                invalidConnections.add(service);

                validDocuments.removeIf(doc -> service.equals(doc.getService()));
            }
        }

        if (!invalidConnections.isEmpty()) {
            String msg = MessageUtils.t("guiswing_show_error_not_logged")
                + " " + String.join(", ", invalidConnections)
                + " " + MessageUtils.t("guiswing_show_error_not_logged3");

            gui.showErrorAlert(
                MessageUtils.t("guiswing_show_error_not_logged_title"),
                msg
            );

            LOG.error("Se encontraron {} conexiones inválidas: {}",
                invalidConnections.size(),
                String.join(", ", invalidConnections)
            );
        }

        LOG.info("Validación completada. Documentos válidos: {}/{}",
            validDocuments.size(),
            virtualDocuments.size()
        );

        return validDocuments;
    }

}
