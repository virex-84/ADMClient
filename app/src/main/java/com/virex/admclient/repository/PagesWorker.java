package com.virex.admclient.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import android.text.Html;

import com.virex.admclient.App;
import com.virex.admclient.Utils;
import com.virex.admclient.db.database.AppDataBase;
import com.virex.admclient.db.entity.Page;
import com.virex.admclient.db.entity.Topic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;

import androidx.work.Worker;
import androidx.work.WorkerParameters;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * "Воркер" для загрузки постов в БД
 */
public class PagesWorker extends Worker {

    public static final String EXTRA_FORUMID = "forumid";
    public static final String EXTRA_TOPICID = "topicid";
    public static final String EXTRA_PAGEID = "pageid";

    public static final String EXTRA_ACTION = "action";
    static final int ACTION_LOAD_FROM_NETWORK = 1;
    static final int ACTION_CHANGE_BOOKMARK = 2;

    public static final int ACTION_LOAD_BOOKMARKED_TOPICS_FROM_NETWORK = 4;


    private AppDataBase database = AppDataBase.getAppDatabase(getApplicationContext());

    public PagesWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        int forumID=getInputData().getInt(EXTRA_FORUMID,-1);
        int topicID=getInputData().getInt(EXTRA_TOPICID,-1);
        int pageID=getInputData().getInt(EXTRA_PAGEID,-1);

        int action=getInputData().getInt(EXTRA_ACTION,-1);
        switch(action){
            case ACTION_LOAD_FROM_NETWORK:
                loadFromNetwork(forumID, topicID);
                break;
            case ACTION_CHANGE_BOOKMARK:
                changePageBookmark(forumID,topicID,pageID);
                break;
            case ACTION_LOAD_BOOKMARKED_TOPICS_FROM_NETWORK:
                loadBookmarkedTopicsFromNetwork();
                break;
        }

        return Result.success();
    }

    private int loadFromNetwork(int forumID, int topicID){
        int loadedCount=0;
        boolean isError=true;
        try {
            int count = database.pageDao().countByForumAndTopic(forumID,topicID);
            //Response<ResponseBody> result=App.getPageApi().getPages(topicID,forumID,count,-1).execute();
            Response<ResponseBody> result=App.getPageApi().getPages2(topicID,forumID,count,-1).execute();

            if (result.isSuccessful()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(result.body().byteStream(), "windows-1251"));
                String line;
                int cnt=0;

                while ((line = br.readLine()) != null) {

                    //если в первой строке - количество постов, то грузим без ошибки
                    if (line.contains("Allcount=") && cnt==0){
                        isError=false;

                        if (line.contains("state=closed")){
                            database.topicDao().setState(forumID,topicID, "closed");
                        }

                    //} else if (line.contains("ERROR=")){

                    //единственное что есть в каждом посте на дельфимастер - <font class=date>
                    } else if (line.contains("font class=date") && !isError) {
                        Page page=new Page();
                        page.n = forumID;//сам апи сайта не выдает номер конференции
                        page.id = topicID;
                        page.content=line;
                        page.num=count;

                        page.parcedContent=Html.fromHtml(page.content).toString().toLowerCase();//для поиска удаляем все теги, оптимизация для поиска

                        String author=page.content.substring(0,page.content.indexOf("(<font class=date"));
                        page.author=Html.fromHtml(author).toString();//"выцепляем" автора

                        database.pageDao().insert(page);
                        loadedCount++;
                        count++;
                    }

                    //если воркера прерывали
                    if (isStopped()) return loadedCount;

                    try {
                        //каждые 10 постов - позволяем базе данных "отобразить" в recucleview
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
        return loadedCount;
    }

    //вставка или обновление
    private void upsertPage(int forumID, int topicID, Page page){

    }

    private void changePageBookmark(int forumID, int topicID, int pageId){
        database.pageDao().changeBookMark(forumID,topicID, pageId);

        //если хотя-бы один пост в топике помечен - помечаем весь топик
        int countBookmarked=database.pageDao().countBookmarkedByForumAndTopic(forumID, topicID);
        if (countBookmarked>0) database.topicDao().setBookMark(forumID, topicID,true);

        //обновляем количество помеченных постов для топика
        database.topicDao().setCountBookmarkedPages(forumID, topicID, countBookmarked);
    }

    private void loadBookmarkedTopicsFromNetwork() {

        int allCount=0;
        List<Topic> topics= database.topicDao().getBookmarkedTopics();
        for (Topic item: topics){
            int loadedCount=loadFromNetwork(item.n,item.id);
            if (loadedCount>0) {
                //добавляем уведомление о не прочитанном топике
                Utils.sendNotification(getApplicationContext(),item.n,item.id,String.format(Locale.ENGLISH,"%d новых соообщений в %s",loadedCount,item.title),loadedCount);
                allCount+=loadedCount;
            }
        }
        //устанавливаем количество общее количество непрочитанных сообщений
        Utils.setBadge(getApplicationContext(),allCount);
    }

}
