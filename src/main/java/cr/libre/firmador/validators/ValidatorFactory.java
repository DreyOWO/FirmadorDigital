package cr.libre.firmador.validators;

import cr.libre.firmador.documents.MimeTypeDetector;
import cr.libre.firmador.documents.SupportedMimeTypeEnum;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

public class ValidatorFactory {
    public static Validator getValidator(String fileName) {
        SupportedMimeTypeEnum mimetype = MimeTypeDetector.detect(fileName);
        Validator validator = new GeneralValidator();
        if (mimetype.isOpenxmlformats()) {
            validator = new OOXMLValidator();
        }
        if (mimetype.isZIP()) {
            if (!isASiC(new File(fileName))) {
                return null;
            }
        }
        try {
        validator.loadDocumentPath(fileName);
        } catch (java.lang.UnsupportedOperationException e) {
            return null; // format is not detected by dss
        }
        return validator;
    }

    public static Validator getValidator(byte[] data, String name) {
        return null;
    }

    public static boolean isASiC(File file) {
        try (ZipFile zip = new ZipFile(file)) {
            return zip.getEntry("META-INF/signatures.p7s") != null;
        } catch (IOException e) {
            return false;
        }
    }

}
