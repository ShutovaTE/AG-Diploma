package com.example.vag.recommendation.service;

import com.example.vag.recommendation.dto.RecommendationDTO;

import java.util.List;

/**
 * Интерфейс сервиса рекомендаций.
 * 
 * Определяет контракт для получения персональных рекомендаций
 * для пользователей на основе гибридной системы рекомендаций
 * (контентная + коллаборативная фильтрация).
 */
public interface RecommendationService {
    
    /**
     * Получить персонализированные рекомендации для пользователя.
     * 
     * @param userId ID пользователя
     * @param topN количество рекомендаций (по умолчанию 10)
     * @return список рекомендованных работ
     */
    List<RecommendationDTO> getRecommendationsForUser(Long userId, int topN);
    
    /**
     * Получить рекомендации с использованием значения по умолчанию.
     * 
     * @param userId ID пользователя
     * @return список рекомендованных работ
     */
    default List<RecommendationDTO> getRecommendationsForUser(Long userId) {
        return getRecommendationsForUser(userId, 10);
    }
    
    /**
     * Проверка доступности системы рекомендаций.
     * 
     * @return true, если Python-скрипт и все зависимости доступны
     */
    boolean isRecommendationSystemAvailable();

    void clearModelCache();

    void retrainModel();
}
