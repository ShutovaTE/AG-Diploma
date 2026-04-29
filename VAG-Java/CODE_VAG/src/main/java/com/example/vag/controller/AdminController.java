package com.example.vag.controller;

import com.example.vag.dto.ModerationResult;
import com.example.vag.model.Artwork;
import com.example.vag.model.Category;
import com.example.vag.model.Exhibition;
import com.example.vag.model.User;
import com.example.vag.service.*;
import com.example.vag.util.FileUploadUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import javax.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final ArtworkService artworkService;
    private final CategoryService categoryService;
    private final ExhibitionService exhibitionService;

    private final ModerationService moderationService;
    private final FileUploadUtil fileUploadUtil;

    public AdminController(UserService userService, ArtworkService artworkService,
                           CategoryService categoryService, ExhibitionService exhibitionService,
                           ModerationService moderationService, FileUploadUtil fileUploadUtil) {
        this.userService = userService;
        this.artworkService = artworkService;
        this.categoryService = categoryService;
        this.exhibitionService = exhibitionService;
        this.moderationService = moderationService;
        this.fileUploadUtil = fileUploadUtil;
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userService.findAll();
        model.addAttribute("users", users);
        return "admin/users";
    }

    @PostMapping("/artworks/recheck/{id}")
    public String recheckArtwork(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Artwork artwork = artworkService.findById(id).orElseThrow();

            // Получаем файл из MinIO
            InputStream inputStream = fileUploadUtil.getFile(artwork.getImagePath());
            byte[] bytes = inputStream.readAllBytes();
            inputStream.close();

            String filename = artwork.getImagePath().substring(artwork.getImagePath().lastIndexOf('/') + 1);

            MultipartFile multipartFile = new MultipartFile() {
                @Override
                public String getName() { return "imageFile"; }

                @Override
                public String getOriginalFilename() { return filename; }

                @Override
                public String getContentType() {
                    String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
                    switch (ext) {
                        case "webp": return "image/webp";
                        case "png": return "image/png";
                        default: return "image/jpeg";
                    }
                }

                @Override
                public boolean isEmpty() { return bytes == null || bytes.length == 0; }

                @Override
                public long getSize() { return bytes.length; }

                @Override
                public byte[] getBytes() { return bytes; }

                @Override
                public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }

                @Override
                public void transferTo(File dest) throws IOException {
                    Files.write(dest.toPath(), bytes);
                }
            };

            // Повторная модерация
            ModerationResult result = moderationService.moderateImage(multipartFile, artwork.getId());

            // Сохраняем AI-отчёт в artwork
            artwork.setAiReport(result.getAiReport());
            artworkService.save(artwork);

            if (result.isApproved()) {
                artworkService.approveArtwork(id);
                redirectAttributes.addFlashAttribute("message",
                        "Изображение прошло повторную проверку и одобрено");
            } else if (result.isNeedsManualReview()) {
                redirectAttributes.addFlashAttribute("warning",
                        result.getManualReviewReason());

                // Сохраняем причину в artwork
                artwork.setRejectionReason("Требуется ручная проверка: " + result.getManualReviewReason());
                artworkService.save(artwork);
            } else {
                artworkService.rejectArtwork(id, result.getRejectionReason());
                redirectAttributes.addFlashAttribute("message",
                        result.getRejectionReason());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при проверке: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/admin/artworks";
    }

    @GetMapping("/artworks")
    public String listArtworks(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Artwork> artworkPage;

        if (status != null && !status.isEmpty()) {
            artworkPage = artworkService.findByStatus(status.toUpperCase(), pageable);
            System.out.println("Выбран статус фильтрации: " + status);
        } else {
            artworkPage = artworkService.findAllPaginated(pageable);
            
            System.out.println("Найдено " + artworkPage.getTotalElements() + " публикаций");
            
            Map<String, Long> statusCounts = new HashMap<>();
            for (Artwork artwork : artworkPage.getContent()) {
                String stat = artwork.getStatus();
                statusCounts.put(stat, statusCounts.getOrDefault(stat, 0L) + 1);
            }
            
            statusCounts.forEach((stat, count) -> 
                System.out.println("Статус " + stat + ": " + count + " публикаций"));
        }

        model.addAttribute("artworks", artworkPage);
        return "admin/artworks";
    }

    @PostMapping("/artworks/approve/{id}")
    public String approveArtwork(@PathVariable Long id) {
        artworkService.approveArtwork(id);
        return "redirect:/admin/artworks?approved";
    }

    @GetMapping("/artworks/reject/{id}")
    public String showRejectForm(@PathVariable Long id, Model model) {
        Artwork artwork = artworkService.findByIdWithCategories(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid artwork ID"));
        model.addAttribute("artwork", artwork);
        return "admin/reject-artwork";
    }

    @PostMapping("/artworks/reject/{id}")
    public String rejectArtwork(@PathVariable Long id, 
                                @RequestParam String rejectionReason,
                                RedirectAttributes redirectAttributes) {
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Причина отклонения обязательна для заполнения");
            return "redirect:/admin/artworks/reject/" + id;
        }
        artworkService.rejectArtwork(id, rejectionReason.trim());
        redirectAttributes.addFlashAttribute("message", "Публикация отклонена");
        return "redirect:/admin/artworks?rejected";
    }

    @GetMapping("/categories")
    public String listCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Category> categoryPage = categoryService.findAll(pageable);

        categoryPage.forEach(category ->
                category.setApprovedArtworksCount(
                        artworkService.countApprovedArtworksByCategoryId(category.getId())
                ));

        model.addAttribute("categories", categoryPage);
        return "admin/categories";
    }

    @GetMapping("/categories/create")
    public String showCreateForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin/create-category";
    }

    @PostMapping("/categories/create")
    public String createCategories(@Valid @ModelAttribute("category") Category category, 
                                   BindingResult bindingResult, 
                                   RedirectAttributes redirectAttributes, 
                                   Model model) {
        
        if (categoryService.findByName(category.getName()).isPresent()) {
            bindingResult.rejectValue("name", "error.category", "Категория с таким именем уже существует");
        }
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("category", category);
            return "admin/create-category";
        }
        
        categoryService.save(category);
        redirectAttributes.addFlashAttribute("message", "Категория успешно создана!");
        redirectAttributes.addAttribute("created", true);
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Category category = categoryService.findById(id).orElseThrow();
        model.addAttribute("category", category);
        return "admin/edit-category";
    }

    @PostMapping("/categories/edit/{id}")
    public String updateCategory(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("category") Category category,
            BindingResult result,
            RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            return "admin/edit-category";
        }
        categoryService.update(id, category);
        redirectAttributes.addFlashAttribute("success", "Category updated");
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Category category = categoryService.findById(id).orElseThrow();
        long approvedArtworksCount = artworkService.countApprovedArtworksByCategoryId(id);
        
        if (approvedArtworksCount > 0) {
            redirectAttributes.addFlashAttribute("error", "Невозможно удалить категорию, так как она используется в " + approvedArtworksCount + " публикациях");
            return "redirect:/admin/categories";
        }
        
        categoryService.delete(category);
        redirectAttributes.addFlashAttribute("success", "Категория успешно удалена");
        return "redirect:/admin/categories";
    }


    @GetMapping("/exhibitions")
    public String listExhibitions(Model model) {
        List<Exhibition> exhibitions = exhibitionService.findAll();
        model.addAttribute("exhibitions", exhibitions);
        return "admin/exhibitions";
    }

    @PostMapping("/comment/{id}")
    public String addComment(@PathVariable Long id,
                             @RequestParam String content,
                             RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        artworkService.addComment(id, user, content);
        return "redirect:/artwork/details/" + id;
    }
}