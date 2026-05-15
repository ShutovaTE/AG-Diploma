package com.example.vag.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.concurrent.TimeUnit;

@Service
public class YandexVisionService {
    private static final Logger log = LoggerFactory.getLogger(YandexVisionService.class);
    private static final String VISION_URL = "https://vision.api.cloud.yandex.net/vision/v1/batchAnalyze";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_BASE_DELAY_MS = 300L;
    private static final double ADULT_CONFIDENCE_THRESHOLD = 0.7;
    private static final double VIOLENCE_CONFIDENCE_THRESHOLD = 0.85;

    @Value("${YANDEX_API_KEY:${yandex.api.key:}}")
    private String apiKey;

    @Value("${YANDEX_FOLDER_ID:${yandex.folder.id:}}")
    private String folderId;

    private OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .build();
    private ObjectMapper objectMapper = new ObjectMapper();
    private String visionUrl = VISION_URL;

    /**
     * Проверяет изображение на наличие контента 18+ через Yandex Vision.
     */
    public boolean hasExplicitContent(MultipartFile file) throws IOException {
        if (!isConfigured()) {
            log.warn("Yandex Vision не настроен. Проверка пропущена.");
            return false;
        }

        try {
            String base64Image = encodeFileToBase64(file);
            String requestBody = String.format(
                    "{\"folderId\":\"%s\",\"analyze_specs\":[{\"content\":\"%s\",\"features\":[{\"type\":\"CLASSIFICATION\",\"classificationConfig\":{\"model\":\"moderation\"}}]}]}",
                    folderId, base64Image
            );

            JsonNode root = executeVisionRequestWithRetry(requestBody);
            if (root == null) {
                return false;
            }
            return containsExplicitFromModeration(root);
        } catch (Exception e) {
            log.error("Ошибка при проверке Yandex Vision", e);
        }
        return false;
    }

    /**
     * Определяет категории изображения.
     */
    public List<String> detectCategories(MultipartFile file) throws IOException {
        List<String> categories = new ArrayList<>();

        if (!isConfigured()) {
            return categories;
        }

        try {
            String base64Image = encodeFileToBase64(file);
            String requestBody = String.format(
                    "{\"folderId\":\"%s\",\"analyze_specs\":[{\"content\":\"%s\",\"features\":[{\"type\":\"LABEL_DETECTION\",\"maxResults\":10}]}]}",
                    folderId, base64Image
            );
            JsonNode root = executeVisionRequestWithRetry(requestBody);
            if (root == null) {
                return categories;
            }
            JsonNode results = root.path("results");
            if (results.isArray() && results.size() > 0) {
                JsonNode labels = results.get(0).path("results").get(0).path("labelAnnotations");
                for (JsonNode label : labels) {
                    if (label.path("confidence").asDouble() > 0.5) {
                        categories.add(label.path("description").asText());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при определении категорий через Yandex Vision", e);
        }
        return categories;
    }

    private boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && folderId != null && !folderId.isBlank();
    }

    private JsonNode executeVisionRequestWithRetry(String rawJsonBody) throws IOException {
        IOException lastIoException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            Request request = new Request.Builder()
                    .url(visionUrl)
                    .post(RequestBody.create(rawJsonBody, MediaType.parse("application/json")))
                    .addHeader("Authorization", "Api-Key " + apiKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    ResponseBody body = response.body();
                    if (body == null) {
                        log.warn("Yandex Vision вернул пустой ответ (attempt={})", attempt);
                        return null;
                    }
                    return objectMapper.readTree(body.string());
                }

                String errorBody = response.body() != null ? response.body().string() : "";
                boolean shouldRetry = response.code() >= 500 || response.code() == 429;
                log.warn("Yandex Vision HTTP {} (attempt={}): {}", response.code(), attempt, errorBody);
                if (!shouldRetry || attempt == MAX_RETRIES) {
                    return null;
                }
            } catch (IOException e) {
                lastIoException = e;
                log.warn("Сетевая ошибка Yandex Vision (attempt={}): {}", attempt, e.getMessage());
                if (attempt == MAX_RETRIES) {
                    break;
                }
            }
            sleepBeforeRetry(attempt);
        }

        if (lastIoException != null) {
            throw lastIoException;
        }
        return null;
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            long delay = RETRY_BASE_DELAY_MS * (1L << (attempt - 1));
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("Ожидание повтора прервано");
        }
    }

    private boolean containsExplicitFromModeration(JsonNode root) {
        JsonNode results = root.path("results");
        if (!results.isArray() || results.isEmpty()) {
            return false;
        }
        JsonNode classification = results.get(0).path("results").get(0).path("classification");
        JsonNode properties = classification.path("properties");
        for (JsonNode prop : properties) {
            String name = prop.path("name").asText();
            double confidence = prop.path("confidence").asDouble();
            log.debug("YandexVision: {}={}", name, confidence);
            if (("adult".equals(name) || "adult_content".equals(name))
                    && confidence > ADULT_CONFIDENCE_THRESHOLD) {
                return true;
            }
            if ("violence".equals(name) && confidence > VIOLENCE_CONFIDENCE_THRESHOLD) {
                log.info("Обнаружены сцены насилия (confidence={})", confidence);
            }
        }
        return false;
    }

    void setHttpClientForTests(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    void setObjectMapperForTests(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void setVisionUrlForTests(String visionUrl) {
        this.visionUrl = visionUrl;
    }

    private byte[] compressImage(MultipartFile file) throws IOException {
        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new IOException("Не удалось прочитать изображение");
        }

        int maxWidth = 1024;
        int maxHeight = 1024;

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

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    private String encodeFileToBase64(MultipartFile file) throws IOException {
        byte[] compressedBytes = compressImage(file);
        return Base64.getEncoder().encodeToString(compressedBytes);
    }
}