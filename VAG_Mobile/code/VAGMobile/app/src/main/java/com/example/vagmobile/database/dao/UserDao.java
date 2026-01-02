package com.example.vagmobile.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.vagmobile.database.entity.UserEntity;

public interface UserDao {

    @Insert
    void insert(UserEntity user);

    @Update
    void update(UserEntity user);

    @Query("SELECT * FROM users WHERE id = :id")
    UserEntity getById(Long id);

    @Query("SELECT * FROM users WHERE username = :username")
    UserEntity getByUsername(String username);

    @Query("DELETE FROM users")
    void deleteAll();
}