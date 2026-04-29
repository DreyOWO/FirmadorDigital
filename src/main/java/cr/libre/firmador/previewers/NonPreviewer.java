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
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import cr.libre.firmador.Settings;
import cr.libre.firmador.SettingsManager;

public class NonPreviewer implements PreviewerInterface {
    private PDDocument document = null;
    private PDFRenderer renderer = null;
    private Settings settings;

    NonPreviewer() {
        settings = SettingsManager.getInstance().getAndCreateSettings();
    }
    @Override
    public void loadDocument(String filename) throws Throwable {
		document = Loader.loadPDF(
				new RandomAccessReadBuffer(this.getClass().getClassLoader().getResourceAsStream("nonPreview.pdf")));
    }

    @Override
    public void loadDocument(byte[] data) throws Throwable {
		document = Loader.loadPDF(
				new RandomAccessReadBuffer(this.getClass().getClassLoader().getResourceAsStream("nonPreview.pdf")));
    }

    @Override
    public PDDocument getDocument() {
        return document;
    }

    @Override
    public int getNumberOfPages() {
        if (document != null)
            return document.getNumberOfPages();
        return 0;
    }

    @Override
    public PDFRenderer getRender() {
        if (renderer == null)
            renderer = new PDFRenderer(document);
        return renderer;
    }

    @Override
    public BufferedImage getPageImage(int page) throws Throwable {

        return getRender().renderImage(page, settings.pDFImgScaleFactor);
    }

    @Override
    public boolean showSignLabelPreview() {
        return false;
    }

    @Override
    public void closePreview() {
        if (document != null) {
            try {
                document.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean canConfigurePreview() {
        return true;
    }
}
