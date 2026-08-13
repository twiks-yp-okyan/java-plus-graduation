package ru.practicum.explorewithme.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.explorewithme.dto.category.CategoryDto;
import ru.practicum.explorewithme.dto.category.NewCategory;
import ru.practicum.explorewithme.model.category.Category;

@UtilityClass
public class CategoryMapper {

    public Category toCategory(NewCategory newCategory) {
        Category category = new Category();
        category.setName(newCategory.getName());
        return category;
    }

    public Category toCategory(Long id, NewCategory newCategory) {
        Category category = new Category();
        category.setId(id);
        category.setName(newCategory.getName());
        return category;
    }

    public CategoryDto toCategoryDto(Category category) {
        return new CategoryDto(category.getId(), category.getName());
    }
}
