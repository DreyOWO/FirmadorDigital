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

import java.util.List;

import cr.libre.firmador.Settings;
import cr.libre.firmador.cards.CardSignInfo;
import cr.libre.firmador.documents.Document;
import cr.libre.firmador.gui.GUIInterface;
import cr.libre.firmador.remote.FirmadorRemoteDocument;
import cr.libre.firmador.remote.RemoteSignatureValueDTO;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.ws.dto.RemoteDocument;

public interface DocumentSigner {
    void setGui(GUIInterface gui);

    void setSettings(Settings settings);

    DSSDocument sign(Document toSignDocument, CardSignInfo card);

	FirmadorRemoteDocument getTobeSignedRemote(Document toSignDocument, CardSignInfo card);

	RemoteDocument signRemoteDocument(Document toSignDocument, CardSignInfo card, RemoteSignatureValueDTO signature);
    DSSDocument extend(DSSDocument document);

    void setDetached(List<DSSDocument> detacheddocs);
}
