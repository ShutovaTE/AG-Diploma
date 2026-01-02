package com.example.vagmobile.model;

import java.io.Serializable;

public class Category implements Serializable {
    private Long id;
    private String name;
    private String description;

    private Long approvedArtworksCount;

    public Category() {}

    public Category(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getApprovedArtworksCount() { return approvedArtworksCount; }
    public void setApprovedArtworksCount(Long approvedArtworksCount) { this.approvedArtworksCount = approvedArtworksCount; }

    @Override
    public String toString() { return name; }
}