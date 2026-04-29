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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.invoke.MethodHandles;
import java.security.KeyStore.PasswordProtection;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.cards.CardSignInfo;
import javax.swing.SwingConstants;
import java.awt.FlowLayout;
import javax.swing.JTextPane;
import javax.swing.BorderFactory;
import javax.swing.Box;

public class RequestPinWindowRemote extends JFrame {
    @SuppressWarnings("serial")
    final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final long serialVersionUID = -8464569433812264362L;
    private JPanel contentPane;
	private JPasswordField pinField = new JPasswordField(10);
    @SuppressWarnings("serial")
    protected CardSignInfo card;
	private JLabel certificateid;
    private JLabel label;
    private JLabel lblNewLabel;
	private String documentName;
	private JLabel iconlabel;
	private JPanel iconPanel;
	private JPanel panel;
	private JPanel certpanel;
	private JPanel signpanel;
	private JTextPane infotext;
	private JPanel infopanel;
	private Component horizontalGlue;
	private Component horizontalStrut;

    @SuppressWarnings("this-escape")
    public RequestPinWindowRemote() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 651, 262);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);

		GridBagConstraints gbcinfo = new GridBagConstraints();
		gbcinfo.gridx = 3; // Columna inicial
		gbcinfo.gridy = 0; // Fila inicial
		gbcinfo.gridwidth = 3; // Abarca 3 columnas
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.X_AXIS));

		GridBagConstraints gbcicon = new GridBagConstraints();
		gbcicon.gridx = 0; // Columna inicial
		gbcicon.gridy = 0; // Fila inicial
		gbcicon.gridheight = 2; // Abarca 2 filas

		iconPanel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) iconPanel.getLayout();
		flowLayout.setVgap(15);
		flowLayout.setHgap(15);
		flowLayout.setAlignment(FlowLayout.LEFT);
		contentPane.add(iconPanel);

		iconlabel = new JLabel("");
		iconPanel.add(iconlabel);
		iconlabel.setHorizontalAlignment(SwingConstants.CENTER);

		panel = new JPanel();
		contentPane.add(panel);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		certpanel = new JPanel();
		certpanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		certpanel.setAlignmentY(Component.TOP_ALIGNMENT);
		certpanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(certpanel);
		certpanel.setLayout(new BoxLayout(certpanel, BoxLayout.X_AXIS));

		lblNewLabel = new JLabel(MessageUtils.t("pin_dialog_remote_certificate"));
		certpanel.add(lblNewLabel);
		lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);

		horizontalStrut = Box.createHorizontalStrut(20);
		certpanel.add(horizontalStrut);
		certificateid = new JLabel("");
		certpanel.add(certificateid);

		signpanel = new JPanel();
		signpanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		signpanel.setAlignmentY(Component.TOP_ALIGNMENT);
		signpanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(signpanel);
		signpanel.setLayout(new BoxLayout(signpanel, BoxLayout.X_AXIS));

		label = new JLabel("Ingrese su PIN:  ");
		signpanel.add(label);
		label.setHorizontalAlignment(SwingConstants.LEFT);

		horizontalGlue = Box.createHorizontalGlue();
		signpanel.add(horizontalGlue);
		pinField.setHorizontalAlignment(SwingConstants.LEFT);
		signpanel.add(pinField);

		pinField.setBounds(124, 34, 312, 36);

		infopanel = new JPanel();
		infopanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		infopanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(infopanel);
		infopanel.setLayout(new BoxLayout(infopanel, BoxLayout.X_AXIS));

		infotext = new JTextPane();
		infotext.setOpaque(false);
		infopanel.add(infotext);
		// infotext.setBackground(UIManager.getColor("Label.background"));
		infotext.setContentType("text/html");
		infotext.setEditable(false);
		pinField.addHierarchyListener(new HierarchyListener() {
			public void hierarchyChanged(HierarchyEvent event) {
				final Component component = event.getComponent();
				if (component.isShowing() && (event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
					Window toplevel = SwingUtilities.getWindowAncestor(component);
					toplevel.addWindowFocusListener(new WindowAdapter() {
						public void windowGainedFocus(WindowEvent event) {
							component.requestFocus();
						}
					});
				}
			}
		});
		pinField.getAccessibleContext()
				.setAccessibleDescription(MessageUtils.t("pin_dialog_remote_certificate_accesible"));
		pinField.grabFocus();
    }


	public int showandwait() {
        SwingUtilities.invokeLater(() -> {
            Window window = SwingUtilities.getWindowAncestor(contentPane);
            if (window != null) {
                window.setAlwaysOnTop(true);
                window.toFront();
                window.requestFocus();
            }
            pinField.requestFocusInWindow();
        });
        int action =0;
        boolean ok=false;
        Window parent = SwingUtilities.getWindowAncestor(contentPane);
        while(!ok) {
            action = JOptionPane.showConfirmDialog(parent, contentPane, MessageUtils.t("pin_dialog_title"),
                    JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE);
            if(action==JOptionPane.OK_OPTION) {
                if (pinField.getPassword().length > 0 && this.card != null) {
                    this.card.setPin(new PasswordProtection(pinField.getPassword())); // PasswordProtection is passed as reference, password.destroy() would remove the referred in card variable
                    pinField.setText(""); // However, https://stackoverflow.com/a/36828836
                    ok=true;
                }else {
                    JOptionPane.showMessageDialog(null, MessageUtils.t("pin_dialog_error_title"),
                            MessageUtils.t("pin_dialog_error_context"), JOptionPane.WARNING_MESSAGE);
                }
            }else if(action==JOptionPane.CANCEL_OPTION || action==JOptionPane.CLOSED_OPTION) {
                ok=true;
            }
        }
		return action;
    }

    public PasswordProtection getPassword() {
        return new PasswordProtection(pinField.getPassword());
    }

    public CardSignInfo getCardInfo() {
        return this.card;
    }

	public CardSignInfo getCard() {
		return card;
	}

	public void setCard(CardSignInfo card) {
		certificateid.setText(card.getDisplayInfo());

		this.card = card;
	}

	public String getDocumentName() {
		return documentName;
	}

	public void setDocumentName(String documentName) {
		this.documentName = documentName;
		this.infotext.setText(MessageUtils.t("pin_dialog_info_remote") + " " + documentName);
	}

	public void setIcon(ImageIcon icon) {
		if (icon != null) {
			iconlabel.setIcon(icon);
		}
	}
}
