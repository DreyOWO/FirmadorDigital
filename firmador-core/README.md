# Firmador Core Library

Core library for digital signatures with Costa Rican smart cards supporting AdES standards (CAdES, PAdES, XAdES, JAdES, ASiC).

## Features

- ✅ **Smart Card Support**: Read certificates from PKCS#11 devices (Costa Rican Firma Digital)
- ✅ **PKCS#12 Support**: Use certificate files (.p12, .pfx)
- ✅ **Multiple Signature Formats**:
  - CAdES (PKCS#7) - For any file type
  - PAdES - PDF signatures
  - XAdES - XML signatures
  - JAdES - JSON signatures
  - ASiC - Container signatures
  - OpenXML - Office documents (Word, Excel, PowerPoint)
- ✅ **Signature Validation**: Validate existing signatures
- ✅ **Costa Rica Specific**: Pre-configured with Costa Rican Root CAs and TSA
- ✅ **Headless Operation**: No GUI required

## Installation

### Maven

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>cr.libre.firmador</groupId>
    <artifactId>firmador-core</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'cr.libre.firmador:firmador-core:2.0.0'
```

## Quick Start

### 1. Detect Smart Cards

```java
import cr.libre.firmador.cards.SmartCardDetector;
import cr.libre.firmador.cards.CardSignInfo;
import java.util.List;

// Initialize detector
SmartCardDetector detector = new SmartCardDetector();

// Read available cards
List<CardSignInfo> cards = detector.readListSmartCard();

// Display available certificates
for (CardSignInfo card : cards) {
    System.out.println("Certificate: " + card.getFirstName() + " " + card.getLastName());
    System.out.println("ID: " + card.getIdentification());
    System.out.println("Expires: " + card.getExpires());
}
```

### 2. Sign a PDF Document (PAdES)

```java
import cr.libre.firmador.signers.FirmadorPAdES;
import cr.libre.firmador.cards.CardSignInfo;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import java.security.KeyStore;

// Load document
DSSDocument document = new FileDocument("document.pdf");

// Get card and set PIN
CardSignInfo card = cards.get(0);
char[] pin = "1234".toCharArray();
card.setPin(new KeyStore.PasswordProtection(pin));

// Sign document (headless - no GUI)
FirmadorPAdES signer = new FirmadorPAdES(null);
DSSDocument signedDoc = signer.sign(document, card);

// Save signed document
signedDoc.save("document-signed.pdf");
```

### 3. Sign Any File (CAdES/PKCS#7)

```java
import cr.libre.firmador.signers.FirmadorCAdES;
import cr.libre.firmador.documents.Document;
import cr.libre.firmador.Settings;

// Create document wrapper
Document doc = new Document("data.xml");
doc.setSettings(new Settings());

// Sign with CAdES
FirmadorCAdES cadesSigner = new FirmadorCAdES(null);
DSSDocument signedDoc = cadesSigner.sign(doc, card);

// Save signature (detached)
signedDoc.save("data.xml.p7s");
```

### 4. Validate Signatures

```java
import cr.libre.firmador.validators.GeneralValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.dss.simplereport.SimpleReport;

// Create validator
GeneralValidator validator = new GeneralValidator();

// Load signed document
validator.loadDocumentPath("document-signed.pdf");

// Check if signed
boolean isSigned = validator.isSigned();
System.out.println("Document is signed: " + isSigned);

// Get validation reports
Reports reports = validator.getReports();
SimpleReport simpleReport = reports.getSimpleReport();

// Check signature validity
boolean isValid = simpleReport.isValid(simpleReport.getFirstSignatureId());
System.out.println("Signature is valid: " + isValid);

// Get number of signatures
int signatureCount = validator.amountOfSignatures();
System.out.println("Number of signatures: " + signatureCount);
```

### 5. Using PKCS#12 Files

```java
import cr.libre.firmador.cards.CardSignInfo;
import cr.libre.firmador.cards.PKCS12Manager;

// Create card info for PKCS#12 file
CardSignInfo card = new CardSignInfo(
    CardSignInfo.PKCS12TYPE,
    "/path/to/certificate.p12",
    "certificate.p12"
);

// Set password
char[] password = "password".toCharArray();
card.setPin(new KeyStore.PasswordProtection(password));

// Use with any signer
FirmadorPAdES signer = new FirmadorPAdES(null);
DSSDocument signed = signer.sign(document, card);
```

### 6. Headless Signing (BasicSigner)

```java
import cr.libre.firmador.signers.BasicSigner;
import eu.europa.esig.dss.ws.dto.ToBeSignedDTO;
import eu.europa.esig.dss.ws.dto.SignatureValueDTO;

// Create basic signer (no GUI)
BasicSigner signer = new BasicSigner(null);

// Sign data
ToBeSignedDTO toBeSigned = new ToBeSignedDTO(dataBytes);
SignatureValueDTO signature = signer.sign(card, toBeSigned);

// Get signature bytes
byte[] signatureBytes = signature.getValue();
```

## Advanced Usage

### Custom Certificate Verification

```java
import cr.libre.firmador.signers.CRSigner;
import eu.europa.esig.dss.spi.validation.CertificateVerifier;

CRSigner signer = new CRSigner(null);
CertificateVerifier verifier = signer.getCertificateVerifier();

// Verifier is pre-configured with:
// - Costa Rica Root CAs
// - OCSP and CRL sources
// - AIA (Authority Information Access)
// - Intermediate certificates
```

### Get Authentication and Signing Certificates

```java
import java.security.cert.X509Certificate;
import java.util.Map;

SmartCardDetector detector = new SmartCardDetector();
Map<String, X509Certificate> certs = detector.getAuthenticationAndSignCertificates();

X509Certificate authCert = certs.get("authentication");
X509Certificate signCert = certs.get("sign");
```

### Get Private Key Directly

```java
import java.security.PrivateKey;

SmartCardDetector detector = new SmartCardDetector();
char[] pin = "1234".toCharArray();
PrivateKey privateKey = detector.getSignPrivateKey(pin);
```

## Configuration

### PKCS#11 Library Path

The library automatically detects the PKCS#11 library for Costa Rican smart cards. You can override it:

```java
// Via environment variable
System.setProperty("LIBASEP11", "/path/to/library.so");

// Or in Settings
Settings settings = new Settings();
settings.extraPKCS11Lib = "/path/to/library.so";
```

### Signature Levels

```java
import cr.libre.firmador.Settings;
import eu.europa.esig.dss.enumerations.SignatureLevel;

Settings settings = new Settings();

// CAdES levels
settings.setCAdESLevel(SignatureLevel.CAdES_BASELINE_B);  // Basic
settings.setCAdESLevel(SignatureLevel.CAdES_BASELINE_T);  // With timestamp
settings.setCAdESLevel(SignatureLevel.CAdES_BASELINE_LT); // Long-term
settings.setCAdESLevel(SignatureLevel.CAdES_BASELINE_LTA); // Archival

// PAdES levels
settings.setPAdESLevel(SignatureLevel.PAdES_BASELINE_B);
settings.setPAdESLevel(SignatureLevel.PAdES_BASELINE_LTA);
```

## Error Handling

```java
try {
    DSSDocument signed = signer.sign(document, card);
} catch (eu.europa.esig.dss.model.DSSException e) {
    // DSS-related errors (invalid certificate, etc.)
    System.err.println("Signature error: " + e.getMessage());
} catch (cr.libre.firmador.cards.UnsupportedArchitectureException e) {
    // Architecture mismatch (32-bit vs 64-bit)
    System.err.println("Architecture error: " + e.getMessage());
} catch (Exception e) {
    // Other errors
    System.err.println("Error: " + e.getMessage());
}
```

## Thread Safety

The library is designed for multi-threaded use:

- `SmartCardDetector` runs in its own thread
- Each signer instance is independent
- Validators can be used concurrently

## Requirements

- Java 8 or higher
- PKCS#11 library for smart card access (automatically detected on Costa Rican systems)
- Internet connection for timestamp and revocation checking (optional)

## Costa Rica Specific Features

- Pre-loaded Root CAs from SINPE and Ministerio de Hacienda
- Configured TSA (Time Stamp Authority): `http://tsa.sinpe.fi.cr/tsaHttp/`
- Support for electronic tax receipts (Factura Electrónica)
- Validation of Costa Rican signature policies

## License

GPL v3+ - See COPYING file for details

## Support

For issues and questions:
- GitHub: https://github.com/[your-org]/firmador
- Website: https://firmador.libre.cr