package com.example.vag.service.impl;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class YandexVisionServiceIntegrationTest {

    private MockWebServer server;
    private YandexVisionService service;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        service = new YandexVisionService();
        service.setHttpClientForTests(new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .writeTimeout(2, TimeUnit.SECONDS)
                .callTimeout(3, TimeUnit.SECONDS)
                .build());
        service.setVisionUrlForTests(server.url("/vision/v1/batchAnalyze").toString());
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "folderId", "test-folder");
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void hasExplicitContent_whenAdultConfidenceHigh_shouldReturnTrue() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"results\":[{\"results\":[{\"classification\":{\"properties\":[{\"name\":\"adult\",\"confidence\":0.91}]}}]}]}"));

        boolean result = service.hasExplicitContent(testImage());

        assertTrue(result);
        assertEquals(1, server.getRequestCount());
    }

    @Test
    public void hasExplicitContent_whenServerFailsThenSuccess_shouldRetryAndReturnTrue() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("{\"message\":\"temporary\"}"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"results\":[{\"results\":[{\"classification\":{\"properties\":[{\"name\":\"adult_content\",\"confidence\":0.81}]}}]}]}"));

        boolean result = service.hasExplicitContent(testImage());

        assertTrue(result);
        assertEquals(2, server.getRequestCount());
    }

    @Test
    public void hasExplicitContent_whenUnauthorized_shouldReturnFalseWithoutRetry() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"message\":\"Unknown api key\"}"));

        boolean result = service.hasExplicitContent(testImage());

        assertFalse(result);
        assertEquals(1, server.getRequestCount());
    }

    private MockMultipartFile testImage() throws Exception {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return new MockMultipartFile("imageFile", "test.jpg", "image/jpeg", baos.toByteArray());
    }
}
