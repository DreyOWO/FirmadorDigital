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

import java.lang.invoke.MethodHandles;
import java.util.List;

import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cr.libre.firmador.gui.GUIInterface;

public class ProcessDocumentWorker extends SwingWorker<Void, Void> {
	final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	@SuppressWarnings("unused")
	private GUIInterface gui; //FIXME is this actally used here?
	private List<Document> documents;
	private DocumentManager docmanager;

	public ProcessDocumentWorker(List<Document> documents, DocumentManager docmanager, GUIInterface gui) {
		this.documents = documents;
		this.gui = gui;
		this.docmanager = docmanager;
	}

	@Override
	protected Void doInBackground() throws Exception {
        try{
            for (Document document : documents) {
                try {
                    document.validate();
                } catch (Throwable e) {
                    LOG.error("Document validate with errors: " + document.getName());
                    LOG.error("Process Worker: " + e.getMessage(), e);

                } finally {
                    LOG.info("Document validate loaded: " + document.getName());

                }
                try {
                    document.loadPreview();
                } catch (Throwable e) {
                    LOG.error("Document preview with errors: " + document.getName());
                    LOG.error("Preview Worker: " + e.getMessage(), e);

                } finally {
                    LOG.info("Document preview loaded: " + document.getName());

                }
            }
        } catch (Throwable e) {
            LOG.error("Process Worker: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void done() {
        LOG.info("ProcessDocumentWorker: done() ejecutado");
        docmanager.endProcessDocument();
        super.done();
    }

}
