package cr.libre.firmador.connections;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cr.libre.firmador.gui.GUIInterface;
import cr.libre.firmador.gui.GUISwing;
import cr.libre.firmador.remote.RemoteHttpWorker;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class Connection {
    private String name;
    private String service;
    private Integer port;
    private SwingWorker<?, ?> worker;
    private List<String> errorList = new ArrayList<String>();
    private String baseUrl;
    private String negotiationUrl;
    private String negotiationStartUrl;
    private String completeUrl;
    private String endSessionUrl;
    private String previewUrl;
    private String signUrl;
    private String deleteUrl;
    private String loginUrl;
    private String validateUrl;
    private String virtualDocumentsUrl;
    private Boolean logged;
    private Boolean startOn;
    private String userLogged;

    public Connection(String name, String service, Integer port, SwingWorker<?, ?> worker) {
        this.name = name;
        this.service = service;
        this.port = port;
        this.worker = worker;
        this.startOn = false;
        this.userLogged = "";
    }

    public Connection(String name, String service, Integer port, SwingWorker<?, ?> worker, String baseUrl,
            String negotiationUrl, String negotiationStartUrl, String completeUrl, String endSessionUrl,
            String previewUrl, String signUrl,
            String deleteUrl, String loginUrl, String validateUrl, String virtualDocumentsUrl, Boolean startOn) {
        this.name = name;
        this.service = service;
        this.port = port;
        this.worker = worker;
        this.baseUrl = baseUrl;
        this.negotiationUrl = negotiationUrl;
        this.completeUrl = completeUrl;
        this.endSessionUrl = endSessionUrl;
        this.previewUrl = previewUrl;
        this.signUrl = signUrl;
        this.deleteUrl = deleteUrl;
        this.loginUrl = loginUrl;
        this.validateUrl = validateUrl;
        this.virtualDocumentsUrl = virtualDocumentsUrl;
        this.logged = false;
        this.startOn = startOn;
        this.userLogged = "";
    }

    public Connection(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        this.name = node.get("name").asText();
        this.service = node.get("service").asText();
        this.port = 0;
        this.worker = null;
        this.baseUrl = node.get("base_url").asText();
        this.negotiationUrl = node.get("negotiation_start_url").asText();
        this.negotiationStartUrl = node.get("negotiation_connection").asText();
        this.completeUrl = node.get("document_complete_url").asText();
        this.endSessionUrl = node.get("end_session_url").asText();
        this.previewUrl = node.get("document_preview_url").asText();
        this.signUrl = node.get("document_sign_url").asText();
        this.deleteUrl = node.get("document_delete_url").asText();
        this.loginUrl = node.get("login_url").asText();
        this.validateUrl = node.get("validate_url").asText();
        this.virtualDocumentsUrl = node.get("virtual_documents_url").asText();
        this.logged = false;
        this.startOn = false;
        this.userLogged = "";
    }

    public void setErrorList(List<String> errorList) {
        this.errorList = errorList;
    }

    public void addError(String error) {
        errorList.add(error);
    }

    public List<String> getErrorList() {
        return errorList;
    }

    public String getName() {
        return name;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getPort() {
        return port;
    }

    public void setSwingWorker(SwingWorker<?, ?> worker) {
        this.worker = worker;
    }

    public boolean isRunning() {
        return worker != null && worker.getState() == SwingWorker.StateValue.STARTED;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getNegotiationUrl(Boolean complete) {
        if (complete)
            return baseUrl + negotiationUrl;
        else
            return negotiationUrl;
    }

    public String getNegotiationStartUrl(Boolean complete) {
        if (complete)
            return baseUrl + negotiationStartUrl;
        else
            return negotiationStartUrl;
    }

    public String getCompleteUrl(Boolean complete) {
        if (complete)
            return baseUrl + completeUrl;
        else
            return completeUrl;
    }

    public String getEndSessionUrl(Boolean complete) {
        if (complete)
            return baseUrl + endSessionUrl;
        else
            return endSessionUrl;
    }

    public String getPreviewUrl(Boolean complete) {
        if (complete)
            return baseUrl + previewUrl;
        else
            return previewUrl;
    }

    public String getSignUrl(Boolean complete) {
        if (complete)
            return baseUrl + signUrl;
        else
            return signUrl;
    }

    public String getDeleteUrl(Boolean complete) {
        if (complete)
            return baseUrl + deleteUrl;
        else
            return deleteUrl;
    }

    public String getLoginUrl(Boolean complete) {
        if (complete)
            return baseUrl + loginUrl;
        else
            return loginUrl;
    }

    public String getValidateUrl(Boolean complete) {
        if (complete)
            return baseUrl + validateUrl;
        else
            return validateUrl;
    }

    public String getGetVirtualDocumentsUrl(Boolean complete) {
        if (complete)
            return baseUrl + virtualDocumentsUrl;
        else
            return virtualDocumentsUrl;
    }

    public Boolean isExternal() {
        return !getService().equals("Firmador Remoto") && !getService().equals("Gaudi");
    }

    public Boolean isLogged() {
        return logged;
    }

    public String getService() {
        return service;
    }

    public void setLogged(Boolean logged) {
        this.logged = logged;
    }

    public Boolean getStartOn() {
        return startOn;
    }

    public void setStartOn(Boolean startOn) {
        this.startOn = startOn;
    }

    public String getUserLogged() {
        return userLogged;
    }

    public void setUserLogged(String userLogged) {
        this.userLogged = userLogged;
    }

    public void start(GUIInterface gui, String name) {
        setUserLogged("");
        if (name.equals("Firmador Remoto")) {
            if (isRunning())
                return;
            RemoteHttpWorker<Void, byte[]> workerRemote = new RemoteHttpWorker<Void, byte[]>(gui, this);
            ((GUISwing) gui).setRemoteWorker(workerRemote);
            setSwingWorker(workerRemote);
            worker.execute();
        } else if (name.equals("Gaudi")) {
            if (isRunning())
                return;
            GaudiIntegration<Void, byte[]> workerRemote = new GaudiIntegration<Void, byte[]>(gui, this);
            ((GUISwing) gui).setGaudiSpeaker(workerRemote);
            setSwingWorker(workerRemote);
            worker.execute();
        } else {
            if (isRunning())
                return;
            RemoteIntegration<Void, byte[]> workerRemote = new RemoteIntegration<Void, byte[]>(gui, this);
            ((GUISwing) gui).setUCRSpeaker(workerRemote);
            setSwingWorker(workerRemote);
            worker.execute();
        }
    }

    public void stop(GUIInterface gui) {
        setUserLogged("");
        if (getName().equals("Firmador Remoto")) {
            if (worker != null) {
                if (worker instanceof RemoteHttpWorker) {
                    ((RemoteHttpWorker<?, ?>) worker).stop();
                } else {
                    worker.cancel(true);
                }
            }
        } else if (getName().equals("Gaudi")) {
            if (worker != null) {
                if (worker instanceof GaudiIntegration) {
                    ((GaudiIntegration<?, ?>) worker).stop();
                } else {
                    worker.cancel(true);
                }
            }
        } else {
            logged = false;
            if (worker != null) {
                if (worker instanceof RemoteIntegration) {
                    ((RemoteIntegration<?, ?>) worker).stop();
                } else {
                    worker.cancel(true);
                }
            }
        }
        ((GUISwing) gui).getListDocumentPanel().reloadView();
    }

}
