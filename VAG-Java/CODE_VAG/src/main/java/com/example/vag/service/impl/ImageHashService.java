package com.example.vag.service.impl;

import com.example.vag.model.Artwork;
import com.example.vag.model.ImageHash;
import com.example.vag.repository.ImageHashRepository;
import com.example.vag.util.HashUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ImageHashService {

    private final ImageHashRepository imageHashRepository;

    public ImageHashService(ImageHashRepository imageHashRepository) {
        this.imageHashRepository = imageHashRepository;
    }

    @Transactional
    public ImageHash saveHash(Artwork artwork, MultipartFile file) throws IOException {
        String pHash = HashUtils.computePHash(file);
        String md5 = HashUtils.computeMD5(file);
        ImageHash hash = new ImageHash(artwork, pHash, md5);
        return imageHashRepository.save(hash);
    }

    public boolean isExactDuplicate(MultipartFile file) throws IOException {
        String md5 = HashUtils.computeMD5(file);
        return imageHashRepository.findByMd5(md5).isPresent();
    }

    public boolean isSimilarToExisting(MultipartFile file, Long excludeArtworkId) throws IOException {
        String newHash = HashUtils.computePHash(file);
        if (newHash == null) return false;

        List<ImageHash> existingHashes = imageHashRepository.findApprovedHashesExcluding(excludeArtworkId);
        for (ImageHash existing : existingHashes) {
            if (existing.getPHash() != null && HashUtils.isSimilar(newHash, existing.getPHash(), 10)) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public void deleteByArtworkId(Long artworkId) {
        imageHashRepository.findByArtworkId(artworkId)
                .ifPresent(imageHashRepository::delete);
    }
}