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

import java.security.Key;
import java.security.KeyStore;
import java.security.Provider;
import java.security.KeyStore.PasswordProtection;
import java.security.cert.X509Certificate;
import java.util.List;

public interface CardManagerInterface {
    public Provider getProvider();

    List<X509Certificate> getCertificates() throws Throwable;

    KeyStore getKeyStore(Long slotID, PasswordProtection password) throws Throwable;

    Key getPrivateKey(String token, Long slotID, PasswordProtection password) throws Throwable;

    X509Certificate getCertificate(String token, Long slotID, PasswordProtection password) throws Throwable;


    void setSerialNumber(String serialnumber);

    public CardSignInfo loadTokens(CardSignInfo card, KeyStore keystore);
}
