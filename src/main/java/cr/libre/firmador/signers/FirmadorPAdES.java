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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.Settings;
import cr.libre.firmador.cards.CardSignInfo;
import cr.libre.firmador.documents.Document;
import cr.libre.firmador.gui.GUIInterface;
import cr.libre.firmador.remote.FirmadorRemoteDocument;
import cr.libre.firmador.remote.RemoteSignatureValueDTO;
import eu.europa.esig.dss.alert.exception.AlertException;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignerTextPosition;
import eu.europa.esig.dss.enumerations.VisualSignatureRotation;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.model.x509.X500PrincipalHelper;
import eu.europa.esig.dss.pades.DSSJavaFont;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.PAdESTimestampParameters;
import eu.europa.esig.dss.pades.SignatureFieldParameters;
import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pades.SignatureImageTextParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.pdf.pdfbox.PdfBoxNativeObjectFactory;
import eu.europa.esig.dss.service.tsp.OnlineTSPSource;
import eu.europa.esig.dss.spi.DSSASN1Utils;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.spi.validation.CertificateVerifier;
import eu.europa.esig.dss.ws.dto.RemoteCertificate;
import eu.europa.esig.dss.ws.dto.RemoteColor;
import eu.europa.esig.dss.ws.dto.RemoteDocument;
import eu.europa.esig.dss.ws.dto.ToBeSignedDTO;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.ws.signature.common.RemoteDocumentSignatureServiceImpl;
import eu.europa.esig.dss.ws.signature.dto.parameters.RemoteSignatureFieldParameters;
import eu.europa.esig.dss.ws.signature.dto.parameters.RemoteSignatureImageParameters;
import eu.europa.esig.dss.ws.signature.dto.parameters.RemoteSignatureImageTextParameters;
import eu.europa.esig.dss.ws.signature.dto.parameters.RemoteSignatureParameters;

import javax.imageio.ImageIO;

public class FirmadorPAdES extends CRSigner implements DocumentSigner {
    final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    PAdESSignatureParameters parameters;

    public FirmadorPAdES(GUIInterface gui) {
        super(gui);
    }

    public DSSDocument sign(DSSDocument toSignDocument, CardSignInfo card, Settings docSettings, Document document) {

        CertificateVerifier verifier = null;
        PAdESService service = null;
        parameters = new PAdESSignatureParameters();
        SignatureValue signatureValue = null;
        DSSDocument signedDocument = null;
        SignatureTokenConnection token = null;
        gui.nextStep(MessageUtils.t("signers_getting_verification_services"));
        @SuppressWarnings("unused")
        String image = docSettings.getImage(); //FIXME it seems unused, fetched later via docSettings.image directly 
        try {
            token = getSignatureConnection(card);
        } catch (DSSException|AlertException|Error e) {
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
        try {
            gui.nextStep(MessageUtils.t("signers_getting_card_certificates"));
            CertificateToken certificate = privateKey.getCertificate();
            parameters.setSignatureLevel(docSettings.getPAdESLevel());
            parameters.setAppName("Firmador " + settings.getVersion() + ", https://firmador.libre.cr");
            parameters.setContentSize(13312);
            parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);
            parameters.setSigningCertificate(certificate);
            if (!docSettings.reason.isEmpty())
                parameters.setReason(docSettings.reason);
            if (!docSettings.place.isEmpty())
                parameters.setLocation(docSettings.place);
            if (!docSettings.contact.isEmpty())
                parameters.setContactInfo(docSettings.contact);
            OnlineTSPSource onlineTSPSource = new OnlineTSPSource(TSA_URL);
            gui.nextStep(MessageUtils.t("signers_getting_tsp_services"));
            verifier = this.getCertificateVerifier(certificate);
            service = new PAdESService(verifier);
            service.setPdfObjFactory(new PdfBoxNativeObjectFactory());
            service.setTspSource(onlineTSPSource);
			Date date = new Date();
            if (docSettings.isVisibleSignature)
				appendVisibleSignature(certificate, docSettings, document);
            gui.nextStep(MessageUtils.t("signers_adding_graphic_representation"));
            parameters.bLevel().setSigningDate(date);

            ToBeSigned dataToSign = service.getDataToSign(toSignDocument, parameters);

            gui.nextStep(MessageUtils.t("signers_getting_data_structure"));
            signatureValue = token.sign(dataToSign, parameters.getDigestAlgorithm(), privateKey);
        } catch (DSSException|AlertException|Error e) {
            if (FirmadorUtils.getRootCause(e).getLocalizedMessage().equals("The new signature field position overlaps with an existing annotation!")) {
                LOG.error("Error al firmar (traslape de firma)", e);
                e.printStackTrace();
                gui.showMessage(MessageUtils.t("signers_signature_overlap"));
                return null;
            } else {
                LOG.error("Error al solicitar firma al dispositivo", e);
                gui.showError(FirmadorUtils.getRootCause(e));
            }
        } catch (IllegalArgumentException e) {
            if (FirmadorUtils.getRootCause(e).getMessage().contains("is expired")) {
                LOG.warn("El certificado seleccionado para firmar ha vencido", e);
                e.printStackTrace();
                gui.showMessage(MessageUtils.t("signers_expired_certificate"));
                return null;
            } else {
                LOG.error("Error al solicitar firma al dispositivo", e);
                gui.showError(FirmadorUtils.getRootCause(e));
            }
        }

        try {
            gui.nextStep(MessageUtils.t("signers_signing_data_structure"));
            signedDocument = service.signDocument(toSignDocument, parameters, signatureValue);

            gui.nextStep(MessageUtils.t("signers_document_sign_complete"));
        } catch (Exception e) {
            LOG.error("Error al procesar información de firma avanzada", e);
            e.printStackTrace();
            gui.showMessage(String.format(MessageUtils.t("signers_not_possible_to_add_timestamp_sign"), FirmadorUtils.getRootCause(e)));
            parameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);
            try {
                signedDocument = service.signDocument(toSignDocument, parameters, signatureValue);
            } catch (Exception ex) {
                LOG.error("Error al procesar información de firma avanzada en nivel fallback (sin Internet) a AdES-B", e);
                gui.showError(FirmadorUtils.getRootCause(e));
            }
        }
        return signedDocument;
    }

    public DSSDocument extend(DSSDocument document) {
        PAdESSignatureParameters parameters = new PAdESSignatureParameters();
        parameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_LTA);
		parameters.setContentSize(3072);
        gui.nextStep(MessageUtils.t("signers_extending_document_with_timestamp"));


        CertificateVerifier verifier = this.getCertificateVerifier();
        PAdESService service = new PAdESService(verifier);
        OnlineTSPSource onlineTSPSource = new OnlineTSPSource(TSA_URL);
        service.setTspSource(onlineTSPSource);
        DSSDocument extendedDocument = null;

        try {
            extendedDocument = service.extendDocument(document, parameters);
        } catch (Exception e) {
            LOG.error("Error al procesar información para al ampliar el nivel de firma avanzada a LTA (sello adicional)", e);
            e.printStackTrace();
            gui.showMessage(String.format(MessageUtils.t("signers_not_possible_to_add_timestamp_extend"), FirmadorUtils.getRootCause(e)));
            gui.nextStep(MessageUtils.t("signers_additional_stamp_completed"));
        }
        if (extendedDocument != null)
            gui.nextStep(MessageUtils.t("signers_additional_stamp_completed"));
        return extendedDocument;
    }

    public DSSDocument timestamp(DSSDocument documentToTimestamp, Boolean visibleTimestamp) {
        CertificateVerifier verifier = this.getCertificateVerifier();
        PAdESService service = new PAdESService(verifier);
        DSSDocument timestampedDocument = null;
        try {
            OnlineTSPSource onlineTSPSource = new OnlineTSPSource(TSA_URL);
            service.setTspSource(onlineTSPSource);
        } catch (DSSException|Error e) {
            LOG.error("Error al preparar el servicio de sello de tiempo)", e);
            gui.showError(FirmadorUtils.getRootCause(e));
        }
        try {
            PAdESTimestampParameters timestampParameters = new PAdESTimestampParameters();
            if (visibleTimestamp) {
                SignatureImageParameters imageParameters = new SignatureImageParameters();
                imageParameters.getFieldParameters().setRotation(VisualSignatureRotation.AUTOMATIC);
                imageParameters.getFieldParameters().setOriginX(0);
                imageParameters.getFieldParameters().setOriginY(0);
                SignatureImageTextParameters textParameters = new SignatureImageTextParameters();
                textParameters.setFont(new DSSJavaFont(new Font(settings.getFontName(settings.font, true), settings.getFontStyle(settings.font), settings.fontSize)));
                SimpleDateFormat date = new SimpleDateFormat(settings.getDateFormat());
                date.setTimeZone(TimeZone.getTimeZone("America/Costa_Rica"));
                textParameters.setText(String.format(MessageUtils.t("signers_info_timestamp_included"), date.format(new Date())));
                textParameters.setTextColor(settings.getFontColor());
                textParameters.setBackgroundColor(settings.getBackgroundColor());
                textParameters.setSignerTextPosition(SignerTextPosition.RIGHT);
                imageParameters.setTextParameters(textParameters);
                imageParameters.getFieldParameters().setPage(1);
                timestampParameters.setImageParameters(imageParameters);
                timestampParameters.setAppName("Firmador " + settings.getVersion() + ", https://firmador.libre.cr");
            }
            timestampedDocument = service.timestamp(documentToTimestamp, timestampParameters);
        } catch (Exception e) {
            LOG.error("Error al procesar información para al agregar un sello de tiempo independiente)", e);
            gui.showError(FirmadorUtils.getRootCause(e));
        }
        return timestampedDocument;
    }


	private void appendVisibleSignature(CertificateToken certificate, Settings docSettings, Document toSignDocument) {
        SignatureImageParameters imageParameters = new SignatureImageParameters();
        imageParameters.getFieldParameters().setRotation(VisualSignatureRotation.AUTOMATIC);
        SignatureFieldParameters fparamet = imageParameters.getFieldParameters();
        fparamet.setOriginX(docSettings.signX);
        fparamet.setOriginY(docSettings.signY);
        //fparamet.setWidth(width);
        //fparamet.setHeight(height);


        if (docSettings.image != null && !docSettings.image.trim().isEmpty()) {
            try {
                URI imageUri = new URI(docSettings.image);
                BufferedImage originalImage = ImageIO.read(imageUri.toURL());

                // Escalar a tamaño fijo
                int dpi = 21;
                int targetWidth = (int) Math.round(settings.signImageWidth * 72.0 / dpi);
                int targetHeight = (int) Math.round(settings.signImageHeight * 72.0 / dpi);
                Image scaledImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);

                // Convertir a BufferedImage
                BufferedImage resizedBuffered = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = resizedBuffered.createGraphics();
                g2d.drawImage(scaledImage, 0, 0, null);
                g2d.dispose();

                // Convertir a bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(resizedBuffered, "png", baos);
                baos.flush();

                // Pasar imagen escalada a DSS
                imageParameters.setImage(new InMemoryDocument(baos.toByteArray()));

            } catch (Exception e) {
                LOG.error("Error al procesar la imagen para la representación visual de firma", e);
                e.printStackTrace();
            }
        }

        if (!"ONLY IMAGE".equals(settings.fontAlignment)) {
            SignatureImageTextParameters textParameters = new SignatureImageTextParameters();

            textParameters.setFont(new DSSJavaFont(new Font(settings.getFontName(settings.font, true), settings.getFontStyle(settings.font), settings.fontSize)));

            String text = getSignatureText(certificate, docSettings);
            textParameters.setText(text);
            textParameters.setTextColor(settings.getFontColor());
            textParameters.setBackgroundColor(settings.getBackgroundColor());
            textParameters.setSignerTextPosition(settings.getFontAlignment());

            imageParameters.setTextParameters(textParameters);
        }

        // imageParameters.setImageScaling(ImageScaling.STRETCH);
        imageParameters.getFieldParameters().setPage(getPageNumber(toSignDocument, docSettings));
        parameters.setImageParameters(imageParameters);
    }

    public int getPageNumber(Document toSignDocument, Settings settings) {
        if (settings.pageNumber > 0) {
            return settings.pageNumber;
        }else{
            int pages = toSignDocument.getNumberOfPages();
            if (settings.pageNumber == 0) {
                return pages;
            }
            if ((pages+1) + settings.pageNumber <= 0) {
                return pages;
            }
            return (pages+1) + settings.pageNumber;
        }
    }

    public DSSDocument sign(Document toSignDocument, CardSignInfo card) {
        DSSDocument signeddocument = sign(toSignDocument.getDSSDocument(), card, toSignDocument.getSettings(), toSignDocument);
        return signeddocument;
    }

    @Override
    public void setDetached(List<DSSDocument> detacheddocs) {
        // TODO Auto-generated method stub

    }

	private String getSignatureText(CertificateToken certificate, Settings docSettings) {
		Date date = new Date();
		String cn = DSSASN1Utils.getSubjectCommonName(certificate);
		X500PrincipalHelper subject = certificate.getSubject();
		String o = DSSASN1Utils.extractAttributeFromX500Principal(BCStyle.O, subject);
		String sn = DSSASN1Utils.extractAttributeFromX500Principal(BCStyle.SERIALNUMBER, subject);
        SimpleDateFormat fecha = new SimpleDateFormat(settings.getDateFormat());
        if (docSettings.dateFormat != null && !docSettings.dateFormat.trim().isEmpty()){
            fecha = new SimpleDateFormat(docSettings.dateFormat);
        }
        fecha.setTimeZone(TimeZone.getTimeZone("America/Costa_Rica"));
        String additionalText = "";
        if (!docSettings.hideSignatureAdvice) {
            additionalText = settings.getDefaultSignMessage();
            if (docSettings.defaultSignMessage != null && !docSettings.defaultSignMessage.trim().isEmpty()) {
                additionalText = docSettings.defaultSignMessage;
            }
        }
		Boolean hasReason = false;
		Boolean hasLocation = false;
		if (docSettings.reason != null && !docSettings.reason.trim().isEmpty()) {
			hasReason = true;
			additionalText = MessageUtils.t("signers_visible_signature_reason") + " " + docSettings.reason + "\n";
		}
		if (docSettings.place != null && !docSettings.place.trim().isEmpty()) {
			hasLocation = true;
			if (hasReason)
				additionalText += MessageUtils.t("signers_visible_signature_place") + " " + docSettings.place;
			else
				additionalText = MessageUtils.t("signers_visible_signature_place") + " " + docSettings.place;
		}
		if (docSettings.contact != null && !docSettings.contact.trim().isEmpty()) {
			if (hasReason || hasLocation)
				additionalText += "  " + MessageUtils.t("signers_visible_signature_contact") + " "
						+ docSettings.contact;
			else
				additionalText = MessageUtils.t("signers_visible_signature_contact") + " " + docSettings.contact;
		}
		return cn + "\n" + o + ", " + sn + ".\n" + MessageUtils.t("signers_visible_signature_declared_date") + " "
				+ fecha.format(date) + "\n" + additionalText.replaceAll("\t", " ");
	}

	private RemoteDocument getRemoteImage(Settings docSettings) {
		byte[] img = docSettings.getByteImage();
		if (img == null)
			return null;

		return new RemoteDocument(img, "image");
	}

	private RemoteDocument getFont(Settings docSettings) {
		RemoteDocument fontdoc = null;
		if (docSettings.font.startsWith("base64:")) {
			String[] structure = docSettings.font.split(":");
			byte[] decodedBytes = Base64.getDecoder().decode(structure[2]);
			fontdoc = new RemoteDocument(decodedBytes, structure[1]);
		} else if (docSettings.font.startsWith("file:")) {
			String[] structure = docSettings.font.split(":");
			File f = new File(structure[2]);
			byte[] decodedBytes;
			try {
				decodedBytes = FileUtils.readFileToByteArray(f);
				fontdoc = new RemoteDocument(decodedBytes, structure[1]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return fontdoc;
	}

	private RemoteSignatureImageParameters getRemoteSignatureImageParameters(Document toSignDocument,
			CardSignInfo card) {
		Settings docSettings = toSignDocument.getSettings();
		if (!docSettings.isVisibleSignature)
			return null;
		// OK doing image signature
		RemoteSignatureImageParameters remoteimgparams = new RemoteSignatureImageParameters();
		RemoteSignatureFieldParameters fieldparameters = remoteimgparams.getFieldParameters();
		if (fieldparameters == null)
			fieldparameters = new RemoteSignatureFieldParameters();

		fieldparameters.setRotation(VisualSignatureRotation.AUTOMATIC);
		fieldparameters.setOriginX((float) docSettings.signX);
		fieldparameters.setOriginY((float) docSettings.signY);
		remoteimgparams.setFieldParameters(fieldparameters);
		if (!docSettings.onlyimage) {
			RemoteSignatureImageTextParameters textParameters = new RemoteSignatureImageTextParameters();
			String text = getSignatureText(card.getCertificate(), docSettings);
			textParameters.setText(text);

			Color textcolor = docSettings.getFontColor();
			Color backgroundcolor = docSettings.getBackgroundColor();
			RemoteColor remotetextcolor = new RemoteColor(textcolor.getRed(), textcolor.getGreen(), textcolor.getBlue(),
					textcolor.getAlpha());
			RemoteColor remotebackgroundcolor = new RemoteColor(backgroundcolor.getRed(), backgroundcolor.getGreen(),
					backgroundcolor.getBlue(), backgroundcolor.getAlpha());
			textParameters.setTextColor(remotetextcolor);
			textParameters.setBackgroundColor(remotebackgroundcolor);
			textParameters.setSignerTextPosition(docSettings.getFontAlignment());
			textParameters.setSize(docSettings.fontSize);

			RemoteDocument font = getFont(docSettings);
			if (font != null)
				textParameters.setFont(font);
			remoteimgparams.setTextParameters(textParameters);
		}
		RemoteDocument img = getRemoteImage(docSettings);
		if (img != null) {
			remoteimgparams.setImage(img);
		}
		// imageParameters.setImageScaling(ImageScaling.STRETCH);
		remoteimgparams.getFieldParameters().setPage(docSettings.pageNumber);
		return remoteimgparams;
	}

	// RemoteSignatureImageParameters
	private RemoteSignatureParameters getRemoteSignatureParameters(Document toSignDocument, CardSignInfo card) {
		RemoteSignatureParameters remoteParameters = new RemoteSignatureParameters();
		Settings docSettings = toSignDocument.getSettings();
		SignatureLevel padeslevel = docSettings.getPAdESLevel();
		RemoteCertificate remoteCertificate = new RemoteCertificate(card.getCertificate().getEncoded());
		remoteParameters.setSigningCertificate(remoteCertificate);
		remoteParameters.setDigestAlgorithm(DigestAlgorithm.SHA256);
		remoteParameters.setSignatureLevel(padeslevel);
		return remoteParameters;
	}

	@Override
	public FirmadorRemoteDocument getTobeSignedRemote(Document toSignDocument, CardSignInfo card) {

		CertificateVerifier verifier = this.getCertificateVerifier();
		PAdESService service = new PAdESService(verifier);
		OnlineTSPSource onlineTSPSource = new OnlineTSPSource(TSA_URL);
		service.setTspSource(onlineTSPSource);

		RemoteDocumentSignatureServiceImpl remoteservice = new RemoteDocumentSignatureServiceImpl();
		RemoteSignatureParameters remoteParameters = getRemoteSignatureParameters(toSignDocument, card);
		RemoteDocument toRemoteSignDocument = toSignDocument.getRemoteDocument();
		RemoteSignatureImageParameters imgparameters = getRemoteSignatureImageParameters(toSignDocument, card);
		if (imgparameters != null) {
			remoteParameters.setImageParameters(imgparameters);
		}
		toSignDocument.setRemoteParameters(remoteParameters);
		remoteservice.setPadesService(service);
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
		PAdESService service = new PAdESService(verifier);
		OnlineTSPSource onlineTSPSource = new OnlineTSPSource(TSA_URL);
		service.setTspSource(onlineTSPSource);

		RemoteDocumentSignatureServiceImpl remoteservice = new RemoteDocumentSignatureServiceImpl();
		RemoteSignatureParameters remoteParameters = toSignDocument.getRemoteParameters();
		RemoteDocument toRemoteSignDocument = toSignDocument.getRemoteDocument();

		remoteservice.setPadesService(service);
		RemoteDocument doc = remoteservice.signDocument(toRemoteSignDocument, remoteParameters,
				signature.getSignature());
		return doc;
	}

}
