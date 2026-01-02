package com.example.vag.dto;

import lombok.Data;

public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String description; // ДОБАВЛЕНО
    private String role;
    private Integer artworksCount; // ДОБАВЛЕНО

    // Конструкторы
    public UserDTO() {}

    public UserDTO(Long id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    public UserDTO(Long id, String username, String email, String description) { // ДОБАВЛЕНО
        this.id = id;
        this.username = username;
        this.email = email;
        this.description = description;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // ДОБАВЛЕНО: Геттер и сеттер для description
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    // ДОБАВЛЕНО: Геттер и сеттер для artworksCount
    public Integer getArtworksCount() { return artworksCount; }
    public void setArtworksCount(Integer artworksCount) { this.artworksCount = artworksCount; }

    @Override
    public String toString() {
        return "UserDTO{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", description='" + description + '\'' +
                ", role='" + role + '\'' +
                ", artworksCount=" + artworksCount +
                '}';
    }
}