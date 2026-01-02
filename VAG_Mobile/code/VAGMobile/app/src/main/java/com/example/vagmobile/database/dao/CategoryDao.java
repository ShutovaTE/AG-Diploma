package com.example.vagmobile.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.vagmobile.database.entity.CategoryEntity;
import java.util.List;

@Dao
public interface CategoryDao {

    @Insert
    void insert(CategoryEntity category);

    @Insert
    void insertAll(List<CategoryEntity> categories);

    @Query("SELECT * FROM categories")
    List<CategoryEntity> getAll();

    @Query("SELECT * FROM categories WHERE id = :id")
    CategoryEntity getById(Long id);

    @Query("DELETE FROM categories")
    void deleteAll();
}