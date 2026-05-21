package com.example.vag.service;

import com.example.vag.model.Artwork;
import com.example.vag.repository.ArtworkRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageFeatureAsyncService {

    private final ImageFeatureService imageFeatureService;
    private final ArtworkRepository artworkRepository;

    public ImageFeatureAsyncService(
            ImageFeatureService imageFeatureService,
            ArtworkRepository artworkRepository
    ) {
        this.imageFeatureService = imageFeatureService;
        this.artworkRepository = artworkRepository;
    }

    @Async
    public void analyzeArtwork(
            Long artworkId,
            MultipartFile imageFile
    ) {

        try {

            Artwork artwork = artworkRepository
                    .findById(artworkId)
                    .orElse(null);

            if (artwork == null) {
                return;
            }

            var analysis =
                    imageFeatureService.analyze(imageFile);

            artwork.setAverageRed(
                    analysis.getAverageRed());

            artwork.setAverageGreen(
                    analysis.getAverageGreen());

            artwork.setAverageBlue(
                    analysis.getAverageBlue());

            artwork.setColorHistogram(
                    analysis.getColorHistogram());

            artwork.setDetectedObjects(
                    analysis.getDetectedObjects());

            artworkRepository.save(artwork);

        } catch (Exception e) {

            System.err.println(
                    "Ошибка анализа изображения: "
                            + e.getMessage()
            );
        }
    }
}