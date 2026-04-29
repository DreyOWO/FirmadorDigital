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

package cr.libre.firmador.plugins;

import cr.libre.firmador.Settings;
import cr.libre.firmador.SettingsManager;

public class InstallerPlugin implements Plugin {

    @Override
    public void start() {
        Settings settings = SettingsManager.getInstance().getAndCreateSettings();
        settings.activePlugins.remove("cr.libre.firmador.plugins.InstallerPlugin");
        settings.availablePlugins.remove("cr.libre.firmador.plugins.InstallerPlugin");
        SettingsManager.getInstance().setSettings(settings, true);

    }

    @Override
    public void startLogging() {
        // TODO Auto-generated method stub

    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean getIsRunnable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean interactWithDocuments() {
        return false;
    }
}
