package com.example.vag.service.impl;

import com.example.vag.model.*;
import com.example.vag.recommendation.dto.RecommendationDTO;
import com.example.vag.recommendation.service.RecommendationService;
import com.example.vag.repository.*;
import com.example.vag.service.ArtworkService;
import com.example.vag.service.ExhibitionService;
import com.example.vag.service.ImageFeatureService;
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

import javax.persistence.EntityNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
/**
 * Основная бизнес-логика работы с публикациями:
 * сохранение, модерация, реакции пользователей и уведомления.
 */
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
    private final ImageFeatureService imageFeatureService;

    public ArtworkServiceImpl(ArtworkRepository artworkRepository,
                              CategoryRepository categoryRepository,
                              CommentRepository commentRepository,
                              LikeRepository likeRepository,
                              FileUploadUtil fileUploadUtil,
                              ExhibitionService exhibitionService,
                              NotificationService notificationService, 
                              RecommendationService recommendationService,
                              ModerationService moderationService,
                              ImageHashService imageHashService,
                              ImageFeatureService imageFeatureService) {
        this.artworkRepository = artworkRepository;
        this.categoryRepository = categoryRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.fileUploadUtil = fileUploadUtil;
        this.exhibitionService = exhibitionService;
        this.notificationService = notificationService;
        this.recommendationService = recommendationService;
        this.moderationService = moderationService;
        this.imageFeatureService = imageFeatureService;
        this.imageHashService = imageHashService;
    }

    @Override
    public Artwork save(Artwork artwork) {
        return artworkRepository.save(artwork);
    }

    @Override
    public Artwork create(Artwork artwork, MultipartFile imageFile, User user) throws IOException {
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

        applyImageFeatures(artwork, imageFile);

        // Сохраняем отчёт ИИ
        artwork.setAiReport(moderationResult.getAiReport());

        // Сохраняем ID похожей работы
        artwork.setSimilarArtworkId(moderationResult.getSimilarArtworkId());
        artwork.setSimilarArtworkTitle(moderationResult.getSimilarArtworkTitle());

        // Формируем причину для уведомления (чистый текст) и для админ-панели (HTML)
        String reasonForUser = null;      // для уведомлений — без HTML
        String reasonForAdmin = null;     // для админ-панели — с HTML-ссылкой

        if (!moderationResult.isApproved()) {
            if (moderationResult.isNeedsManualReview()) {
                artwork.setStatus(Artwork.ArtworkStatus.PENDING.name());

                reasonForUser = moderationResult.getManualReviewReason();           // чистый текст
                reasonForAdmin = moderationResult.getManualReviewReasonHtml();      // HTML с ссылкой

                artwork.setRejectionReason(reasonForAdmin != null ? reasonForAdmin : reasonForUser);
            } else {
                artwork.setStatus(Artwork.ArtworkStatus.REJECTED.name());

                reasonForUser = moderationResult.getRejectionReason();              // чистый текст
                reasonForAdmin = moderationResult.getRejectionReasonHtml();         // HTML с ссылкой

                artwork.setRejectionReason(reasonForAdmin != null ? reasonForAdmin : reasonForUser);
            }
        } else {
            artwork.setStatus(Artwork.ArtworkStatus.APPROVED.name());
        }

        Artwork saved = artworkRepository.save(artwork);

        // Сохраняем хеш
        if (!Artwork.ArtworkStatus.REJECTED.name().equals(saved.getStatus())) {
            try {
                imageHashService.saveHash(saved, imageFile);
            } catch (Exception e) {
                System.err.println("Не удалось сохранить хеш изображения: " + e.getMessage());
            }
        }

        // Уведомления — используем ЧИСТЫЙ ТЕКСТ (без HTML)
        if (Artwork.ArtworkStatus.APPROVED.name().equals(saved.getStatus())) {
            notificationService.create(user,
                    "Ваша публикация \"" + saved.getTitle() + "\" прошла проверку и опубликована.",
                    "/artwork/details/" + saved.getId());
        } else if (Artwork.ArtworkStatus.REJECTED.name().equals(saved.getStatus())) {
            notificationService.create(user,
                    "Ваша публикация \"" + saved.getTitle() + "\" отклонена: " +
                            (reasonForUser != null ? reasonForUser : saved.getRejectionReason()),
                    "/artwork/details/" + saved.getId());
        } else {
            notificationService.create(user,
                    "Ваша публикация \"" + saved.getTitle() + "\" отправлена на модерацию.",
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
    @Transactional(readOnly = true)
    public Page<Artwork> findPaginatedApprovedArtworksByLikes(Pageable pageable) {
        return artworkRepository.findApprovedArtworksByLikes(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Artwork> findPaginatedApprovedArtworksByDate(Pageable pageable) {
        return artworkRepository.findApprovedArtworksByDate(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.example.vag.recommendation.dto.RecommendationDTO> getRecommendationsForUser(Long userId) {
        return recommendationService.getRecommendationsForUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Artwork> findSimilarApprovedArtworks(Artwork artwork, int limit) {
        if (artwork == null || limit <= 0) {
            return Collections.emptyList();
        }

        Set<Long> currentCategoryIds = artwork.getCategories() == null ?
                Collections.emptySet() : artwork.getCategories().stream()
                .map(category -> category.getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Long currentAuthorId = artwork.getUser() != null ? artwork.getUser().getId() : null;
        List<Artwork> candidates = artworkRepository.findApprovedArtworksExcept(artwork.getId());
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        double maxLikes = candidates.stream()
                .mapToDouble(art -> art.getLikes())
                .max()
                .orElse(1.0);

        List<Artwork> ranked = candidates.stream()
                .sorted((a, b) -> {
                    double scoreA = calculateSimilarityScore(artwork, currentCategoryIds, currentAuthorId, a, maxLikes);
                    double scoreB = calculateSimilarityScore(artwork, currentCategoryIds, currentAuthorId, b, maxLikes);
                    return Double.compare(scoreB, scoreA);
                })
                .limit(limit)
                .collect(Collectors.toList());

        for (Artwork candidate : ranked) {
            int percent = (int) Math.round(calculateSimilarityScore(artwork, currentCategoryIds, currentAuthorId, candidate, maxLikes) * 100);
            candidate.setMatchPercentage(Math.min(100, Math.max(0, percent)));
        }

        return ranked;
    }

    private double calculateSimilarityScore(Artwork source,
                                            Set<Long> sourceCategoryIds,
                                            Long sourceAuthorId,
                                            Artwork candidate,
                                            double maxLikes) {
        if (candidate == null) {
            return 0.0;
        }

        Set<Long> candidateCategoryIds = candidate.getCategories() == null ?
                Collections.emptySet() : candidate.getCategories().stream()
                .map(category -> category.getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        long categoryOverlap = candidateCategoryIds.stream()
                .filter(sourceCategoryIds::contains)
                .count();

        double categoryScore = sourceCategoryIds.isEmpty() ? 0.0 : (double) categoryOverlap / sourceCategoryIds.size();
        double authorScore = sourceAuthorId != null && candidate.getUser() != null && Objects.equals(candidate.getUser().getId(), sourceAuthorId)
                ? 1.0 : 0.0;
        double popularityScore = candidate.getLikes() / maxLikes;

        // Основной вес даётся категориям, меньше — автору, немного — популярности.
        double objectScore = calculateObjectMatchScore(source.getDetectedObjects(), candidate.getDetectedObjects());
        double colorScore = calculateColorMatchScore(source, candidate);

        // Перераспределённые веса: категории остались важными, объектная похожесть добавляет контекст,
        // цветовая похожесть учитывает визуальное совпадение без серьёзной нагрузки.
        return categoryScore * 0.40
                + objectScore * 0.25
                + colorScore * 0.20
                + authorScore * 0.10
                + popularityScore * 0.05;
    }

    private double calculateObjectMatchScore(String sourceDetectedObjects, String candidateDetectedObjects) {
        if (sourceDetectedObjects == null || sourceDetectedObjects.isBlank()
                || candidateDetectedObjects == null || candidateDetectedObjects.isBlank()) {
            return 0.0;
        }

        var sourceSet = parseDetectedObjects(sourceDetectedObjects);
        var candidateSet = parseDetectedObjects(candidateDetectedObjects);
        if (sourceSet.isEmpty() || candidateSet.isEmpty()) {
            return 0.0;
        }

        long intersection = sourceSet.stream().filter(candidateSet::contains).count();
        long union = sourceSet.size() + candidateSet.size() - intersection;
        if (union == 0) {
            return 0.0;
        }
        return (double) intersection / union;
    }

    private double calculateColorMatchScore(Artwork source, Artwork candidate) {
        if (source.getAverageRed() == null || source.getAverageGreen() == null || source.getAverageBlue() == null
                || candidate.getAverageRed() == null || candidate.getAverageGreen() == null || candidate.getAverageBlue() == null) {
            return 0.0;
        }

        double dr = source.getAverageRed() - candidate.getAverageRed();
        double dg = source.getAverageGreen() - candidate.getAverageGreen();
        double db = source.getAverageBlue() - candidate.getAverageBlue();
        double distance = Math.sqrt(dr * dr + dg * dg + db * db);
        double maxDistance = Math.sqrt(3.0 * 255.0 * 255.0);
        return Math.max(0.0, 1.0 - (distance / maxDistance));
    }

    private java.util.Set<String> parseDetectedObjects(String detectedObjects) {
        if (detectedObjects == null || detectedObjects.isBlank()) {
            return java.util.Collections.emptySet();
        }
        return java.util.Arrays.stream(detectedObjects.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .map(String::toLowerCase)
                .collect(java.util.stream.Collectors.toSet());
    }

    private void applyImageFeatures(Artwork artwork, org.springframework.web.multipart.MultipartFile imageFile) {
        try {
            var analysis = imageFeatureService.analyze(imageFile);
            artwork.setAverageRed(analysis.getAverageRed());
            artwork.setAverageGreen(analysis.getAverageGreen());
            artwork.setAverageBlue(analysis.getAverageBlue());
            artwork.setColorHistogram(analysis.getColorHistogram());
            artwork.setDetectedObjects(analysis.getDetectedObjects());
        } catch (Exception e) {
            System.err.println("Ошибка анализа изображения: " + e.getMessage());
        }
    }

    @Override
    public void rescanArtworkFeatures(Long artworkId) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid artwork ID"));
        if (artwork.getImagePath() == null || artwork.getImagePath().isBlank()) {
            throw new IllegalArgumentException("У публикации нет пути к изображению для пересканирования");
        }
        try {
            var fileName = artwork.getImagePath().substring(artwork.getImagePath().lastIndexOf('/') + 1);
            var multipartFile = fileUploadUtil.getAsMultipartFile(artwork.getImagePath(), fileName);
            applyImageFeatures(artwork, multipartFile);
            artworkRepository.save(artwork);
            recommendationService.clearModelCache();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка пересканирования изображения: " + e.getMessage(), e);
        }
    }

    @Override
    public void rescanAllArtworkFeatures() {
        List<Artwork> allArtworks = artworkRepository.findAll();
        for (Artwork artwork : allArtworks) {
            if (artwork.getImagePath() == null || artwork.getImagePath().isBlank()) {
                continue;
            }
            try {
                var fileName = artwork.getImagePath().substring(artwork.getImagePath().lastIndexOf('/') + 1);
                var multipartFile = fileUploadUtil.getAsMultipartFile(artwork.getImagePath(), fileName);
                applyImageFeatures(artwork, multipartFile);
                artworkRepository.save(artwork);
            } catch (Exception e) {
                System.err.println("Ошибка при пересканировании публикации " + artwork.getId() + ": " + e.getMessage());
            }
        }
        recommendationService.clearModelCache();
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
        try {
            imageHashService.deleteByArtworkId(artwork.getId());
        } catch (Exception e) {
            System.err.println("Ошибка при удалении хеша: " + e.getMessage());
        }

        Artwork artworkWithExhibitions = artworkRepository.findById(artwork.getId())
                .orElseThrow(() -> new IllegalArgumentException("Произведение искусства не найдено"));

        Set<Exhibition> exhibitions = new HashSet<>(artworkWithExhibitions.getExhibitions());
        for (Exhibition exhibition : exhibitions) {
            exhibitionService.removeArtworkFromExhibition(exhibition.getId(), artwork.getId());
        }

        String imagePath = artworkWithExhibitions.getImagePath();
        if (imagePath != null && !imagePath.isBlank()) {
            long referenceCount = artworkRepository.countByImagePath(imagePath);
            if (referenceCount <= 1) {
                try {
                    fileUploadUtil.deleteFile(imagePath);
                    System.out.println("Файл удалён из MinIO: " + imagePath);
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка удаления файла из MinIO: " + imagePath, e);
                }
            } else {
                System.out.println(" Файл не удалён, на него ссылаются " + referenceCount + " публикаций: " + imagePath);
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
        artwork.setRejectionReason(null);
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
    public void reportArtwork(Long artworkId, User reporter, String reason) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new EntityNotFoundException("Artwork not found"));

        artwork.setComplaintCount(artwork.getComplaintCount() + 1);

        if (reason != null && !reason.trim().isEmpty()) {
            artwork.setLastComplaintReason(reason.trim());
        }

        artworkRepository.save(artwork);
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

    @Override
    public Page<Artwork> searchArtworksByTitle(String title, Pageable pageable) {
        return artworkRepository.findByTitleContainingIgnoreCaseAndStatus(title, "APPROVED", pageable);
    }
}