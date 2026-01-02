package com.example.vag.controller.mobile;

import com.example.vag.dto.ArtworkDTO;
import com.example.vag.dto.UserDTO;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mobile/users")
public class MobileUserController {

    private final UserService userService;
    private final ArtworkService artworkService;
    private final ArtworkMapper artworkMapper;
    private final MobileAuthController mobileAuthController;

    public MobileUserController(UserService userService,
                                ArtworkService artworkService,
                                ArtworkMapper artworkMapper,
                                MobileAuthController mobileAuthController) {
        this.userService = userService;
        this.artworkService = artworkService;
        this.artworkMapper = artworkMapper;
        this.mobileAuthController = mobileAuthController;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getCurrentUserProfile(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = mobileAuthController.getUserFromToken(authHeader);
            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Authentication required");
                return ResponseEntity.status(401).body(response);
            }

            User fullUser = userService.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            UserDTO userDTO = artworkMapper.toUserDTO(fullUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", userDTO);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch user profile: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/profile/update")
    public ResponseEntity<?> updateUserProfile(@RequestBody Map<String, String> profileRequest,
                                               @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = mobileAuthController.getUserFromToken(authHeader);
            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Authentication required");
                return ResponseEntity.status(401).body(response);
            }

            String username = profileRequest.get("username");
            String email = profileRequest.get("email");
            String description = profileRequest.get("description");
            String currentPassword = profileRequest.get("currentPassword");

            if (currentPassword == null || currentPassword.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Current password is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (userService.authenticate(user.getUsername(), currentPassword) == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Current password is incorrect");
                return ResponseEntity.badRequest().body(response);
            }

            if (username != null && !username.equals(user.getUsername())) {
                if (userService.findByUsername(username).isPresent()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Username already exists");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            if (email != null && !email.equals(user.getEmail())) {
                if (userService.findByEmail(email).isPresent()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Email already exists");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            User updatedUser = new User();
            updatedUser.setId(user.getId());
            if (username != null && !username.trim().isEmpty()) {
                updatedUser.setUsername(username.trim());
            } else {
                updatedUser.setUsername(user.getUsername());
            }

            if (email != null && !email.trim().isEmpty()) {
                updatedUser.setEmail(email.trim());
            } else {
                updatedUser.setEmail(user.getEmail());
            }

            if (description != null) {
                updatedUser.setDescription(description.trim());
            } else {
                updatedUser.setDescription(user.getDescription());
            }

            User savedUser = userService.update(updatedUser);
            UserDTO userDTO = artworkMapper.toUserDTO(savedUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", userDTO);
            response.put("message", "Profile updated successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update profile: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long userId) {
        try {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            UserDTO userDTO = artworkMapper.toUserDTO(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", userDTO);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "User not found: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{userId}/artworks")
    public ResponseEntity<?> getUserArtworks(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            User currentUser = null;
            if (authHeader != null && !authHeader.trim().isEmpty()) {
                currentUser = mobileAuthController.getUserFromToken(authHeader);
            }

            boolean isOwnProfile = currentUser != null && currentUser.getId().equals(userId);

            List<Artwork> artworks;

            if (isOwnProfile) {
                artworks = artworkService.findByUserWithDetails(user);
            } else {
                artworks = artworkService.findByUserWithDetails(user).stream()
                        .filter(artwork -> "APPROVED".equals(artwork.getStatus()))
                        .collect(Collectors.toList());
            }

            List<ArtworkDTO> artworkDTOs = artworkMapper.toSimpleDTOList(artworks);
            UserDTO userDTO = artworkMapper.toUserDTO(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", userDTO);
            response.put("artworks", artworkDTOs);
            response.put("totalItems", artworks.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch user artworks: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/liked/artworks")
    public ResponseEntity<?> getLikedArtworks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            User user = mobileAuthController.getUserFromToken(authHeader);

            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Authentication required");
                return ResponseEntity.status(401).body(response);
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<Artwork> artworkPage = artworkService.findLikedArtworks(user, pageable);

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
            response.put("message", "Failed to fetch liked artworks: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/artists/random")
    public ResponseEntity<?> getRandomArtists(@RequestParam(defaultValue = "4") int count) {
        try {
            List<User> randomArtists = userService.findRandomArtists(count);
            List<UserDTO> userDTOs = randomArtists.stream()
                    .map(artworkMapper::toUserDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("artists", userDTOs);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch random artists: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/artists")
    public ResponseEntity<?> getAllArtists() {
        try {
            List<User> artists = userService.findAllWithArtworksCount();
            List<UserDTO> userDTOs = artworkMapper.toArtistsWithCountDTOList(artists);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("users", userDTOs);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch artists: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/profile/update-with-password")
    public ResponseEntity<?> updateUserProfileWithPassword(@RequestBody Map<String, String> profileRequest,
                                                          @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = mobileAuthController.getUserFromToken(authHeader);
            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Authentication required");
                return ResponseEntity.status(401).body(response);
            }

            String username = profileRequest.get("username");
            String email = profileRequest.get("email");
            String description = profileRequest.get("description");
            String currentPassword = profileRequest.get("currentPassword");

            if (currentPassword == null || currentPassword.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Current password is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (userService.authenticate(user.getUsername(), currentPassword) == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Current password is incorrect");
                return ResponseEntity.badRequest().body(response);
            }

            if (username != null && !username.equals(user.getUsername())) {
                if (userService.findByUsername(username).isPresent()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Username already exists");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            if (email != null && !email.equals(user.getEmail())) {
                if (userService.findByEmail(email).isPresent()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Email already exists");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            User updatedUser = new User();
            updatedUser.setId(user.getId());
            if (username != null && !username.trim().isEmpty()) {
                updatedUser.setUsername(username.trim());
            } else {
                updatedUser.setUsername(user.getUsername());
            }

            if (email != null && !email.trim().isEmpty()) {
                updatedUser.setEmail(email.trim());
            } else {
                updatedUser.setEmail(user.getEmail());
            }

            if (description != null) {
                updatedUser.setDescription(description.trim());
            } else {
                updatedUser.setDescription(user.getDescription());
            }

            User savedUser = userService.update(updatedUser);
            UserDTO userDTO = artworkMapper.toUserDTO(savedUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", userDTO);
            response.put("message", "Profile updated successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update profile: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/profile/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> passwordRequest,
                                           @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = mobileAuthController.getUserFromToken(authHeader);
            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Authentication required");
                return ResponseEntity.status(401).body(response);
            }

            String currentPassword = passwordRequest.get("currentPassword");
            String newPassword = passwordRequest.get("newPassword");

            if (currentPassword == null || currentPassword.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Current password is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (newPassword == null || newPassword.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "New password is required");
                return ResponseEntity.badRequest().body(response);
            }

            boolean skipPasswordCheck = "true".equals(passwordRequest.get("skipPasswordCheck"));
            if (!skipPasswordCheck && userService.authenticate(user.getUsername(), currentPassword) == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Current password is incorrect");
                return ResponseEntity.badRequest().body(response);
            }

            if (currentPassword.equals(newPassword)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "New password must be different from current password");
                return ResponseEntity.badRequest().body(response);
            }

            User updatedUser = new User();
            updatedUser.setId(user.getId());
            updatedUser.setPassword(newPassword);
            updatedUser.setUsername(user.getUsername());
            updatedUser.setEmail(user.getEmail());
            updatedUser.setDescription(user.getDescription());

            userService.update(updatedUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password changed successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to change password: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/artworks/{artworkId}")
    public ResponseEntity<?> deleteUserArtwork(@PathVariable Long artworkId,
                                               @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = mobileAuthController.getUserFromToken(authHeader);
            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Authentication required");
                return ResponseEntity.status(401).body(response);
            }

            Artwork artwork = artworkService.findById(artworkId)
                    .orElseThrow(() -> new RuntimeException("Artwork not found"));

            if (!artwork.getUser().getId().equals(user.getId())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "You can only delete your own artworks");
                return ResponseEntity.status(403).body(response);
            }

            artworkService.delete(artwork);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Artwork deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to delete artwork: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(response);
        }
    }
}