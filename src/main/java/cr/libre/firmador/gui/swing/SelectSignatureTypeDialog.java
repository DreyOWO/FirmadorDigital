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

import cr.libre.firmador.documents.Document;
import cr.libre.firmador.documents.SupportedMimeTypeEnum;
import cr.libre.firmador.gui.GUISwing;
import cr.libre.firmador.signers.DocumentSigner;
import cr.libre.firmador.signers.FirmadorASiC;
import cr.libre.firmador.signers.FirmadorCAdES;
import cr.libre.firmador.signers.FirmadorJAdES;
import cr.libre.firmador.signers.FirmadorOpenDocument;
import cr.libre.firmador.signers.FirmadorPAdES;
import cr.libre.firmador.signers.FirmadorXAdES;
import cr.libre.firmador.signers.FirmadorOpenXmlFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.awt.*;
import java.lang.invoke.MethodHandles;

public class SelectSignatureTypeDialog {


    public static DocumentSigner show(GUISwing gui, SupportedMimeTypeEnum mimeType, DocumentSigner currentSigner, Document document) {
        final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
        JRadioButton asicButton = new JRadioButton("ASIC-E");
        JRadioButton padesButton = new JRadioButton("PAdES");
        JRadioButton cadesButton = new JRadioButton("CAdES");
        JRadioButton xadesButton = new JRadioButton("XAdES");
        JRadioButton jadesButton = new JRadioButton("JAdES");
        JRadioButton openButton = new JRadioButton("OpenDocument");
        JRadioButton openXmlButton = new JRadioButton("OpenXML");

        ButtonGroup group = new ButtonGroup();
        group.add(asicButton);

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Seleccione el tipo de firma:"));

        panel.add(asicButton);

        if (mimeType.isPDF()) {
            group.add(padesButton);
            panel.add(padesButton);
        }else if (mimeType.isOpenDocument()){
            group.add(openButton);
            panel.add(openButton);
        }else if (mimeType.isOpenxmlformats()) {
            panel.remove(asicButton);
            panel.add(openXmlButton);
            group.add(openXmlButton);
        } else if (mimeType.isXML()) {
            group.add(xadesButton);
            panel.add(xadesButton);
            group.add(jadesButton);
            panel.add(jadesButton);
        }else{
            group.add(cadesButton);
            panel.add(cadesButton);
            group.add(jadesButton);
            panel.add(jadesButton);
        }

        if (currentSigner != null){
            if (currentSigner instanceof FirmadorPAdES){
                LOG.info("PAdES");
                padesButton.setSelected(true);
            }else if (currentSigner instanceof FirmadorXAdES){
                LOG.info("XAdES");
                xadesButton.setSelected(true);
            }else if (currentSigner instanceof FirmadorCAdES){
                LOG.info("CAdES");
                cadesButton.setSelected(true);
            }else if (currentSigner instanceof FirmadorJAdES){
                LOG.info("JAdES");
                jadesButton.setSelected(true);
            }else if (currentSigner instanceof FirmadorOpenXmlFormat){
                LOG.info("OpenXML");
                openXmlButton.setSelected(true);
            }else if (currentSigner instanceof FirmadorOpenDocument){
                LOG.info("OpenDocument");
                openButton.setSelected(true);
            }else{
                LOG.info("ASiC");
                asicButton.setSelected(true);
            }
        }

        int result = JOptionPane.showConfirmDialog(null, panel, "Tipo de firma",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            return getDocumentSigner(gui, padesButton, openButton, openXmlButton, cadesButton, xadesButton, jadesButton);
        }

        return null;
    }

    public static DocumentSigner getDocumentSigner( GUISwing gui, JRadioButton padesButton, JRadioButton openButton, JRadioButton openXmlButton, JRadioButton cadesButton, JRadioButton xadesButton, JRadioButton jadesButton) {
        if (padesButton.isSelected()) {
            return new FirmadorPAdES(gui);
        } else if (xadesButton.isSelected()) {
            return new FirmadorXAdES(gui, false);
        }else if (cadesButton.isSelected()) {
            return new FirmadorCAdES(gui);
        }else if (jadesButton.isSelected()) {
            return new FirmadorJAdES(gui);
        }else if (openXmlButton.isSelected()) {
            return new FirmadorOpenXmlFormat(gui);
        }else if (openButton.isSelected()) {
            return new FirmadorOpenDocument(gui);
        }else{
            return new FirmadorASiC(gui);
        }
    }
}
