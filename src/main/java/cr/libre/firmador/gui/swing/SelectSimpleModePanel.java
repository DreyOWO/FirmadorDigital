package cr.libre.firmador.gui.swing;

import javax.swing.*;
import java.awt.*;
import cr.libre.firmador.MessageUtils;

@SuppressWarnings("serial")
public class SelectSimpleModePanel extends JPanel {

    private JRadioButton simplifiedModeYes;
    private JRadioButton simplifiedModeNo;
    private ButtonGroup buttonGroup;

    @SuppressWarnings("this-escape")
    public SelectSimpleModePanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Panel principal con GridBagLayout para centrar contenido
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Título/Pregunta
        JLabel questionLabel = new JLabel(MessageUtils.t("select_mode_question"));
        questionLabel.setFont(questionLabel.getFont().deriveFont(Font.BOLD, 14f));
        gbc.gridwidth = 2;
        mainPanel.add(questionLabel, gbc);

        // Radio buttons
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(15, 20, 5, 5);
        simplifiedModeYes = new JRadioButton(MessageUtils.t("select_mode_yes"));
        simplifiedModeYes.setSelected(true); // Por defecto: modo simplificado
        mainPanel.add(simplifiedModeYes, gbc);

        gbc.gridy++;
        simplifiedModeNo = new JRadioButton(MessageUtils.t("select_mode_no"));
        mainPanel.add(simplifiedModeNo, gbc);

        // Agrupar radio buttons
        buttonGroup = new ButtonGroup();
        buttonGroup.add(simplifiedModeYes);
        buttonGroup.add(simplifiedModeNo);

        // Nota informativa
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 5, 5, 5);
        JLabel infoLabel = new JLabel("<html><i>" + MessageUtils.t("select_mode_info") + "</i></html>");
        infoLabel.setFont(infoLabel.getFont().deriveFont(11f));
        infoLabel.setForeground(Color.GRAY);
        mainPanel.add(infoLabel, gbc);

        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Retorna true si el usuario seleccionó el modo simplificado
     */
    public boolean isSimplifiedModeSelected() {
        return simplifiedModeYes.isSelected();
    }

    /**
     * Establece la selección del modo simplificado
     */
    public void setSimplifiedModeSelected(boolean simplified) {
        if (simplified) {
            simplifiedModeYes.setSelected(true);
        } else {
            simplifiedModeNo.setSelected(true);
        }
    }
}
