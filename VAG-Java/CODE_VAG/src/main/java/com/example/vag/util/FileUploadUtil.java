package com.example.vag.util;

import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
/**
 * Утилита для работы с MinIO: загрузка, удаление, чтение
 * и генерация временных ссылок на файлы.
 */
public class FileUploadUtil {
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public FileUploadUtil(MinioProperties minioProperties) {
        this.minioProperties = minioProperties;
        this.minioClient = MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    @PostConstruct
    public void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minioProperties.getBucket()).build()
            );
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucket()).build());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize MinIO bucket", e);
        }
    }

    /**
     * Генерирует presigned URL для прямой загрузки файла в MinIO с браузера пользователя.
     */
    public String generatePresignedUrl(Long userId, String originalFileName, String contentType) {
        try {
            String safeFileName = UUID.randomUUID().toString() + "_" +
                    originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");

            String objectKey = "artwork-images/" + userId + "/" + safeFileName;

            System.out.println("🔗 Генерация presigned URL для: " + objectKey);

            String presignedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .expiry(10, TimeUnit.MINUTES)
                            .build()
            );

            System.out.println("✅ Presigned URL создан (действителен 10 мин)");
            return presignedUrl;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    /**
     * Генерирует presigned URL для КОНКРЕТНОГО objectKey.
     */
    public String generatePresignedUrlForObject(String objectKey, String contentType) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .expiry(10, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL for: " + objectKey, e);
        }
    }

    /**
     * Возвращает публичный URL для доступа к файлу.
     */
    public String getPublicUrl(String objectKey) {
        return minioProperties.getEndpoint() + "/" + minioProperties.getBucket() + "/" + objectKey;
    }

    public void saveFile(Long userId, String safeFileName, MultipartFile multipartFile) throws IOException {
        String objectKey = "artwork-images/" + userId + "/" + safeFileName;
        try (InputStream inputStream = multipartFile.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .stream(inputStream, multipartFile.getSize(), -1)
                            .contentType(multipartFile.getContentType())
                            .build()
            );
        } catch (Exception e) {
            throw new IOException("Failed to upload file to MinIO", e);
        }
    }

    public InputStream getFile(String objectKey) throws IOException {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new IOException("Failed to load file from MinIO", e);
        }
    }

    public void deleteFile(String objectKey) throws IOException {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new IOException("Failed to delete file from MinIO", e);
        }
    }

    /**
     * Получает файл из MinIO как MultipartFile (для ИИ-модерации).
     */
    public MultipartFile getAsMultipartFile(String objectKey, String fileName) throws IOException {
        try {
            // Проверяем, существует ли объект
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .build()
            );

            System.out.println("✅ Файл найден в MinIO: " + objectKey + " (размер: " + stat.size() + " байт)");

            InputStream inputStream = getFile(objectKey);
            byte[] bytes = inputStream.readAllBytes();
            inputStream.close();

            final byte[] fileBytes = bytes;  // для использования в анонимном классе

            return new MultipartFile() {
                @Override
                public String getName() { return "imageFile"; }

                @Override
                public String getOriginalFilename() { return fileName; }

                @Override
                public String getContentType() {
                    String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
                    switch (ext) {
                        case "webp": return "image/webp";
                        case "png": return "image/png";
                        default: return "image/jpeg";
                    }
                }

                @Override
                public boolean isEmpty() { return fileBytes.length == 0; }

                @Override
                public long getSize() { return fileBytes.length; }

                @Override
                public byte[] getBytes() { return fileBytes; }

                @Override
                public InputStream getInputStream() { return new ByteArrayInputStream(fileBytes); }

                @Override
                public void transferTo(File dest) throws IOException {
                    Files.write(dest.toPath(), fileBytes);
                }
            };
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException e) {
            System.err.println("❌ Ошибка MinIO: " + e.getMessage());
            throw new IOException("Не удалось получить файл из MinIO: " + objectKey, e);
        }
    }

}