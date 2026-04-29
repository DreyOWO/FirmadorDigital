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

import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;

public class PKCS12Manager implements CardManagerInterface {
    private String locationFile;
    private KeyStore keyStore;

    @Override
    public Provider getProvider() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<X509Certificate> getCertificates() throws Throwable {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public KeyStore getKeyStore(Long slotID, PasswordProtection password) throws Throwable {
        if (keyStore == null) {
            keyStore = KeyStore.getInstance("PKCS12", "BC");
            try (FileInputStream fis = new FileInputStream(this.locationFile)) {
                keyStore.load(fis, password.getPassword());
            }
        }
        return keyStore;
    }

    @Override
    public Key getPrivateKey(String token, Long slotID, PasswordProtection password) throws Throwable {
        KeyStore keystore = this.getKeyStore(Long.valueOf(0), password);
        Key key = null;
        String alias = keystore.aliases().nextElement();
        key = keystore.getKey(alias, password.getPassword());
        return key;
    }

    @Override
    public X509Certificate getCertificate(String token, Long slotID, PasswordProtection password) throws Throwable {
        KeyStore keystore = this.getKeyStore(Long.valueOf(0), password);
        X509Certificate certificate = null;
        String alias = keystore.aliases().nextElement();
        certificate = (X509Certificate) keystore.getCertificate(alias);
        return certificate;
    }


    @Override
    public void setSerialNumber(String serialnumber) {
        locationFile = serialnumber;
        keyStore = null;
    }

    @Override
    public CardSignInfo loadTokens(CardSignInfo card, KeyStore keystore) {

        try {
            Enumeration<String> enumeration = keystore.aliases();
            if (enumeration.hasMoreElements()) {
                String alias = enumeration.nextElement();
                card.setTokenSerialNumber(alias);
            }
        } catch (KeyStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return card;
    }



}
