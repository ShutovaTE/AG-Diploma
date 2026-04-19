package com.example.vag.service.impl;

import com.example.vag.model.*;
import com.example.vag.recommendation.service.RecommendationService;
import com.example.vag.repository.*;
import com.example.vag.service.ArtworkService;
import com.example.vag.service.ExhibitionService;
import com.example.vag.service.ModerationService;
import com.example.vag.service.NotificationService;
import com.example.vag.util.FileUploadUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import com.example.vag.dto.ModerationResult;
import com.example.vag.service.ModerationService;
import com.example.vag.service.impl.ImageHashService;

import javax.persistence.EntityNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ArtworkServiceImpl implements ArtworkService {

    private final ArtworkRepository artworkRepository;
    private final CategoryRepository categoryRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final FileUploadUtil fileUploadUtil;
    private final ExhibitionService exhibitionService;
    private final NotificationService notificationService;
    private final RecommendationService recommendationService;
    private final ModerationService moderationService;
    private final ImageHashService imageHashService;

    public ArtworkServiceImpl(ArtworkRepository artworkRepository,
                              CategoryRepository categoryRepository,
                              CommentRepository commentRepository,
                              LikeRepository likeRepository,
                              FileUploadUtil fileUploadUtil,
                              ExhibitionService exhibitionService,
                              NotificationService notificationService, 
                              RecommendationService recommendationService,
                              ModerationService moderationService,
                              ImageHashService imageHashService) {
        this.artworkRepository = artworkRepository;
        this.categoryRepository = categoryRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.fileUploadUtil = fileUploadUtil;
        this.exhibitionService = exhibitionService;
        this.notificationService = notificationService;
        this.recommendationService = recommendationService;
        this.moderationService = moderationService;
        this.imageHashService = imageHashService;
    }

    @Override
    public Artwork save(Artwork artwork) {
        return artworkRepository.save(artwork);
    }

    @Override
    public Artwork create(Artwork artwork, MultipartFile imageFile, User user) throws IOException {
        // 1. Запускаем AI-модерацию
        ModerationResult moderationResult = moderationService.moderateImage(imageFile, null);

        List<Category> categories = categoryRepository.findAllByIds(artwork.getCategoryIds());
        artwork.setCategories(new HashSet<>(categories));

        String originalFileName = StringUtils.cleanPath(imageFile.getOriginalFilename());
        String safeFileName = originalFileName
                .replace(" ", "_")
                .replaceAll("[^a-zA-Z0-9._-]", "");

        String relativePath = "artwork-images/" + user.getId() + "/" + safeFileName;
        artwork.setImagePath(relativePath);

        fileUploadUtil.saveFile(user.getId(), safeFileName, imageFile);

        artwork.setDateCreation(LocalDate.now());
        artwork.setUser(user);
        artwork.setLikes(0);
        artwork.setViews(0);

        // 2. Устанавливаем статус на основе результата модерации
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

        Artwork saved = artworkRepository.save(artwork);

        // 3. Сохраняем хеш для будущих проверок (если не отклонено)
        if (!Artwork.ArtworkStatus.REJECTED.name().equals(saved.getStatus())) {
            try {
                imageHashService.saveHash(saved, imageFile);
            } catch (Exception e) {
                System.err.println("Не удалось сохранить хеш изображения: " + e.getMessage());
            }
        }

        // 4. Отправляем уведомление
        if (Artwork.ArtworkStatus.APPROVED.name().equals(saved.getStatus())) {
            notificationService.create(user,
                    "Ваша публикация \"" + saved.getTitle() + "\" прошла автоматическую проверку и опубликована.",
                    "/artwork/details/" + saved.getId());
        } else if (Artwork.ArtworkStatus.REJECTED.name().equals(saved.getStatus())) {
            notificationService.create(user,
                    "Ваша публикация \"" + saved.getTitle() + "\" отклонена: " + saved.getRejectionReason(),
                    "/artwork/details/" + saved.getId());
        } else {
            notificationService.create(user,
                    "Ваша публикация \"" + saved.getTitle() + "\" отправлена на модерацию. " + saved.getRejectionReason(),
                    "/artwork/details/" + saved.getId());
        }

        recommendationService.clearModelCache();
        return saved;
    }
    @Override
    @Transactional(readOnly = true)
    public Page<Artwork> findPaginatedApprovedArtworks(Pageable pageable) {
        return artworkRepository.findApprovedArtworks(pageable);
    }

    @Override
    public Page<Artwork> findByCategoryId(Long categoryId, Pageable pageable) {
        return artworkRepository.findByCategoryId(categoryId, pageable);
    }

    @Override
    public List<Artwork> findAll() {
        return artworkRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Artwork> findByStatus(String status) {
        return artworkRepository.findByStatus(status);
    }



    @Override
    public List<Artwork> findByUserWithDetails(User user) {
        return artworkRepository.findByUserWithDetails(user.getId());
    }

    @Override
    public List<Artwork> findByExhibitionId(Long exhibitionId) {
        return artworkRepository.findByExhibitionId(exhibitionId);
    }

    @Override
    public Optional<Artwork> findById(Long id) {
        return artworkRepository.findById(id);
    }

    @Override
    public void delete(Artwork artwork) {
        imageHashService.deleteByArtworkId(artwork.getId());
        Artwork artworkWithExhibitions = artworkRepository.findById(artwork.getId())
                .orElseThrow(() -> new IllegalArgumentException("Произведение искусства не найдено"));

        Set<Exhibition> exhibitions = new HashSet<>(artworkWithExhibitions.getExhibitions());
        for (Exhibition exhibition : exhibitions) {
            exhibitionService.removeArtworkFromExhibition(exhibition.getId(), artwork.getId());
        }

        String imagePath = artworkWithExhibitions.getImagePath();
        if (imagePath != null && !imagePath.isBlank()) {
            try {
                fileUploadUtil.deleteFile(imagePath);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка удаления файла из MinIO: " + imagePath, e);
            }
        }

        artworkRepository.delete(artworkWithExhibitions);
        recommendationService.clearModelCache();
    }

    @Override
    public void approveArtwork(Long artworkId) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid artwork ID"));
        artwork.setStatus(Artwork.ArtworkStatus.APPROVED.name());
        artwork.setRejectionReason(null); // Очищаем причину отклонения при одобрении
        artworkRepository.save(artwork);
        notificationService.create(
                artwork.getUser(),
                "Ваша публикация \"" + artwork.getTitle() + "\" была одобрена.",
                "/artwork/details/" + artwork.getId()
        );
        recommendationService.clearModelCache();
    }

    @Override
    public void rejectArtwork(Long artworkId, String rejectionReason) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid artwork ID"));
        artwork.setStatus(Artwork.ArtworkStatus.REJECTED.name());
        artwork.setRejectionReason(rejectionReason);
        artworkRepository.save(artwork);
        notificationService.create(
                artwork.getUser(),
                "Ваша публикация \"" + artwork.getTitle() + "\" была отклонена. Причина: " + rejectionReason,
                "/artwork/details/" + artwork.getId()
        );
    }



    @Override
    public void likeArtwork(Long artworkId, User user) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid artwork ID"));

        if (!likeRepository.existsByArtworkAndUser(artwork, user)) {
            Like like = new Like();
            like.setArtwork(artwork);
            like.setUser(user);
            likeRepository.save(like);
            artwork.setLikes(artwork.getLikes() + 1);
            artworkRepository.save(artwork);
            notifyArtworkAuthorAboutLike(artwork, user);
        }
        recommendationService.clearModelCache();
    }

    @Override
    public long countApprovedArtworksByCategoryId(Long categoryId) {
        return artworkRepository.countApprovedArtworksByCategoryId(categoryId);
    }

    @Override
    public void unlikeArtwork(Long artworkId, User user) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid artwork ID"));

        likeRepository.findByArtworkAndUser(artwork, user).ifPresent(like -> {
            likeRepository.delete(like);
            artwork.setLikes(artwork.getLikes() - 1);
            artworkRepository.save(artwork);
        });
        recommendationService.clearModelCache();
    }

    @Override
    public boolean isLikedByUser(Artwork artwork, User user) {
        return likeRepository.existsByArtworkAndUser(artwork, user);
    }

    @Override
    public void addComment(Long artworkId, User user, String content) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new EntityNotFoundException("Artwork not found"));

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setUser(user);
        comment.setArtwork(artwork);

        commentRepository.save(comment);
        notifyArtworkAuthorAboutComment(artwork, user, content);
        recommendationService.clearModelCache();
    }

    @Override
    public Artwork findByIdWithComments(Long id) {
        Artwork artwork = artworkRepository.findByIdWithComments(id).orElseThrow();
        artworkRepository.findByIdWithCategories(id).ifPresent(a ->
                artwork.setCategories(a.getCategories())
        );
        return artwork;
    }

    @Override
    public Page<Artwork> getApprovedArtworks(Pageable pageable) {
        return artworkRepository.findApprovedArtworks(pageable);
    }

    @Override
    public Optional<Artwork> findByIdWithCategories(Long id) {
        return artworkRepository.findByIdWithCategories(id);
    }

    @Override
    public Page<Artwork> findAll(Pageable pageable) {
        return artworkRepository.findAll(pageable);
    }

    @Override
    public Page<Artwork> findAllPaginated(Pageable pageable) {
        return artworkRepository.findAllPaginated(pageable);
    }

    @Override
    public Page<Artwork> findByStatus(String status, Pageable pageable) {
        return artworkRepository.findByStatus(status, pageable);
    }

    @Override
    public Page<Artwork> findByUser(User user, Pageable pageable) {
        return artworkRepository.findByUser(user, pageable);
    }

    @Override
    public Page<Artwork> findByUserAndStatus(User user, String status, Pageable pageable) {
        return artworkRepository.findByUserAndStatus(user, status, pageable);
    }
    @Override
    public List<Artwork> findLikedArtworks(User user) {
        List<Like> likes = likeRepository.findByUser(user);
        return likes.stream()
                .map(Like::getArtwork)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Artwork> findLikedArtworks(User user, Pageable pageable) {
        Page<Like> likes = likeRepository.findByUserWithArtworkDetails(user, pageable);
        return likes.map(Like::getArtwork);
    }
    @Override
    public Page<Artwork> findByExhibitionId(Long exhibitionId, Pageable pageable) {
        return artworkRepository.findByExhibitionId(exhibitionId, pageable);
    }

    private void notifyArtworkAuthorAboutLike(Artwork artwork, User actor) {
        User author = artwork.getUser();
        if (author == null || actor == null || author.getId().equals(actor.getId())) {
            return;
        }
        notificationService.create(
                author,
                "Пользователь " + actor.getUsername() + " поставил лайк вашей публикации \"" + artwork.getTitle() + "\".",
                "/artwork/details/" + artwork.getId()
        );
    }

    private void notifyArtworkAuthorAboutComment(Artwork artwork, User actor, String content) {
        User author = artwork.getUser();
        if (author == null || actor == null || author.getId().equals(actor.getId())) {
            return;
        }
        String commentPreview = buildCommentPreview(content);
        notificationService.create(
                author,
                "Пользователь " + actor.getUsername() + " прокомментировал вашу публикацию \"" + artwork.getTitle() + "\": " + commentPreview,
                "/artwork/details/" + artwork.getId()
        );
    }

    private String buildCommentPreview(String content) {
        if (content == null || content.isBlank()) {
            return "\"\"";
        }
        String normalized = content.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= 80) {
            return "\"" + normalized + "\"";
        }
        return "\"" + normalized.substring(0, 80) + "...\"";
    }
}