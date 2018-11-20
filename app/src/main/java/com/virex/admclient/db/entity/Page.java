package com.virex.admclient.db.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

/**
 * Описание таблицы Пост
 */
@Entity(foreignKeys = @ForeignKey(entity = Topic.class,
        parentColumns = "id",
        childColumns = "topic_id"),
        indices = {@Index("topic_id")})
public class Page {
    @PrimaryKey(autoGenerate = true)
    public int _id;

    @ColumnInfo(name = "forum_id")
    public int n;

    @ColumnInfo(name = "topic_id")
    public int id;

    public String content;

    public boolean isBookMark; //

    public String parcedContent;
    public String author;

    public int num;

    //сравнение для адаптера PagedList (recucleview)
    public static final DiffUtil.ItemCallback<Page> DIFF_CALLBACK = new DiffUtil.ItemCallback<Page>() {

        @Override
        public boolean areItemsTheSame(@NonNull Page oldItem, @NonNull Page newItem) {
            return (oldItem._id == newItem._id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Page oldItem, @NonNull Page newItem) {
            return (oldItem.isBookMark==newItem.isBookMark);
        }

        @Nullable
        @Override
        public Object getChangePayload(@NonNull Page oldItem, @NonNull Page newItem) {
            return super.getChangePayload(oldItem, newItem);
        }
    };
}
