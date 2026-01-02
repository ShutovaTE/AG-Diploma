package com.example.vag.service;

import com.example.vag.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

public interface CategoryService {
    Category save(Category category);
    Optional<Category> findById(Long id);
    List<Category> findAll();
    Page<Category> findAll(Pageable pageable);
    List<Category> findAllById(List<Long> ids);
    void delete(Category category);
    Category update(Category category);
    void deleteById(Long id);
    Category update(Long id, @Valid Category category);
    List<Category> findAllByIds(List<Long> ids);

    Optional<Category> findByName(String name);
}