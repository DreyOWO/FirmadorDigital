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

package cr.libre.firmador.remote;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import cr.libre.firmador.documents.Document;
import cr.libre.firmador.documents.MimeTypeDetector;
import cr.libre.firmador.documents.SupportedMimeTypeEnum;
import cr.libre.firmador.previewers.PreviewerInterface;
import cr.libre.firmador.previewers.PreviewerManager;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.ws.dto.ToBeSignedDTO;

public class FirmadorRemoteDocument {
	private String serialnumber;
	private ToBeSignedDTO tobesigned;
	private final UUID documentid;
	private CertificateToken certificate;
	public String documentName;
    public String b64Document;
	@JsonProperty(required = false)
	private String hostname;
	public String b64image;
    public String mimetype;


	public FirmadorRemoteDocument() {
		this.documentid = null;
		this.certificate = null;
	}
    @JsonIgnoreProperties(ignoreUnknown = true)
	public FirmadorRemoteDocument(ToBeSignedDTO tobesigned, Document toSignDocument, CertificateToken certificate) {
		this.tobesigned = tobesigned;
		serialnumber = certificate.getSerialNumber().toString();
		documentid = toSignDocument.getDocumentID();
        SupportedMimeTypeEnum mimetype = MimeTypeDetector.detect(toSignDocument.getPathName());
        this.mimetype = String.valueOf(mimetype);
        toSignDocument.setMimeType(mimetype);
        PreviewerInterface preview = PreviewerManager.getPreviewManager(mimetype);
        try {
            preview.loadDocument(toSignDocument.getPathName());
        } catch (Throwable e) {
            e.printStackTrace();
        }
        toSignDocument.setPreview(preview);
		this.certificate = certificate;
	}

    public void setB64Document(String b64Document) {
        this.b64Document = b64Document;
    }

	@JsonProperty("serialnumber")
	public String getSerialnumber() {
		return serialnumber.toString();
	}

	public void setSerialnumber(String serialnumber) {
		this.serialnumber = serialnumber;
	}

	public ToBeSignedDTO getTobesigned() {
		return tobesigned;
	}

	public void setTobesigned(ToBeSignedDTO tobesigned) {
		this.tobesigned = tobesigned;
	}

	public UUID getDocumentid() {
		return documentid;
	}

	public CertificateToken getCertificate() {
		return certificate;
	}

	@JsonProperty("certificate")
	public String getB464Certificate() {
		byte[] certificateBytes = certificate.getEncoded();
		// Codifica el arreglo de bytes en Base64
		String base64Encoded = Base64.getEncoder().encodeToString(certificateBytes);
		return base64Encoded;
	}

	@JsonProperty("certificate")
	public void setCertificate(byte[] bytescertificate) throws Throwable {
		X509Certificate x509certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
				.generateCertificate(new ByteArrayInputStream(bytescertificate));

		certificate = new CertificateToken(x509certificate);
	}

	public String getDocumentName() {
		return documentName;
	}

	public void setDocumentName(String documentName) {
		this.documentName = documentName;
	}

	public String getHostname() {
        if (this.hostname.isEmpty()) {
            String hostname = System.getenv("INSTANCE_HOSTNAME");
            if (hostname == null) {
                hostname = "localhost";
            }
        }
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getB64image() {
		return b64image;
	}

	public void setB64image(String b64image) {
		this.b64image = b64image;
	}

	public ImageIcon getImageIcon() {
		if (b64image == null) {
			return null;
		}

		ImageIcon image = null;
		String base64Image;
		if (b64image.contains(",")) {
			base64Image = b64image.split(",")[1];
		} else {
			base64Image = b64image;
		}

		try {
			byte[] imageBytes = Base64.getDecoder().decode(base64Image);
			BufferedImage bimage;
			bimage = ImageIO.read(new ByteArrayInputStream(imageBytes));
			image = new ImageIcon(bimage);
		} catch (IOException e) {
			image = null;
		}

		return image;
	}

    public String getMimetype(){
        return mimetype;
    }


}
