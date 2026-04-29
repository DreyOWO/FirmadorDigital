# Firmador - Digital Signature Library & Application

Firmador is a comprehensive digital signature solution for Costa Rican smart cards, supporting multiple AdES signature formats (CAdES, PAdES, XAdES, JAdES, ASiC).

## 🎯 Project Structure

This project is now organized as a **multi-module Maven project**:

```
firmador/
├── firmador-core/      # 📚 Core library (reusable, no GUI)
│   ├── README.md       # Library usage documentation
│   └── pom.xml
├── firmador-gui/       # 🖥️ GUI application
│   └── pom.xml
├── pom-parent.xml      # Parent POM (build everything)
├── BUILD.md            # Build instructions
└── migrate-sources.*   # Migration scripts
```

## 🚀 Quick Start

### For Library Users

If you want to use Firmador as a library in your Java application:

1. **Build and install the library:**
   ```bash
   mvn clean install -f pom-parent.xml
   ```

2. **Add dependency to your project:**
   ```xml
   <dependency>
       <groupId>cr.libre.firmador</groupId>
       <artifactId>firmador-core</artifactId>
       <version>2.0.0</version>
   </dependency>
   ```

3. **Use in your code:**
   ```java
   import cr.libre.firmador.cards.SmartCardDetector;
   import cr.libre.firmador.signers.FirmadorPAdES;
   
   // Detect smart cards
   SmartCardDetector detector = new SmartCardDetector();
   List<CardSignInfo> cards = detector.readListSmartCard();
   
   // Sign a PDF
   FirmadorPAdES signer = new FirmadorPAdES(null);
   DSSDocument signed = signer.sign(document, card);
   ```

📖 **See [`firmador-core/README.md`](firmador-core/README.md) for complete library documentation**

### For GUI Application Users

If you want to use the GUI application:

1. **Build the application:**
   ```bash
   mvn clean package -f pom-parent.xml
   ```

2. **Run the application:**
   ```bash
   java -jar firmador-gui/target/firmador.jar
   ```

### For Developers

If you want to contribute or modify the code:

1. **Clone the repository:**
   ```bash
   git clone https://github.com/YOUR-ORG/firmador.git
   cd firmador
   ```

2. **Run migration script** (first time only):
   ```bash
   # Linux/Mac
   chmod +x migrate-sources.sh
   ./migrate-sources.sh
   
   # Windows
   migrate-sources.bat
   ```

3. **Build everything:**
   ```bash
   mvn clean install -f pom-parent.xml
   ```

📖 **See [`BUILD.md`](BUILD.md) for detailed build instructions**

## 📦 What's Included

### Firmador Core Library (`firmador-core`)

A standalone Java library providing:

- ✅ **Smart Card Support**: PKCS#11 interface for Costa Rican Firma Digital cards
- ✅ **PKCS#12 Support**: Certificate files (.p12, .pfx)
- ✅ **Multiple Signature Formats**:
  - **CAdES** (PKCS#7) - Universal format for any file
  - **PAdES** - PDF signatures with visual representation
  - **XAdES** - XML signatures
  - **JAdES** - JSON signatures
  - **ASiC** - Container signatures
  - **OpenXML** - Office documents (Word, Excel, PowerPoint)
- ✅ **Signature Validation**: Verify existing signatures
- ✅ **Costa Rica Specific**: Pre-configured Root CAs and TSA
- ✅ **Headless Operation**: No GUI required
- ✅ **Thread-Safe**: Designed for concurrent use

**Key Classes:**
- `SmartCardDetector` - Detect and read smart cards
- `FirmadorPAdES`, `FirmadorCAdES`, `FirmadorXAdES`, etc. - Signature generators
- `GeneralValidator` - Signature validation
- `BasicSigner` - Headless signing operations

### Firmador GUI Application (`firmador-gui`)

A user-friendly desktop application built on top of the core library:

- 🖥️ Modern Swing-based interface with FlatLaf theme
- 📁 Batch processing of multiple documents
- 👁️ Document preview
- 🔌 Plugin system for extensibility
- 🌐 Remote signing support
- 📊 Detailed validation reports

## 🔧 Technology Stack

- **Java**: 8+ (Java 11+ recommended)
- **Build Tool**: Maven 3.6.3+
- **Signature Framework**: EU DSS (Digital Signature Service) 6.4
- **GUI Framework**: Swing with FlatLaf
- **Smart Card**: PKCS#11 (sun.security.pkcs11)
- **PDF**: Apache PDFBox 3.0.7
- **Office**: Apache POI 5.5.1
- **Logging**: SLF4J 2.0.17

## 📚 Documentation

- **[`firmador-core/README.md`](firmador-core/README.md)** - Library usage and API documentation
- **[`BUILD.md`](BUILD.md)** - Build instructions and development guide
- **[Original README.md](README.md)** - Application user guide (if exists)

## 🏗️ Architecture

### Module Dependencies

```
firmador-parent (pom)
├── firmador-core (jar)
│   └── Dependencies: DSS, PDFBox, POI, etc.
└── firmador-gui (jar)
    └── Dependencies: firmador-core, FlatLaf
```

### Core Library Design

The core library is designed to be:

1. **GUI-Independent**: No Swing/AWT dependencies
2. **Modular**: Clear separation of concerns
3. **Extensible**: Interface-based design
4. **Testable**: Comprehensive test coverage
5. **Thread-Safe**: Safe for concurrent use

### Package Organization

**Core Library Packages:**
```
cr.libre.firmador
├── cards/          # Smart card and certificate management
├── signers/        # Signature generation (CAdES, PAdES, etc.)
├── validators/     # Signature validation
├── documents/      # Document handling
├── ooxml/          # Office document support
├── remote/         # Remote signing
├── connections/    # Connection management
└── services/       # Core services
```

**GUI Application Packages:**
```
cr.libre.firmador
├── gui/            # GUI components
├── plugins/        # Plugin system
└── previewers/     # Document preview
```

## 🔐 Security Features

- ✅ PIN-protected smart card access
- ✅ Secure credential storage
- ✅ Certificate validation with OCSP/CRL
- ✅ Timestamp support (TSA)
- ✅ Costa Rican Root CA trust anchors
- ✅ AdES-B, AdES-T, AdES-LT, AdES-LTA levels

## 🇨🇷 Costa Rica Specific

This project is specifically designed for Costa Rican digital signatures:

- **Root CAs**: CA RAIZ NACIONAL - COSTA RICA v2
- **TSA**: http://tsa.sinpe.fi.cr/tsaHttp/
- **Smart Cards**: SINPE Firma Digital cards
- **Tax Receipts**: Support for electronic invoices (Factura Electrónica)
- **Signature Policies**: Costa Rican signature policy validation

## 📋 Requirements

### Runtime Requirements
- Java 8 or higher (Java 11+ recommended)
- PKCS#11 library for smart card access (auto-detected on Costa Rican systems)
- Internet connection for timestamp and revocation checking (optional)

### Development Requirements
- Java Development Kit (JDK) 8+
- Maven 3.6.3+
- Git

## 🚢 Deployment

### Local Maven Repository

After building, the library is available in `~/.m2/repository/`:
```
~/.m2/repository/cr/libre/firmador/
├── firmador-core/2.0.0/
│   └── firmador-core-2.0.0.jar
└── firmador-gui/2.0.0/
    └── firmador-gui-2.0.0.jar
```

### GitHub Packages

To publish to GitHub Packages:

1. Configure `~/.m2/settings.xml`:
   ```xml
   <servers>
       <server>
           <id>github</id>
           <username>YOUR-GITHUB-USERNAME</username>
           <password>YOUR-GITHUB-TOKEN</password>
       </server>
   </servers>
   ```

2. Deploy:
   ```bash
   mvn deploy -f pom-parent.xml
   ```

3. Use in other projects:
   ```xml
   <repositories>
       <repository>
           <id>github</id>
           <url>https://maven.pkg.github.com/YOUR-ORG/firmador</url>
       </repository>
   </repositories>
   ```

## 🧪 Testing

```bash
# Run all tests
mvn test -f pom-parent.xml

# Run core tests only
mvn test -f firmador-core/pom.xml

# Run GUI tests only
mvn test -f firmador-gui/pom.xml

# Skip tests
mvn install -f pom-parent.xml -DskipTests=true
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

GPL v3+ - See [COPYING](COPYING) file for details

## 🔗 Links

- **Website**: https://firmador.libre.cr
- **Documentation**: https://firmador.libre.cr/docs
- **Issues**: https://github.com/YOUR-ORG/firmador/issues

## 💡 Examples

### Sign a PDF with Smart Card

```java
SmartCardDetector detector = new SmartCardDetector();
List<CardSignInfo> cards = detector.readListSmartCard();
CardSignInfo card = cards.get(0);
card.setPin(new KeyStore.PasswordProtection("1234".toCharArray()));

FirmadorPAdES signer = new FirmadorPAdES(null);
DSSDocument document = new FileDocument("contract.pdf");
DSSDocument signed = signer.sign(document, card);
signed.save("contract-signed.pdf");
```

### Validate a Signature

```java
GeneralValidator validator = new GeneralValidator();
validator.loadDocumentPath("contract-signed.pdf");

if (validator.isSigned()) {
    Reports reports = validator.getReports();
    SimpleReport simple = reports.getSimpleReport();
    boolean valid = simple.isValid(simple.getFirstSignatureId());
    System.out.println("Valid: " + valid);
}
```

### Sign Multiple Files (Batch)

```java
SmartCardDetector detector = new SmartCardDetector();
CardSignInfo card = detector.readListSmartCard().get(0);
card.setPin(new KeyStore.PasswordProtection("1234".toCharArray()));

FirmadorCAdES signer = new FirmadorCAdES(null);
String[] files = {"doc1.xml", "doc2.xml", "doc3.xml"};

for (String file : files) {
    DSSDocument doc = new FileDocument(file);
    DSSDocument signed = signer.sign(doc, card);
    signed.save(file + ".p7s");
}
```

## 🆘 Support

For questions and support:
- 📧 Email: support@firmador.libre.cr
- 💬 GitHub Issues: https://github.com/YOUR-ORG/firmador/issues
- 📖 Documentation: https://firmador.libre.cr/docs

## 🎉 Acknowledgments

- EU DSS Framework for signature standards
- Costa Rican SINPE for smart card infrastructure
- All contributors to this project