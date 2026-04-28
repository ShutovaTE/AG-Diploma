package com.example.vag.dto;

public class PresignedUploadResult {
    private String presignedUrl;    // URL для прямой загрузки
    private String objectKey;       // Путь в MinIO
    private String publicUrl;       // Публичный URL
    private String safeFileName;    // Безопасное имя файла

    public PresignedUploadResult(String presignedUrl, String objectKey,
                                 String publicUrl, String safeFileName) {
        this.presignedUrl = presignedUrl;
        this.objectKey = objectKey;
        this.publicUrl = publicUrl;
        this.safeFileName = safeFileName;
    }

    // Геттеры
    public String getPresignedUrl() { return presignedUrl; }
    public String getObjectKey() { return objectKey; }
    public String getPublicUrl() { return publicUrl; }
    public String getSafeFileName() { return safeFileName; }
}