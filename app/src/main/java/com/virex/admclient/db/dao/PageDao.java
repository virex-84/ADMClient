package com.virex.admclient.db.dao;

import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.virex.admclient.db.entity.Page;

/**
 * Интерфейс работы с "таблицей" Посты
 */
@Dao
public interface PageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Page... page);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void update(Page... page);

    @Transaction
    @Query("Select COUNT(_id) FROM page WHERE forum_id==:forum_id and topic_id==:topic_id")
    int countByForumAndTopic(int forum_id, int topic_id);

    //фильтруем, если флаг isOnlyBookMark=false то ищем всё, иначе - ищем посты с isBookMark=true
    @Transaction
    @Query("SELECT * FROM page WHERE forum_id==:forum_id and topic_id==:topic_id and LOWER(parcedContent) LIKE LOWER('%' || :text || '%') AND cast(isBookMark as varchar(1)) like (CASE :isOnlyBookMark WHEN 0 THEN '%' WHEN 1 THEN '1' END) ORDER BY _id ASC")
    DataSource.Factory<Integer, Page> pagesDataSourceFiltered(int forum_id, int topic_id, String text, boolean isOnlyBookMark);

    @Query("UPDATE page SET isBookMark = not isBookMark WHERE _id == :page_id and forum_id==:forum_id and topic_id==:topic_id")
    int changeBookMark(int forum_id, int topic_id, int page_id);

    @Transaction
    @Query("Select COUNT(_id) FROM page WHERE forum_id==:forum_id and topic_id==:topic_id and isBookMark = 1")
    int countBookmarkedByForumAndTopic(int forum_id, int topic_id);

    @Query("UPDATE page SET isBookMark = 0 WHERE forum_id==:forum_id and topic_id==:topic_id")
    int clearBookMark(int forum_id, int topic_id);

    /*
    @Transaction
    @Query("Select COUNT(_id) FROM page WHERE forum_id==:forum_id and topic_id==:topic_id and isBookMark = 1")
    LiveData<Integer> countBookmarkedByForumAndTopic(int forum_id, int topic_id);
*/

    @Transaction
    @Query("SELECT * FROM page WHERE forum_id==:forum_id and topic_id==:topic_id and num=:id")
    LiveData<Page> getPage(int forum_id, int topic_id, int id);

}