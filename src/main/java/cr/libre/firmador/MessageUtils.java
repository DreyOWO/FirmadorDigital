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

package cr.libre.firmador;

import java.util.Locale;
import java.util.ResourceBundle;

public class MessageUtils {
    public static String t(String msgkey) {
        Settings settings = SettingsManager.getInstance().getAndCreateSettings();
        Locale locale = new Locale.Builder().setLanguage(settings.language).setRegion(settings.country).build();
        ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);
        return bundle.getString(msgkey);
    }

    public static char k(char key) {
        return key;
    }

    public static String html2txt(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("<[^>]*>", "");
    }
}
