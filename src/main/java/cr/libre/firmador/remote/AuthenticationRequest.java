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
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.TimeZone;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.imageio.ImageIO;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.swing.ImageIcon;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cr.libre.firmador.documents.Document;
import cr.libre.firmador.gui.GUIInterface;
import eu.europa.esig.dss.model.x509.CertificateToken;

public class AuthenticationRequest {

	private String serialnumber;
	public String authCode;
	public String b64Salt;
	public String authIdentifier;
	public String authTime;
	public String domain;
	public String b64image;

	@JsonIgnore
	private CertificateToken certificate;
	@JsonIgnore
	private GUIInterface gui;

	public GUIInterface getGui() {
		return gui;
	}

	public void setGui(GUIInterface gui) {
		this.gui = gui;
	}

	public AuthenticationRequest() {

	}

	public String getAuthCode() {
		return authCode;
	}

	public void setAuthCode(String authCode) {
		this.authCode = authCode;
	}

	public String getAuthIdentifier() {
		return authIdentifier;
	}

	public void setAuthIdentifier(String authIdentifier) {
		this.authIdentifier = authIdentifier;
	}

	public String getAuthTime() {
		return authTime;
	}

	public void setAuthTime(String authTime) {
		this.authTime = authTime;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getSerialnumber() {
		return serialnumber;
	}

	public void setSerialnumber(String serialnumber) {
		this.serialnumber = serialnumber;
	}

	private byte[] getSalt() {
		return Base64.getDecoder().decode(this.b64Salt);
	}
	private String getTemplate() throws IOException {
		URL resource = this.getClass().getClassLoader().getResource("xml/authentication_template.xml");
		if (resource != null) {
			InputStream inputStream = resource.openStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

			StringBuilder content = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				content.append(line).append("\n");
			}
			reader.close();
			return content.toString();
		}
		return null;
	}

	private static String deriveKey(String password, byte[] salt, int iterations, int keyLength) throws Exception {
		PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		byte[] key = factory.generateSecret(spec).getEncoded();
		return Base64.getEncoder().encodeToString(key);
	}

	private String generateAuthCode() throws Exception {
		int iterations = 100000; // Número de iteraciones
		int keyLength = 256; // Longitud de la clave en bits
		return deriveKey(this.authCode, getSalt(), iterations, keyLength);

	}

	private String getSigner() throws InvalidNameException {
		String commonName = "";
		LdapName ldapName = new LdapName(this.certificate.getSubject().getPrincipal().getName("RFC1779"));
		for (Rdn rdn : ldapName.getRdns()) {
			if (rdn.getType().equals("CN")) {
				commonName = rdn.getValue().toString();
				break;
			}
		}

		return commonName;
	}

	private String getIdentification() throws InvalidNameException {
		String identification = "";
		LdapName ldapName = new LdapName(this.certificate.getSubject().getPrincipal().getName("RFC1779"));
		for (Rdn rdn : ldapName.getRdns()) {
			if (rdn.getType().equals("OID.2.5.4.5")) {
				identification = rdn.getValue().toString();
				break;
			}
		}
		return identification;

	}

	private byte[] buildDocumentContent() throws Exception {
		String template = getTemplate();
		SimpleDateFormat date = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		date.setTimeZone(TimeZone.getTimeZone("America/Costa_Rica"));
		template = template.replace("{REQUEST_DATE}", this.getAuthTime()).replace("{DOMAIN}", this.getDomain())
				.replace("{TRANSACTION}", this.getAuthIdentifier())
				.replace("{SIGNER}", getSigner()).replace("{IDENTIFICATION}", this.getIdentification())
				.replace("{AUTHCODE}", this.generateAuthCode())
				.replace("{EMITION_DATE}", date.format(new Date()));
		return template.getBytes();
	}

	public Document getSignDocument() throws Throwable {

		Document doc = new Document(this.gui, buildDocumentContent(), "autorizacion.xml", 0);
		return doc;
		
	}

	public CertificateToken getCertificate() {
		return certificate;
	}

	public void setCertificate(CertificateToken certificate) {
		this.certificate = certificate;
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

	public String getShortAuthCode() {
		if (this.authCode.length() > 6) {
			return this.authCode.substring(this.authCode.length() - 6);
		}
		return null;
	}
}
