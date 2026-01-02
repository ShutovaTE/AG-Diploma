package com.example.vag.controller.mobile;

import com.example.vag.dto.AuthResponse;
import com.example.vag.model.User;
import com.example.vag.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/mobile/auth")
public class MobileAuthController {

    private final UserService userService;

    private static final Map<String, User> tokenStore = new ConcurrentHashMap<>();
    private static final Map<Long, String> userTokenStore = new ConcurrentHashMap<>();

    public MobileAuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> registerRequest) {
        try {
            String username = registerRequest.get("username");
            String email = registerRequest.get("email");
            String password = registerRequest.get("password");

            System.out.println("=== MOBILE REGISTER ATTEMPT ===");
            System.out.println("Username: " + username);
            System.out.println("Email: " + email);

            if (username == null || username.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Username is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (email == null || email.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (password == null || password.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Password is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (userService.findByUsername(username.trim()).isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Username already exists");
                return ResponseEntity.badRequest().body(response);
            }

            if (userService.findByEmail(email.trim()).isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email already exists");
                return ResponseEntity.badRequest().body(response);
            }

            User newUser = new User();
            newUser.setUsername(username.trim());
            newUser.setEmail(email.trim());
            newUser.setPassword(password);

            User registeredUser = userService.register(newUser);

            System.out.println("User registered successfully: " + registeredUser.getUsername());
            System.out.println("User description: " + registeredUser.getDescription());
            System.out.println("User role: " + registeredUser.getRole().getName().name());

            String token = UUID.randomUUID().toString();
            tokenStore.put(token, registeredUser);
            userTokenStore.put(registeredUser.getId(), token);

            AuthResponse authResponse = new AuthResponse();
            authResponse.setSuccess(true);
            authResponse.setMessage("Registration successful");
            authResponse.setId(registeredUser.getId());
            authResponse.setUsername(registeredUser.getUsername());
            authResponse.setEmail(registeredUser.getEmail());
            authResponse.setDescription(registeredUser.getDescription()); 
            authResponse.setRole(registeredUser.getRole().getName().name());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", authResponse);
            response.put("token", token);
            response.put("message", "Registration successful");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("Registration failed: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Registration failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");

            System.out.println("=== MOBILE LOGIN ATTEMPT ===");
            System.out.println("Username: " + username);

            User user = userService.authenticate(username, password);

            System.out.println("User role: " + user.getRole().getName().name());
            System.out.println("User description: " + user.getDescription());

            String token = UUID.randomUUID().toString();
            tokenStore.put(token, user);
            userTokenStore.put(user.getId(), token);

            System.out.println("=== TOKEN SAVED ===");
            System.out.println("Token generated: " + token);
            System.out.println("Token store size after save: " + tokenStore.size());
            System.out.println("User ID: " + user.getId());
            System.out.println("User username: " + user.getUsername());
            System.out.println("User description: " + user.getDescription());

            AuthResponse authResponse = new AuthResponse();
            authResponse.setSuccess(true);
            authResponse.setMessage("Login successful");
            authResponse.setId(user.getId());
            authResponse.setUsername(user.getUsername());
            authResponse.setEmail(user.getEmail());
            authResponse.setDescription(user.getDescription()); 
            authResponse.setRole(user.getRole().getName().name());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", authResponse);
            response.put("token", token);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid credentials: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            if (token != null && tokenStore.containsKey(token)) {
                User user = tokenStore.get(token);
                tokenStore.remove(token);
                userTokenStore.remove(user.getId());
                System.out.println("User logged out: " + user.getUsername());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Logout successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Logout failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/check")
    public ResponseEntity<?> checkAuth(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            if (token != null && tokenStore.containsKey(token)) {
                User user = tokenStore.get(token);
                Map<String, Object> response = new HashMap<>();
                response.put("authenticated", true);
                response.put("username", user.getUsername());
                response.put("email", user.getEmail());
                response.put("description", user.getDescription()); 
                response.put("role", user.getRole().getName().name());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("authenticated", false);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("authenticated", false);
            return ResponseEntity.ok(response);
        }
    }

    public User getUserFromToken(String authHeader) {
        try {
            String token = extractToken(authHeader);
            System.out.println("=== TOKEN VALIDATION ===");
            System.out.println("Token: " + token);
            System.out.println("Token store size: " + tokenStore.size());

            if (token != null && tokenStore.containsKey(token)) {
                User userFromStore = tokenStore.get(token);
                
                User managedUser = userService.findById(userFromStore.getId())
                        .orElseThrow(() -> new RuntimeException("User not found in database"));

                System.out.println("User found: " + managedUser.getUsername());
                System.out.println("User description: " + managedUser.getDescription());
                System.out.println("User role: " + (managedUser.getRole() != null ? managedUser.getRole().getName().name() : "null"));
                return managedUser;
            } else {
                System.out.println("Token not found in store");
                return null;
            }
        } catch (Exception e) {
            System.out.println("=== TOKEN ERROR ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }
}