package com.example.vagmobile.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;

public class Comment implements Serializable {
    @SerializedName("id")
    private Long id;

    @SerializedName("content")
    private String content;

    @SerializedName("dateCreated")
    private Date dateCreated;

    @SerializedName("user")
    private User user;

    @SerializedName("artwork")
    private Artwork artwork;

    public Comment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Artwork getArtwork() { return artwork; }
    public void setArtwork(Artwork artwork) { this.artwork = artwork; }
}