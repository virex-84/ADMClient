package com.virex.admclient.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

/**
 * Описание таблицы Форум
 */
@Entity(foreignKeys = @ForeignKey(entity = Forum.class,
        parentColumns = "n",
        childColumns = "forum_id"),
        indices = {@Index("forum_id")})
public class Topic {

    @ColumnInfo(name = "forum_id")
    public int n;

    @PrimaryKey
    public int id;

    public String name;//автор

    public String title;//заголовок
    public String findTitle;//для поиска

    public String dsc;//описание
    public String findDsc;//для поиска


    public String answers;
    public String email;
    public int count;//кол-во


    public String date;
    public int lastmod;
    public String vd;
    public String loginid;

    //поля устанавливаемые пользователем - нужно добавить в com.virex.admclient.repository.TopicsWorker upsertTopic !
    public boolean isReaded;        //помечаем прочитана ли вся ветка
    public int lastcount;           //предыдущее кол-во
    public boolean isBookMark;      //признак "закладка"
    public int countBookmarkedPages;//количество помеченных топиков

    public String state="";

    //сравнение для адаптера PagedList (recucleview)
    public static final DiffUtil.ItemCallback<Topic> DIFF_CALLBACK = new DiffUtil.ItemCallback<Topic>(){

        //сравнивание разные ли это элементы
        //т.к. мы находимся в определенном форуме, то сравнивать по номеру форума - лишнее
        @Override
        public boolean areItemsTheSame(@NonNull Topic oldItem, @NonNull Topic newItem) {
            return (oldItem.id == newItem.id);
        }

        //сравнивание что изменилось в элементе
        //изменится может количество постов и флаг "прочитаная ветка"
        @Override
        public boolean areContentsTheSame(@NonNull Topic oldItem, @NonNull Topic newItem) {
            return  (oldItem.count == newItem.count) && (oldItem.isReaded == newItem.isReaded) && (oldItem.isBookMark==newItem.isBookMark);
        }
    };
}
