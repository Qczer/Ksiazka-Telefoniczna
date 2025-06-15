import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.formdev.flatlaf.FlatDarkLaf;

public class Main extends JFrame {
    private int indeksWybranegoKontaktu = -1;
    private final CardLayout cardLayout;
    private final JPanel mainPanel;
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
                indeksWybranegoKontaktu = -1;
                pokazKsiazkaPanel();
            } else {
                JOptionPane.showMessageDialog(this, "Wybierz bazę z listy.");
            }
        });

        JButton nowa = new JButton("Utwórz");
        nowa.addActionListener(e -> {
            String nowaNazwa = JOptionPane.showInputDialog(this, "Podaj nazwę nowej bazy:");
            if (nowaNazwa != null && !nowaNazwa.isBlank()) {
                if (!nowaNazwa.endsWith(".json")) nowaNazwa += ".json";
                File plik = new File(folderBaz + "/" + nowaNazwa);
                if (plik.exists()) {
                    JOptionPane.showMessageDialog(this, "Plik już istnieje.");
                } else {
                    bazaDanych = new BazaDanych(plik.getPath());
                    bazaDanych.write(new ArrayList<>());
                    indeksWybranegoKontaktu = -1;
                    pokazKsiazkaPanel();
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
                        break;
                    }
                }
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Czy na pewno chcesz usunąć baze danych: " + wybrane.replaceFirst("\\.json$", "") + "?",
                    "Potwierdź usunięcie", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (wybranyPlik != null && wybranyPlik.delete()) {
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
                            indeksWybranegoKontaktu = -1;
                            pokazKsiazkaPanel();
                        }
                    }
                }
                else
                    list.clearSelection();
            }
        });

        return panel;
    }

    private JPanel ksiazkaPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        final ArrayList<Map<String, Object>> data = bazaDanych.read();

        JPanel topPanel = new JPanel(new BorderLayout());

        final JButton zmienBazeDanychBtn = new JButton("Zmień bazę danych");
        zmienBazeDanychBtn.addActionListener(e -> {
            bazaDanych = null;
            pokazStartPanel();
        });

        JPanel inputPanel = new JPanel(new FlowLayout());
        final JTextField szukajField = new JTextField(20);
        inputPanel.add(new JLabel("Wyszukaj:"));
        inputPanel.add(szukajField);

        topPanel.add(inputPanel, BorderLayout.CENTER);
        topPanel.add(zmienBazeDanychBtn, BorderLayout.EAST);

        panel.add(topPanel, BorderLayout.NORTH);

        final JPanel listaKontaktowPanel = new JPanel();
        listaKontaktowPanel.setLayout(new BoxLayout(listaKontaktowPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(listaKontaktowPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());


        final JButton dodajBtn = new JButton("Dodaj");
        dodajBtn.addActionListener(e -> pokazDodajWpisPanel());

        final JButton poprawBtn = new JButton("Popraw");
        poprawBtn.addActionListener(e -> {
            if(indeksWybranegoKontaktu == -1) {
                JOptionPane.showMessageDialog(this, "Wybierz kontakt do poprawy.");
                return;
            }
            pokazPoprawWybranyWpisPanel(indeksWybranegoKontaktu);
        });

        final JButton usunBtn = new JButton("Usuń");
        usunBtn.addActionListener(e -> {
            if(indeksWybranegoKontaktu == -1) {
                JOptionPane.showMessageDialog(Main.this, "Wybierz kontakt, który chcesz usunąć.");
                return;
            }
            String wybrane = data.get(indeksWybranegoKontaktu).get("nazwa").toString();
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Czy na pewno chcesz usunąć kontakt: " + wybrane.split(" ")[0] + "?",
                    "Potwierdź usunięcie", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                String imgName = data.get(indeksWybranegoKontaktu).get("zdjęcie").toString();
                data.remove(indeksWybranegoKontaktu);
                bazaDanych.write(data);
                JOptionPane.showMessageDialog(this, "Kontakt został usunięty.");

                boolean uzywanePrzezInnych = data.stream().anyMatch(k -> imgName.equals(k.get("zdjęcie")));

                if (!uzywanePrzezInnych) {
                    File staryPlik = new File("images/" + imgName);
                    if (staryPlik.exists()) {
                        staryPlik.delete();
                    }
                }

                indeksWybranegoKontaktu = -1;
                pokazKsiazkaPanel();
            }
        });

        buttonPanel.add(dodajBtn);
        buttonPanel.add(poprawBtn);
        buttonPanel.add(usunBtn);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        final JPanel[] aktualnieZaznaczonyPanel = {null};

        final Runnable updateResults = () -> {
            listaKontaktowPanel.removeAll();
            String query = szukajField.getText().toLowerCase().trim();

            boolean znaleziono = false;

            for (int i = 0; i < data.size(); i++) {
                final int finalI = i;
                Map<String, Object> kontakt = data.get(i);
                String nazwa = kontakt.get("nazwa").toString().toLowerCase();
                String miejscowosc = kontakt.get("miejscowosc").toString().toLowerCase();
                @SuppressWarnings("unchecked")
                ArrayList<String> numeryList = (ArrayList<String>) kontakt.get("numery");

                boolean pasuje;
                if (query.isEmpty()) {
                    pasuje = true;
                } else {
                    pasuje = nazwa.contains(query) || miejscowosc.contains(query);
                    if (!pasuje) {
                        pasuje = numeryList.stream().anyMatch(nr -> nr.toLowerCase().contains(query));
                    }
                }

                if (pasuje) {
                    znaleziono = true;

                    JPanel kontaktPanel = new JPanel(new BorderLayout());
                    kontaktPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                    kontaktPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

                    String imgName = kontakt.get("zdjęcie").toString();
                    JLabel photoLabel = new JLabel();
                    String sciezka = "images/" + imgName;
                    File imgFile = new File(sciezka);
                    int labelSize = 120;
                    if (imgFile.exists() && !imgName.isEmpty()) {
                        photoLabel.setPreferredSize(new Dimension(labelSize, labelSize));
                        photoLabel.setMinimumSize(new Dimension(labelSize, labelSize));
                        photoLabel.setMaximumSize(new Dimension(labelSize, labelSize));
                        photoLabel.setHorizontalAlignment(JLabel.CENTER);
                        photoLabel.setVerticalAlignment(JLabel.CENTER);

                        ImageIcon icon = new ImageIcon(sciezka);
                        Image img = icon.getImage();

                        int maxSize = 110;
                        int width = icon.getIconWidth();
                        int height = icon.getIconHeight();

                        double scale = 1.0;
                        if (width > maxSize || height > maxSize) {
                            double scaleWidth = (double) maxSize / width;
                            double scaleHeight = (double) maxSize / height;
                            scale = Math.min(scaleWidth, scaleHeight);
                        }

                        int newWidth = (int) (width * scale);
                        int newHeight = (int) (height * scale);

                        Image scaled = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                        photoLabel.setIcon(new ImageIcon(scaled));
                    } else {
                        photoLabel.setText("Brak zdjęcia");
                        photoLabel.setPreferredSize(new Dimension(labelSize, labelSize));
                        photoLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        photoLabel.setVerticalAlignment(SwingConstants.CENTER);
                    }
                    kontaktPanel.add(photoLabel, BorderLayout.WEST);

                    JPanel danePanel = new JPanel();
                    danePanel.setLayout(new BoxLayout(danePanel, BoxLayout.Y_AXIS));
                    danePanel.add(Box.createVerticalGlue());

                    JLabel nazwaLabel = new JLabel(kontakt.get("nazwa").toString());
                    nazwaLabel.setFont(nazwaLabel.getFont().deriveFont(14f));
                    danePanel.add(nazwaLabel);

                    JLabel miejscowoscLabel = new JLabel(kontakt.get("miejscowosc").toString());
                    miejscowoscLabel.setFont(miejscowoscLabel.getFont().deriveFont(14f));
                    danePanel.add(miejscowoscLabel);

                    JLabel numeryLabel = new JLabel(String.join(", ", numeryList));
                    numeryLabel.setFont(numeryLabel.getFont().deriveFont(14f));
                    danePanel.add(numeryLabel);

                    danePanel.add(Box.createVerticalGlue());

                    kontaktPanel.add(danePanel, BorderLayout.CENTER);

                    kontaktPanel.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            if (aktualnieZaznaczonyPanel[0] != null) {
                                aktualnieZaznaczonyPanel[0].setBackground(null);
                            }
                            kontaktPanel.setBackground(new Color(90, 90, 90));
                            aktualnieZaznaczonyPanel[0] = kontaktPanel;
                            indeksWybranegoKontaktu = finalI;
                        }
                    });

                    listaKontaktowPanel.add(kontaktPanel);
                }
            }

            if (!znaleziono) {
                JLabel brakLabel = new JLabel("Brak wyników.");
                brakLabel.setFont(brakLabel.getFont().deriveFont(14f));
                brakLabel.setHorizontalAlignment(SwingConstants.CENTER);
                JPanel pustyPanel = new JPanel(new BorderLayout());
                pustyPanel.add(brakLabel, BorderLayout.CENTER);
                pustyPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                listaKontaktowPanel.add(pustyPanel);
            }

            listaKontaktowPanel.revalidate();
            listaKontaktowPanel.repaint();
        };

        szukajField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateResults.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateResults.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateResults.run(); }
        });

        updateResults.run();

        return panel;
    }

    private JPanel dodajWpisPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        AtomicReference<String> imgRef = new AtomicReference<>("");
        AtomicReference<File> selectedFile = new AtomicReference<>(null);

        JPanel inputPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        JTextField nazwaField = new JTextField();
        JTextField miejscowoscField = new JTextField();
        JTextArea numeryArea = new JTextArea(5, 20);
        JLabel obrazekLabel = new JLabel();
        int labelSize = 150;
        obrazekLabel.setPreferredSize(new Dimension(labelSize, labelSize));
        obrazekLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        obrazekLabel.setHorizontalAlignment(JLabel.CENTER);
        obrazekLabel.setVerticalAlignment(JLabel.CENTER);
        JButton wybierzZdjecieBtn = new JButton("Wybierz zdjęcie");

        wybierzZdjecieBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Wybierz zdjęcie");
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "Obrazy (*.jpg, *.jpeg, *.png)",
                    "jpg", "jpeg", "png"
            );
            fileChooser.setFileFilter(filter);
            fileChooser.setAcceptAllFileFilterUsed(false);

            int result = fileChooser.showOpenDialog(null);

            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (file != null) {
                    selectedFile.set(file);
                    imgRef.set(file.getName());

                    ImageIcon icon = new ImageIcon(file.getAbsolutePath());
                    Image originalImage = icon.getImage();

                    int imgWidth = icon.getIconWidth();
                    int imgHeight = icon.getIconHeight();

                    double widthRatio = (double) labelSize / imgWidth;
                    double heightRatio = (double) labelSize / imgHeight;
                    double scaleFactor = Math.min(widthRatio, heightRatio);

                    int newWidth = (int) (imgWidth * scaleFactor);
                    int newHeight = (int) (imgHeight * scaleFactor);

                    Image scaledImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                    obrazekLabel.setIcon(new ImageIcon(scaledImage));
                }
            }
        });

        inputPanel.add(new JLabel("Nazwa:"));
        inputPanel.add(nazwaField);
        inputPanel.add(new JLabel("Miejscowość:"));
        inputPanel.add(miejscowoscField);
        inputPanel.add(new JLabel("Numery (jeden na linię):"));
        inputPanel.add(new JScrollPane(numeryArea));
        inputPanel.add(obrazekLabel);
        inputPanel.add(wybierzZdjecieBtn);

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

            File file = selectedFile.get();
            String imgName = Optional.ofNullable(imgRef.get()).orElse("");

            if (file != null) {
                File destinationFolder = new File("images");
                if (!destinationFolder.exists()) {
                    destinationFolder.mkdirs();
                }

                File destinationFile = new File(destinationFolder, file.getName());

                try {
                    Files.copy(file.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException err) {
                    err.printStackTrace();
                    JOptionPane.showMessageDialog(panel, "Błąd podczas kopiowania pliku.");
                    return;
                }
            }

            Kontakt kontakt = new Kontakt(nazwa, miejscowosc, numery, imgName);
            ArrayList<Map<String, Object>> data = bazaDanych.read();
            data.add(kontakt.content);
            bazaDanych.write(data);

            JOptionPane.showMessageDialog(this, "Kontakt zapisany.");
            indeksWybranegoKontaktu = -1;
            pokazKsiazkaPanel();
            nazwaField.setText("");
            miejscowoscField.setText("");
            numeryArea.setText("");
        });

        JButton wrocBtn = new JButton("Wróć");
        wrocBtn.addActionListener(e -> {
            indeksWybranegoKontaktu = -1;
            pokazKsiazkaPanel();
            nazwaField.setText("");
            miejscowoscField.setText("");
            numeryArea.setText("");
            obrazekLabel.setIcon(null);
            imgRef.set("");
            selectedFile.set(null);
        });

        buttonPanel.add(zapiszButton);
        buttonPanel.add(wrocBtn);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel poprawWybranyWpisPanel(int id) {
        ArrayList<Map<String, Object>> data = bazaDanych.read();
        Map<String, Object> kontakt = data.get(id);
        JPanel panel = new JPanel(new BorderLayout());
        AtomicReference<String> imgRef = new AtomicReference<>("");

        JPanel inputPanel = new JPanel(new GridLayout(0, 2, 5, 5));

        JTextField nazwaField = new JTextField(kontakt.get("nazwa").toString());
        JTextField miejscowoscField = new JTextField(kontakt.get("miejscowosc").toString());

        JTextArea numeryArea = new JTextArea(5, 20);
        Object numeryObj = kontakt.get("numery");
        if (numeryObj instanceof java.util.List<?> numeryList) {
            StringBuilder sb = new StringBuilder();
            for (Object numer : numeryList) {
                sb.append(numer.toString()).append("\n");
            }
            numeryArea.setText(sb.toString());
        }

        JLabel obrazekLabel = new JLabel();
        int labelSize = 150;
        obrazekLabel.setPreferredSize(new Dimension(labelSize, labelSize));
        obrazekLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        obrazekLabel.setHorizontalAlignment(JLabel.CENTER);
        obrazekLabel.setVerticalAlignment(JLabel.CENTER);

        String currImgName = kontakt.get("zdjęcie") != null ? kontakt.get("zdjęcie").toString() : "";
        if (!currImgName.isBlank()) {
            File imgFile = new File("images/" + currImgName);
            if (imgFile.exists()) {
                ImageIcon icon = new ImageIcon(imgFile.getAbsolutePath());
                Image originalImage = icon.getImage();

                int labelWidth = 150;
                int labelHeight = 150;

                int imgWidth = icon.getIconWidth();
                int imgHeight = icon.getIconHeight();

                double widthRatio = (double) labelWidth / imgWidth;
                double heightRatio = (double) labelHeight / imgHeight;
                double scaleFactor = Math.min(widthRatio, heightRatio);

                int newWidth = (int) (imgWidth * scaleFactor);
                int newHeight = (int) (imgHeight * scaleFactor);

                Image scaledImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                obrazekLabel.setIcon(new ImageIcon(scaledImage));
                imgRef.set(currImgName);
            }
        }

        JButton wybierzZdjecieBtn = new JButton("Wybierz zdjęcie");
        wybierzZdjecieBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Wybierz zdjęcie");

            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "Obrazy (*.jpg, *.jpeg, *.png, *.gif)",
                    "jpg", "jpeg", "png", "gif"
            );
            fileChooser.setFileFilter(filter);
            fileChooser.setAcceptAllFileFilterUsed(false);

            int result = fileChooser.showOpenDialog(null);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();

                File destinationFolder = new File("images");
                if (!destinationFolder.exists()) {
                    destinationFolder.mkdirs();
                }

                File destinationFile = new File(destinationFolder, selectedFile.getName());
                imgRef.set(selectedFile.getName());

                try {
                    Files.copy(selectedFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    ImageIcon icon = new ImageIcon(destinationFile.getAbsolutePath());
                    Image originalImage = icon.getImage();

                    int imgWidth = icon.getIconWidth();
                    int imgHeight = icon.getIconHeight();

                    double widthRatio = (double) labelSize / imgWidth;
                    double heightRatio = (double) labelSize / imgHeight;
                    double scaleFactor = Math.min(widthRatio, heightRatio);

                    int newWidth = (int) (imgWidth * scaleFactor);
                    int newHeight = (int) (imgHeight * scaleFactor);

                    Image scaledImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                    obrazekLabel.setIcon(new ImageIcon(scaledImage));
                } catch (IOException err) {
                    err.printStackTrace();
                }
            }
        });

        inputPanel.add(new JLabel("Nazwa:"));
        inputPanel.add(nazwaField);
        inputPanel.add(new JLabel("Miejscowość:"));
        inputPanel.add(miejscowoscField);
        inputPanel.add(new JLabel("Numery (jeden na linię):"));
        inputPanel.add(new JScrollPane(numeryArea));
        inputPanel.add(obrazekLabel);
        inputPanel.add(wybierzZdjecieBtn);

        panel.add(inputPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();

        JButton poprawBtn = new JButton("Popraw");
        poprawBtn.addActionListener((ActionEvent e) -> {
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

            String stareZdjecie = kontakt.get("zdjęcie") != null ? kontakt.get("zdjęcie").toString() : "";
            String noweZdjecie = Optional.ofNullable(imgRef.get()).orElse("");

            if (!stareZdjecie.equals(noweZdjecie) && !stareZdjecie.isBlank()) {
                boolean uzywanePrzezInnych = data.stream()
                        .filter(k -> k != kontakt) // pomiń aktualny kontakt
                        .anyMatch(k -> stareZdjecie.equals(k.get("zdjęcie")));

                if (!uzywanePrzezInnych) {
                    File staryPlik = new File("images/" + stareZdjecie);
                    if (staryPlik.exists()) {
                        staryPlik.delete();
                    }
                }
            }

            Kontakt nowyKontakt = new Kontakt(nazwa, miejscowosc, numery, noweZdjecie);
            data.set(id, nowyKontakt.content);
            bazaDanych.write(data);

            JOptionPane.showMessageDialog(this, "Kontakt poprawiony.");
            indeksWybranegoKontaktu = -1;
            pokazKsiazkaPanel();

            nazwaField.setText("");
            miejscowoscField.setText("");
            numeryArea.setText("");
            obrazekLabel.setIcon(null);
            imgRef.set("");
        });

        JButton wrocBtn = new JButton("Wróć");
        wrocBtn.addActionListener(e -> {indeksWybranegoKontaktu = -1; pokazKsiazkaPanel();});

        buttonPanel.add(poprawBtn);
        buttonPanel.add(wrocBtn);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }
    private void pokazStartPanel() {
        JPanel nowyStartPanel = startPanel();
        mainPanel.removeAll();
        mainPanel.add(nowyStartPanel, "start");
        cardLayout.show(mainPanel, "start");
        mainPanel.revalidate();
        mainPanel.repaint();
    }
    private void pokazKsiazkaPanel() {
        JPanel nowyPanel = ksiazkaPanel();
        mainPanel.removeAll();
        mainPanel.add(nowyPanel, "ksiazka");
        cardLayout.show(mainPanel, "ksiazka");
        mainPanel.revalidate();
        mainPanel.repaint();
    }
    private void pokazDodajWpisPanel() {
        JPanel nowyPanel = dodajWpisPanel();
        mainPanel.removeAll();
        mainPanel.add(nowyPanel, "dodaj wpis");
        cardLayout.show(mainPanel, "dodaj wpis");
        mainPanel.revalidate();
        mainPanel.repaint();
    }
    private void pokazPoprawWybranyWpisPanel(int id) {
        JPanel nowyPanel = poprawWybranyWpisPanel(id);
        mainPanel.removeAll();
        mainPanel.add(nowyPanel, "popraw wybrany wpis");
        cardLayout.show(mainPanel, "popraw wybrany wpis");
        mainPanel.revalidate();
        mainPanel.repaint();
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
