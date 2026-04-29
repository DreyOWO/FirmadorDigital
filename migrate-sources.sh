#!/bin/bash
# Migration script to organize source files into core and GUI modules

set -e

echo "=== Firmador Source Migration Script ==="
echo "This script will organize source files into firmador-core and firmador-gui modules"
echo ""

# Create directory structure
echo "Creating directory structure..."
mkdir -p firmador-core/src/main/java/cr/libre/firmador
mkdir -p firmador-core/src/main/java9
mkdir -p firmador-core/src/main/resources
mkdir -p firmador-core/src/test/java

mkdir -p firmador-gui/src/main/java/cr/libre/firmador
mkdir -p firmador-gui/src/main/java9
mkdir -p firmador-gui/src/main/resources
mkdir -p firmador-gui/src/test/java

# Copy core packages (no GUI dependencies)
echo "Copying core packages..."
cp -r src/main/java/cr/libre/firmador/cards firmador-core/src/main/java/cr/libre/firmador/
cp -r src/main/java/cr/libre/firmador/signers firmador-core/src/main/java/cr/libre/firmador/
cp -r src/main/java/cr/libre/firmador/validators firmador-core/src/main/java/cr/libre/firmador/
cp -r src/main/java/cr/libre/firmador/documents firmador-core/src/main/java/cr/libre/firmador/
cp -r src/main/java/cr/libre/firmador/ooxml firmador-core/src/main/java/cr/libre/firmador/
cp -r src/main/java/cr/libre/firmador/remote firmador-core/src/main/java/cr/libre/firmador/
cp -r src/main/java/cr/libre/firmador/connections firmador-core/src/main/java/cr/libre/firmador/
cp -r src/main/java/cr/libre/firmador/services firmador-core/src/main/java/cr/libre/firmador/

# Copy core utility classes
echo "Copying core utility classes..."
cp src/main/java/cr/libre/firmador/CertificateManager.java firmador-core/src/main/java/cr/libre/firmador/
cp src/main/java/cr/libre/firmador/MessageUtils.java firmador-core/src/main/java/cr/libre/firmador/
cp src/main/java/cr/libre/firmador/Report.java firmador-core/src/main/java/cr/libre/firmador/
cp src/main/java/cr/libre/firmador/Settings.java firmador-core/src/main/java/cr/libre/firmador/
cp src/main/java/cr/libre/firmador/SettingsManager.java firmador-core/src/main/java/cr/libre/firmador/
cp src/main/java/cr/libre/firmador/SingleInstanceManager.java firmador-core/src/main/java/cr/libre/firmador/
cp src/main/java/cr/libre/firmador/ConfigListener.java firmador-core/src/main/java/cr/libre/firmador/

# Copy GUI packages
echo "Copying GUI packages..."
cp -r src/main/java/cr/libre/firmador/gui firmador-gui/src/main/java/cr/libre/firmador/
cp -r src/main/java/cr/libre/firmador/plugins firmador-gui/src/main/java/cr/libre/firmador/
cp -r src/main/java/cr/libre/firmador/previewers firmador-gui/src/main/java/cr/libre/firmador/

# Copy main class
echo "Copying main application class..."
cp src/main/java/Firmador.java firmador-gui/src/main/java/

# Copy Java 9+ specific code
echo "Copying Java 9+ code..."
if [ -d "src/main/java9" ]; then
    cp -r src/main/java9/* firmador-core/src/main/java9/ 2>/dev/null || true
    cp -r src/main/java9/* firmador-gui/src/main/java9/ 2>/dev/null || true
fi

# Copy resources
echo "Copying resources..."
# Core resources (certificates, messages, etc.)
cp -r src/main/resources/certs firmador-core/src/main/resources/
cp -r src/main/resources/dgt firmador-core/src/main/resources/
cp -r src/main/resources/xml firmador-core/src/main/resources/
cp -r src/main/resources/xslt firmador-core/src/main/resources/
cp src/main/resources/dss-messages_es.properties firmador-core/src/main/resources/
cp src/main/resources/messages*.properties firmador-core/src/main/resources/

# GUI resources (icons, images)
cp src/main/resources/*.png firmador-gui/src/main/resources/ 2>/dev/null || true
cp src/main/resources/*.ico firmador-gui/src/main/resources/ 2>/dev/null || true
cp src/main/resources/install_windows.vbs firmador-gui/src/main/resources/ 2>/dev/null || true
cp src/main/resources/nonPreview.pdf firmador-gui/src/main/resources/ 2>/dev/null || true
cp -r src/main/resources/icons firmador-gui/src/main/resources/ 2>/dev/null || true

# Copy tests
echo "Copying tests..."
# Core tests
cp -r src/test/java/cr/libre/firmador/signers firmador-core/src/test/java/cr/libre/firmador/ 2>/dev/null || true
cp -r src/test/java/cr/libre/firmador/validators firmador-core/src/test/java/cr/libre/firmador/ 2>/dev/null || true
cp src/test/java/cr/libre/firmador/TestSettingsManager.java firmador-core/src/test/java/cr/libre/firmador/ 2>/dev/null || true

# GUI tests
cp -r src/test/java/cr/libre/firmador/gui firmador-gui/src/test/java/cr/libre/firmador/ 2>/dev/null || true
cp -r src/test/java/cr/libre/firmador/plugins firmador-gui/src/test/java/cr/libre/firmador/ 2>/dev/null || true
cp -r src/test/java/cr/libre/firmador/previewers firmador-gui/src/test/java/cr/libre/firmador/ 2>/dev/null || true

# Copy test utilities
cp -r src/test/java/utils firmador-core/src/test/java/ 2>/dev/null || true

echo ""
echo "=== Migration Complete ==="
echo ""
echo "Next steps:"
echo "1. Review the migrated files in firmador-core/ and firmador-gui/"
echo "2. Build the project: mvn clean install -f pom-parent.xml"
echo "3. Test the GUI application: java -jar firmador-gui/target/firmador.jar"
echo ""
echo "Note: The original src/ directory is preserved. You can delete it after verifying the migration."

# Made with Bob
