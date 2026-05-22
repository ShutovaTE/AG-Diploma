package com.example.vag.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

@Service
public class OpenClipService {

    private static final Logger log = LoggerFactory.getLogger(OpenClipService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String pythonExecutable = "D:/Git/AG-Diploma/VAG-Java/venv/Scripts/python.exe";
    private final String scriptPath = "D:/Git/AG-Diploma/VAG-Java/CODE_VAG/ML-Recommendation/openclip_extractor.py";

    public List<String> detectCategories(MultipartFile file) {
        List<String> result = new ArrayList<>();
        if (file == null || file.isEmpty()) {
            return result;
        }

        File tmp = null;
        try {
            tmp = resizeForOpenClip(file);
            file.transferTo(tmp);

            ProcessBuilder pb = new ProcessBuilder(pythonExecutable, scriptPath, "--image", tmp.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process p = pb.start();

            byte[] out = p.getInputStream().readAllBytes();
            int exit = p.waitFor();
            String text = new String(out, StandardCharsets.UTF_8).trim();
            if (text.isEmpty()) {
                log.warn("OpenCLIP extractor produced no output (exit={})", exit);
                return result;
            }

            // Log raw output for diagnostics
            log.debug("OpenCLIP extractor raw output (exit={}): {}", exit, text.length() > 1000 ? text.substring(0, 1000) + "..." : text);

            JsonNode root = null;
            try {
                root = objectMapper.readTree(text);
            } catch (Exception ex) {
                List<Integer> candidates = new ArrayList<>();
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    if (c == '{' || c == '[') candidates.add(i);
                }
                for (int idx : candidates) {
                    String substr = text.substring(idx);
                    try {
                        root = objectMapper.readTree(substr);
                        break;
                    } catch (Exception ex2) {
                        // ignore and continue trying other positions
                    }
                }
                if (root == null) {
                    log.warn("Failed to parse JSON from OpenCLIP output after trimming: {}", ex.getMessage());
                }
            }

            if (root == null) {
                log.warn("OpenCLIP extractor did not return valid JSON (exit={})", exit);
                return result;
            }

            if (root.has("error")) {
                log.warn("OpenCLIP extractor returned error: {}", root.path("message").asText());
                return result;
            }
            if (root.has("tags") && root.get("tags").isArray()) {
                for (JsonNode n : root.get("tags")) {
                    result.add(n.asText());
                }
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error while running OpenCLIP extractor", e);
        } finally {
            if (tmp != null && tmp.exists()) {
                tmp.delete();
            }
        }

        return result;
    }

    private File resizeForOpenClip(MultipartFile file) throws IOException {

        BufferedImage original =
                javax.imageio.ImageIO.read(file.getInputStream());

        int targetSize = 224;

        BufferedImage resized =
                new BufferedImage(
                        targetSize,
                        targetSize,
                        BufferedImage.TYPE_INT_RGB
                );

        java.awt.Graphics2D g = resized.createGraphics();

        g.drawImage(
                original,
                0,
                0,
                targetSize,
                targetSize,
                null
        );

        g.dispose();

        File tmp =
                File.createTempFile(
                        "openclip_",
                        ".jpg"
                );

        javax.imageio.ImageIO.write(
                resized,
                "jpg",
                tmp
        );

        return tmp;
    }
}
