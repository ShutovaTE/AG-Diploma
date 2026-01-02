package com.example.vag.service.impl;

import com.example.vag.model.Exhibition;
import com.example.vag.model.User;
import com.example.vag.model.Artwork;
import com.example.vag.repository.ExhibitionRepository;
import com.example.vag.service.ExhibitionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ExhibitionServiceImpl implements ExhibitionService {

    private final ExhibitionRepository exhibitionRepository;

    public ExhibitionServiceImpl(ExhibitionRepository exhibitionRepository) {
        this.exhibitionRepository = exhibitionRepository;
    }

    @Override
    public Exhibition save(Exhibition exhibition) {
        return exhibitionRepository.save(exhibition);
    }

    @Override
    public Exhibition create(Exhibition exhibition, User user) {
        exhibition.setUser(user);
        return exhibitionRepository.save(exhibition);
    }

    @Override
    public List<Exhibition> findAll() {
        return exhibitionRepository.findAll();
    }

    @Override
    public List<Exhibition> findByUser(User user) {
        return exhibitionRepository.findByUserId(user.getId());
    }

    @Override
    public List<Exhibition> findPublicExhibitions() {
        return exhibitionRepository.findByAuthorOnlyFalse();
    }

    @Override
    public Optional<Exhibition> findById(Long id) {
        return exhibitionRepository.findById(id);
    }

    @Override
    public void delete(Exhibition exhibition) {
        exhibitionRepository.delete(exhibition);
    }


    @Override
    public Exhibition update(Exhibition updatedExhibition) {
        Exhibition exhibition = exhibitionRepository.findById(updatedExhibition.getId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid exhibition ID"));

        exhibition.setTitle(updatedExhibition.getTitle());
        exhibition.setDescription(updatedExhibition.getDescription());
        exhibition.setAuthorOnly(updatedExhibition.isAuthorOnly());

        return exhibitionRepository.save(exhibition);
    }

    @Override
    public Page<Exhibition> findPaginatedExhibitions(int page, int size) {
        return exhibitionRepository.findAllExhibitions(PageRequest.of(page, size));
    }

    @Override
    public long countApprovedArtworksInExhibition(Long exhibitionId) {
        return exhibitionRepository.countApprovedArtworksInExhibition(exhibitionId);
    }

    @Override
    public Artwork getFirstApprovedArtworkInExhibition(Long exhibitionId) {
        List<Artwork> artworks = exhibitionRepository.findFirstApprovedArtworkInExhibition(exhibitionId);
        return artworks.isEmpty() ? null : artworks.get(0);
    }

    @Override
    public Exhibition removeArtworkFromExhibition(Long exhibitionId, Long artworkId) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new IllegalArgumentException("Выставка не найдена"));
        
        exhibition.getArtworks().removeIf(artwork -> artwork.getId().equals(artworkId));
        return exhibitionRepository.save(exhibition);
    }

    @Override
    public Page<Exhibition> getPublicExhibitions(Pageable pageable) {
        return exhibitionRepository.findPublicExhibitions(pageable);
    }

    @Override
    public Page<Exhibition> findByUser(User user, Pageable pageable) {
        return exhibitionRepository.findByUser(user, pageable);
    }
}