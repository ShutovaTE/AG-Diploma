package com.example.vag.service.impl;

import com.example.vag.util.FileUploadUtil;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class NSFWDetectionServiceTest {

    private NSFWDetectionService nsfwDetectionService;

    @Before
    public void setUp() {
        nsfwDetectionService = new NSFWDetectionService(mock(FileUploadUtil.class));
    }

    @Test
    public void analyze_whenModelUnavailable_shouldReturnSafeResult() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "img.jpg", "image/jpeg", new byte[]{1, 2});

        NSFWDetectionService.NsfwAnalysisResult result = nsfwDetectionService.analyze(file);

        assertFalse(result.isExplicit());
        assertNotNull(result.getScores());
        assertTrue(result.getScores().isEmpty());
    }

    @Test
    public void getRejectionReason_whenPornVeryHigh_shouldReturnStrongMessage() {
        String reason = nsfwDetectionService.getRejectionReason(Map.of("porn", 0.95f));
        assertEquals("Обнаружен откровенный контент (высокая уверенность)", reason);
    }

    @Test
    public void getRejectionReason_whenPornAboveThreshold_shouldReturnAdultMessage() {
        String reason = nsfwDetectionService.getRejectionReason(Map.of("porn", 0.75f));
        assertEquals("Обнаружен контент для взрослых", reason);
    }

    @Test
    public void getRejectionReason_whenHentaiVeryHigh_shouldReturnHentaiStrongMessage() {
        String reason = nsfwDetectionService.getRejectionReason(Map.of("hentai", 0.96f));
        assertEquals("Обнаружен рисованный контент 18+ (высокая уверенность)", reason);
    }

    @Test
    public void getRejectionReason_whenHentaiAboveThreshold_shouldReturnHentaiMessage() {
        String reason = nsfwDetectionService.getRejectionReason(Map.of("hentai", 0.72f));
        assertEquals("Обнаружен рисованный контент для взрослых", reason);
    }

    @Test
    public void getRejectionReason_whenSexyAboveThreshold_shouldReturnSexyMessage() {
        String reason = nsfwDetectionService.getRejectionReason(Map.of("sexy", 0.85f));
        assertEquals("Обнаружен откровенный контент", reason);
    }

    @Test
    public void getRejectionReason_whenNoScores_shouldReturnGenericMessage() {
        String reason = nsfwDetectionService.getRejectionReason(Map.of());
        assertEquals("Обнаружен неподобающий контент", reason);
    }
}
