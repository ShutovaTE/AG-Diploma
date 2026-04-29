package com.example.vag.controller;

import com.example.vag.model.Artwork;
import com.example.vag.model.Exhibition;
import com.example.vag.model.User;
import com.example.vag.service.ArtworkService;
import com.example.vag.service.NotificationService;
import com.example.vag.service.UserService;
import com.example.vag.validation.UpdateValidation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.example.vag.service.ExhibitionService;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final ArtworkService artworkService;
    private final ExhibitionService exhibitionService;
    private final NotificationService notificationService;

    public UserController(UserService userService,
                          ArtworkService artworkService,
                          ExhibitionService exhibitionService,
                          NotificationService notificationService) {
        this.userService = userService;
        this.artworkService = artworkService;
        this.exhibitionService = exhibitionService;
        this.notificationService = notificationService;
    }

    @GetMapping("/profile")
    @Transactional(readOnly = true)
    public String showProfile(Model model) {
        User user = userService.getCurrentUser();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isOwnProfile = authentication != null && authentication.getName().equals(user.getUsername());

        List<Artwork> artworks;
        if (isOwnProfile) {
            artworks = artworkService.findByUserWithDetails(user);
        } else {
            artworks = artworkService.findByUserWithDetails(user).stream()
                .filter(artwork -> "APPROVED".equals(artwork.getStatus()))
                .collect(Collectors.toList());
        }

        List<Exhibition> exhibitions = exhibitionService.findByUser(user);
        exhibitions.forEach(exhibition -> {
            exhibition.getUser().getUsername();
            if (!exhibition.getArtworks().isEmpty()) {
                exhibition.getArtworks().forEach(artwork -> {
                    artwork.getStatus();
                });
            }
        });

        model.addAttribute("user", user);
        model.addAttribute("artworks", artworks);
        model.addAttribute("exhibitions", exhibitions);
        model.addAttribute("isOwnProfile", isOwnProfile);
        return "user/profile";
    }

    @GetMapping("/profile/{id}")
    @Transactional(readOnly = true)
    public String showUserProfile(@PathVariable Long id, Model model) {
        User user = userService.findById(id).orElseThrow();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isOwnProfile = authentication != null && authentication.getName().equals(user.getUsername());

        List<Artwork> artworks;
        if (isOwnProfile) {
            artworks = artworkService.findByUserWithDetails(user);
        } else {
            artworks = artworkService.findByUserWithDetails(user).stream()
                .filter(artwork -> "APPROVED".equals(artwork.getStatus()))
                .collect(Collectors.toList());
        }

        List<Exhibition> exhibitions = exhibitionService.findByUser(user);
        exhibitions.forEach(exhibition -> {
            exhibition.getUser().getUsername();
            if (!exhibition.getArtworks().isEmpty()) {
                exhibition.getArtworks().forEach(artwork -> {
                    artwork.getStatus();
                });
            }
        });

        model.addAttribute("user", user);
        model.addAttribute("artworks", artworks);
        model.addAttribute("exhibitions", exhibitions);
        model.addAttribute("isOwnProfile", isOwnProfile);
        model.addAttribute("isAuthenticated", authentication != null);
        return "user/profile";
    }

    @GetMapping("/settings")
    public String showSettings(Model model) {
        User user = userService.getCurrentUser();
        model.addAttribute("user", user);
        return "user/settings";
    }

    @PostMapping("/settings")
    public String updateSettings(@Validated(UpdateValidation.class) @ModelAttribute("user") User user,
                                 BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "user/settings";
        }

        User currentUser = userService.getCurrentUser();

        if (!user.getUsername().equals(currentUser.getUsername())) {
            if (userService.findByUsername(user.getUsername()).isPresent()) {
                bindingResult.rejectValue("username", "error.user", "Пользователь с таким именем уже существует");
                return "user/settings";
            }
        }

        if (!user.getEmail().equals(currentUser.getEmail())) {
            if (userService.findByEmail(user.getEmail()).isPresent()) {
                bindingResult.rejectValue("email", "error.user", "Пользователь с таким email уже существует");
                return "user/settings";
            }
        }

        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            if (!user.getPassword().equals(user.getConfirmPassword())) {
                bindingResult.rejectValue("confirmPassword", "error.user", "Пароли не совпадают");
                return "user/settings";
            }
        }

        userService.update(user);

        if (!user.getUsername().equals(currentUser.getUsername())) {
            SecurityContextHolder.getContext().setAuthentication(null);
        }

        return "redirect:/user/profile?updated";
    }

    @GetMapping("/liked")
    public String likedArtworks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {

        User user = userService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        Page<Artwork> artworkPage = artworkService.findLikedArtworks(user, pageable);

        model.addAttribute("artworks", artworkPage);
        return "user/liked";
    }

    @GetMapping("/notifications")
    public String notifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        User user = userService.getCurrentUser();
        notificationService.markAllAsRead(user);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("notifications", notificationService.findAll(user, pageable));
        return "user/notifications";
    }

    @RequestMapping(value = "/notifications/mark-read", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public void markNotificationsRead() {
        User user = userService.getCurrentUser();
        if (user != null) {
            notificationService.markAllAsRead(user);
        }
    }

    @PostMapping("/notifications/delete/{id}")
    public String deleteNotification(@PathVariable("id") Long notificationId,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        User user = userService.getCurrentUser();
        notificationService.deleteForUser(user, notificationId);
        return "redirect:/user/notifications?page=" + page + "&size=" + size;
    }

    @PostMapping("/notifications/delete")
    public String deleteNotifications(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(value = "selectedIds", required = false) List<Long> selectedIds,
                                      @RequestParam(value = "deleteAll", defaultValue = "false") boolean deleteAll,
                                      Model model) {
        User user = userService.getCurrentUser();

        if (deleteAll) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<com.example.vag.model.Notification> notificationsPage = notificationService.findAll(user, pageable);
            List<Long> idsOnPage = notificationsPage.getContent().stream()
                    .map(com.example.vag.model.Notification::getId)
                    .collect(Collectors.toList());
            notificationService.deleteForUser(user, idsOnPage);
        } else {
            notificationService.deleteForUser(user, selectedIds == null ? new ArrayList<>() : selectedIds);
        }

        return "redirect:/user/notifications?page=" + page + "&size=" + size;
    }

    @GetMapping("/list")
    @Transactional(readOnly = true)
    public String listUsers(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("username"));
        Page<User> userPage = userService.findAll(pageable);

        userPage.getContent().forEach(user -> {
            user.getArtworks().size();
            user.getExhibitions().size();
        });

        model.addAttribute("users", userPage);
        return "user/list";
    }
}