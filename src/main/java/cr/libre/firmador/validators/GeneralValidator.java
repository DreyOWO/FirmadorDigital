package cr.libre.firmador.validators;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cr.libre.firmador.Report;
import eu.europa.esig.dss.enumerations.MimeTypeEnum;
import eu.europa.esig.dss.enumerations.TokenExtractionStrategy;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.service.crl.OnlineCRLSource;
import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.policy.SignaturePolicyProvider;
import eu.europa.esig.dss.spi.signature.AdvancedSignature;
import eu.europa.esig.dss.spi.x509.CertificateSource;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.dss.spi.x509.aia.DefaultAIASource;
import eu.europa.esig.dss.spi.validation.CertificateVerifier;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.identifier.UserFriendlyIdentifierProvider;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.dss.xades.validation.XMLDocumentValidator;

public class GeneralValidator implements Validator {
    private SignedDocumentValidator documentValidator;

    public DSSDocument loadDocumentPath(String fileName) {
        CertificateSource trustedCertSource = new CommonTrustedCertificateSource();
        trustedCertSource.addCertificate(DSSUtils.loadCertificate(this.getClass().getClassLoader().getResourceAsStream("certs/CA RAIZ NACIONAL - COSTA RICA v2.crt")));
        trustedCertSource.addCertificate(DSSUtils.loadCertificate(this.getClass().getClassLoader().getResourceAsStream("certs/CA RAIZ NACIONAL COSTA RICA.cer")));
        // Just for testing for now, it should be adviced this Root CA is not trusted and not a part of national official document format policy. It is just for tax office purposes
        //trustedCertSource.addCertificate(DSSUtils.loadCertificate(this.getClass().getClassLoader().getResourceAsStream("certs/CA RAIZ MINISTERIO DE HACIENDA.crt")));
        CertificateVerifier cv = new CommonCertificateVerifier();
        cv.setTrustedCertSources(trustedCertSource);
        cv.setOcspSource(new OnlineOCSPSource());
        cv.setCrlSource(new OnlineCRLSource());
        cv.setAIASource(new DefaultAIASource());
        FileDocument fileDocument = new FileDocument(fileName);
        documentValidator = SignedDocumentValidator.fromDocument(fileDocument);
        documentValidator.setCertificateVerifier(cv);
        documentValidator.setTokenExtractionStrategy(TokenExtractionStrategy.EXTRACT_ALL);
        documentValidator.setTokenIdentifierProvider(new UserFriendlyIdentifierProvider());
        // List<AdvancedSignature> signatures = documentValidator.getSignatures();

        //documentValidator.setIncludeSemantics(true);
        if (fileDocument.getMimeType() == MimeTypeEnum.XML) {
            String electronicReceipt = new XMLDocumentValidator(fileDocument).getRootElement().getDocumentElement().getTagName();
            String[] receiptTypes = {"FacturaElectronica", "TiqueteElectronico", "NotaDebitoElectronica", "NotaCreditoElectronica", "FacturaElectronicaCompra", "FacturaElectronicaExportacion", "MensajeReceptor"};
            if (Arrays.asList(receiptTypes).contains(electronicReceipt)) {
                SignaturePolicyProvider signaturePolicyProvider = new SignaturePolicyProvider(); // Custom policy provider for offline policy validation (no PDF download required)
                Map<String, DSSDocument> signaturePoliciesById = new HashMap<>();
                signaturePoliciesById.put("https://atv.hacienda.go.cr/ATV/ComprobanteElectronico/docs/esquemas/2016/v4.3/Resoluci%C3%B3n_General_sobre_disposiciones_t%C3%A9cnicas_comprobantes_electr%C3%B3nicos_para_efectos_tributarios.pdf",
                    new InMemoryDocument(this.getClass().getClassLoader().getResourceAsStream("dgt/Resolucion_General_sobre_disposiciones_tecnicas_comprobantes_electronicos_para_efectos_tributarios.pdf"))); // 4.3 after URL change
                signaturePoliciesById.put("https://www.hacienda.go.cr/ATV/ComprobanteElectronico/docs/esquemas/2016/v4.3/Resoluci%C3%B3n_General_sobre_disposiciones_t%C3%A9cnicas_comprobantes_electr%C3%B3nicos_para_efectos_tributarios.pdf",
                    new InMemoryDocument(this.getClass().getClassLoader().getResourceAsStream("dgt/Resolucion_General_sobre_disposiciones_tecnicas_comprobantes_electronicos_para_efectos_tributarios.pdf"))); // 4.3 before URL change
                signaturePolicyProvider.setSignaturePoliciesById(signaturePoliciesById);
                documentValidator.setSignaturePolicyProvider(signaturePolicyProvider);
            }
        }
        return fileDocument;
    }

    public Reports getReports() {
        Reports reports = documentValidator.validateDocument();
        return reports;
    }

    public boolean isSigned() {
        return !documentValidator.getSignatures().isEmpty();
    }

    @Override
    public boolean hasStringReport() {
        return false;
    }

    @Override
    public String getStringReport() throws Throwable {
       String reportstr="";
        Reports validatorReports = this.getReports();
        if (validatorReports != null) {
            Report report = new Report(validatorReports);
            reportstr = report.getReport();

        }
        
        return reportstr;
    }


    @Override
    public int amountOfSignatures() {
        List<AdvancedSignature> signatures = documentValidator.getSignatures();
        return signatures.size();
    }
}
