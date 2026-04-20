package com.example.vag.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class ExternalImageSearchService {

    @Value("${serpapi.api.key:}")
    private String serpApiKey;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public int findSimilarImagesCount(MultipartFile file) throws IOException {
        if (serpApiKey == null || serpApiKey.isEmpty()) return 0;

        String base64Image;
        try (InputStream is = file.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            base64Image = Base64.getEncoder().encodeToString(bytes);
        }

        String imageUrl = "data:image/jpeg;base64," + base64Image;
        String encodedImageUrl = URLEncoder.encode(imageUrl, StandardCharsets.UTF_8);

        String url = "https://serpapi.com/search?engine=google_reverse_image&image_url="
                + encodedImageUrl + "&api_key=" + serpApiKey;

        Request request = new Request.Builder().url(url).build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) return 0;
            String body = response.body().string();
            JsonNode root = objectMapper.readTree(body);
            JsonNode inlineImages = root.path("inline_images");
            return inlineImages.isArray() ? inlineImages.size() : 0;
        }
    }
}