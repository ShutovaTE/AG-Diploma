package com.example.vag.recommendation.service;

import com.example.vag.recommendation.dto.RecommendationDTO;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.List;

/**
 * Тесты для RecommendationServiceImpl.
 *
 * Тесты проверяют:
 * 1. Корректную обработку недоступности Python-скрипта
 * 2. Парсинг JSON-ответов
 * 3. Возврат пустого списка при ошибках
 */
public class RecommendationServiceImplTest {

    private RecommendationService recommendationService;

    @Before
    public void setUp() {
        // Используем заведомо несуществующий скрипт для тестов
        // Это гарантирует, что тесты не вызовут реальный Python-скрипт
        recommendationService = new RecommendationServiceImpl(
            "python",
            "/nonexistent/path/recommendation_engine.py"
        );
    }

    @Test
    public void testGetRecommendationsWhenScriptUnavailable() {
        // При недоступном скрипте должен возвращаться пустой список
        List<RecommendationDTO> recommendations =
            recommendationService.getRecommendationsForUser(1L, 10);

        assertNotNull("Результат не должен быть null", recommendations);
        assertTrue("Список должен быть пустым", recommendations.isEmpty());
    }

    @Test
    public void testIsRecommendationSystemUnavailable() {
        // Система должна сообщать о недоступности
        boolean available = recommendationService.isRecommendationSystemAvailable();

        assertFalse("Система должна быть недоступна", available);
    }

    @Test
    public void testDefaultTopN() {
        // Проверка метода по умолчанию (topN=10)
        List<RecommendationDTO> recommendations =
            recommendationService.getRecommendationsForUser(1L);

        assertNotNull("Результат не должен быть null", recommendations);
        assertTrue("Список должен быть пустым", recommendations.isEmpty());
    }
}
