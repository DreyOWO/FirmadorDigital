package cr.libre.firmador.remote;

import java.util.UUID;

public class DocumentPreview {
    private UUID documentid;
    private String page;

    public DocumentPreview() {}

    public UUID getDocumentid() {
        return documentid;
    }

    public void setDocumentid(UUID documentid) {
        this.documentid = documentid;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

}
