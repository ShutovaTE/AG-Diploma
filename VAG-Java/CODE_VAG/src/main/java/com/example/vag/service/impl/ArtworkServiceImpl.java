package com.example.vag.service.impl;

import com.example.vag.model.*;
import com.example.vag.repository.*;
import com.example.vag.service.ArtworkService;
import com.example.vag.service.ExhibitionService;
import com.example.vag.util.FileUploadUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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

    public ArtworkServiceImpl(ArtworkRepository artworkRepository,
                              CategoryRepository categoryRepository,
                              CommentRepository commentRepository,
                              LikeRepository likeRepository,
                              FileUploadUtil fileUploadUtil,
                              ExhibitionService exhibitionService) {
        this.artworkRepository = artworkRepository;
        this.categoryRepository = categoryRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.fileUploadUtil = fileUploadUtil;
        this.exhibitionService = exhibitionService;
    }

    @Override
    public Artwork save(Artwork artwork) {
        return artworkRepository.save(artwork);
    }

    @Override
    public Artwork create(Artwork artwork, MultipartFile imageFile, User user) throws IOException {
        List<Category> categories = categoryRepository.findAllByIds(artwork.getCategoryIds());
        artwork.setCategories(new HashSet<>(categories));

        String originalFileName = StringUtils.cleanPath(imageFile.getOriginalFilename());
        String safeFileName = originalFileName
                .replace(" ", "_")          // Замена пробелов
                .replaceAll("[^a-zA-Z0-9._-]", "");

        String relativePath = "artwork-images/" + user.getId() + "/" + safeFileName;
        artwork.setImagePath(relativePath);

        fileUploadUtil.saveFile(user.getId(), safeFileName, imageFile);

        artwork.setDateCreation(LocalDate.now());
        artwork.setUser(user);
        artwork.setStatus(Artwork.ArtworkStatus.PENDING.name());
        artwork.setLikes(0);
        artwork.setViews(0);
        return artworkRepository.save(artwork);
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
        Artwork artworkWithExhibitions = artworkRepository.findById(artwork.getId())
                .orElseThrow(() -> new IllegalArgumentException("Произведение искусства не найдено"));
        
        Set<Exhibition> exhibitions = new HashSet<>(artworkWithExhibitions.getExhibitions());
        for (Exhibition exhibition : exhibitions) {
            exhibitionService.removeArtworkFromExhibition(exhibition.getId(), artwork.getId());
        }
        
        artworkRepository.delete(artworkWithExhibitions);
    }

    @Override
    public void approveArtwork(Long artworkId) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid artwork ID"));
        artwork.setStatus(Artwork.ArtworkStatus.APPROVED.name());
        artworkRepository.save(artwork);
    }

    @Override
    public void rejectArtwork(Long artworkId) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid artwork ID"));
        artwork.setStatus(Artwork.ArtworkStatus.REJECTED.name());
        artworkRepository.save(artwork);
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
        }
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

}