package com.example.vag.service.impl;

import com.example.vag.model.Role;
import com.example.vag.model.User;
import com.example.vag.repository.RoleRepository;
import com.example.vag.repository.UserRepository;
import com.example.vag.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            Optional<User> existingUser = userRepository.findById(user.getId());
            if (existingUser.isPresent()) {
                User dbUser = existingUser.get();
                if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                    dbUser.setPassword(passwordEncoder.encode(user.getPassword()));
                }
                dbUser.setUsername(user.getUsername());
                dbUser.setEmail(user.getEmail());
                dbUser.setDescription(user.getDescription()); // ДОБАВЛЕНО
                return userRepository.save(dbUser);
            }
        }
        return userRepository.save(user);
    }

    @Override
    public User register(User user) {
        Role userRole = roleRepository.findByName(Role.RoleName.ARTIST)
                .orElseThrow(() -> new IllegalStateException("Artist role not found"));

        user.setRole(userRole);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllWithArtworksCount() {
        List<User> users = userRepository.findAllArtistsWithArtworks();
        // Инициализируем коллекции для предотвращения LazyInitializationException
        users.forEach(user -> {
            user.getArtworks().size(); // Инициализируем коллекцию
            if (user.getExhibitions() != null) {
                user.getExhibitions().size(); // Инициализируем коллекцию
            }
        });
        return users;
    }

    @Override
    public Optional<User> findById(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Инициализируем коллекции
            user.getArtworks().size();
            if (user.getExhibitions() != null) {
                user.getExhibitions().size();
            }
        }
        return userOpt;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public void delete(User user) {
        userRepository.delete(user);
    }

    @Override
    public User update(User updatedUser) {
        User user = userRepository.findById(updatedUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (updatedUser.getUsername() != null) {
            user.setUsername(updatedUser.getUsername());
        }

        if (updatedUser.getEmail() != null) {
            user.setEmail(updatedUser.getEmail());
        }

        if (updatedUser.getDescription() != null) {
            user.setDescription(updatedUser.getDescription());
        }

        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }

        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public User authenticate(String username, String password) {
        System.out.println("=== AUTHENTICATING USER ===");
        System.out.println("Username: " + username);

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Проверяем пароль
            if (passwordEncoder.matches(password, user.getPassword())) {
                System.out.println("Authentication successful for: " + username);
                System.out.println("User description: " + user.getDescription());
                System.out.println("User role: " + (user.getRole() != null ? user.getRole().getName() : "null"));
                return user;
            } else {
                System.out.println("Invalid password for: " + username);
                throw new RuntimeException("Invalid password");
            }
        } else {
            System.out.println("User not found: " + username);
            throw new RuntimeException("User not found");
        }
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        System.out.println("=== GETTING CURRENT USER ===");
        System.out.println("Authentication: " + authentication);

        if (authentication == null || !authentication.isAuthenticated()) {
            System.out.println("User not authenticated");
            return null;
        }

        Object principal = authentication.getPrincipal();
        System.out.println("Principal type: " + (principal != null ? principal.getClass().getName() : "null"));
        System.out.println("Principal: " + principal);

        // Если principal - строка "anonymousUser", значит пользователь не аутентифицирован
        if (principal instanceof String && "anonymousUser".equals(principal)) {
            System.out.println("User is anonymous");
            return null;
        }

        // Для мобильных пользователей
        if (principal instanceof User) {
            User user = (User) principal;
            System.out.println("Mobile user found: " + user.getUsername());
            System.out.println("User description: " + user.getDescription());
            System.out.println("User role: " + (user.getRole() != null ? user.getRole().getName() : "null"));
            return user;
        }
        // Для веб-пользователей Spring Security
        else if (principal instanceof org.springframework.security.core.userdetails.User) {
            org.springframework.security.core.userdetails.User securityUser =
                    (org.springframework.security.core.userdetails.User) principal;
            String username = securityUser.getUsername();
            System.out.println("Web user found: " + username);

            // Ищем пользователя в базе данных
            Optional<User> userOpt = findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                System.out.println("Web user details: " + user.getUsername() +
                        ", role: " + user.getRole().getName() +
                        ", description: " + user.getDescription());
                return user;
            } else {
                System.out.println("Web user not found in database: " + username);
                return null;
            }
        }

        System.out.println("Unknown principal type: " + (principal != null ? principal.getClass().getName() : "null"));
        return null;
    }

    @Override
    public List<User> findRandomArtists(int count) {
        List<User> artists = userRepository.findRandomArtists(count);
        // Инициализируем коллекции для предотвращения LazyInitializationException
        artists.forEach(artist -> {
            artist.getArtworks().size();
        });
        return artists.size() > count ? artists.subList(0, count) : artists;
    }
}