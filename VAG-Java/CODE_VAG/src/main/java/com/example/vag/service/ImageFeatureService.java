package com.example.vag.service;

import com.example.vag.dto.ImageAnalysisResult;
import com.example.vag.service.impl.OpenClipService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ImageFeatureService {

    private final OpenClipService visionService;
    private final ObjectMapper objectMapper;

    public ImageFeatureService(OpenClipService visionService) {
        this.visionService = visionService;
        this.objectMapper = new ObjectMapper();
    }

    public ImageAnalysisResult analyze(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return new ImageAnalysisResult(null, null, null, null, null);
        }

        BufferedImage image;
        try {
            image = ImageIO.read(imageFile.getInputStream());
        } catch (IOException e) {
            return new ImageAnalysisResult(null, null, null, null, null);
        }

        if (image == null) {
            return new ImageAnalysisResult(null, null, null, null, null);
        }

        int width = image.getWidth();
        int height = image.getHeight();
        long redSum = 0;
        long greenSum = 0;
        long blueSum = 0;
        long pixelCount = 0;
        int[] histogram = new int[64];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                redSum += red;
                greenSum += green;
                blueSum += blue;
                pixelCount++;

                int redBin = red >> 6;
                int greenBin = green >> 6;
                int blueBin = blue >> 6;
                int index = (redBin << 4) | (greenBin << 2) | blueBin;
                histogram[index]++;
            }
        }

        Integer averageRed = pixelCount > 0 ? (int) (redSum / pixelCount) : null;
        Integer averageGreen = pixelCount > 0 ? (int) (greenSum / pixelCount) : null;
        Integer averageBlue = pixelCount > 0 ? (int) (blueSum / pixelCount) : null;
        String histogramJson = buildHistogramJson(histogram);

        String detectedObjects = null;
        try {
            List<String> labels = visionService.detectCategories(imageFile);
            List<String> normalized = labels.stream()
                    .filter(label -> label != null && !label.isBlank())
                    .map(this::normalizeTag)
                    .distinct()
                    .limit(10)
                    .collect(Collectors.toList());
            if (!normalized.isEmpty()) {
                detectedObjects = String.join(",", normalized);
            }
        } catch (Exception e) {
        }

        return new ImageAnalysisResult(averageRed, averageGreen, averageBlue, histogramJson, detectedObjects);
    }

    private String buildHistogramJson(int[] histogram) {
        if (histogram == null || histogram.length == 0) {
            return null;
        }
        try {
            Map<String, Integer> payload = new LinkedHashMap<>();
            for (int i = 0; i < histogram.length; i++) {
                payload.put("bin_" + i, histogram[i]);
            }
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeTag(String tag) {
        if (tag == null) {
            return null;
        }
        return tag.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
