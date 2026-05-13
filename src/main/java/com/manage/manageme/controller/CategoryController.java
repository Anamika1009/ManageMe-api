package com.manage.manageme.controller;

import com.manage.manageme.dto.CategoryDTO;
import com.manage.manageme.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryDTO> saveCategory(@RequestBody CategoryDTO categoryDTO) {
        CategoryDTO savedCategory = categoryService.saveCategory(categoryDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCategory);
    }

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getCategoriesForCurrentProfile() {
        List<CategoryDTO> categories = categoryService.getCategoriesForCurrentProfile();
        return ResponseEntity.ok(categories);
    }

    // ✅ Fixed: /{type} → /type/{type} to avoid conflict with PUT /{categoryId}
    @GetMapping("/type/{type}")
    public ResponseEntity<List<CategoryDTO>> getCategoriesByTypeForCurrentProfile(@PathVariable String type) {
        List<CategoryDTO> list = categoryService.getCategoriesByTypeForCurrentProfile(type);
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryDTO> updateCategoriesForCurrentProfile(
            @PathVariable Long categoryId,
            @RequestBody CategoryDTO categoryDTO) {
        CategoryDTO updatedCategory = categoryService.updateCategory(categoryId, categoryDTO);
        return ResponseEntity.ok(updatedCategory);
    }
}