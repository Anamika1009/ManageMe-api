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
    //save category
    public CategoryDTO saveCategory (CategoryDTO categoryDTO){
        ProfileEntity profile = profileService.getCurrentProfile();
        if(categoryRepository.existsByNameAndProfileId(categoryDTO.getName(), profile.getId() )){
            throw new RuntimeException("Category with this name already exists");
        }
        CategoryEntity newCategory = toEntity(categoryDTO, profile);
        CategoryEntity savedCategory = categoryRepository.save(newCategory);
        return toDTO(savedCategory);
    }
    // get categories for the current user profile
    public List<CategoryDTO> getCategoriesForCurrentProfile(){
        ProfileEntity profile = profileService.getCurrentProfile();
        java.util.List<CategoryEntity> categories = categoryRepository.findByProfileId(profile.getId());
        return categories.stream().map(this::toDTO).toList();
    }

    // get categories for the current user by the type
    public List<CategoryDTO> getCategoriesByTypeForCurrentProfile(String type){
        ProfileEntity profile = profileService.getCurrentProfile();
        List<CategoryEntity> entities = categoryRepository.findByTypeAndProfileId(type, profile.getId());
        return entities.stream().map(this::toDTO).toList();
    }
    // update categories for the current user
    public CategoryDTO updateCategory(Long categoryId, CategoryDTO categoryDTO){
        ProfileEntity profile = profileService.getCurrentProfile();
        CategoryEntity category = categoryRepository.findByIdAndProfileId(categoryId, profile.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        if(categoryRepository.existsByNameAndProfileId(categoryDTO.getName(), profile.getId() )){
            throw new RuntimeException("Category with this name already exists");
        }
        category.setName(categoryDTO.getName());
        category.setIcon(categoryDTO.getIcon());
        category.setType(categoryDTO.getType());
        CategoryEntity updatedCategory = categoryRepository.save(category);
        return toDTO(updatedCategory);
    }

    // helper method
    private CategoryEntity toEntity (CategoryDTO categoryDTO, ProfileEntity profile){
        return CategoryEntity.builder()
                .name(categoryDTO.getName())
                .icon(categoryDTO.getIcon())
                .type(categoryDTO.getType())
                .profile(profile)
                .build();
    }
     private CategoryDTO toDTO (CategoryEntity categoryEntity){
        return CategoryDTO.builder()
                .id(categoryEntity.getId())
                .profileId(categoryEntity.getProfile()!=null? categoryEntity.getProfile().getId(): null)
                .name(categoryEntity.getName())
                .icon(categoryEntity.getIcon())
                .type(categoryEntity.getType())
                .createdAt(categoryEntity.getCreatedAt())
                .updatedAt(categoryEntity.getUpdatedAt())
                .build();
     }




}
