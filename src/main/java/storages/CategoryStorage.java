package storages;

import burp.api.montoya.logging.Logging;
import burp.api.montoya.persistence.Persistence;
import models.Category;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CategoryStorage {
    private static final String STORAGE_KEY = "category_db";
    private final Persistence persistence;
    private final List<Category> categories;

    private static final String ROW_DELIMITER = "\n";

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
        String serialized = this.categories.stream()
                .map(category ->
                        URLEncoder.encode(category.category(), StandardCharsets.UTF_8)
                )
                .collect(Collectors.joining(ROW_DELIMITER));
        persistence.preferences().setString(STORAGE_KEY, serialized);
    }

    private List<Category> loadCategories() {
        String savedData = persistence.preferences().getString(STORAGE_KEY);

        if (savedData == null || savedData.isEmpty()) {
            return new ArrayList<>();
        }

        List<Category> loadedData = new ArrayList<>();

        for (String encodedData : savedData.split(Pattern.quote(ROW_DELIMITER))) {
            String decodedData = URLDecoder.decode(encodedData, StandardCharsets.UTF_8);
            loadedData.add(new Category(decodedData));
        }

        return loadedData;
    }
}
