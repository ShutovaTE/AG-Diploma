package com.example.vag.util;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class UploadsInitializer {

    @PostConstruct
    public void init() {
        // Storage moved to MinIO (S3), local folder initialization is no longer required.
    }
}