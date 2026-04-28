package com.example.vag.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class YandexVisionService {

    @Value("${yandex.api.key:}")
    private String apiKey;

    @Value("${yandex.folder.id:}")
    private String folderId;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Проверяет изображение на наличие контента 18+ через Yandex Vision.
     */
    public boolean hasExplicitContent(MultipartFile file) throws IOException {
        if (apiKey == null || apiKey.isEmpty() || folderId == null || folderId.isEmpty()) {
            System.err.println("Yandex Vision не настроен. Пропускаем проверку.");
            return false;
        }

        try {
            String base64Image = encodeFileToBase64(file);

            // Правильный JSON для moderation модели
            String requestBody = String.format(
                    "{\"folderId\":\"%s\",\"analyze_specs\":[{\"content\":\"%s\",\"features\":[{\"type\":\"CLASSIFICATION\",\"classificationConfig\":{\"model\":\"moderation\"}}]}]}",
                    folderId, base64Image
            );

            System.out.println("📤 Отправка запроса в Yandex Vision...");

            Request request = new Request.Builder()
                    .url("https://vision.api.cloud.yandex.net/vision/v1/batchAnalyze")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .addHeader("Authorization", "Api-Key " + apiKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("❌ Yandex Vision error: " + response.code());
                    String errorBody = response.body() != null ? response.body().string() : "null";
                    System.err.println("Response: " + errorBody);
                    return false;
                }

                String body = response.body().string();
                JsonNode root = objectMapper.readTree(body);

                // Проверяем результаты классификации
                JsonNode results = root.path("results");
                if (results.isArray() && results.size() > 0) {
                    JsonNode classification = results.get(0).path("results").get(0).path("classification");
                    JsonNode properties = classification.path("properties");

                    for (JsonNode prop : properties) {
                        String name = prop.path("name").asText();
                        double confidence = prop.path("confidence").asDouble();

                        System.out.println("Yandex: " + name + " = " + String.format("%.2f", confidence));

                        // Проверяем категории взрослого контента
                        if (("adult".equals(name) || "adult_content".equals(name)) && confidence > 0.7) {
                            System.out.println("Обнаружен взрослый контент!");
                            return true;
                        }
                        if ("violence".equals(name) && confidence > 0.85) {
                            System.out.println("Обнаружены сцены насилия (отправляем на модерацию)");
                            // Можно вернуть true для блокировки или false для пропуска
                        }
                    }
                }
                System.out.println("Yandex Vision: контент OK");
            }
        } catch (Exception e) {
            System.err.println("Yandex Vision exception: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Определяет категории изображения.
     */
    public List<String> detectCategories(MultipartFile file) throws IOException {
        List<String> categories = new ArrayList<>();

        if (apiKey == null || apiKey.isEmpty() || folderId == null || folderId.isEmpty()) {
            return categories;
        }

        try {
            String base64Image = encodeFileToBase64(file);
            String requestBody = String.format(
                    "{\"folderId\":\"%s\",\"analyze_specs\":[{\"content\":\"%s\",\"features\":[{\"type\":\"LABEL_DETECTION\",\"maxResults\":10}]}]}",
                    folderId, base64Image
            );

            Request request = new Request.Builder()
                    .url("https://vision.api.cloud.yandex.net/vision/v1/batchAnalyze")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .addHeader("Authorization", "Api-Key " + apiKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String body = response.body().string();
                    JsonNode root = objectMapper.readTree(body);

                    JsonNode results = root.path("results");
                    if (results.isArray() && results.size() > 0) {
                        JsonNode labels = results.get(0).path("results").get(0).path("labelAnnotations");
                        for (JsonNode label : labels) {
                            if (label.path("confidence").asDouble() > 0.5) {
                                categories.add(label.path("description").asText());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Yandex Vision categories error: " + e.getMessage());
        }
        return categories;
    }
    private byte[] compressImage(MultipartFile file) throws IOException {
        // Читаем изображение
        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new IOException("Не удалось прочитать изображение");
        }

        // Максимальные размеры
        int maxWidth = 1024;
        int maxHeight = 1024;

        // Изменяем размер, если нужно
        int width = image.getWidth();
        int height = image.getHeight();

        if (width > maxWidth || height > maxHeight) {
            double ratio = Math.min((double) maxWidth / width, (double) maxHeight / height);
            int newWidth = (int) (width * ratio);
            int newHeight = (int) (height * ratio);

            Image scaledImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            BufferedImage newImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = newImage.createGraphics();
            g.drawImage(scaledImage, 0, 0, null);
            g.dispose();
            image = newImage;
        }

        // Сохраняем в JPEG с качеством 70%
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    private String encodeFileToBase64(MultipartFile file) throws IOException {
        // Сжимаем перед кодированием
        byte[] compressedBytes = compressImage(file);
        return Base64.getEncoder().encodeToString(compressedBytes);
    }
}