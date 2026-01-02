package com.example.vag.util;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

@Component
public class FileUploadUtil {
    private final String UPLOAD_BASE = "D:/Java/apache-tomcat-9.0.104/webapps/vag/uploads/";
//     private final String UPLOAD_BASE = "D:/Java/apache-tomcat-9.0.97/webapps/vag/uploads/";
    public void saveFile(Long userId, String safeFileName, MultipartFile multipartFile) throws IOException {
        String userDir = UPLOAD_BASE + "artwork-images/" + userId + "/";
        Path uploadPath = Paths.get(userDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        try (InputStream inputStream = multipartFile.getInputStream()) {
            Path filePath = uploadPath.resolve(safeFileName);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}