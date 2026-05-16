package com.example.vag.recommendation.service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Сервис для управления жизненным циклом модели рекомендаций.
 */
public class ModelManagementService {

    private static final Logger logger = Logger.getLogger(ModelManagementService.class.getName());

    // === КОНФИГУРАЦИЯ ===
    private final String pythonExecutable;
    private final String modelTrainerScriptPath;
    private final String modelCacheDir;
    private final String trainingLogPath;

    // === СОСТОЯНИЕ МОДЕЛИ ===
    private volatile boolean isModelReady = false;
    private volatile LocalDateTime lastTrainingTime = null;
    private volatile TrainingStatus trainingStatus = TrainingStatus.IDLE;
    private volatile String lastError = null;
    private final List<String> trainingLogs = Collections.synchronizedList(new ArrayList<>());

    // === СИНХРОНИЗАЦИЯ ===
    private final ReentrantReadWriteLock modelLock = new ReentrantReadWriteLock();

    // === ПЕРЕЧИСЛЕНИЕ СТАТУСОВ ===
    public enum TrainingStatus {
        IDLE("Модель не используется"),
        IN_PROGRESS("Обучение в процессе"),
        COMPLETED("Обучение завершено"),
        ERROR("Произошла ошибка при обучении");

        private final String description;

        TrainingStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // === КОНСТРУКТОР ===
    public ModelManagementService(String pythonExecutable,
                                  String modelTrainerScriptPath,
                                  String modelCacheDir) {

        String resolvedPython = resolvePythonExecutable(pythonExecutable);

        this.pythonExecutable = resolvedPython;
        this.modelTrainerScriptPath = resolvePath(modelTrainerScriptPath);
        this.modelCacheDir = resolvePath(modelCacheDir);
        this.trainingLogPath = Paths.get(this.modelCacheDir, "training.log").toString();

        logger.info("ModelManagementService инициализирован");
        logger.info("Python: " + this.pythonExecutable);
        logger.info("Trainer: " + this.modelTrainerScriptPath);
        logger.info("Cache dir: " + this.modelCacheDir);
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

        return userDir.resolve(path).normalize().toString();
    }

    /**
     * Определение корректного Python executable.
     */
    private static String resolvePythonExecutable(String candidateExecutable) {

        // если путь указан явно и существует — используем его
        if (candidateExecutable != null && !candidateExecutable.isBlank()) {

            Path path = Paths.get(candidateExecutable);

            if (Files.exists(path)) {
                return path.toAbsolutePath().normalize().toString();
            }
        }

        // fallback на python
        if (isCommandAvailable("python")) {
            return "python";
        }

        // fallback на py
        if (isCommandAvailable("py")) {
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

    // ========================================================================
    // МЕТОДЫ УПРАВЛЕНИЯ МОДЕЛЬЮ
    // ========================================================================

    public synchronized void initializeModel() {

        logger.info("Инициализация модели рекомендаций...");

        modelLock.writeLock().lock();

        try {

            if (isModelFileExists()) {

                isModelReady = true;
                lastTrainingTime = getModelTrainingTime();

                logger.info("✓ Модель найдена");
                logger.info("Последнее обучение: " + lastTrainingTime);

            } else {

                logger.info("Модель не найдена. Запуск первичного обучения...");

                new Thread(this::performModelTraining, "ModelTrainingThread").start();
            }

        } finally {
            modelLock.writeLock().unlock();
        }
    }

    public boolean retrainModel() {

        modelLock.readLock().lock();

        try {

            if (trainingStatus == TrainingStatus.IN_PROGRESS) {

                logger.warning("Обучение уже в процессе");

                return false;
            }

        } finally {
            modelLock.readLock().unlock();
        }

        new Thread(this::performModelTraining, "ModelTrainingThread").start();

        return true;
    }

    /**
     * Выполнение обучения модели.
     */
    private void performModelTraining() {

        modelLock.writeLock().lock();

        try {

            trainingStatus = TrainingStatus.IN_PROGRESS;
            lastError = null;

            trainingLogs.clear();

            addLog("Начало процесса обучения модели");
            addLog("Python executable: " + pythonExecutable);
            addLog("Trainer script: " + modelTrainerScriptPath);

            try {

                ProcessBuilder processBuilder = new ProcessBuilder(
                        pythonExecutable,
                        modelTrainerScriptPath,
                        "--force"
                );

                // КРИТИЧЕСКИ ВАЖНО:
                // запускать python из директории ML-Recommendation
                File workingDir = new File(modelTrainerScriptPath).getParentFile();
                processBuilder.directory(workingDir);

                processBuilder.environment().put("PYTHONIOENCODING", "utf-8");

                processBuilder.redirectErrorStream(true);

                logger.info("Запуск: " + String.join(" ", processBuilder.command()));
                logger.info("Working dir: " + workingDir.getAbsolutePath());

                Process process = processBuilder.start();

                StringBuilder output = new StringBuilder();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), "UTF-8"))) {

                    String line;

                    while ((line = reader.readLine()) != null) {

                        output.append(line).append("\n");

                        addLog(line);

                        logger.info("[PYTHON] " + line);
                    }
                }

                int exitCode = process.waitFor();

                logger.info("Python process exit code: " + exitCode);

                if (exitCode == 0) {

                    isModelReady = true;

                    lastTrainingTime = LocalDateTime.now();

                    trainingStatus = TrainingStatus.COMPLETED;

                    lastError = null;

                    addLog("✓ Обучение завершено успешно!");

                    logger.info("✓ Модель успешно переобучена");

                } else {

                    trainingStatus = TrainingStatus.ERROR;

                    lastError = "Python-скрипт завершился с кодом ошибки: " + exitCode;

                    addLog("✗ Ошибка: " + lastError);

                    logger.severe("✗ Ошибка обучения: " + lastError);

                    logger.severe("Вывод Python:\n" + output);
                }

            } catch (Exception e) {

                trainingStatus = TrainingStatus.ERROR;

                lastError = e.getMessage();

                addLog("✗ Исключение: " + lastError);

                logger.log(Level.SEVERE,
                        "✗ Ошибка при запуске скрипта обучения",
                        e);
            }

        } finally {

            modelLock.writeLock().unlock();
        }
    }

    public boolean isModelReady() {
        return isModelReady && trainingStatus != TrainingStatus.ERROR;
    }

    public Map<String, Object> getTrainingStatus() {

        modelLock.readLock().lock();

        try {

            Map<String, Object> status = new LinkedHashMap<>();

            status.put("model_ready", isModelReady);
            status.put("training_status", trainingStatus.name());
            status.put("training_status_description", trainingStatus.getDescription());
            status.put("last_training_time",
                    lastTrainingTime != null
                            ? lastTrainingTime.toString()
                            : "Никогда");

            status.put("last_error", lastError);

            status.put("model_exists", isModelFileExists());

            List<String> recentLogs = new ArrayList<>(trainingLogs);

            if (recentLogs.size() > 50) {
                recentLogs = recentLogs.subList(recentLogs.size() - 50, recentLogs.size());
            }

            status.put("recent_logs", recentLogs);

            return status;

        } finally {
            modelLock.readLock().unlock();
        }
    }

    public String getFullTrainingLog() {

        try {

            if (Files.exists(Paths.get(trainingLogPath))) {

                return new String(Files.readAllBytes(Paths.get(trainingLogPath)));
            }

        } catch (IOException e) {

            logger.log(Level.WARNING,
                    "Не удалось прочитать лог обучения",
                    e);
        }

        return "Лог обучения не найден";
    }

    public LocalDateTime getLastTrainingTime() {
        return lastTrainingTime;
    }

    // ========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ========================================================================

    private boolean isModelFileExists() {

        try {

            return Files.exists(
                    Paths.get(modelCacheDir, "recommendation_model.pkl")
            );

        } catch (Exception e) {

            return false;
        }
    }

    private LocalDateTime getModelTrainingTime() {

        try {

            String metadataPath = Paths.get(
                    modelCacheDir,
                    "model_metadata.json"
            ).toString();

            if (Files.exists(Paths.get(metadataPath))) {

                String content = new String(
                        Files.readAllBytes(Paths.get(metadataPath))
                );

                int startIdx = content.indexOf("\"training_date\": \"");

                if (startIdx > 0) {

                    startIdx += "\"training_date\": \"".length();

                    int endIdx = content.indexOf("\"", startIdx);

                    String dateStr = content.substring(startIdx, endIdx);

                    return LocalDateTime.parse(
                            dateStr,
                            DateTimeFormatter.ISO_DATE_TIME
                    );
                }
            }

        } catch (Exception e) {

            logger.log(Level.WARNING,
                    "Не удалось получить время обучения из метаданных",
                    e);
        }

        return LocalDateTime.now();
    }

    private void addLog(String message) {

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        String logEntry = "[" + timestamp + "] " + message;

        trainingLogs.add(logEntry);
    }

    public TrainingStatus getStatus() {
        return trainingStatus;
    }

    public void reset() {

        modelLock.writeLock().lock();

        try {

            isModelReady = false;

            trainingStatus = TrainingStatus.IDLE;

            lastError = null;

            trainingLogs.clear();

            logger.info("Состояние ModelManagementService сброшено");

        } finally {

            modelLock.writeLock().unlock();
        }
    }
}