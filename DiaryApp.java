import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class DiaryApp extends JFrame {
    private String username;
    private JTextField nameField;
    private JLabel greetingLabel;
    private JComboBox<String> moodComboBox;
    private JTextArea entryArea;
    private JTextArea bestMemoryArea;
    private JTextArea gratefulArea;
    private JTextArea upsetArea;
    private DefaultListModel<String> entryListModel;
    private JList<String> entryList;
    private JButton addButton, updateButton, deleteButton;
    private Map<String, DiaryEntry> diaryEntries = new LinkedHashMap<>();
    private final String USER_FILE = "username.txt";
    private final String DATA_FILE = "diary_data.txt";

    public DiaryApp() {
        setTitle("Personal Diary App");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        loadUsername();
        loadEntries();

        setLayout(new BorderLayout());
        add(createTopPanel(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);

        refreshEntryList();
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10,10,10,10));
        greetingLabel = new JLabel();
        greetingLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        greetingLabel.setForeground(new Color(25, 50, 80));
        updateGreeting();
        panel.add(greetingLabel, BorderLayout.WEST);

        if (username == null) {
            nameField = new JTextField(15);
            JButton saveNameBtn = new JButton("Save Name");
            saveNameBtn.addActionListener(e -> {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter your name.");
                    return;
                }
                username = name;
                saveUsername();
                updateGreeting();
                nameField.setVisible(false);
                saveNameBtn.setVisible(false);
            });
            JPanel inputPanel = new JPanel();
            inputPanel.add(new JLabel("Enter your name: "));
            inputPanel.add(nameField);
            inputPanel.add(saveNameBtn);
            panel.add(inputPanel, BorderLayout.EAST);
        }

        return panel;
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
        panel.setBorder(new EmptyBorder(10,10,10,10));

        // Left side - Entry list
        entryListModel = new DefaultListModel<>();
        entryList = new JList<>(entryListModel);
        entryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        entryList.setFont(new Font("Monospaced", Font.PLAIN, 14));
        entryList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String key = entryList.getSelectedValue();
                if (key != null) {
                    DiaryEntry entry = diaryEntries.get(key);
                    if (entry != null) {
                        loadEntry(entry);
                    }
                }
            }
        });
        JScrollPane listScroll = new JScrollPane(entryList);
        listScroll.setBorder(BorderFactory.createTitledBorder("Diary Entries (Date)"));
        panel.add(listScroll);

        // Right side - Entry form
        JPanel formPanel = new JPanel(new BorderLayout(5,5));
        formPanel.setBorder(BorderFactory.createTitledBorder("Diary Entry"));

        JPanel topForm = new JPanel(new GridLayout(2, 2, 10, 10));

        // Mood dropdown
        topForm.add(new JLabel("Mood:"));
        moodComboBox = new JComboBox<>(new String[] {"😊 Happy", "😐 Neutral", "😞 Sad", "😠 Angry", "😴 Tired"});
        moodComboBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
        topForm.add(moodComboBox);

        // Date (current date)
        topForm.add(new JLabel("Date:"));
        JLabel dateLabel = new JLabel(currentDate());
        dateLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        topForm.add(dateLabel);

        formPanel.add(topForm, BorderLayout.NORTH);

        // Main diary text area
        entryArea = new JTextArea(6, 30);
        entryArea.setLineWrap(true);
        entryArea.setWrapStyleWord(true);
        entryArea.setFont(new Font("Serif", Font.PLAIN, 14));
        JScrollPane entryScroll = new JScrollPane(entryArea);
        entryScroll.setBorder(BorderFactory.createTitledBorder("Diary Entry"));
        formPanel.add(entryScroll, BorderLayout.CENTER);

        // Optional prompts panel
        JPanel promptsPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        promptsPanel.setBorder(BorderFactory.createTitledBorder("Optional Reflections"));

        bestMemoryArea = createPromptTextArea("Best Memory");
        gratefulArea = createPromptTextArea("Grateful For");
        upsetArea = createPromptTextArea("Anything Upset You");

        promptsPanel.add(bestMemoryArea);
        promptsPanel.add(gratefulArea);
        promptsPanel.add(upsetArea);

        formPanel.add(promptsPanel, BorderLayout.SOUTH);

        panel.add(formPanel);

        return panel;
    }

    private JTextArea createPromptTextArea(String title) {
        JTextArea area = new JTextArea(3, 30);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Serif", Font.ITALIC, 13));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createTitledBorder(title));
        return area;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panel.setBorder(new EmptyBorder(10,10,10,10));

        addButton = new JButton("Add / Save Entry");
        addButton.setBackground(new Color(70, 130, 180));
        addButton.setForeground(Color.white);
        addButton.setFocusPainted(false);
        addButton.addActionListener(e -> saveCurrentEntry());

        updateButton = new JButton("Update Entry");
        updateButton.setBackground(new Color(255, 140, 0));
        updateButton.setForeground(Color.white);
        updateButton.setFocusPainted(false);
        updateButton.addActionListener(e -> updateCurrentEntry());

        deleteButton = new JButton("Delete Entry");
        deleteButton.setBackground(new Color(220, 20, 60));
        deleteButton.setForeground(Color.white);
        deleteButton.setFocusPainted(false);
        deleteButton.addActionListener(e -> deleteCurrentEntry());

        panel.add(addButton);
        panel.add(updateButton);
        panel.add(deleteButton);

        return panel;
    }

    private void saveCurrentEntry() {
        String date = currentDate();
        String mood = (String) moodComboBox.getSelectedItem();
        String diaryText = entryArea.getText().trim();

        if (diaryText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Diary entry cannot be empty.");
            return;
        }

        DiaryEntry entry = new DiaryEntry(date, mood, diaryText,
                bestMemoryArea.getText().trim(),
                gratefulArea.getText().trim(),
                upsetArea.getText().trim());

        diaryEntries.put(date, entry);
        saveEntries();
        refreshEntryList();
        JOptionPane.showMessageDialog(this, "Entry saved successfully.");
    }

    private void updateCurrentEntry() {
        String selectedDate = entryList.getSelectedValue();
        if (selectedDate == null) {
            JOptionPane.showMessageDialog(this, "Select an entry to update.");
            return;
        }
        String mood = (String) moodComboBox.getSelectedItem();
        String diaryText = entryArea.getText().trim();
        if (diaryText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Diary entry cannot be empty.");
            return;
        }
        DiaryEntry entry = new DiaryEntry(selectedDate, mood, diaryText,
                bestMemoryArea.getText().trim(),
                gratefulArea.getText().trim(),
                upsetArea.getText().trim());

        diaryEntries.put(selectedDate, entry);
        saveEntries();
        refreshEntryList();
        JOptionPane.showMessageDialog(this, "Entry updated successfully.");
    }

    private void deleteCurrentEntry() {
        String selectedDate = entryList.getSelectedValue();
        if (selectedDate == null) {
            JOptionPane.showMessageDialog(this, "Select an entry to delete.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete the entry for " + selectedDate + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            diaryEntries.remove(selectedDate);
            saveEntries();
            refreshEntryList();
            clearEntryForm();
            JOptionPane.showMessageDialog(this, "Entry deleted.");
        }
    }

    private void refreshEntryList() {
        entryListModel.clear();
        for (String date : diaryEntries.keySet()) {
            entryListModel.addElement(date);
        }
    }

    private void loadEntry(DiaryEntry entry) {
        moodComboBox.setSelectedItem(entry.mood);
        entryArea.setText(entry.diaryText);
        bestMemoryArea.setText(entry.bestMemory);
        gratefulArea.setText(entry.grateful);
        upsetArea.setText(entry.upset);
    }

    private void clearEntryForm() {
        moodComboBox.setSelectedIndex(0);
        entryArea.setText("");
        bestMemoryArea.setText("");
        gratefulArea.setText("");
        upsetArea.setText("");
    }

    private void updateGreeting() {
        if (username != null) {
            greetingLabel.setText("Welcome, " + username + "! Start your diary entry.");
        } else {
            greetingLabel.setText("Welcome! Please enter your name to start.");
        }
    }

    private String currentDate() {
        return java.time.LocalDate.now().toString();
    }

    private void loadUsername() {
        try {
            File file = new File(USER_FILE);
            if (file.exists()) {
                List<String> lines = Files.readAllLines(file.toPath());
                if (!lines.isEmpty()) {
                    username = lines.get(0);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not load username.");
        }
    }

    private void saveUsername() {
        try {
            Files.write(Paths.get(USER_FILE), username.getBytes());
        } catch (IOException e) {
            System.err.println("Could not save username.");
        }
    }

    private void loadEntries() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            diaryEntries.clear();
            String line;
            while ((line = br.readLine()) != null) {
                // Format: date|mood|diaryText|bestMemory|grateful|upset
                String[] parts = line.split("\\|", -1);
                if (parts.length == 6) {
                    DiaryEntry entry = new DiaryEntry(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]);
                    diaryEntries.put(parts[0], entry);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not load diary entries.");
        }
    }

    private void saveEntries() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_FILE))) {
            for (DiaryEntry entry : diaryEntries.values()) {
                // Escape any | characters in text fields
                String line = String.join("|",
                        entry.date,
                        entry.mood.replace("|", " "),
                        entry.diaryText.replace("|", " "),
                        entry.bestMemory.replace("|", " "),
                        entry.grateful.replace("|", " "),
                        entry.upset.replace("|", " "));
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Could not save diary entries.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new DiaryApp().setVisible(true);
        });
    }

    // Inner class for diary entries
    private static class DiaryEntry {
        String date, mood, diaryText, bestMemory, grateful, upset;

        DiaryEntry(String date, String mood, String diaryText, String bestMemory, String grateful, String upset) {
            this.date = date;
            this.mood = mood;
            this.diaryText = diaryText;
            this.bestMemory = bestMemory;
            this.grateful = grateful;
            this.upset = upset;
        }
    }
}
