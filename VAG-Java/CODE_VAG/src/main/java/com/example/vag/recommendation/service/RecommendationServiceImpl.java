package com.example.vag.recommendation.service;

import com.example.vag.recommendation.dto.RecommendationDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Реализация сервиса рекомендаций.
 * 
 * Обеспечивает:
 * - Получение рекомендаций с обучением модели (старый режим, для обратной совместимости)
 * - Получение расширённого списка рекомендаций из готовой модели (новый режим)
 * - Рандомизированный выбор 12 из ТОП-50 при каждом запросе (Pinterest-подход)
 * - Проверку доступности системы рекомендаций
 */
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger logger = Logger.getLogger(RecommendationServiceImpl.class.getName());

    private final String pythonExecutable;
    private final String scriptPath;
    private final ObjectMapper objectMapper;
    private final ModelManagementService modelManagementService;

    // === КОНСТАНТЫ ===
    private static final int TOP_N_FROM_MODEL = 50;  // Получаем ТОП-50 из Python
    private static final int FINAL_RECOMMENDATIONS_COUNT = 12;  // Показываем 12 пользователю
    private static final Random random = new Random();

    // === КОНСТРУКТОРЫ ===

    public RecommendationServiceImpl() {
        this("D:/Git/AG-Diploma/VAG-Java/venv/Scripts/python.exe", getDefaultScriptPath(), null);
    }

    public RecommendationServiceImpl(String pythonExecutable, String scriptPath) {
        this(pythonExecutable, scriptPath, null);
    }

    public RecommendationServiceImpl(String pythonExecutable, String scriptPath, 
                                     ModelManagementService modelManagementService) {
        this.pythonExecutable = resolvePythonExecutable(pythonExecutable);
        this.scriptPath = resolvePath(scriptPath);
        this.modelManagementService = modelManagementService;
        this.objectMapper = new ObjectMapper();
        logger.info("RecommendationService инициализирован с Python: " + this.pythonExecutable + ", скрипт: " + this.scriptPath);
    }

    private static String getDefaultScriptPath() {
        return resolvePath(Paths.get("ML-Recommendation", "recommendation_engine.py").toString());
    }

    private static String resolvePath(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }

        Path candidate = Paths.get(path);
        if (candidate.isAbsolute()) {
            return candidate.normalize().toString();
        }

        Path userDir = Paths.get(System.getProperty("user.dir"));
        Path resolved = userDir.resolve(path).normalize();
        if (Files.exists(resolved)) {
            return resolved.toString();
        }

        Path current = userDir;
        for (int i = 0; i < 4 && current != null; i++) {
            resolved = current.resolve(path).normalize();
            if (Files.exists(resolved)) {
                return resolved.toString();
            }
            current = current.getParent();
        }

        // Если файл ещё не найден, возвращаем путь относительно рабочей директории,
        // чтобы последующая проверка могла отловить ошибку и залогировать путь.
        return userDir.resolve(path).normalize().toString();
    }

    private static String resolvePythonExecutable(String candidateExecutable) {
        if (candidateExecutable == null || candidateExecutable.isBlank()) {
            candidateExecutable = "python";
        }

        if (isCommandAvailable(candidateExecutable)) {
            return candidateExecutable;
        }

        if (!"py".equalsIgnoreCase(candidateExecutable) && isCommandAvailable("py")) {
            logger.info("Python executable '" + candidateExecutable + "' недоступен, используется 'py'");
            return "py";
        }

        return candidateExecutable;
    }

    private static boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command, "--version");
            Process process = builder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // === ОСНОВНЫЕ МЕТОДЫ ===

    /**
     * Получение рекомендаций для пользователя (новый режим).
     * 
     * Алгоритм:
     * 1. Загружает ТОП-50 релевантных работ из готовой модели
     * 2. Случайно выбирает 12 из них
     * 3. Сортирует выбранные 12 по релевантности (скорам)
     * 
     * При каждом обновлении страницы пользователь видит разные рекомендации!
     */
    @Override
    public List<RecommendationDTO> getRecommendationsForUser(Long userId, int topN) {
        // Используем новый режим расширенного получения рекомендаций
        return getRecommendationsForUserExtended(userId, topN);
    }

    /**
     * Получение расширённого списка рекомендаций (ТОП-50) с рандомизацией.
     * 
     * Это основной метод для получения рекомендаций при обновлении страницы.
     * 
     * Параметры:
     *     userId: ID пользователя
     *     topN: Сколько рекомендаций показать (по умолчанию 12)
     * 
     * Возвращает:
     *     Список из N рекомендаций, отсортированный по релевантности
     */
    public List<RecommendationDTO> getRecommendationsForUserExtended(Long userId, int topN) {
        if (topN <= 0) {
            topN = FINAL_RECOMMENDATIONS_COUNT;
        }

        // === Проверка готовности модели ===
        if (modelManagementService != null && !modelManagementService.isModelReady()) {
            logger.warning("Модель рекомендаций не готова. Пользователь: " + userId);
            return Collections.emptyList();
        }

        if (!isRecommendationSystemAvailable()) {
            logger.warning("Система рекомендаций недоступна. Пользователь: " + userId);
            return Collections.emptyList();
        }

        try {
            // === ШАГ 1: Получение ТОП-50 из Python ===
            List<RecommendationDTO> allRecommendations = getRecommendationsFromPythonExtended(userId);

            if (allRecommendations.isEmpty()) {
                logger.warning("Не получено рекомендаций из Python для пользователя: " + userId);
                return Collections.emptyList();
            }

            logger.info("Получено " + allRecommendations.size() + " рекомендаций для пользователя " + userId);

            // === ШАГ 2: Рандомный выбор topN из всех рекомендаций ===
            List<RecommendationDTO> randomized = selectRandomRecommendations(allRecommendations, topN);

            // === ШАГ 3: Сортировка выбранных по скорам (релевантности) ===
            randomized.sort((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()));

            logger.info("Возвращено " + randomized.size() + " рандомизированных рекомендаций для пользователя " + userId);
            return randomized;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при получении рекомендаций для пользователя " + userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Вызов Python скрипта с флагом --extended для получения ТОП-50.
     * 
     * Эта функция загружает готовую модель, не переучивает её.
     */
    private List<RecommendationDTO> getRecommendationsFromPythonExtended(Long userId) {
        try {
            // === Запуск Python скрипта в режиме --extended ===
            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExecutable,
                    scriptPath,
                    "--user_id", userId.toString(),
                    "--extended"
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
                logger.severe("Python-скрипт завершился с кодом ошибки " + exitCode);
                return Collections.emptyList();
            }

            return parseRecommendations(output.toString());

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка вызова Python-скрипта рекомендаций (extended)", e);
            return Collections.emptyList();
        }
    }

    /**
     * Рандомный выбор N рекомендаций из полного списка.
     * 
     * Если в списке меньше N элементов, возвращаются все.
     */
    private List<RecommendationDTO> selectRandomRecommendations(List<RecommendationDTO> allRecs, int n) {
        if (allRecs.size() <= n) {
            return new ArrayList<>(allRecs);
        }

        // Используем Fisher-Yates shuffle для выбора N случайных элементов
        List<RecommendationDTO> shuffled = new ArrayList<>(allRecs);
        Collections.shuffle(shuffled, random);
        return shuffled.stream()
                .limit(n)
                .collect(Collectors.toList());
    }

    /**
     * Парсинг JSON-ответа Python скрипта в список DTO.
     */
    @SuppressWarnings("unchecked")
    private List<RecommendationDTO> parseRecommendations(String jsonOutput) {
        try {
            // === Удаление возможных предупреждений перед JSON ===
            int jsonStart = jsonOutput.indexOf('{');
            if (jsonStart > 0) {
                jsonOutput = jsonOutput.substring(jsonStart);
            }

            var rootNode = objectMapper.readTree(jsonOutput);

            // === Проверка на ошибки ===
            if (rootNode.has("error")) {
                logger.warning("Python-скрипт вернул ошибку: " + rootNode.get("error").asText());
                return Collections.emptyList();
            }

            if (rootNode.has("success") && !rootNode.get("success").asBoolean()) {
                String errorMsg = rootNode.has("error") ? rootNode.get("error").asText() : "unknown";
                logger.warning("Python-скрипт вернул success=false. Ошибка: " + errorMsg);
                triggerRetrainIfNeeded(errorMsg);
                return Collections.emptyList();
            }

            if (!rootNode.has("recommendations")) {
                logger.warning("В ответе отсутствует поле 'recommendations'");
                return Collections.emptyList();
            }

            // === Парсинг рекомендаций ===
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

    /**
     * Проверка доступности системы рекомендаций.
     */
    @Override
    public boolean isRecommendationSystemAvailable() {
        try {
            // === Проверка наличия Python скрипта ===
            File scriptFile = new File(scriptPath);
            if (!scriptFile.exists()) {
                logger.warning("Python скрипт не найден: " + scriptPath);
                return false;
            }

            // === Проверка доступности Python интерпретатора ===
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

    private void triggerRetrainIfNeeded(String errorMsg) {
        if (modelManagementService == null) {
            return;
        }

        if (errorMsg == null) {
            return;
        }

        String normalized = errorMsg.toLowerCase();
        if (normalized.contains("требуется переобучение") || normalized.contains("не найдена") || normalized.contains("устарела") || normalized.contains("не удалось загрузить")) {
            boolean started = modelManagementService.retrainModel();
            if (started) {
                logger.info("Автоматическое переобучение запускается из-за ошибки рекомендаций: " + errorMsg);
            } else {
                logger.info("Автоматическое переобучение уже выполняется: " + errorMsg);
            }
        }
    }

    /**
     * Переобучение модели (старый метод для совместимости).
     * Теперь делегирует к ModelManagementService.
     */
    @Override
    public void retrainModel() {
        if (modelManagementService != null) {
            logger.info("Запуск переобучения модели через ModelManagementService");
            modelManagementService.retrainModel();
        } else {
            logger.warning("ModelManagementService не инициализирован, переобучение невозможно");
        }
    }

    /**
     * Очистка кэша модели (старый метод для совместимости).
     */
    @Override
    public void clearModelCache() {

        logger.info(
                "Модель рекомендаций помечена как устаревшая. "
                        + "Автоматическое переобучение отключено."
        );

    }
}
