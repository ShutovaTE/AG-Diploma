package com.example.vag.dto;

public class SimilarArtworkInfo {
    private Long artworkId;
    private String title;
    private int hammingDistance;

    public SimilarArtworkInfo(Long artworkId, String title, int hammingDistance) {
        this.artworkId = artworkId;
        this.title = title;
        this.hammingDistance = hammingDistance;
    }

    public Long getArtworkId() { return artworkId; }
    public String getTitle() { return title; }
    public int getHammingDistance() { return hammingDistance; }

    /**
     * Возвращает процент сходства (0 = идентично, 100 = полностью разное).
     */
    public int getSimilarityPercent() {
        return 100 - (hammingDistance * 100 / 64);
    }
}