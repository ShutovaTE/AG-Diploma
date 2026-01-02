package com.example.vag.repository;

import com.example.vag.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.role.name = 'ARTIST' ORDER BY FUNCTION('RAND')")
    List<User> findRandomArtists(@Param("count") int count);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.artworks WHERE u.role.name = 'ARTIST' ORDER BY SIZE(u.artworks) DESC")
    List<User> findAllArtistsWithArtworks();

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.artworks WHERE u.id = :id")
    Optional<User> findByIdWithArtworks(@Param("id") Long id);

}