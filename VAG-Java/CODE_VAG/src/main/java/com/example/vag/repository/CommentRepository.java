package com.example.vag.repository;

import com.example.vag.model.Artwork;
import com.example.vag.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByArtworkOrderByDateCreatedDesc(Artwork artwork);
}