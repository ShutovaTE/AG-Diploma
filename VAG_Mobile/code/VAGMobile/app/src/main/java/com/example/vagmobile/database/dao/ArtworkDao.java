package com.example.vagmobile.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.vagmobile.database.entity.ArtworkEntity;
import java.util.List;

@Dao
public interface ArtworkDao {

    @Insert
    void insert(ArtworkEntity artwork);

    @Insert
    void insertAll(List<ArtworkEntity> artworks);

    @Update
    void update(ArtworkEntity artwork);

    @Query("SELECT * FROM artworks")
    List<ArtworkEntity> getAll();

    @Query("SELECT * FROM artworks WHERE id = :id")
    ArtworkEntity getById(Long id);

    @Query("SELECT * FROM artworks WHERE category_id = :categoryId")
    List<ArtworkEntity> getByCategory(Long categoryId);

    @Query("SELECT * FROM artworks WHERE user_id = :userId")
    List<ArtworkEntity> getByUser(Long userId);

    @Query("DELETE FROM artworks")
    void deleteAll();

    @Query("DELETE FROM artworks WHERE id = :id")
    void deleteById(Long id);
}