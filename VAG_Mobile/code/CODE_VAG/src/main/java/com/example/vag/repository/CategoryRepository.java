package com.example.vag.repository;

import com.example.vag.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Добавьте этот метод для поиска по имени
    Optional<Category> findByName(String name);

    // Метод для поиска категорий по списку ID
    @Query("SELECT c FROM Category c WHERE c.id IN :ids")
    List<Category> findAllByIds(@Param("ids") List<Long> ids);

    // Существующие методы
    List<Category> findAllByOrderByName();

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.artworks WHERE c.id = :id")
    Category findByIdWithArtworks(@Param("id") Long id);

    // Стандартный метод Spring Data JPA
    List<Category> findAllById(Iterable<Long> ids);
}