package com.example.vag.dto;

import java.time.LocalDateTime;

public class CommentDTO {
    private Long id;
    private String content;
    private LocalDateTime dateCreated; // Используем LocalDateTime для Comment
    private UserDTO user;

    // Конструкторы
    public CommentDTO() {}

    public CommentDTO(Long id, String content, LocalDateTime dateCreated) {
        this.id = id;
        this.content = content;
        this.dateCreated = dateCreated;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getDateCreated() { return dateCreated; }
    public void setDateCreated(LocalDateTime dateCreated) { this.dateCreated = dateCreated; }

    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }
}