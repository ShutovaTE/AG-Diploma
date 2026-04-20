package com.example.vag.service;

import com.example.vag.dto.ModerationResult;
import org.springframework.web.multipart.MultipartFile;

public interface ModerationService {
    ModerationResult moderateImage(MultipartFile file, Long excludeArtworkId);
}