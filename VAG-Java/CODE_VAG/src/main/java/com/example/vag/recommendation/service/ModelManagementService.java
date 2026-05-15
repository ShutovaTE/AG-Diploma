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
 * 
 * Обеспечивает:
 * - Инициализацию модели при старте приложения
 * - Асинхронное переобучение модели по триггеру администратора
 * - Отслеживание статуса обучения (IDLE, IN_PROGRESS, COMPLETED, ERROR)
 * - Получение логов процесса обучения
 * - Безопасный доступ к модели через ReentrantReadWriteLock
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
        IDLE("Модель неиспользуется"),
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
    public ModelManagementService(String pythonExecutable, String modelTrainerScriptPath, String modelCacheDir) {
        this.pythonExecutable = resolvePythonExecutable(pythonExecutable);
        this.modelTrainerScriptPath = resolvePath(modelTrainerScriptPath);
        this.modelCacheDir = resolvePath(modelCacheDir);
        this.trainingLogPath = Paths.get(this.modelCacheDir, "training.log").toString();
        logger.info("ModelManagementService инициализирован с Python: " + this.pythonExecutable + ", тренером: " + this.modelTrainerScriptPath);
        logger.info("ModelManagementService использует кэш модели: " + this.modelCacheDir);
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

    // === МЕТОДЫ УПРАВЛЕНИЯ МОДЕЛЬЮ ===

    /**
     * Инициализация модели при запуске приложения.
     * Проверяет наличие готовой модели, если её нет - запускает обучение.
     */
    public synchronized void initializeModel() {
        logger.info("Инициализация модели рекомендаций...");

        modelLock.writeLock().lock();
        try {
            if (isModelFileExists()) {
                isModelReady = true;
                lastTrainingTime = getModelTrainingTime();
                logger.info("✓ Модель найдена, инициализация успешна. " +
                        "Последнее обучение: " + lastTrainingTime);
            } else {
                logger.info("Модель не найдена. Запуск первичного обучения...");
                // Запуск обучения асинхронно при старте
                new Thread(this::performModelTraining, "ModelTrainingThread").start();
            }
        } finally {
            modelLock.writeLock().unlock();
        }
    }

    /**
     * Запуск переобучения модели.
     * Выполняется асинхронно в отдельном потоке, не блокирует основное приложение.
     * 
     * Возвращает:
     *     boolean: true если обучение было начато, false если уже в процессе
     */
    public boolean retrainModel() {
        modelLock.readLock().lock();
        try {
            if (trainingStatus == TrainingStatus.IN_PROGRESS) {
                logger.warning("Обучение уже в процессе. Запрос отклонён.");
                return false;
            }
        } finally {
            modelLock.readLock().unlock();
        }

        // Запуск обучения в отдельном потоке
        new Thread(this::performModelTraining, "ModelTrainingThread").start();
        return true;
    }

    /**
     * Выполнение процесса обучения модели.
     * Этот метод вызывается в отдельном потоке и не должен вызываться напрямую.
     */
    private void performModelTraining() {
        modelLock.writeLock().lock();
        try {
            trainingStatus = TrainingStatus.IN_PROGRESS;
            lastError = null;
            trainingLogs.clear();
            addLog("Начало процесса обучения модели");
            addLog("Запуск Python-скрипта: " + modelTrainerScriptPath);

            try {
                // === Запуск Python скрипта обучения ===
                ProcessBuilder processBuilder = new ProcessBuilder(
                        pythonExecutable,
                        modelTrainerScriptPath,
                        "--force"
                );
                processBuilder.environment().put("PYTHONIOENCODING", "utf-8");
                processBuilder.redirectErrorStream(true);

                logger.info("Запуск: " + String.join(" ", processBuilder.command()));
                Process process = processBuilder.start();

                // === Чтение выходных данных ===
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        addLog(line);
                    }
                }

                // === Ожидание завершения процесса ===
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    // Успешное обучение
                    isModelReady = true;
                    lastTrainingTime = LocalDateTime.now();
                    trainingStatus = TrainingStatus.COMPLETED;
                    lastError = null;

                    addLog("✓ Обучение завершено успешно!");
                    logger.info("✓ Модель успешно переобучена");

                } else {
                    // Ошибка при обучении
                    trainingStatus = TrainingStatus.ERROR;
                    lastError = "Python-скрипт завершился с кодом ошибки: " + exitCode;
                    addLog("✗ Ошибка: " + lastError);
                    logger.severe("✗ Ошибка обучения: " + lastError);
                }

            } catch (Exception e) {
                trainingStatus = TrainingStatus.ERROR;
                lastError = e.getMessage();
                addLog("✗ Исключение: " + lastError);
                logger.log(Level.SEVERE, "✗ Ошибка при запуске скрипта обучения", e);
            }

        } finally {
            modelLock.writeLock().unlock();
        }
    }

    /**
     * Проверка готовности модели.
     * Безопасное чтение статуса без блокировок (для частых проверок).
     */
    public boolean isModelReady() {
        return isModelReady && trainingStatus != TrainingStatus.ERROR;
    }

    /**
     * Получение информации о статусе обучения и модели.
     * Используется для admin-панели мониторинга.
     */
    public Map<String, Object> getTrainingStatus() {
        modelLock.readLock().lock();
        try {
            Map<String, Object> status = new LinkedHashMap<>();

            status.put("model_ready", isModelReady);
            status.put("training_status", trainingStatus.name());
            status.put("training_status_description", trainingStatus.getDescription());
            status.put("last_training_time", lastTrainingTime != null ? lastTrainingTime.toString() : "Никогда");
            status.put("last_error", lastError);
            status.put("model_exists", isModelFileExists());

            // Размер файла модели
            if (isModelFileExists()) {
                try {
                    long fileSize = Files.size(Paths.get(modelCacheDir, "recommendation_model.pkl"));
                    status.put("model_file_size_mb", String.format("%.2f", fileSize / (1024.0 * 1024.0)));
                } catch (IOException e) {
                    status.put("model_file_size_mb", "unknown");
                }
            }

            // Последние логи (последние 50 строк)
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

    /**
     * Получение полного лога обучения из файла training.log.
     */
    public String getFullTrainingLog() {
        try {
            if (Files.exists(Paths.get(trainingLogPath))) {
                return new String(Files.readAllBytes(Paths.get(trainingLogPath)));
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Не удалось прочитать лог обучения", e);
        }
        return "Лог обучения не найден";
    }

    /**
     * Получение дата-времени последнего обучения модели.
     */
    public LocalDateTime getLastTrainingTime() {
        return lastTrainingTime;
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    /**
     * Проверка наличия файла модели.
     */
    private boolean isModelFileExists() {
        try {
            return Files.exists(Paths.get(modelCacheDir, "recommendation_model.pkl"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Получение времени последнего обучения из метаданных модели.
     */
    private LocalDateTime getModelTrainingTime() {
        try {
            String metadataPath = Paths.get(modelCacheDir, "model_metadata.json").toString();
            if (Files.exists(Paths.get(metadataPath))) {
                // Парсим JSON метаданных (упрощённо)
                String content = new String(Files.readAllBytes(Paths.get(metadataPath)));
                // Извлекаем дату из поля "training_date"
                int startIdx = content.indexOf("\"training_date\": \"");
                if (startIdx > 0) {
                    startIdx += "\"training_date\": \"".length();
                    int endIdx = content.indexOf("\"", startIdx);
                    String dateStr = content.substring(startIdx, endIdx);
                    // Парсим ISO формат
                    return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Не удалось получить время обучения из метаданных", e);
        }
        return LocalDateTime.now();
    }

    /**
     * Добавление сообщения в лог обучения (синхронизированный список).
     */
    private void addLog(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String logEntry = "[" + timestamp + "] " + message;
        trainingLogs.add(logEntry);
    }

    /**
     * Получение текущего статуса обучения.
     */
    public TrainingStatus getStatus() {
        return trainingStatus;
    }

    /**
     * Сброс состояния (для тестирования).
     */
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
