package com.example.vag.service;

import com.example.vag.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User save(User user);
    User register(User user);
    List<User> findAll();
    Page<User> findAll(Pageable pageable);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    void delete(User user);
    User update(User updatedUser);
    User authenticate(String username, String password);
    User getCurrentUser();
    List<User> findRandomArtists(int count);

    // ДОБАВЛЕНО: Метод для получения всех пользователей с количеством публикаций
    List<User> findAllWithArtworksCount();
}