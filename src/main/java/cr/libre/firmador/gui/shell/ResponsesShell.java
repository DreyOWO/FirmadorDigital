package cr.libre.firmador.gui.shell;

import eu.europa.esig.dss.ws.dto.RemoteDocument;

import java.util.List;

public class ResponsesShell {
    public static class ResponseListSignDocument {
        public List<ResponseSignDocument> listResponseSignDocuments;

        public ResponseListSignDocument() {
        }

        public ResponseListSignDocument(List<ResponseSignDocument> listResponseSignDocuments) {
            this.listResponseSignDocuments = listResponseSignDocuments;
        }
    }

    public static class ResponseSignDocument {
        public String base64SignedDocument;
        public String externalId;

        public ResponseSignDocument() {
        }

        public ResponseSignDocument(String base64SignedDocument, String externalId) {
            this.base64SignedDocument = base64SignedDocument;
            this.externalId = externalId;
        }
    }

    public static class ResponseListRemoteDocumet {
        private List<ResponseRemoteDocument> listResponseRemoteDocuments;

        public ResponseListRemoteDocumet() {
        }

        public ResponseListRemoteDocumet(List<ResponseRemoteDocument> listResponseRemoteDocuments) {
            this.listResponseRemoteDocuments = listResponseRemoteDocuments;
        }

        public List<ResponseRemoteDocument> getListResponseRemoteDocuments() {
            return listResponseRemoteDocuments;
        }
    }


    public static class ResponseRemoteDocument {
        private RemoteDocument remoteDocument;
        private String externalId;

        public ResponseRemoteDocument() {
        }

        public ResponseRemoteDocument(RemoteDocument remoteDocument, String externalId) {
            this.remoteDocument = remoteDocument;
            this.externalId = externalId;
        }

        public RemoteDocument getRemoteDocument() {
            return remoteDocument;
        }

        public String getExternalId() {
            return externalId;
        }
    }

    public static class ListValidateDocumentResponse {
        private List<ValidateDocumentResponse> listValidateDocumentResponses;

        public ListValidateDocumentResponse() {
        }

        public ListValidateDocumentResponse(List<ValidateDocumentResponse> listValidateDocumentResponses) {
            this.listValidateDocumentResponses = listValidateDocumentResponses;
        }

        public List<ValidateDocumentResponse> getListValidateDocumentResponses() {
            return listValidateDocumentResponses;
        }
    }

    public static class ValidateDocumentResponse {
        public String externalId;
        public String report;

        public ValidateDocumentResponse() {
        }

        public ValidateDocumentResponse(String externalId, String report) {
            this.externalId = externalId;
            this.report = report;
        }

        public String getExternalId() {
            return externalId;
        }

        public String getReport() {
            return report;
        }
    }

    public static class PreviewDocumentResponse {
        List<String> previewImages;

        public PreviewDocumentResponse() {
        }

        public PreviewDocumentResponse(List<String> previewImages) {
            this.previewImages = previewImages;
        }

        public List<String> getPreviewImages() {
            return previewImages;
        }
    }
}
