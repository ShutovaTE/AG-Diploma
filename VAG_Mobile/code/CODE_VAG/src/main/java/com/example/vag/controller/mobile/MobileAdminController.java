package com.example.vag.controller.mobile;

import com.example.vag.dto.ArtworkDTO;
import com.example.vag.mapper.ArtworkMapper;
import com.example.vag.model.Artwork;
import com.example.vag.model.User;
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

@RestController
@RequestMapping("/api/mobile/admin")
public class MobileAdminController {

    private final ArtworkService artworkService;
    private final UserService userService;
    private final ArtworkMapper artworkMapper;
    private final MobileAuthController mobileAuthController;

    public MobileAdminController(ArtworkService artworkService, UserService userService,
                                 ArtworkMapper artworkMapper, MobileAuthController mobileAuthController) {
        this.artworkService = artworkService;
        this.userService = userService;
        this.artworkMapper = artworkMapper;
        this.mobileAuthController = mobileAuthController;
    }

    @GetMapping("/artworks")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<?> getAllArtworksForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            System.out.println("=== GET ALL ARTWORKS FOR ADMIN ===");
            System.out.println("AuthHeader: " + authHeader);
            System.out.println("Page: " + page + ", Size: " + size + ", Status: " + status);

            User currentUser = mobileAuthController.getUserFromToken(authHeader);
            System.out.println("Current user: " + (currentUser != null ? currentUser.getUsername() : "null"));

            if (currentUser == null || !currentUser.hasRole("ADMIN")) {
                System.out.println("Access denied - user is null or not admin");
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Access denied"
                ));
            }

            System.out.println("User is admin, proceeding...");

            Pageable pageable = PageRequest.of(page, size);
            Page<Artwork> artworkPage = artworkService.findAllPaginated(pageable);

            List<ArtworkDTO> artworkDTOs = artworkMapper.toSimpleDTOList(artworkPage.getContent());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("artworks", artworkDTOs);
            response.put("totalPages", artworkPage.getTotalPages());
            response.put("currentPage", artworkPage.getNumber());
            response.put("totalItems", artworkPage.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to fetch artworks: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/artworks/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<?> getArtworkForAdmin(@PathVariable Long id,
                                                @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            System.out.println("=== GET ARTWORK FOR ADMIN ===");
            System.out.println("Artwork ID: " + id);
            System.out.println("AuthHeader: " + authHeader);

            User currentUser = mobileAuthController.getUserFromToken(authHeader);
            if (currentUser == null || !currentUser.hasRole("ADMIN")) {
                System.out.println("Access denied - user is null or not admin");
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Access denied"
                ));
            }

            System.out.println("User is admin, loading artwork details...");

            Artwork artwork = artworkService.findByIdWithComments(id);

            if (artwork == null) {
                return ResponseEntity.notFound().build();
            }

            System.out.println("Artwork loaded - Title: " + artwork.getTitle());
            System.out.println("Artwork status: " + artwork.getStatus());
            System.out.println("Artwork user: " + (artwork.getUser() != null ? artwork.getUser().getUsername() : "null"));
            System.out.println("Artwork image: " + artwork.getImagePath());
            System.out.println("Artwork description: " + artwork.getDescription());

            ArtworkDTO artworkDTO = artworkMapper.toDTO(artwork);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("artwork", artworkDTO);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("ERROR fetching artwork for admin: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to fetch artwork: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/artworks/{id}/approve")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> approveArtwork(@PathVariable Long id,
                                            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User currentUser = mobileAuthController.getUserFromToken(authHeader);
            if (currentUser == null || !currentUser.hasRole("ADMIN")) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Access denied"
                ));
            }

            artworkService.approveArtwork(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Artwork approved successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to approve artwork: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/artworks/{id}/reject")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> rejectArtwork(@PathVariable Long id,
                                           @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User currentUser = mobileAuthController.getUserFromToken(authHeader);
            if (currentUser == null || !currentUser.hasRole("ADMIN")) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Access denied"
                ));
            }

            artworkService.rejectArtwork(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Artwork rejected successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to reject artwork: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/artworks/stats")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<?> getArtworkStats(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User currentUser = mobileAuthController.getUserFromToken(authHeader);
            if (currentUser == null || !currentUser.hasRole("ADMIN")) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Access denied"
                ));
            }

            List<Artwork> pendingArtworks = artworkService.findByStatus("PENDING");
            List<Artwork> approvedArtworks = artworkService.findByStatus("APPROVED");
            List<Artwork> rejectedArtworks = artworkService.findByStatus("REJECTED");

            Map<String, Object> stats = new HashMap<>();
            stats.put("pending", pendingArtworks.size());
            stats.put("approved", approvedArtworks.size());
            stats.put("rejected", rejectedArtworks.size());
            stats.put("total", pendingArtworks.size() + approvedArtworks.size() + rejectedArtworks.size());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stats", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to get stats: " + e.getMessage()
            ));
        }
    }
}