package storages;

import burp.api.montoya.persistence.Persistence;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import models.Category;
import models.Notes;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import java.util.Collections;
import java.util.stream.Collectors;

public class NotesStorage {
    private static final String STORAGE_KEY = "notes_db";
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
        Gson gson = new Gson();
        String json = gson.toJson(notes);

        persistence.preferences().setString(STORAGE_KEY, json);
    }

    private List<Notes> loadNotes() {
        String savedData = persistence.preferences().getString(STORAGE_KEY);

        if (savedData == null || savedData.isEmpty()) {
            return new ArrayList<>();
        }

        Gson gson = new Gson();
        Type categoryListType = new TypeToken<List<Notes>>() {}.getType();
        try {
            return gson.fromJson(savedData, categoryListType);
        } catch (JsonSyntaxException e) {
            return new ArrayList<>();
        }
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
