package com.manage.manageme.service;

import com.manage.manageme.dto.CategoryDTO;
import com.manage.manageme.entity.CategoryEntity;
import com.manage.manageme.entity.ProfileEntity;
import com.manage.manageme.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final ProfileService profileService;
    private final CategoryRepository categoryRepository;

    // Save category
    public CategoryDTO saveCategory(CategoryDTO categoryDTO) {
        ProfileEntity profile = profileService.getCurrentProfile();
        if (categoryRepository.existsByNameAndProfileId(categoryDTO.getName(), profile.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category with this name already exists");
        }
        CategoryEntity newCategory = toEntity(categoryDTO, profile);
        CategoryEntity savedCategory = categoryRepository.save(newCategory);
        return toDTO(savedCategory);
    }

    // Get all categories for current profile
    public List<CategoryDTO> getCategoriesForCurrentProfile() {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<CategoryEntity> categories = categoryRepository.findByProfileId(profile.getId());
        return categories.stream().map(this::toDTO).toList();
    }

    // Get categories by type for current profile
    public List<CategoryDTO> getCategoriesByTypeForCurrentProfile(String type) {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<CategoryEntity> entities = categoryRepository.findByTypeAndProfileId(type, profile.getId());
        return entities.stream().map(this::toDTO).toList();
    }

    // Update category
    public CategoryDTO updateCategory(Long categoryId, CategoryDTO categoryDTO) {
        ProfileEntity profile = profileService.getCurrentProfile();

        CategoryEntity category = categoryRepository.findByIdAndProfileId(categoryId, profile.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        // ✅ Fixed: only check name conflict if the name actually changed
        boolean nameChanged = !category.getName().equalsIgnoreCase(categoryDTO.getName());
        if (nameChanged && categoryRepository.existsByNameAndProfileId(categoryDTO.getName(), profile.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category with this name already exists");
        }

        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());
        category.setIcon(categoryDTO.getIcon());
        category.setType(categoryDTO.getType());

        CategoryEntity updatedCategory = categoryRepository.save(category);
        return toDTO(updatedCategory);
    }

    // Helpers
    private CategoryEntity toEntity(CategoryDTO categoryDTO, ProfileEntity profile) {
        return CategoryEntity.builder()
                .name(categoryDTO.getName())
                .description(categoryDTO.getDescription())
                .icon(categoryDTO.getIcon())
                .type(categoryDTO.getType())
                .profile(profile)
                .build();
    }

    private CategoryDTO toDTO(CategoryEntity categoryEntity) {
        return CategoryDTO.builder()
                .id(categoryEntity.getId())
                .profileId(categoryEntity.getProfile() != null ? categoryEntity.getProfile().getId() : null)
                .name(categoryEntity.getName())
                .description(categoryEntity.getDescription())
                .icon(categoryEntity.getIcon())
                .type(categoryEntity.getType())
                .createdAt(categoryEntity.getCreatedAt())
                .updatedAt(categoryEntity.getUpdatedAt())
                .build();
    }
}