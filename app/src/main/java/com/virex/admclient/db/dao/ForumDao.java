package com.virex.admclient.db.dao;

import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

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