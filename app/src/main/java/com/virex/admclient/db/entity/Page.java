package com.virex.admclient.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

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
