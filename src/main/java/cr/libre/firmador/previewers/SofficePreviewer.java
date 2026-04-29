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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
//import java.nio.file.FileSystems;
import java.nio.file.Files;
//import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cr.libre.firmador.Settings;
import cr.libre.firmador.SettingsManager;
import cr.libre.firmador.documents.MimeTypeDetector;
import cr.libre.firmador.documents.SupportedMimeTypeEnum;

public class SofficePreviewer implements PreviewerInterface {
    final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private PDDocument document = null;
    private PDFRenderer renderer = null;
    private Settings settings;

    SofficePreviewer() {
        settings = SettingsManager.getInstance().getAndCreateSettings();
    }

    public void loadDocument(String fileName) throws Throwable {

        File importfile = new File(fileName);

        SupportedMimeTypeEnum mimetype = MimeTypeDetector.detect(fileName);

        String conversorsource = "pdf:writer_pdf_Export";
        if (mimetype == SupportedMimeTypeEnum.XLSX || mimetype == SupportedMimeTypeEnum.ODS) {
            conversorsource = "pdf:calc_pdf_Export";
        }
        if (mimetype == SupportedMimeTypeEnum.ODP || mimetype == SupportedMimeTypeEnum.PPTX) {
            conversorsource = "pdf:draw_pdf_Export";
        }

        //String separator = FileSystems.getDefault().getSeparator();
        //String guestFilename = FilenameUtils.removeExtension(importfile.getName()) + ".pdf";
		File tmpfile = Files.createTempDirectory("firmadorlibre").toFile();
		String tmpdir = tmpfile.getAbsolutePath();

		//String[] command = new String[] { settings.getSofficePath(), "--outdir", tmpdir, "--headless", "--convert-to", conversorsource,
		//		importfile.toURI().normalize().toString() };
        String[] command = new String[] {
            settings.getSofficePath(),
            "--headless",
            "--convert-to", conversorsource,
            "--outdir", tmpdir,
            importfile.getAbsolutePath()
        };

        ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.directory(tmpfile);
		String text = "";
		try {
			// Iniciar el proceso
			Process process = processBuilder.start();

			// Leer la salida del proceso
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				text += line;
			}

			// Esperar a que el proceso termine
			int exitCode = process.waitFor();
			LOG.info("El proceso terminó con el código: " + exitCode);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}



		/**
		 * String[] command = new String[] { settings.getSofficePath(),
		 * String.format("--headless --convert-to %s --outdir %s %s", conversorsource,
		 * tmpdir, importfile.toURI().normalize()) }; Process theProcess =
		 * Runtime.getRuntime().exec(command);
		 *
		 * InputStream inputStream = theProcess.getErrorStream();
		 *
		 * String text = new BufferedReader(new InputStreamReader(inputStream,
		 * StandardCharsets.UTF_8)).lines() .collect(Collectors.joining("\n"));
		 */
        LOG.info("Salida de LibreOffice: " + text);
        // .transferTo(System.out);

        File[] files = new File(tmpdir).listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
        if (files != null && files.length > 0) {
            File path = files[0];
            document = Loader.loadPDF(new RandomAccessReadBufferedFile(path));
        } else {
            LOG.error("No se encontró el PDF convertido en el directorio temporal: " + tmpdir);
        }

        renderer = null;
    }

    @Override
    public void loadDocument(byte[] data) throws Throwable {
		document = Loader.loadPDF(new RandomAccessReadBuffer(data));
        renderer = null;
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
        if (renderer == null) {
            if (document == null) {
                LOG.error("No se puede crear el renderer: document es null");
                return null;
            }
            renderer = new PDFRenderer(document);
        }
        return renderer;
    }


    @Override
    public BufferedImage getPageImage(int page) throws Throwable {

        return getRender().renderImage(page, settings.pDFImgScaleFactor);
    }

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
        File path = new File(settings.getSofficePath());

        return path.exists();
    }
}
