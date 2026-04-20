package com.example.vag.service.impl;

import ai.onnxruntime.*;
import com.example.vag.util.FileUploadUtil;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

@Service
public class NSFWDetectionService {

    private final FileUploadUtil fileUploadUtil;
    private OrtEnvironment env;
    private OrtSession session;

    // Путь к модели в MinIO
    private static final String MODEL_PATH = "models/nsfw_model.onnx";

    private static final String[] CLASSES = {"drawings", "hentai", "neutral", "porn", "sexy"};
    private static final double PORN_THRESHOLD = 0.7;
    private static final double HENTAI_THRESHOLD = 0.7;
    private static final double SEXY_THRESHOLD = 0.8;

    public NSFWDetectionService(FileUploadUtil fileUploadUtil) {
        this.fileUploadUtil = fileUploadUtil;
    }

    @PostConstruct
    public void init() {
        try {
            env = OrtEnvironment.getEnvironment();

            System.out.println("Загрузка NSFW модели из MinIO: " + MODEL_PATH);

            // Загружаем модель из MinIO
            byte[] modelBytes;
            try (InputStream is = fileUploadUtil.getFile(MODEL_PATH)) {
                modelBytes = is.readAllBytes();
            }

            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);

            session = env.createSession(modelBytes, options);
            System.out.println(" модель успешно загружена из MinIO (" + modelBytes.length + " байт)");

        } catch (Exception e) {
            System.err.println("Ошибка загрузки NSFW модели из MinIO: " + e.getMessage());
            System.err.println("   Убедитесь, что файл " + MODEL_PATH + " загружен в MinIO");
            System.err.println("   NSFW-проверка будет отключена");
            session = null;
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (session != null) session.close();
            if (env != null) env.close();
        } catch (OrtException e) {
            System.err.println("Ошибка при закрытии NSFW сессии: " + e.getMessage());
        }
    }

    /**
     * Проверяет, доступна ли модель.
     */
    public boolean isAvailable() {
        return session != null;
    }

    /**
     * Проверяет, содержит ли изображение контент 18+.
     */
    public boolean isNSFW(MultipartFile file) throws IOException, OrtException {
        if (!isAvailable()) {
            System.out.println("NSFW модель недоступна, пропускаем проверку");
            return false;
        }

        float[] inputData = preprocessImage(file);
        long[] shape = {1, 299, 299, 3};
        OnnxTensor inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData), shape);

        Map<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input_1", inputTensor);

        OrtSession.Result result = session.run(inputs);
        OnnxTensor outputTensor = (OnnxTensor) result.get(0);

        float[] output = new float[CLASSES.length];
        outputTensor.getFloatBuffer().get(output);

        inputTensor.close();
        result.close();

        Map<String, Float> scores = new HashMap<>();
        for (int i = 0; i < CLASSES.length; i++) {
            scores.put(CLASSES[i], output[i]);
        }

        System.out.println("📊 NSFW Scores: " + scores);

        if (scores.get("porn") > PORN_THRESHOLD) return true;
        if (scores.get("hentai") > HENTAI_THRESHOLD) return true;
        if (scores.get("sexy") > SEXY_THRESHOLD) return true;
        if (scores.get("porn") + scores.get("hentai") > 1.2) return true;

        return false;
    }

    private float[] preprocessImage(MultipartFile file) throws IOException {
        Path tempFile = Files.createTempFile("nsfw_", ".jpg");
        try {
            Files.copy(file.getInputStream(), tempFile,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            Mat img = imread(tempFile.toAbsolutePath().toString());
            if (img.empty()) {
                throw new IOException("Не удалось загрузить изображение");
            }

            Mat resized = new Mat();
            resize(img, resized, new Size(299, 299));

            Mat rgb = new Mat();
            cvtColor(resized, rgb, COLOR_BGR2RGB);

            int totalElements = 299 * 299 * 3;
            float[] data = new float[totalElements];

            byte[] byteData = new byte[totalElements];
            rgb.data().get(byteData, 0, totalElements);

            for (int i = 0; i < totalElements; i++) {
                data[i] = (byteData[i] & 0xFF) / 255.0f;
            }

            img.release();
            resized.release();
            rgb.release();

            return data;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Возвращает понятную причину отклонения.
     */
    public String getRejectionReason(MultipartFile file) throws IOException, OrtException {
        if (!isAvailable()) {
            return "NSFW модель недоступна";
        }

        float[] inputData = preprocessImage(file);
        long[] shape = {1, 299, 299, 3};
        OnnxTensor inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData), shape);

        Map<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input_1", inputTensor);

        OrtSession.Result result = session.run(inputs);
        OnnxTensor outputTensor = (OnnxTensor) result.get(0);

        float[] output = new float[CLASSES.length];
        outputTensor.getFloatBuffer().get(output);
        inputTensor.close();
        result.close();

        Map<String, Float> scores = new HashMap<>();
        for (int i = 0; i < CLASSES.length; i++) {
            scores.put(CLASSES[i], output[i]);
        }

        if (scores.get("porn") > 0.9) {
            return "Обнаружен откровенный контент (высокая уверенность)";
        } else if (scores.get("porn") > PORN_THRESHOLD) {
            return "Обнаружен контент для взрослых";
        } else if (scores.get("hentai") > 0.9) {
            return "Обнаружен рисованный контент 18+ (высокая уверенность)";
        } else if (scores.get("hentai") > HENTAI_THRESHOLD) {
            return "Обнаружен рисованный контент для взрослых";
        } else if (scores.get("sexy") > SEXY_THRESHOLD) {
            return "Обнаружен откровенный контент";
        }
        return "Обнаружен неподобающий контент";
    }
}