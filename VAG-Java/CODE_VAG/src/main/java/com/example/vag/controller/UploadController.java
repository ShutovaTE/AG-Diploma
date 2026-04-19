package com.example.vag.controller;

import com.example.vag.util.FileUploadUtil;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

@Controller
public class UploadController {

    private final FileUploadUtil fileUploadUtil;

    public UploadController(FileUploadUtil fileUploadUtil) {
        this.fileUploadUtil = fileUploadUtil;
    }

    @GetMapping("/uploads/**")
    public ResponseEntity<byte[]> getUpload(HttpServletRequest request) throws IOException {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestUri.substring(contextPath.length());
        String objectKey = path.replaceFirst("^/uploads/", "");

        try (InputStream inputStream = fileUploadUtil.getFile(objectKey)) {
            byte[] bytes = inputStream.readAllBytes();
            String contentType = URLConnection.guessContentTypeFromName(objectKey);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(bytes);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
