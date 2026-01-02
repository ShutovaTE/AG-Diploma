package com.example.vagmobile.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "categories")
public class CategoryEntity {
    @PrimaryKey
    private Long id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "approved_artworks_count")
    private Long approvedArtworksCount;

    @ColumnInfo(name = "last_updated")
    private Long lastUpdated;

    public CategoryEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getApprovedArtworksCount() { return approvedArtworksCount; }
    public void setApprovedArtworksCount(Long approvedArtworksCount) { this.approvedArtworksCount = approvedArtworksCount; }

    public Long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Long lastUpdated) { this.lastUpdated = lastUpdated; }
}