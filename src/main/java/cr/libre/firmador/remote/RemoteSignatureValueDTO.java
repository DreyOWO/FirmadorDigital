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

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.ws.dto.SignatureValueDTO;

public class RemoteSignatureValueDTO {
	public SignatureValueDTO signature;
	public final UUID documentid;
	public CertificateToken certificate;

	public RemoteSignatureValueDTO() {
		documentid = null;
	}

    @SuppressWarnings("this-escape")
	public RemoteSignatureValueDTO(FirmadorRemoteDocument docrequest, SignatureValueDTO signature) {
		this.setSignature(signature);
		this.documentid = docrequest.getDocumentid();
		this.certificate = docrequest.getCertificate();
	}

	@JsonProperty("certificate")
	public String getB464Certificate() {
		byte[] certificateBytes = certificate.getEncoded();
		String base64Encoded = Base64.getEncoder().encodeToString(certificateBytes);
		return base64Encoded;
	}

	@JsonProperty("certificate")
	public void setCertificate(byte[] bytescertificate) throws Throwable {
		X509Certificate x509certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
				.generateCertificate(new ByteArrayInputStream(bytescertificate));

		certificate = new CertificateToken(x509certificate);
	}

	public SignatureValueDTO getSignature() {
		return signature;
	}

	public void setSignature(SignatureValueDTO signature) {
		this.signature = signature;
	}

	public UUID getDocumentid() {
		return documentid;
	}

	public CertificateToken getCertificate() {
		return certificate;
	}
}
