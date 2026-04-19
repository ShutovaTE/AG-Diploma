package com.example.vag.recommendation.dto;

/**
 * DTO для передачи данных о рекомендованных работах.
 * 
 * Этот класс предназначен для сериализации в JSON при вызове
 * Python-скрипта рекомендаций и передачи в frontend.
 */
public class RecommendationDTO {
    
    private Long artworkId;
    private String title;
    private String author;
    private String categories;
    private Integer likes;
    private Double score;
    
    public RecommendationDTO() {
    }
    
    public RecommendationDTO(Long artworkId, String title, String author, 
                             String categories, Integer likes, Double score) {
        this.artworkId = artworkId;
        this.title = title;
        this.author = author;
        this.categories = categories;
        this.likes = likes;
        this.score = score;
    }
    
    public Long getArtworkId() {
        return artworkId;
    }
    
    public void setArtworkId(Long artworkId) {
        this.artworkId = artworkId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getCategories() {
        return categories;
    }
    
    public void setCategories(String categories) {
        this.categories = categories;
    }
    
    public Integer getLikes() {
        return likes;
    }
    
    public void setLikes(Integer likes) {
        this.likes = likes;
    }
    
    public Double getScore() {
        return score;
    }
    
    public void setScore(Double score) {
        this.score = score;
    }
}
