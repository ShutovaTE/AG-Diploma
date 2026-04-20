package com.example.vag.service.impl;

import com.example.vag.dto.ModerationResult;
import com.example.vag.service.ModerationService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ModerationServiceImpl implements ModerationService {

    private final YandexVisionService visionService;
    private final ImageHashService imageHashService;
    private final NSFWDetectionService nsfwService;

    public ModerationServiceImpl(YandexVisionService visionService,
                                 ImageHashService imageHashService,
                                 NSFWDetectionService nsfwService) {
        this.visionService = visionService;
        this.imageHashService = imageHashService;
        this.nsfwService = nsfwService;
    }

    @Override
    public ModerationResult moderateImage(MultipartFile file, Long excludeArtworkId) {
        ModerationResult result = new ModerationResult();
        result.setApproved(true);

        try {
            System.out.println("Начинаем модерацию: " + file.getOriginalFilename());

            // 1. Yandex Vision
            System.out.println("Проверка Yandex Vision...");
            if (visionService.hasExplicitContent(file)) {
                result.setApproved(false);
                result.setRejectionReason("Изображение содержит неприемлемый контент");
                return result;
            }

            // 2. NSFW (с проверкой доступности)
            System.out.println("Проверка NSFW-модели...");
            if (nsfwService.isAvailable()) {
                try {
                    if (nsfwService.isNSFW(file)) {
                        String reason = nsfwService.getRejectionReason(file);
                        result.setApproved(false);
                        result.setRejectionReason(reason);
                        return result;
                    }
                } catch (Exception e) {
                    System.err.println(" Ошибка NSFW: " + e.getMessage());
                }
            } else {
                System.out.println(" NSFW модель недоступна, пропускаем");
            }

            // 3. MD5
            if (imageHashService.isExactDuplicate(file)) {
                result.setApproved(false);
                result.setRejectionReason("Это изображение уже опубликовано");
                return result;
            }

            // 4. pHash
            if (imageHashService.isSimilarToExisting(file, excludeArtworkId)) {
                result.setNeedsManualReview(true);
                result.setManualReviewReason("Изображение очень похоже на существующую работу");
                result.setApproved(false);
                return result;
            }

            System.out.println("Модерация пройдена!");

        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
            result.setApproved(false);
            result.setRejectionReason("Ошибка при анализе: " + e.getMessage());
        }

        return result;
    }
}