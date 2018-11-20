package com.virex.admclient.db.dao;

import android.arch.lifecycle.LiveData;
import android.arch.paging.DataSource;
import android.arch.paging.PositionalDataSource;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import com.virex.admclient.db.entity.Forum;

/**
 * Интерфейс работы с "таблицей" Форумы
 */
@Dao
public interface ForumDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Forum... forum);

    @Update(onConflict = OnConflictStrategy.IGNORE)
    void update(Forum... forum);

    @Query("SELECT * FROM forum ORDER BY n ASC")
    DataSource.Factory<Integer, Forum> forumsDataSource();

    @Query("SELECT * FROM forum WHERE n = :forum_id")
    LiveData<Forum> getForum(int forum_id);
}