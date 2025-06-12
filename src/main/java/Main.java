import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;

public class Main extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private BazaDanych bazaDanych;

    private final String folderBaz = "./bazy";

    public Main() {
        super("Książka Telefoniczna - MK");
        setSize(1000, 600);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        File folder = new File(folderBaz);
        if (!folder.exists()) folder.mkdirs();

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(startPanel(), "start");
        mainPanel.add(menuPanel(), "menu");
        mainPanel.add(dodajWpisPanel(), "dodaj wpis");

        add(mainPanel);
        cardLayout.show(mainPanel, "start");

        setVisible(true);
    }

    private JPanel startPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Wybierz bazę danych lub utwórz nową", JLabel.CENTER);
        panel.add(label, BorderLayout.NORTH);

        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> list = new JList<>(model);
        JScrollPane scrollPane = new JScrollPane(list);

        File[] pliki = new File(folderBaz).listFiles((dir, name) -> name.endsWith(".json"));
        if (pliki != null) {
            for (File f : pliki) model.addElement(f.getName());
        }

        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttons = new JPanel();

        JButton wybierz = new JButton("Wybierz");
        wybierz.addActionListener(e -> {
            String wybrane = list.getSelectedValue();
            if (wybrane != null) {
                bazaDanych = new BazaDanych(folderBaz + "/" + wybrane);
                cardLayout.show(mainPanel, "menu");
            } else {
                JOptionPane.showMessageDialog(this, "Wybierz bazę z listy.");
            }
        });

        JButton nowa = new JButton("Utwórz nową");
        nowa.addActionListener(e -> {
            String nowaNazwa = JOptionPane.showInputDialog(this, "Podaj nazwę nowej bazy:");
            if (nowaNazwa != null && !nowaNazwa.isBlank()) {
                if (!nowaNazwa.endsWith(".json")) nowaNazwa += ".json";
                File plik = new File(folderBaz + "/" + nowaNazwa);
                if (plik.exists()) {
                    JOptionPane.showMessageDialog(this, "Plik już istnieje.");
                } else {
                    bazaDanych = new BazaDanych(plik.getPath());
                    bazaDanych.write(new ArrayList<>()); // Tworzy pusty plik
                    cardLayout.show(mainPanel, "menu");
                }
            }
        });

        JButton usunBtn = new JButton("Usuń");
        usunBtn.addActionListener(e -> {
            String wybrane = list.getSelectedValue();
            File wybranyPlik = null;

            if(pliki != null) {
                for (File plik : pliki) {
                    if (plik.getName().toLowerCase().contains(wybrane.toLowerCase())) {
                        wybranyPlik = plik;
                        break; // pierwszy pasujący
                    }
                }
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Czy na pewno chcesz usunąć baze danych: " + wybrane.replaceFirst("\\.json$", "") + "?",
                    "Potwierdź usunięcie", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (wybranyPlik.delete()) {
                    JOptionPane.showMessageDialog(this, "Baza została usunięta.");
                    pokazStartPanel();
                }
            }
        });

        buttons.add(wybierz);
        buttons.add(nowa);
        buttons.add(usunBtn);
        panel.add(buttons, BorderLayout.SOUTH);

        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());

                if (index != -1) {
                    if (e.getClickCount() == 2) {
                        String wybrane = list.getSelectedValue();
                        if (wybrane != null) {
                            bazaDanych = new BazaDanych(folderBaz + "/" + wybrane);
                            cardLayout.show(mainPanel, "menu");
                        }
                    }
                }
                else
                    list.clearSelection();
            }
        });

        return panel;
    }

    private JPanel menuPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 3, 10, 10));
        String[] labels = {
                "1. Dodaj wpis",
                "2. Usuń wpis",
                "3. Popraw wpis",
                "4. Wyświetl wpis",
                "5. Wyświetl całą książkę",
                "6. Wyszukaj"
        };

        for (int i = 0; i < labels.length; i++) {
            int opcja = i + 1;
            JButton button = new JButton(labels[i]);
            button.addActionListener(e -> {
                switch (opcja) {
                    case 1 -> cardLayout.show(mainPanel, "dodaj wpis");
                    case 2 -> pokazUsunWpisPanel();
                    case 3 -> pokazPoprawWpisPanel();
                    case 4 -> wyswietlCalaKsiazkePanel();
                    default -> JOptionPane.showMessageDialog(this, "Opcja jeszcze nie zaimplementowana.");
                }
            });
            panel.add(button);
        }

        JButton zmienBazeBtn = new JButton("← Zmień bazę danych");
        zmienBazeBtn.addActionListener(e -> {
            bazaDanych = null;
            pokazStartPanel();
        });

        panel.add(zmienBazeBtn);
        return panel;
    }

    private JPanel dodajWpisPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        JTextField nazwaField = new JTextField();
        JTextField miejscowoscField = new JTextField();
        JTextArea numeryArea = new JTextArea(5, 20);

        inputPanel.add(new JLabel("Nazwa:"));
        inputPanel.add(nazwaField);
        inputPanel.add(new JLabel("Miejscowość:"));
        inputPanel.add(miejscowoscField);
        inputPanel.add(new JLabel("Numery (jeden na linię):"));
        inputPanel.add(new JScrollPane(numeryArea));

        panel.add(inputPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();

        JButton zapiszButton = new JButton("Zapisz");
        zapiszButton.addActionListener((ActionEvent e) -> {
            String nazwa = nazwaField.getText();
            String miejscowosc = miejscowoscField.getText();
            String[] numeryLines = numeryArea.getText().split("\\R");

            ArrayList<String> numery = new ArrayList<>();
            for (String s : numeryLines) {
                if (!s.isBlank()) numery.add(s.trim());
            }

            if (nazwa.isEmpty() || miejscowosc.isEmpty() || numery.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Wszystkie pola muszą być wypełnione.");
                return;
            }

            Kontakt kontakt = new Kontakt(nazwa, miejscowosc, numery);
            ArrayList<Map<String, Object>> data = bazaDanych.read();
            data.add(kontakt.content);
            bazaDanych.write(data);

            JOptionPane.showMessageDialog(this, "Kontakt zapisany.");
            cardLayout.show(mainPanel, "menu");
        });

        JButton wrocButton = new JButton("Wróć");
        wrocButton.addActionListener(e -> cardLayout.show(mainPanel, "menu"));

        buttonPanel.add(zapiszButton);
        buttonPanel.add(wrocButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel usunWpisPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel inputPanel = new JPanel();

        panel.add(inputPanel, BorderLayout.CENTER);

        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> list = new JList<>(model);
        JScrollPane scrollPane = new JScrollPane(list);

        ArrayList<Map<String, Object>> data = bazaDanych.read();

        for(Map<String, Object> kontakt : data) {
            ArrayList<String> numeryList = (ArrayList<String>) kontakt.get("numery");
            String numery = String.join(" ", numeryList);
            model.addElement(
                    kontakt.get("nazwa").toString() + "   " +
                    kontakt.get("miejscowosc").toString() + "   " +
                    numery
            );
        }

        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();

        JButton wrocButton = new JButton("Wróć");
        wrocButton.addActionListener(e -> cardLayout.show(mainPanel, "menu"));

        JButton usunButton = new JButton("Usuń");
        usunButton.addActionListener(e -> {
            String wybrane = list.getSelectedValue();
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Czy na pewno chcesz usunąć kontakt: " + wybrane.split(" ")[0] + "?",
                    "Potwierdź usunięcie", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                data.remove(list.getSelectedIndex());
                bazaDanych.write(data);
                JOptionPane.showMessageDialog(this, "Kontakt został usunięty.");
                cardLayout.show(mainPanel, "menu");
            }
        });

        buttonPanel.add(usunButton);
        buttonPanel.add(wrocButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());

                if (index != -1) {
                    if (e.getClickCount() == 2) {
                        String wybrane = list.getSelectedValue();
                        if (wybrane != null) {
                            int confirm = JOptionPane.showConfirmDialog(Main.this,
                                    "Czy na pewno chcesz usunąc kontakt: " + wybrane.split(" ")[0] + "?",
                                    "Potwierdź usunięcie", JOptionPane.YES_NO_OPTION);
                            if (confirm == JOptionPane.YES_OPTION) {
                                data.remove(list.getSelectedIndex());
                                bazaDanych.write(data);
                                JOptionPane.showMessageDialog(Main.this, "Kontakt został usunięty.");
                                cardLayout.show(mainPanel, "menu");
                            }
                        }
                    }
                }
                else
                    list.clearSelection();
            }
        });

        return panel;
    }

    private JPanel poprawWpisPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel inputPanel = new JPanel();

        panel.add(inputPanel, BorderLayout.CENTER);

        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> list = new JList<>(model);
        JScrollPane scrollPane = new JScrollPane(list);

        ArrayList<Map<String, Object>> data = bazaDanych.read();

        for(Map<String, Object> kontakt : data) {
            ArrayList<String> numeryList = (ArrayList<String>) kontakt.get("numery");
            String numery = String.join(" ", numeryList);
            model.addElement(
                    kontakt.get("nazwa").toString() + "   " +
                            kontakt.get("miejscowosc").toString() + "   " +
                            numery
            );
        }

        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();

        JButton wrocButton = new JButton("Wróć");
        wrocButton.addActionListener(e -> cardLayout.show(mainPanel, "menu"));

        JButton poprawButton = new JButton("Popraw");
        poprawButton.addActionListener(e -> {
            int wybrane = list.getSelectedIndex();
            pokazPoprawWybranyWpisPanel(wybrane);
        });

        buttonPanel.add(poprawButton);
        buttonPanel.add(wrocButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());

                if (index != -1) {
                    if (e.getClickCount() == 2) {
                        String wybrane = list.getSelectedValue();
                        if (wybrane != null) {
                            pokazPoprawWybranyWpisPanel(list.getSelectedIndex());
                        }
                    }
                }
                else
                    list.clearSelection();
            }
        });

        return panel;
    }

    private JPanel poprawWybranyWpisPanel(int id) {
        ArrayList<Map<String, Object>> data = bazaDanych.read();
        Map<String, Object> kontakt = data.get(id);
        JPanel panel = new JPanel(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        JTextField nazwaField = new JTextField();
        nazwaField.setText(kontakt.get("nazwa").toString());
        JTextField miejscowoscField = new JTextField();
        miejscowoscField.setText(kontakt.get("miejscowosc").toString());
        JTextArea numeryArea = new JTextArea(5, 20);
        Object numeryObj = kontakt.get("numery");
        if (numeryObj instanceof java.util.List<?> numeryList) {
            StringBuilder sb = new StringBuilder();
            for (Object numer : numeryList) {
                sb.append(numer.toString()).append("\n");
            }
            numeryArea.setText(sb.toString());
        }

        inputPanel.add(new JLabel("Nazwa:"));
        inputPanel.add(nazwaField);
        inputPanel.add(new JLabel("Miejscowość:"));
        inputPanel.add(miejscowoscField);
        inputPanel.add(new JLabel("Numery (jeden na linię):"));
        inputPanel.add(new JScrollPane(numeryArea));

        panel.add(inputPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();

        JButton zapiszButton = new JButton("Popraw");
        zapiszButton.addActionListener((ActionEvent e) -> {
            String nazwa = nazwaField.getText();
            String miejscowosc = miejscowoscField.getText();
            String[] numeryLines = numeryArea.getText().split("\\R");

            ArrayList<String> numery = new ArrayList<>();
            for (String s : numeryLines) {
                if (!s.isBlank()) numery.add(s.trim());
            }

            if (nazwa.isEmpty() || miejscowosc.isEmpty() || numery.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Wszystkie pola muszą być wypełnione.");
                return;
            }

            Kontakt nowyKontakt = new Kontakt(nazwa, miejscowosc, numery);
            data.set(id, nowyKontakt.content );
            bazaDanych.write(data);

            JOptionPane.showMessageDialog(this, "Kontakt poprawiony.");
            cardLayout.show(mainPanel, "menu");
        });

        JButton wrocButton = new JButton("Wróć");
        wrocButton.addActionListener(e -> cardLayout.show(mainPanel, "menu"));

        buttonPanel.add(zapiszButton);
        buttonPanel.add(wrocButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel wyswietlCalaKsiazkePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        return panel;
    }

    private void pokazStartPanel() {
        JPanel nowyStartPanel = startPanel();  // generuj nowy panel z aktualnymi plikami
        mainPanel.add(nowyStartPanel, "start");
        cardLayout.show(mainPanel, "start");
    }
    private void pokazUsunWpisPanel() {
        JPanel nowyStartPanel = usunWpisPanel();  // generuj nowy panel z aktualnymi plikami
        mainPanel.add(nowyStartPanel, "usun wpis");
        cardLayout.show(mainPanel, "usun wpis");
    }
    private void pokazPoprawWpisPanel() {
        JPanel nowyStartPanel = poprawWpisPanel();  // generuj nowy panel z aktualnymi plikami
        mainPanel.add(nowyStartPanel, "popraw wpis");
        cardLayout.show(mainPanel, "popraw wpis");
    }
    private void pokazPoprawWybranyWpisPanel(int id) {
        JPanel nowyStartPanel = poprawWybranyWpisPanel(id);  // generuj nowy panel z aktualnymi plikami
        mainPanel.add(nowyStartPanel, "popraw wybrany wpis");
        cardLayout.show(mainPanel, "popraw wybrany wpis");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
