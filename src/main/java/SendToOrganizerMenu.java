import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Annotations;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.organizer.Organizer;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SendToOrganizerMenu implements ContextMenuItemsProvider {
    private final Organizer organizer;
    private final NotesStorage notesStorage;

    public SendToOrganizerMenu(MontoyaApi api, NotesStorage notesStorage) {
        this.organizer = api.organizer();
        this.notesStorage = notesStorage;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> menuItems = new ArrayList<>();
        List<Notes> notes = new ArrayList<>(notesStorage.getNotes());

        if (notes.isEmpty()) {
            JMenuItem noNotesItem = new JMenuItem("No notes available");
            noNotesItem.setEnabled(false);
            menuItems.add(noNotesItem);
        } else {
            notes.sort(Comparator.comparing(Notes::notes));
            for (Notes note : notes) {
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
                        JOptionPane.showMessageDialog(null, "No request responses selected.", "Warning", JOptionPane.WARNING_MESSAGE);
                    }
                });
                menuItems.add(menuItem);
            }
        }
        return menuItems;
    }

    private void sendToOrganizerWithNotes(HttpRequestResponse requestResponse, Notes note) {
            Annotations annotations = Annotations.annotations(note.notes());
            HttpRequestResponse annotatedRequestResponse = requestResponse.withAnnotations(annotations);
            organizer.sendToOrganizer(annotatedRequestResponse);
    }
}
