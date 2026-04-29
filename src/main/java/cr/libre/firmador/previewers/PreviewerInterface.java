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

package cr.libre.firmador.previewers;

import java.awt.image.BufferedImage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public interface PreviewerInterface {

    void loadDocument(String filename) throws Throwable;

    void loadDocument(byte[] data) throws Throwable;

    PDDocument getDocument();

    BufferedImage getPageImage(int page) throws Throwable;

    int getNumberOfPages();

    PDFRenderer getRender();

    boolean showSignLabelPreview();

    void closePreview();

    boolean canConfigurePreview();
}

