package com.example.vag.service.impl;

import com.example.vag.dto.ModerationResult;
import com.example.vag.dto.SimilarArtworkInfo;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ModerationServiceImplTest {

    private YandexVisionService visionService;
    private ImageHashService imageHashService;
    private NSFWDetectionService nsfwService;
    private ModerationServiceImpl moderationService;

    @Before
    public void setUp() {
        visionService = mock(YandexVisionService.class);
        imageHashService = mock(ImageHashService.class);
        nsfwService = mock(NSFWDetectionService.class);
        moderationService = new ModerationServiceImpl(visionService, imageHashService, nsfwService);
    }

    @Test
    public void moderateImage_whenVisionDetectsExplicit_shouldReject() throws Exception {
        MockMultipartFile file = testImage();
        when(visionService.hasExplicitContent(file)).thenReturn(true);

        ModerationResult result = moderationService.moderateImage(file, null);

        assertFalse(result.isApproved());
        assertEquals("Изображение содержит контент 18+", result.getRejectionReason());
        verifyNoInteractions(nsfwService);
        verifyNoInteractions(imageHashService);
    }

    @Test
    public void moderateImage_whenNsfwDetectsExplicit_shouldRejectWithReason() throws Exception {
        MockMultipartFile file = testImage();
        when(visionService.hasExplicitContent(file)).thenReturn(false);
        when(nsfwService.isAvailable()).thenReturn(true);
        NSFWDetectionService.NsfwAnalysisResult analysisResult =
                new NSFWDetectionService.NsfwAnalysisResult(true, java.util.Map.of("porn", 0.95f));
        when(nsfwService.analyze(file)).thenReturn(analysisResult);
        when(nsfwService.getRejectionReason(analysisResult.getScores()))
                .thenReturn("Обнаружен откровенный контент (высокая уверенность)");

        ModerationResult result = moderationService.moderateImage(file, null);

        assertFalse(result.isApproved());
        assertEquals("Обнаружен откровенный контент (высокая уверенность)", result.getRejectionReason());
        verify(imageHashService, never()).findDuplicateByMd5(any());
    }

    @Test
    public void moderateImage_whenMd5Duplicate_shouldRejectAndAddHtmlLink() throws Exception {
        MockMultipartFile file = testImage();
        when(visionService.hasExplicitContent(file)).thenReturn(false);
        when(nsfwService.isAvailable()).thenReturn(false);
        when(imageHashService.findDuplicateByMd5(file))
                .thenReturn(new SimilarArtworkInfo(42L, "<script>alert(1)</script>", 0));

        ModerationResult result = moderationService.moderateImage(file, null);

        assertFalse(result.isApproved());
        assertNotNull(result.getRejectionReasonHtml());
        assertTrue(result.getRejectionReasonHtml().contains("/vag/artwork/details/42"));
        assertFalse(result.getRejectionReasonHtml().contains("<script>"));
    }

    @Test
    public void moderateImage_whenPhashSimilar_shouldSetManualReview() throws Exception {
        MockMultipartFile file = testImage();
        when(visionService.hasExplicitContent(file)).thenReturn(false);
        when(nsfwService.isAvailable()).thenReturn(false);
        when(imageHashService.findDuplicateByMd5(file)).thenReturn(null);
        when(imageHashService.findSimilarArtwork(file, 7L))
                .thenReturn(new SimilarArtworkInfo(77L, "Похожая работа", 9));

        ModerationResult result = moderationService.moderateImage(file, 7L);

        assertFalse(result.isApproved());
        assertTrue(result.isNeedsManualReview());
        assertEquals(Long.valueOf(77L), result.getSimilarArtworkId());
        assertNotNull(result.getManualReviewReason());
        assertNotNull(result.getManualReviewReasonHtml());
    }

    @Test
    public void moderateImage_whenAllChecksPass_shouldApprove() throws Exception {
        MockMultipartFile file = testImage();
        when(visionService.hasExplicitContent(file)).thenReturn(false);
        when(nsfwService.isAvailable()).thenReturn(false);
        when(imageHashService.findDuplicateByMd5(file)).thenReturn(null);
        when(imageHashService.findSimilarArtwork(file, null)).thenReturn(null);

        ModerationResult result = moderationService.moderateImage(file, null);

        assertTrue(result.isApproved());
        assertFalse(result.isNeedsManualReview());
        assertNotNull(result.getAiReport());
    }

    @Test
    public void moderateImage_whenVisionThrows_shouldContinuePipeline() throws Exception {
        MockMultipartFile file = testImage();
        when(visionService.hasExplicitContent(file)).thenThrow(new RuntimeException("vision down"));
        when(nsfwService.isAvailable()).thenReturn(false);
        when(imageHashService.findDuplicateByMd5(file)).thenReturn(null);
        when(imageHashService.findSimilarArtwork(file, null)).thenReturn(null);

        ModerationResult result = moderationService.moderateImage(file, null);

        assertTrue(result.isApproved());
        assertNotNull(result.getAiReport());
        assertTrue(result.getAiReport().contains("Ошибка"));
    }

    @Test
    public void moderateImage_whenNsfwSafe_shouldContinueToHashChecks() throws Exception {
        MockMultipartFile file = testImage();
        when(visionService.hasExplicitContent(file)).thenReturn(false);
        when(nsfwService.isAvailable()).thenReturn(true);
        NSFWDetectionService.NsfwAnalysisResult analysisResult =
                new NSFWDetectionService.NsfwAnalysisResult(false, java.util.Map.of("neutral", 0.99f));
        when(nsfwService.analyze(file)).thenReturn(analysisResult);
        when(imageHashService.findDuplicateByMd5(file)).thenReturn(null);
        when(imageHashService.findSimilarArtwork(file, null)).thenReturn(null);

        ModerationResult result = moderationService.moderateImage(file, null);

        assertTrue(result.isApproved());
        verify(imageHashService).findDuplicateByMd5(file);
        verify(imageHashService).findSimilarArtwork(file, null);
    }

    @Test
    public void moderateImage_whenMd5ServiceFails_shouldRejectWithAnalysisError() throws Exception {
        MockMultipartFile file = testImage();
        when(visionService.hasExplicitContent(file)).thenReturn(false);
        when(nsfwService.isAvailable()).thenReturn(false);
        when(imageHashService.findDuplicateByMd5(file)).thenThrow(new RuntimeException("db unavailable"));

        ModerationResult result = moderationService.moderateImage(file, null);

        assertFalse(result.isApproved());
        assertNotNull(result.getRejectionReason());
        assertTrue(result.getRejectionReason().contains("Ошибка при анализе"));
    }

    @Test
    public void moderateImage_whenDuplicateFound_shouldNotRunPhashCheck() throws Exception {
        MockMultipartFile file = testImage();
        when(visionService.hasExplicitContent(file)).thenReturn(false);
        when(nsfwService.isAvailable()).thenReturn(false);
        when(imageHashService.findDuplicateByMd5(file))
                .thenReturn(new SimilarArtworkInfo(10L, "Уже есть", 0));

        ModerationResult result = moderationService.moderateImage(file, null);

        assertFalse(result.isApproved());
        verify(imageHashService, never()).findSimilarArtwork(any(), any());
    }

    private MockMultipartFile testImage() {
        return new MockMultipartFile("imageFile", "test.jpg", "image/jpeg", new byte[]{1, 2, 3, 4});
    }
}
