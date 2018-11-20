package com.virex.admclient.db.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Описание таблицы Анкета
 */
@Entity
public class Anketa {
    @PrimaryKey(autoGenerate = true)
    public int _id;

    public String sex;
    public String name;
    public String hobby;
    public String homepage;
    public String city;
    public String login;
    public String about;
    public String education;
    public String id;
    public String date;
    public String email;
    public String day;
    public String icq;
}
