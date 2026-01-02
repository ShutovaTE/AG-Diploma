package com.example.vag.repository;

import com.example.vag.model.Artwork;
import com.example.vag.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByArtworkOrderByDateCreatedDesc(Artwork artwork);
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.artwork.id = :artworkId")
    void deleteAllByArtworkId(@Param("artworkId") Long artworkId);
}