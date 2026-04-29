package com.example.vag.service.impl;

import com.example.vag.dto.ModerationResult;
import com.example.vag.dto.SimilarArtworkInfo;
import com.example.vag.service.ModerationService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

        StringBuilder report = new StringBuilder();
        report.append("ОТЧЁТ AI-МОДЕРАЦИИ\n");
        report.append("Файл: ").append(file.getOriginalFilename()).append("\n");
        report.append("Размер: ").append(file.getSize()).append(" байт\n\n");

        try {
            // 1. Yandex Vision
            report.append("1. Yandex Vision (облачный API):\n");
            try {
                boolean hasExplicit = visionService.hasExplicitContent(file);
                report.append("   Результат: ").append(hasExplicit ? "ОБНАРУЖЕН 18+" : "OK").append("\n");
                if (hasExplicit) {
                    result.setApproved(false);
                    result.setRejectionReason("Изображение содержит контент 18+");
                    result.setAiReport(report.toString());
                    return result;
                }
            } catch (Exception e) {
                report.append("   Ошибка: ").append(e.getMessage()).append("\n");
            }

            // 2. NSFW
            report.append("\n2. NSFW-модель ONNX (локальная):\n");
            try {
                if (nsfwService.isAvailable()) {
                    boolean isNsfw = nsfwService.isNSFW(file);
                    report.append("   Результат: ").append(isNsfw ? " ОБНАРУЖЕН 18+" : "OK").append("\n");
                    if (isNsfw) {
                        String reason = nsfwService.getRejectionReason(file);
                        result.setApproved(false);
                        result.setRejectionReason(reason);
                        result.setAiReport(report.toString());
                        return result;
                    }
                } else {
                    report.append("   Модель недоступна, пропущено\n");
                }
            } catch (Exception e) {
                report.append("   Ошибка: ").append(e.getMessage()).append("\n");
            }

            // 3. MD5 (точное совпадение)
            report.append("\n3. MD5 (точное совпадение):\n");
            SimilarArtworkInfo duplicate = imageHashService.findDuplicateByMd5(file);
            if (duplicate != null) {
                report.append("   Результат: НАЙДЕН ДУБЛИКАТ\n");
                report.append("   Дубликат работы: \"").append(duplicate.getTitle()).append("\"\n");
                report.append("   Ссылка на дубликат работы: /vag/artwork/details/")
                        .append(duplicate.getArtworkId()).append("\n");

                result.setApproved(false);

                // Для уведомления пользователю — чистый текст
                result.setRejectionReason("Это изображение уже опубликовано (Название опубликованной работы «" + duplicate.getTitle() + "»).");

                // Для админ-панели — HTML с ссылкой (сохраняем в aiReport или отдельное поле)
                result.setRejectionReasonHtml(
                        "Это изображение уже опубликовано. " +
                                "<a href='/vag/artwork/details/" + duplicate.getArtworkId() + "' " +
                                "target='_blank' style='color: #007bff; font-weight: bold; text-decoration: underline;'>" +
                                "Посмотреть работу «" + duplicate.getTitle() + "»" +
                                "</a>"
                );

                result.setSimilarArtworkId(duplicate.getArtworkId());
                result.setSimilarArtworkTitle(duplicate.getTitle());
                result.setAiReport(report.toString());
                return result;
            }
            report.append("   Результат: OK\n");

            // 4. pHash (визуальное сходство)
            report.append("\n4. pHash (визуальное сходство):\n");
            SimilarArtworkInfo similar = imageHashService.findSimilarArtwork(file, excludeArtworkId);
            if (similar != null) {
                int similarityPercent = similar.getSimilarityPercent();
                report.append("   Результат: НАЙДЕНО ПОХОЖЕЕ ИЗОБРАЖЕНИЕ\n");

                result.setNeedsManualReview(true);

                // Для уведомления — чистый текст
                result.setManualReviewReason("Изображение похоже на работу «" + similar.getTitle() + "» (сходство: " + similarityPercent + "%).");

                // Для админ-панели — HTML с ссылкой
                result.setManualReviewReasonHtml(
                        "Изображение очень похоже на существующую работу. " +
                                "<a href='/vag/artwork/details/" + similar.getArtworkId() + "' " +
                                "target='_blank' style='color: #007bff; font-weight: bold; text-decoration: underline;'>" +
                                "Посмотреть работу «" + similar.getTitle() + "»" +
                                "</a> (сходство: " + similarityPercent + "%)"
                );

                result.setSimilarArtworkId(similar.getArtworkId());
                result.setSimilarArtworkTitle(similar.getTitle());
                result.setApproved(false);
                result.setAiReport(report.toString());
                return result;
            }
            report.append("   Результат: OK\n");

            report.append("\nИТОГ: ПРОВЕРКА ПРОЙДЕНА");
            result.setAiReport(report.toString());

        } catch (Exception e) {
            report.append("\nОШИБКА: ").append(e.getMessage());
            result.setApproved(false);
            result.setRejectionReason("Ошибка при анализе: " + e.getMessage());
            result.setAiReport(report.toString());
        }

        return result;
    }
}