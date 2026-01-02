package com.example.vag.controller.mobile;

import com.example.vag.dto.ExhibitionDTO;
import com.example.vag.dto.ArtworkDTO;
import com.example.vag.dto.UserDTO;
import com.example.vag.mapper.ArtworkMapper;
import com.example.vag.model.Exhibition;
import com.example.vag.model.Artwork;
import com.example.vag.model.User;
import com.example.vag.service.ExhibitionService;
import com.example.vag.service.ArtworkService;
import com.example.vag.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mobile")
public class MobileExhibitionController {

    private final ExhibitionService exhibitionService;
    private final ArtworkService artworkService;
    private final UserService userService;
    private final ArtworkMapper artworkMapper;

    public MobileExhibitionController(ExhibitionService exhibitionService,
                                     ArtworkService artworkService,
                                     UserService userService,
                                     ArtworkMapper artworkMapper) {
        this.exhibitionService = exhibitionService;
        this.artworkService = artworkService;
        this.userService = userService;
        this.artworkMapper = artworkMapper;
    }

    private User getCurrentUser(String authHeader) {
        if (authHeader == null) {
            return null;
        }

        MobileAuthController authController = new MobileAuthController(userService);
        return authController.getUserFromToken(authHeader);
    }

    @GetMapping("/exhibitions")
    public ResponseEntity<?> getExhibitions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Exhibition> exhibitionPage = exhibitionService.findPaginatedExhibitions(page, size);

            List<ExhibitionDTO> exhibitionDTOs = exhibitionPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("exhibitions", exhibitionDTOs);
            response.put("totalPages", exhibitionPage.getTotalPages());
            response.put("currentPage", exhibitionPage.getNumber());
            response.put("totalItems", exhibitionPage.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Не удалось загрузить выставки");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/exhibitions/{id}")
    public ResponseEntity<?> getExhibition(@PathVariable Long id) {
        try {
            Exhibition exhibition = exhibitionService.findById(id).orElseThrow();
            ExhibitionDTO exhibitionDTO = convertToDTO(exhibition);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("exhibition", exhibitionDTO);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Выставка не найдена");
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/exhibitions/create")
    public ResponseEntity<?> createExhibition(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(defaultValue = "false") boolean authorOnly,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            User currentUser = getCurrentUser(authHeader);

            if (currentUser == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Требуется аутентификация");
                return ResponseEntity.status(401).body(response);
            }

            Exhibition exhibition = new Exhibition();
            exhibition.setTitle(title);
            exhibition.setDescription(description);
            exhibition.setAuthorOnly(authorOnly);

            Exhibition savedExhibition = exhibitionService.create(exhibition, currentUser);
            ExhibitionDTO exhibitionDTO = convertToDTO(savedExhibition);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("exhibition", exhibitionDTO);
            response.put("message", "Выставка успешно создана");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Не удалось создать выставку");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/exhibitions/{id}")
    public ResponseEntity<?> updateExhibition(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(defaultValue = "false") boolean authorOnly,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            User currentUser = getCurrentUser(authHeader);

            if (currentUser == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Требуется аутентификация");
                return ResponseEntity.status(401).body(response);
            }

            Exhibition existingExhibition = exhibitionService.findById(id).orElseThrow();

            if (!existingExhibition.getUser().getId().equals(currentUser.getId()) &&
                !currentUser.hasRole("ADMIN")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Доступ запрещен");
                return ResponseEntity.status(403).body(response);
            }

            existingExhibition.setTitle(title);
            existingExhibition.setDescription(description);
            existingExhibition.setAuthorOnly(authorOnly);

            Exhibition updatedExhibition = exhibitionService.update(existingExhibition);
            ExhibitionDTO exhibitionDTO = convertToDTO(updatedExhibition);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("exhibition", exhibitionDTO);
            response.put("message", "Выставка успешно обновлена");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Не удалось обновить выставку");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/exhibitions/{id}")
    public ResponseEntity<?> deleteExhibition(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            User currentUser = getCurrentUser(authHeader);

            if (currentUser == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Требуется аутентификация");
                return ResponseEntity.status(401).body(response);
            }

            Exhibition exhibition = exhibitionService.findById(id).orElseThrow();

            if (!exhibition.getUser().getId().equals(currentUser.getId()) &&
                !currentUser.hasRole("ADMIN")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Доступ запрещен");
                return ResponseEntity.status(403).body(response);
            }

            exhibitionService.delete(exhibition);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Выставка успешно удалена");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Не удалось удалить выставку");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/exhibitions/{id}/artworks")
    public ResponseEntity<?> getExhibitionArtworks(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        try {
            Exhibition exhibition = exhibitionService.findById(id).orElseThrow();
            Pageable pageable = PageRequest.of(page, size);

            Page<Artwork> artworkPage = artworkService.findByExhibitionId(id, pageable);

            List<ArtworkDTO> artworkDTOs = artworkMapper.toSimpleDTOList(artworkPage.getContent());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("artworks", artworkDTOs);
            response.put("totalPages", artworkPage.getTotalPages());
            response.put("currentPage", artworkPage.getNumber());
            response.put("totalItems", artworkPage.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Не удалось загрузить работы выставки");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/exhibitions/{exhibitionId}/artworks/{artworkId}")
    public ResponseEntity<?> addArtworkToExhibition(
            @PathVariable Long exhibitionId,
            @PathVariable Long artworkId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            User currentUser = getCurrentUser(authHeader);

            if (currentUser == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Требуется аутентификация");
                return ResponseEntity.status(401).body(response);
            }

            Exhibition exhibition = exhibitionService.findById(exhibitionId).orElseThrow();
            Artwork artwork = artworkService.findById(artworkId).orElseThrow();

            if (exhibition.isAuthorOnly() && !exhibition.getUser().getId().equals(currentUser.getId())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Доступ запрещен");
                return ResponseEntity.status(403).body(response);
            }

            exhibition.getArtworks().add(artwork);
            artwork.getExhibitions().add(exhibition);
            exhibitionService.save(exhibition);
            artworkService.save(artwork);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Работа успешно добавлена в выставку");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Не удалось добавить работу в выставку");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/exhibitions/{exhibitionId}/artworks/{artworkId}")
    public ResponseEntity<?> removeArtworkFromExhibition(
            @PathVariable Long exhibitionId,
            @PathVariable Long artworkId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            User currentUser = getCurrentUser(authHeader);

            if (currentUser == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Требуется аутентификация");
                return ResponseEntity.status(401).body(response);
            }

            Exhibition exhibition = exhibitionService.findById(exhibitionId).orElseThrow();
            Artwork artwork = artworkService.findById(artworkId).orElseThrow();

            if (!exhibition.getUser().getId().equals(currentUser.getId()) &&
                !artwork.getUser().getId().equals(currentUser.getId()) &&
                !currentUser.hasRole("ADMIN")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Доступ запрещен");
                return ResponseEntity.status(403).body(response);
            }

            exhibitionService.removeArtworkFromExhibition(exhibitionId, artworkId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Работа успешно удалена из выставки");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Не удалось удалить работу из выставки");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/users/{userId}/exhibitions")
    public ResponseEntity<?> getUserExhibitions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Exhibition> exhibitionPage = exhibitionService.findByUser(userService.findById(userId).orElseThrow(), pageable);

            List<ExhibitionDTO> exhibitionDTOs = exhibitionPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("exhibitions", exhibitionDTOs);
            response.put("totalPages", exhibitionPage.getTotalPages());
            response.put("currentPage", exhibitionPage.getNumber());
            response.put("totalItems", exhibitionPage.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Не удалось загрузить выставки пользователя");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/exhibitions/{exhibitionId}/add-existing-artworks")
    public ResponseEntity<?> getUserArtworksForExhibition(
            @PathVariable Long exhibitionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            User currentUser = getCurrentUser(authHeader);

            if (currentUser == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Требуется аутентификация");
                return ResponseEntity.status(401).body(response);
            }

            Exhibition exhibition = exhibitionService.findById(exhibitionId).orElseThrow();

            if (exhibition.isAuthorOnly() && !exhibition.getUser().getId().equals(currentUser.getId())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Доступ запрещен");
                return ResponseEntity.status(403).body(response);
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<Artwork> userArtworksPage = artworkService.findByUser(currentUser, pageable);

            Set<Artwork> exhibitionArtworks = exhibition.getArtworks();

            List<ArtworkDTO> availableArtworks = userArtworksPage.getContent().stream()
                .filter(artwork -> !exhibitionArtworks.contains(artwork))
                .map(artworkMapper::toSimpleDTO)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("exhibition", convertToDTO(exhibition));
            response.put("artworks", availableArtworks);
            response.put("totalPages", userArtworksPage.getTotalPages());
            response.put("currentPage", userArtworksPage.getNumber());
            response.put("totalItems", userArtworksPage.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Не удалось загрузить работы для добавления в выставку");
            return ResponseEntity.badRequest().body(response);
        }
    }

    private ExhibitionDTO convertToDTO(Exhibition exhibition) {
        ExhibitionDTO dto = new ExhibitionDTO();
        dto.setId(exhibition.getId());
        dto.setTitle(exhibition.getTitle());
        dto.setDescription(exhibition.getDescription());
        dto.setImageUrl(exhibition.getImageUrl());
        dto.setAuthorOnly(exhibition.isAuthorOnly());
        dto.setCreatedAt(exhibition.getCreatedAt());

        dto.setArtworksCount((int) exhibitionService.countApprovedArtworksInExhibition(exhibition.getId()));

        Artwork firstArtwork = exhibitionService.getFirstApprovedArtworkInExhibition(exhibition.getId());
        System.out.println("MobileExhibitionController: Exhibition " + exhibition.getId() + " - firstArtwork: " +
                          (firstArtwork != null ? firstArtwork.getTitle() : "null"));
        if (firstArtwork != null) {
            dto.setFirstArtwork(artworkMapper.toSimpleDTO(firstArtwork));
        }

        if (exhibition.getUser() != null) {
            UserDTO userDTO = new UserDTO();
            userDTO.setId(exhibition.getUser().getId());
            userDTO.setUsername(exhibition.getUser().getUsername());
            userDTO.setEmail(exhibition.getUser().getEmail());
            userDTO.setDescription(exhibition.getUser().getDescription());
            userDTO.setRole(exhibition.getUser().getRole().getName().name());
            dto.setUser(userDTO);
        }

        return dto;
    }
}
