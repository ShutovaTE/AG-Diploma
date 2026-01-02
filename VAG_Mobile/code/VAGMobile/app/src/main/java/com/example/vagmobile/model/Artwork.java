package com.example.vagmobile.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Artwork implements Serializable {
    private static final long serialVersionUID = 1L;

    @SerializedName("id")
    private Long id;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("imagePath")
    private String imagePath;

    @SerializedName("dateCreation")
    private Date dateCreation;

    @SerializedName("status")
    private String status;

    @SerializedName("likes")
    private int likes;

    @SerializedName("views")
    private int views;

    @SerializedName("user")
    private User user;

    @SerializedName("categories")
    private List<Category> categories;

    @SerializedName("comments")
    private List<Comment> comments;

    private boolean liked;
    public Artwork() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public Date getDateCreation() { return dateCreation; }
    public void setDateCreation(Date dateCreation) { this.dateCreation = dateCreation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<Category> getCategories() { return categories; }
    public void setCategories(List<Category> categories) { this.categories = categories; }

    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }

    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }

    public String getCategoriesString() {
        if (categories == null || categories.isEmpty()) {
            return "Без категории";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            if (category.getName() != null) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(category.getName());
            }
        }

        return sb.toString();
    }
    private Integer artworksCount;

    public Integer getArtworksCount() {
        return artworksCount;
    }

    public void setArtworksCount(Integer artworksCount) {
        this.artworksCount = artworksCount;
    }
    public boolean hasCategories() {
        return categories != null && !categories.isEmpty();
    }
}