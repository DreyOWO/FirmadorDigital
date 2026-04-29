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

import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cr.libre.firmador.Settings;
import cr.libre.firmador.SettingsManager;
import cr.libre.firmador.gui.GUIInterface;

@SuppressWarnings("serial")
public class SwingMainWindowFrame extends JFrame {
    final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public GUIInterface gui;
    public JTabbedPane optionsTabbedPane;
    public static final long serialVersionUID = -7495851994719690589L;
    protected Image image = new ImageIcon(this.getClass().getClassLoader().getResource("firmador.png")).getImage();
    protected JPopupMenu menu;
    protected Settings settings;

    public void setGUIInterface(GUIInterface gui, Boolean isSimplifiedMode) {
        this.gui = gui;
        this.setDropTarget(new DropTarget() {
            public synchronized void drop(DropTargetDropEvent e) {
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);

                    if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> droppedFiles = (List<File>) e.getTransferable()
                                .getTransferData(DataFlavor.javaFileListFlavor);
                        long dirCount = droppedFiles.stream().filter(File::isDirectory).count();
                        if (dirCount == droppedFiles.size() && dirCount > 1) {
                            if (isSimplifiedMode) {
                                return;
                            }
                            gui.addDirectories(droppedFiles.toArray(new File[0]));
                            return; // Salimos para no cargar documentos
                        }
                        List<File> allFiles = new ArrayList<>();
                        for (File file : droppedFiles) {
                            if (file.isDirectory()) {
                                File[] filesInDir = file.listFiles(f -> f.isFile());
                                if (filesInDir != null) {
                                    allFiles.addAll(Arrays.asList(filesInDir));
                                }
                            } else {
                                allFiles.add(file);
                            }
                        }
                        if (!allFiles.isEmpty()) {
                            File[] filesArray = allFiles.toArray(new File[0]);
                            if (isSimplifiedMode) {
                                gui.addDocuments(new File[] { filesArray[0] });
                            } else {
                                gui.addDocuments(filesArray);
                            }
                        }
                    } else {
                        LOG.warn("El DataFlavor no es compatible: " + Arrays.toString(e.getCurrentDataFlavors()));
                    }
                } catch (Exception ex) {
                    LOG.error("Error procesando archivos arrastrados", ex);
                    ex.printStackTrace();
                }
            }
        });
    }

    public void startInterface() {
        JPanel pdfOptionsPanel = new JPanel();
        pdfOptionsPanel.setOpaque(false);
        JPanel advancedOptionsPanel = new JPanel();
        advancedOptionsPanel.setOpaque(false);
        optionsTabbedPane = new JTabbedPane();
        optionsTabbedPane.addTab("Opciones PDF", pdfOptionsPanel);
        optionsTabbedPane.setToolTipTextAt(0,
                "<html>En esta pestaña se muestran opciones específicas<br>para documentos en formato PDF.</html>");
        optionsTabbedPane.addTab("Opciones avanzadas", advancedOptionsPanel);
        optionsTabbedPane.setToolTipTextAt(1,
                "<html>En esta pestaña se muestran opciones avanzadas<br>relacionadas con la creación de la firma.</html>");
    }

    public void loadGUI() {
        settings = SettingsManager.getInstance().getAndCreateSettings();
        /*
         * menu = new JPopupMenu();
         * JMenuItem mAll = new JMenuItem("Deseleccionar modo remoto");
         * menu.add(mAll);
         * mAll.addActionListener(new ActionListener() {
         * public void actionPerformed(ActionEvent e) {
         * settings.startServer = false;
         * SettingsManager.getInstance().setSettings(settings, true);
         * gui.
         * showMessage("Debe reiniciar la aplicación para que los cambios tengan efecto"
         * );
         * }
         * });
         * this.addMouseListener(new MouseAdapter() {
         * public void mouseClicked(MouseEvent e) {
         * if (e.getButton() == MouseEvent.BUTTON3) menu.show(null, e.getX(), e.getY());
         * // Aparece el menú contextual
         * }
         * });
         */
        this.setIconImage(image.getScaledInstance(256, 256, Image.SCALE_SMOOTH));
        startInterface();
    }

    public SwingMainWindowFrame(String name) throws HeadlessException {
        super(name);
    }

}
