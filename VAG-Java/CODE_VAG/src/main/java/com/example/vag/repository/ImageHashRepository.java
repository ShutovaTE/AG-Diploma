package com.example.vag.repository;

import com.example.vag.model.ImageHash;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageHashRepository extends JpaRepository<ImageHash, Long> {

    Optional<ImageHash> findByArtworkId(Long artworkId);

    Optional<ImageHash> findByMd5(String md5);

    @Query("SELECT h FROM ImageHash h WHERE h.active = true AND h.artwork.status = 'APPROVED'")
    List<ImageHash> findAllActiveHashes();

    @Query("SELECT h FROM ImageHash h WHERE h.active = true AND h.artwork.status = 'APPROVED' AND h.artwork.id <> :excludeArtworkId")
    List<ImageHash> findAllActiveHashesExcluding(@Param("excludeArtworkId") Long excludeArtworkId);
}