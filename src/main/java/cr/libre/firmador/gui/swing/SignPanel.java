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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
//import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.accessibility.AccessibleState;
//import javax.accessibility.AccessibleContext;
//import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cr.libre.firmador.signers.FirmadorCAdES;
import cr.libre.firmador.signers.FirmadorJAdES;
import cr.libre.firmador.signers.FirmadorXAdES;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cr.libre.firmador.cards.CardSignInfo;
import cr.libre.firmador.ConfigListener;
import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.Settings;
import cr.libre.firmador.SettingsManager;
import cr.libre.firmador.cards.SmartCardDetector;
import cr.libre.firmador.cards.SmartCardListener;
import cr.libre.firmador.documents.Document;
import cr.libre.firmador.documents.SupportedMimeTypeEnum;
import cr.libre.firmador.gui.GUIInterface;
import cr.libre.firmador.gui.GUISwing;
import cr.libre.firmador.previewers.PreviewerInterface;
import cr.libre.firmador.signers.FirmadorUtils;

public class SignPanel extends JPanel implements ConfigListener, SmartCardListener {
    @SuppressWarnings("serial")
    final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final long serialVersionUID = 945116850482545687L;

    @SuppressWarnings("unused")
    private static class CheckBoxListener implements PropertyChangeListener { //FIXME is this actually used?
        @Override
        public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if ("AccessibleState".equals(propertyName)) {
                AccessibleState state = (AccessibleState) e.getNewValue();
                if (state == AccessibleState.CHECKED) {
                    System.out.println("Se ha seleccionado el JCheckBox");
                } else {
                    System.out.println("Se ha deseleccionado el JCheckBox");
                }
            }
        }
    }

    private JScrollPane imgScroll;
    private ScrollableJPanel imagePanel;
    private JLabel imageLabel;
    private JLabel signatureLabel;
    private JCheckBox signatureVisibleCheckBox, isImgWithDPI;
    private JButton positionLabel;
    private JLabel positionDescriptionLabel;
    private JLabel reasonLabel;
    private JLabel locationLabel;
    private JLabel contactInfoLabel;
    private JTextField reasonField;
    private JTextField locationField;
    private JTextField contactInfoField;
    private JLabel pageLabel;
    private JLabel information_message;
    private JSpinner pageSpinner;
    private JLabel AdESFormatLabel;
    private ButtonGroup AdESFormatButtonGroup;
    private JRadioButton CAdESButton;
    private JRadioButton XAdESButton;
    private JRadioButton JAdESButton;
    private JButton signButton;
    private JButton saveButton;
    private JButton validateButton;
    private JLabel AdESLevelLabel;
    private JRadioButton levelTButton;
    private JRadioButton levelLTButton;
    private JRadioButton levelLTAButton;
    private ButtonGroup AdESLevelButtonGroup;
    @SuppressWarnings("serial")
    protected Settings settings;
    @SuppressWarnings("serial")
    public GUIInterface gui;
    @SuppressWarnings("serial")
    private SmartCardDetector smartCardDetector;
    @SuppressWarnings("serial")
    private PreviewerInterface preview;
    @SuppressWarnings("serial")
    private Document currentDocument = null;
    private JRadioButton ASICEButton;
    @SuppressWarnings("serial")
    private List<CardSignInfo> cards = null;
    private boolean isInitializingSpinner = false;
    @SuppressWarnings("serial")
    private Map<Integer, BufferedImage> imageCache = new HashMap<>();
    private String currentDocumentId = null;
    private JLabel validateLabel;

    public void setGUI(GUIInterface gui) {
        this.gui = gui;
    }

    public void setDocument(Document document) {
        String newDocId = document.getPathName() + document.hashCode();
        if (currentDocumentId == null || !currentDocumentId.equals(newDocId)) {
            imageCache.clear();
            currentDocumentId = newDocId;
        }

        currentDocument = document;
        SupportedMimeTypeEnum mimeType = document.getMimeType();

        hideButtons();
        if (mimeType.isPDF()) {
            showSignButtons();
            signatureLabel.setVisible(true);
        } else if (mimeType.isOpenxmlformats() || mimeType.isOpenDocument()) {
            getSignButton().setEnabled(true);
            saveButton.setEnabled(true);
        } else {
            shownonPDFButtons();
        }
    }

    public void setPreview(PreviewerInterface preview) {
        this.preview = preview;
        int pages = 1;
        if (currentDocument.isVirtual()) {
            pages = currentDocument.getPages();
        } else {
            pages = preview.getNumberOfPages();
        }
        if (pages > 0) {
            isInitializingSpinner = true;
            SpinnerNumberModel model = ((SpinnerNumberModel) this.getPageSpinner().getModel());
            model.setMinimum(pages - (pages * 2));
            model.setMaximum(pages);
            if (settings.pageNumber <= pages && settings.pageNumber > 0)
                this.getPageSpinner().setValue(settings.pageNumber);
            else
                this.getPageSpinner().setValue(1);
            // signPanel.paintPDFViewer();
            isInitializingSpinner = false;
        }
    }

    public JScrollPane getImageScrollPane(ScrollableJPanel panel) {
        JScrollPane imgScrollPane = new JScrollPane();
        imgScrollPane.setPreferredSize(new Dimension(100, 200));
        imgScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        imgScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        imgScrollPane.setBorder(null);
        imgScrollPane.setViewportView(panel);
        imgScrollPane.setOpaque(false);
        imgScrollPane.getViewport().setOpaque(false);
        imgScrollPane.setVisible(true);
        // Accesibilidad: Área de vista previa del documento
        imgScrollPane.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_preview_scroll_area"));
        return imgScrollPane;
    }

    @SuppressWarnings("this-escape")
    public SignPanel() {
        super();
        settings = SettingsManager.getInstance().getAndCreateSettings();
        signatureVisibleCheckBox = new JCheckBox(MessageUtils.t("signpanel_visible_checkbox"),
                settings.withoutVisibleSign);
        signatureVisibleCheckBox.getAccessibleContext().setAccessibleName(MessageUtils.t("signpanel_visible_checkbox"));
        signatureVisibleCheckBox.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("signpanel_visible_checkbox_accessible"));
        signatureVisibleCheckBox.setToolTipText(MessageUtils.t("signpanel_visible_checkbox_tooltip"));
        signatureVisibleCheckBox.setOpaque(false);

        positionLabel = new JButton("X: Y: ");
        // Accesibilidad: Etiqueta de posición de firma
        positionLabel.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_position_value_label"));
        positionLabel.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("accessibility_position_value_description"));

        positionDescriptionLabel = new JLabel("Posición: ");
        // Accesibilidad: Descripción de posición
        positionDescriptionLabel.getAccessibleContext().setAccessibleName(null);

        reasonLabel = new JLabel(MessageUtils.t("signpanel_reason"));
        // Accesibilidad: Etiqueta de razón
        reasonLabel.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_reason_label"));

        locationLabel = new JLabel(MessageUtils.t("signpanel_place"));
        // Accesibilidad: Etiqueta de ubicación
        locationLabel.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_location_label"));

        contactInfoLabel = new JLabel(MessageUtils.t("signpanel_contact"));
        // Accesibilidad: Etiqueta de contacto
        contactInfoLabel.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_contact_label"));

        reasonField = new JTextField();
        reasonField.setText(settings.reason);
        reasonField.setToolTipText(MessageUtils.t("signpanel_reason_tooltip"));
        reasonField.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("signpanel_reason_tooltip_accessible"));
        // Accesibilidad: Campo de razón
        reasonField.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_reason_field"));

        locationField = new JTextField();
        locationField.setText(settings.place);
        locationField.setToolTipText(MessageUtils.t("signpanel_place_tooltip"));
        locationField.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("signpanel_place_tooltip_accessible"));
        // Accesibilidad: Campo de ubicación
        locationField.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_location_field"));

        contactInfoField = new JTextField();
        contactInfoField.setText(settings.contact);
        contactInfoField.setToolTipText(MessageUtils.t("signpanel_contact_tooltip"));
        contactInfoField.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("signpanel_contact_tooltip_accessible"));
        // Accesibilidad: Campo de contacto
        contactInfoField.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_contact_field"));

        AdESFormatLabel = new JLabel(MessageUtils.t("signpanel_formato_ades"));
        // Accesibilidad: Etiqueta de formato AdES
        AdESFormatLabel.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_ades_format_label"));

        CAdESButton = new JRadioButton("CAdES");
        CAdESButton.setActionCommand("CAdES");
        CAdESButton.setContentAreaFilled(false);
        // Accesibilidad: Botón CAdES
        CAdESButton.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_cades_button"));

        XAdESButton = new JRadioButton("XAdES", true);
        XAdESButton.setActionCommand("XAdES");
        XAdESButton.setContentAreaFilled(false);
        // Accesibilidad: Botón XAdES
        XAdESButton.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_xades_button"));

        JAdESButton = new JRadioButton("JAdES ");
        JAdESButton.setActionCommand("JAdES");
        JAdESButton.setContentAreaFilled(false);
        // Accesibilidad: Botón JAdES
        JAdESButton.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_jades_button"));

        ASICEButton = new JRadioButton("ASiC-E");
        ASICEButton.setActionCommand("ASiC-E");
        ASICEButton.setContentAreaFilled(false);
        // Accesibilidad: Botón ASiC-E
        ASICEButton.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_asice_button"));

        AdESFormatButtonGroup = new ButtonGroup();
        AdESFormatButtonGroup.add(CAdESButton);
        AdESFormatButtonGroup.add(XAdESButton);
        AdESFormatButtonGroup.add(JAdESButton);
        AdESFormatButtonGroup.add(ASICEButton);

        AdESLevelLabel = new JLabel(MessageUtils.t("signpanel_level_ades"));
        // Accesibilidad: Etiqueta de nivel AdES
        AdESLevelLabel.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_ades_level_label"));

        levelTButton = new JRadioButton("T");
        levelTButton.setActionCommand("T");
        levelTButton.setContentAreaFilled(false);
        // Accesibilidad: Botón nivel T
        levelTButton.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_level_t_button"));

        levelLTButton = new JRadioButton("LT");
        levelLTButton.setActionCommand("LT");
        levelLTButton.setContentAreaFilled(false);
        // Accesibilidad: Botón nivel LT
        levelLTButton.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_level_lt_button"));

        levelLTAButton = new JRadioButton("LTA", true);
        levelLTAButton.setActionCommand("LTA");
        levelLTAButton.setContentAreaFilled(false);
        // Accesibilidad: Botón nivel LTA
        levelLTAButton.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_level_lta_button"));

        AdESLevelButtonGroup = new ButtonGroup();
        AdESLevelButtonGroup.add(levelTButton);
        AdESLevelButtonGroup.add(levelLTButton);
        AdESLevelButtonGroup.add(levelLTAButton);

        pageLabel = new JLabel(MessageUtils.t("signpanel_page"));
        // Accesibilidad: Etiqueta de página
        pageLabel.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_page_label"));
        pageSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        pageSpinner.setToolTipText("<html>" + MessageUtils.t("signpanel_page_tooltip"));
        pageSpinner.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("signpanel_page_tooltip_accessible"));
        pageSpinner.setMaximumSize(pageSpinner.getPreferredSize());
        pageSpinner.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_page_spinner"));

        signatureLabel = new JLabel();
        // FIXME partially dead code?
        signatureLabel.setFont(new Font(settings.getFontName(settings.font, false),
                settings.getFontStyle(settings.font), settings.fontSize));
        signatureLabel
                .setText("<html><span style='font-size: '" + settings.fontSize * settings.pDFImgScaleFactor + "pt'" +
                        "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;FIRMA<br>" +
                        "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;VISIBLE</span></html>");
        signatureLabel.setForeground(new Color(0, 0, 0, 0));
        signatureLabel.setBackground(new Color(127, 127, 127, 127));
        signatureLabel.setOpaque(true);
        // Accesibilidad: Etiqueta de firma visible
        signatureLabel.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_signature_label"));

        imagePanel = new ScrollableJPanel(false, false);

        imageLabel = new JLabel();
        // Accesibilidad: Etiqueta de imagen del documento
        imageLabel.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_image_label"));

        signatureLabel.setBounds((int) ((float) settings.signX * settings.pDFImgScaleFactor),
                (int) ((float) settings.signY * settings.pDFImgScaleFactor),
                (int) ((float) settings.signWidth * settings.pDFImgScaleFactor),
                (int) ((float) settings.signHeight * settings.pDFImgScaleFactor));

        imageLabel.add(signatureLabel);
        imagePanel.add(imageLabel);
        imgScroll = this.getImageScrollPane(imagePanel);

        isImgWithDPI = new JCheckBox("Reescalar con DPI", settings.isImgWithDpi);
        isImgWithDPI.setToolTipText(
                "<html>Marque esta casilla si desea reescalar la imagen para una mejor resolucion en monitores con un DPI alto.</html>");
        isImgWithDPI.setOpaque(false);
        // Accesibilidad: Checkbox de DPI
        isImgWithDPI.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_dpi_checkbox"));

        isImgWithDPI.addActionListener(e -> {
            settings.isImgWithDpi = isImgWithDPI.isSelected();
            renderPreviewViewer(getNumberPageSpinner());
        });

        signButton = new JButton(MessageUtils.t("signpanel_sign_btn"));
        signButton.setToolTipText(MessageUtils.t("signpanel_sign_tooltip"));
        signButton.getAccessibleContext().setAccessibleDescription(MessageUtils.t("signpanel_sign_tooltip_accessible"));
        // Accesibilidad: Botón firmar
        signButton.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_sign_button"));

        signButton.setOpaque(false);
        signButton.setMnemonic(MessageUtils.k('S'));

        saveButton = new JButton(MessageUtils.t("signpanel_save_btn"));
        saveButton.setToolTipText(MessageUtils.t("signpanel_save_tooltip"));
        saveButton.getAccessibleContext().setAccessibleDescription(MessageUtils.t("signpanel_save_tooltip_accessible"));
        // Accesibilidad: Botón guardar configuración
        saveButton.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_save_config_button"));

        saveButton.setOpaque(false);
        saveButton.setMnemonic(MessageUtils.k('G'));

        validateLabel = new JLabel(
                MessageUtils.t("signpanel_validate_label") + " 0");
        validateLabel.getAccessibleContext().setAccessibleName(MessageUtils.t("signpanel_validate_label_accessible"));
        validateLabel.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("signpanel_validate_label_description"));

        validateButton = new JButton(MessageUtils.t("signpanel_validate_btn"));
        validateButton.setToolTipText(MessageUtils.t("signpanel_validate_tooltip"));
        validateButton.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("signpanel_validate_tooltip_accessible"));
        validateButton.setOpaque(false);
        validateButton.setMnemonic(MessageUtils.k('V'));

        // signatureLabel.setToolTipText("<html>Esta etiqueta es un recuadro arrastrable
        // que representa<br>la ubicación de la firma visible en la página
        // seleccionada.<br><br>Se puede cambiar su posición haciendo clic sobre el
        // recuadro<br>y moviendo el mouse sin soltar el botón de clic<br>hasta soltarlo
        // en la posición deseada.</html>");
        if (System.getProperty("os.name").startsWith("Mac"))
            signatureLabel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        else
            signatureLabel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

        information_message = new JLabel();
        // Accesibilidad: Mensaje de información
        information_message.getAccessibleContext().setAccessibleName(MessageUtils.t("accessibility_info_message"));
        information_message.getAccessibleContext()
                .setAccessibleDescription(MessageUtils.t("accessibility_info_message_description"));

        this.setOpaque(false);

        // initializeActions();
    }

    public int getNumberPageSpinner() {
        if ((int) pageSpinner.getValue() > 0) {
            return (int) pageSpinner.getValue() - 1;
        } else {
            int pages = 1;
            if (currentDocument.isVirtual()) {
                pages = currentDocument.getPages();
            } else {
                pages = preview.getNumberOfPages();
            }
            if (pages + (int) pageSpinner.getValue() < 0) {
                return pages - 1;
            }
            return pages + (int) pageSpinner.getValue();
        }
    }

    public Rectangle calculateSignatureRectangle() {
        Rectangle reg = new Rectangle(getPDFVisibleSignatureX(),
                getPDFVisibleSignatureY(),
                signatureLabel.getWidth(),
                signatureLabel.getHeight());

        return reg;

    }

    public float getScaleFactor() {
        float scaleFactor = 1;
        if (settings.isImgWithDpi || isImgWithDPI.isSelected()) {
            scaleFactor = settings.scaleFactorDpi;
        } else {
            scaleFactor = settings.pDFImgScaleFactor;
        }
        return scaleFactor;
    }

    public int getPDFVisibleSignatureX() {
        return (int) ((float) signatureLabel.getLocation().x / getScaleFactor());
    }

    public int getPDFVisibleSignatureY() {
        return (int) ((float) signatureLabel.getLocation().y / getScaleFactor());
    }

    public void paintPDFViewer() {
        int page = getNumberPageSpinner();
        renderPreviewViewer(page);
    }

    public void renderPreviewViewer(int page) {
        if (preview != null) {
            int totalPages = currentDocument.isVirtual()
                    ? currentDocument.getPages()
                    : preview.getNumberOfPages();

            int actualPage = page < 0 ? totalPages + page + 1 : page;

            if (actualPage >= 0 && actualPage < totalPages) {
                if (currentDocument.isVirtual()) {
                    if (imageCache.containsKey(actualPage)) {
                        BufferedImage pageImage = imageCache.get(actualPage);
                        LOG.info("Imagen cargada desde caché: página " + actualPage);
                        updateImageLabel(pageImage);
                    } else {
                        SwingWorker<BufferedImage, Void> worker = new SwingWorker<BufferedImage, Void>() {
                            @Override
                            protected BufferedImage doInBackground() throws Exception {
                                return ((GUISwing) gui).getPageImageFromApi(currentDocument, actualPage);
                            }

                            @Override
                            protected void done() {
                                try {
                                    BufferedImage pageImage = get();
                                    imageCache.put(actualPage, pageImage);
                                    LOG.info("Imagen cargada desde API y guardada en caché: página " + actualPage);
                                    updateImageLabel(pageImage);
                                } catch (Exception ex) {
                                    LOG.error(MessageUtils.t("signpanel_log_render_preview"), ex);
                                    gui.showError(FirmadorUtils.getRootCause(ex));
                                } finally {
                                    ((GUISwing) gui).desativateLoadDialog();
                                }
                            }
                        };

                        ((GUISwing) gui).getLoadDialogWorker().setVisible(true);
                        ((GUISwing) gui).getLoadDialogWorker().setTitle(MessageUtils.t("signpanel_load_image"));
                        worker.execute();
                    }
                } else {
                    try {
                        BufferedImage pageImage = renderLocalPDF(actualPage);
                        updateImageLabel(pageImage);
                    } catch (Exception ex) {
                        LOG.error(MessageUtils.t("signpanel_log_render_preview"), ex);
                        gui.showError(FirmadorUtils.getRootCause(ex));
                    }
                }
            }
        }
    }

    private BufferedImage renderLocalPDF(int page) throws Exception {
        PDFRenderer renderer = preview.getRender();

        if (settings.isImgWithDpi) {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            int dpi = toolkit.getScreenResolution();
            BufferedImage img = renderer.renderImageWithDPI(page, dpi);

            settings.setScaleFactorDpi(
                    (float) img.getWidth() /
                            (float) renderer.renderImage(page, settings.pDFImgScaleFactor).getWidth());
            return img;
        } else {
            return renderer.renderImage(page, settings.pDFImgScaleFactor);
        }
    }

    private void updateImageLabel(BufferedImage pageImage) {
        if (pageImage != null) {
            imageLabel.setSize(pageImage.getWidth(), pageImage.getHeight());
            imageLabel.setIcon(new ImageIcon(pageImage));
        }

        if (preview.showSignLabelPreview()) {
            previewSignLabel();
        } else {
            signatureLabel.setVisible(false);
        }

        showPreviewButtons();
    }

    public void previewSignLabel() {
        String previewimg = settings.getImage();
        String table;
        float scaleFactor = getScaleFactor();
        if (previewimg != null) {
/*
            // FIXME this seems unused
            BufferedImage bufferedImage = null;
            try {
                bufferedImage = ImageIO.read(new URI(previewimg).toURL());
            } catch (Exception e) {
                LOG.error(MessageUtils.t("signpanel_error_render_image"), e);
                gui.showError(FirmadorUtils.getRootCause(e));
            }
*/

            int previewimgWidth = settings.signImageWidth;
            int previewimgHeight = settings.signImageHeight;

            table = "<table cellpadding=0 cellspacing=0 border=0>";
            if ("ONLY IMAGE".equals(settings.fontAlignment)) {
                table += "<tr><td style='text-align: center'><img src=\"" + settings.getImage() + "\" width=\""
                        + previewimgWidth + "\" height=\"" + previewimgHeight + "\"></td></tr>";
            } else if (settings.fontAlignment.contains("BOTTOM")) {
                table += "<tr><td><img src=\"" + settings.getImage() + "\" width=\"" + previewimgWidth + "\" height=\""
                        + previewimgHeight + "\"></td></tr>";
                table += "<tr><td><span style='font-size: " + (settings.fontSize * scaleFactor) + "pt'>"
                        + getTextExample() + "</span></td></tr>";
            } else if (settings.fontAlignment.contains("LEFT")) {
                table += "<tr><td><span style='font-size: " + (settings.fontSize * scaleFactor) + "pt'>"
                        + getTextExample() + "</span></td>";
                table += "<td><img src=\"" + settings.getImage() + "\" width=\"" + previewimgWidth + "\" height=\""
                        + previewimgHeight + "\"></td></tr>";
            } else if (settings.fontAlignment.contains("TOP")) {
                table += "<tr><td><span style='font-size: " + (settings.fontSize * scaleFactor) + "pt'>"
                        + getTextExample() + "</span></td></tr>";
                table += "<tr><td><img src=\"" + settings.getImage() + "\" width=\"" + previewimgWidth + "\" height=\""
                        + previewimgHeight + "\"></td></tr>";
            } else {
                table += "<tr><td><img src=\"" + settings.getImage() + "\" width=\"" + previewimgWidth + "\" height=\""
                        + previewimgHeight + "\"></td>";
                table += "<td><span style='font-size: " + (settings.fontSize * scaleFactor) + "pt'>" + getTextExample()
                        + "</span></td></tr>";
            }
            table += "</table>";
        } else {
            table = "<span style='font-size: " + settings.fontSize * scaleFactor + "pt'>" + getTextExample()
                    + "</span>";
        }
        signatureLabel.setFont(new Font(settings.getFontName(settings.font, false),
                settings.getFontStyle(settings.font), settings.fontSize));
        signatureLabel.setText("<html>" + table + "</html>");
        signatureLabel.setForeground(new Color(0, 0, 0, 0));
        signatureLabel.setBackground(new Color(127, 127, 127, 127));
        signatureLabel.setOpaque(true);
        signatureLabel.setSize(signatureLabel.getPreferredSize());

    }

    public void initializeActions() {
        positionLabel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showPositionModal();
            }
        });

        imageLabel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                int newX = e.getX() - signatureLabel.getWidth() / 2;
                int newY = e.getY() - signatureLabel.getHeight() / 2;

                int maxX = imageLabel.getWidth() - signatureLabel.getWidth();
                int maxY = imageLabel.getHeight() - signatureLabel.getHeight();

                newX = Math.max(0, Math.min(newX, maxX));
                newY = Math.max(0, Math.min(newY, maxY));

                signatureLabel.setLocation(newX, newY);
                positionLabel.setText(String.format("X: %d - Y: %d", newX, newY));
            }
        });

        imageLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 3) {
                    previewSignLabel();
                } else if (evt.getClickCount() == 2) {
                    int newX = evt.getX() - signatureLabel.getWidth() / 2;
                    int newY = evt.getY() - signatureLabel.getHeight() / 2;

                    int maxX = imageLabel.getWidth() - signatureLabel.getWidth();
                    int maxY = imageLabel.getHeight() - signatureLabel.getHeight();

                    newX = Math.max(0, Math.min(newX, maxX));
                    newY = Math.max(0, Math.min(newY, maxY));

                    signatureLabel.setLocation(newX, newY);
                    positionLabel.setText(String.format("X: %d - Y: %d", newX, newY));
                }
            }
        });

        pageSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (isInitializingSpinner)
                    return;
                int value = (int) pageSpinner.getValue();
                if (value == 0) {
                    Object last = ((JSpinner.DefaultEditor) pageSpinner.getEditor()).getTextField()
                            .getClientProperty("lastValue");
                    int lastValue = 1;
                    if (last instanceof Integer)
                        lastValue = (Integer) last;
                    if (lastValue > 0) {
                        pageSpinner.setValue(-1);
                    } else {
                        pageSpinner.setValue(1);
                    }
                } else {
                    ((JSpinner.DefaultEditor) pageSpinner.getEditor()).getTextField().putClientProperty("lastValue",
                            value);
                }
                paintPDFViewer();
            }
        });

        signButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                Settings settings = gui.getCurrentSettings();
                currentDocument.setSettings(settings);
                String savefile = null;
                String suffix = "";
                if (!settings.overwriteSourceFile) {
                    suffix = "-firmado";
                }
                if (!currentDocument.getIsremote()) {
                    savefile = ((GUISwing) gui).showSaveDialog(currentDocument.getPathName(), suffix,
                            currentDocument.getExtension());
                    if (savefile != null) { // cancel option
                        currentDocument.setPathToSave(savefile);
                    }
                }
                gui.signDocument(currentDocument);
                if (currentDocument.isVirtual()) {
                    clean();
                    ((GUISwing) gui).displayFunctionality("document");
                }
            }
        });
        saveButton.addActionListener(new

        ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (currentDocument != null) {
                    currentDocument.setSettings(gui.getCurrentSettings());
                    gui.showMessage(MessageUtils.t("signpanel_dialog_save_configuration"));
                }
            }
        });

        validateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (settings.isSimplifiedMode()) {
                    gui.displayFunctionality("validate");
                } else {
                    loadReportDocument();
                }
            }
        });

    }

    public void loadReportDocument() {
        if (currentDocument != null) {
            gui.displayFunctionality("document");
            ((GUISwing) gui).loadReportDocument(currentDocument, false);
        }
    }

    public void createLayout(GroupLayout signLayout, JPanel signPanel) {
        this.setOpaque(false);
        this.setLayout(signLayout);
        signLayout.setAutoCreateGaps(true);
        signLayout.setAutoCreateContainerGaps(true);
        signLayout.setHorizontalGroup(
                signLayout.createSequentialGroup()
                        .addComponent(imgScroll, 200, 600, GroupLayout.PREFERRED_SIZE)
                        .addGroup(signLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                .addComponent(pageLabel)
                                .addComponent(positionDescriptionLabel)
                                .addComponent(reasonLabel)
                                .addComponent(locationLabel)
                                .addComponent(contactInfoLabel)
                                .addComponent(AdESFormatLabel)
                                .addComponent(AdESLevelLabel))
                        .addGroup(signLayout.createParallelGroup()
                                .addGroup(signLayout.createSequentialGroup()
                                        .addComponent(pageSpinner)
                                        .addComponent(signatureVisibleCheckBox)
                                        .addComponent(isImgWithDPI))
                                .addComponent(positionLabel)
                                .addComponent(reasonField)
                                .addComponent(locationField)
                                .addComponent(contactInfoField)
                                .addGroup(signLayout.createSequentialGroup()
                                        .addComponent(CAdESButton)
                                        .addComponent(XAdESButton)
                                        .addComponent(JAdESButton)
                                        .addComponent(ASICEButton))
                                .addGroup(signLayout.createSequentialGroup()
                                        .addComponent(levelTButton)
                                        .addComponent(levelLTButton)
                                        .addComponent(levelLTAButton)
                                        .addComponent(signButton).addComponent(saveButton))
                                .addGroup(signLayout.createSequentialGroup()
                                        .addComponent(validateLabel)
                                        .addComponent(validateButton))
                                .addGroup(signLayout.createSequentialGroup()
                                        .addComponent(information_message))));

        signLayout.setVerticalGroup(
                signLayout.createParallelGroup()
                        .addComponent(imgScroll)
                        .addGroup(signLayout.createSequentialGroup()
                                .addGroup(signLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(pageSpinner)
                                        .addComponent(signatureVisibleCheckBox)
                                        .addComponent(pageLabel)
                                        .addComponent(isImgWithDPI))
                                .addGroup(signLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(positionDescriptionLabel).addComponent(positionLabel))
                                .addGroup(signLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(reasonField)
                                        .addComponent(reasonLabel))
                                .addGroup(signLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(locationField)
                                        .addComponent(locationLabel))
                                .addGroup(signLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(contactInfoField)
                                        .addComponent(contactInfoLabel))
                                .addGroup(signLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(AdESFormatLabel)
                                        .addComponent(CAdESButton)
                                        .addComponent(XAdESButton)
                                        .addComponent(JAdESButton)
                                        .addComponent(ASICEButton))
                                .addGroup(signLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(AdESLevelLabel)
                                        .addComponent(levelTButton)
                                        .addComponent(levelLTButton)
                                        .addComponent(levelLTAButton)
                                        .addComponent(signButton).addComponent(saveButton))
                                .addGroup(signLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(validateLabel)
                                        .addComponent(validateButton))
                                .addGroup(signLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(information_message))));
    }

    public void hideButtons() {
        signButton.setEnabled(false);
        pageLabel.setVisible(false);
        pageSpinner.setVisible(false);
        signatureVisibleCheckBox.setVisible(false);
        positionDescriptionLabel.setVisible(false);
        isImgWithDPI.setVisible(false);
        positionLabel.setVisible(false);
        reasonLabel.setVisible(false);
        reasonField.setVisible(false);
        locationLabel.setVisible(false);
        locationField.setVisible(false);
        contactInfoLabel.setVisible(false);
        contactInfoField.setVisible(false);
        AdESFormatLabel.setVisible(false);
        CAdESButton.setVisible(false);
        XAdESButton.setVisible(false);
        JAdESButton.setVisible(false);
        ASICEButton.setVisible(false);
        AdESLevelLabel.setVisible(false);
        levelTButton.setVisible(false);
        levelLTButton.setVisible(false);
        levelLTAButton.setVisible(false);
        saveButton.setVisible(false);
        validateLabel.setVisible(false);
        validateButton.setVisible(false);
        signatureLabel.setVisible(false);
    }

    public void showPreviewButtons() {
        imagePanel.setVisible(true);
        imageLabel.setVisible(true);
        pageLabel.setVisible(true);
        pageSpinner.setVisible(true);
        imagePanel.setBorder(new LineBorder(Color.BLACK));
        validateLabel.setVisible(true);
        validateButton.setVisible(true);
        this.seeNumberOfSignatures();
    }

    public void showSignButtons() {
        showPreviewButtons();
        signatureVisibleCheckBox.setVisible(true);
        isImgWithDPI.setVisible(true);
        saveButton.setVisible(true);

        reasonLabel.setVisible(true);
        reasonField.setVisible(true);
        locationLabel.setVisible(true);
        locationField.setVisible(true);
        contactInfoLabel.setVisible(true);
        contactInfoField.setVisible(true);
        positionDescriptionLabel.setVisible(true);
        positionLabel.setVisible(true);

        /*
         * AdESLevelLabel.setVisible(true);
         * levelTButton.setVisible(true);
         * levelLTButton.setVisible(true);
         * levelLTAButton.setVisible(true);
         */
    }

    public void shownonPDFButtons() {

        if (currentDocument != null) {
            AdESFormatLabel.setVisible(true);
            signButton.setEnabled(true);
            saveButton.setEnabled(true);
            saveButton.setVisible(true);
            validateButton.setVisible(true);
            validateLabel.setVisible(true);
            this.seeNumberOfSignatures();
            if (currentDocument.getSigner() != null) {
                if (currentDocument.getSigner() instanceof FirmadorCAdES) {
                    CAdESButton.setVisible(true);
                    CAdESButton.setSelected(true);
                } else if (currentDocument.getSigner() instanceof FirmadorXAdES) {
                    XAdESButton.setVisible(true);
                    XAdESButton.setSelected(true);
                } else if (currentDocument.getSigner() instanceof FirmadorJAdES) {
                    JAdESButton.setVisible(true);
                    JAdESButton.setSelected(true);
                } else {
                    CAdESButton.setVisible(true);
                    ASICEButton.setSelected(true);
                }
                ASICEButton.setVisible(true);
                AdESLevelLabel.setVisible(false);
                levelTButton.setVisible(false);
                levelLTButton.setVisible(false);
                levelLTAButton.setVisible(false);
            }
        }
    }

    public void docHideButtons() {
        imagePanel.setVisible(false);
        imageLabel.setVisible(false);
        pageLabel.setVisible(false);
        pageSpinner.setVisible(false);
        signatureVisibleCheckBox.setVisible(false);
        isImgWithDPI.setVisible(false);
        positionDescriptionLabel.setVisible(false);
        positionLabel.setVisible(false);
        reasonLabel.setVisible(false);
        reasonField.setVisible(false);
        locationLabel.setVisible(false);
        locationField.setVisible(false);
        contactInfoLabel.setVisible(false);
        contactInfoField.setVisible(false);
        AdESFormatLabel.setVisible(false);
        CAdESButton.setVisible(false);
        XAdESButton.setVisible(false);
        JAdESButton.setVisible(false);
        ASICEButton.setVisible(false);
        AdESLevelLabel.setVisible(false);
        levelTButton.setVisible(false);
        levelLTButton.setVisible(false);
        levelLTAButton.setVisible(false);
        saveButton.setVisible(false);
        validateButton.setVisible(false);
        validateLabel.setVisible(false);
    }

    public void updateConfig() {
        signatureVisibleCheckBox.setSelected(settings.withoutVisibleSign);
        reasonField.setText(settings.reason);
        locationField.setText(settings.place);
        contactInfoField.setText(settings.contact);
        signatureLabel.setBounds((int) ((float) settings.signX * settings.pDFImgScaleFactor),
                (int) ((float) settings.signY * settings.pDFImgScaleFactor),
                (int) ((float) settings.signWidth * settings.pDFImgScaleFactor),
                (int) ((float) settings.signHeight * settings.pDFImgScaleFactor));

        try {
            if (preview != null) {
                int pages = 1;
                if (currentDocument.isVirtual()) {
                    pages = currentDocument.getPages();
                } else {
                    pages = preview.getNumberOfPages();
                }
                if (settings.pageNumber <= pages && settings.pageNumber > 0) {
                    pageSpinner.setValue(settings.pageNumber);
                } else {
                    pageSpinner.setValue(1);
                }
                isInitializingSpinner = false;
                paintPDFViewer();
            }
        } catch (Exception e) {
            LOG.error("Error actualizando configuración", e);
        }
        pageSpinner.setValue(0);
    }

    public JCheckBox getSignatureVisibleCheckBox() {
        return signatureVisibleCheckBox;
    }

    public void setSignatureVisibleCheckBox(JCheckBox signatureVisibleCheckBox) {
        this.signatureVisibleCheckBox = signatureVisibleCheckBox;
    }

    public JTextField getReasonField() {
        return reasonField;
    }

    public void setReasonField(JTextField reasonField) {
        this.reasonField = reasonField;
    }

    public JTextField getLocationField() {
        return locationField;
    }

    public void setLocationField(JTextField locationField) {
        this.locationField = locationField;
    }

    public JTextField getContactInfoField() {
        return contactInfoField;
    }

    public void setContactInfoField(JTextField contactInfoField) {
        this.contactInfoField = contactInfoField;
    }

    public JSpinner getPageSpinner() {
        return pageSpinner;
    }

    public void setPageSpinner(JSpinner pageSpinner) {
        this.pageSpinner = pageSpinner;
    }

    public JRadioButton getXAdESButton() {
        return XAdESButton;
    }

    public void setXAdESButton(JRadioButton xAdESButton) {
        XAdESButton = xAdESButton;
    }

    public JRadioButton getJAdESButton() {
        return JAdESButton;
    }

    public void setJAdESButton(JRadioButton jAdESButton) {
        JAdESButton = jAdESButton;
    }

    public JButton getSignButton() {
        return signButton;
    }

    public void setSignButton(JButton signButton) {
        this.signButton = signButton;
    }

    public JRadioButton getLevelTButton() {
        return levelTButton;
    }

    public void setLevelTButton(JRadioButton levelTButton) {
        this.levelTButton = levelTButton;
    }

    public JRadioButton getLevelLTButton() {
        return levelLTButton;
    }

    public void setLevelLTButton(JRadioButton levelLTButton) {
        this.levelLTButton = levelLTButton;
    }

    public JRadioButton getLevelLTAButton() {
        return levelLTAButton;
    }

    public void setLevelLTAButton(JRadioButton levelLTAButton) {
        this.levelLTAButton = levelLTAButton;
    }

    public JLabel getImageLabel() {
        return imageLabel;
    }

    public void setImageLabel(JLabel imageLabel) {
        this.imageLabel = imageLabel;
    }

    public JLabel getSignatureLabel() {
        return signatureLabel;
    }

    public void setSignatureLabel(JLabel signatureLabel) {
        this.signatureLabel = signatureLabel;
    }

    public ButtonGroup getAdESLevelButtonGroup() {
        return AdESLevelButtonGroup;
    }

    public void setAdESLevelButtonGroup(ButtonGroup adESLevelButtonGroup) {
        AdESLevelButtonGroup = adESLevelButtonGroup;
    }

    public JLabel getAdESFormatLabel() {
        return AdESFormatLabel;
    }

    public void setAdESFormatLabel(JLabel adESFormatLabel) {
        AdESFormatLabel = adESFormatLabel;
    }

    public ButtonGroup getAdESFormatButtonGroup() {
        return AdESFormatButtonGroup;
    }

    public void setAdESFormatButtonGroup(ButtonGroup adESFormatButtonGroup) {
        AdESFormatButtonGroup = adESFormatButtonGroup;
    }

    public String getTextExample() {
        String reason = reasonField.getText();
        String location = locationField.getText();
        String contactInfo = contactInfoField.getText();
        Boolean hasReason = false;
        Boolean hasLocation = false;
        Boolean hasContact = false;
        String commonName = MessageUtils.t("signpanel_name_person");
        String identification = "XXX-XXXXXXXXXXXX";
        String organization = MessageUtils.t("signpanel_type_person");
        String additionalText = new String();
        if (cards != null && !cards.isEmpty()) {
            CardSignInfo card = cards.get(0);
            commonName = card.getCommonName();
            organization = card.getOrganization();
            identification = card.getIdentification();
        }

        DateTimeFormatter dtf = DateTimeFormatter
                .ofPattern(this.settings.dateFormatByLanguage.get(this.settings.language));
        LocalDateTime now = LocalDateTime.now();
        additionalText = commonName + "<br>" + organization + ", " + identification
                + MessageUtils.t("signpanel_declared_date") + dtf.format(now) + "<br>";
        if (reason != null && !reason.trim().isEmpty()) {
            hasReason = true;
            additionalText += MessageUtils.t("signpanel_reason") + " " + reason + "\n";
        }
        if (location != null && !location.trim().isEmpty()) {
            hasLocation = true;
            additionalText += MessageUtils.t("signpanel_place") + " " + location;
        }
        if (contactInfo != null && !contactInfo.trim().isEmpty()) {
            hasContact = true;
            additionalText += " " + MessageUtils.t("signpanel_contact") + " " + contactInfo;
        }
        if (!(hasReason || hasLocation || hasContact)) {
            additionalText += settings.getDefaultSignMessage();
        }

        additionalText = additionalText.replace("\n", "<br>");

        return additionalText;
    }

    public void seeNumberOfSignatures() {
        validateLabel.setText(MessageUtils.t("signpanel_validate_label") + " " + currentDocument.amountOfSignatures());
    }

    public boolean isASiC() {
        if (currentDocument != null) {
            if (ASICEButton.isVisible()) {
                if (ASICEButton.isSelected()) {
                    return true;
                }
            }
            this.seeNumberOfSignatures();
        }
        return false;
    }

    public boolean isCAdES() {
        if (currentDocument != null) {
            if (currentDocument.getMimeType() == SupportedMimeTypeEnum.BINARY
                    || currentDocument.getMimeType().isOpenDocument()) {
                if (XAdESButton.isSelected() || CAdESButton.isSelected() || JAdESButton.isSelected()) {
                    return true;
                }
            }
            this.seeNumberOfSignatures();
        }
        return false;
    }

    public void clean() {
        hideButtons();
        imageLabel.setIcon(null);
        currentDocument = null;
        imageCache.clear();
        currentDocumentId = null;
    }

    public SmartCardDetector getSmartCardDetector() {
        return smartCardDetector;
    }

    public void setSmartCardDetector(SmartCardDetector smartCardDetector) {
        this.smartCardDetector = smartCardDetector;
        this.smartCardDetector.addListener(this);
        try {
            this.smartCardDetector.readSaveListSmartCard();
        } catch (Throwable e) {
            LOG.error(MessageUtils.t("signpanel_log_render_preview"), e);
            e.printStackTrace();
        }
    }

    public void cardsDetectionChange(List<CardSignInfo> cards) {
        this.cards = cards;
        this.previewSignLabel();
    }

    private void showPositionModal() {
        JDialog modal = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                MessageUtils.t("signpanel_sign_position"), true);
        modal.setLayout(new GridLayout(3, 3, 10, 10));
        modal.setSize(400, 400);
        modal.setLocationRelativeTo(this);

        // Posiciones: Superior
        modal.add(createPositionButton(MessageUtils.t("signpanel_sign_topleft"), 0, 0));
        modal.add(createPositionButton(MessageUtils.t("signpanel_sign_topcenter"), 0.5, 0));
        modal.add(createPositionButton(MessageUtils.t("signpanel_sign_topright"), 1, 0));

        // Posiciones: Centro
        modal.add(createPositionButton(MessageUtils.t("signpanel_sign_centerleft"), 0, 0.5));
        modal.add(createPositionButton(MessageUtils.t("signpanel_sign_centercenter"), 0.5, 0.5));
        modal.add(createPositionButton(MessageUtils.t("signpanel_sign_centerright"), 1, 0.5));

        // Posiciones: Inferior
        modal.add(createPositionButton(MessageUtils.t("signpanel_sign_bottomleft"), 0, 1));
        modal.add(createPositionButton(MessageUtils.t("signpanel_sign_bottomcenter"), 0.5, 1));
        modal.add(createPositionButton(MessageUtils.t("signpanel_sign_bottomright"), 1, 1));

        modal.setVisible(true);
    }

    private JButton createPositionButton(String text, double xRatio, double yRatio) {
        JButton btn = new JButton(text);
        btn.addActionListener(e -> {
            int padding = 10; // Margen de seguridad

            int maxX = imageLabel.getWidth() - signatureLabel.getWidth() - padding;
            int maxY = imageLabel.getHeight() - signatureLabel.getHeight() - padding;

            int x = (int) (maxX * xRatio) + padding;
            int y = (int) (maxY * yRatio) + padding;

            // Asegurar que no quede fuera de límites
            x = Math.max(padding, Math.min(x, maxX));
            y = Math.max(padding, Math.min(y, maxY));

            signatureLabel.setLocation(x, y);
            positionLabel.setText(String.format("X: %d - Y: %d", x, y));
            ((JDialog) SwingUtilities.getWindowAncestor(btn)).dispose();
        });
        return btn;
    }

}
