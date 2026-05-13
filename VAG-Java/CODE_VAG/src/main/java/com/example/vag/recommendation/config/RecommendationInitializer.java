package com.example.vag.recommendation.config;

import com.example.vag.recommendation.service.ModelManagementService;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import java.util.logging.Logger;

/**
 * Инициализатор системы рекомендаций при запуске приложения.
 * 
 * Этот компонент срабатывает при завершении инициализации контекста Spring
 * и запускает проверку/переобучение модели рекомендаций.
 */
@Component
public class RecommendationInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = Logger.getLogger(RecommendationInitializer.class.getName());

    private final ModelManagementService modelManagementService;

    // === ФЛАГ ДЛЯ ЗАЩИТЫ ОТ МНОЖЕСТВЕННЫХ СРАБАТЫВАНИЙ ===
    private static boolean initialized = false;

    public RecommendationInitializer(ModelManagementService modelManagementService) {
        this.modelManagementService = modelManagementService;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Защита от множественных срабатываний в случае multiple contexts
        if (initialized) {
            return;
        }
        initialized = true;

        logger.info("============================================");
        logger.info("Инициализация системы рекомендаций VAG");
        logger.info("============================================");

        // === ИНИЦИАЛИЗАЦИЯ МОДЕЛИ ===
        try {
            modelManagementService.initializeModel();
            logger.info("✓ Система рекомендаций инициализирована успешно");
        } catch (Exception e) {
            logger.severe("✗ Ошибка при инициализации системы рекомендаций: " + e.getMessage());
            e.printStackTrace();
        }

        logger.info("============================================");
    }
}
