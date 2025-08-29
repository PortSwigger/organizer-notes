import models.Category;
import storages.CategoryStorage;
import storages.NotesStorage;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;

import java.util.List;

public class CategoryManagerDialog extends JDialog {
    private final DefaultTableModel categoryTableModel;
    private JTable categoryTable;
    private final CategoryStorage categoryStorage;
    private final NotesStorage notesStorage;
    private OrganizerNotesTab organizerNotesTab;

    public CategoryManagerDialog( Component parent, OrganizerNotesTab organizerNotesTab, CategoryStorage categoryStorage, NotesStorage notesStorage) {
        this.categoryStorage = categoryStorage;
        this.notesStorage = notesStorage;
        this.organizerNotesTab = organizerNotesTab;

        setTitle("Manage Categories");
        setModal(true);
        setSize(400, 300);
        setLocationRelativeTo(parent);

        categoryTableModel = new DefaultTableModel(new Object[]{"No.", "Category Name"}, 0);
        categoryTable = new JTable(categoryTableModel);

        categoryTable = new JTable(categoryTableModel) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        TableColumn column = categoryTable.getColumnModel().getColumn(0);
        column.setPreferredWidth(50);
        column.setMaxWidth(50);
        column.setMinWidth(50);

        refreshTable();

        JScrollPane scrollPane = new JScrollPane(categoryTable);

        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> addCategory());

        JButton editButton = new JButton("Edit");
        editButton.addActionListener(e -> editCategory());

        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> deleteCategory());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        setVisible(true);
    }



    private void refreshTable() {
        categoryTableModel.setRowCount(0); // Clear existing rows
        organizerNotesTab.refreshNotesTable();
        List<Category> categories = categoryStorage.getCategories();
        for (int i = 0; i < categories.size(); i++) {
            categoryTableModel.addRow(new Object[]{i + 1, categories.get(i).category()});
        }
    }

    private void addCategory() {
        String name = JOptionPane.showInputDialog(this, "Enter category name:");
        if (name != null && !name.trim().isEmpty()) {
            categoryStorage.addCategory(new Category(name));

            refreshTable();
        }
    }

    private void editCategory() {
        int[] selectedRows = categoryTable.getSelectedRows();
        if (selectedRows.length > 1) {
            JOptionPane.showMessageDialog(this,
                    "You can only edit one note at a time!",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int selectedRow = categoryTable.getSelectedRow();
        if (selectedRow >= 0) {
            String currentName = (String) categoryTableModel.getValueAt(selectedRow, 1);
            String newName = JOptionPane.showInputDialog(this, "Edit category name:", currentName);
            if (newName != null && !newName.trim().isEmpty()) {
                categoryStorage.updateCategory(selectedRow, new Category(newName));
                notesStorage.updateNotesCategory(currentName,newName);
                refreshTable();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a category to edit!","Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void deleteCategory() {
        int[] selectedRows = categoryTable.getSelectedRows();
        if (selectedRows.length > 0) {
            int confirm = JOptionPane.showConfirmDialog(this, "Delete " + selectedRows.length + " selected category(s)?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    int rowIndex = selectedRows[i];
                    String categoryName = (String) categoryTableModel.getValueAt(rowIndex, 1);

                    categoryStorage.deleteCategory(rowIndex);
                    notesStorage.updateNotesCategory(categoryName, "");
                }
                refreshTable();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a category to delete!","Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

}
