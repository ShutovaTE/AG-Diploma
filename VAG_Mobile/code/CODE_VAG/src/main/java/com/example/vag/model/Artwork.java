package com.example.vag.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "artworks")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"exhibitions", "user", "artworkLikes", "comments"})
public class Artwork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty(message = "Название не может быть пустым")
    @Column(nullable = false)
    private String title;

    @NotEmpty(message = "Описание не может быть пустым")
    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "artwork_category",
            joinColumns = @JoinColumn(name = "artwork_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @JsonIgnore
    private Set<Category> categories = new HashSet<>();

    @Transient
    private List<Long> categoryIds;

    @Column(nullable = false)
    private String imagePath;

    @Transient
    private MultipartFile imageFile;

    @Column(nullable = false)
    private LocalDate dateCreation;

    @ManyToMany(mappedBy = "artworks", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Exhibition> exhibitions = new HashSet<>();

    // ИЗМЕНЕНО: EAGER загрузка для веб-шаблонов
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Убрали @JsonIgnore для веб-шаблонов

    @Column(nullable = false)
    private String status; // PENDING, APPROVED, REJECTED

    @Column(nullable = false)
    private int likes = 0;

    @Column(nullable = false)
    private int views = 0;

    @OneToMany(mappedBy = "artwork", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Like> artworkLikes;

    @OneToMany(mappedBy = "artwork", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Comment> comments;

    @Transient
    private Boolean liked;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDate.now();
    }

    public enum ArtworkStatus {
        PENDING, APPROVED, REJECTED
    }

    // Безопасный toString без ленивых коллекций
    @Override
    public String toString() {
        return "Artwork{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + (description != null ? description.substring(0, Math.min(50, description.length())) : "") + '\'' +
                ", status='" + status + '\'' +
                ", likes=" + likes +
                ", views=" + views +
                ", dateCreation=" + dateCreation +
                ", user=" + (user != null ? user.getUsername() : "null") +
                '}';
    }
}