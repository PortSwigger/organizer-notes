package storages;

import burp.api.montoya.persistence.Persistence;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import models.Category;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CategoryStorage {
    private static final String STORAGE_KEY = "category_db";
    private final Persistence persistence;
    private final List<Category> categories;


    public CategoryStorage(Persistence persistence) {
        this.persistence = persistence;
        this.categories = loadCategories();
    }

    public List<Category> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    public void addCategory(Category category) {
        if (!this.categories.contains(category) && !category.category().isEmpty()) {
            this.categories.add(category);
            saveCategories();
        }
    }

    public void deleteCategory(int index) {
        if (index >= 0 && index < this.categories.size()) {
            this.categories.remove(index);
            saveCategories();
        }
    }

    public void updateCategory(int index, Category newCategory) {
        if (index >= 0 && index < this.categories.size() && !newCategory.category().isEmpty()) {
            this.categories.set(index, newCategory);
            saveCategories();
        }
    }

    private void saveCategories() {
        Gson gson = new Gson();
        String json = gson.toJson(this.categories);

        persistence.preferences().setString(STORAGE_KEY, json);
    }

    private List<Category> loadCategories() {
        String savedData = persistence.preferences().getString(STORAGE_KEY);

        if (savedData == null || savedData.isEmpty()) {
            return new ArrayList<>();
        }

        Gson gson = new Gson();
        Type listType = new TypeToken<List<Category>>() {}.getType();
        try {
            return gson.fromJson(savedData, listType);
        } catch (JsonSyntaxException e) {
            return new ArrayList<>();
        }
    }
}
