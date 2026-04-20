package com.example.vag.util;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class HashUtils {

    /**
     * Вычисляет перцептивный хеш (pHash) изображения с использованием OpenCV.
     * Возвращает 64-битный хеш в виде hex-строки.
     */
    public static String computePHash(MultipartFile file) throws IOException {
        Path tempFile = Files.createTempFile("upload_", ".tmp");
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        Mat img = imread(tempFile.toAbsolutePath().toString());
        Files.deleteIfExists(tempFile);

        if (img.empty()) {
            return null;
        }

        // 1. Изменяем размер до 32x32
        Mat resized = new Mat();
        resize(img, resized, new Size(32, 32));

        // 2. Преобразуем в оттенки серого
        Mat gray = new Mat();
        cvtColor(resized, gray, COLOR_BGR2GRAY);

        // 3. Преобразуем в float для DCT
        Mat grayFloat = new Mat();
        gray.convertTo(grayFloat, CV_32F);

        // 4. Применяем DCT
        Mat dct = new Mat();
        dct(grayFloat, dct);

        // 5. Берём верхний левый квадрат 8x8 (исключая первый столбец/строку для лучшей устойчивости)
        Mat dctSub = dct.rowRange(1, 9).colRange(1, 9).clone();

        // 6. Вычисляем среднее значение
        Scalar meanScalar = mean(dctSub);
        double meanValue = meanScalar.get(0);

        // 7. Строим хеш
        long hash = 0L;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                float pixelValue = dctSub.ptr(i).getFloat(j);
                if (pixelValue > meanValue) {
                    hash |= (1L << (i * 8 + j));
                }
            }
        }

        // Освобождаем память
        img.release();
        resized.release();
        gray.release();
        grayFloat.release();
        dct.release();
        dctSub.release();

        return String.format("%016x", hash);
    }

    /**
     * Вычисляет MD5 хеш содержимого файла.
     */
    public static String computeMD5(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * Вычисляет расстояние Хэмминга между двумя pHash.
     */
    public static int hammingDistance(String hash1, String hash2) {
        if (hash1 == null || hash2 == null) return Integer.MAX_VALUE;
        long h1 = Long.parseUnsignedLong(hash1, 16);
        long h2 = Long.parseUnsignedLong(hash2, 16);
        return Long.bitCount(h1 ^ h2);
    }

    /**
     * Проверяет, похожи ли изображения по pHash.
     * Порог 10 означает, что изображения считаются похожими, если различаются не более чем на 10 бит из 64.
     */
    public static boolean isSimilar(String hash1, String hash2, int threshold) {
        if (hash1 == null || hash2 == null) return false;
        return hammingDistance(hash1, hash2) <= threshold;
    }
}