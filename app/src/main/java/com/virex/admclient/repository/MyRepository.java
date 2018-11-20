package com.virex.admclient.repository;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.Observer;
import android.arch.paging.DataSource;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


import com.virex.admclient.App;
import com.virex.admclient.Utils;
import com.virex.admclient.db.database.AppDataBase;
import com.virex.admclient.db.entity.Forum;
import com.virex.admclient.db.entity.Page;
import com.virex.admclient.db.entity.Topic;
import com.virex.admclient.network.PostBody;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.virex.admclient.Utils.URLEncodeString;

/**
 * Репозиторий
 * Реализация работы с базой данных и сетью
 */
public class MyRepository {

    private AppDataBase database;

    //фильтрованный список постов
    private MediatorLiveData<PagedList<Page>> filteredPages=new MediatorLiveData<>();
    private LiveData<PagedList<Page>> pages;

    //фильтрованный список тем
    private MediatorLiveData<PagedList<Topic>> filteredTopics=new MediatorLiveData<>();
    private LiveData<PagedList<Topic>> topics;


    public MyRepository(Context context) {
        database = AppDataBase.getAppDatabase(context.getApplicationContext());
    }

    public void loadForumNetwork() {
        OneTimeWorkRequest simpleRequest = new OneTimeWorkRequest.Builder(ForumsWorker.class).build();
        WorkManager.getInstance().beginUniqueWork("loadForumNetwork",ExistingWorkPolicy.REPLACE,simpleRequest).enqueue();
    }

    public LiveData<Forum> getForum(int forumID) {
        return database.forumDao().getForum(forumID);
    }

    public LiveData<PagedList<Forum>> getForums(){
        LiveData<PagedList<Forum>> pagedListLiveData;

        PagedList.Config config = new PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setPageSize(10)
                .build();

        pagedListLiveData = new LivePagedListBuilder<>(database.forumDao().forumsDataSource(), config)
                .setFetchExecutor(Executors.newSingleThreadExecutor())
                .setBoundaryCallback(new PagedList.BoundaryCallback<Forum>() {
                    @Override
                    public void onZeroItemsLoaded() {
                        super.onZeroItemsLoaded();
                        //база пустая
                        loadForumNetwork();
                    }

                    @Override
                    public void onItemAtFrontLoaded(@NonNull Forum itemAtFront) {
                        super.onItemAtFrontLoaded(itemAtFront);
                    }

                    @Override
                    public void onItemAtEndLoaded(@NonNull Forum itemAtEnd) {
                        super.onItemAtEndLoaded(itemAtEnd);
                    }
                })
                .build();

        return  pagedListLiveData;
    }

    //--------------------------------------------------------------------------
    private LiveData<PagedList<Topic>> createFilteredTopics(final int forumID, String title, boolean isOnlyBookMark) {
        LiveData<PagedList<Topic>> PagedListLiveData=null;

        PagedList.Config config = new PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setPageSize(10)
                .build();

        DataSource.Factory<Integer, Topic> convert =database.topicDao().topicsDataSourceFiltered(forumID, title,isOnlyBookMark);

        PagedListLiveData = new LivePagedListBuilder<>(convert, config)
                .setFetchExecutor(Executors.newSingleThreadExecutor())
                .setBoundaryCallback(new PagedList.BoundaryCallback<Topic>() {
                    @Override
                    public void onZeroItemsLoaded() {
                        super.onZeroItemsLoaded();
                        //база пустая - грузим из сети
                        loadTopicNetwork(forumID);
                    }
                })
                .build();

        return  PagedListLiveData;
    }

    public LiveData<PagedList<Topic>> getTopicsFilteredList(int forumID, String title, boolean isOnlyBookMark){
        setFilterTopicsList(forumID, title, isOnlyBookMark);
        return filteredTopics;
    }


    public void setFilterTopicsList(int forumID, final String title, boolean isOnlyBookMark){
        filteredTopics.removeSource(topics);
        topics=createFilteredTopics(forumID, title, isOnlyBookMark);

        filteredTopics.addSource(topics, new Observer<PagedList<Topic>>() {
            @Override
            public void onChanged(@Nullable PagedList<Topic> Topics) {
                filteredTopics.setValue(Topics);
            }
        });
    }

    //пометка о том что ветка прочитана
    public void setReadTopic(int forumID, int topicID, int count){
        Data data = new Data.Builder()
                .putInt(TopicsWorker.EXTRA_ACTION, TopicsWorker.ACTION_SET_READ_TOPIC)
                .putInt(TopicsWorker.EXTRA_FORUMID, forumID)
                .putInt(TopicsWorker.EXTRA_TOPICID, topicID)
                .putInt(TopicsWorker.EXTRA_COUNT, count)
                .build();
        OneTimeWorkRequest simpleRequest = new OneTimeWorkRequest.Builder(TopicsWorker.class)
                .setInputData(data)
                .build();
        WorkManager.getInstance().beginUniqueWork("setReadTopic",ExistingWorkPolicy.APPEND,simpleRequest).enqueue();
    }

    public void loadTopicNetwork(int forumID) {
        Data data = new Data.Builder()
                .putInt(TopicsWorker.EXTRA_ACTION, TopicsWorker.ACTION_LOAD_FROM_NETWORK)
                .putInt(TopicsWorker.EXTRA_FORUMID, forumID)
                .build();
        OneTimeWorkRequest simpleRequest = new OneTimeWorkRequest.Builder(TopicsWorker.class)
                .setInputData(data)
                //.addTag("simple_work")
                .build();
        WorkManager.getInstance().beginUniqueWork("loadTopicNetwork",ExistingWorkPolicy.REPLACE,simpleRequest).enqueue();
    }

    public LiveData<Topic> getTopic(int forumID, int topicID) {
        return database.topicDao().getTopic(forumID,topicID);
    }

    //пометить/снять флаг "вкладка"
    public void changeTopicBookmark(Topic topic){
        Data data = new Data.Builder()
                .putInt(PagesWorker.EXTRA_ACTION, TopicsWorker.ACTION_CHANGE_BOOKMARK)
                .putInt(PagesWorker.EXTRA_FORUMID, topic.n)
                .putInt(PagesWorker.EXTRA_TOPICID, topic.id)
                .build();
        OneTimeWorkRequest simpleRequest = new OneTimeWorkRequest.Builder(TopicsWorker.class)
                .setInputData(data)
                .build();
        //APPEND - нужно дождаться предыдущей смены флага "в избранном"
        WorkManager.getInstance().beginUniqueWork("changeTopicBookmark",ExistingWorkPolicy.APPEND,simpleRequest).enqueue();
    }

    //--------------------------------------------------------------------------

    public int countBookmarkedByForumAndTopic(int forum_id, int topic_id){
        return database.pageDao().countBookmarkedByForumAndTopic(forum_id,topic_id);
    }

    private LiveData<PagedList<Page>> createFilteredPages(final int forumID, final int topicID, String text, boolean isOnlyBookMark) {
        LiveData<PagedList<Page>> pagedListLiveData=null;

        PagedList.Config config = new PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setPageSize(10)
                .build();

        DataSource.Factory<Integer, Page> convert =database.pageDao().pagesDataSourceFiltered(forumID, topicID, text, isOnlyBookMark);


        /* не понадобилось - конвертируем при вставке в базу
        переменные можно обнулить, но количество изменять нельзя (mapByPage)
        convert=convert.map(new Function<Page, Page>() {
            @Override
            public Page apply(Page input) {
                input.parcedContent=Html.fromHtml(input.content).toString();
                return input;
            }
        });
        */

        pagedListLiveData = new LivePagedListBuilder<>(convert, config)
                .setFetchExecutor(Executors.newSingleThreadExecutor())
                .setBoundaryCallback(new PagedList.BoundaryCallback<Page>() {
                    @Override
                    public void onZeroItemsLoaded() {
                        super.onZeroItemsLoaded();
                        //база пустая - грузим из сети
                        loadPagesNetwork(forumID, topicID);
                    }
                })
                .build();

        return  pagedListLiveData;
    }

    public LiveData<PagedList<Page>> getPagesFilteredList(int forumID, int topicID, String text, boolean isOnlyBookMark){
        setFilterPagesList(forumID, topicID, text, isOnlyBookMark);
        return filteredPages;
    }


    //при установке фильтра - "переподписываемся" к новым данным
    public void setFilterPagesList(int forumID, int topicID, final String text, boolean isOnlyBookMark){

        filteredPages.removeSource(pages);
        pages=createFilteredPages(forumID, topicID, text, isOnlyBookMark);

        filteredPages.addSource(pages, new Observer<PagedList<Page>>() {
            @Override
            public void onChanged(@Nullable PagedList<Page> pages) {
                filteredPages.setValue(pages);
            }
        });
    }

    //добавление топика
    public void addTopicNetwork(final int forumID, String login, String password, String email, String signature, String title, String text, final PostBody.OnPostCallback onPostCallback) {
        login=URLEncodeString(login);
        password=URLEncodeString(password);
        email=URLEncodeString(email);
        signature=URLEncodeString(signature);
        title=URLEncodeString(title);
        text=URLEncodeString(text);
        String add=URLEncodeString("Добавить");//"%C4%EE%E1%E0%E2%E8%F2%FC"

        App.getTopicApi().addTopic(String.valueOf(forumID),login,password,email,signature,title,text,add, "1").enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(response.body().byteStream(), "windows-1251"));
                        String line;

                        //в теге pre обычно есть сообщение об ошибке
                        while ((line = br.readLine()) != null) {
                            String pre = Utils.extractHTMLTag("pre",line);
                            if (!TextUtils.isEmpty(pre)){
                                if (onPostCallback!=null) {
                                    onPostCallback.onError(pre);
                                    return;
                                }
                            }

                            //а при ошибочном посте (неверный логин/пароль) ошибку приходится детектировать из тега title
                            String title = Utils.extractHTMLTag("title",line);
                            if (!TextUtils.isEmpty(title)){
                                if (title.toLowerCase().contains("Мастера DELPHI | Пароль?".toLowerCase())) {
                                    if (onPostCallback != null) {
                                        onPostCallback.onError("Не корректный пароль");
                                        return;
                                    }
                                }
                            }
                        }
                        if (onPostCallback!=null) {
                            onPostCallback.onSuccess(null);
                        }
                    } catch (IOException e) {
                        if (onPostCallback!=null) {
                            onPostCallback.onError(e.getMessage());
                            return;
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (onPostCallback!=null) {
                    onPostCallback.onError(t.getMessage());
                    return;
                }
            }
        });
    }

    //добавление поста
    public void addPostNetwork(final int forumID, final int topicID, String login, String password, String email, String signature, String text, final PostBody.OnPostCallback onPostCallback) {
        login=URLEncodeString(login);
        password=URLEncodeString(password);
        email=URLEncodeString(email);
        signature=URLEncodeString(signature);
        text=URLEncodeString(text);
        String add2=URLEncodeString("Добавить");//"%C4%EE%E1%E0%E2%E8%F2%FC"

        App.getPageApi().addPost(String.valueOf(forumID),String.valueOf(topicID),login,password,email,signature,text,add2, "1").enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(response.body().byteStream(), "windows-1251"));
                        String line;
                        while ((line = br.readLine()) != null) {

                            //в теге pre обычно есть сообщение об ошибке
                            String pre = Utils.extractHTMLTag("pre",line);
                            if (!TextUtils.isEmpty(pre)){
                                if (onPostCallback!=null) {
                                    onPostCallback.onError(pre);
                                    return;
                                }
                            }

                            //а при ошибочном посте (неверный логин/пароль) ошибку приходится детектировать из тега title
                            String title = Utils.extractHTMLTag("title",line);
                            if (!TextUtils.isEmpty(title)){
                                if (title.toLowerCase().contains("Мастера DELPHI | Пароль?".toLowerCase())) {
                                    if (onPostCallback != null) {
                                        onPostCallback.onError("Не корректный пароль");
                                        return;
                                    }
                                }
                            }
                        }
                        //ошибок нет
                        if (onPostCallback!=null) {
                            onPostCallback.onSuccess(null);
                        }
                    } catch (IOException e) {
                        if (onPostCallback!=null) {
                            onPostCallback.onError(e.getMessage());
                            return;
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (onPostCallback!=null) {
                    onPostCallback.onError(t.getMessage());
                    return;
                }
            }
        });

    }

    //загрузка постов в базу
    public void loadPagesNetwork(int forumID, int topicID) {
        Data data = new Data.Builder()
                .putInt(PagesWorker.EXTRA_ACTION, PagesWorker.ACTION_LOAD_FROM_NETWORK)
                .putInt(PagesWorker.EXTRA_FORUMID, forumID)
                .putInt(PagesWorker.EXTRA_TOPICID, topicID)
                .build();
        OneTimeWorkRequest simpleRequest = new OneTimeWorkRequest.Builder(PagesWorker.class)
                .setInputData(data)
                .build();
        //REPLACE - можно прервать загрузку
        WorkManager.getInstance().beginUniqueWork("loadPagesNetwork",ExistingWorkPolicy.REPLACE,simpleRequest).enqueue();
    }

    //пометить/снять флаг "вкладка"
    public void changePageBookmark(final Page page){
        Data data = new Data.Builder()
                .putInt(PagesWorker.EXTRA_ACTION, PagesWorker.ACTION_CHANGE_BOOKMARK)
                .putInt(PagesWorker.EXTRA_FORUMID, page.n)
                .putInt(PagesWorker.EXTRA_TOPICID, page.id)
                .putInt(PagesWorker.EXTRA_PAGEID, page._id)
                .build();
        OneTimeWorkRequest simpleRequest = new OneTimeWorkRequest.Builder(PagesWorker.class)
                .setInputData(data)
                .build();
        //APPEND - нужно дождаться предыдущей смены флага "в избранном"
        WorkManager.getInstance().beginUniqueWork("changePageBookmark",ExistingWorkPolicy.APPEND,simpleRequest).enqueue();
    }


    public void clearDataBase(){
        WorkManager.getInstance().cancelAllWork().addListener(new Runnable() {
            @Override
            public void run() {

            }
        }, new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        database.clearAllTables();
                    }
                });
            }
        });

    }

}
