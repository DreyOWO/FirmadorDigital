# Building Firmador Library and Application

This document explains how to build the Firmador project in its new multi-module structure.

## Project Structure

```
firmador/
├── pom-parent.xml          # Parent POM (use this to build everything)
├── pom.xml                 # Original monolithic POM (deprecated)
├── BUILD.md                # This file
├── firmador-core/          # Core library module
│   ├── pom.xml
│   ├── README.md           # Library usage documentation
│   └── src/
│       ├── main/java/      # Core library source (no GUI)
│       ├── main/java9/     # Java 9+ specific code
│       ├── main/resources/ # Certificates, messages, etc.
│       └── test/java/      # Core tests
├── firmador-gui/           # GUI application module
│   ├── pom.xml
│   └── src/
│       ├── main/java/      # GUI source code
│       ├── main/java9/     # Java 9+ specific code
│       └── test/java/      # GUI tests
└── src/                    # Original source (to be migrated)
```

## Prerequisites

- Java 8 or higher (Java 11+ recommended)
- Maven 3.6.3 or higher
- Git

## Quick Build

### Build Everything

```bash
# From the project root directory
mvn clean install -f pom-parent.xml
```

This will:
1. Build `firmador-core` library
2. Install it to your local Maven repository
3. Build `firmador-gui` application (depends on core)
4. Create executable JAR: `firmador-gui/target/firmador.jar`

### Build Only the Library

```bash
mvn clean install -f firmador-core/pom.xml
```

This creates:
- `firmador-core/target/firmador-core-2.0.0.jar` - The library
- Installs to `~/.m2/repository/cr/libre/firmador/firmador-core/2.0.0/`

### Build Only the GUI Application

```bash
# First ensure core is installed
mvn clean install -f firmador-core/pom.xml

# Then build GUI
mvn clean package -f firmador-gui/pom.xml
```

## Migration Steps (For Developers)

The project is being migrated from a monolithic structure to a multi-module structure. Here's what needs to be done:

### Step 1: Organize Source Files

#### Core Module (firmador-core/src/main/java/)
Move these packages (NO GUI dependencies):
- `cr.libre.firmador.cards.*` - Smart card management
- `cr.libre.firmador.signers.*` - Signature generation
- `cr.libre.firmador.validators.*` - Signature validation
- `cr.libre.firmador.documents.*` - Document handling
- `cr.libre.firmador.ooxml.*` - Office document support
- `cr.libre.firmador.remote.*` - Remote signing
- `cr.libre.firmador.connections.*` - Connection management
- `cr.libre.firmador.services.*` - Core services
- Core utility classes:
  - `CertificateManager.java`
  - `MessageUtils.java`
  - `Report.java`
  - `Settings.java`
  - `SettingsManager.java`
  - `SingleInstanceManager.java`
  - `ConfigListener.java`

#### GUI Module (firmador-gui/src/main/java/)
Move these packages:
- `cr.libre.firmador.gui.*` - All GUI code
- `cr.libre.firmador.plugins.*` - Plugin system (GUI-dependent)
- `cr.libre.firmador.previewers.*` - Document preview (GUI-dependent)
- `Firmador.java` - Main application class

#### Resources
- **Core**: `firmador-core/src/main/resources/`
  - `certs/` - Certificate files
  - `dgt/` - Tax office documents
  - `xml/` - XML templates
  - `xslt/` - XSLT transformations
  - `dss-messages_es.properties`
  - `messages*.properties`

- **GUI**: `firmador-gui/src/main/resources/`
  - `*.png`, `*.ico` - Icons and images
  - `install_windows.vbs`
  - `nonPreview.pdf`

### Step 2: Update Imports

After moving files, update imports in GUI module:
```java
// No changes needed - same package structure
import cr.libre.firmador.cards.SmartCardDetector;
import cr.libre.firmador.signers.FirmadorPAdES;
```

### Step 3: Make Core GUI-Independent

The core library should work without GUI. Update these classes:

**Option A: Make GUIInterface Optional**
```java
// In signers and other core classes
public class FirmadorPAdES extends CRSigner implements DocumentSigner {
    public FirmadorPAdES(GUIInterface gui) {
        super(gui);
        // gui can be null for headless operation
    }
    
    private void notifyProgress(String message) {
        if (gui != null) {
            gui.nextStep(message);
        } else {
            LOG.info(message);
        }
    }
}
```

**Option B: Use Callbacks**
```java
// Create a callback interface in core
public interface ProgressCallback {
    void onProgress(String message);
    void onError(Throwable error);
}

// Use in signers
public class FirmadorPAdES {
    private ProgressCallback callback;
    
    public void setProgressCallback(ProgressCallback callback) {
        this.callback = callback;
    }
}
```

### Step 4: Test the Build

```bash
# Clean everything
mvn clean -f pom-parent.xml

# Build with tests
mvn install -f pom-parent.xml

# Run GUI application
java -jar firmador-gui/target/firmador.jar
```

## Using the Library in Your Project

### From Local Maven Repository

After building, the library is available in your local Maven repository:

```xml
<dependency>
    <groupId>cr.libre.firmador</groupId>
    <artifactId>firmador-core</artifactId>
    <version>2.0.0</version>
</dependency>
```

### From GitHub Packages

To publish to GitHub Packages, add to `pom-parent.xml`:

```xml
<distributionManagement>
    <repository>
        <id>github</id>
        <name>GitHub Packages</name>
        <url>https://maven.pkg.github.com/YOUR-ORG/firmador</url>
    </repository>
</distributionManagement>
```

Then configure `~/.m2/settings.xml`:

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

Deploy with:
```bash
mvn deploy -f pom-parent.xml
```

### Using from GitHub Packages

In your project's `pom.xml`:

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

## Development Workflow

### Making Changes to Core Library

```bash
# 1. Make changes in firmador-core/src/
# 2. Build and install
mvn clean install -f firmador-core/pom.xml

# 3. Test in GUI (if needed)
mvn clean package -f firmador-gui/pom.xml
java -jar firmador-gui/target/firmador.jar
```

### Making Changes to GUI

```bash
# 1. Make changes in firmador-gui/src/
# 2. Build
mvn clean package -f firmador-gui/pom.xml

# 3. Run
java -jar firmador-gui/target/firmador.jar
```

### Running Tests

```bash
# All tests
mvn test -f pom-parent.xml

# Core tests only
mvn test -f firmador-core/pom.xml

# GUI tests only
mvn test -f firmador-gui/pom.xml

# Skip tests
mvn install -f pom-parent.xml -DskipTests=true
```

## Troubleshooting

### "Package does not exist" errors

Make sure core is installed first:
```bash
mvn clean install -f firmador-core/pom.xml
```

### PKCS11 errors on Java 9+

The build automatically adds required exports. If you still have issues:
```bash
mvn clean install -f pom-parent.xml \
  -Darguments="--add-exports=jdk.crypto.cryptoki/sun.security.pkcs11.wrapper=ALL-UNNAMED"
```

### Multi-Release JAR issues

Both modules create Multi-Release JARs for Java 9+ compatibility. Ensure:
- `src/main/java9/` exists in both modules
- Java 9+ code is in this directory
- Manifest includes `Multi-Release: true`

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Build and Test

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '11'
          distribution: 'temurin'
      
      - name: Build with Maven
        run: mvn clean install -f pom-parent.xml
      
      - name: Upload Core Library
        uses: actions/upload-artifact@v3
        with:
          name: firmador-core
          path: firmador-core/target/firmador-core-*.jar
      
      - name: Upload GUI Application
        uses: actions/upload-artifact@v3
        with:
          name: firmador-gui
          path: firmador-gui/target/firmador.jar
```

## Version Management

To update version across all modules:

```bash
mvn versions:set -DnewVersion=2.1.0 -f pom-parent.xml
mvn versions:commit -f pom-parent.xml
```

## Next Steps

1. ✅ Create parent POM structure
2. ✅ Create firmador-core module POM
3. ✅ Create firmador-gui module POM
4. ⏳ Migrate source files to appropriate modules
5. ⏳ Update core to be GUI-independent
6. ⏳ Test build and functionality
7. ⏳ Update CI/CD pipelines
8. ⏳ Publish to GitHub Packages

## Questions?

- Check `firmador-core/README.md` for library usage
- See original `README.md` for application usage
- Open an issue on GitHub