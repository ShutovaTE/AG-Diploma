package com.example.vag.recommendation.controller;

import com.example.vag.recommendation.dto.RecommendationDTO;
import com.example.vag.recommendation.service.RecommendationService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * REST-контроллер для получения рекомендаций.
 * 
 * Возвращает JSON-ответы с персонализированными рекомендациями.
 * Может использоваться frontend'ом для отображения рекомендаций
 * на страницах пользователя, главной странице и т.д.
 * 
 * Эндпоинты:
 *   GET /api/recommendations        — рекомендации для авторизованного пользователя
 *   GET /api/recommendations/{userId} — рекомендации для указанного пользователя
 *   GET /api/recommendations/status  — статус системы рекомендаций
 */
// Для активации нужно раскомментировать аннотации и зарегистрировать бин в dispatcher-servlet.xml:
//
// @RestController
// @RequestMapping("/api/recommendations")
public class RecommendationController {
    
    private static final Logger logger = Logger.getLogger(RecommendationController.class.getName());
    
    private final RecommendationService recommendationService;
    
    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }
    
    /**
     * Получить рекомендации для авторизованного пользователя.
     * 
     * Пример запроса: GET /api/recommendations?topN=5
     * Пример ответа:
     * {
     *   "success": true,
     *   "recommendations": [
     *     {
     *       "artworkId": 5,
     *       "title": "Мона Лиза",
     *       "author": "Леонардо да Винчи",
     *       "categories": "портрет,ренессанс",
     *       "likes": 15,
     *       "score": 0.85
     *     }
     *   ],
     *   "count": 1
     * }
     */
    // Для активации раскомментировать:
    // @GetMapping
    // public ResponseEntity<Map<String, Object>> getRecommendationsForCurrentUser(
    //         @RequestParam(defaultValue = "10") int topN,
    //         Authentication authentication) {
    //     
    //     Map<String, Object> response = new HashMap<>();
    //     
    //     if (authentication == null) {
    //         response.put("success", false);
    //         response.put("message", "Пользователь не авторизован");
    //         return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    //     }
    //     
    //     // Получение ID пользователя из Spring Security
    //     String username = authentication.getName();
    //     // Здесь нужно получить User entity по username и извлечь ID
    //     // Это зависит от вашей реализации UserService
    //     Long userId = getUserIdByUsername(username);
    //     
    //     List<RecommendationDTO> recommendations = 
    //         recommendationService.getRecommendationsForUser(userId, topN);
    //     
    //     response.put("success", true);
    //     response.put("recommendations", recommendations);
    //     response.put("count", recommendations.size());
    //     return ResponseEntity.ok(response);
    // }
    
    /**
     * Получить рекомендации для указанного пользователя.
     * 
     * Пример запроса: GET /api/recommendations/1?topN=5
     * Доступно только администраторам.
     */
    // Для активации раскомментировать:
    // @GetMapping("/{userId}")
    // @PreAuthorize("hasRole('ADMIN')")
    // public ResponseEntity<Map<String, Object>> getRecommendationsForUser(
    //         @PathVariable Long userId,
    //         @RequestParam(defaultValue = "10") int topN) {
    //     
    //     Map<String, Object> response = new HashMap<>();
    //     
    //     List<RecommendationDTO> recommendations = 
    //         recommendationService.getRecommendationsForUser(userId, topN);
    //     
    //     response.put("success", true);
    //     response.put("userId", userId);
    //     response.put("recommendations", recommendations);
    //     response.put("count", recommendations.size());
    //     return ResponseEntity.ok(response);
    // }
    
    /**
     * Проверка статуса системы рекомендаций.
     * 
     * Пример запроса: GET /api/recommendations/status
     * Пример ответа:
     * {
     *   "available": true,
     *   "message": "Система рекомендаций готова к работе"
     * }
     */
    // Для активации раскомментировать:
    // @GetMapping("/status")
    // public ResponseEntity<Map<String, Object>> getStatus() {
    //     Map<String, Object> response = new HashMap<>();
    //     
    //     boolean available = recommendationService.isRecommendationSystemAvailable();
    //     response.put("available", available);
    //     
    //     if (available) {
    //         response.put("message", "Система рекомендаций готова к работе");
    //     } else {
    //         response.put("message", "Система рекомендаций недоступна. " +
    //             "Проверьте наличие Python и файла recommendation_engine.py");
    //     }
    //     
    //     return ResponseEntity.ok(response);
    // }
    
    // Вспомогательный метод для получения ID пользователя по имени
    // Нужно реализовать через UserService
    // private Long getUserIdByUsername(String username) {
    //     // Зависит от вашей реализации UserService
    //     // Пример: return userService.findByUsername(username).getId();
    //     return null;
    // }
}
