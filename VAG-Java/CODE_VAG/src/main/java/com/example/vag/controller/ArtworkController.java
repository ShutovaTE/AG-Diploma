package com.example.vag.controller;

import com.example.vag.dto.ModerationResult;
import com.example.vag.model.Artwork;
import com.example.vag.model.*;
import com.example.vag.service.*;
import com.example.vag.service.impl.ImageHashService;
import com.example.vag.util.FileUploadUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/artwork")
/**
 * Контроллер публикаций: создание, просмотр, редактирование,
 * модерация и пользовательские действия (лайки, комментарии, жалобы).
 */
public class ArtworkController {

    private final ArtworkService artworkService;
    private final CategoryService categoryService;
    private final UserService userService;
    private final ExhibitionService exhibitionService;
    private final ModerationService moderationService;
    private final ImageHashService imageHashService;
    private final NotificationService notificationService;
    private final FileUploadUtil fileUploadUtil;

    public ArtworkController(ArtworkService artworkService, CategoryService categoryService,
                             UserService userService, ExhibitionService exhibitionService,
                             ModerationService moderationService, ImageHashService imageHashService,
                             NotificationService notificationService, FileUploadUtil fileUploadUtil) {
        this.artworkService = artworkService;
        this.categoryService = categoryService;
        this.userService = userService;
        this.exhibitionService = exhibitionService;
        this.moderationService = moderationService;
        this.imageHashService = imageHashService;
        this.notificationService = notificationService;
        this.fileUploadUtil = fileUploadUtil;
    }

    @GetMapping("/list")
    public String listArtworks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {

        page = Math.max(0, page);
        
        Page<Artwork> artworkPage = artworkService.findPaginatedApprovedArtworks(PageRequest.of(page, size));
        model.addAttribute("artworks", artworkPage);
        return "artwork/list";
    }

    @GetMapping("/details/{id}")
    public String viewArtwork(@PathVariable("id") Long id, Model model) {
        Artwork artwork = artworkService.findByIdWithComments(id);
        User currentUser = null;
        try {
            currentUser = userService.getCurrentUser();
        } catch (Exception e) {
            
        }

        boolean isApproved = Artwork.ArtworkStatus.APPROVED.name().equals(artwork.getStatus());
        boolean isAuthor = currentUser != null && currentUser.getId().equals(artwork.getUser().getId());
        boolean isAdmin = currentUser != null && currentUser.hasRole("ADMIN");

        if (!isApproved && !isAuthor && !isAdmin) {
            return "redirect:/auth/access-denied";
        }

        model.addAttribute("artwork", artwork);
        model.addAttribute("isLiked", currentUser != null && artworkService.isLikedByUser(artwork, currentUser));
        model.addAttribute("isAuthenticated", currentUser != null);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isAuthor", isAuthor);

        return "artwork/details";
    }

    @GetMapping("/create")
    public String showCreateForm(@RequestParam(required = false) Long exhibitionId, Model model) {
        model.addAttribute("artwork", new Artwork());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("selectedCategoryIds", new ArrayList<Long>());
        model.addAttribute("exhibitionId", exhibitionId);
        return "artwork/create";
    }

    /*@PostMapping("/create")
    @Transactional
    public String createArtwork(
            @Valid @ModelAttribute("artwork") Artwork artwork,
            BindingResult bindingResult,
            @RequestParam("categoryIds") List<Long> categoryIds,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(required = false) Long exhibitionId,
            Model model) throws IOException {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("selectedCategoryIds", categoryIds);
            model.addAttribute("exhibitionId", exhibitionId);
            return "artwork/create";
        }

        Artwork savedArtwork = artworkService.create(artwork, imageFile, currentUser);

        if (exhibitionId != null) {
            Exhibition exhibition = exhibitionService.findById(exhibitionId).orElseThrow();
            Long currentUserId = currentUser.getId();
            Long exhibitionUserId = exhibition.getUser().getId();

            if (!exhibition.isAuthorOnly() || currentUserId.equals(exhibitionUserId)) {
                exhibition.getArtworks().add(savedArtwork);
                savedArtwork.getExhibitions().add(exhibition);
                exhibitionService.save(exhibition);
                artworkService.save(savedArtwork);
                return "redirect:/exhibition/details/" + exhibitionId;
            }
        }

        return "redirect:/user/profile?created";
    }*/

   /* @PostMapping("/create")
    @Transactional
    public String createArtwork(
            @Valid @ModelAttribute("artwork") Artwork artwork,
            BindingResult bindingResult,
            @RequestParam("categoryIds") List<Long> categoryIds,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(required = false) Long exhibitionId,
            Model model,
            RedirectAttributes redirectAttributes) throws IOException {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("selectedCategoryIds", categoryIds);
            model.addAttribute("exhibitionId", exhibitionId);
            return "artwork/create";
        }

        if (imageFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Необходимо выбрать изображение для загрузки");
            redirectAttributes.addFlashAttribute("artwork", artwork);
            redirectAttributes.addFlashAttribute("selectedCategoryIds", categoryIds);
            redirectAttributes.addFlashAttribute("exhibitionId", exhibitionId);
            return "redirect:/artwork/create";
        }

        String contentType = imageFile.getContentType();
        String originalFilename = imageFile.getOriginalFilename();
        boolean isValidType = false;
        if (contentType != null) {
            isValidType = contentType.equals("image/jpeg")
                    || contentType.equals("image/png")
                    || contentType.equals("image/webp");
        }
        boolean isValidExtension = originalFilename != null &&
                originalFilename.toLowerCase().matches(".*\\.(jpg|jpeg|png|webp)$");

        if (!isValidType && !isValidExtension) {
            redirectAttributes.addFlashAttribute("error",
                    "Неподдерживаемый формат файла. Разрешены: JPEG, PNG, WebP.");
            redirectAttributes.addFlashAttribute("artwork", artwork);
            redirectAttributes.addFlashAttribute("selectedCategoryIds", categoryIds);
            redirectAttributes.addFlashAttribute("exhibitionId", exhibitionId);
            return "redirect:/artwork/create";
        }

        Artwork savedArtwork = artworkService.create(artwork, imageFile, currentUser);

        if (exhibitionId != null) {
            Exhibition exhibition = exhibitionService.findById(exhibitionId).orElseThrow();
            Long currentUserId = currentUser.getId();
            Long exhibitionUserId = exhibition.getUser().getId();

            if (!exhibition.isAuthorOnly() || currentUserId.equals(exhibitionUserId)) {
                exhibition.getArtworks().add(savedArtwork);
                savedArtwork.getExhibitions().add(exhibition);
                exhibitionService.save(exhibition);
                artworkService.save(savedArtwork);
                return "redirect:/exhibition/details/" + exhibitionId;
            } else {
                redirectAttributes.addFlashAttribute("warning",
                        "Вы не можете добавлять работы в эту выставку (только автор выставки).");
            }
        }

        redirectAttributes.addFlashAttribute("message", "Произведение успешно опубликовано!");
        return "redirect:/user/profile?created";
    }*/

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Artwork existingArtwork = artworkService.findByIdWithCategories(id).orElseThrow();
        User currentUser = userService.getCurrentUser();

        if (!existingArtwork.getUser().getId().equals(currentUser.getId()) &&
                !currentUser.hasRole("ADMIN")) {
            return "redirect:/auth/access-denied";
        }

        List<Long> selectedCategoryIds = existingArtwork.getCategories().stream()
                .map(Category::getId)
                .collect(Collectors.toList());

        model.addAttribute("selectedCategoryIds", selectedCategoryIds);
        model.addAttribute("artwork", existingArtwork);
        model.addAttribute("categories", categoryService.findAll());
        return "artwork/edit";
    }


    @PostMapping("/comment/{id}")
    public String addComment(@PathVariable Long id,
                             @RequestParam String content,
                             RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        artworkService.addComment(id, user, content);
        return "redirect:/artwork/details/" + id;
    }

    @PostMapping("/report/{id}")
    public String reportArtwork(@PathVariable Long id,
                                @RequestParam(value = "reason", required = false) String reason,
                                RedirectAttributes redirectAttributes) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        Artwork artwork = artworkService.findById(id).orElseThrow();
        if (artwork.getUser() != null && artwork.getUser().getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("warning", "Нельзя пожаловаться на собственную публикацию.");
            return "redirect:/artwork/details/" + id;
        }

        artworkService.reportArtwork(id, currentUser, reason);

        String reasonText = (reason == null || reason.trim().isEmpty()) ? "Без комментария" : reason.trim();
        List<User> admins = userService.findAll().stream()
                .filter(user -> user.hasRole("ADMIN"))
                .collect(Collectors.toList());

        for (User admin : admins) {
            notificationService.create(
                    admin,
                    "Поступила жалоба на публикацию \"" + artwork.getTitle() + "\" от пользователя " +
                            currentUser.getUsername() + ". Причина: " + reasonText,
                    "/artwork/details/" + artwork.getId()
            );
        }

        redirectAttributes.addFlashAttribute("message", "Жалоба отправлена администратору.");
        return "redirect:/artwork/details/" + id;
    }


    @PostMapping("/edit")
    @Transactional
    public String updateArtwork(
            @Valid @ModelAttribute("artwork") Artwork artwork,
            BindingResult bindingResult,
            @RequestParam("categoryIds") List<Long> categoryIds,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "artistComment", required = false) String artistComment,
            Model model) throws IOException {

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("selectedCategoryIds", categoryIds);
            return "artwork/edit";
        }

        Artwork existingArtwork = artworkService.findByIdWithCategories(artwork.getId()).orElseThrow();
        User currentUser = userService.getCurrentUser();

        if (!existingArtwork.getUser().getId().equals(currentUser.getId()) &&
                !currentUser.hasRole("ADMIN")) {
            return "redirect:/auth/access-denied";
        }

        List<Category> categories = categoryService.findAllByIds(categoryIds);
        existingArtwork.setCategories(new HashSet<>(categories));
        existingArtwork.setTitle(artwork.getTitle());
        existingArtwork.setDescription(artwork.getDescription());
        existingArtwork.setStatus(Artwork.ArtworkStatus.PENDING.name());
        existingArtwork.setRejectionReason(null);

        if (artistComment != null && !artistComment.trim().isEmpty()) {
            existingArtwork.setArtistComment(artistComment.trim());
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = StringUtils.cleanPath(imageFile.getOriginalFilename());
            String safeFileName = fileName
                    .replace(" ", "_")
                    .replaceAll("[^a-zA-Z0-9._-]", "");
            String relativePath = "artwork-images/" + currentUser.getId() + "/" + safeFileName;
            existingArtwork.setImagePath(relativePath);
            fileUploadUtil.saveFile(currentUser.getId(), safeFileName, imageFile);
        }

        artworkService.save(existingArtwork);
        return "redirect:/user/profile?updated";
    }

    /**
     * Возвращает presigned URL для прямой загрузки в MinIO.
     */
    @GetMapping("/upload-url")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getUploadUrl(
            @RequestParam String fileName,
            @RequestParam String contentType) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        if (!isValidImageType(contentType)) {
            return ResponseEntity.badRequest().build();
        }

        // Генерируем UUID ТОЛЬКО ОДИН РАЗ
        String uuid = UUID.randomUUID().toString();
        String safeFileName = uuid + "_" + fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String objectKey = "artwork-images/" + currentUser.getId() + "/" + safeFileName;

        System.out.println("📤 Генерация presigned URL для: " + objectKey);

        // Генерируем presigned URL для ЭТОГО же objectKey
        String presignedUrl = fileUploadUtil.generatePresignedUrlForObject(objectKey, contentType);

        Map<String, String> response = new HashMap<>();
        response.put("presignedUrl", presignedUrl);
        response.put("objectKey", objectKey);
        response.put("safeFileName", safeFileName);
        response.put("publicUrl", fileUploadUtil.getPublicUrl(objectKey));

        return ResponseEntity.ok(response);
    }

    /**
     * Создание публикации с уже загруженным в MinIO файлом.
     */
    @PostMapping("/create")
    @Transactional
    public String createArtwork(
            @Valid @ModelAttribute("artwork") Artwork artwork,
            BindingResult bindingResult,
            @RequestParam("categoryIds") List<Long> categoryIds,
            @RequestParam("imagePath") String imagePath,
            @RequestParam("safeFileName") String safeFileName,
            @RequestParam(required = false) Long exhibitionId,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("selectedCategoryIds", categoryIds);
            model.addAttribute("exhibitionId", exhibitionId);
            return "artwork/create";
        }

        if (imagePath == null || imagePath.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Необходимо загрузить изображение");
            return "redirect:/artwork/create";
        }

        try {
            MultipartFile imageFile = fileUploadUtil.getAsMultipartFile(imagePath, safeFileName);

            // ИИ-модерация
            ModerationResult moderationResult = moderationService.moderateImage(imageFile, null);

            List<Category> categories = categoryService.findAllByIds(categoryIds);
            artwork.setCategories(new HashSet<>(categories));
            artwork.setImagePath(imagePath);
            artwork.setDateCreation(java.time.LocalDate.now());
            artwork.setUser(currentUser);
            artwork.setLikes(0);
            artwork.setViews(0);

            // Сохраняем отчёт ИИ
            artwork.setAiReport(moderationResult.getAiReport());

            if (!moderationResult.isApproved()) {
                if (moderationResult.isNeedsManualReview()) {
                    artwork.setStatus(Artwork.ArtworkStatus.PENDING.name());
                    artwork.setRejectionReason("Требуется ручная проверка: " + moderationResult.getManualReviewReason());
                } else {
                    artwork.setStatus(Artwork.ArtworkStatus.REJECTED.name());
                    artwork.setRejectionReason(moderationResult.getRejectionReason());
                }
            } else {
                artwork.setStatus(Artwork.ArtworkStatus.APPROVED.name());
            }

            Artwork saved = artworkService.save(artwork);

            if (!Artwork.ArtworkStatus.REJECTED.name().equals(saved.getStatus())) {
                try {
                    imageHashService.saveHash(saved, imageFile);
                } catch (Exception e) {
                    System.err.println("Не удалось сохранить хеш: " + e.getMessage());
                }
            }

            // Уведомления
            if (Artwork.ArtworkStatus.APPROVED.name().equals(saved.getStatus())) {
                notificationService.create(currentUser,
                        "Ваша публикация \"" + saved.getTitle() + "\" прошла проверку и опубликована.",
                        "/artwork/details/" + saved.getId());
            } else if (Artwork.ArtworkStatus.REJECTED.name().equals(saved.getStatus())) {
                notificationService.create(currentUser,
                        "Ваша публикация \"" + saved.getTitle() + "\" отклонена: " + saved.getRejectionReason(),
                        "/artwork/details/" + saved.getId());
            } else {
                notificationService.create(currentUser,
                        "Ваша публикация \"" + saved.getTitle() + "\" отправлена на модерацию.",
                        "/artwork/details/" + saved.getId());
            }

            if (exhibitionId != null) {
                Exhibition exhibition = exhibitionService.findById(exhibitionId).orElseThrow();
                if (!exhibition.isAuthorOnly() || currentUser.getId().equals(exhibition.getUser().getId())) {
                    exhibition.getArtworks().add(saved);
                    saved.getExhibitions().add(exhibition);
                    exhibitionService.save(exhibition);
                    artworkService.save(saved);
                    return "redirect:/exhibition/details/" + exhibitionId;
                }
            }

            redirectAttributes.addFlashAttribute("message", "Произведение успешно опубликовано!");
            return "redirect:/user/profile?created";

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при загрузке: " + e.getMessage());
            return "redirect:/artwork/create";
        }
    }
    private boolean isValidImageType(String contentType) {
        return contentType != null && (
                contentType.equals("image/jpeg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/webp")
        );
    }

    @PostMapping("/delete/{id}")
    public String deleteArtwork(@PathVariable Long id) {
        Artwork artwork = artworkService.findById(id).orElseThrow();
        User currentUser = userService.getCurrentUser();

        if (!artwork.getUser().getId().equals(currentUser.getId()) &&
                !currentUser.hasRole("ADMIN")) {
            return "redirect:/auth/access-denied";
        }

        artworkService.delete(artwork);
        return "redirect:/user/profile?deleted";
    }

    @PostMapping("/like/{id}")
    public String likeArtwork(@PathVariable Long id) {
        User user = userService.getCurrentUser();
        artworkService.likeArtwork(id, user);
        return "redirect:/artwork/details/" + id;
    }

    @PostMapping("/unlike/{id}")
    public String unlikeArtwork(@PathVariable Long id) {
        User user = userService.getCurrentUser();
        artworkService.unlikeArtwork(id, user);
        return "redirect:/artwork/details/" + id;
    }

    @GetMapping("/artworks")
    public String showArtworks(Model model, @RequestParam(defaultValue = "0") int page) {
        int pageSize = 12;
        
        page = Math.max(0, page);
        
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("id").descending());
        Page<Artwork> artworkPage = artworkService.getApprovedArtworks(pageable);
        
        if (page > 0 && artworkPage.getContent().isEmpty()) {
            return "redirect:/artwork/artworks?page=" + (page - 1);
        }
        
        model.addAttribute("artworks", artworkPage);
        return "artwork/list";
    }

}