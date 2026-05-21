package com.example.vag.repository;

import com.example.vag.model.Artwork;
import com.example.vag.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("SELECT c " +
        "FROM Comment c " +
        "JOIN FETCH c.user " +
        "WHERE c.artwork = :artwork " +
        "ORDER BY c.dateCreated DESC")
    List<Comment> findByArtworkOrderByDateCreatedDesc(
            @Param("artwork") Artwork artwork
    );
}