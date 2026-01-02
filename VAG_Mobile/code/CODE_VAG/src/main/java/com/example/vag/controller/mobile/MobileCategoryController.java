package com.example.vag.controller.mobile;

import com.example.vag.dto.ArtworkDTO;
import com.example.vag.dto.CategoryDTO;
import com.example.vag.mapper.ArtworkMapper;
import com.example.vag.model.Artwork;
import com.example.vag.model.Category;
import com.example.vag.model.User;
import com.example.vag.service.ArtworkService;
import com.example.vag.service.CategoryService;
import com.example.vag.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mobile/categories")
public class MobileCategoryController {

        private final CategoryService categoryService;
        private final ArtworkService artworkService;
        private final ArtworkMapper artworkMapper;
        private final UserService userService;
        private final MobileAuthController mobileAuthController;

        public MobileCategoryController(CategoryService categoryService, ArtworkService artworkService,
                                        ArtworkMapper artworkMapper, UserService userService,
                                        MobileAuthController mobileAuthController) {
            this.categoryService = categoryService;
            this.artworkService = artworkService;
            this.artworkMapper = artworkMapper;
            this.userService = userService;
            this.mobileAuthController = mobileAuthController;
        }

    @GetMapping
    public ResponseEntity<?> getAllCategories() {
        try {
            System.out.println("=== GET ALL CATEGORIES ===");
            List<Category> categories = categoryService.findAll();
            System.out.println("Found " + categories.size() + " categories");

            for (Category category : categories) {
                Long approvedCount = artworkService.countApprovedArtworksByCategoryId(category.getId());
                category.setApprovedArtworksCount(approvedCount != null ? approvedCount : 0L);
                System.out.println("Category: " + category.getName() + ", approved artworks: " + approvedCount);
            }

            List<CategoryDTO> categoryDTOs = artworkMapper.toCategoryDTOList(categories);
            System.out.println("Converted to " + categoryDTOs.size() + " DTOs");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("categories", categoryDTOs);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("ERROR getting categories: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Не удалось загрузить категории");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCategory(@PathVariable Long id) {
        try {
            Category category = categoryService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            Long approvedCount = artworkService.countApprovedArtworksByCategoryId(id);
            category.setApprovedArtworksCount(approvedCount);

            CategoryDTO categoryDTO = artworkMapper.toCategoryDTO(category);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("category", categoryDTO);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Category not found");
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/artworks")
    public ResponseEntity<?> getArtworksByCategory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            Category category = categoryService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            Pageable pageable = PageRequest.of(page, size);
            Page<Artwork> artworks = artworkService.findByCategoryId(id, pageable);

            List<ArtworkDTO> artworkDTOs = artworkMapper.toDTOList(artworks.getContent());
            CategoryDTO categoryDTO = artworkMapper.toCategoryDTO(category);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("category", categoryDTO);
            response.put("artworks", artworkDTOs);
            response.put("totalPages", artworks.getTotalPages());
            response.put("currentPage", artworks.getNumber());
            response.put("totalItems", artworks.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch artworks");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody Map<String, String> categoryRequest,
                                            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User currentUser = mobileAuthController.getUserFromToken(authHeader);
            if (currentUser == null || !currentUser.hasRole("ADMIN")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Access denied. Admin rights required.");
                return ResponseEntity.status(403).body(response);
            }

            String name = categoryRequest.get("name");
            String description = categoryRequest.get("description");

            if (name == null || name.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Category name is required");
                return ResponseEntity.badRequest().body(response);
            }

            Category category = new Category();
            category.setName(name.trim());
            category.setDescription(description != null ? description.trim() : "");

            Category savedCategory = categoryService.save(category);
            CategoryDTO categoryDTO = artworkMapper.toCategoryDTO(savedCategory);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("category", categoryDTO);
            response.put("message", "Category created successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to create category: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, 
                                            @RequestBody Map<String, String> categoryRequest,
                                            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User currentUser = mobileAuthController.getUserFromToken(authHeader);
            if (currentUser == null || !currentUser.hasRole("ADMIN")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Access denied. Admin rights required.");
                return ResponseEntity.status(403).body(response);
            }

            Category existingCategory = categoryService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            String name = categoryRequest.get("name");
            String description = categoryRequest.get("description");

            if (name != null && !name.trim().isEmpty()) {
                existingCategory.setName(name.trim());
            }
            if (description != null) {
                existingCategory.setDescription(description.trim());
            }

            Category updatedCategory = categoryService.save(existingCategory);
            CategoryDTO categoryDTO = artworkMapper.toCategoryDTO(updatedCategory);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("category", categoryDTO);
            response.put("message", "Category updated successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update category: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id,
                                            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User currentUser = mobileAuthController.getUserFromToken(authHeader);
            if (currentUser == null || !currentUser.hasRole("ADMIN")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Access denied. Admin rights required.");
                return ResponseEntity.status(403).body(response);
            }

            Category category = categoryService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            
            Long approvedCount = artworkService.countApprovedArtworksByCategoryId(id);
            if (approvedCount > 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Cannot delete category with " + approvedCount + " approved artworks");
                return ResponseEntity.badRequest().body(response);
            }

            categoryService.delete(category);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Category deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to delete category: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}