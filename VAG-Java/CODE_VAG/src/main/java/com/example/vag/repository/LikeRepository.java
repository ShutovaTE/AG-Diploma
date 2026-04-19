package com.example.vag.repository;

import com.example.vag.model.Artwork;
import com.example.vag.model.Like;
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
public interface LikeRepository extends JpaRepository<Like, Long> {
    @Query(value = "SELECT l FROM Like l JOIN FETCH l.artwork a JOIN FETCH a.user WHERE l.user = :user",
            countQuery = "SELECT COUNT(l) FROM Like l WHERE l.user = :user")
    Page<Like> findByUserWithArtworkDetails(@Param("user") User user, Pageable pageable);

    Optional<Like> findByArtworkAndUser(@Param("artwork") Artwork artwork, @Param("user") User user);
    boolean existsByArtworkAndUser(@Param("artwork") Artwork artwork, @Param("user") User user);
    List<Like> findByUser(User user);
}


