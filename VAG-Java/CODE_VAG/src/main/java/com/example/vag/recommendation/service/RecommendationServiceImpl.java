package com.example.vag.recommendation.service;

import com.example.vag.recommendation.dto.RecommendationDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Реализация сервиса рекомендаций.
 * 
 * Вызывает Python-скрипт recommendation_engine.py через ProcessBuilder,
 * получает JSON-ответ и десериализует его в RecommendationDTO.
 * 
 * ТРЕБОВАНИЯ:
 * 1. Python 3.8+ установлен и доступен через команду "python"
 * 2. Установлены зависимости:
 *    - pip install pandas numpy scikit-learn mysql-connector-python
 * 3. recommendation_engine.py доступен по пути {projectRoot}/ML-Recommendation/
 * 4. В recommendation_engine.py настроено подключение к MySQL
 */
public class RecommendationServiceImpl implements RecommendationService {
    
    private static final Logger logger = Logger.getLogger(RecommendationServiceImpl.class.getName());
    
    private final String pythonExecutable;
    private final String scriptPath;
    private final ObjectMapper objectMapper;
    
    /**
     * Конструктор с путями по умолчанию.
     * 
     * Ожидает, что скрипт находится в ../ML-Recommendation/recommendation_engine.py
     * относительно директории проекта.
     */
    public RecommendationServiceImpl() {
        this("python", getDefaultScriptPath());
    }
    
    /**
     * Конструктор с настраиваемыми путями.
     * 
     * @param pythonExecutable путь к Python (например, "python", "python3" или полный путь)
     * @param scriptPath путь к скрипту рекомендаций
     */
    public RecommendationServiceImpl(String pythonExecutable, String scriptPath) {
        this.pythonExecutable = pythonExecutable;
        this.scriptPath = scriptPath;
        this.objectMapper = new ObjectMapper();
    }
    
    private static String getDefaultScriptPath() {
        // Ожидается, что скрипт лежит рядом с директорией CODE_VAG
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
            // Формирование команды: python recommendation_engine.py --user_id 1
            ProcessBuilder processBuilder = new ProcessBuilder(
                pythonExecutable, scriptPath, "--user_id", userId.toString()
            );
            processBuilder.redirectErrorStream(true); // Объединяем stdout и stderr
            
            Process process = processBuilder.start();
            
            // Чтение вывода Python-скрипта
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            
            // Ожидание завершения процесса
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.severe("Python-скрипт завершился с кодом " + exitCode + 
                              ". Вывод: " + output.toString());
                return Collections.emptyList();
            }
            
            // Парсинг JSON-ответа
            return parseRecommendations(output.toString());
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка вызова Python-скрипта рекомендаций", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Парсинг JSON-ответа от Python-скрипта.
     * 
     * Ожидаемый формат:
     * {
     *   "user_id": 1,
     *   "recommendations": [
     *     {
     *       "artwork_id": 5,
     *       "title": "Мона Лиза",
     *       "author": "Леонардо да Винчи",
     *       "categories": "портрет,ренессанс",
     *       "likes": 15,
     *       "score": 0.85
     *     }
     *   ],
     *   "algorithm": "hybrid"
     * }
     */
    @SuppressWarnings("unchecked")
    private List<RecommendationDTO> parseRecommendations(String jsonOutput) {
        try {
            // Парсим весь JSON
            var rootNode = objectMapper.readTree(jsonOutput);
            
            // Проверяем наличие ошибок
            if (rootNode.has("error")) {
                logger.warning("Python-скрипт вернул ошибку: " + rootNode.get("error").asText());
                return Collections.emptyList();
            }
            
            // Извлекаем массив рекомендаций
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
            // Проверка доступности скрипта
            File scriptFile = new File(scriptPath);
            if (!scriptFile.exists()) {
                logger.warning("Скрипт рекомендаций не найден: " + scriptPath);
                return false;
            }
            
            // Проверка доступности Python
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
}
