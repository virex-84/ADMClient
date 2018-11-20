package com.virex.admclient.repository;

import android.content.Context;
import android.support.annotation.NonNull;

import com.virex.admclient.App;
import com.virex.admclient.Utils;
import com.virex.admclient.db.database.AppDataBase;
import com.virex.admclient.db.entity.Topic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import androidx.work.Worker;
import androidx.work.WorkerParameters;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * "Воркер" для загрузки топиков в БД
 */
public class TopicsWorker extends Worker {

    static final String EXTRA_FORUMID = "forumid";
    static final String EXTRA_TOPICID = "topicid";
    static final String EXTRA_COUNT = "count";

    static final String EXTRA_ACTION = "action";
    static final int ACTION_LOAD_FROM_NETWORK = 1;
    static final int ACTION_SET_READ_TOPIC = 2;
    static final int ACTION_CHANGE_BOOKMARK = 3;

    private AppDataBase database = AppDataBase.getAppDatabase(getApplicationContext());

    public TopicsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        int forumID=getInputData().getInt(EXTRA_FORUMID,-1);
        int topicID=getInputData().getInt(EXTRA_TOPICID,-1);
        int count=getInputData().getInt(EXTRA_COUNT,-1);

        int action=getInputData().getInt(EXTRA_ACTION,-1);
        switch(action){
            case ACTION_LOAD_FROM_NETWORK:
                loadFromNetwork(forumID);
                break;
            case ACTION_SET_READ_TOPIC:
                setReadTopic(forumID,topicID, count);
                break;
            case ACTION_CHANGE_BOOKMARK:
                changeTopicBookmark(forumID,topicID);
                break;
        }

        return Result.SUCCESS;
    }

    private void loadFromNetwork(int forumID) {
        int lastmod = -1;

        int count = database.topicDao().countByForum(forumID);
        if (count > 0) lastmod = database.topicDao().lastmodByForum(forumID);

        //запрос на сервер и сохранение в базу
        try {
            //синхронный
            Response<ResponseBody> result=App.getTopicApi().getTopics(lastmod, forumID).execute();
            if (result.isSuccessful()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(result.body().byteStream(), "windows-1251"));
                String line;
                int cnt=0;
                while ((line = br.readLine()) != null) {
                    Topic topic = Utils.parceLineToObject(Topic.class, line);
                    upsertTopic(forumID, topic);//сам апи сайта не выдает номер конференции

                    //если воркера прерывали
                    if (isCancelled()) return ;

                    try {
                        //каждые 10 тем - позволяем базе данных "отобразить" в recucleview
                        if ((cnt % 10)==0) Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    cnt++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setReadTopic(int forumID, int topicID, int count) {
        final Topic findTopic = database.topicDao().findTopic(forumID, topicID);
        if (findTopic != null) {
            findTopic.isReaded = true;

            if (count>=findTopic.count) {
                findTopic.count=count;
                findTopic.lastcount=count;
            }
            database.topicDao().update(findTopic);
        }
    }

    //вставка или обновление
    private void upsertTopic(int forumID, Topic topic){
        if (topic==null) return;
        topic.n=forumID;
        Topic findTopic=database.topicDao().findTopic(topic.n, topic.id);
        if (findTopic==null){
            topic.lastcount=topic.count;
            if (topic.title!=null) topic.findTitle = topic.title.toLowerCase();
            if (topic.dsc!=null) topic.findDsc = topic.dsc.toLowerCase();
            database.topicDao().insert(topic);
        } else {
            if (topic.lastmod>findTopic.lastmod || topic.count>findTopic.count){
                topic.lastcount=findTopic.lastcount;
                topic.isReaded=false;
                //придется перезаписывать для поиска
                topic.findTitle = findTopic.findTitle;
                topic.findDsc = findTopic.findDsc;
                //сохраняем количество помеченных постов
                topic.countBookmarkedPages = findTopic.countBookmarkedPages;
                //пометка
                topic.isBookMark = findTopic.isBookMark;

                topic.state=findTopic.state;
                database.topicDao().insert(topic);
            }
        }
    }

    private void changeTopicBookmark(int forumID, int topicID){
        /*
          Если с ветки снимают закладку - то необходимо снять закладки во всех принадлежащих
          к ней постов
         */
        Topic findTopic=database.topicDao().findTopic(forumID, topicID);
        if (findTopic!=null){
          if (findTopic.isBookMark) {
              database.pageDao().clearBookMark(forumID,topicID);
              database.topicDao().setCountBookmarkedPages(forumID, topicID,0);
          }
        }
        database.topicDao().changeBookMark(forumID,topicID);
    }
}
