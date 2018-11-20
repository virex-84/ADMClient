package com.virex.admclient.db.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;

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
            return oldItem.equals(newItem);
        }
    };
}
