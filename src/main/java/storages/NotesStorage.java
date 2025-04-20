package storages;

import burp.api.montoya.persistence.Persistence;
import models.Category;
import models.Notes;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import java.util.Collections;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NotesStorage {
    private static final String STORAGE_KEY = "notes_db";
    private static final String ROW_DELIMITER = "\n";
    private static final String FIELD_DELIMITER = ",";

    private final Persistence persistence;
    private final List<Notes> notes;

    public NotesStorage(Persistence persistence) {
        this.persistence = persistence;
        this.notes = loadNotes();
    }

    public List<Notes> getNotes() {
        return Collections.unmodifiableList(notes);
    }

    public List<Notes> getNotesByCategory(String category) {
        return notes.stream()
                .filter(note -> note.category().category().equals(category))
                .collect(Collectors.toList());
    }

    public List<Notes> getNotesUncategorized() {
        return notes.stream()
                .filter(note -> note.category().category().isEmpty())
                .collect(Collectors.toList());
    }

    public void addNote(Notes note) {
        if (!notes.contains(note) && !note.notes().isEmpty()) {
            if(note.category().category().equals("-- Select Category --")){
                note = new Notes(note.notes(), new Category(""));
            }

            notes.add(note);
            saveNotes();
        }
    }

    public void deleteNote(int index) {
        if (index >= 0 && index < notes.size()) {
            notes.remove(index);
            saveNotes();
        }
    }

    public void updateNote(int index, Notes newNote) {
        if(newNote.category().category().equals("-- Select Category --")){
            newNote = new Notes(newNote.notes(), new Category(""));
        }

        if (index >= 0 && index < notes.size()  && !newNote.notes().isEmpty()) {
            notes.set(index, newNote);
            saveNotes();
        }
    }

    private void saveNotes() {
        String serializedNotes = notes.stream()
                .map(note -> {
                    String encodedNote = URLEncoder.encode(note.notes(), StandardCharsets.UTF_8);
                    String encodedCategory;
                    encodedCategory = URLEncoder.encode(note.category().category(), StandardCharsets.UTF_8);
                    return encodedNote + FIELD_DELIMITER + encodedCategory;
                })
                .collect(Collectors.joining(ROW_DELIMITER));

        persistence.preferences().setString(STORAGE_KEY, serializedNotes);
    }

    private List<Notes> loadNotes() {
        String savedData = persistence.preferences().getString(STORAGE_KEY);

        if (savedData == null || savedData.isEmpty()) {
            return new ArrayList<>();
        }

        List<Notes> loadedNotes = new ArrayList<>();

        for (String encodedText : savedData.split(Pattern.quote(ROW_DELIMITER))) {
            String[] fields = encodedText.split(Pattern.quote(FIELD_DELIMITER));
            String decodedNote = URLDecoder.decode(fields[0], StandardCharsets.UTF_8);
            String decodedCategory = fields.length > 1
                    ? URLDecoder.decode(fields[1], StandardCharsets.UTF_8)
                    : "";
            loadedNotes.add(new Notes(decodedNote, new Category(decodedCategory)));
        }

        return loadedNotes;
    }

    public void updateNotesCategory(String oldCategory, String newCategory) {
        boolean changed = false;

        for (int i = 0; i < notes.size(); i++) {
            Notes note = notes.get(i);
            if (note.category().category().equals(oldCategory)) {
                Notes updatedNote = new Notes(note.notes(), new Category(newCategory));
                notes.set(i, updatedNote);
                changed = true;
            }
        }

        if (changed) {
            saveNotes();
        }
    }
}
