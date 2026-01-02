package com.example.vag.dto;

public class CategoryDTO {
    private Long id;
    private String name;
    private String description;
    private Long approvedArtworksCount = 0L;

    // Конструкторы
    public CategoryDTO() {}

    public CategoryDTO(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getApprovedArtworksCount() {
        return approvedArtworksCount != null ? approvedArtworksCount : 0L;
    }

    public void setApprovedArtworksCount(Long approvedArtworksCount) {
        this.approvedArtworksCount = approvedArtworksCount != null ? approvedArtworksCount : 0L;
    }}