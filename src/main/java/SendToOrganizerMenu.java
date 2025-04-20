import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Annotations;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.organizer.Organizer;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import models.Category;
import models.Notes;
import storages.CategoryStorage;
import storages.NotesStorage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class SendToOrganizerMenu implements ContextMenuItemsProvider {
    private final Organizer organizer;
    private final NotesStorage notesStorage;
    private final CategoryStorage categoryStorage;

    public SendToOrganizerMenu(MontoyaApi api, NotesStorage notesStorage, CategoryStorage categoryStorage) {
        this.organizer = api.organizer();
        this.notesStorage = notesStorage;
        this.categoryStorage = categoryStorage;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> menuItems = new ArrayList<>();

        List<Category> categories = new ArrayList<>(categoryStorage.getCategories());
        List<Notes> notesUncategorized = new ArrayList<>(notesStorage.getNotesUncategorized());

        if (categories.isEmpty() && notesUncategorized.isEmpty()) {
            JMenuItem noNotesItem = new JMenuItem("No notes available");
            noNotesItem.setEnabled(false);
            menuItems.add(noNotesItem);
        } else {
            if (!categories.isEmpty()) {
                categories.sort(Comparator.comparing(Category::category));
                for (Category category : categories) {
                    JMenu subMenu = new JMenu(category.category());
                    List<Notes> notesByCategory = new ArrayList<>(notesStorage.getNotesByCategory(category.category()));
                    if (notesByCategory.isEmpty()) {
                        JMenuItem noNotesItem = new JMenuItem("No notes available");
                        noNotesItem.setEnabled(false);
                        subMenu.add(noNotesItem);
                    } else {
                        notesByCategory.sort(Comparator.comparing(Notes::notes));
                        for (Notes note : notesByCategory) {
                            String noteText = note.notes();
                            String displayText = noteText.length() > 100 ? noteText.substring(0, 100) : noteText;

                            JMenuItem menuItem = new JMenuItem(displayText);
                            menuItem.addActionListener((ActionEvent e) -> {
                                List<HttpRequestResponse> selectedRequests = event.selectedRequestResponses();
                                if (selectedRequests != null && !selectedRequests.isEmpty()) {
                                    for (HttpRequestResponse requestResponse : selectedRequests) {
                                        sendToOrganizerWithNotes(requestResponse, note);
                                    }
                                } else if (event.messageEditorRequestResponse().isPresent()) {
                                    sendToOrganizerWithNotes(event.messageEditorRequestResponse().get().requestResponse(), note);
                                } else {
                                    JOptionPane.showMessageDialog(null, "No request responses selected", "Warning", JOptionPane.WARNING_MESSAGE);
                                }
                            });
                            subMenu.add(menuItem);
                        }
                    }
                    menuItems.add(subMenu);
                }
            }

            if (!notesUncategorized.isEmpty()) {
                notesUncategorized.sort(Comparator.comparing(Notes::notes));
                for (Notes note : notesUncategorized) {
                    String noteText = note.notes();
                    String displayText = noteText.length() > 100 ? noteText.substring(0, 100) : noteText;

                    JMenuItem menuItem = new JMenuItem(displayText);
                    menuItem.addActionListener((ActionEvent e) -> {
                        List<HttpRequestResponse> selectedRequests = event.selectedRequestResponses();
                        if (selectedRequests != null && !selectedRequests.isEmpty()) {
                            for (HttpRequestResponse requestResponse : selectedRequests) {
                                sendToOrganizerWithNotes(requestResponse, note);
                            }
                        } else if (event.messageEditorRequestResponse().isPresent()) {
                            sendToOrganizerWithNotes(event.messageEditorRequestResponse().get().requestResponse(), note);
                        } else {
                            JOptionPane.showMessageDialog(null, "No request responses selected", "Warning", JOptionPane.WARNING_MESSAGE);
                        }
                    });
                    menuItems.add(menuItem);
                }
            }
        }
        return menuItems;
    }

    private void sendToOrganizerWithNotes(HttpRequestResponse requestResponse, Notes note) {
        String selectedCategory = "";
        if (!note.category().category().isEmpty()) {
            selectedCategory = "[" + note.category().category() + "] ";
        }

        String selectedNote = selectedCategory + note.notes();
        Annotations annotations = Annotations.annotations(selectedNote);
        HttpRequestResponse annotatedRequestResponse = requestResponse.withAnnotations(annotations);
        organizer.sendToOrganizer(annotatedRequestResponse);
    }
}
