package com.example.vag.dto;

import java.util.ArrayList;
import java.util.List;

public class ModerationResult {
    private boolean approved;
    private String rejectionReason;
    private String rejectionReasonHtml;
    private boolean needsManualReview;
    private String manualReviewReason;
    private String manualReviewReasonHtml;
    private List<String> suggestedCategories = new ArrayList<>();
    private Long similarArtworkId;
    private String similarArtworkTitle;
    private String aiReport;

    // Геттеры и сеттеры
    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getRejectionReasonHtml() { return rejectionReasonHtml; }
    public void setRejectionReasonHtml(String rejectionReasonHtml) { this.rejectionReasonHtml = rejectionReasonHtml; }

    public boolean isNeedsManualReview() { return needsManualReview; }
    public void setNeedsManualReview(boolean needsManualReview) { this.needsManualReview = needsManualReview; }

    public String getManualReviewReason() { return manualReviewReason; }
    public void setManualReviewReason(String manualReviewReason) { this.manualReviewReason = manualReviewReason; }

    public String getManualReviewReasonHtml() { return manualReviewReasonHtml; }
    public void setManualReviewReasonHtml(String manualReviewReasonHtml) { this.manualReviewReasonHtml = manualReviewReasonHtml; }

    public List<String> getSuggestedCategories() { return suggestedCategories; }
    public void setSuggestedCategories(List<String> suggestedCategories) { this.suggestedCategories = suggestedCategories; }

    public Long getSimilarArtworkId() { return similarArtworkId; }
    public void setSimilarArtworkId(Long similarArtworkId) { this.similarArtworkId = similarArtworkId; }

    public String getSimilarArtworkTitle() { return similarArtworkTitle; }
    public void setSimilarArtworkTitle(String similarArtworkTitle) { this.similarArtworkTitle = similarArtworkTitle; }

    public String getAiReport() { return aiReport; }
    public void setAiReport(String aiReport) { this.aiReport = aiReport; }
}