package com.healthinsight;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class HealthInsightApp extends JFrame {

    private final JTextField nameField = new JTextField();
    private final JSpinner ageSpinner = new JSpinner(new SpinnerNumberModel(30, 0, 120, 1));
    private final JComboBox<String> sexCombo = new JComboBox<>(new String[]{"Female", "Male", "Non-binary", "Prefer not to say"});

    private final JPanel symptomsPanel = new JPanel(new GridLayout(0, 2, 6, 6));
    private final List<JCheckBox> symptomCheckboxes = new ArrayList<>();

    private final JTextArea resultArea = new JTextArea(14, 60);
    private final JTextArea noteArea = new JTextArea(3, 60);

    private final JLabel dbStatusLabel = new JLabel("DB: Not connected");
    private final JLabel infoLabel = new JLabel("This tool provides general information only and is not a diagnosis.");

    private final JButton evaluateBtn = new JButton("Get Insights");
    private final JButton saveBtn = new JButton("Save to Database");
    private final JButton historyBtn = new JButton("View History");

    private final SymptomEngine engine = new SymptomEngine();
    private DatabaseManager db;

    public HealthInsightApp() {
        super("Health Insight (Educational)");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));
        setMinimumSize(new Dimension(1000, 700));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildMain(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        // Populate symptoms
        loadSymptoms();

        // Wire handlers
        evaluateBtn.addActionListener(this::onEvaluate);
        saveBtn.addActionListener(this::onSave);
        historyBtn.addActionListener(this::onHistory);

        // Init DB in background
        SwingUtilities.invokeLater(this::initDatabase);

        // Show disclaimer modal
        SwingUtilities.invokeLater(this::showDisclaimerDialog);

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(12, 12, 0, 12));
        JLabel title = new JLabel("Health Insight");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        p.add(title, BorderLayout.WEST);
        return p;
    }

    private JPanel buildMain() {
        JPanel container = new JPanel(new BorderLayout(12, 12));
        container.setBorder(new EmptyBorder(0, 12, 12, 12));

        JPanel left = buildForm();
        JPanel right = buildOutput();

        container.add(left, BorderLayout.WEST);
        container.add(right, BorderLayout.CENTER);
        return container;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel();
        form.setLayout(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Enter Details"));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        // Name
        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
        form.add(new JLabel("Name:"), gc);
        gc.gridx = 1; gc.gridy = 0; gc.weightx = 1;
        nameField.setColumns(14);
        form.add(nameField, gc);

        // Age
        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0;
        form.add(new JLabel("Age:"), gc);
        gc.gridx = 1; gc.gridy = 1; gc.weightx = 1;
        form.add(ageSpinner, gc);

        // Sex
        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0;
        form.add(new JLabel("Sex:"), gc);
        gc.gridx = 1; gc.gridy = 2; gc.weightx = 1;
        form.add(sexCombo, gc);

        // Symptoms
        JPanel symptomsContainer = new JPanel(new BorderLayout(6,6));
        symptomsContainer.setBorder(BorderFactory.createTitledBorder("Select Symptoms"));
        JScrollPane sp = new JScrollPane(symptomsPanel);
        sp.setPreferredSize(new Dimension(360, 350));
        symptomsContainer.add(sp, BorderLayout.CENTER);

        gc.gridx = 0; gc.gridy = 3; gc.gridwidth = 2; gc.weightx = 1; gc.fill = GridBagConstraints.BOTH;
        form.add(symptomsContainer, gc);

        // Note
        JPanel notePanel = new JPanel(new BorderLayout(6,6));
        notePanel.setBorder(BorderFactory.createTitledBorder("Notes (optional)"));
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        notePanel.add(new JScrollPane(noteArea), BorderLayout.CENTER);
        gc.gridx = 0; gc.gridy = 4; gc.gridwidth = 2; gc.weightx = 1; gc.weighty = 1; gc.fill = GridBagConstraints.BOTH;
        form.add(notePanel, gc);

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btns.add(evaluateBtn);
        btns.add(saveBtn);
        btns.add(historyBtn);
        gc.gridx = 0; gc.gridy = 5; gc.gridwidth = 2; gc.weighty = 0; gc.fill = GridBagConstraints.HORIZONTAL;
        form.add(btns, gc);

        return form;
    }

    private JPanel buildOutput() {
        JPanel out = new JPanel(new BorderLayout(6,6));
        out.setBorder(BorderFactory.createTitledBorder("Insights"));

        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);

        out.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        return out;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(new EmptyBorder(6, 12, 12, 12));

        dbStatusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        infoLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        infoLabel.setForeground(new Color(96, 96, 96));

        footer.add(dbStatusLabel, BorderLayout.WEST);
        footer.add(infoLabel, BorderLayout.EAST);
        return footer;
    }

    private void loadSymptoms() {
        symptomsPanel.removeAll();
        List<String> syms = engine.getAvailableSymptoms();
        for (String s : syms) {
            JCheckBox cb = new JCheckBox(s);
            symptomCheckboxes.add(cb);
            symptomsPanel.add(cb);
        }
        symptomsPanel.revalidate();
        symptomsPanel.repaint();
    }

    private void onEvaluate(ActionEvent e) {
        List<String> selected = symptomCheckboxes.stream()
                .filter(AbstractButton::isSelected)
                .map(AbstractButton::getText)
                .collect(Collectors.toList());

        String name = nameField.getText().trim();
        int age = (int) ageSpinner.getValue();
        String sex = Objects.toString(sexCombo.getSelectedItem(), "Prefer not to say");

        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select at least one symptom.", "No Symptoms", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SymptomEngine.EvaluationResult res = engine.evaluate(selected, age, sex);

        StringBuilder sb = new StringBuilder();
        sb.append("Hello").append(name.isEmpty() ? "" : " " + name).append(", here are your personalized insights:\n\n");

        if (res.isUrgent()) {
            sb.append("! URGENT WARNING !\n");
            sb.append("Some symptoms may need urgent evaluation. If you have severe chest pain, trouble breathing, confusion,\n");
            sb.append("or symptoms are rapidly worsening, seek emergency care now.\n\n");
        }

        sb.append("Most likely possibilities (not a diagnosis):\n");
        int rank = 1;
        for (SymptomEngine.ConditionSuggestion s : res.getTopSuggestions()) {
            sb.append(String.format("  %d) %s (score %d)\n", rank++, s.conditionName(), s.score()));
            sb.append("     Tip: ").append(s.advice()).append("\n");
        }
        sb.append("\nGeneral tips:\n");
        sb.append("  • Stay hydrated and rest.\n");
        sb.append("  • If symptoms persist, worsen, or you’re concerned, consult a qualified healthcare professional.\n");
        if (age >= 65) sb.append("  • Adults 65+ should consider earlier medical advice.\n");

        sb.append("\nSelected symptoms: ").append(String.join(", ", selected)).append("\n");
        if (!noteArea.getText().trim().isEmpty()) {
            sb.append("Notes: ").append(noteArea.getText().trim()).append("\n");
        }
        sb.append("\nGenerated: ").append(LocalDateTime.now());

        resultArea.setText(sb.toString());
        resultArea.setCaretPosition(0);
    }

    private void onSave(ActionEvent e) {
        if (db == null || !db.isConnected()) {
            JOptionPane.showMessageDialog(this,
                    "Database is not connected.\nYou can still use insights without saving, or configure DB settings in DatabaseManager.",
                    "DB Not Connected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a name before saving.", "Missing Name", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> selected = symptomCheckboxes.stream()
                .filter(AbstractButton::isSelected)
                .map(AbstractButton::getText)
                .collect(Collectors.toList());
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select at least one symptom before saving.", "No Symptoms", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int age = (int) ageSpinner.getValue();
        String sex = Objects.toString(sexCombo.getSelectedItem(), "Prefer not to say");
        SymptomEngine.EvaluationResult res = engine.evaluate(selected, age, sex);

        String top = res.getTopSuggestions().stream()
                .map(s -> s.conditionName() + " (score " + s.score() + ")")
                .collect(Collectors.joining("; "));

        String adviceCombined = res.getTopSuggestions().stream()
                .map(SymptomEngine.ConditionSuggestion::advice)
                .distinct()
                .collect(Collectors.joining(" | "));

        try {
            long userId = db.ensureUser(name, age, sex);
            db.saveAssessment(userId,
                    String.join(", ", selected),
                    top,
                    adviceCombined,
                    res.isUrgent(),
                    noteArea.getText().trim());
            JOptionPane.showMessageDialog(this, "Assessment saved.", "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to save: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onHistory(ActionEvent e) {
        if (db == null || !db.isConnected()) {
            JOptionPane.showMessageDialog(this,
                    "Database is not connected; cannot load history.",
                    "DB Not Connected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a name to view that user's history.", "Missing Name", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Long userId = db.findUserIdByName(name);
            if (userId == null) {
                JOptionPane.showMessageDialog(this, "No records found for: " + name, "No History", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            List<DatabaseManager.AssessmentRecord> recs = db.fetchRecentAssessments(userId, 10);
            if (recs.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No records found for: " + name, "No History", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            showHistoryDialog(name, recs);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load history: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showHistoryDialog(String name, List<DatabaseManager.AssessmentRecord> recs) {
        JTextArea ta = new JTextArea(18, 80);
        ta.setEditable(false);
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        StringBuilder sb = new StringBuilder();
        sb.append("Recent assessments for ").append(name).append(":\n\n");
        int i = 1;
        for (DatabaseManager.AssessmentRecord r : recs) {
            sb.append(String.format("%d) %s\n", i++, r.createdAt()));
            sb.append("   Symptoms: ").append(r.symptoms()).append("\n");
            sb.append("   Top: ").append(r.topConditions()).append("\n");
            if (r.urgent()) sb.append("   Flag: URGENT\n");
            if (r.notes() != null && !r.notes().isBlank()) sb.append("   Notes: ").append(r.notes()).append("\n");
            sb.append("\n");
        }
        ta.setText(sb.toString());
        ta.setCaretPosition(0);

        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(800, 400));
        JOptionPane.showMessageDialog(this, sp, "History", JOptionPane.PLAIN_MESSAGE);
    }

    private void initDatabase() {
        db = new DatabaseManager();
        try {
            db.initializeDatabase();
            dbStatusLabel.setText("DB: " + (db.isConnected() ? "Connected" : "Not connected"));
            dbStatusLabel.setForeground(db.isConnected() ? new Color(0, 128, 0) : new Color(180, 0, 0));
        } catch (Exception ex) {
            dbStatusLabel.setText("DB: Not connected");
            dbStatusLabel.setForeground(new Color(180, 0, 0));
        }
    }

    private void showDisclaimerDialog() {
        JTextArea ta = new JTextArea(
                "Important: Educational Use Only\n\n" +
                "This tool provides general health information based on user-entered symptoms.\n" +
                "It is NOT a medical diagnosis or a substitute for professional advice.\n" +
                "Seek urgent care for severe or rapidly worsening symptoms.\n\n" +
                "By continuing, you acknowledge and understand the above."
        );
        ta.setEditable(false);
        ta.setWrapStyleWord(true);
        ta.setLineWrap(true);
        ta.setOpaque(false);
        ta.setBorder(new EmptyBorder(10, 10, 10, 10));

        JCheckBox agree = new JCheckBox("I understand and want to continue.");
        Object[] params = { ta, agree };

        int res;
        do {
            res = JOptionPane.showConfirmDialog(this, params, "Disclaimer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (res != JOptionPane.OK_OPTION || !agree.isSelected()) {
                int exit = JOptionPane.showConfirmDialog(this, "Exit the app?", "Confirm Exit", JOptionPane.YES_NO_OPTION);
                if (exit == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            } else {
                break;
            }
        } while (true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new HealthInsightApp().setVisible(true);
        });
    }
}