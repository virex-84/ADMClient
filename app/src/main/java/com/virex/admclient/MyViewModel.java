package com.virex.admclient;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.paging.PagedList;
import android.support.annotation.NonNull;

import com.virex.admclient.db.entity.Forum;
import com.virex.admclient.db.entity.Page;
import com.virex.admclient.db.entity.Topic;
import com.virex.admclient.network.PostBody;
import com.virex.admclient.repository.MyRepository;

/**
 * Модель данных
 */
public class MyViewModel extends AndroidViewModel {
    private MyRepository myRepository;

    public MyViewModel(@NonNull Application application) {
        super(application);
        myRepository=new MyRepository(application);
    }

    //--------------------------------------------------------------------------
    //список форумов
    LiveData<PagedList<Forum>> allForums(){
        return myRepository.getForums();
    }

    //обновить список форумов
    void refreshForums(){
        myRepository.loadForumNetwork();
    }

    //получить инфу о форуме
    LiveData<Forum> getForum(int forumID){
        return myRepository.getForum(forumID);
    }

    //--------------------------------------------------------------------------
    //список топиков(веток)
    LiveData<PagedList<Topic>> allTopicsListFiltered(int forumID, String title, boolean isOnlyBookMark, int limit){
        return myRepository.getTopicsFilteredList(forumID, title, isOnlyBookMark,limit);
    }

    void setFilterTopicsList(int forumID, String title, boolean isOnlyBookMark, int limit){
        myRepository.setFilterTopicsList(forumID, title, isOnlyBookMark, limit);
    }

    void refreshTopics(int forumID) {
        myRepository.loadTopicNetwork(forumID);
    }

    void setReadTopic(int forumID, int topicID, int count){
        myRepository.setReadTopic(forumID, topicID, count);
    }

    //пометить/снять флаг "вкладка"
    void changeTopicBookmark(Topic topic){
        myRepository.changeTopicBookmark(topic);
    }

    LiveData<Topic> getTopic(int forumID, int topicID){
        return myRepository.getTopic(forumID,topicID);
    }

    //добавить топик
    void addTopicNetwork(final int forumID, String login, String password, String email, String signature, String title, String text, PostBody.OnPostCallback onPostCallback) {
        myRepository.addTopicNetwork(forumID,login,password,email,signature, title,text,onPostCallback);
    }

    //--------------------------------------------------------------------------
    //фильтрованный список постов
    LiveData<PagedList<Page>> allPagesListFiltered(int forumID, int topicID, String text, boolean isOnlyBookMark){
        return myRepository.getPagesFilteredList(forumID, topicID, text, isOnlyBookMark);
    }

    void setFilterPagesList(int forumID, int topicID, String text, boolean isOnlyBookMark){
        myRepository.setFilterPagesList(forumID, topicID, text, isOnlyBookMark);
    }

    void refreshPages(int forumID, int topicID){
        myRepository.loadPagesNetwork(forumID,topicID);
    }

    //добавить пост
    void addPostNetwork(final int forumID, final int topicID, String login, String password, String email, String signature, String text, PostBody.OnPostCallback onPostCallback) {
        myRepository.addPostNetwork(forumID,topicID,login,password,email,signature,text,onPostCallback);
    }

    //пометить/снять флаг "вкладка"
    void changePageBookmark(final Page page){
        myRepository.changePageBookmark(page);
    }

    public int countBookmarkedByForumAndTopic(int forum_id, int topic_id){
        return myRepository.countBookmarkedByForumAndTopic(forum_id,topic_id);
    }

}
