package com.example.vag.recommendation.service;

import com.example.vag.recommendation.dto.RecommendationDTO;

import java.util.List;

/**
 * Интерфейс сервиса рекомендаций.
 * 
 * Определяет контракт для получения персональных рекомендаций
 * для пользователей на основе гибридной системы рекомендаций
 * (контентная + коллаборативная фильтрация).
 * 
 * Теперь с поддержкой рандомизированного выбора из ТОП-50 (Pinterest-подход):
 * при каждом обновлении страницы пользователь видит разные рекомендации!
 */
public interface RecommendationService {
    
    /**
     * Получить персонализированные рекомендации для пользователя.
     * 
     * НОВОЕ: Использует расширенный режим с получением ТОП-50 и случайным выбором N из них.
     * 
     * @param userId ID пользователя
     * @param topN количество рекомендаций для показа (по умолчанию 12)
     * @return список рекомендованных работ, случайно выбранных из ТОП-50 и отсортированных по релевантности
     */
    List<RecommendationDTO> getRecommendationsForUser(Long userId, int topN);
    
    /**
     * Получить рекомендации с использованием значения по умолчанию (12).
     * 
     * @param userId ID пользователя
     * @return список из 12 рекомендованных работ
     */
    default List<RecommendationDTO> getRecommendationsForUser(Long userId) {
        return getRecommendationsForUser(userId, 12);
    }
    
    /**
     * Проверка доступности системы рекомендаций.
     * 
     * @return true, если Python-скрипт и все зависимости доступны
     */
    boolean isRecommendationSystemAvailable();

    /**
     * Очистка кэша модели (устарел - используйте ModelManagementService).
     */
    void clearModelCache();

    /**
     * Переобучение модели (устарел - используйте ModelManagementService).
     */
    void retrainModel();
}

