package com.example.vag.service;

import com.example.vag.model.Artwork;
import com.example.vag.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface ArtworkService {
    Artwork save(Artwork artwork);
    Artwork create(Artwork artwork, MultipartFile imageFile, User user) throws IOException;
    List<Artwork> findAll();
    List<Artwork> findByStatus(String status);
    List<Artwork> findByUserWithDetails(User user);
    Page<Artwork> findPaginatedApprovedArtworks(Pageable pageable);
    Page<Artwork> findByCategoryId(Long categoryId, Pageable pageable);
    List<Artwork> findByExhibitionId(Long exhibitionId);
    Optional<Artwork> findById(Long id);
    void delete(Artwork artwork);
    void approveArtwork(Long artworkId);
    void rejectArtwork(Long artworkId);
    void likeArtwork(Long artworkId, User user);
    void unlikeArtwork(Long artworkId, User user);
    boolean isLikedByUser(Artwork artwork, User user);
    void addComment(Long artworkId, User user, String content);
    public Artwork findByIdWithComments(Long id);
    long countApprovedArtworksByCategoryId(Long categoryId);
    Page<Artwork> getApprovedArtworks(Pageable pageable);
    Optional<Artwork> findByIdWithCategories(Long id);
    Page<Artwork> findAll(Pageable pageable);
    Page<Artwork> findByStatus(String status, Pageable pageable);
    Page<Artwork> findByUser(User user, Pageable pageable);
    Page<Artwork> findByUserAndStatus(User user, String status, Pageable pageable);

    List<Artwork> findLikedArtworks(User user);
    Page<Artwork> findLikedArtworks(User user, Pageable pageable);
    Page<Artwork> findByExhibitionId(Long exhibitionId, Pageable pageable);
    Page<Artwork> findAllPaginated(Pageable pageable);
}