package ru.practicum.explorewithme.service.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.explorewithme.dto.category.CategoryDto;
import ru.practicum.explorewithme.dto.category.NewCategory;
import ru.practicum.explorewithme.exception.ConflictDataException;
import ru.practicum.explorewithme.exception.NotFoundException;
import ru.practicum.explorewithme.mapper.CategoryMapper;
import ru.practicum.explorewithme.model.category.Category;
import ru.practicum.explorewithme.repository.CategoryRepository;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    public CategoryDto createCategoryByAdmin(NewCategory newCategory) {
        log.info("Create new category by admin");
        Category category = CategoryMapper.toCategory(newCategory);
        Category savedCategory;
        try {
            savedCategory = categoryRepository.save(category);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictDataException("Category already exists");
        }
        CategoryDto categoryDto = CategoryMapper.toCategoryDto(savedCategory);
        log.info("Saved category={}", categoryDto);
        return categoryDto;
    }

    @Override
    public void deleteCategoryByAdmin(Long catId) {
        log.info("Try to delete category by id={}", catId);
        if (!categoryRepository.existsById(catId)) {
            throw new NotFoundException("No category by ID=" + catId);
        }
        try {
            categoryRepository.deleteById(catId);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictDataException("Category can`t be deleted by ID=" + catId);
        }
        log.info("Category has been deleted by ID={}", catId);
    }

    @Override
    public CategoryDto updateCategoryByAdmin(Long catId, NewCategory newCategory) {
        log.info("Try to update category by ID={} value={}", catId, newCategory);
        if (!categoryRepository.existsById(catId)) {
            throw new NotFoundException("No category by ID=" + catId);
        }
        Category category = CategoryMapper.toCategory(catId, newCategory);
        try {
            categoryRepository.save(category);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictDataException("Category is already exists");
        }
        CategoryDto categoryDto = CategoryMapper.toCategoryDto(category);
        log.info("Category was updated correctly");
        return categoryDto;
    }

    @Override
    public List<CategoryDto> getCategories(int from, int size) {
        log.info("Try to get categories from={} size={}", from, size);
        PageRequest pageRequest = PageRequest.of(from > 0 ? from / size : 0, size);
        return categoryRepository.findAll(pageRequest)
                .stream()
                .map(CategoryMapper::toCategoryDto)
                .toList();
    }

    @Override
    public CategoryDto getCategoryById(Long catId) {
        return getCateGoryById(catId);
    }

    @Override
    public CategoryDto getCateGoryById(Long id) {
        log.info("try to find category by id={}", id);
        Optional<Category> optionalCategory = categoryRepository.findById(id);
        if (optionalCategory.isEmpty()) {
            log.error("not found category by id={}", id);
            throw new NotFoundException("not found category by id=" + id);
        }
        log.info("found category by id={}", id);
        return CategoryMapper.toCategoryDto(optionalCategory.get());
    }
}
