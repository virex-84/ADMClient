package com.virex.admclient.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.virex.admclient.db.entity.Anketa;

/**
 * Интерфейс работы с "таблицей" Анкета
 */
@Dao
public interface AnketaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Anketa... anketa);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void update(Anketa... anketa);
}