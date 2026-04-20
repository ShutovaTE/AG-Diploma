package com.example.vag.dto;

import java.util.ArrayList;
import java.util.List;

public class ModerationResult {
    private boolean approved;
    private String rejectionReason;
    private boolean needsManualReview;
    private String manualReviewReason;
    private List<String> suggestedCategories = new ArrayList<>();

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public boolean isNeedsManualReview() { return needsManualReview; }
    public void setNeedsManualReview(boolean needsManualReview) { this.needsManualReview = needsManualReview; }

    public String getManualReviewReason() { return manualReviewReason; }
    public void setManualReviewReason(String manualReviewReason) { this.manualReviewReason = manualReviewReason; }

    public List<String> getSuggestedCategories() { return suggestedCategories; }
    public void setSuggestedCategories(List<String> suggestedCategories) { this.suggestedCategories = suggestedCategories; }
}