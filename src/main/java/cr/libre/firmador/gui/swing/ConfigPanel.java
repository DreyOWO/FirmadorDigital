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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.invoke.MethodHandles;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.JFileChooser;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import cr.libre.firmador.gui.GUIInterface;
import cr.libre.firmador.gui.GUISwing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.Settings;
import cr.libre.firmador.SettingsManager;

public class ConfigPanel extends ScrollableJPanel {
    @SuppressWarnings("serial")
    final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    JTextArea defaultSignMessage;
    JScrollPane scrollableDefaultSignMessage;
    @SuppressWarnings("serial")
    Settings settings;
    @SuppressWarnings("serial")
    SettingsManager manager;
    @SuppressWarnings("serial")
    private GUIInterface gui;
    private Integer iconSize = 32;
    private JButton btFontColor, btBackgroundColor, btImage, btPKCS11Module, btBrowser;
    private JCheckBox withoutVisibleSign, /* useLTA, */ overwriteSourceFile, startFimadorRemote, showLogs, isImgWithDpi,
            isSimplifiedMode;
    private JComboBox<String> font, fontPosition, pAdESLevel, xAdESLevel, cAdESLevel, jAdESLevel, language, windowstate;
    private JPanel advancedBottomSpace;
    private JScrollPane configPanel;
    private JSpinner signWidth, signHeight, fontSize, signX, signY, pageNumber, portNumber, signImageWidth,
            signImageHeight;
    private JTextField reason, place, contact, dateFormat, fontColor, backgroundColor, imageText, pKCS11ModuleText,
            pDFImgScaleFactor, preferredBrowser;
    private Pkcs12ConfigPanel pKCS12Panel;
    private PluginManagerPlugin pluginsActive;
    private ScrollableJPanel simplePanel, advancedPanel;
    private boolean isAdvancedOptions = false;
    private JTextField sofficePath;
    private JTextArea registeredAllowedOrigins;
    private JLabel pkcs11Info1, pkcs11Info2;
    private static final long serialVersionUID = 1L;

    private void createSimpleConfigPanel() {
        simplePanel = new ScrollableJPanel();
        simplePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        simplePanel.setLayout(new BoxLayout(simplePanel, 1));
        simplePanel.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_simple_panel_accessible"));

        JPanel checkboxesContainer = new JPanel();
        checkboxesContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        checkboxesContainer.setLayout(new BoxLayout(checkboxesContainer, BoxLayout.Y_AXIS));
        checkboxesContainer.setOpaque(false);

        JPanel checkpanel = new JPanel();
        checkpanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        checkpanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        checkpanel.setLayout(new BoxLayout(checkpanel, 0));
        checkpanel.setOpaque(false);
        checkpanel.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_check_panel_accessible"));

        isSimplifiedMode = new JCheckBox(MessageUtils.t("configpanel_simplified_mode") + "        ",
                this.settings.simplified_mode);
        isSimplifiedMode.setToolTipText(MessageUtils.t("configpanel_simplified_mode"));
        isSimplifiedMode.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("configpanel_simplified_mode_accessible"));
        isSimplifiedMode.setOpaque(false);
        checkpanel.add(isSimplifiedMode);

        withoutVisibleSign = new JCheckBox(MessageUtils.t("configpanel_without_visible_signature") + "        ",
                this.settings.withoutVisibleSign);
        withoutVisibleSign.setToolTipText(MessageUtils.t("configpanel_without_visible_signature"));
        withoutVisibleSign.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("configpanel_without_visible_signature_accessible"));
        withoutVisibleSign.setOpaque(false);
        checkpanel.add(withoutVisibleSign);
        /*
         * useLTA = new JCheckBox("Usar LTA automático", this.settings.useLTA);
         * checkpanel.add(useLTA);
         * checkpanel.setOpaque(false);
         * useLTA.addItemListener(new ItemListener() {
         * public void itemStateChanged(ItemEvent arg0) {
         * changeLTA();
         * }
         * });
         */
        showLogs = new JCheckBox(MessageUtils.t("configpanel_view_logs") + "        ", this.settings.showLogs);
        showLogs.setToolTipText(MessageUtils.t("configpanel_view_logs"));
        showLogs.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_view_logs_accessible"));
        showLogs.setOpaque(false);
        checkpanel.add(showLogs);

        JPanel checkpanelDown = new JPanel();
        checkpanelDown.setAlignmentX(Component.CENTER_ALIGNMENT);
        checkpanelDown.setBorder(new EmptyBorder(10, 10, 10, 10));
        checkpanelDown.setLayout(new BoxLayout(checkpanelDown, 0));
        checkpanelDown.setOpaque(false);
        checkpanelDown.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_check_panel_accessible"));

        overwriteSourceFile = new JCheckBox(MessageUtils.t("configpanel_rewrite_original_file"),
                this.settings.overwriteSourceFile);
        overwriteSourceFile.setToolTipText(MessageUtils.t("configpanel_rewrite_original_file"));
        overwriteSourceFile.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("configpanel_rewrite_original_file_accessible"));
        overwriteSourceFile.setOpaque(false);
        checkpanelDown.add(overwriteSourceFile);

        startFimadorRemote = new JCheckBox(MessageUtils.t("configpanel_start_fimador_remote"),
                this.settings.startFimadorRemote);
        startFimadorRemote.setToolTipText(MessageUtils.t("configpanel_start_fimador_remote_tooltip"));
        startFimadorRemote.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("configpanel_start_fimador_remote_accessible"));
        startFimadorRemote.setOpaque(false);
        checkpanelDown.add(startFimadorRemote);

        isImgWithDpi = new JCheckBox("Reescalado con DPI", this.settings.isImgWithDpi);
        isImgWithDpi.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_img_with_dpi_accessible"));
        isImgWithDpi.setOpaque(false);
        checkpanelDown.add(isImgWithDpi);

        checkboxesContainer.add(checkpanel);
        checkboxesContainer.add(checkpanelDown);

        simplePanel.add(checkboxesContainer);

        reason = new JTextField();
        reason.setText(this.settings.reason);
        reason.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_reason_accessible"));

        place = new JTextField();
        place.setText(this.settings.place);
        place.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_place_accessible"));

        contact = new JTextField();
        contact.setText(this.settings.contact);
        contact.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_contact_accessible"));

        dateFormat = new JTextField();
        dateFormat.setText(this.settings.dateFormat);
        dateFormat.setToolTipText(MessageUtils.t("configpanel_must_be_compatible_with_java_date_formats"));
        dateFormat.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_date_format_accessible"));

        sofficePath = new JTextField();
        sofficePath.setText(this.settings.getSofficePath());
        sofficePath.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_soffice_path_accessible"));

        defaultSignMessage = new JTextArea();
        defaultSignMessage.setText(settings.defaultSignMessage);
        defaultSignMessage.setToolTipText(MessageUtils.t("configpanel_default_sign_message_help"));
        defaultSignMessage.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("configpanel_default_sign_message_accessible"));
        defaultSignMessage.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, /*
                                                                                               * forward traversal
                                                                                               * textarea with tab
                                                                                               */
                KeyboardFocusManager.getCurrentKeyboardFocusManager()
                        .getDefaultFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        defaultSignMessage.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, /*
                                                                                                * reverse traversal
                                                                                                * textarea with
                                                                                                * shift+tab
                                                                                                */
                KeyboardFocusManager.getCurrentKeyboardFocusManager()
                        .getDefaultFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        scrollableDefaultSignMessage = new JScrollPane(defaultSignMessage);
        scrollableDefaultSignMessage.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("configpanel_default_sign_message_scroll_accessible"));

        if (UIManager.getLookAndFeel().getClass().getName().contains("GTKLookAndFeel")) {
            defaultSignMessage.setBorder(reason.getBorder()); // Add text margins like in text fields for GTK
            scrollableDefaultSignMessage.setViewportBorder(BorderFactory.createTitledBorder("")); // Add border to
                                                                                                  // textarea for GTK
        }
        if (UIManager.getLookAndFeel().getClass().getName().contains("WindowsLookAndFeel"))
            defaultSignMessage.setFont(reason.getFont()); // Windows defaults to fixed width font (Courier), use same as
                                                          // jTextField

        pageNumber = new JSpinner();
        pageNumber.setModel(new SpinnerNumberModel(this.settings.pageNumber, null, null, 1));
        pageNumber.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_page_number_accessible"));
        JComponent editor = pageNumber.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_page_number_accessible"));
        }

        signWidth = new JSpinner();
        signWidth.setModel(new SpinnerNumberModel(this.settings.signWidth, null, null, 1));
        signWidth.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_sign_width_accessible"));
        editor = signWidth.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_sign_width_accessible"));
        }

        signHeight = new JSpinner();
        signHeight.setModel(new SpinnerNumberModel(this.settings.signHeight, null, null, 1));
        signHeight.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_sign_height_accessible"));
        editor = signHeight.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_sign_height_accessible"));
        }

        signX = new JSpinner();
        signX.setModel(new SpinnerNumberModel(this.settings.signX, null, null, 1));
        signX.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_sign_x_accessible"));
        editor = signX.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_sign_x_accessible"));
        }

        signY = new JSpinner();
        signY.setModel(new SpinnerNumberModel(this.settings.signY, null, null, 1));
        signY.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_sign_y_accessible"));
        editor = signY.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_sign_y_accessible"));
        }

        fontSize = new JSpinner();
        fontSize.setModel(new SpinnerNumberModel(this.settings.fontSize, null, null, 1));
        fontSize.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_font_size_accessible"));
        editor = fontSize.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_font_size_accessible"));
        }

        String fonts[];
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("mac"))
            fonts = new String[] {
                    "Helvetica Regular", "Helvetica Oblique", "Helvetica Bold", "Helvetica Bold Oblique",
                    "Times New Roman Regular", "Times New Roman Italic", "Times New Roman Bold",
                    "Times New Roman Bold Italic",
                    "Courier New Regular", "Courier New Italic", "Courier New Bold", "Courier New Bold Italic"
            };
        else if (osName.contains("linux"))
            fonts = new String[] {
                    "Nimbus Sans Regular", "Nimbus Sans Italic", "Nimbus Sans Bold", "Nimbus Sans Bold Italic",
                    "Nimbus Roman Regular", "Nimbus Roman Italic", "Nimbus Roman Bold", "Nimbus Roman Bold Italic",
                    "Nimbus Mono PS Regular", "Nimbus Mono PS Italic", "Nimbus Mono PS Bold",
                    "Nimbus Mono PS Bold Italic"
            };
        else if (osName.contains("windows"))
            fonts = new String[] {
                    "Arial Regular", "Arial Italic", "Arial Bold", "Arial Bold Italic",
                    "Times New Roman Regular", "Times New Roman Italic", "Times New Roman Bold",
                    "Times New Roman Bold Italic",
                    "Courier New Regular", "Courier New Italic", "Courier New Bold", "Courier New Bold Italic"
            };
        else
            fonts = new String[] { Font.SANS_SERIF, Font.SERIF, Font.MONOSPACED };
        font = new JComboBox<String>(fonts);
        font.setSelectedItem(settings.font);
        font.setOpaque(false);
        font.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_font_accessible"));

        fontPosition = new JComboBox<String>(new String[] { "RIGHT", "LEFT", "BOTTOM", "TOP", "ONLY IMAGE" });
        fontPosition.setSelectedItem(settings.fontAlignment);
        fontPosition.setOpaque(false);
        fontPosition.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_font_position_accessible"));

        language = new JComboBox<String>(settings.languagesList);
        language.setSelectedItem(settings.language);
        language.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_language_accessible"));

        JPanel browserPanel = new JPanel();
        browserPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        browserPanel.setLayout(new BoxLayout(browserPanel, 0));
        browserPanel.setOpaque(false);

        btBrowser = new JButton(MessageUtils.t("configpanel_choose"));
        btBrowser.setToolTipText(MessageUtils.t("configpanel_choose"));
        btBrowser.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_browset_button_accessible"));

        preferredBrowser = new JTextField();
        preferredBrowser.setText(settings.preferredBrowser);
        preferredBrowser.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("configpanel_preferred_browser_accessible"));

        browserPanel.add(btBrowser);
        browserPanel.add(preferredBrowser);

        windowstate = new JComboBox<String>(settings.windowSetateList);
        windowstate.setSelectedItem(settings.startwindowstate);
        windowstate.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_window_state_accessible"));

        JPanel fontColorPanel = new JPanel();
        fontColorPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        fontColorPanel.setLayout(new BoxLayout(fontColorPanel, 0));
        fontColorPanel.setOpaque(false);
        fontColorPanel.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("configpanel_font_color_panel_accessible"));

        fontColor = new JTextField();
        fontColor.setText(this.settings.fontColor);
        fontColor.setToolTipText(MessageUtils.t("configpanel_use_the_word_transparent_if_you_do_not_want_a_color"));
        fontColor.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_font_color_accessible"));
        fontColor.getDocument().addDocumentListener(new DocumentListener() {
            public void updateIcon(DocumentEvent edoc) {
                try {
                    String text = fontColor.getText();
                    if (!text.isEmpty() && !text.equalsIgnoreCase("transparente")) {
                        Color color = Color.decode(text);
                        btFontColor.setIcon(createImageIcon(color));
                    } else
                        btFontColor.setIcon(getTransparentImageIcon());
                } catch (Exception e) {
                    LOG.error(MessageUtils.t("configpanel_font_color_change_error"), e);
                    e.printStackTrace();
                }
            }

            public void insertUpdate(DocumentEvent e) {
                updateIcon(e);
            }

            public void removeUpdate(DocumentEvent e) {
                updateIcon(e);
            }

            public void changedUpdate(DocumentEvent e) {
                updateIcon(e);
            }
        });
        btFontColor = new JButton(MessageUtils.t("configpanel_choose"));
        btFontColor.setToolTipText(MessageUtils.t("configpanel_choose"));
        btFontColor.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("configpanel_font_color_button_accessible"));
        btFontColor.setOpaque(false);
        setIcons(btFontColor, this.settings.fontColor, this.settings.getFontColor());
        fontColorPanel.add(btFontColor);
        fontColorPanel.add(fontColor);

        JPanel backgroundColorPanel = new JPanel();
        backgroundColorPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        backgroundColorPanel.setLayout(new BoxLayout(backgroundColorPanel, 0));
        backgroundColorPanel.setOpaque(false);
        backgroundColorPanel.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("configpanel_background_color_panel_accessible"));

        backgroundColor = new JTextField();
        backgroundColor.setToolTipText(
                MessageUtils.t("configpanel_use_the_word_transparent_if_you_do_not_want_a_background_color"));
        String backgroundColorText = this.settings.backgroundColor;
        backgroundColor.setText(backgroundColorText);
        backgroundColor.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("configpanel_background_color_accessible"));

        btBackgroundColor = new JButton(MessageUtils.t("configpanel_choose"));
        btBackgroundColor.setToolTipText(MessageUtils.t("configpanel_choose"));
        btBackgroundColor.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("configpanel_background_color_button_accessible"));
        btBackgroundColor.setOpaque(false);
        setIcons(btBackgroundColor, this.settings.backgroundColor, this.settings.getBackgroundColor());
        backgroundColorPanel.add(btBackgroundColor);
        backgroundColorPanel.add(backgroundColor);
        backgroundColor.getDocument().addDocumentListener(new DocumentListener() {
            public void updateIcon(DocumentEvent edoc) {
                try {
                    String text = backgroundColor.getText();
                    if (!text.isEmpty() && !text.equalsIgnoreCase("transparente")) {
                        Color color = Color.decode(text);
                        btBackgroundColor.setIcon(createImageIcon(color));
                    } else
                        btBackgroundColor.setIcon(getTransparentImageIcon());
                } catch (Exception e) {
                    LOG.error(MessageUtils.t("configpanel_background_color_change_error"), e);
                    e.printStackTrace();
                }
            }

            public void insertUpdate(DocumentEvent e) {
                updateIcon(e);
            }

            public void removeUpdate(DocumentEvent e) {
                updateIcon(e);
            }

            public void changedUpdate(DocumentEvent e) {
                updateIcon(e);
            }
        });

        JPanel imagePanel = new JPanel();
        imagePanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        imagePanel.setLayout(new BoxLayout(imagePanel, 0));
        imagePanel.setOpaque(false);

        imageText = new JTextField();
        imageText.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_image_text_accessible"));

        btImage = new JButton(MessageUtils.t("configpanel_choose"));
        btImage.setToolTipText(MessageUtils.t("configpanel_choose"));
        btImage.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_image_button_accessible"));
        btImage.setIcon(createImageIcon(new Color(255, 255, 255, 0)));
        if (this.settings.image != null && !settings.image.trim().isEmpty()) {
            imageText.setText(this.settings.image);
            btImage.setIcon(this.getIcon(this.settings.image));
        }
        imagePanel.add(btImage);
        imagePanel.add(imageText);

        signImageWidth = new JSpinner();
        signImageWidth.setModel(new SpinnerNumberModel(this.settings.signImageWidth, null, null, 1));
        signImageWidth.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("configpanel_sign_image_width_accessible"));
        editor = signImageWidth.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.getAccessibleContext()
                    .setAccessibleName(MessageUtils.t("configpanel_sign_image_width_accessible"));
        }

        signImageHeight = new JSpinner();
        signImageHeight.setModel(new SpinnerNumberModel(this.settings.signImageHeight, null, null, 1));
        signImageHeight.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("configpanel_sign_image_height_accessible"));
        editor = signImageHeight.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.getAccessibleContext()
                    .setAccessibleName(MessageUtils.t("configpanel_sign_image_height_accessible"));
        }

        portNumber = new JSpinner();
        portNumber.setModel(new SpinnerNumberModel((int) this.settings.portNumber, 1024, 65535, 1));
        portNumber.setEditor(new JSpinner.NumberEditor(portNumber, "0"));
        portNumber.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_port_number_accessible"));
        editor = portNumber.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_port_number_accessible"));
        }

        addSettingsBox(simplePanel, MessageUtils.t("configpanel_reason") + ":", reason);
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_place") + ":", place);
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_contact") + ":", contact);
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_date_format") + ":", dateFormat);
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_signature_message") + ":", scrollableDefaultSignMessage,
                new Dimension(150, 50));
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_initial_page") + ":", pageNumber);
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_signature_width") + ":", signWidth);
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_signature_height") + ":", signHeight);
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_initial_position_x") + ":", signX);
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_initial_position_y") + ":", signY);
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_font_size") + ":", fontSize);
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_font") + ":", font);
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_font_position") + ":", fontPosition);
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_font_color") + ":", fontColorPanel);
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_background_color") + ":", backgroundColorPanel);
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_signature_image") + ":", imagePanel);
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_signature_image_width") + ":", signImageWidth);
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_signature_image_height") + ":", signImageHeight);
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_listening_port") + ":", portNumber);
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_libreoffice_route") + ":", sofficePath);
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_language") + ":", language);
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_preferred_browser") + ":", browserPanel);
        addSettingsBox(simplePanel, MessageUtils.t("configpanel_windowstate") + ":", windowstate);

        btFontColor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                showFontColorPicker();
            }
        });
        btBackgroundColor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                showBackgroundColorPicker();
            }
        });
        btImage.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                showImagePicker();
            }
        });
        btBrowser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                showExecPicker();
            }
        });
        configPanel = new JScrollPane(simplePanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        configPanel.setPreferredSize(new Dimension(700, 400));
        configPanel.setBorder(null);
        configPanel.setOpaque(false);
        configPanel.getViewport().setOpaque(false);
        configPanel.getAccessibleContext().setAccessibleName(null);
        add(configPanel, BorderLayout.CENTER);
    }

    /*
     * private void changeLTA() {
     * if (useLTA.isSelected()){
     * pAdESLevel.setSelectedItem("LTA");
     * xAdESLevel.setSelectedItem("LTA");
     * cAdESLevel.setSelectedItem("LTA");
     * jAdESLevel.setSelectedItem("LTA");
     * }
     * }
     */
    private void createAdvancedConfigPanel() {
        advancedPanel = new ScrollableJPanel();
        advancedPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        advancedPanel.setLayout(new BoxLayout(advancedPanel, 1));
        advancedPanel.getAccessibleContext().setAccessibleName(null);

        pDFImgScaleFactor = new JTextField();
        pDFImgScaleFactor.setText(String.format("%.2f", this.settings.pDFImgScaleFactor));
        pDFImgScaleFactor.setToolTipText(MessageUtils.t("configpanel_scale_factor_to_present_the_pdf_page_preview"));
        pDFImgScaleFactor.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("configpanel_pdf_scale_factor_accessible"));

        pluginsActive = new PluginManagerPlugin();
        pluginsActive.setPreferredSize(new Dimension(450, 130));
        pluginsActive.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("configpanel_plugins_manager_accessible"));
        advancedPanel.add(pluginsActive);

        pKCS12Panel = new Pkcs12ConfigPanel();
        pKCS12Panel.setList(settings.pKCS12File);
        pKCS12Panel.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_pkcs12_panel_accessible"));
        pKCS12Panel.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("configpanel_pkcs12_panel_accessible_description"));
        advancedPanel.add(pKCS12Panel);

        String pAdESLevelOptions[] = { "T", "LT", "LTA" };
        pAdESLevel = new JComboBox<String>(pAdESLevelOptions);
        pAdESLevel.setSelectedItem(settings.pAdESLevel);
        pAdESLevel.setOpaque(false);
        pAdESLevel.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_pades_level_accessible"));
        addSettingsBox(advancedPanel, MessageUtils.t("configpanel_level") + " PAdES:", pAdESLevel);

        String xAdESLevelOptions[] = { "T", "LT", "LTA" };
        xAdESLevel = new JComboBox<String>(xAdESLevelOptions);
        xAdESLevel.setSelectedItem(settings.xAdESLevel);
        xAdESLevel.setOpaque(false);
        xAdESLevel.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_xades_level_accessible"));
        addSettingsBox(advancedPanel, MessageUtils.t("configpanel_level") + " XAdES:", xAdESLevel);

        String cAdESLevelOptions[] = { "T", "LT", "LTA" };
        cAdESLevel = new JComboBox<String>(cAdESLevelOptions);
        cAdESLevel.setSelectedItem(settings.cAdESLevel);
        cAdESLevel.setOpaque(false);
        cAdESLevel.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_cades_level_accessible"));
        addSettingsBox(advancedPanel, MessageUtils.t("configpanel_level") + " CAdES:", cAdESLevel);

        String jAdESLevelOptions[] = { "T", "LT", "LTA" };
        jAdESLevel = new JComboBox<String>(jAdESLevelOptions);
        jAdESLevel.setSelectedItem(settings.jAdESLevel);
        jAdESLevel.setOpaque(false);
        jAdESLevel.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_jades_level_accessible"));
        addSettingsBox(advancedPanel, MessageUtils.t("configpanel_level") + " JAdES:", jAdESLevel);

        addSettingsBox(advancedPanel, MessageUtils.t("configpanel_pdf_preview_scale"), pDFImgScaleFactor);

        registeredAllowedOrigins = new JTextArea(5, 20);
        registeredAllowedOrigins.setText(this.settings.getFormattedAllowedPorts());
        registeredAllowedOrigins.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("configpanel_allowed_origins_accessible"));
        registeredAllowedOrigins.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("configpanel_allowed_origins_accessible_description"));

        JScrollPane scrollPane = new JScrollPane(registeredAllowedOrigins);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getAccessibleContext().setAccessibleName(null);

        JLabel portsLabel = new JLabel(MessageUtils.t("configpanel_allowed_hosts"));
        portsLabel.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("configpanel_allowed_hosts_label_accessible"));

        JPanel portsPanel = new JPanel(new BorderLayout(5, 5));
        portsPanel.add(portsLabel, BorderLayout.NORTH);
        portsPanel.add(scrollPane, BorderLayout.CENTER);
        portsPanel.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_ports_panel_accessible"));
        advancedPanel.add(portsPanel, BorderLayout.CENTER);

        JPanel pKCS11ModulePanel = new JPanel();
        pKCS11ModulePanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        pKCS11ModulePanel.setLayout(new BoxLayout(pKCS11ModulePanel, 0));
        pKCS11ModulePanel.setOpaque(false);
        pKCS11ModulePanel.getAccessibleContext().setAccessibleName(null);

        pKCS11ModuleText = new JTextField();
        pKCS11ModuleText.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("configpanel_pkcs11_module_text_accessible"));

        btPKCS11Module = new JButton(MessageUtils.t("configpanel_choose"));
        btPKCS11Module.setToolTipText(MessageUtils.t("configpanel_choose"));
        btPKCS11Module.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_pkcs11_button_accessible"));
        if (this.settings.extraPKCS11Lib != null)
            pKCS11ModuleText.setText(this.settings.extraPKCS11Lib);
        pKCS11ModulePanel.add(pKCS11ModuleText);
        pKCS11ModulePanel.add(btPKCS11Module);
        btPKCS11Module.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                String path = getFilePath();
                if (path != null)
                    pKCS11ModuleText.setText(path);
            }
        });
        addSettingsBox(advancedPanel, MessageUtils.t("configpanel_file") + " PKCS11", pKCS11ModulePanel); // FIXME
                                                                                                          // prefill
                                                                                                          // with
        // default paths when
        // unset
        pkcs11Info1 = new JLabel(MessageUtils.t("configpanel_the_file") + " PKCS11 "
                + MessageUtils.t("configpanel_is_automatically_detected") + ", ");
        pkcs11Info1.setToolTipText(MessageUtils.t("configpanel_the_file") + " PKCS11 "
                + MessageUtils.t("configpanel_is_automatically_detected") + ", ");
        pkcs11Info1.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_pkcs11_info1_accessible"));

        pkcs11Info2 = new JLabel(MessageUtils.t("configpanel_but_could_be_write_using_the_previous_field"));
        pkcs11Info2.setToolTipText(MessageUtils.t("configpanel_but_could_be_write_using_the_previous_field"));
        pkcs11Info2.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_pkcs11_info2_accessible"));
        advancedPanel.add(pkcs11Info1);
        advancedPanel.add(pkcs11Info2);

        advancedBottomSpace = new JPanel();
        advancedBottomSpace.setOpaque(false);
        advancedPanel.add(advancedBottomSpace);
        // changeLTA();
    }

    @SuppressWarnings("this-escape")
    public ConfigPanel() {
        manager = SettingsManager.getInstance();
        settings = manager.getAndCreateSettings();
        setLayout(new BorderLayout(0, 0));
        this.createSimpleConfigPanel();
        this.createAdvancedConfigPanel();

        JPanel optionswitchpanel = new JPanel();
        optionswitchpanel.getAccessibleContext().setAccessibleName(null);
        add(optionswitchpanel, BorderLayout.NORTH);

        JButton showadvanced = new JButton(MessageUtils.t("configpanel_advanced_options"));
        showadvanced.setToolTipText(MessageUtils.t("configpanel_advanced_options"));
        showadvanced.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("configpanel_show_advanced_button_accessible"));
        showadvanced.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("configpanel_show_advanced_button_accessible_description"));
        showadvanced.setOpaque(false);
        optionswitchpanel.setOpaque(false);
        optionswitchpanel.add(showadvanced);
        showadvanced.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                isAdvancedOptions = !isAdvancedOptions;
                if (isAdvancedOptions) {
                    showadvanced.setText(MessageUtils.t("configpanel_basic_options"));
                    configPanel.setViewportView(advancedPanel);
                    simplePanel.setVisible(false);
                    advancedPanel.setVisible(true);
                } else {
                    showadvanced.setText(MessageUtils.t("configpanel_advanced_options"));
                    configPanel.setViewportView(simplePanel);
                    advancedPanel.setVisible(false);
                    simplePanel.setVisible(true);
                }
            }
        });

        JPanel btns = new JPanel();
        btns.setOpaque(false);
        btns.getAccessibleContext().setAccessibleName(null);
        add(btns, BorderLayout.SOUTH);

        JButton restartbtn = new JButton(MessageUtils.t("configpanel_restore"));
        restartbtn.setToolTipText(MessageUtils.t("configpanel_restore"));
        restartbtn.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_restart_button_accessible"));
        restartbtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                restartSettings();
                ((GUISwing) gui).showNotification(MessageUtils.t("configpanel_restore_done"),
                        GUISwing.NotificationType.SUCCESS);
            }
        });
        btns.add(restartbtn);

        JButton applywithoutsave = new JButton(MessageUtils.t("configpanel_apply_without_saving"));
        applywithoutsave.setToolTipText(MessageUtils.t("configpanel_apply_without_saving"));
        applywithoutsave.getAccessibleContext()
                .setAccessibleName(MessageUtils.t("configpanel_apply_without_save_button_accessible"));
        applywithoutsave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                chargeSettings();
                showMessage(MessageUtils.t("configpanel_language_not_applied_save_and_restart"));
                ((GUISwing) gui).showNotification(MessageUtils.t("configpanel_applywithoutsave"),
                        GUISwing.NotificationType.SUCCESS);
                // if (startServer.isSelected()) showMessage("Modo remoto no se activará, debe
                // guardar y reiniciar la aplicación.");
            }
        });
        btns.add(applywithoutsave);

        JButton btSave = new JButton(MessageUtils.t("configpanel_save"));
        btSave.setToolTipText(MessageUtils.t("configpanel_save"));
        btSave.getAccessibleContext().setAccessibleName(MessageUtils.t("configpanel_save_button_accessible"));
        btSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                saveSettings();
                showMessage(MessageUtils.t("configpanel_language_applied_on_restart"));
                ((GUISwing) gui).showNotification(MessageUtils.t("configpanel_save_done"),
                        GUISwing.NotificationType.SUCCESS);
                // if (startServer.isSelected()) showMessage("El Modo remoto se iniciará al
                // reinicio de la aplicación, puede desactivarlo con el menú contextual obtenido
                // con clic derecho.");
            }
        });
        btns.add(btSave);
    }

    public void refreshAdvancedConfigPanel() {
        LOG.info("Refreshing advanced config panel");
        advancedPanel.setVisible(false);
        advancedPanel.removeAll();
        createAdvancedConfigPanel();
        if (isAdvancedOptions) {
            configPanel.setViewportView(advancedPanel);
        }
        advancedPanel.revalidate();
        advancedPanel.repaint();
    }

    public JLabel addSettingsBox(JPanel panel, String text, JComponent item) {
        return this.addSettingsBox(panel, text, item, new Dimension(150, 30));
    }

    public JLabel addSettingsBox(JPanel panel, String text, JComponent item, Dimension d) {
        JLabel label = new JLabel(text);
        label.setToolTipText(text);
        label.getAccessibleContext().setAccessibleName(text);
        label.getAccessibleContext().setAccessibleDescription(text);
        JPanel itempanel = new JPanel();
        label.setPreferredSize(new Dimension(150, 30));
        itempanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        itempanel.setLayout(new BoxLayout(itempanel, 0));
        itempanel.setOpaque(false);
        itempanel.add(label);
        itempanel.add(item);
        panel.add(itempanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        item.setPreferredSize(d);
        return label;
    }

    public void chargeSettings() {
        settings.simplified_mode = this.isSimplifiedMode.isSelected();
        settings.reason = reason.getText();
        settings.place = place.getText();
        settings.contact = contact.getText();
        settings.dateFormat = this.dateFormat.getText();
        settings.defaultSignMessage = defaultSignMessage.getText();
        settings.withoutVisibleSign = withoutVisibleSign.isSelected();
        settings.isImgWithDpi = isImgWithDpi.isSelected();
        settings.startFimadorRemote = startFimadorRemote.isSelected();
        // settings.useLTA = useLTA.isSelected();
        settings.showLogs = this.showLogs.isSelected();
        settings.overwriteSourceFile = overwriteSourceFile.isSelected();
        settings.pageNumber = Integer.parseInt(pageNumber.getValue().toString());
        settings.signWidth = Integer.parseInt(signWidth.getValue().toString());
        settings.signHeight = Integer.parseInt(signHeight.getValue().toString());
        settings.fontSize = Integer.parseInt(fontSize.getValue().toString());
        settings.signX = Integer.parseInt(signX.getValue().toString());
        settings.signY = Integer.parseInt(signY.getValue().toString());
        settings.signImageWidth = Integer.parseInt(signImageWidth.getValue().toString());
        settings.signImageHeight = Integer.parseInt(signImageHeight.getValue().toString());
        settings.font = font.getSelectedItem().toString();
        settings.fontAlignment = fontPosition.getSelectedItem().toString();
        settings.fontColor = fontColor.getText();
        settings.backgroundColor = backgroundColor.getText();
        settings.image = imageText.getText();
        // settings.startServer = this.startServer.isSelected();
        settings.portNumber = Integer.parseInt(portNumber.getValue().toString());
        settings.pAdESLevel = pAdESLevel.getSelectedItem().toString();
        settings.xAdESLevel = xAdESLevel.getSelectedItem().toString();
        settings.cAdESLevel = cAdESLevel.getSelectedItem().toString();
        settings.jAdESLevel = jAdESLevel.getSelectedItem().toString();
        settings.sofficePath = sofficePath.getText();
        settings.language = language.getSelectedItem().toString();
        settings.preferredBrowser = preferredBrowser.getText();
        settings.startwindowstate = windowstate.getSelectedItem().toString();
        settings.country = settings.countryByLanguage.get(settings.language);
        settings.pDFImgScaleFactor = Float.parseFloat(pDFImgScaleFactor.getText().replace(",", "."));
        settings.pKCS12File = pKCS12Panel.getList();
        settings.extraPKCS11Lib = pKCS11ModuleText.getText();
        if (settings.extraPKCS11Lib.isEmpty())
            settings.extraPKCS11Lib = null;
        settings.activePlugins = pluginsActive.getActivePlugin();
        settings.registeredAllowedOrigins = (registeredAllowedOrigins.getText());
        settings.updateConfig();
    }

    public void restartSettings() {
        Settings settings = new Settings();
        isSimplifiedMode.setSelected(settings.simplified_mode);
        withoutVisibleSign.setSelected(settings.withoutVisibleSign);
        // useLTA.setSelected(settings.useLTA);
        showLogs.setSelected(settings.showLogs);
        overwriteSourceFile.setSelected(settings.overwriteSourceFile);
        isImgWithDpi.setSelected(settings.isImgWithDpi);
        startFimadorRemote.setSelected(settings.startFimadorRemote);
        reason.setText(settings.reason);
        place.setText(settings.place);
        contact.setText(settings.contact);
        dateFormat.setText(settings.dateFormat);
        defaultSignMessage.setText(settings.defaultSignMessage);
        pageNumber.setValue(settings.pageNumber);
        signWidth.setValue(settings.signWidth);
        signHeight.setValue(settings.signHeight);
        signX.setValue(settings.signX);
        signY.setValue(settings.signY);
        signImageWidth.setValue(settings.signImageWidth);
        signImageHeight.setValue(settings.signImageHeight);
        fontSize.setValue(settings.fontSize);
        font.setSelectedItem(settings.font);
        fontColor.setText(settings.fontColor);
        backgroundColor.setText(settings.backgroundColor);
        setIcons(btFontColor, fontColor.getText(), this.settings.getFontColor());
        setIcons(btBackgroundColor, backgroundColor.getText(), this.settings.getBackgroundColor());
        // startServer.setSelected(settings.startServer);
        portNumber.setValue(settings.portNumber);
        fontPosition.setSelectedItem(settings.fontAlignment);
        pAdESLevel.setSelectedItem(settings.pAdESLevel);
        xAdESLevel.setSelectedItem(settings.xAdESLevel);
        cAdESLevel.setSelectedItem(settings.cAdESLevel);
        jAdESLevel.setSelectedItem(settings.jAdESLevel);
        sofficePath.setText(settings.getSofficePath());
        language.setSelectedItem(settings.language);
        preferredBrowser.setText(settings.preferredBrowser);
        windowstate.setSelectedItem(settings.startwindowstate);
        pDFImgScaleFactor.setText(String.format("%.2f", settings.pDFImgScaleFactor));
        if (settings.image != null && !settings.image.trim().isEmpty()) {
            imageText.setText(settings.image);
            btImage.setIcon(this.getIcon(settings.image));
        } else {
            imageText.setText("");
            btImage.setIcon(createImageIcon(new Color(255, 255, 255, 0)));
        }
        if (settings.pKCS12File != null)
            pKCS12Panel.setList(settings.pKCS12File);
        if (settings.extraPKCS11Lib != null)
            pKCS11ModuleText.setText(settings.extraPKCS11Lib);
        else
            pKCS11ModuleText.setText("");
        pluginsActive.loadPlugins(settings);

    }

    private void setIcons(JButton component, String text, Color color) {
        if (text.equalsIgnoreCase("transparente"))
            component.setIcon(getTransparentImageIcon());
        else
            component.setIcon(createImageIcon(color));
    }

    public void saveSettings() {
        chargeSettings();
        this.manager.setSettings(this.settings, true);
    }

    public void showFontColorPicker() {
        Color newColor = JColorChooser.showDialog(this, MessageUtils.t("configpanel_text_color"),
                this.settings.getFontColor());
        if (newColor != null) {
            String buf = Integer.toHexString(newColor.getRGB());
            String hex = "#" + buf.substring(buf.length() - 6);
            fontColor.setText(hex);
        }
    }

    public void showBackgroundColorPicker() {
        Color newColor = JColorChooser.showDialog(this, MessageUtils.t("configpanel_background_color"),
                this.settings.getBackgroundColor());
        if (newColor != null) {
            String buf = Integer.toHexString(newColor.getRGB());
            String hex = "#" + buf.substring(buf.length() - 6);
            backgroundColor.setText(hex);
        }
    }

    public String getFilePath() {
        FileDialog loadDialog = new FileDialog((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, this),
                MessageUtils.t("configpanel_select_a_file"));
        loadDialog.setMultipleMode(false);
        loadDialog.setLocationRelativeTo(null);
        loadDialog.setVisible(true);
        loadDialog.dispose();
        File[] files = loadDialog.getFiles();
        if (files.length > 0)
            return files[0].toString();
        else
            return null;
    }

    public void showExecPicker() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle(MessageUtils.t("configpanel_select_browser"));

        // Filtro para ejecutables según el SO
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".exe");
                }

                public String getDescription() {
                    return "Ejecutables (*.exe)";
                }
            });
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            preferredBrowser.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    public void showImagePicker() {
        FileDialog imageDialog = new FileDialog((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, this),
                MessageUtils.t("configpanel_select_an_image"));
        imageDialog.setFilenameFilter(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg")
                        || name.toLowerCase().endsWith(".jpeg")
                        || name.toLowerCase().endsWith(".gif") || name.toLowerCase().endsWith(".tif")
                        || name.toLowerCase().endsWith(".tiff");
            }
        });
        imageDialog.setFile("*.png;*.jpg;*.jpeg;*.gif;*.tif;*.tiff");
        imageDialog.setLocationRelativeTo(null);
        imageDialog.setVisible(true);
        imageDialog.dispose();
        if (imageDialog.getFile() != null) {
            imageText.setText(imageDialog.getDirectory() + imageDialog.getFile());
            btImage.setIcon(this.getIcon(imageText.getText()));
        }
    }

    public Icon getIcon(String path) {
        return new ImageIcon(new ImageIcon(path).getImage().getScaledInstance(iconSize, iconSize, Image.SCALE_DEFAULT));
    }

    public Icon getTransparentImageIcon() {
        BufferedImage image = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(0, 0, 0, 100));
        for (int x = 4; x < 28; x += 8)
            for (int y = 4; y < 28; y += 8)
                graphics.fillRect(x, y, 4, 4);
        graphics.setColor(new Color(130, 130, 130));
        for (int x = 8; x < 28; x += 8)
            for (int y = 8; y < 28; y += 8)
                graphics.fillRect(x, y, 4, 4);
        return new ImageIcon(image);
    }

    public ImageIcon createImageIcon(Color color) {
        BufferedImage image = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setPaint(new Color(255, 255, 255, 0));
        graphics.setBackground(new Color(255, 255, 255, 0));
        graphics.fillRect(0, 0, iconSize, iconSize);
        graphics.setColor(color);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Ellipse2D.Float circle = new Ellipse2D.Float(4F, 6F, iconSize - 15, iconSize - 15);
        graphics.fill(circle);
        return new ImageIcon(image);
    }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(null, new CopyableJLabel(message), MessageUtils.t("configpanel_Firmador_message"),
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void setGui(GUIInterface gui) {
        this.gui = gui;
    }

}
