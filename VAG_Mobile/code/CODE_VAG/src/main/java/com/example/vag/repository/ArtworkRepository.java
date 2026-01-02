package com.example.vag.repository;

import com.example.vag.model.Artwork;
import com.example.vag.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtworkRepository extends JpaRepository<Artwork, Long> {

    // Существующие методы
    List<Artwork> findAll();
    Optional<Artwork> findById(Long id);

    // Методы для поиска с категориями и комментариями
    @Query("SELECT a FROM Artwork a LEFT JOIN FETCH a.categories WHERE a.id = :id")
    Optional<Artwork> findByIdWithCategories(@Param("id") Long id);

    @Query("SELECT a FROM Artwork a LEFT JOIN FETCH a.comments WHERE a.id = :id")
    Artwork findByIdWithComments(@Param("id") Long id);

    // Методы для пагинации
    Page<Artwork> findAll(Pageable pageable);
    Page<Artwork> findByStatus(String status, Pageable pageable);
    Page<Artwork> findByUser(User user, Pageable pageable);
    Page<Artwork> findByUserAndStatus(User user, String status, Pageable pageable);

    // Методы для поиска по статусу
    List<Artwork> findByStatus(String status);

    // ИСПРАВЛЕНО: убрали JOIN FETCH из count запроса
    Page<Artwork> findByStatusOrderByDateCreationDesc(String status, Pageable pageable);

    // Методы для поиска по категории
    @Query("SELECT a FROM Artwork a JOIN a.categories c WHERE c.id = :categoryId AND a.status = :status")
    Page<Artwork> findByCategoryIdAndStatus(@Param("categoryId") Long categoryId, @Param("status") String status, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Artwork a JOIN a.categories c WHERE c.id = :categoryId AND a.status = 'APPROVED'")
    Long countApprovedArtworksByCategoryId(@Param("categoryId") Long categoryId);

    Long countByCategoriesId(Long categoryId);

    // Методы для поиска по пользователю
    @Query("SELECT a FROM Artwork a LEFT JOIN FETCH a.categories WHERE a.user = :user")
    List<Artwork> findByUserWithDetails(@Param("user") User user);

    // Методы для поиска по лайкам
    @Query("SELECT a FROM Artwork a JOIN a.artworkLikes l WHERE l.user.id = :userId")
    Page<Artwork> findLikedArtworks(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT a FROM Artwork a JOIN a.artworkLikes l WHERE l.user = :user")
    List<Artwork> findLikedArtworksByUser(@Param("user") User user);

    // Методы для поиска по выставкам
    List<Artwork> findByExhibitionsId(Long exhibitionId);
    Page<Artwork> findByExhibitionsId(Long exhibitionId, Pageable pageable);

    // Метод для получения работ выставки с фильтрацией по видимости для пользователя
    @Query(value = "SELECT DISTINCT a FROM Artwork a " +
            "JOIN a.exhibitions e " +
            "WHERE e.id = :exhibitionId " +
            "AND (:isAdmin = true OR a.status = 'APPROVED' OR (:userId IS NOT NULL AND a.user.id = :userId)) " +
            "ORDER BY a.id DESC",
            countQuery = "SELECT COUNT(DISTINCT a) FROM Artwork a " +
            "JOIN a.exhibitions e " +
            "WHERE e.id = :exhibitionId " +
            "AND (:isAdmin = true OR a.status = 'APPROVED' OR (:userId IS NOT NULL AND a.user.id = :userId))")
    Page<Artwork> findByExhibitionIdFiltered(@Param("exhibitionId") Long exhibitionId,
                                            @Param("userId") Long userId,
                                            @Param("isAdmin") boolean isAdmin,
                                            Pageable pageable);

    // Методы для поиска
    @Query("SELECT a FROM Artwork a WHERE (LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :query, '%'))) AND a.status = 'APPROVED'")
    Page<Artwork> searchApproved(@Param("query") String query, Pageable pageable);

    // ИСПРАВЛЕНО: метод для загрузки с пользователем (только для списка, не для count)
    @Query("SELECT a FROM Artwork a JOIN FETCH a.user WHERE a.status = 'APPROVED' ORDER BY a.dateCreation DESC")
    List<Artwork> findApprovedArtworksWithUser();

    // ДОБАВЛЕНО: метод для пагинации с пользователем (без JOIN FETCH в count)
    @Query(value = "SELECT a FROM Artwork a WHERE a.status = 'APPROVED' ORDER BY a.dateCreation DESC",
            countQuery = "SELECT COUNT(a) FROM Artwork a WHERE a.status = 'APPROVED'")
    Page<Artwork> findApprovedArtworksWithUserPaged(Pageable pageable);

    // ДОБАВЛЕНО: Методы для загрузки artwork с пользователем

    // Для списка artwork с пользователем и категориями
    @Query("SELECT a FROM Artwork a " +
            "LEFT JOIN FETCH a.user " +
            "LEFT JOIN FETCH a.categories " +
            "WHERE a.status = :status")
    List<Artwork> findByStatusWithUserAndCategories(@Param("status") String status);

    // Для пагинации artwork с пользователем
    @Query(value = "SELECT a FROM Artwork a " +
            "LEFT JOIN FETCH a.user " +
            "WHERE a.status = :status",
            countQuery = "SELECT COUNT(a) FROM Artwork a WHERE a.status = :status")
    Page<Artwork> findByStatusWithUser(@Param("status") String status, Pageable pageable);

    // Для детального просмотра artwork с пользователем, комментариями и категориями
    @Query("SELECT a FROM Artwork a " +
            "LEFT JOIN FETCH a.user " +
            "LEFT JOIN FETCH a.categories " +
            "LEFT JOIN FETCH a.comments c " +
            "LEFT JOIN FETCH c.user " +
            "WHERE a.id = :id")
    Optional<Artwork> findByIdWithUserAndCommentsAndCategories(@Param("id") Long id);

    // Для администратора - все artwork с пользователем
    @Query(value = "SELECT a FROM Artwork a " +
            "LEFT JOIN FETCH a.user",
            countQuery = "SELECT COUNT(a) FROM Artwork a")
    Page<Artwork> findAllWithUser(Pageable pageable);

    // Для поиска с пользователем
    @Query(value = "SELECT a FROM Artwork a " +
            "LEFT JOIN FETCH a.user " +
            "WHERE (LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(a.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND a.status = 'APPROVED'",
            countQuery = "SELECT COUNT(a) FROM Artwork a " +
                    "WHERE (LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
                    "OR LOWER(a.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                    "AND a.status = 'APPROVED'")
    Page<Artwork> searchApprovedWithUser(@Param("query") String query, Pageable pageable);

    // Для категорий с пользователем
    @Query(value = "SELECT a FROM Artwork a " +
            "LEFT JOIN FETCH a.user " +
            "JOIN a.categories c " +
            "WHERE c.id = :categoryId AND a.status = 'APPROVED'",
            countQuery = "SELECT COUNT(a) FROM Artwork a " +
                    "JOIN a.categories c " +
                    "WHERE c.id = :categoryId AND a.status = 'APPROVED'")
    Page<Artwork> findByCategoryIdWithUser(@Param("categoryId") Long categoryId, Pageable pageable);

    // Для лайков с пользователем
    @Query(value = "SELECT a FROM Artwork a " +
            "LEFT JOIN FETCH a.user " +
            "JOIN a.artworkLikes l " +
            "WHERE l.user.id = :userId",
            countQuery = "SELECT COUNT(a) FROM Artwork a " +
                    "JOIN a.artworkLikes l " +
                    "WHERE l.user.id = :userId")
    Page<Artwork> findLikedArtworksWithUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT a FROM Artwork a WHERE a.status = 'APPROVED' ORDER BY FUNCTION('RAND')")
    List<Artwork> findRandomApprovedArtworks(@Param("count") int count);
    @Query("SELECT a FROM Artwork a WHERE a.user.id = :userId ORDER BY a.dateCreation DESC")
    Page<Artwork> findByUserIdOrderByDateCreationDesc(@Param("userId") Long userId, Pageable pageable);
}