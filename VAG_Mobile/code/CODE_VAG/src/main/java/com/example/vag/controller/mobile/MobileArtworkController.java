package com.example.vag.controller.mobile;

import com.example.vag.dto.ArtworkDTO;
import com.example.vag.mapper.ArtworkMapper;
import com.example.vag.model.Artwork;
import com.example.vag.model.User;
import com.example.vag.repository.CategoryRepository;
import com.example.vag.service.ArtworkService;
import com.example.vag.service.UserService;
import com.example.vag.util.FileUploadUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mobile")
public class MobileArtworkController {

    private final ArtworkService artworkService;
    private final UserService userService;
    private final FileUploadUtil fileUploadUtil;
    private final ArtworkMapper artworkMapper;
    private final CategoryRepository categoryRepository;

    public MobileArtworkController(ArtworkService artworkService,
                                   UserService userService,
                                   FileUploadUtil fileUploadUtil,
                                   ArtworkMapper artworkMapper,
                                   CategoryRepository categoryRepository) {
        this.artworkService = artworkService;
        this.userService = userService;
        this.fileUploadUtil = fileUploadUtil;
        this.artworkMapper = artworkMapper;
        this.categoryRepository = categoryRepository;
    }

    private User getCurrentUser(String authHeader) {
        if (authHeader == null) {
            return null;
        }

        MobileAuthController authController = new MobileAuthController(userService);
        return authController.getUserFromToken(authHeader);
    }

    @GetMapping("/artworks")
    public ResponseEntity<?> getApprovedArtworks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Artwork> artworks = artworkService.findPaginatedApprovedArtworks(pageable);

            List<ArtworkDTO> artworkDTOs = artworkMapper.toSimpleDTOList(artworks.getContent());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("artworks", artworkDTOs);
            response.put("totalPages", artworks.getTotalPages());
            response.put("currentPage", artworks.getNumber());
            response.put("totalItems", artworks.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Не удалось загрузить работы");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/artworks/{id}")
    public ResponseEntity<?> getArtwork(@PathVariable Long id,
                                        @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Artwork artwork = artworkService.findByIdWithComments(id);
            System.out.println("MobileArtworkController: Found artwork with ID: " + id + ", comments count: " + (artwork.getComments() != null ? artwork.getComments().size() : 0));

            User currentUser = getCurrentUser(authHeader);

            boolean isApproved = "APPROVED".equals(artwork.getStatus());
            boolean isAuthor = currentUser != null && artwork.getUser() != null &&
                    currentUser.getId().equals(artwork.getUser().getId());
            boolean isAdmin = currentUser != null && currentUser.hasRole("ADMIN");

            if (!isApproved && !isAuthor && !isAdmin) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Доступ запрещен");
                return ResponseEntity.status(403).body(response);
            }

            if (isApproved) {
                artworkService.incrementViews(id);
            }

            if (currentUser != null) {
                boolean isLiked = artworkService.isLikedByUser(artwork, currentUser);
                artwork.setLiked(isLiked);
            }

            ArtworkDTO artworkDTO = artworkMapper.toDTO(artwork);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("artwork", artworkDTO);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Работа не найдена");
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/artworks/{id}/like")
    public ResponseEntity<?> likeArtwork(@PathVariable Long id,
                                         @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = getCurrentUser(authHeader);

            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Требуется аутентификация");
                return ResponseEntity.status(401).body(response);
            }

            artworkService.likeArtwork(id, user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Лайк успешно поставлен");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Не удалось поставить лайк");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/artworks/{id}/unlike")
    public ResponseEntity<?> unlikeArtwork(@PathVariable Long id,
                                           @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = getCurrentUser(authHeader);

            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Требуется аутентификация");
                return ResponseEntity.status(401).body(response);
            }

            artworkService.unlikeArtwork(id, user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Лайк успешно убран");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Не удалось убрать лайк");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/artworks/{id}/comment")
    public ResponseEntity<?> addComment(@PathVariable Long id,
                                        @RequestParam String content,
                                        @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = getCurrentUser(authHeader);

            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Требуется аутентификация");
                return ResponseEntity.status(401).body(response);
            }

            artworkService.addComment(id, user, content);

            Artwork artwork = artworkService.findByIdWithComments(id);
            ArtworkDTO artworkDTO = artworkMapper.toDTO(artwork);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("artwork", artworkDTO);
            response.put("message", "Комментарий успешно добавлен");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Не удалось добавить комментарий");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/artworks/create")
    public ResponseEntity<?> createArtwork(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam List<Long> categoryIds,
            @RequestParam MultipartFile imageFile,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            User currentUser = getCurrentUser(authHeader);

            if (currentUser == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Требуется аутентификация");
                return ResponseEntity.status(401).body(response);
            }

            Artwork artwork = new Artwork();
            artwork.setTitle(title);
            artwork.setDescription(description);

            Artwork savedArtwork = artworkService.createWithCategories(artwork, imageFile, currentUser, categoryIds);
            ArtworkDTO artworkDTO = artworkMapper.toSimpleDTO(savedArtwork);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("artwork", artworkDTO);
            response.put("message", "Работа успешно создана");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Не удалось создать работу");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/artworks/create-simple")
    public ResponseEntity<?> createArtworkSimple(
            @RequestParam String title,
            @RequestParam String description,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            User currentUser = getCurrentUser(authHeader);

            if (currentUser == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Требуется аутентификация");
                return ResponseEntity.status(401).body(response);
            }

            Artwork artwork = new Artwork();
            artwork.setTitle(title);
            artwork.setDescription(description);
            artwork.setStatus("PENDING"); 

            Artwork savedArtwork = artworkService.create(artwork, null, currentUser);
            ArtworkDTO artworkDTO = artworkMapper.toSimpleDTO(savedArtwork);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("artwork", artworkDTO);
            response.put("message", "Работа успешно создана");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Не удалось создать работу");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/users/{userId}/artworks/all")
    public ResponseEntity<?> getAllUserArtworks(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        User currentUser = getCurrentUser(authHeader);
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Требуется аутентификация"));
        }

        boolean isOwnProfile = currentUser.getId().equals(userId);
        boolean isAdmin = currentUser.hasRole("ADMIN");

        if (!isOwnProfile && !isAdmin) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Доступ запрещён"));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Artwork> artworksPage = artworkService.findAllByUserId(userId, pageable); 

        List<ArtworkDTO> dtos = artworkMapper.toSimpleDTOList(artworksPage.getContent());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("artworks", dtos);
        response.put("totalPages", artworksPage.getTotalPages());
        response.put("currentPage", artworksPage.getNumber());
        response.put("totalItems", artworksPage.getTotalElements());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/artworks/{id}")
    public ResponseEntity<?> deleteArtwork(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        User currentUser = getCurrentUser(authHeader);
        if (currentUser == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Требуется авторизация"));
        }

        try {
            artworkService.deleteArtworkCompletely(id, currentUser);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Публикация полностью удалена"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/artworks/{id}/update")
    public ResponseEntity<?> updateArtwork(@PathVariable Long id,
                                           @RequestParam String title,
                                           @RequestParam String description,
                                           @RequestParam List<Long> categoryIds,
                                           @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                           @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            User currentUser = getCurrentUser(authHeader);

            if (currentUser == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Требуется аутентификация");
                return ResponseEntity.status(401).body(response);
            }

            Artwork updatedArtwork = artworkService.updateWithCategories(id, title, description, imageFile, currentUser, categoryIds);
            ArtworkDTO artworkDTO = artworkMapper.toSimpleDTO(updatedArtwork);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("artwork", artworkDTO);
            response.put("message", "Публикация успешно обновлена");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Не удалось обновить публикацию: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}