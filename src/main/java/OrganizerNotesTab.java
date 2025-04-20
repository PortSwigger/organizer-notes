
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import models.Category;
import models.Notes;
import storages.CategoryStorage;
import storages.NotesStorage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

public class OrganizerNotesTab {
    private final Logging logging;
    private final JPanel panel;
    private final DefaultTableModel tableModel;
    private final JTable notesTable;
    private final NotesStorage notesStorage;
    private final CategoryStorage categoryStorage;

    public OrganizerNotesTab(Logging logging, NotesStorage notesStorage, CategoryStorage categoryStorage) {
        this.logging = logging;

        this.notesStorage = notesStorage;
        this.categoryStorage = categoryStorage;

        panel = new JPanel(new BorderLayout());

        tableModel = new DefaultTableModel(new Object[]{"No.", "Notes", "Category"}, 0);
        notesTable = new JTable(tableModel) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        TableColumn column = notesTable.getColumnModel().getColumn(0);
        column.setPreferredWidth(50);
        column.setMaxWidth(50);
        column.setMinWidth(50);

        updateNotesTable();

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton manageCategoryButton = new JButton("Manage Category");
        manageCategoryButton.addActionListener(e -> {
            CategoryManagerDialog dialog = new CategoryManagerDialog(logging, panel, categoryStorage, notesStorage);
            dialog.setModal(true);
            dialog.setVisible(true);

            updateNotesTable();
        });

        JButton addButton = new JButton("Add Notes");
        addButton.addActionListener(this::addNotes);

        JButton editButton = new JButton("Edit Notes");
        editButton.addActionListener(this::editNotes);

        JButton deleteButton = new JButton("Delete Notes");
        deleteButton.addActionListener(this::deleteNotes);

        JButton importButton = new JButton("Import as JSON");
        importButton.addActionListener(this::importNotes);

        JButton exportButton = new JButton("Export as JSON");
        exportButton.addActionListener(this::exportNotes);

        buttonPanel.add(Box.createVerticalStrut(20));
        buttonPanel.add(manageCategoryButton);
        buttonPanel.add(Box.createVerticalStrut(40));

        buttonPanel.add(addButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(editButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(deleteButton);

        JPanel bottomButtonPanel = new JPanel();
        bottomButtonPanel.setLayout(new BoxLayout(bottomButtonPanel, BoxLayout.Y_AXIS));

        bottomButtonPanel.setBorder(BorderFactory.createEmptyBorder(50, 10, 10, 10));
        bottomButtonPanel.add(importButton);
        bottomButtonPanel.add(Box.createVerticalStrut(10));
        bottomButtonPanel.add(exportButton);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(buttonPanel, BorderLayout.NORTH);
        leftPanel.add(bottomButtonPanel, BorderLayout.SOUTH);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.2;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        mainPanel.add(leftPanel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.8;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(new JScrollPane(notesTable), gbc);

        panel.add(mainPanel, BorderLayout.CENTER);
    }

    public JPanel getPanel() {
        return panel;
    }


    private void deleteNotes(ActionEvent e) {

        int selectedRow = notesTable.getSelectedRow();
        if (selectedRow >= 0) {
            int confirm = JOptionPane.showConfirmDialog(panel, "Delete selected notes?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                notesStorage.deleteNote(selectedRow);
                updateNotesTable();
            }
        } else {
            JOptionPane.showMessageDialog(panel, "Please select a notes to delete!", "Warning", JOptionPane.WARNING_MESSAGE);
        }

    }


    private void addNotes(ActionEvent e) {
        List<Category> categories = categoryStorage.getCategories();
        JComboBox<String> categoryComboBox = new JComboBox<>();
        categoryComboBox.addItem("-- Select Category --");
        for (Category cat : categories) {
            categoryComboBox.addItem(cat.category());
        }

        JTextArea textArea = new JTextArea(7, 50);
        JScrollPane scrollPane = new JScrollPane(textArea);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));

        JLabel categoryLabel = new JLabel("Category:");
        categoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        categoryComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel notesLabel = new JLabel("Notes:");
        notesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);


        inputPanel.add(categoryLabel);
        inputPanel.add(categoryComboBox);
        inputPanel.add(Box.createVerticalStrut(10));
        inputPanel.add(notesLabel);
        inputPanel.add(scrollPane);

        int result = JOptionPane.showConfirmDialog(panel, inputPanel, "Enter new notes",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        String note = textArea.getText().trim();
        String selectedCategory = (String) categoryComboBox.getSelectedItem();

        if (result == JOptionPane.OK_OPTION && !note.isEmpty()) {
            Notes newNote = new Notes(note, new Category(selectedCategory));
            if (notesStorage.getNotes().contains(newNote)) {
                JOptionPane.showMessageDialog(null, "Notes already exists", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                notesStorage.addNote(newNote);
                updateNotesTable();
            }
        }
    }

    private void editNotes(ActionEvent e) {
        int selectedRow = notesTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(panel, "Please select a notes to edit!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Notes oldNote = notesStorage.getNotes().get(selectedRow);

        JTextArea textArea = new JTextArea(6, 50);
        textArea.setText(oldNote.notes());
        JScrollPane scrollPane = new JScrollPane(textArea);

        List<Category> categories = categoryStorage.getCategories();
        JComboBox<String> categoryComboBox = new JComboBox<>();
        categoryComboBox.addItem("-- Select Category --");
        for (Category cat : categories) {
            categoryComboBox.addItem(cat.category());
        }
        categoryComboBox.setSelectedItem(oldNote.category().category());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));

        JLabel categoryLabel = new JLabel("Category:");
        categoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        categoryComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel notesLabel = new JLabel("Notes:");
        notesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);


        inputPanel.add(categoryLabel);
        inputPanel.add(categoryComboBox);
        inputPanel.add(Box.createVerticalStrut(10));
        inputPanel.add(notesLabel);
        inputPanel.add(scrollPane);

        int result = JOptionPane.showConfirmDialog(panel, inputPanel, "Edit note",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        String newContent = textArea.getText().trim();
        String newCategory = (String) categoryComboBox.getSelectedItem();

        if (result == JOptionPane.OK_OPTION && !newContent.isEmpty()) {
            Notes updatedNote = new Notes(newContent, new Category(newCategory));
            notesStorage.updateNote(selectedRow, updatedNote);
            updateNotesTable();
        } else if (result == JOptionPane.OK_OPTION) {
            JOptionPane.showMessageDialog(panel, "Notes cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateNotesTable() {
        tableModel.setRowCount(0);
        List<Notes> notes = notesStorage.getNotes();
        for (int i = 0; i < notes.size(); i++) {
            tableModel.addRow(new Object[]{i + 1, notes.get(i).notes(), notes.get(i).category()});
        }
    }

    private static final String FIELD_DELIMITER = ",";
    private static final String ROW_DELIMITER = "\n";

    private void exportNotes(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showSaveDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            if (!file.getName().toLowerCase().endsWith(".txt")) {
                file = new File(file.getAbsolutePath() + ".txt");
            }

            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                List<Notes> notesList = notesStorage.getNotes();

                if (notesList == null || notesList.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "No notes to export!", "Warning", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String content = notesList.stream()
                        .map(note -> {
                            String encodedNote = URLEncoder.encode(note.notes(), StandardCharsets.UTF_8);
                            String encodedCategory = URLEncoder.encode(note.category().category(), StandardCharsets.UTF_8);
                            return encodedNote + FIELD_DELIMITER + encodedCategory;
                        })
                        .collect(Collectors.joining(ROW_DELIMITER));

                writer.write(content);
                JOptionPane.showMessageDialog(null, "Notes exported successfully to: " + file.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Error exporting notes: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importNotes(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(null);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                String content = Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();

                if (content.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "File is empty", "Warning", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                List<Notes> importedNotes = new ArrayList<>();
                List<Category> importedCategory = new ArrayList<>();
                for (String encodedLine : content.split(Pattern.quote(ROW_DELIMITER))) {
                    String[] fields = encodedLine.split(Pattern.quote(FIELD_DELIMITER));
                    if (fields.length < 1) continue;

                    String decodedNote = URLDecoder.decode(fields[0], StandardCharsets.UTF_8);
                    String decodedCategory = fields.length > 1
                            ? URLDecoder.decode(fields[1], StandardCharsets.UTF_8)
                            : "";
                    importedNotes.add(new Notes(decodedNote, new Category(decodedCategory)));
                    importedCategory.add(new Category(decodedCategory));
                }

                for (Notes note : importedNotes) {
                    if (!notesStorage.getNotes().contains(note)) {
                        notesStorage.addNote(note);
                    }
                }

                for (Category category : importedCategory) {
                    if (!categoryStorage.getCategories().contains(category)) {
                        categoryStorage.addCategory(category);
                    }
                }

                updateNotesTable();
                JOptionPane.showMessageDialog(null, "Notes imported successfully");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Error importing notes: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

}