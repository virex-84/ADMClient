package com.virex.admclient;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.paging.PagedList;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.recyclerview.widget.ConcatAdapter;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.WorkInfo;

import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.virex.admclient.db.entity.Forum;
import com.virex.admclient.db.entity.Topic;
import com.virex.admclient.network.PostBody;
import com.virex.admclient.ui.FooterAdapter;
import com.virex.admclient.ui.PostTopicDialog;
import com.virex.admclient.ui.TopicAdapter;

import static androidx.work.WorkInfo.State.*;
import static com.virex.admclient.repository.ForumsWorker.EXTRA_RESULT;

/**
 * Активность списка топиков в выделенном форуме
 */
public class TopicActivity extends BaseAppCompatActivity {

    private boolean isOnlyBookMark=false;
    private String filter="";
    int limit=-1;

    RecyclerView recyclerView;
    SwipeRefreshLayout swipeRefreshLayout;
    TopicAdapter topicAdapter;
    FooterAdapter footerAdapter;
    MyViewModel model;
    int forumID;

    FloatingActionButton fab;
    PostTopicDialog postTopicDialog;

    //флаг "только что запостили"
    boolean postedNow=false;


    private static final String BUNDLE_RECYCLER_LAYOUT = "recycler_layout";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view);

        showSearchMenuItem=true;
        showBookmarkMenuItem=true;

        forumID = getIntent().getIntExtra("n",-1);

        model = ViewModelProviders.of(this).get(MyViewModel.class);

        recyclerView = findViewById(R.id.tv_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        swipeRefreshLayout = findViewById(R.id.swipe_container);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(false);
                model.refreshTopics(forumID);
            }
        });

        topicAdapter = new TopicAdapter(Topic.DIFF_CALLBACK, new TopicAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, Topic topic) {
                Intent intent = new Intent(getBaseContext(), PageActivity.class);
                intent.putExtra("n",topic.n);
                intent.putExtra("id",topic.id);
                startActivity(intent);
            }

            @Override
            public void onBookMarkClick(final Topic topic, int position) {
                /*
                в данном случае Topic уже устаревший (в т.ч. в countBookmarkedPages - не актуальное значение)
                самая актуальная информация - в адаптере
                 */
                Topic current=topic;
                if (topicAdapter.getCurrentList()!=null) {
                    for (Topic item : topicAdapter.getCurrentList()) {
                        if (item != null) {
                            if ((item.n == topic.n) && (item.id == topic.id)) {
                                current = item;
                                break;
                            }
                        }
                    }
                }
                //если снимают флаг "избранное" у топика, то автоматически снимается флаг и у его постов
                //поэтому спрашиваем
                if (current.isBookMark) {
                    if (current.countBookmarkedPages>0){
                        AlertDialog.Builder dialog = new AlertDialog.Builder(recyclerView.getContext() /*getSupportActionBar().getThemedContext()*/);
                        dialog.setCancelable(false);
                        dialog.setMessage(String.format(getString(R.string.setunbookmark_dialog_message),current.countBookmarkedPages));
                        dialog.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                model.changeTopicBookmark(topic);
                            }
                        }).setNegativeButton(getString(R.string.cancel),null).show();
                    } else
                        model.changeTopicBookmark(topic);
                } else
                    model.changeTopicBookmark(topic);
            }
        });
        topicAdapter.setColors(getResources().getColor(R.color.white),colorAccent,colorAccent);

        footerAdapter = new FooterAdapter(new FooterAdapter.OnItemClickListener() {
            @Override
            public void onReloadClick() {
                swipeRefreshLayout.setRefreshing(false);
                model.refreshTopics(forumID);
            }
        });

        ConcatAdapter concatAdapter=new ConcatAdapter(topicAdapter,footerAdapter);
        recyclerView.setAdapter(concatAdapter);

        String pref_topics_limit=options.getString("pref_topics_limit","-1");
        try {
            limit = Integer.parseInt(pref_topics_limit);
        } catch(Exception ignore){
        }
        model.allTopicsListFiltered(forumID,filter,isOnlyBookMark,limit).observe(this, new Observer<PagedList<Topic>>() {
            @Override
            public void onChanged(@Nullable PagedList<Topic> topics) {
                topicAdapter.submitList(topics);

                scrolltoFirst();

                //восстанавливаем позицию списка
                if (savedInstanceState != null) {
                    Parcelable state = savedInstanceState.getParcelable(BUNDLE_RECYCLER_LAYOUT);
                    recyclerView.getLayoutManager().onRestoreInstanceState(state);
                    savedInstanceState.remove(BUNDLE_RECYCLER_LAYOUT);
                }
            }
        });

        model.getAllMessages().observe(this, new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                if (workInfo!=null){
                    if (workInfo.getState()==RUNNING || workInfo.getState()==ENQUEUED ){
                        footerAdapter.setStatus(FooterAdapter.Status.LOADING,null);
                    }

                    if (workInfo.getState()==FAILED) {
                        Data data = workInfo.getOutputData();
                        if (data.getString(EXTRA_RESULT) != null) {
                            footerAdapter.setStatus(FooterAdapter.Status.ERROR,workInfo.getOutputData().getString(EXTRA_RESULT));
                            Toast.makeText(TopicActivity.this, workInfo.getOutputData().getString(EXTRA_RESULT), Toast.LENGTH_SHORT).show();
                        }
                    }

                    if (workInfo.getState()==SUCCEEDED || workInfo.getState()==CANCELLED ){
                        footerAdapter.setStatus(FooterAdapter.Status.SUCCESS,null);

                        if (topicAdapter.getCurrentList()!=null && topicAdapter.getCurrentList().size()==0)
                            footerAdapter.setStatus(FooterAdapter.Status.MESSAGE,getString(R.string.list_is_empty));
                    }
                }
            }
        });

        model.getForum(forumID).observe(this, new Observer<Forum>() {
            @Override
            public void onChanged(@Nullable Forum forum) {
                if (forum!=null) setTitle(forum.title);
            }
        });

        fab=findViewById(R.id.fab);
        fab.show();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postTopicDialog =new PostTopicDialog(new PostTopicDialog.OnDialogClickListener() {
                    @Override
                    public void onOkClick(String title, String text) {
                        postTopicDialog.setStartLoading();
                        String pref_login=options.getString("pref_login","");
                        String pref_password=options.getString("pref_password","");
                        String pref_email=options.getString("pref_email","");
                        String pref_signature=options.getString("pref_signature","");
                        model.addTopicNetwork(forumID, pref_login, pref_password, pref_email, pref_signature, title, text, new PostBody.OnPostCallback() {
                            @Override
                            public void onError(String message) {
                                if (message!=null) {
                                    postTopicDialog.setError(message);
                                    postTopicDialog.setFinishLoading();
                                }
                            }

                            @Override
                            public void onSuccess(String message) {
                                postedNow=true;
                                model.refreshTopics(forumID);
                                postTopicDialog.dismiss();
                            }
                        });
                    }
                });
                postTopicDialog.show(getSupportFragmentManager(), "topic");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        int new_limit=-1;
        String pref_topics_limit=options.getString("pref_topics_limit","-1");
        try {
            new_limit = Integer.parseInt(pref_topics_limit);
        } catch(Exception ignore){
        }
        if (limit!=new_limit) recreate();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //сохраняем позицию списка
        try {
            super.onSaveInstanceState(outState);
            outState.putParcelable(BUNDLE_RECYCLER_LAYOUT, recyclerView.getLayoutManager().onSaveInstanceState());
        } catch(Exception e){
        }
    }

    //обновление списка (указали фильтр)
    private void refreshDataSource(){
        model.setFilterTopicsList(forumID,filter,isOnlyBookMark, limit);
        //помечаем фильтр для выделения текста в адаптере
        topicAdapter.markText(filter);
        //принудительная перерисовка recycleview
        topicAdapter.notifyDataSetChanged();
    }

    //изменился текст поиска
    @Override
    public void onSearchTextChange(String query) {
        //прячем кнопу добавления топика при поиске
        if (TextUtils.isEmpty(query)){
            fab.show();
        } else {
            fab.hide();
        }
        filter=query.toLowerCase();
        refreshDataSource();
    }

    //фильтр по флагу "закладка"
    public void onBookmarkCheck(boolean checked){
        isOnlyBookMark=checked;
        refreshDataSource();
    }

    //скролл вверх
    void scrolltoFirst(){
        if (postedNow && topicAdapter.getItemCount()>0) {
            postedNow=false;
            recyclerView.smoothScrollToPosition(0);
        }
    }
}
