package com.example.vag.dto;

import java.util.Objects;

public class ImageAnalysisResult {
    private final Integer averageRed;
    private final Integer averageGreen;
    private final Integer averageBlue;
    private final String colorHistogram;
    private final String detectedObjects;

    public ImageAnalysisResult(Integer averageRed, Integer averageGreen, Integer averageBlue,
                               String colorHistogram, String detectedObjects) {
        this.averageRed = averageRed;
        this.averageGreen = averageGreen;
        this.averageBlue = averageBlue;
        this.colorHistogram = colorHistogram;
        this.detectedObjects = detectedObjects;
    }

    public Integer getAverageRed() {
        return averageRed;
    }

    public Integer getAverageGreen() {
        return averageGreen;
    }

    public Integer getAverageBlue() {
        return averageBlue;
    }

    public String getColorHistogram() {
        return colorHistogram;
    }

    public String getDetectedObjects() {
        return detectedObjects;
    }

    public boolean hasColorFeatures() {
        return averageRed != null && averageGreen != null && averageBlue != null;
    }

    public boolean hasDetectedObjects() {
        return detectedObjects != null && !detectedObjects.isBlank();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImageAnalysisResult)) return false;
        ImageAnalysisResult that = (ImageAnalysisResult) o;
        return Objects.equals(averageRed, that.averageRed)
                && Objects.equals(averageGreen, that.averageGreen)
                && Objects.equals(averageBlue, that.averageBlue)
                && Objects.equals(colorHistogram, that.colorHistogram)
                && Objects.equals(detectedObjects, that.detectedObjects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(averageRed, averageGreen, averageBlue, colorHistogram, detectedObjects);
    }
}
