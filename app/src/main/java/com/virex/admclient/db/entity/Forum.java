package com.virex.admclient.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

/**
 * Описание таблицы Форум
 */
@Entity
public class Forum {
    @PrimaryKey
    public int n;
    public String title;
    public String dsc;

    //сравнение для адаптера PagedList (recucleview)
    public static final DiffUtil.ItemCallback<Forum> DIFF_CALLBACK = new DiffUtil.ItemCallback<Forum>(){

        @Override
        public boolean areItemsTheSame(@NonNull Forum oldItem, @NonNull Forum newItem) {
            return oldItem.n == newItem.n;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Forum oldItem, @NonNull Forum newItem) {
            //return oldItem.title.equals(newItem.title);
            return oldItem.title.equals(newItem.title) && oldItem.dsc.equals(newItem.dsc);
        }
    };
}
