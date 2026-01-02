package com.example.vagmobile.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;
import com.example.vagmobile.database.dao.ArtworkDao;
import com.example.vagmobile.database.dao.CategoryDao;
import com.example.vagmobile.database.dao.UserDao;
import com.example.vagmobile.database.entity.ArtworkEntity;
import com.example.vagmobile.database.entity.CategoryEntity;
import com.example.vagmobile.database.entity.UserEntity;

@Database(
        entities = {ArtworkEntity.class, UserEntity.class, CategoryEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract ArtworkDao artworkDao();
    public abstract UserDao userDao();
    public abstract CategoryDao categoryDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "vagmobile.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}