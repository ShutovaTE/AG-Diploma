package com.example.vag.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.vag.recommendation.service.ModelManagementService;

@Service
public class RecommendationRetrainService {

    private final ModelManagementService modelManagementService;

    public RecommendationRetrainService(
            ModelManagementService modelManagementService
    ) {
        this.modelManagementService =
                modelManagementService;
    }

    @Async
    public void retrainAsync() {

        try {
            modelManagementService.retrainModel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}