package cr.libre.firmador.connections;

import cr.libre.firmador.SettingsManager;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public final class ServicesUrlsIO {

    private static final String FILE_NAME = "servicesUrls.xml";

    private ServicesUrlsIO() {
    }

    public static void save(List<Connection> connections) throws Exception {
        SettingsManager settingsManager = SettingsManager.getInstance();
        Path dir = settingsManager.getConfigDir();
        Files.createDirectories(dir);
        Path file = dir.resolve(FILE_NAME);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Element root = doc.createElement("servicesUrls");
        doc.appendChild(root);

        for (Connection c : connections) {
            // if (c.getService().contains("Firmador Remoto") ||
            // c.getService().contains("Gaudi"))
            // continue;
            Element connEl = doc.createElement("connection");
            connEl.setAttribute("name", safe(c.getName()));
            appendTextElement(doc, connEl, "service", c.getService());
            appendTextElement(doc, connEl, "baseUrl", c.getBaseUrl());
            appendTextElement(doc, connEl, "negotiationUrl", c.getNegotiationUrl(false));
            appendTextElement(doc, connEl, "negotiationStartUrl", c.getNegotiationStartUrl(false));
            appendTextElement(doc, connEl, "completeUrl", c.getCompleteUrl(false));
            appendTextElement(doc, connEl, "endSessionUrl", c.getEndSessionUrl(false));
            appendTextElement(doc, connEl, "previewUrl", c.getPreviewUrl(false));
            appendTextElement(doc, connEl, "signUrl", c.getSignUrl(false));
            appendTextElement(doc, connEl, "deleteUrl", c.getDeleteUrl(false));
            appendTextElement(doc, connEl, "loginUrl", c.getLoginUrl(false));
            appendTextElement(doc, connEl, "validateUrl", c.getValidateUrl(false));
            appendTextElement(doc, connEl, "virtualDocumentsUrl", c.getGetVirtualDocumentsUrl(false));
            appendTextElement(doc, connEl, "port", c.getPort() != null ? String.valueOf(c.getPort()) : "0");
            appendTextElement(doc, connEl, "startOn", String.valueOf(c.getStartOn()));
            root.appendChild(connEl);
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(doc), new StreamResult(file.toFile()));
    }

    public static List<Connection> load() throws Exception {
        SettingsManager settingsManager = SettingsManager.getInstance();
        Path file = settingsManager.getConfigDir().resolve(FILE_NAME);
        List<Connection> result = new ArrayList<>();
        if (!Files.exists(file))
            return result;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file.toFile());
        doc.getDocumentElement().normalize();

        NodeList nodes = doc.getElementsByTagName("connection");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE)
                continue;

            Element el = (Element) n;
            String name = el.getAttribute("name");
            String service = text(el, "service");
            String baseUrl = text(el, "baseUrl");
            String negotiationUrl = text(el, "negotiationUrl");
            String negotiationStartUrl = text(el, "negotiationStartUrl");
            String completeUrl = text(el, "completeUrl");
            String endSessionUrl = text(el, "endSessionUrl");
            String previewUrl = text(el, "previewUrl");
            String singUrl = text(el, "signUrl");
            String deleteUrl = text(el, "deleteUrl");
            String loginUrl = text(el, "loginUrl");
            String validateUrl = text(el, "validateUrl");
            String virtualDocumentsUrl = text(el, "virtualDocumentsUrl");
            String portStr = text(el, "port");
            int port = portStr.isEmpty() ? 0 : Integer.parseInt(portStr);
            String startOn = text(el, "startOn");

            Connection c = new Connection(
                    name,
                    service,
                    port,
                    null,
                    baseUrl,
                    negotiationUrl,
                    negotiationStartUrl,
                    completeUrl,
                    endSessionUrl,
                    previewUrl,
                    singUrl,
                    deleteUrl,
                    loginUrl,
                    validateUrl,
                    virtualDocumentsUrl,
                    Boolean.parseBoolean(startOn));
            result.add(c);
        }
        return result;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static void appendTextElement(Document doc, Element parent, String tag, String value) {
        if (value == null || value.isEmpty())
            return;
        Element el = doc.createElement(tag);
        el.appendChild(doc.createTextNode(value));
        parent.appendChild(el);
    }

    private static String text(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0)
            return "";
        Node n = nl.item(0);
        return n.getTextContent() == null ? "" : n.getTextContent().trim();
    }

/* FIXME private and never used locally, remove if not planned to be used here
    private static boolean isAbsoluteUrl(String s) {
        return s != null && s.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*");
    }
*/

}
