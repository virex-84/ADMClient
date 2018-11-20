package com.virex.admclient.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

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