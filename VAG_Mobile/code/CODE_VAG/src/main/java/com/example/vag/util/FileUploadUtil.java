package com.example.vag.util;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class FileUploadUtil {

    // ИСПРАВЛЕНО: Абсолютный путь к папке uploads
    private final String UPLOAD_BASE = "D:/Java/apache-tomcat-9.0.97/webapps/vag/uploads/";

    public String saveFile(Long userId, MultipartFile file) throws IOException {
        return saveFile(userId, file.getOriginalFilename(), file);
    }

    public String saveFile(Long userId, String fileName, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        // ИСПРАВЛЕНО: Сохраняем оригинальное имя файла, но делаем его безопасным
        String originalFileName = file.getOriginalFilename();
        String safeFileName = makeFileNameSafe(originalFileName);

        // ДОБАВЛЕНО: Если нужно уникальное имя, используем UUID + оригинальное имя
        String uniqueFileName = generateUniqueFileName(safeFileName);

        // Создаем путь для сохранения
        String userDir = UPLOAD_BASE + "artwork-images/" + userId + "/";
        Path uploadPath = Paths.get(userDir);

        System.out.println("=== FILE UPLOAD DEBUG ===");
        System.out.println("Original filename: " + originalFileName);
        System.out.println("Safe filename: " + safeFileName);
        System.out.println("Unique filename: " + uniqueFileName);
        System.out.println("Saving file to: " + uploadPath.toString());

        if (!Files.exists(uploadPath)) {
            System.out.println("Creating directory: " + uploadPath);
            Files.createDirectories(uploadPath);
        }

        // Сохраняем файл
        Path filePath = uploadPath.resolve(uniqueFileName);
        System.out.println("Full file path: " + filePath.toString());

        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File saved successfully: " + uniqueFileName);

            // Проверяем что файл создан
            if (Files.exists(filePath)) {
                System.out.println("File verified: " + filePath.toString());
                System.out.println("File size: " + Files.size(filePath) + " bytes");
            } else {
                System.err.println("ERROR: File was not created: " + filePath.toString());
            }
        } catch (IOException e) {
            System.err.println("Error saving file: " + e.getMessage());
            throw e;
        }

        // ВАЖНО: Возвращаем путь ОТНОСИТЕЛЬНО папки uploads
        String relativePath = "artwork-images/" + userId + "/" + uniqueFileName;
        System.out.println("Relative path for DB: " + relativePath);
        System.out.println("=== END FILE UPLOAD DEBUG ===");

        return relativePath;
    }

    /**
     * НОВЫЙ МЕТОД: Генерирует уникальное имя файла
     */
    private String generateUniqueFileName(String safeFileName) {
        // Вариант 2: UUID + оригинальное имя (рекомендуется - уникально)
        String fileExtension = "";
        int dotIndex = safeFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = safeFileName.substring(dotIndex);
            safeFileName = safeFileName.substring(0, dotIndex);
        }
        return safeFileName + "_" + UUID.randomUUID().toString().substring(0, 8) + fileExtension;
    }

    /**
     * Метод для нормализации пути к изображению
     */
    public static String normalizeImagePath(String imagePath) {
        if (imagePath == null) return null;

        System.out.println("Normalizing image path: " + imagePath);

        // Убираем начальный слеш если есть
        if (imagePath.startsWith("/")) {
            imagePath = imagePath.substring(1);
        }

        // Убираем дублирующиеся uploads/
        if (imagePath.startsWith("uploads/")) {
            imagePath = imagePath.substring("uploads/".length());
        }

        // ДОБАВЛЕНО: Убираем дублирующиеся vag/uploads/
        if (imagePath.startsWith("vag/uploads/")) {
            imagePath = imagePath.substring("vag/uploads/".length());
        }

        System.out.println("Normalized path: " + imagePath);
        return imagePath;
    }

    /**
     * Метод для получения полного URL для веб-доступа
     */
    public static String getWebAccessiblePath(String imagePath) {
        if (imagePath == null) return "/resources/images/default-artwork.jpg";

        // Если путь уже содержит /uploads/, возвращаем как есть
        if (imagePath.startsWith("/uploads/")) {
            return imagePath;
        }

        // Если путь уже содержит /vag/uploads/, возвращаем как есть
        if (imagePath.startsWith("/vag/uploads/")) {
            return imagePath;
        }

        // Иначе добавляем /uploads/
        return "/uploads/" + imagePath;
    }

    /**
     * Метод для проверки существования файла
     */
    public boolean fileExists(String imagePath) {
        if (imagePath == null) return false;

        // Нормализуем путь
        String normalizedPath = normalizeImagePath(imagePath);
        Path filePath = Paths.get(UPLOAD_BASE, normalizedPath);

        boolean exists = Files.exists(filePath);
        System.out.println("File exists check: " + filePath + " -> " + exists);

        if (exists) {
            try {
                System.out.println("File size: " + Files.size(filePath) + " bytes");
            } catch (IOException e) {
                System.err.println("Error getting file size: " + e.getMessage());
            }
        }

        return exists;
    }

    /**
     * Метод для получения абсолютного пути к файлу
     */
    public String getAbsolutePath(String imagePath) {
        if (imagePath == null) return null;

        String normalizedPath = normalizeImagePath(imagePath);
        return UPLOAD_BASE + normalizedPath;
    }

    /**
     * Метод для отладки пути изображения
     */
    public void debugImagePath(String imagePath, String context) {
        System.out.println("=== IMAGE PATH DEBUG: " + context + " ===");
        System.out.println("Original path: " + imagePath);
        System.out.println("Normalized path: " + normalizeImagePath(imagePath));
        System.out.println("Web accessible path: " + getWebAccessiblePath(imagePath));
        System.out.println("Absolute path: " + getAbsolutePath(imagePath));
        System.out.println("File exists: " + fileExists(imagePath));
        System.out.println("=== END DEBUG ===");
    }

    /**
     * НОВЫЙ МЕТОД: Сохраняет файл с оригинальным именем (без UUID)
     */
    public String saveFileWithOriginalName(Long userId, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        String originalFileName = file.getOriginalFilename();
        String safeFileName = makeFileNameSafe(originalFileName);

        // Создаем путь для сохранения
        String userDir = UPLOAD_BASE + "artwork-images/" + userId + "/";
        Path uploadPath = Paths.get(userDir);

        System.out.println("=== FILE UPLOAD WITH ORIGINAL NAME ===");
        System.out.println("Original filename: " + originalFileName);
        System.out.println("Safe filename: " + safeFileName);
        System.out.println("Saving file to: " + uploadPath.toString());

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Проверяем, не существует ли уже файл с таким именем
        Path filePath = uploadPath.resolve(safeFileName);
        int counter = 1;
        while (Files.exists(filePath)) {
            String nameWithoutExt = safeFileName.substring(0, safeFileName.lastIndexOf('.'));
            String ext = safeFileName.substring(safeFileName.lastIndexOf('.'));
            safeFileName = nameWithoutExt + "_" + counter + ext;
            filePath = uploadPath.resolve(safeFileName);
            counter++;
        }

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("File saved with original name: " + safeFileName);

        String relativePath = "artwork-images/" + userId + "/" + safeFileName;
        System.out.println("Relative path for DB: " + relativePath);
        System.out.println("=== END FILE UPLOAD ===");

        return relativePath;
    }

    /**
     * НОВЫЙ МЕТОД: Создает безопасное имя файла из оригинального
     */
    private String makeFileNameSafe(String originalFileName) {
        if (originalFileName == null) {
            return "image_" + System.currentTimeMillis() + ".jpg";
        }

        // Убираем путь если есть
        String fileName = originalFileName;
        int lastSeparator = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        if (lastSeparator > 0) {
            fileName = fileName.substring(lastSeparator + 1);
        }

        // Заменяем небезопасные символы
        fileName = fileName
                .replace(" ", "_")
                .replaceAll("[^a-zA-Z0-9._-]", "");

        // Убедимся что есть расширение
        if (!fileName.contains(".")) {
            fileName += ".jpg";
        }

        return fileName;
    }

    /**
     * НОВЫЙ МЕТОД: Получает оригинальное имя файла из пути
     */
    public static String getFileNameFromPath(String imagePath) {
        if (imagePath == null) return null;

        // Извлекаем имя файла из пути
        String normalizedPath = normalizeImagePath(imagePath);
        int lastSlash = normalizedPath.lastIndexOf('/');
        if (lastSlash >= 0) {
            return normalizedPath.substring(lastSlash + 1);
        }
        return normalizedPath;
    }

    public void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            System.out.println("deleteFile: путь пустой — ничего не удаляем");
            return;
        }

        try {
            // Нормализуем путь (убираем лишние /uploads/ и т.д.)
            String normalizedPath = normalizeImagePath(relativePath);
            Path fullPath = Paths.get(UPLOAD_BASE + normalizedPath).normalize();

            // Защита от атак типа ../../
            if (!fullPath.startsWith(Paths.get(UPLOAD_BASE).normalize())) {
                System.err.println("ОШИБКА БЕЗОПАСНОСТИ: Попытка удалить файл за пределами uploads! Путь: " + fullPath);
                return;
            }

            if (Files.exists(fullPath)) {
                Files.delete(fullPath);
                System.out.println("Файл успешно удалён: " + fullPath);
            } else {
                System.out.println("Файл не найден (уже удалён?): " + fullPath);
            }
        } catch (Exception e) {
            System.err.println("Не удалось удалить файл: " + relativePath);
            e.printStackTrace();
            // НЕ бросаем исключение — удаление публикации важнее, чем ошибка удаления файла
        }
    }
}