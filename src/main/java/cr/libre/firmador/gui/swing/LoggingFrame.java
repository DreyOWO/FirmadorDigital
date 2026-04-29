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

package cr.libre.firmador.gui.swing;

import javax.swing.GroupLayout;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

public class LoggingFrame extends ScrollableJPanel {
    private static final long serialVersionUID = 2015584665968200047L;
    private JTextArea logtext;
    private JScrollPane logScrollPane;

    @SuppressWarnings("this-escape")
    public LoggingFrame() {
        super();
        logtext = new JTextArea();
        logtext.setLineWrap(true);
        logtext.setWrapStyleWord(true);
        logtext.setOpaque(false);

        logScrollPane = new JScrollPane(logtext);
        logScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        logScrollPane.setBorder(null);
        logScrollPane.setOpaque(false);
        logScrollPane.getViewport().setOpaque(false);
        logScrollPane.setPreferredSize(logScrollPane.getPreferredSize());
        logScrollPane.setVisible(true);

        GroupLayout layout = new GroupLayout(this);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(
            layout.createParallelGroup().addComponent(logScrollPane));
        layout.setVerticalGroup(
            layout.createSequentialGroup().addComponent(logScrollPane));
        this.setLayout(layout);
        this.setOpaque(false);
    }

    public void showInfo(String message) {
        this.logtext.append("\n"+message);

    }

    public JScrollPane getLogScrollPane() {
        return logScrollPane;
    }

}
