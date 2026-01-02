package com.example.vagmobile.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Date;

public class Exhibition implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("id")
    private Long id;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("authorOnly")
    private boolean authorOnly;

    @SerializedName("createdAt")
    private Date createdAt;

    @SerializedName("user")
    private User user;

    @SerializedName("artworksCount")
    private int artworksCount;

    @SerializedName("firstArtwork")
    private Artwork firstArtwork;

    public Exhibition() {}

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

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public int getArtworksCount() { return artworksCount; }
    public void setArtworksCount(int artworksCount) { this.artworksCount = artworksCount; }

    public Artwork getFirstArtwork() { return firstArtwork; }
    public void setFirstArtwork(Artwork firstArtwork) { this.firstArtwork = firstArtwork; }

    public String getAuthorOnlyText() {
        return authorOnly ? "Приватная" : "Публичная";
    }

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.trim().isEmpty();
    }

    public boolean hasArtworks() {
        return artworksCount > 0;
    }

    public String getArtworksCountText() {
        if (artworksCount == 0) {
            return "Нет работ";
        } else if (artworksCount == 1) {
            return "1 работа";
        } else if (artworksCount < 5) {
            return artworksCount + " работы";
        } else {
            return artworksCount + " работ";
        }
    }
}
