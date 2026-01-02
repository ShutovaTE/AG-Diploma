package com.example.vagmobile.model;

import java.io.Serializable;

public class DocPage implements Serializable {
    private String title;
    private String rawUrl;

    public DocPage(String title, String rawUrl) {
        this.title = title;
        this.rawUrl = rawUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getRawUrl() {
        return rawUrl;
    }
}