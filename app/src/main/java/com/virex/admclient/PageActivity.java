package com.virex.admclient;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.arch.paging.PagedList;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;


import com.google.gson.Gson;

import com.virex.admclient.db.entity.Page;
import com.virex.admclient.db.entity.Topic;
import com.virex.admclient.network.PostBody;
import com.virex.admclient.ui.PagesAdapter;
import com.virex.admclient.ui.PostPageDialog;
import com.virex.admclient.ui.SwipyRefreshLayout.SwipyRefreshLayout;
import com.virex.admclient.ui.SwipyRefreshLayout.SwipyRefreshLayoutDirection;

/**
 * Активность списка постов
 */
public class PageActivity extends BaseAppCompatActivity {

    MyViewModel model;

    private boolean isOnlyBookMark=false;
    private String filter="";
    private int forumID;
    private int topicID;
    private SwipyRefreshLayout swipeRefreshLayout;
    private boolean topicIsClosed=false;

    PagesAdapter adapter;
    RecyclerView recyclerView;
    ProgressBar progressBar;

    private String SHARED_OPTIONS;//
    private String SHARED_RECYCLER_POSITION = "SHARED_RECYCLER_POSITION";

    LinearLayoutManager.SavedState position;

    PostPageDialog postPageDialog;

    Snackbar snackbar;

    //флаг "только что запостили"
    boolean postedNow=false;

    int lastcount=0;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view_pages);

        //отображаем меню поиска и фильтр по признаку "закладка"
        showSearchMenuItem=true;
        showBookmarkMenuItem=true;

        forumID = getIntent().getIntExtra("n",-1);
        topicID = getIntent().getIntExtra("id",-1);

        SHARED_OPTIONS=String.format("%s-%d-%d",PageActivity.class.getSimpleName(),forumID,topicID);

        model = ViewModelProviders.of(this).get(MyViewModel.class);

        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.tv_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        swipeRefreshLayout = findViewById(R.id.swipe_container);

        //обновление по свайпу вниз/вверх
        swipeRefreshLayout.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh(SwipyRefreshLayoutDirection direction) {
                swipeRefreshLayout.setRefreshing(false);
                model.refreshPages(forumID,topicID);
                //Log.e("onRefresh",direction.name());
            }
        });

        adapter = new PagesAdapter(Page.DIFF_CALLBACK, new PagesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position, Page page) {

            }

            //нажатие "ответ"
            @Override
            public void onReplyClick(final int position, final Page page) {
                if (topicIsClosed){
                    showSnackBarInfo(recyclerView,getString(R.string.topic_is_closed),getString(R.string.ok));
                    return;
                }
                postPageDialog =new PostPageDialog(page.author, null, position, new PostPageDialog.OnDialogClickListener() {
                    @Override
                    public void onOkClick(String citate, String text) {
                        postPageDialog.setStartLoading();
                        String pref_login=options.getString("pref_login","");
                        String pref_password=options.getString("pref_password","");
                        String pref_email=options.getString("pref_email","");
                        String pref_signature=options.getString("pref_signature","");
                        //отправляем сообщение на сайт
                        text=makeQuote(page.author,position,text);
                        model.addPostNetwork(forumID, topicID, pref_login, pref_password, pref_email, pref_signature, text, new PostBody.OnPostCallback() {
                            @Override
                            public void onError(String message) {
                                if (message!=null) {
                                    postPageDialog.setError(message);
                                    postPageDialog.setFinishLoading();
                                }
                            }

                            @Override
                            public void onSuccess(String message) {
                                postedNow=true;
                                model.refreshPages(forumID,topicID);
                                //закрываем диалог
                                postPageDialog.dismiss();
                            }
                        });
                    }
                });
                postPageDialog.show(getSupportFragmentManager(), "reply");
            }

            //нажатие "цитата"
            @Override
            public void onQuoteClick(final int position, Page page) {
                if (topicIsClosed){
                    showSnackBarInfo(recyclerView,getString(R.string.topic_is_closed),getString(R.string.ok));
                    return;
                }
                postPageDialog =new PostPageDialog(page.author, page.parcedContent.trim(), position, new PostPageDialog.OnDialogClickListener() {
                    @Override
                    public void onOkClick(String citate, String text) {
                        postPageDialog.setStartLoading();
                        String pref_login=options.getString("pref_login","");
                        String pref_password=options.getString("pref_password","");
                        String pref_email=options.getString("pref_email","");
                        String pref_signature=options.getString("pref_signature","");
                        text=makeQuote(citate,position,text);
                        //отправляем сообщение на сайт
                        model.addPostNetwork(forumID, topicID, pref_login, pref_password, pref_email, pref_signature, text, new PostBody.OnPostCallback() {
                            @Override
                            public void onError(String message) {
                                if (message!=null) {
                                    postPageDialog.setError(message);
                                    postPageDialog.setFinishLoading();
                                }
                            }

                            @Override
                            public void onSuccess(String message) {
                                postedNow=true;
                                model.refreshPages(forumID,topicID);
                                //закрываем диалог
                                postPageDialog.dismiss();
                            }
                        });
                    }
                });
                postPageDialog.show(getSupportFragmentManager(), "quote");
            }

            @Override
            public void onLinkClick(String link) {
                //для "перемещенных" ссылок
                if (link.contains("forum.pl?n=") && (!link.contains("http://delphimaster.ru/cgi-bin/"))) {
                    link="http://delphimaster.ru/cgi-bin/".concat(link);
                }
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                startActivity(intent);
            }

            @Override
            public void onBookMarkClick(Page page, int position) {
                savePosition();
                model.changePageBookmark(page);
            }
        });

        recyclerView.setAdapter(adapter);
        adapter.setColors(getResources().getColor(R.color.white),colorAccent);

        recyclerView.clearAnimation();
        recyclerView.setItemAnimator(null);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                //скрываем сообщение при прокрутке
                if (snackbar!=null){
                    snackbar.dismiss();
                    snackbar=null;
                }
            }
        });

        //возвращаем позицию при открытии Acivity/смены положения экрана
        restorePositionPreference();

        model.allPagesListFiltered(forumID,topicID,filter,isOnlyBookMark).observe(this, new Observer<PagedList<Page>>() {
            @Override
            public void onChanged(@Nullable final PagedList<Page> pages) {

                //если посты загрузились, то помечаем что топик прочитан
                if (pages!=null) {
                    if (pages.size() > 0)
                        model.setReadTopic(forumID,topicID, pages.size());
                }

                adapter.submitList(pages);

                if (pages == null) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    if (pages.size()>0){
                        progressBar.setVisibility(View.GONE);
                    } else
                        progressBar.setVisibility(View.VISIBLE);
                }


                //возвращаем позицию после обновления данных (например после фильтра)
                restorePosition();
                //небольшой "фикс"
                //при загрузке данных, позиция уже на 100 посте и выше изменяется
                //поэтому приходится еще раз вызвать восстановление позиции
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        restorePosition();
                        position=null;
                        scrolltoLast();
                    }
                }, 1);

            }
        });

        model.getTopic(forumID,topicID).observe(this, new Observer<Topic>() {
            @Override
            public void onChanged(@Nullable Topic topic) {
                if (topic!=null) {
                    //заголовок
                    setTitle(topic.title);

                    //если тема закрыта
                    if (topic.state.equals("closed")){
                        topicIsClosed=true;

                        //отображаем иконку
                        getSupportActionBar().setDisplayShowHomeEnabled(true);
                        getSupportActionBar().setLogo(R.drawable.ic_lock_outline);
                        getSupportActionBar().setDisplayUseLogoEnabled(true);
                    }

                    //дозагружаем посты
                    if (topic.lastcount<topic.count) model.refreshPages(forumID,topicID);

                    //дожидаемся полной загрузки постов
                    //и предлагаем промотать до свежих постов
                    if (topic.lastcount<topic.count) lastcount=topic.lastcount;
                    if ((topic.lastcount==topic.count) && (lastcount>0))
                        snackbar=showSnackBarScroll(recyclerView,lastcount+1,getString(R.string.action_scroll_to_post),getString(R.string.ok));
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePositionPreference();
    }

    //сохраняем позицию при закрытии Activity
    private void savePositionPreference(){
        String pos=new Gson().toJson(recyclerView.getLayoutManager().onSaveInstanceState());
        SharedPreferences settings = getSharedPreferences(SHARED_OPTIONS, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SHARED_RECYCLER_POSITION, pos);
        editor.apply();
    }

    //восстанавливаем позицию при открытии Activity
    private void restorePositionPreference(){
        SharedPreferences settings = getSharedPreferences(SHARED_OPTIONS, MODE_PRIVATE);
        String pos = settings.getString(SHARED_RECYCLER_POSITION, "");
        position =new Gson().fromJson(pos, LinearLayoutManager.SavedState.class);
        restorePosition();
    }

    //сохранение позиции списка
    private void savePosition(){
        position= (LinearLayoutManager.SavedState) recyclerView.getLayoutManager().onSaveInstanceState();
    }

    //восстановление позиции списка
    private void restorePosition(){
        recyclerView.getLayoutManager().onRestoreInstanceState(position);
    }

    //обновление списка (указали фильтр)
    private void refreshDataSource(){
        model.setFilterPagesList(forumID,topicID,filter,isOnlyBookMark);
        //помечаем фильтр для выделения текста в адаптере
        adapter.markText(filter);
        //принудительная перерисовка recycleview
        adapter.notifyDataSetChanged();
    }

    //изменился текст поиска
    @Override
    public void onSearchTextChange(String query) {
        filter=query.toLowerCase();
        refreshDataSource();
    }

    //фильтр по флагу "закладка"
    public void onBookmarkCheck(boolean checked){
        isOnlyBookMark=checked;
        refreshDataSource();
    }

    //после добавления поста, перемещаемся в самый низ
    void scrolltoLast(){
        if (postedNow && adapter.getItemCount()>0) {
            postedNow=false;
            recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
        }
    }

    //делаем цитату (обрамляем курсивом)
    String makeQuote(String quote, int position, String text){
        String quoteLines="";

        for (String line: quote.split("\n")){
            if (quoteLines.isEmpty()){
                quoteLines=quoteLines.concat("> ".concat(line).concat(String.format(" [%d]",position)).concat("\n"));
            } else
                quoteLines=quoteLines.concat("> ".concat(line).concat("\n"));
        }

        return "<i>".concat(quoteLines).concat("</i>").concat(text);
    }


}
