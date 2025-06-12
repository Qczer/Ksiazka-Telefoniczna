import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Main extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;

    public Main() {
        setTitle("Książka Telefonicza - MK");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 600);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Panel z 6 przyciskami
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 3));
        for (int i = 1; i <= 6; i++) {
            JButton btn = new JButton("Przycisk " + i);
            int buttonNumber = i;
            btn.addActionListener(e -> {
                // po kliknięciu pokaż panel "detail" z numerem przycisku
                showDetailPanel(buttonNumber);
            });
            buttonsPanel.add(btn);
        }

        // Panel, który pokaże się po kliknięciu przycisku
        JPanel detailPanel = new JPanel();
        detailPanel.setName("detailPanel");  // ustaw nazwę

        mainPanel.add(buttonsPanel, "buttons");
        mainPanel.add(detailPanel, "detail");

        add(mainPanel);
        cardLayout.show(mainPanel, "buttons");

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void showDetailPanel(int buttonNumber) {
        JPanel detailPanel = (JPanel) mainPanel.getComponent(1); // "detail" panel
        detailPanel.removeAll();
        detailPanel.add(new JLabel("Kliknąłeś przycisk nr " + buttonNumber));
        detailPanel.revalidate();
        detailPanel.repaint();

        cardLayout.show(mainPanel, "detail");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
