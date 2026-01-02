package com.example.vag.repository;

import com.example.vag.model.Artwork;
import com.example.vag.model.Like;
import com.example.vag.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    // Метод для пагинации с пользователем
    Page<Like> findByUser(User user, Pageable pageable);

    // Метод без пагинации (для списка)
    List<Like> findByUser(User user);

    // Альтернативный метод с JOIN (если нужны связанные данные)
    @Query("SELECT l FROM Like l JOIN l.artwork a WHERE l.user = :user")
    Page<Like> findLikesByUserWithArtwork(@Param("user") User user, Pageable pageable);

    @Query("SELECT l FROM Like l JOIN l.artwork a WHERE l.user = :user")
    List<Like> findLikesByUserWithArtwork(@Param("user") User user);

    Optional<Like> findByArtworkAndUser(Artwork artwork, User user);

    boolean existsByArtworkAndUser(Artwork artwork, User user);

    // Метод для удаления лайка
    @Modifying
    @Query("DELETE FROM Like l WHERE l.artwork = :artwork AND l.user = :user")
    void deleteByArtworkAndUser(@Param("artwork") Artwork artwork, @Param("user") User user);

    // Метод для подсчета лайков публикации
    long countByArtwork(Artwork artwork);

    // Дополнительный метод для проверки существования лайка
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Like l WHERE l.artwork = :artwork AND l.user = :user")
    boolean existsByArtworkIdAndUserId(@Param("artwork") Artwork artwork, @Param("user") User user);
    @Modifying
    @Query("DELETE FROM Like l WHERE l.artwork.id = :artworkId")
    void deleteAllByArtworkId(@Param("artworkId") Long artworkId);
}