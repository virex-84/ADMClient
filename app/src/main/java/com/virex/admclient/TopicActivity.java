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

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;

import com.virex.admclient.db.entity.Forum;
import com.virex.admclient.db.entity.Topic;
import com.virex.admclient.network.PostBody;
import com.virex.admclient.ui.PostTopicDialog;
import com.virex.admclient.ui.TopicAdapter;

/**
 * Активность списка топиков в выделенном форуме
 */
public class TopicActivity extends BaseAppCompatActivity {

    private boolean isOnlyBookMark=false;
    private String filter="";
    int limit=-1;

    ProgressBar progressBar;
    RecyclerView recyclerView;
    SwipeRefreshLayout swipeRefreshLayout;
    TopicAdapter adapter;
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

        progressBar = findViewById(R.id.progressBar);
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

        adapter = new TopicAdapter(Topic.DIFF_CALLBACK, new TopicAdapter.OnItemClickListener() {
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
                if (adapter.getCurrentList()!=null) {
                    for (Topic item : adapter.getCurrentList()) {
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

        adapter.setColors(getResources().getColor(R.color.white),colorAccent,colorAccent);
        recyclerView.setAdapter(adapter);

        String pref_topics_limit=options.getString("pref_topics_limit","-1");
        try {
            limit = Integer.parseInt(pref_topics_limit);
        } catch(Exception ignore){
        }
        model.allTopicsListFiltered(forumID,filter,isOnlyBookMark,limit).observe(this, new Observer<PagedList<Topic>>() {
            @Override
            public void onChanged(@Nullable PagedList<Topic> topics) {
                adapter.submitList(topics);

                if (topics == null) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    if (topics.size()>0){
                        progressBar.setVisibility(View.GONE);
                    } else
                        progressBar.setVisibility(View.VISIBLE);
                }

                scrolltoFirst();

                //восстанавливаем позицию списка
                if (savedInstanceState != null) {
                    Parcelable state = savedInstanceState.getParcelable(BUNDLE_RECYCLER_LAYOUT);
                    recyclerView.getLayoutManager().onRestoreInstanceState(state);
                    savedInstanceState.remove(BUNDLE_RECYCLER_LAYOUT);
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
        super.onSaveInstanceState(outState);
        //сохраняем позицию списка
        outState.putParcelable(BUNDLE_RECYCLER_LAYOUT,recyclerView.getLayoutManager().onSaveInstanceState());
    }

    //обновление списка (указали фильтр)
    private void refreshDataSource(){
        model.setFilterTopicsList(forumID,filter,isOnlyBookMark, limit);
        //помечаем фильтр для выделения текста в адаптере
        adapter.markText(filter);
        //принудительная перерисовка recycleview
        adapter.notifyDataSetChanged();
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
        if (postedNow && adapter.getItemCount()>0) {
            postedNow=false;
            recyclerView.smoothScrollToPosition(0);
        }
    }
}
