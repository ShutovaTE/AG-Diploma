package com.example.vag.recommendation.controller;

import com.example.vag.model.User;
import com.example.vag.recommendation.dto.RecommendationDTO;
import com.example.vag.recommendation.dto.TrainingStatusDTO;
import com.example.vag.recommendation.service.RecommendationService;
import com.example.vag.recommendation.service.ModelManagementService;
import com.example.vag.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST-контроллер для получения рекомендаций и управления моделью.
 * 
 * Endpoints:
 * - GET  /api/recommendations - получить рекомендации для текущего пользователя
 * - GET  /api/recommendations/{userId} - получить рекомендации для конкретного пользователя (ADMIN)
 * - GET  /api/recommendations/status - проверить статус системы
 * - POST /api/recommendations/retrain - запустить переобучение модели (ADMIN)
 * - GET  /api/recommendations/training-status - получить статус обучения (ADMIN)
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);

    private final RecommendationService recommendationService;
    private final UserService userService;
    private final ModelManagementService modelManagementService;

    /**
     * Конструктор с внедрением зависимостей.
     */
    public RecommendationController(RecommendationService recommendationService,
                                    UserService userService,
                                    ModelManagementService modelManagementService) {
        this.recommendationService = recommendationService;
        this.userService = userService;
        this.modelManagementService = modelManagementService;
    }

    // ========================================================================
    // ENDPOINTS ДЛЯ ПОЛУЧЕНИЯ РЕКОМЕНДАЦИЙ
    // ========================================================================

    /**
     * Получить рекомендации для текущего авторизованного пользователя.
     * 
     * Query параметры:
     *   topN (optional): количество рекомендаций (по умолчанию 12)
     * 
     * Возвращает:
     *   12 случайно выбранных и отсортированных по релевантности рекомендаций.
     *   При каждом обновлении страницы набор рекомендаций меняется!
     * 
     * @return JSON с рекомендациями
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getRecommendationsForCurrentUser(
            @RequestParam(defaultValue = "12") int topN,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        if (authentication == null) {
            response.put("success", false);
            response.put("message", "Пользователь не авторизован");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String username = authentication.getName();
        Long userId = getUserIdByUsername(username);

        if (userId == null) {
            response.put("success", false);
            response.put("message", "Пользователь не найден");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        logger.info("Получение " + topN + " рекомендаций для пользователя: " + userId);

        List<RecommendationDTO> recommendations =
                recommendationService.getRecommendationsForUser(userId, topN);

        response.put("success", true);
        response.put("userId", userId);
        response.put("recommendations", recommendations);
        response.put("count", recommendations.size());
        response.put("message", "Рекомендации получены успешно");

        return ResponseEntity.ok(response);
    }

    /**
     * Получить рекомендации для указанного пользователя (только для администраторов).
     * 
     * @param userId ID пользователя
     * @param topN количество рекомендаций
     * @return JSON с рекомендациями
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getRecommendationsForUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "12") int topN) {

        Map<String, Object> response = new HashMap<>();

        logger.info("ADMIN запросил рекомендации для пользователя: " + userId);

        List<RecommendationDTO> recommendations =
                recommendationService.getRecommendationsForUser(userId, topN);

        response.put("success", true);
        response.put("userId", userId);
        response.put("recommendations", recommendations);
        response.put("count", recommendations.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Проверка статуса системы рекомендаций.
     * 
     * @return JSON со статусом системы
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();

        boolean available = recommendationService.isRecommendationSystemAvailable();
        boolean modelReady = modelManagementService.isModelReady();

        response.put("success", true);
        response.put("system_available", available);
        response.put("model_ready", modelReady);

        if (available && modelReady) {
            response.put("status", "READY");
            response.put("message", "Система рекомендаций готова к работе");
        } else {
            response.put("status", "NOT_READY");
            if (!available) {
                response.put("message", "Система рекомендаций недоступна. " +
                        "Проверьте наличие Python и файла recommendation_engine.py");
            } else if (!modelReady) {
                response.put("message", "Модель рекомендаций обучается. Пожалуйста, подождите.");
            }
        }

        return ResponseEntity.ok(response);
    }

    // ========================================================================
    // ENDPOINTS ДЛЯ УПРАВЛЕНИЯ МОДЕЛЬЮ (только для ADMIN)
    // ========================================================================

    /**
     * Запуск переобучения модели.
     * 
     * Операция выполняется асинхронно. Обучение происходит в фоне
     * и не блокирует основное приложение.
     * 
     * Используйте /api/recommendations/training-status для проверки
     * статуса обучения.
     * 
     * @return JSON с результатом запуска
     */
    @PostMapping("/retrain")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> retrainModel() {
        Map<String, Object> response = new HashMap<>();

        logger.info("ADMIN инициирован запрос на переобучение модели");

        try {
            boolean started = modelManagementService.retrainModel();

            if (started) {
                response.put("success", true);
                response.put("message", "Переобучение модели начато. " +
                        "Проверьте статус через /api/recommendations/training-status");
                response.put("task_status", "IN_PROGRESS");
                return ResponseEntity.accepted().body(response);
            } else {
                response.put("success", false);
                response.put("message", "Переобучение уже в процессе. Дождитесь завершения.");
                response.put("task_status", "ALREADY_RUNNING");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

        } catch (Exception e) {
            logger.error("Ошибка при запуске переобучения: " + e.getMessage());
            response.put("success", false);
            response.put("message", "Ошибка при запуске переобучения: " + e.getMessage());
            response.put("error_type", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Получить статус процесса обучения модели.
     * 
     * Возвращает:
     * - Текущий статус (IDLE, IN_PROGRESS, COMPLETED, ERROR)
     * - Время последнего обучения
     * - Размер файла модели
     * - Последние логи процесса обучения
     * 
     * @return JSON со статусом обучения
     */
    @GetMapping("/training-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getTrainingStatus() {
        logger.info("ADMIN запросил статус обучения модели");

        try {
            Map<String, Object> statusMap = modelManagementService.getTrainingStatus();
            TrainingStatusDTO statusDTO = new TrainingStatusDTO(statusMap);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("training_status", statusDTO);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Ошибка при получении статуса обучения: " + e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при получении статуса: " + e.getMessage());
            response.put("error_type", e.getClass().getSimpleName());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Получить полный лог обучения модели.
     * 
     * @return Текст лога обучения
     */
    @GetMapping("/training-log")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getTrainingLog() {
        logger.info("ADMIN запросил полный лог обучения");

        try {
            String log = modelManagementService.getFullTrainingLog();
            return ResponseEntity.ok()
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .body(log);
        } catch (Exception e) {
            logger.error("Ошибка при получении лога: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при получении лога: " + e.getMessage());
        }
    }

    // ========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ========================================================================

    /**
     * Получение ID пользователя по username.
     */
    private Long getUserIdByUsername(String username) {
        return userService.findByUsername(username)
                .map(User::getId)
                .orElse(null);
    }
}
