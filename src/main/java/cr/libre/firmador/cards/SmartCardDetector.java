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

package cr.libre.firmador.cards;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Semaphore;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cr.libre.firmador.ConfigListener;
import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.Settings;
import cr.libre.firmador.SettingsManager;
import cr.libre.firmador.signers.CRSigner;

public class SmartCardDetector extends Thread implements ConfigListener {
    final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Semaphore waitforrequest = new Semaphore(1);
    private boolean stop = false;

    protected Settings settings;
    @SuppressWarnings("unused")
    private String lib; //FIXME is this even used here?

    private PKCS11Manager pkcs11manager = null;
    private List<CardSignInfo> cardinfo = null;
    private List<SmartCardListener> listeners;

    public SmartCardDetector() {
        settings = SettingsManager.getInstance().getAndCreateSettings();
        listeners = new ArrayList<SmartCardListener>();
        cardinfo = new ArrayList<CardSignInfo>();
        // settings.addListener(this);
    }

    public void restoreSessions() {
        pkcs11manager.logoutAndCloseAllSessions(cardinfo.get(0).getSlotID());
    }

    public void login(CardSignInfo card) {
        try {
            pkcs11manager.login(card.getSlotID(), card.getPin());
        } catch (Throwable e) {
            LOG.error("Error al iniciar sesión en el dispositivo", e);
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            while (!this.stop) {
                this.waitforrequest.acquire(); // first time acquire and don't lock
                this.findCertificates(true);
            }
        } catch (Throwable e) {
            this.stop = true;
            e.printStackTrace();
        }
    }

    public void updateLib() {
        lib = CRSigner.getPkcs11Lib();
    }

    private void findCertificates(boolean includepkcs12) throws Throwable {
        try {
            cardinfo = readListSmartCard();
        } catch (Throwable e) {
            LOG.info("readListSmartCard thrown", e);
            if (e.getMessage().toString().contains("incompatible architecture")) {
                throw new UnsupportedArchitectureException(MessageUtils.t("smartcardDetector_unsupported_arch"), e);
            }
            cardinfo = new ArrayList<CardSignInfo>();
        }
        if (includepkcs12) {
            File f;
            for (String pkcs12 : settings.pKCS12File) {
                f = new File(pkcs12);
                if (f.exists())
                    cardinfo.add(new CardSignInfo(CardSignInfo.PKCS12TYPE, pkcs12, f.getName()));
            }

        }
        notifyListener();

    }

    public List<CardSignInfo> readSaveListSmartCard() throws Throwable {

        if (this.cardinfo == null) {
            findCertificates(true);
        } else {
            this.waitforrequest.release();
        }

        return this.cardinfo;
    }

    public List<CardSignInfo> readListSmartCard() throws Throwable {
        List<CardSignInfo> cardinfo = new ArrayList<CardSignInfo>();
        // TODO: Replace with PKCS11 Manager
        if (pkcs11manager == null)
            pkcs11manager = new PKCS11Manager();
        String expires;
        String serialnumber;
        for (X509Certificate certificate : pkcs11manager.getCertificates()) {
            boolean[] keyUsage = certificate.getKeyUsage();
            if (certificate.getBasicConstraints() == -1 && keyUsage[0] && keyUsage[1]) {
                LdapName ldapName = new LdapName(certificate.getSubjectX500Principal().getName("RFC1779"));
                String firstName = "", lastName = "", identification = "", commonName = "", organization = "";
                for (Rdn rdn : ldapName.getRdns()) {
                    if (rdn.getType().equals("OID.2.5.4.5"))
                        identification = rdn.getValue().toString();
                    if (rdn.getType().equals("OID.2.5.4.4"))
                        lastName = rdn.getValue().toString();
                    if (rdn.getType().equals("OID.2.5.4.42"))
                        firstName = rdn.getValue().toString();
                    if (rdn.getType().equals("CN"))
                        commonName = rdn.getValue().toString();
                    if (rdn.getType().equals("O"))
                        organization = rdn.getValue().toString();
                }
                expires = new SimpleDateFormat("yyyy-MM-dd").format(certificate.getNotAfter());
                serialnumber = new String(pkcs11manager.getTokenByCert(certificate));
                LOG.debug(firstName + " " + lastName + " (" + identification + "), " + organization + ", "
                    + certificate.getSerialNumber().toString(16) + " [Token serial number: " + serialnumber
                    + "] (Expires: " + expires + ")");
                cardinfo.add(new CardSignInfo(CardSignInfo.PKCS11TYPE, identification, firstName, lastName, commonName,
                    organization, expires, certificate.getSerialNumber().toString(16), serialnumber,
                    pkcs11manager.getSlotByCert(certificate), certificate));
            }

        }
        return cardinfo;
    }

    public Map<String, X509Certificate> getAuthenticationAndSignCertificates() throws Throwable {
        Map<String, X509Certificate> certs = new HashMap<>();
        if (pkcs11manager == null)
            pkcs11manager = new PKCS11Manager();
        String expires;
        String serialnumber;
        for (X509Certificate certificate : pkcs11manager.getCertificates()) {
            boolean[] keyUsage = certificate.getKeyUsage();
            List<String> ekuOids = null;
            try {
                ekuOids = certificate.getExtendedKeyUsage();
            } catch (CertificateParsingException e) {
                ekuOids = Collections.emptyList();
            }
            if (ekuOids != null && ekuOids.contains("1.3.6.1.5.5.7.3.2")) {
                certs.put("authentication", certificate);
            } else {
                certs.put("sign", certificate);
            }
            if (certificate.getBasicConstraints() == -1 && keyUsage[0] && keyUsage[1]) {
                LdapName ldapName = new LdapName(certificate.getSubjectX500Principal().getName("RFC1779"));
                String firstName = "", lastName = "", identification = "", commonName = "", organization = "";
                for (Rdn rdn : ldapName.getRdns()) {
                    if (rdn.getType().equals("OID.2.5.4.5"))
                        identification = rdn.getValue().toString();
                    if (rdn.getType().equals("OID.2.5.4.4"))
                        lastName = rdn.getValue().toString();
                    if (rdn.getType().equals("OID.2.5.4.42"))
                        firstName = rdn.getValue().toString();
                    if (rdn.getType().equals("CN"))
                        commonName = rdn.getValue().toString();
                    if (rdn.getType().equals("O"))
                        organization = rdn.getValue().toString();
                }
                expires = new SimpleDateFormat("yyyy-MM-dd").format(certificate.getNotAfter());
                serialnumber = new String(pkcs11manager.getTokenByCert(certificate));
                cardinfo.add(new CardSignInfo(CardSignInfo.PKCS11TYPE, identification, firstName, lastName, commonName,
                    organization, expires, certificate.getSerialNumber().toString(16), serialnumber,
                    pkcs11manager.getSlotByCert(certificate), certificate));

            }
        }
        return certs;
    }

    public PrivateKey getSignPrivateKey(char[] pin) throws Throwable {
        if (pkcs11manager == null)
            pkcs11manager = new PKCS11Manager();

        for (X509Certificate cert : pkcs11manager.getCertificates()) {
            List<String> ekuOids;
            try {
                ekuOids = cert.getExtendedKeyUsage();
            } catch (CertificateParsingException e) {
                ekuOids = Collections.emptyList();
            }
            // "1.3.6.1.5.5.7.3.2" es el OID de autenticación, así que buscamos el otro
            if (ekuOids == null || !ekuOids.contains("1.3.6.1.5.5.7.3.2")) {
                // Este es el de firma ("sign")
                Long slotId = pkcs11manager.getSlotByCert(cert);
                char[] token = pkcs11manager.getTokenByCert(cert);
                if (slotId == null || token == null) continue;

                KeyStore keystore = pkcs11manager.getKeyStore(slotId, new KeyStore.PasswordProtection(pin));
                String alias = new String(token);
                Key key = keystore.getKey(alias, pin);
                if (key instanceof PrivateKey) {
                    return (PrivateKey) key;
                }
            }
        }
        return null;
    }

    @Override
    public void updateConfig() {
    }

    private void notifyListener() {
        for (SmartCardListener listener : listeners) {
            listener.cardsDetectionChange(cardinfo);
        }
    }

    public void addListener(SmartCardListener toAdd) {
        listeners.add(toAdd);
    }

    public void invalideCache() {
        if (pkcs11manager != null)
            pkcs11manager.invalideCache();

    }

    public boolean isCardPresent() {
        try {
            if (pkcs11manager == null)
                pkcs11manager = new PKCS11Manager();
            List<X509Certificate> certs = pkcs11manager.getCertificates();
            return certs != null && !certs.isEmpty();
        } catch (Throwable e) {
            return false;
        }
    }

    public void setCardInfo(List<CardSignInfo> cardinfo) {
        this.cardinfo = cardinfo;
    }

    public List<CardSignInfo> getListCardInfo() {
        return cardinfo;
    }

}

