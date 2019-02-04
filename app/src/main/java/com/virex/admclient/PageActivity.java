package com.virex.admclient;

import android.arch.lifecycle.LiveData;
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
    LinearLayoutManager linearLayoutManager;
    ProgressBar progressBar;
    ProgressBar progressBarRead;

    private String SHARED_OPTIONS;//
    private String SHARED_RECYCLER_POSITION = "SHARED_RECYCLER_POSITION";

    PostPageDialog postPageDialog;

    Snackbar snackbar;

    //флаг "только что запостили"
    boolean postedNow=false;

    int lastcount=0;

    LiveData<Page> previewPage;
    Observer<Page> observer;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view_pages);

        //отображаем меню поиска и фильтр по признаку "закладка"
        showSearchMenuItem=true;
        showBookmarkMenuItem=true;
        showGoUpDownMenuItem=true;

        forumID = getIntent().getIntExtra("n",-1);
        topicID = getIntent().getIntExtra("id",-1);

        SHARED_OPTIONS=String.format("%s-%d-%d",PageActivity.class.getSimpleName(),forumID,topicID);

        model = ViewModelProviders.of(this).get(MyViewModel.class);

        progressBar = findViewById(R.id.progressBar);
        progressBarRead = findViewById(R.id.progressBarRead);
        recyclerView = findViewById(R.id.tv_recycler_view);
        linearLayoutManager=new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        swipeRefreshLayout = findViewById(R.id.swipe_container);

        //окрашиваем прогрессбар в основной цвет
        progressBarRead.getProgressDrawable().setColorFilter(colorAccent, android.graphics.PorterDuff.Mode.SRC_IN);

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
                postPageDialog =new PostPageDialog(page.author, page.content, position, new PostPageDialog.OnDialogClickListener() {
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
                savePositionPreference();
                model.changePageBookmark(page);
            }

            @Override
            public void onPreviewPostClick(int position) {
                /*
                фикс ушами
                получаем данные от LiveData один раз и сразу же отписываемся,
                что-бы при любом изменении данных (например bookmark)
                заново не запустилось диалоговое окно
                */

                previewPage=model.getPost(forumID,topicID,position);
                observer = new Observer<Page>(){
                    @Override
                    public void onChanged(@Nullable Page page) {
                        if (page==null) return;

                        postPageDialog =new PostPageDialog(page.author, page.content, page.num, new PostPageDialog.OnDialogClickListener() {
                            @Override
                            public void onOkClick(String citate, String text) {
                                postPageDialog.dismiss();
                            }
                        });
                        postPageDialog.setOnlyPreview(true);
                        postPageDialog.show(getSupportFragmentManager(), "preview");

                        //отписываемся
                        previewPage.removeObserver(this);
                    }
                };
                //запускаем получение данных
                previewPage.observe(PageActivity.this, observer);
            }

            //событие возникает когда адаптер полностью загрузил данные
            @Override
            public void onCurrentListLoaded() {
                //восстанавливаем позицию
                restorePositionPreference();
                //fix повторяем восстановление позиции
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        restorePositionPreference();
                        updateProgressBar();

                        //если запостили - предлагаем перейти в последний пост
                        if (postedNow){
                            postedNow=false;
                            snackbar=showSnackBarScroll(recyclerView,adapter.getItemCount() - 1,getString(R.string.action_scroll_to_post),getString(R.string.ok));
                        }
                    }
                }, 1);
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

                //прогресс чтения
                updateProgressBar();

                //скрываем прогресс когда список постов не скроллируют
                switch (newState){
                    case RecyclerView.SCROLL_STATE_SETTLING:
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        progressBarRead.setVisibility(View.VISIBLE);
                        break;
                    case RecyclerView.SCROLL_STATE_IDLE:
                        progressBarRead.setVisibility(View.INVISIBLE);
                        break;
                }
            }
        });

        //отфильтрованный список постов
        model.allPagesListFiltered(forumID,topicID,filter,isOnlyBookMark).observe(this, new Observer<PagedList<Page>>() {
            @Override
            public void onChanged(@Nullable final PagedList<Page> pages) {

                //если посты загрузились, то помечаем что топик прочитан
                if (pages!=null) {
                    if (pages.size() > 0)
                        model.setReadTopic(forumID,topicID, pages.size());
                }

                //загружаем данные в адаптер
                adapter.submitList(pages);

                if (pages == null) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    if (pages.size()>0){
                        progressBar.setVisibility(View.GONE);
                    } else
                        progressBar.setVisibility(View.VISIBLE);

                    progressBarRead.setMax(pages.size()-1);
                }

            }
        });

        //информация о топике
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
        String pos=new Gson().toJson(linearLayoutManager.onSaveInstanceState());
        SharedPreferences settings = getSharedPreferences(SHARED_OPTIONS, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SHARED_RECYCLER_POSITION, pos);
        editor.apply();
    }

    //восстанавливаем позицию при открытии Activity
    private void restorePositionPreference(){
        recyclerView.stopScroll();
        SharedPreferences settings = getSharedPreferences(SHARED_OPTIONS, MODE_PRIVATE);
        String pos = settings.getString(SHARED_RECYCLER_POSITION, "");
        LinearLayoutManager.SavedState position =new Gson().fromJson(pos, LinearLayoutManager.SavedState.class);
        linearLayoutManager.onRestoreInstanceState(position);
    }

    //обновление списка (указали фильтр)
    private void refreshDataSource(){
        model.setFilterPagesList(forumID,topicID,filter,isOnlyBookMark);
        //помечаем фильтр для выделения текста в адаптере
        adapter.markText(filter);
        //принудительная перерисовка recycleview
        adapter.notifyDataSetChanged();
    }

    //нажали меню ввода фильтра (поиск)
    @Override
    public void onSearchBegin() {
        //сохраняем текущую позицию
        savePositionPreference();
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

    private void updateProgressBar(){
        progressBarRead.setProgress(linearLayoutManager.findLastVisibleItemPosition());
    }

    @Override
    public void onGoUp() {
        recyclerView.stopScroll();
        linearLayoutManager.scrollToPositionWithOffset(0,0);
        //fix updateProgressBar не актуален, linearLayoutManager.findLastVisibleItemPosition() еще не знает об изменениях
        progressBarRead.setProgress(0);
    }

    @Override
    public void onGoBottom() {
        recyclerView.stopScroll();
        linearLayoutManager.scrollToPositionWithOffset(adapter.getItemCount() - 1,0);
        //fix updateProgressBar не актуален, linearLayoutManager.findLastVisibleItemPosition() еще не знает об изменениях
        progressBarRead.setProgress(adapter.getItemCount() - 1);
    }
}
