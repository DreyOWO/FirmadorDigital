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

package cr.libre.firmador.signers;

import cr.libre.firmador.Settings;
import cr.libre.firmador.documents.SupportedMimeTypeEnum;
import cr.libre.firmador.gui.GUIInterface;


public class DocumentSignerDetector {
    public static DocumentSigner getDocumentSigner(GUIInterface gui, Settings settings,
            SupportedMimeTypeEnum mimeType) {
        DocumentSigner signer;
        if (mimeType.isPDF()) {
            signer = new FirmadorPAdES(gui);
        } else if (mimeType.isOpenDocument()) {
            signer = new FirmadorOpenDocument(gui);
        } else if (mimeType.isOpenxmlformats()) {
            signer = new FirmadorOpenXmlFormat(gui);
        } else if (mimeType.isXML()) {
			signer = new FirmadorXAdES(gui, true);
        } else if (mimeType.isASiC() || mimeType.isImage()){
            signer = new FirmadorASiC(gui);
        } else if (mimeType.isJSON()) {
            signer = new FirmadorJAdES(gui);
        }else{
            signer = new FirmadorASiC(gui);
            //signer = new FirmadorCAdES(gui);
        }
        return signer;
    }
}
