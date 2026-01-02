package com.example.vag.dto;

import java.time.LocalDate;
import java.util.List;

public class ExhibitionDTO {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private boolean authorOnly;
    private LocalDate createdAt;
    private UserDTO user;
    private int artworksCount;
    private ArtworkDTO firstArtwork; // Для превью первой работы в выставке

    // Конструкторы
    public ExhibitionDTO() {}

    public ExhibitionDTO(Long id, String title, String description, String imageUrl,
                        boolean authorOnly, LocalDate createdAt, int artworksCount) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.authorOnly = authorOnly;
        this.createdAt = createdAt;
        this.artworksCount = artworksCount;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isAuthorOnly() { return authorOnly; }
    public void setAuthorOnly(boolean authorOnly) { this.authorOnly = authorOnly; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }

    public int getArtworksCount() { return artworksCount; }
    public void setArtworksCount(int artworksCount) { this.artworksCount = artworksCount; }

    public ArtworkDTO getFirstArtwork() { return firstArtwork; }
    public void setFirstArtwork(ArtworkDTO firstArtwork) { this.firstArtwork = firstArtwork; }
}
