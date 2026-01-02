package com.example.vag.service.impl;

import com.example.vag.model.*;
import com.example.vag.repository.ArtworkRepository;
import com.example.vag.repository.CategoryRepository;
import com.example.vag.repository.ExhibitionRepository;
import com.example.vag.repository.LikeRepository;
import com.example.vag.repository.CommentRepository; // ДОБАВЛЕНО
import com.example.vag.service.ArtworkService;
import com.example.vag.util.FileUploadUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ArtworkServiceImpl implements ArtworkService {

    private final ArtworkRepository artworkRepository;
    private final CategoryRepository categoryRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository; // ДОБАВЛЕНО
    private final FileUploadUtil fileUploadUtil;

    public ArtworkServiceImpl(ArtworkRepository artworkRepository,
                              CategoryRepository categoryRepository,
                              ExhibitionRepository exhibitionRepository,
                              LikeRepository likeRepository,
                              CommentRepository commentRepository, // ДОБАВЛЕНО
                              FileUploadUtil fileUploadUtil) {
        this.artworkRepository = artworkRepository;
        this.categoryRepository = categoryRepository;
        this.exhibitionRepository = exhibitionRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository; // ДОБАВЛЕНО
        this.fileUploadUtil = fileUploadUtil;
    }

    // ОБНОВЛЕНО: Методы для работы с пользователем

    @Transactional(readOnly = true)
    @Override
    public Page<Artwork> findPaginatedApprovedArtworks(Pageable pageable) {
        return artworkRepository.findByStatusWithUser("APPROVED", pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Artwork findByIdWithComments(Long id) {
        // ИСПРАВЛЕНО: Используем метод с полной загрузкой всех данных
        Artwork artwork = artworkRepository.findByIdWithUserAndCommentsAndCategories(id)
                .orElseThrow(() -> new RuntimeException("Artwork not found"));

        System.out.println("ArtworkServiceImpl: findByIdWithComments - публикация ID: " + id);
        System.out.println("ArtworkServiceImpl: комментарии до сортировки: " + (artwork.getComments() != null ? artwork.getComments().size() : 0));

        // Сортируем комментарии по дате создания
        if (artwork.getComments() != null) {
            artwork.getComments().sort((c1, c2) -> c1.getDateCreated().compareTo(c2.getDateCreated()));
            System.out.println("ArtworkServiceImpl: комментарии после сортировки: " + artwork.getComments().size());
        }

        return artwork;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Artwork> findAllPaginated(Pageable pageable) {
        return artworkRepository.findAllWithUser(pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Artwork> searchApprovedArtworks(String query, Pageable pageable) {
        return artworkRepository.searchApprovedWithUser(query, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Artwork> findByCategoryId(Long categoryId, Pageable pageable) {
        return artworkRepository.findByCategoryIdWithUser(categoryId, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Artwork> findLikedArtworks(User user, Pageable pageable) {
        return artworkRepository.findLikedArtworksWithUser(user.getId(), pageable);
    }

    // ИСПРАВЛЕНО: Методы с правильными типами возврата
    @Override
    public long countApprovedArtworksByCategoryId(Long categoryId) {
        Long count = artworkRepository.countApprovedArtworksByCategoryId(categoryId);
        return count != null ? count : 0L;
    }

    @Override
    public long countArtworksByCategoryId(Long categoryId) {
        Long count = artworkRepository.countByCategoriesId(categoryId);
        return count != null ? count : 0L;
    }

    // ДОБАВЛЕНО: Отсутствующий метод create
    @Override
    @Transactional
    public Artwork create(Artwork artwork, MultipartFile imageFile, User user) throws IOException {
        artwork.setUser(user);
        artwork.setStatus("PENDING");
        artwork.setLikes(0);
        artwork.setViews(0);
        artwork.setDateCreation(LocalDate.now());

        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = StringUtils.cleanPath(imageFile.getOriginalFilename());
            String safeFileName = fileName.replace(" ", "_").replaceAll("[^a-zA-Z0-9._-]", "");
            String relativePath = "artwork-images/" + user.getId() + "/" + safeFileName;
            artwork.setImagePath(relativePath);

            fileUploadUtil.saveFile(user.getId(), safeFileName, imageFile);
        }

        return artworkRepository.save(artwork);
    }

    // Существующие методы остаются без изменений
    @Override
    public List<Artwork> findAll() {
        return artworkRepository.findAll();
    }

    @Override
    public Optional<Artwork> findById(Long id) {
        return artworkRepository.findById(id);
    }

    @Override
    public Artwork save(Artwork artwork) {
        return artworkRepository.save(artwork);
    }

    @Override
    public void delete(Artwork artwork) {
        artworkRepository.delete(artwork);
    }

    @Override
    public List<Artwork> findByStatus(String status) {
        return artworkRepository.findByStatus(status);
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
    public List<Artwork> findByUserWithDetails(User user) {
        return artworkRepository.findByUserWithDetails(user);
    }

    @Override
    @Transactional
    public void likeArtwork(Long artworkId, User user) {
        Optional<Artwork> artworkOpt = artworkRepository.findById(artworkId);
        if (artworkOpt.isPresent()) {
            Artwork artwork = artworkOpt.get();

            // ИСПРАВЛЕНО: Проверяем, не лайкнул ли уже пользователь
            boolean alreadyLiked = likeRepository.existsByArtworkAndUser(artwork, user);
            if (!alreadyLiked) {
                artwork.setLikes(artwork.getLikes() + 1);
                artworkRepository.save(artwork);

                // Сохраняем информацию о лайке
                Like like = new Like();
                like.setArtwork(artwork);
                like.setUser(user);
                likeRepository.save(like);

                System.out.println("User " + user.getUsername() + " liked artwork " + artworkId);
            } else {
                System.out.println("User " + user.getUsername() + " already liked artwork " + artworkId);
            }
        }
    }

    @Override
    @Transactional
    public void unlikeArtwork(Long artworkId, User user) {
        Optional<Artwork> artworkOpt = artworkRepository.findById(artworkId);
        if (artworkOpt.isPresent()) {
            Artwork artwork = artworkOpt.get();

            // ИСПРАВЛЕНО: Находим и удаляем лайк
            Optional<Like> likeOpt = likeRepository.findByArtworkAndUser(artwork, user);
            if (likeOpt.isPresent()) {
                Like like = likeOpt.get();
                likeRepository.delete(like);

                if (artwork.getLikes() > 0) {
                    artwork.setLikes(artwork.getLikes() - 1);
                    artworkRepository.save(artwork);
                }

                System.out.println("User " + user.getUsername() + " unliked artwork " + artworkId);
            } else {
                System.out.println("User " + user.getUsername() + " didn't like artwork " + artworkId);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLikedByUser(Artwork artwork, User user) {
        return likeRepository.existsByArtworkAndUser(artwork, user);
    }

    @Override
    @Transactional
    public void addComment(Long artworkId, User user, String content) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new RuntimeException("Artwork not found"));

        System.out.println("ArtworkServiceImpl: Добавляем комментарий к публикации ID: " + artworkId);
        System.out.println("ArtworkServiceImpl: Текущие комментарии: " + (artwork.getComments() != null ? artwork.getComments().size() : 0));

        // ИСПРАВЛЕНО: Создаем и сохраняем комментарий
        Comment comment = new Comment();
        comment.setContent(content);
        comment.setUser(user);
        comment.setArtwork(artwork);
        comment.setDateCreated(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);
        System.out.println("ArtworkServiceImpl: Комментарий сохранен с ID: " + savedComment.getId());

        // Добавляем комментарий к публикации
        if (artwork.getComments() == null) {
            artwork.setComments(new ArrayList<>());
        }
        artwork.getComments().add(comment);

        artworkRepository.save(artwork);
        System.out.println("ArtworkServiceImpl: После добавления комментариев: " + artwork.getComments().size());

        System.out.println("Comment added by " + user.getUsername() + " to artwork " + artworkId);
    }

    @Override
    public void approveArtwork(Long id) {
        Optional<Artwork> artworkOpt = artworkRepository.findById(id);
        if (artworkOpt.isPresent()) {
            Artwork artwork = artworkOpt.get();
            artwork.setStatus("APPROVED");
            artworkRepository.save(artwork);
        }
    }

    @Override
    public void rejectArtwork(Long id) {
        Optional<Artwork> artworkOpt = artworkRepository.findById(id);
        if (artworkOpt.isPresent()) {
            Artwork artwork = artworkOpt.get();
            artwork.setStatus("REJECTED");
            artworkRepository.save(artwork);
        }
    }

    @Override
    public Optional<Artwork> findByIdWithCategories(Long id) {
        return artworkRepository.findByIdWithCategories(id);
    }

    @Override
    public Page<Artwork> getApprovedArtworks(Pageable pageable) {
        return artworkRepository.findByStatus("APPROVED", pageable);
    }

    @Override
    public Page<Artwork> findAll(Pageable pageable) {
        return artworkRepository.findAll(pageable);
    }

    @Override
    public Page<Artwork> findByUserAndStatus(User user, String status, Pageable pageable) {
        return artworkRepository.findByUserAndStatus(user, status, pageable);
    }

    @Override
    public List<Artwork> findLikedArtworks(User user) {
        return artworkRepository.findLikedArtworksByUser(user);
    }

    @Override
    public Page<Artwork> findByExhibitionId(Long exhibitionId, Pageable pageable) {
        return artworkRepository.findByExhibitionsId(exhibitionId, pageable);
    }

    @Override
    public Page<Artwork> findByExhibitionIdFiltered(Long exhibitionId, Long userId, boolean isAdmin, Pageable pageable) {
        System.out.println("=== ARTWORK SERVICE FILTERING ===");
        System.out.println("Exhibition ID: " + exhibitionId);
        System.out.println("User ID: " + userId);
        System.out.println("Is Admin: " + isAdmin);
        System.out.println("=================================");

        Page<Artwork> result = artworkRepository.findByExhibitionIdFiltered(exhibitionId, userId, isAdmin, pageable);

        System.out.println("Found artworks count: " + result.getContent().size());
        result.getContent().forEach(artwork ->
            System.out.println("Artwork: " + artwork.getTitle() + " (status: " + artwork.getStatus() + ", user: " + artwork.getUser().getId() + ")")
        );

        return result;
    }

    @Override
    public List<Artwork> findByExhibitionId(Long exhibitionId) {
        return artworkRepository.findByExhibitionsId(exhibitionId);
    }

    @Override
    @Transactional
    public Artwork createWithCategories(Artwork artwork, MultipartFile imageFile, User user, List<Long> categoryIds) throws IOException {
        artwork.setUser(user);
        artwork.setStatus("PENDING");
        artwork.setLikes(0);
        artwork.setViews(0);
        artwork.setDateCreation(LocalDate.now());

        // Устанавливаем категории
        if (categoryIds != null && !categoryIds.isEmpty()) {
            List<Category> categories = categoryRepository.findAllById(categoryIds);
            artwork.setCategories(new HashSet<>(categories));
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            // ИСПРАВЛЕНО: Используем метод с сохранением оригинального имени
            String relativePath = fileUploadUtil.saveFileWithOriginalName(user.getId(), imageFile);
            artwork.setImagePath(relativePath);

            System.out.println("=== ARTWORK CREATION DEBUG ===");
            System.out.println("Original filename: " + imageFile.getOriginalFilename());
            System.out.println("Saved image path: " + relativePath);
            System.out.println("Final filename in DB: " + FileUploadUtil.getFileNameFromPath(relativePath));
        }

        return artworkRepository.save(artwork);
    }

    public void incrementViews(Long artworkId) {
        try {
            Artwork artwork = artworkRepository.findById(artworkId)
                    .orElseThrow(() -> new RuntimeException("Artwork not found"));

            // Увеличиваем счетчик просмотров
            artwork.setViews(artwork.getViews() + 1);
            artworkRepository.save(artwork);

            System.out.println("Incremented views for artwork " + artworkId + ". Current views: " + artwork.getViews());
        } catch (Exception e) {
            System.out.println("Error incrementing views for artwork " + artworkId + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public List<Artwork> findRandomArtworks(int count) {
        List<Artwork> artworks = artworkRepository.findRandomApprovedArtworks(count);
        return artworks.size() > count ? artworks.subList(0, count) : artworks;
    }

    @Override
    public Page<Artwork> findAllByUserId(Long userId, Pageable pageable) {
        return artworkRepository.findByUserIdOrderByDateCreationDesc(userId, pageable);
    }

    // ArtworkServiceImpl.java
    @Transactional
    public void deleteArtworkCompletely(Long artworkId, User currentUser) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new RuntimeException("Работа не найдена"));

        // Проверка прав: только автор или админ
        if (!artwork.getUser().getId().equals(currentUser.getId()) &&
                !currentUser.hasRole("ADMIN")) {
            throw new RuntimeException("Нет прав на удаление");
        }

        // 1. Удаляем файл с диска
        if (artwork.getImagePath() != null) {
            fileUploadUtil.deleteFile(artwork.getImagePath());
        }

        // 2. Удаляем ВСЕ лайки (через отдельный репозиторий или SQL)
        likeRepository.deleteAllByArtworkId(artworkId);

        // 3. Удаляем ВСЕ комментарии
        commentRepository.deleteAllByArtworkId(artworkId);

        // 4. Удаляем связи с выставками
        // Находим все выставки, которые содержат эту работу
        List<Exhibition> exhibitionsWithArtwork = exhibitionRepository.findAll()
                .stream()
                .filter(exhibition -> exhibition.getArtworks()
                        .stream()
                        .anyMatch(exhibitionArtwork -> exhibitionArtwork.getId().equals(artworkId)))
                .collect(Collectors.toList());

        // Удаляем работу из каждой выставки
        for (Exhibition exhibition : exhibitionsWithArtwork) {
            exhibition.getArtworks().removeIf(exhibitionArtwork -> exhibitionArtwork.getId().equals(artworkId));
            exhibitionRepository.save(exhibition);
        }

        // 5. Удаляем сам Artwork — каскадно удалит связи с категориями
        artworkRepository.delete(artwork);
    }

    @Override
    public Artwork updateWithCategories(Long artworkId, String title, String description, MultipartFile imageFile, User user, List<Long> categoryIds) throws IOException {
        Artwork existingArtwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new RuntimeException("Artwork not found"));

        // Проверяем права доступа - только автор или админ могут редактировать
        if (!existingArtwork.getUser().getId().equals(user.getId()) && !user.hasRole("ADMIN")) {
            throw new RuntimeException("Access denied");
        }

        // Обновляем поля
        existingArtwork.setTitle(title);
        existingArtwork.setDescription(description);

        // Устанавливаем статус PENDING после редактирования
        existingArtwork.setStatus("PENDING");

        // Обновляем категории
        if (categoryIds != null && !categoryIds.isEmpty()) {
            List<Category> categories = categoryRepository.findAllById(categoryIds);
            existingArtwork.setCategories(new HashSet<>(categories));
        } else {
            existingArtwork.getCategories().clear();
        }

        // Обновляем изображение, если оно предоставлено
        if (imageFile != null && !imageFile.isEmpty()) {
            String relativePath = fileUploadUtil.saveFileWithOriginalName(user.getId(), imageFile);
            existingArtwork.setImagePath(relativePath);

            System.out.println("=== ARTWORK UPDATE DEBUG ===");
            System.out.println("Original filename: " + imageFile.getOriginalFilename());
            System.out.println("Saved image path: " + relativePath);
            System.out.println("Final filename in DB: " + FileUploadUtil.getFileNameFromPath(relativePath));
        }

        return artworkRepository.save(existingArtwork);
    }
}