/* Firmador is a program to sign documents using AdES standards.

Copyright (C) Firmador authors.

This file is part of Firmador.

Firmador is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Firmador is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Firmador.  If not, see <http://www.gnu.org/licenses/>.  */

package cr.libre.firmador.documents;

import eu.europa.esig.dss.model.DSSDocument;

public class MimeTypeDetector {

    public static SupportedMimeTypeEnum detect(String fileName) {
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            for (SupportedMimeTypeEnum supportedmimetype : SupportedMimeTypeEnum.values()) {
                for (String ext : supportedmimetype.extensions) {
                    if (lower.endsWith("." + ext)) {
                        return supportedmimetype;
                    }
                }
            }
        }
        return SupportedMimeTypeEnum.BINARY;
    }

    public static SupportedMimeTypeEnum detect(byte[] data, String name) {
        return detect(name);
    }

    public static SupportedMimeTypeEnum fromMimeType(String mimeType) {
        if (mimeType == null) {
            return SupportedMimeTypeEnum.BINARY;
        }
        for (SupportedMimeTypeEnum supported : SupportedMimeTypeEnum.values()) {
            if (supported.getMimeTypeString().equalsIgnoreCase(mimeType)) {
                return supported;
            }
        }
        return SupportedMimeTypeEnum.BINARY; // fallback si no coincide
    }


    public static SupportedMimeTypeEnum detect(DSSDocument toSignDocument) {
        /**
         * Una reimplementación será necesaria en un futuro próximo ya que la forma de detectar a este punto es un poco arcaica
         * */

        if (toSignDocument.getName().endsWith(".xlsx") || toSignDocument.getName().endsWith(".XLSX")) {
            return  SupportedMimeTypeEnum.XLSX;
        }
        if (toSignDocument.getName().endsWith(".docx") || toSignDocument.getName().endsWith(".DOCX")) {
            return SupportedMimeTypeEnum.DOCX;
        }
        if (toSignDocument.getName().endsWith(".pptx") || toSignDocument.getName().endsWith(".PPTX")) {
            return SupportedMimeTypeEnum.PPTX;
        }
        if (toSignDocument.getName().endsWith(".asice") || toSignDocument.getName().endsWith(".asice")) {
            return SupportedMimeTypeEnum.ASICE;
        }
        if (toSignDocument.getName().endsWith(".zip") || toSignDocument.getName().endsWith(".zip")) {
            return SupportedMimeTypeEnum.ZIP;
        }
        if (toSignDocument.getName().endsWith(".jpg") || toSignDocument.getName().endsWith(".JPG")) {
            return SupportedMimeTypeEnum.JPG;
        }
        if (toSignDocument.getName().endsWith(".png") || toSignDocument.getName().endsWith(".PNG")) {
            return SupportedMimeTypeEnum.PNG;
        }


        String mimeType = toSignDocument.getMimeType().getMimeTypeString();
        String current_mimetype;
        SupportedMimeTypeEnum selected = SupportedMimeTypeEnum.BINARY;
        for (SupportedMimeTypeEnum supportedmimetype : SupportedMimeTypeEnum.values()) {
            current_mimetype = supportedmimetype.getMimeTypeString();
            if (current_mimetype.contentEquals(mimeType)) {
                selected = supportedmimetype;
                break;
            }
        }
        return selected;
    }

}
