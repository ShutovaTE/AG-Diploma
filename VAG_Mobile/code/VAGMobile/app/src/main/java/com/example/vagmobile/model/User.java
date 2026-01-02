package com.example.vagmobile.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class User implements Serializable {
    @SerializedName("id")
    private Long id;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("role")
    private Object role;

    @SerializedName("description") // ДОБАВЛЕНО
    private String description;

    private Integer artworksCount;

    public Integer getArtworksCount() {
        return artworksCount;
    }

    public void setArtworksCount(Integer artworksCount) {
        this.artworksCount = artworksCount;
    }

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Object getRole() { return role; }
    public void setRole(Object role) { this.role = role; }

    // ДОБАВЛЕНО: Геттер и сеттер для description
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}