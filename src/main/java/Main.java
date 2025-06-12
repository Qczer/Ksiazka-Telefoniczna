import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import com.formdev.flatlaf.FlatDarkLaf;

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
                "4. Wyświetl całą książkę",
                "5. Wyszukaj",
                "← Zmień bazę danych"
        };

        for (int i = 0; i < labels.length; i++) {
            int opcja = i + 1;
            JButton button = new JButton(labels[i]);
            button.addActionListener(e -> {
                switch (opcja) {
                    case 1 -> cardLayout.show(mainPanel, "dodaj wpis");
                    case 2 -> pokazUsunWpisPanel();
                    case 3 -> pokazPoprawWpisPanel();
                    case 4 -> pokazWyswietlCalaKsiazkePanel();
                    case 5 -> pokazWyszukajPanel();
                    case 6 -> {bazaDanych = null; pokazStartPanel();}
                    default -> JOptionPane.showMessageDialog(this, "Opcja jeszcze nie zaimplementowana.");
                }
            });
            panel.add(button);
        }

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
            nazwaField.setText("");
            miejscowoscField.setText("");
            numeryArea.setText("");
        });

        JButton wrocButton = new JButton("Wróć");
        wrocButton.addActionListener(e -> {
            cardLayout.show(mainPanel, "menu");
            nazwaField.setText("");
            miejscowoscField.setText("");
            numeryArea.setText("");
        });

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

        // Dane do tabeli
        String[] kolumny = {"Nazwa", "Miejscowość", "Numery"};
        ArrayList<Map<String, Object>> data = bazaDanych.read();

        String[][] rows = new String[data.size()][3];

        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> kontakt = data.get(i);

            rows[i][0] = kontakt.get("nazwa").toString();
            rows[i][1] = kontakt.get("miejscowosc").toString();
            String numeryStr = kontakt.get("numery").toString();
            numeryStr = numeryStr.substring(1, numeryStr.length() - 1); // usuwa pierwsze i ostatnie znaki '[' i ']'
            rows[i][2] = numeryStr;
        }

        JTable tabela = new JTable(rows, kolumny);
        JScrollPane scrollPane = new JScrollPane(tabela);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton wroc = new JButton("← Wróć");
        wroc.addActionListener(e -> cardLayout.show(mainPanel, "menu"));
        panel.add(wroc, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel wyszukajPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel inputPanel = new JPanel(new FlowLayout());
        JTextField szukajField = new JTextField(20);

        inputPanel.add(new JLabel("Wyszukaj:"));
        inputPanel.add(szukajField);

        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> wynikiList = new JList<>(model);
        JScrollPane scrollPane = new JScrollPane(wynikiList);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Funkcja do aktualizacji wyników
        Runnable updateResults = () -> {
            model.clear();
            String query = szukajField.getText().toLowerCase().trim();

            ArrayList<Map<String, Object>> data = bazaDanych.read();

            for (Map<String, Object> kontakt : data) {
                String nazwa = kontakt.get("nazwa").toString().toLowerCase();
                String miejscowosc = kontakt.get("miejscowosc").toString().toLowerCase();
                @SuppressWarnings("unchecked")
                ArrayList<String> numery = (ArrayList<String>) kontakt.get("numery");

                boolean pasuje;
                if (query.isEmpty()) {
                    pasuje = true; // puste pole - pokaz wszystko
                } else {
                    pasuje = nazwa.contains(query) || miejscowosc.contains(query);
                    if (!pasuje) {
                        for (String nr : numery) {
                            if (nr.toLowerCase().contains(query)) {
                                pasuje = true;
                                break;
                            }
                        }
                    }
                }

                if (pasuje) {
                    String numeryTekst = String.join(", ", numery);
                    // separator dwie spacje
                    model.addElement(kontakt.get("nazwa") + "  " + kontakt.get("miejscowosc") + "  " + numeryTekst);
                }
            }

            if (model.isEmpty()) {
                model.addElement("Brak wyników.");
            }
        };

        // Listener do dynamicznego filtrowania przy każdej zmianie tekstu
        szukajField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateResults.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateResults.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateResults.run(); }
        });

        // Pokaz wszystkie od razu po wczytaniu panelu
        updateResults.run();

        JButton wrocBtn = new JButton("Wróć");
        wrocBtn.addActionListener(e -> cardLayout.show(mainPanel, "menu"));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(wrocBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void pokazStartPanel() {
        JPanel nowyStartPanel = startPanel();
        mainPanel.add(nowyStartPanel, "start");
        cardLayout.show(mainPanel, "start");
    }
    private void pokazUsunWpisPanel() {
        JPanel nowyStartPanel = usunWpisPanel();
        mainPanel.add(nowyStartPanel, "usun wpis");
        cardLayout.show(mainPanel, "usun wpis");
    }
    private void pokazPoprawWpisPanel() {
        JPanel nowyStartPanel = poprawWpisPanel();
        mainPanel.add(nowyStartPanel, "popraw wpis");
        cardLayout.show(mainPanel, "popraw wpis");
    }
    private void pokazPoprawWybranyWpisPanel(int id) {
        JPanel nowyStartPanel = poprawWybranyWpisPanel(id);
        mainPanel.add(nowyStartPanel, "popraw wybrany wpis");
        cardLayout.show(mainPanel, "popraw wybrany wpis");
    }

    private void pokazWyswietlCalaKsiazkePanel() {
        JPanel nowyStartPanel = wyswietlCalaKsiazkePanel();
        mainPanel.add(nowyStartPanel, "wyswietl cala ksiazke");
        cardLayout.show(mainPanel, "wyswietl cala ksiazke");
    }
    private void pokazWyszukajPanel() {
        JPanel nowyStartPanel = wyszukajPanel();
        mainPanel.add(nowyStartPanel, "wyszukaj ksiazke");
        cardLayout.show(mainPanel, "wyszukaj ksiazke");
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        SwingUtilities.invokeLater(Main::new);
    }
}
