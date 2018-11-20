package com.virex.admclient.db.dao;

import android.arch.lifecycle.LiveData;
import android.arch.paging.DataSource;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.Update;

import com.virex.admclient.db.entity.Topic;

import java.util.ArrayList;
import java.util.List;

/**
 * Интерфейс работы с "таблицей" Топики
 */
@Dao
public interface TopicDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Topic... topic);

    @Transaction
    @Update(onConflict = OnConflictStrategy.REPLACE)
    void update(Topic... topic);

    @Transaction
    @Query("Select COUNT(id) FROM topic WHERE forum_id == :forum_id")
    int countByForum(int forum_id);

    @Transaction
    @Query("Select lastmod FROM topic WHERE forum_id == :forum_id ORDER BY lastmod DESC LIMIT 1")
    int lastmodByForum(int forum_id);

    @Transaction
    @Query("Select * FROM topic WHERE  forum_id == :forum_id AND id == :topic_id")
    Topic findTopic(int forum_id, int topic_id);

    @Transaction
    @Query("Select * FROM topic WHERE  forum_id == :forum_id AND id == :topic_id")
    LiveData<Topic> getTopic(int forum_id, int topic_id);

    @Transaction
    @Query("SELECT * FROM topic WHERE forum_id==:forum_id and (findTitle LIKE LOWER('%' || :text || '%') or findDsc LIKE LOWER('%' || :text || '%') ) AND cast(isBookMark as varchar(1)) like (CASE :isOnlyBookMark WHEN 0 THEN '%' WHEN 1 THEN '1' END) ORDER BY lastmod DESC" )
    DataSource.Factory<Integer, Topic> topicsDataSourceFiltered(int forum_id, String text, boolean isOnlyBookMark);


    @Query("UPDATE topic SET isBookMark = not isBookMark WHERE id == :topic_id and forum_id==:forum_id")
    int changeBookMark(int forum_id, int topic_id);

    @Query("UPDATE topic SET isBookMark = :value WHERE id == :topic_id and forum_id==:forum_id")
    int setBookMark(int forum_id, int topic_id, boolean value);

    @Query("UPDATE topic SET countBookmarkedPages = :value WHERE id == :topic_id and forum_id==:forum_id")
    int setCountBookmarkedPages(int forum_id, int topic_id, int value);

    @Transaction
    @Query("Select * FROM topic WHERE  isBookMark=1 ")
    List<Topic> getBookmarkedTopics();

    @Query("UPDATE topic SET state = :value WHERE id == :topic_id and forum_id==:forum_id")
    int setState(int forum_id, int topic_id, String value);
}