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

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.Window;

public class RequestHostAuthorizationRemote extends JFrame {
    private static final long serialVersionUID = 1L;
    private JLabel hostLabel;
    @SuppressWarnings("unused")
    private String requestHost; // FIXME is this actually used?
    private int authorized = 2; // Para indicar si se autorizó o no

    @SuppressWarnings("this-escape")
    public RequestHostAuthorizationRemote() {
        super("Solicitud de autorización de host");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 500, 200);

        // Crear panel de contenido
        JPanel contentPane = new JPanel();
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        setContentPane(contentPane);

        // Crear etiqueta del host
        hostLabel = new JLabel("Esperando solicitud...");
        hostLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPane.add(hostLabel);
    }

    // Método para esperar la acción del usuario (autorizar o cancelar)
    public int showAndWait(String host) {
        //this.requestHost = host;
        hostLabel.setText(host);

        SwingUtilities.invokeLater(() -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.setAlwaysOnTop(true);
                window.toFront();
                window.requestFocus();
            }
        });

        Object[] options = { "Autorizar", "Autorizar solo esta vez", "Cancelar" };

        int choice = JOptionPane.showOptionDialog(
            this,
            "La aplicación quiere registrarse como un dominio de confianza:\n" + host,
            "Solicitud de Autorización",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[2]);

        switch (choice) {
            case 0:
                authorized = 1;
                break;
            case 1:
                authorized = 2;
                break;
            case 2:
                authorized = 3;
                break;
        }

        return authorized;
    }
}
