package com.example.vag.recommendation.service;

import com.example.vag.recommendation.dto.RecommendationDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger logger = Logger.getLogger(RecommendationServiceImpl.class.getName());

    private final String pythonExecutable;
    private final String scriptPath;
    private final ObjectMapper objectMapper;

    public RecommendationServiceImpl() {
        this("python", getDefaultScriptPath());
    }

    public RecommendationServiceImpl(String pythonExecutable, String scriptPath) {
        this.pythonExecutable = pythonExecutable;
        this.scriptPath = scriptPath;
        this.objectMapper = new ObjectMapper();
        logger.info("RecommendationService initialized with script: " + scriptPath);
    }

    private static String getDefaultScriptPath() {
        String projectRoot = System.getProperty("user.dir");
        return Paths.get(projectRoot, "..", "ML-Recommendation", "recommendation_engine.py").toString();
    }

    @Override
    public List<RecommendationDTO> getRecommendationsForUser(Long userId, int topN) {
        if (!isRecommendationSystemAvailable()) {
            logger.warning("Система рекомендаций недоступна. Возвращается пустой список.");
            return Collections.emptyList();
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                pythonExecutable, scriptPath, "--user_id", userId.toString()
            );
            processBuilder.environment().put("PYTHONIOENCODING", "utf-8");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.severe("Python-скрипт завершился с кодом " + exitCode +
                              ". Вывод: " + output.toString());
                return Collections.emptyList();
            }

            return parseRecommendations(output.toString());

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка вызова Python-скрипта рекомендаций", e);
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<RecommendationDTO> parseRecommendations(String jsonOutput) {
        try {
            // Удаляем возможные предупреждения в начале (на всякий случай)
            int jsonStart = jsonOutput.indexOf('{');
            if (jsonStart > 0) {
                jsonOutput = jsonOutput.substring(jsonStart);
            }

            var rootNode = objectMapper.readTree(jsonOutput);

            if (rootNode.has("error")) {
                logger.warning("Python-скрипт вернул ошибку: " + rootNode.get("error").asText());
                return Collections.emptyList();
            }

            if (!rootNode.has("recommendations")) {
                logger.warning("В ответе отсутствует поле 'recommendations'");
                return Collections.emptyList();
            }

            var recommendationsNode = rootNode.get("recommendations");
            List<RecommendationDTO> recommendations = new ArrayList<>();

            for (var recNode : recommendationsNode) {
                RecommendationDTO dto = new RecommendationDTO();
                dto.setArtworkId(recNode.has("artwork_id") ? recNode.get("artwork_id").asLong() : null);
                dto.setTitle(recNode.has("title") ? recNode.get("title").asText() : "");
                dto.setAuthor(recNode.has("author") ? recNode.get("author").asText() : "");
                dto.setCategories(recNode.has("categories") ? recNode.get("categories").asText() : "");
                dto.setLikes(recNode.has("likes") ? recNode.get("likes").asInt() : 0);
                dto.setScore(recNode.has("score") ? recNode.get("score").asDouble() : 0.0);
                recommendations.add(dto);
            }

            return recommendations;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка парсинга JSON-ответа: " + jsonOutput, e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isRecommendationSystemAvailable() {
        try {
            File scriptFile = new File(scriptPath);
            if (!scriptFile.exists()) {
                logger.warning("Скрипт рекомендаций не найден: " + scriptPath);
                return false;
            }

            ProcessBuilder checkProcess = new ProcessBuilder(pythonExecutable, "--version");
            Process process = checkProcess.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                logger.warning("Python недоступен через команду: " + pythonExecutable);
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Ошибка проверки доступности системы рекомендаций", e);
            return false;
        }
    }

    @Override
    public void retrainModel() {
        clearModelCache();
        logger.info("Кэш модели очищен. При следующем запросе рекомендаций модель будет переобучена.");
    }

    @Override
    public void clearModelCache() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                pythonExecutable, scriptPath, "--clear_cache"
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.info("Кэш модели успешно очищен.");
            } else {
                logger.warning("Ошибка при очистке кэша модели, код: " + exitCode);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка вызова очистки кэша Python-скрипта", e);
        }
    }
}