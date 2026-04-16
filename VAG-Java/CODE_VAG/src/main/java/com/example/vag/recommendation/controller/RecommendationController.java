package com.example.vag.recommendation.controller;

import com.example.vag.model.User;
import com.example.vag.recommendation.dto.RecommendationDTO;
import com.example.vag.recommendation.service.RecommendationService;
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
 * REST-контроллер для получения рекомендаций.
 * Возвращает JSON-ответы с персонализированными рекомендациями.
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);

    private final RecommendationService recommendationService;
    private final UserService userService;

    // Внедрение зависимостей через конструктор
    public RecommendationController(RecommendationService recommendationService,
                                    UserService userService) {
        this.recommendationService = recommendationService;
        this.userService = userService;
    }

    /**
     * Получить рекомендации для текущего авторизованного пользователя.
     * GET /api/recommendations?topN=5
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getRecommendationsForCurrentUser(
            @RequestParam(defaultValue = "10") int topN,
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

        List<RecommendationDTO> recommendations =
                recommendationService.getRecommendationsForUser(userId, topN);

        response.put("success", true);
        response.put("recommendations", recommendations);
        response.put("count", recommendations.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Получить рекомендации для указанного пользователя (только для ADMIN).
     * GET /api/recommendations/{userId}?topN=5
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getRecommendationsForUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int topN) {

        Map<String, Object> response = new HashMap<>();

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
     * GET /api/recommendations/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();

        boolean available = recommendationService.isRecommendationSystemAvailable();
        response.put("available", available);

        if (available) {
            response.put("message", "Система рекомендаций готова к работе");
        } else {
            response.put("message", "Система рекомендаций недоступна. " +
                    "Проверьте наличие Python и файла recommendation_engine.py");
        }

        return ResponseEntity.ok(response);
    }

    // Вспомогательный метод для получения ID пользователя по username
    private Long getUserIdByUsername(String username) {
        return userService.findByUsername(username)
                .map(User::getId)
                .orElse(null);
    }
}