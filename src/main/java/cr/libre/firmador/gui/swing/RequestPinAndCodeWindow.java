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

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;

public class RequestPinAndCodeWindow extends JFrame {
    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JPasswordField pinField = new JPasswordField(10);
    private JTextField codeField = new JTextField(10);
    private JLabel labelPin;
    private JLabel labelCode;
    private JLabel timerLabel;
    private Timer timer;
    private int timeLeftSeconds = 120;
    private boolean timeExpired = false;
    private JLabel entitySummaryLabel;
    private JLabel entityNameLabel;
    private JLabel errorLabel;
    private JPanel bottomContainer;

    @SuppressWarnings("this-escape")
    public RequestPinAndCodeWindow(ImageIcon logo, String nameOfEntity, String resumeOfEntity, String errorMessage) {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 440, 260);

        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(18, 24, 18, 24));
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout());

        int maxWidthPx = 360;
        String resumenHtml = "<html><div style='width:" + maxWidthPx + "px; text-align: center;'>"
            + resumeOfEntity
            + "</div></html>";

        entitySummaryLabel = new JLabel(resumenHtml);
        entitySummaryLabel.setFont(entitySummaryLabel.getFont().deriveFont(Font.PLAIN, 15));
        entitySummaryLabel.setHorizontalAlignment(SwingConstants.CENTER);
        entitySummaryLabel.setBorder(new EmptyBorder(0, 0, 12, 0));
        contentPane.add(entitySummaryLabel, BorderLayout.NORTH);

        // Panel de inputs con GridBagLayout
        JPanel fieldsPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 8, 12, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;

        int row = 0;

        // Logo (columna 0, filas 0 a 2, alineado al centro)
        if (logo != null) {
            JLabel logoLabel = new JLabel(logo);
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridheight = 3;
            gbc.weightx = 0.2;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.VERTICAL;
            gbc.anchor = GridBagConstraints.CENTER;
            fieldsPanel.add(logoLabel, gbc);
        }
        gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;

        int colInput = (logo != null) ? 1 : 0;

        // PIN
        labelPin = new JLabel("Ingrese su PIN:");
        labelPin.setFont(labelPin.getFont().deriveFont(Font.BOLD, 15));
        gbc.gridx = colInput; gbc.gridy = row; gbc.weightx = 0.3;
        gbc.anchor = GridBagConstraints.LINE_START;
        fieldsPanel.add(labelPin, gbc);

        pinField.setFont(pinField.getFont().deriveFont(Font.PLAIN, 18));
        pinField.setColumns(10);
        gbc.gridx = colInput + 1; gbc.gridy = row++; gbc.weightx = 0.7;
        fieldsPanel.add(pinField, gbc);

        // Solo números para PIN
        ((AbstractDocument) pinField.getDocument()).setDocumentFilter(new DigitOnlyDocumentFilter());

        // Código
        labelCode = new JLabel("Ingrese el código:");
        labelCode.setFont(labelCode.getFont().deriveFont(Font.BOLD, 15));
        gbc.gridx = colInput; gbc.gridy = row; gbc.weightx = 0.3;
        gbc.anchor = GridBagConstraints.LINE_START;
        fieldsPanel.add(labelCode, gbc);

        codeField.setFont(codeField.getFont().deriveFont(Font.PLAIN, 18));
        codeField.setColumns(10);
        gbc.gridx = colInput + 1; gbc.gridy = row++; gbc.weightx = 0.7;
        fieldsPanel.add(codeField, gbc);

        // Forzar mayúsculas en código
        ((AbstractDocument) codeField.getDocument()).setDocumentFilter(new UppercaseDocumentFilter());

        JPanel entityNamePanel = new JPanel(new BorderLayout());
        entityNamePanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        entityNamePanel.setOpaque(false); // transparente

        entityNameLabel = new JLabel(nameOfEntity);
        entityNameLabel.setFont(entityNameLabel.getFont().deriveFont(Font.BOLD, 13));
        entityNameLabel.setHorizontalAlignment(SwingConstants.LEFT);
        entityNamePanel.add(entityNameLabel, BorderLayout.WEST);

        // Panel para timer + nombre de entidad (abajo)
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setOpaque(false);
        JPanel timerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        timerLabel = new JLabel("Tiempo restante: 2:00");
        timerLabel.setFont(timerLabel.getFont().deriveFont(Font.BOLD, 15));
        timerPanel.add(timerLabel);

        southPanel.add(timerPanel, BorderLayout.CENTER);
        southPanel.add(entityNamePanel, BorderLayout.SOUTH);

        bottomContainer = new JPanel();
        bottomContainer.setLayout(new BoxLayout(bottomContainer, BoxLayout.Y_AXIS));
        bottomContainer.setOpaque(false);
        bottomContainer.add(southPanel);

        errorLabel = new JLabel();
        errorLabel.setForeground(new Color(180, 0, 0));
        errorLabel.setFont(errorLabel.getFont().deriveFont(Font.BOLD, 13));
        errorLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
        setError(errorMessage);

        bottomContainer.add(errorLabel);


        contentPane.add(fieldsPanel, BorderLayout.CENTER);
        contentPane.add(bottomContainer, BorderLayout.SOUTH);



        // Enfocar automáticamente el campo PIN al mostrar ventana
        pinField.addHierarchyListener(event -> {
            final Component component = event.getComponent();
            if (component.isShowing() && (event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                Window toplevel = SwingUtilities.getWindowAncestor(component);
                toplevel.addWindowFocusListener(new WindowAdapter() {
                    public void windowGainedFocus(WindowEvent event) {
                        component.requestFocus();
                    }
                });
            }
        });
    }

    public void setError(String message) {
        if (errorLabel == null) return;
        if (message == null || message.trim().isEmpty()) {
            errorLabel.setText(" ");
        } else {
            errorLabel.setText(message);
        }
    }

    /** Muestra la ventana y espera. Devuelve true si se aceptó, false si se canceló o si se acaba el tiempo. */
    public boolean showAndWait() {
        boolean ok = false;
        timeLeftSeconds = 120;
        timeExpired = false;

        // Iniciar temporizador
        timer = new Timer(1000, e -> {
            timeLeftSeconds--;
            int min = timeLeftSeconds / 60;
            int sec = timeLeftSeconds % 60;
            timerLabel.setText(String.format("Tiempo restante: %d:%02d", min, sec));
            if (timeLeftSeconds <= 0) {
                timer.stop();
                timeExpired = true;
                Window w = SwingUtilities.getWindowAncestor(contentPane);
                if (w != null) w.dispose();
            }
        });
        timer.start();

        Window parent = SwingUtilities.getWindowAncestor(contentPane);
        if (parent != null) {
            parent.setAlwaysOnTop(true);
            parent.toFront();
            parent.requestFocus();
        }

        while (!ok && !timeExpired) {
            int action = JOptionPane.showConfirmDialog(parent, contentPane, "Ingrese PIN y Código",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

            if (parent != null) parent.setAlwaysOnTop(false);

            if (timeExpired) {
                JOptionPane.showMessageDialog(null, "Tiempo agotado. Intente de nuevo.", "Tiempo agotado", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if (action == JOptionPane.OK_OPTION) {
                if (pinField.getPassword().length > 0 && codeField.getText().trim().length() > 0 ) {
                    ok = true;
                } else {
                    JOptionPane.showMessageDialog(null, "Debe ingresar ambos valores.", "Campos requeridos", JOptionPane.WARNING_MESSAGE);
                }
            } else {
                timer.stop();
                return false;
            }
        }
        timer.stop();
        return ok;
    }

    /** Devuelve el PIN como char[] */
    public char[] getPinPassword() {
        return pinField.getPassword();
    }

    public String getCode() {
        return codeField.getText().trim();
    }

    // ---- Filtros para campos ----

    /** Solo permite dígitos */
    static class DigitOnlyDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string != null && string.matches("\\d+"))
                super.insertString(fb, offset, string, attr);
        }
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text != null && text.matches("\\d+"))
                super.replace(fb, offset, length, text, attrs);
        }
    }

    /** Convierte todo a mayúsculas */
    static class UppercaseDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string != null)
                super.insertString(fb, offset, string.toUpperCase(), attr);
        }
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text != null)
                super.replace(fb, offset, length, text.toUpperCase(), attrs);
        }
    }
}
