
import burp.api.montoya.persistence.Persistence;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import java.util.Collections;
import java.util.stream.Collectors;

public class NotesStorage {
    private static final String STORAGE_KEY = "orgnotes_storage";
    private final Persistence persistence;
    private final List<Notes> notes;

    public NotesStorage(Persistence persistence) {
        this.persistence = persistence;
        this.notes = loadNotes();
    }

    public List<Notes> getNotes() {
        return Collections.unmodifiableList(notes);
    }

    public void addNotes(Notes note) {
        if (!notes.contains(note)) {
            notes.add(note);
            saveNotes();
        }
    }

    public void deleteNotes(int index) {
        if (index >= 0 && index < notes.size()) {
            notes.remove(index);
            saveNotes();
        }
    }

    public void updateNotes(int index, Notes newNote) {
        if (index >= 0 && index < notes.size()) {
            notes.set(index, newNote);
            saveNotes();
        }
    }

    private void saveNotes() {
        String serializedNotes = notes.stream()
                .map(note -> Base64.getEncoder().encodeToString(note.notes().getBytes(StandardCharsets.UTF_8)))
                .collect(Collectors.joining("\n"));
        persistence.preferences().setString(STORAGE_KEY, serializedNotes);
    }

    private List<Notes> loadNotes() {
        String savedData = persistence.preferences().getString(STORAGE_KEY);
        if (savedData != null && !savedData.isEmpty()) {
            List<Notes> loadedNotes = new ArrayList<>();
            for (String encodedText : savedData.split("\n")) {
                String decodedText = new String(Base64.getDecoder().decode(encodedText), StandardCharsets.UTF_8);
                loadedNotes.add(new Notes(decodedText));
            }
            return loadedNotes;
        }
        return new ArrayList<>();
    }

}
