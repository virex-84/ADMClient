package com.virex.admclient.db.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.virex.admclient.db.dao.AnketaDao;
import com.virex.admclient.db.dao.ForumDao;
import com.virex.admclient.db.dao.PageDao;
import com.virex.admclient.db.dao.TopicDao;
import com.virex.admclient.db.entity.Anketa;
import com.virex.admclient.db.entity.Forum;
import com.virex.admclient.db.entity.Page;
import com.virex.admclient.db.entity.Topic;

/**
 * База данных Room
 */
@Database(entities = {Forum.class, Topic.class, Page.class, Anketa.class}, version = 1, exportSchema = false)
public abstract class AppDataBase extends RoomDatabase {
    private static AppDataBase instance;

    private static final String ADMCLIENT_DB = "ADMClient.db";

    public abstract ForumDao forumDao();
    public abstract TopicDao topicDao();
    public abstract PageDao pageDao();
    public abstract AnketaDao anketaDao();

    public static AppDataBase getAppDatabase(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDataBase.class,
                    ADMCLIENT_DB)

                    //разрешить работать с БД в главном потоке - не нужно (у нас работает через WorkManager)
                    //.allowMainThreadQueries()

                    //миграция БД - понадобится позже
                    //.addMigrations(MIGRATION_1_2)
                    //.addMigrations(MIGRATION_2_3)

                    //убиваем всё если схема данных не совпадает
                    .fallbackToDestructiveMigration()

                    .build();
        }
        return instance;
    }

    /*
    //пример миграции
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE topic ADD COLUMN isBookMark INTEGER DEFAULT 0 NOT NULL");
        }
    };

    //пример миграции
    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE topic ADD COLUMN countBookmarkedPages INTEGER DEFAULT 0 NOT NULL");
        }
    };
    */
}
