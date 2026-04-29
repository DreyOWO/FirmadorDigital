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

import java.lang.invoke.MethodHandles;


import java.util.List;

import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.Settings;
import cr.libre.firmador.SettingsManager;
import cr.libre.firmador.cards.CardSignInfo;
import cr.libre.firmador.documents.Document;
import cr.libre.firmador.gui.GUIInterface;
import cr.libre.firmador.remote.FirmadorRemoteDocument;
import cr.libre.firmador.remote.RemoteSignatureValueDTO;
import eu.europa.esig.dss.alert.exception.AlertException;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;

import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.model.x509.CertificateToken;

import eu.europa.esig.dss.jades.JAdESSignatureParameters;


import eu.europa.esig.dss.jades.signature.JAdESService;

import eu.europa.esig.dss.service.tsp.OnlineTSPSource;

import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.spi.validation.CertificateVerifier;

import eu.europa.esig.dss.ws.dto.RemoteCertificate;
import eu.europa.esig.dss.ws.dto.RemoteDocument;
import eu.europa.esig.dss.ws.dto.ToBeSignedDTO;
import eu.europa.esig.dss.ws.signature.common.RemoteDocumentSignatureServiceImpl;
import eu.europa.esig.dss.ws.signature.dto.parameters.RemoteSignatureParameters;
import eu.europa.esig.dss.enumerations.JWSSerializationType;
import eu.europa.esig.dss.enumerations.SigDMechanism;
//import eu.europa.esig.dss.validation.SignedDocumentValidator; // Electronic receipts v4.4 proposal
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirmadorJAdES extends CRSigner implements DocumentSigner {
    final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    JAdESSignatureParameters parameters;

    //private Settings settings;


	public FirmadorJAdES(GUIInterface gui) {
        super(gui);
        settings = SettingsManager.getInstance().getAndCreateSettings();

    }

    public DSSDocument sign(DSSDocument toSignDocument, CardSignInfo card, Settings settings) {
        JAdESService service = null;

        parameters = new JAdESSignatureParameters();

        SignatureValue signatureValue = null;
        DSSDocument signedDocument = null;
        SignatureTokenConnection token = null;
        gui.nextStep(MessageUtils.t("signers_getting_verification_services"));

        try {
            token = getSignatureConnection(card);
        } catch (DSSException|AlertException|Error e) {
            LOG.error("Error al conectar con el dispositivo", e);
            gui.showError(FirmadorUtils.getRootCause(e));
            return null;
        }
        DSSPrivateKeyEntry privateKey = null;
        try {
            privateKey = getPrivateKey(token);
            gui.nextStep(MessageUtils.t("signers_getting_key_handler"));
        } catch (Exception e) {
            LOG.error("Error al acceder al objeto de llave del dispositivo", e);
            gui.showError(FirmadorUtils.getRootCause(e));
            return null;
        }
        try {
            gui.nextStep(MessageUtils.t("signers_getting_card_certificates"));
            CertificateToken certificate = privateKey.getCertificate();
            parameters.setJwsSerializationType(JWSSerializationType.JSON_SERIALIZATION);
            parameters.setSignatureLevel(settings.getJAdESLevel());
            //parameters.setSignatureLevel(SignatureLevel.JAdES_BASELINE_B);

            parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);
            parameters.setSigningCertificate(certificate);
            parameters.setSigningCertificateDigestMethod(parameters.getDigestAlgorithm());


            OnlineTSPSource onlineTSPSource = new OnlineTSPSource(TSA_URL);
            gui.nextStep(MessageUtils.t("signers_getting_tsp_services"));

			service = new JAdESService(this.getCertificateVerifier(certificate));
            service.setTspSource(onlineTSPSource);
            parameters.setBase64UrlEncodedPayload(true);

            parameters.setSignaturePackaging(SignaturePackaging.ENVELOPING);
            parameters.setSigDMechanism(SigDMechanism.NO_SIG_D);

            ToBeSigned dataToSign = service.getDataToSign(toSignDocument, parameters);
            //ToBeSigned dataToSign = service.getDataToBeCounterSigned(toSignDocument, parameters); // Electronic receipts v4.4 proposal
            gui.nextStep(MessageUtils.t("signers_getting_data_structure"));
            signatureValue = token.sign(dataToSign, parameters.getDigestAlgorithm(), privateKey);
        } catch (DSSException|Error e) {
            LOG.error("Error al solicitar firma al dispositivo", e);
            gui.showError(FirmadorUtils.getRootCause(e));
        }

        try {
            gui.nextStep(MessageUtils.t("signers_signing_data_structure"));
            signedDocument = service.signDocument(toSignDocument, parameters, signatureValue);
            //signedDocument = service.counterSignSignature(toSignDocument, parameters, signatureValue); // Electronic receipts v4.4 proposal
            gui.nextStep(MessageUtils.t("signers_document_sign_complete"));
        } catch (Exception e) {
            LOG.error("Error al procesar información de firma avanzada", e);
            e.printStackTrace();
            gui.showMessage(String.format(MessageUtils.t("signers_not_possible_to_add_timestamp_sign"), FirmadorUtils.getRootCause(e)));
            parameters.setSignatureLevel(SignatureLevel.JAdES_BASELINE_B);
            try {
                signedDocument = service.signDocument(toSignDocument, parameters, signatureValue);
                //signedDocument = service.counterSignSignature(toSignDocument, parameters, signatureValue); // Electronic receipts v4.4 proposal
            } catch (Exception ex) {
                LOG.error("Error al procesar información de firma avanzada en nivel fallback (sin Internet) a AdES-B", e);
                gui.showError(FirmadorUtils.getRootCause(e));
            }
        }
        return signedDocument;
    }

    public DSSDocument extend(DSSDocument document) {
        JAdESSignatureParameters parameters = new JAdESSignatureParameters();
        parameters.setSignatureLevel(SignatureLevel.JAdES_BASELINE_LTA);

        parameters.setJwsSerializationType(JWSSerializationType.JSON_SERIALIZATION);
        CertificateVerifier verifier = this.getCertificateVerifier();
        JAdESService service = new JAdESService(verifier);
        OnlineTSPSource onlineTSPSource = new OnlineTSPSource(TSA_URL);
        service.setTspSource(onlineTSPSource);
        DSSDocument extendedDocument = null;
        try {
            extendedDocument = service.extendDocument(document, parameters);
        } catch (Exception e) {
            LOG.error("Error al procesar información para al ampliar el nivel de firma avanzada a LTA (sello adicional)", e);
            e.printStackTrace();
            gui.showMessage(String.format(MessageUtils.t("signers_not_possible_to_add_timestamp_extend"), FirmadorUtils.getRootCause(e)));
        }
        return extendedDocument;
    }

    public DSSDocument sign(Document toSignDocument, CardSignInfo card) {
        DSSDocument doc = sign(toSignDocument.getDSSDocument(), card, toSignDocument.getSettings());
        return doc;
    }

    @Override
    public void setDetached(List<DSSDocument> detacheddocs) {
        // TODO Auto-generated method stub

    }

	private RemoteSignatureParameters getRemoteSignatureParameters(Document toSignDocument, CardSignInfo card) {
		RemoteSignatureParameters remoteParameters = new RemoteSignatureParameters();
		Settings docSettings = toSignDocument.getSettings();
		SignatureLevel jadeslevel = docSettings.getJAdESLevel();
		RemoteCertificate remoteCertificate = new RemoteCertificate(card.getCertificate().getEncoded());
		remoteParameters.setSigningCertificate(remoteCertificate);
		remoteParameters.setDigestAlgorithm(DigestAlgorithm.SHA256);
		remoteParameters.setSignatureLevel(jadeslevel);
		return remoteParameters;
	}

	@Override
	public FirmadorRemoteDocument getTobeSignedRemote(Document toSignDocument, CardSignInfo card) {
		CertificateVerifier verifier = this.getCertificateVerifier();
		JAdESService service = new JAdESService(verifier);
		OnlineTSPSource onlineTSPSource = new OnlineTSPSource(TSA_URL);
		service.setTspSource(onlineTSPSource);

		RemoteDocumentSignatureServiceImpl remoteservice = new RemoteDocumentSignatureServiceImpl();
		RemoteSignatureParameters remoteParameters = getRemoteSignatureParameters(toSignDocument, card);
		RemoteDocument toRemoteSignDocument = toSignDocument.getRemoteDocument();

		toSignDocument.setRemoteParameters(remoteParameters);
		remoteservice.setJadesService(service);
		ToBeSignedDTO tobesigneddto = remoteservice.getDataToSign(toRemoteSignDocument, remoteParameters);
		FirmadorRemoteDocument response = new FirmadorRemoteDocument(tobesigneddto, toSignDocument,
				card.getCertificate());
		response.setDocumentName(toSignDocument.getName());
		return response;
	}

	@Override
	public RemoteDocument signRemoteDocument(Document toSignDocument, CardSignInfo card,
			RemoteSignatureValueDTO signature) {
		CertificateVerifier verifier = this.getCertificateVerifier();
		JAdESService service = new JAdESService(verifier);
		OnlineTSPSource onlineTSPSource = new OnlineTSPSource(TSA_URL);
		service.setTspSource(onlineTSPSource);

		RemoteDocumentSignatureServiceImpl remoteservice = new RemoteDocumentSignatureServiceImpl();
		RemoteSignatureParameters remoteParameters = toSignDocument.getRemoteParameters();
		RemoteDocument toRemoteSignDocument = toSignDocument.getRemoteDocument();

		remoteservice.setJadesService(service);
		RemoteDocument doc = remoteservice.signDocument(toRemoteSignDocument, remoteParameters,
				signature.getSignature());
		return doc;
	}

}
