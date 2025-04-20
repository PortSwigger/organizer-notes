package models;

import java.util.Objects;

public record Category(String category) {
    @Override
    public String toString() {
        return category;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Category category = (Category) obj;
        return Objects.equals(this.category, category.category);
    }

}
