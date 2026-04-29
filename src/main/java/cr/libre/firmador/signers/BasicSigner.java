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

import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.cards.CardSignInfo;
import cr.libre.firmador.gui.GUIInterface;
import cr.libre.firmador.gui.swing.RequestPinWindowRemote;
import eu.europa.esig.dss.alert.exception.AlertException;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.ws.dto.SignatureValueDTO;
import eu.europa.esig.dss.ws.dto.ToBeSignedDTO;

public class BasicSigner extends CRSigner {
	public BasicSigner(GUIInterface gui) {
		super(gui);
	}

	public boolean getPin(CardSignInfo card) {
		RequestPinWindowRemote requestPinWindow = new RequestPinWindowRemote();
		int action = requestPinWindow.showandwait();
		boolean update = false;
		if (action == 0) {
			requestPinWindow.getCardInfo();
			update = true;
		}
		return update;
	}

	public SignatureValueDTO sign(CardSignInfo card, ToBeSignedDTO tobesigned) {
		SignatureTokenConnection token;
		try {
			token = getSignatureConnection(card);
		} catch (DSSException | AlertException | Error e) {
			LOG.error("Error al conectar con el dispositivo", e);
			gui.showError(FirmadorUtils.getRootCause(e));
			return null;
		}
		if (token == null)
			return null;
		DSSPrivateKeyEntry privateKey = null;
		try {
			privateKey = getPrivateKey(token);
			gui.nextStep(MessageUtils.t("signers_getting_key_handler"));
		} catch (Exception e) {
			LOG.error("Error al acceder al objeto de llave del dispositivo", e);
			gui.showError(FirmadorUtils.getRootCause(e));
			return null;
		}

		ToBeSigned dataToSign = new ToBeSigned(tobesigned.getBytes());
		SignatureValue signatureValue = token.sign(dataToSign, SignatureAlgorithm.RSA_SHA256, privateKey);
		SignatureValueDTO rvalue = new SignatureValueDTO(signatureValue.getAlgorithm(), signatureValue.getValue());
		return rvalue;
	}
}
