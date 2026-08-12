package ru.practicum.explorewithme.controller.category;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.dto.category.CategoryDto;
import ru.practicum.explorewithme.dto.category.NewCategory;
import ru.practicum.explorewithme.service.category.CategoryService;

import java.net.URI;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class CategoryAdminController {
    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody NewCategory newCategory) {
        CategoryDto categoryDto = categoryService.createCategoryByAdmin(newCategory);
        URI location = URI.create("/categories/" + categoryDto.id());
        return ResponseEntity.created(location).body(categoryDto);
    }

    @DeleteMapping("/{catId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long catId) {
        categoryService.deleteCategoryByAdmin(catId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{catId}")
    public ResponseEntity<CategoryDto> updateCategory(@Valid @RequestBody NewCategory newCategory,
                                                      @PathVariable Long catId) {
        CategoryDto categoryDto = categoryService.updateCategoryByAdmin(catId, newCategory);
        return ResponseEntity.ok().body(categoryDto);
    }
}
