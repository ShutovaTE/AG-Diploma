package com.example.vag.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "image_hashes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"artwork_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class ImageHash {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id", nullable = false)
    private Artwork artwork;

    @Column(nullable = false, length = 64)
    private String pHash;

    @Column(nullable = false, length = 32)
    private String md5;

    @Column(nullable = false)
    private boolean active = true;

    public ImageHash(Artwork artwork, String pHash, String md5) {
        this.artwork = artwork;
        this.pHash = pHash;
        this.md5 = md5;
    }
}