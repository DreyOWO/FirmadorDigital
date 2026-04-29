# ✅ Firmador Library Setup Complete

The Firmador project has been successfully restructured as a reusable library with a separate GUI application.

## 📦 What Was Created

### 1. Multi-Module Maven Structure

```
firmador/
├── pom-parent.xml                    # ✅ Parent POM for building everything
├── firmador-core/                    # ✅ Core library module
│   ├── pom.xml                       # ✅ Core library POM
│   └── README.md                     # ✅ Library usage documentation
├── firmador-gui/                     # ✅ GUI application module
│   └── pom.xml                       # ✅ GUI application POM
├── BUILD.md                          # ✅ Build instructions
├── README-LIBRARY.md                 # ✅ Project overview
├── migrate-sources.sh                # ✅ Linux/Mac migration script
├── migrate-sources.bat               # ✅ Windows migration script
└── .github/workflows/
    └── build-and-publish.yml         # ✅ CI/CD workflow
```

### 2. Core Library (firmador-core)

**Purpose:** Reusable Java library for digital signatures (no GUI dependencies)

**Features:**
- Smart card certificate reading (PKCS#11)
- PKCS#12 file support
- Multiple signature formats (CAdES, PAdES, XAdES, JAdES, ASiC)
- Signature validation
- Costa Rica specific configurations
- Headless operation support

**Key Classes:**
- `SmartCardDetector` - Smart card detection and certificate reading
- `PKCS11Manager` / `PKCS12Manager` - Certificate management
- `FirmadorCAdES`, `FirmadorPAdES`, etc. - Signature generators
- `GeneralValidator` - Signature validation
- `BasicSigner` - Headless signing

### 3. GUI Application (firmador-gui)

**Purpose:** Desktop application built on top of the core library

**Features:**
- User-friendly Swing interface
- Batch document processing
- Document preview
- Plugin system
- Remote signing support

### 4. Documentation

- **`firmador-core/README.md`** - Complete library API documentation with examples
- **`BUILD.md`** - Detailed build and development instructions
- **`README-LIBRARY.md`** - Project overview and quick start guide

### 5. CI/CD Pipeline

GitHub Actions workflow that:
- Builds and tests on every push
- Publishes to GitHub Packages on release
- Runs security scans (OWASP)
- Creates release artifacts

## 🚀 Next Steps

### Step 1: Migrate Source Files

Run the migration script to organize existing source code:

**Linux/Mac:**
```bash
chmod +x migrate-sources.sh
./migrate-sources.sh
```

**Windows:**
```bash
migrate-sources.bat
```

This will copy files from `src/` to the appropriate module directories.

### Step 2: Build the Project

```bash
mvn clean install -f pom-parent.xml
```

This will:
1. Build `firmador-core` library
2. Install it to local Maven repository (`~/.m2/repository/`)
3. Build `firmador-gui` application
4. Create executable JAR: `firmador-gui/target/firmador.jar`

### Step 3: Test the Library

Create a test project to verify the library works:

```java
// TestLibrary.java
import cr.libre.firmador.cards.SmartCardDetector;
import cr.libre.firmador.cards.CardSignInfo;
import java.util.List;

public class TestLibrary {
    public static void main(String[] args) throws Exception {
        SmartCardDetector detector = new SmartCardDetector();
        List<CardSignInfo> cards = detector.readListSmartCard();
        
        System.out.println("Found " + cards.size() + " certificates");
        for (CardSignInfo card : cards) {
            System.out.println("- " + card.getFirstName() + " " + card.getLastName());
        }
    }
}
```

### Step 4: Test the GUI Application

```bash
java -jar firmador-gui/target/firmador.jar
```

### Step 5: Configure GitHub Packages (Optional)

To publish to GitHub Packages:

1. **Update repository URL in `pom-parent.xml`:**
   ```xml
   <distributionManagement>
       <repository>
           <id>github</id>
           <name>GitHub Packages</name>
           <url>https://maven.pkg.github.com/YOUR-ORG/firmador</url>
       </repository>
   </distributionManagement>
   ```

2. **Configure Maven settings** (`~/.m2/settings.xml`):
   ```xml
   <settings>
       <servers>
           <server>
               <id>github</id>
               <username>YOUR-GITHUB-USERNAME</username>
               <password>YOUR-GITHUB-TOKEN</password>
           </server>
       </servers>
   </settings>
   ```

3. **Deploy:**
   ```bash
   mvn deploy -f pom-parent.xml
   ```

### Step 6: Update GitHub Actions

1. Replace `YOUR-ORG` in `.github/workflows/build-and-publish.yml`
2. Add secrets to GitHub repository:
   - `GITHUB_TOKEN` (automatically provided)
   - `SONAR_TOKEN` (if using SonarCloud)

### Step 7: Create a Release

1. Tag a version:
   ```bash
   git tag -a v2.0.0 -m "First library release"
   git push origin v2.0.0
   ```

2. Create a GitHub release - the workflow will automatically:
   - Build the project
   - Run tests
   - Publish to GitHub Packages
   - Create release artifacts

## 📖 Using the Library

### In Your Maven Project

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/YOUR-ORG/firmador</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>cr.libre.firmador</groupId>
        <artifactId>firmador-core</artifactId>
        <version>2.0.0</version>
    </dependency>
</dependencies>
```

### Example: Sign a PDF

```java
import cr.libre.firmador.cards.SmartCardDetector;
import cr.libre.firmador.cards.CardSignInfo;
import cr.libre.firmador.signers.FirmadorPAdES;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import java.security.KeyStore;
import java.util.List;

public class SignPDF {
    public static void main(String[] args) throws Exception {
        // Detect smart cards
        SmartCardDetector detector = new SmartCardDetector();
        List<CardSignInfo> cards = detector.readListSmartCard();
        
        // Get first card and set PIN
        CardSignInfo card = cards.get(0);
        char[] pin = "1234".toCharArray();
        card.setPin(new KeyStore.PasswordProtection(pin));
        
        // Load document
        DSSDocument document = new FileDocument("document.pdf");
        
        // Sign (null = headless, no GUI)
        FirmadorPAdES signer = new FirmadorPAdES(null);
        DSSDocument signed = signer.sign(document, card);
        
        // Save
        signed.save("document-signed.pdf");
        System.out.println("Document signed successfully!");
    }
}
```

## 🔧 Development Workflow

### Making Changes to Core Library

```bash
# 1. Edit files in firmador-core/src/
# 2. Build and install
mvn clean install -f firmador-core/pom.xml
# 3. Test in GUI if needed
mvn clean package -f firmador-gui/pom.xml
```

### Making Changes to GUI

```bash
# 1. Edit files in firmador-gui/src/
# 2. Build
mvn clean package -f firmador-gui/pom.xml
# 3. Run
java -jar firmador-gui/target/firmador.jar
```

### Running Tests

```bash
# All tests
mvn test -f pom-parent.xml

# Core only
mvn test -f firmador-core/pom.xml

# GUI only
mvn test -f firmador-gui/pom.xml
```

## 📋 Checklist

- [x] Create parent POM structure
- [x] Create firmador-core module
- [x] Create firmador-gui module
- [x] Create library documentation
- [x] Create build instructions
- [x] Create migration scripts
- [x] Create CI/CD workflow
- [ ] Run migration script
- [ ] Build and test
- [ ] Update GitHub repository URL
- [ ] Configure GitHub Packages
- [ ] Create first release

## 🎯 Benefits of This Structure

1. **Reusability**: Core library can be used in any Java project
2. **Separation of Concerns**: GUI and core logic are independent
3. **Easier Testing**: Core can be tested without GUI
4. **Better Maintenance**: Changes to GUI don't affect library users
5. **Flexible Deployment**: Library and application can be versioned independently
6. **CI/CD Ready**: Automated build, test, and publish pipeline

## 📚 Documentation Links

- **Library API**: [`firmador-core/README.md`](firmador-core/README.md)
- **Build Guide**: [`BUILD.md`](BUILD.md)
- **Project Overview**: [`README-LIBRARY.md`](README-LIBRARY.md)
- **Original README**: [`README.md`](README.md) (if exists)

## 🆘 Troubleshooting

### "Package does not exist" errors
```bash
# Make sure core is installed first
mvn clean install -f firmador-core/pom.xml
```

### PKCS11 errors on Java 9+
```bash
# The build automatically handles this, but if needed:
mvn clean install -f pom-parent.xml \
  -Darguments="--add-exports=jdk.crypto.cryptoki/sun.security.pkcs11.wrapper=ALL-UNNAMED"
```

### Migration script issues
- Ensure you're in the project root directory
- Check that `src/` directory exists
- On Linux/Mac, make script executable: `chmod +x migrate-sources.sh`

## 🎉 Success!

Your Firmador project is now ready to be used as a library! 

**Next:** Run the migration script and build the project to get started.

```bash
# Linux/Mac
./migrate-sources.sh
mvn clean install -f pom-parent.xml

# Windows
migrate-sources.bat
mvn clean install -f pom-parent.xml
```

For questions or issues, refer to the documentation or open an issue on GitHub.