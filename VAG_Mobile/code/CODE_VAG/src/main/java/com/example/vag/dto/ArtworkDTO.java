package com.example.vag.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ArtworkDTO {
    private Long id;
    private String title;
    private String description;
    private String imagePath;
    private String status;
    private int likes;
    private int views;
    private LocalDate dateCreation; // Используем LocalDate для Artwork
    private UserDTO user;
    private List<CategoryDTO> categories;
    private List<CommentDTO> comments;
    private Boolean liked; // Используем Boolean вместо boolean

    // Конструкторы
    public ArtworkDTO() {}

    public ArtworkDTO(Long id, String title, String description, String imagePath,
                      String status, int likes, int views, LocalDate dateCreation) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imagePath = imagePath;
        this.status = status;
        this.likes = likes;
        this.views = views;
        this.dateCreation = dateCreation;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }

    public LocalDate getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDate dateCreation) { this.dateCreation = dateCreation; }

    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }

    public List<CategoryDTO> getCategories() { return categories; }
    public void setCategories(List<CategoryDTO> categories) { this.categories = categories; }

    public List<CommentDTO> getComments() { return comments; }
    public void setComments(List<CommentDTO> comments) { this.comments = comments; }

    public Boolean getLiked() { return liked; }
    public void setLiked(Boolean liked) { this.liked = liked; }

    // Для обратной совместимости
    public boolean isLiked() {
        return liked != null ? liked : false;
    }
}