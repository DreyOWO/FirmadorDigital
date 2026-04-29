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

import eu.europa.esig.dss.enumerations.MimeType;

public enum SupportedMimeTypeEnum implements MimeType{
    /** octet-stream */
    BINARY("application/octet-stream"),

    XML("text/xml", "xml"),
	XMLA("application/xml", "xml"),
    /** opendocument text */
    ODT("application/vnd.oasis.opendocument.text", "odt"),

    /** opendocument spreadsheet */
    ODS("application/vnd.oasis.opendocument.spreadsheet", "ods"),

    /** opendocument presentation */
    ODP("application/vnd.oasis.opendocument.presentation", "odp"),

    /** opendocument graphics */
    ODG("application/vnd.oasis.opendocument.graphics", "odg"),

    PDF ("application/pdf", "pdf"),

    DOCX ("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
    XLSX ("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
    PPTX ("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"),


    DOC ("application/msword", "doc"),
    PPT ("application/vnd.ms-powerpoint", "ppt"),
    XLS ("application/vnd.ms-excel", "xls"),
    /** json */
    JSON("application/json", "json"),

    ASICE("application/vnd.etsi.asic-e+zip", "asice"),

    ZIP("application/zip", "zip"),

    JPG("image/jpeg", "jpg", "jpeg"),
    PNG("image/png", "png");


    /** MimeType identifier */
    final String mimeTypeString;

    /** File extension corresponding to the MimeType */
    final String[] extensions;

    SupportedMimeTypeEnum(final String mimeTypeString, final String... extensions) {
        this.extensions = extensions;
        this.mimeTypeString = mimeTypeString;

    }

    @Override
    public String getMimeTypeString() {
        return mimeTypeString;
    }

    @Override
    public String getExtension() {
        if (extensions != null && extensions.length > 0) {
            return extensions[0];
        }
        return null;
    }

    public boolean withoutVisualization() {

        return mimeTypeString == XML.getMimeTypeString() ||
				mimeTypeString == XMLA.getMimeTypeString() || isOpenDocument() || isOpenxmlformats() || isMSoldOffice();
    }
    public boolean isOpenDocument() {
        return mimeTypeString == SupportedMimeTypeEnum.ODG.getMimeTypeString() ||
                mimeTypeString == SupportedMimeTypeEnum.ODP.getMimeTypeString() ||
                mimeTypeString == SupportedMimeTypeEnum.ODS.getMimeTypeString() ||
                mimeTypeString == SupportedMimeTypeEnum.ODT.getMimeTypeString();
    }
    public boolean isOpenxmlformats() {
        return mimeTypeString == SupportedMimeTypeEnum.XLSX.getMimeTypeString() ||
                 mimeTypeString == SupportedMimeTypeEnum.DOCX.getMimeTypeString() ||
                 mimeTypeString == SupportedMimeTypeEnum.PPTX.getMimeTypeString() ;
    }
    public boolean isMSoldOffice() {
        return mimeTypeString == SupportedMimeTypeEnum.XLS.getMimeTypeString() ||
                  mimeTypeString == SupportedMimeTypeEnum.DOC.getMimeTypeString() ||
                  mimeTypeString == SupportedMimeTypeEnum.PPT.getMimeTypeString() ;
    }

    public boolean isPDF() {
        return mimeTypeString == SupportedMimeTypeEnum.PDF.getMimeTypeString();
    }

    public boolean isXML() {
		return mimeTypeString == SupportedMimeTypeEnum.XML.getMimeTypeString()
				|| mimeTypeString == SupportedMimeTypeEnum.XMLA.getMimeTypeString();
    }

    public boolean isASiC() {
        return mimeTypeString == SupportedMimeTypeEnum.ASICE.getMimeTypeString();
    }

    public boolean isJSON() {
        return mimeTypeString == SupportedMimeTypeEnum.JSON.getMimeTypeString();
    }

    public boolean isZIP() {
        return mimeTypeString == SupportedMimeTypeEnum.ZIP.getMimeTypeString();
    }

    public boolean isImage() {
        return mimeTypeString == SupportedMimeTypeEnum.JPG.getMimeTypeString()
                || mimeTypeString == SupportedMimeTypeEnum.PNG.getMimeTypeString();
    }
}
