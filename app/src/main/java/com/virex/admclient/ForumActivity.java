package com.virex.admclient;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.paging.PagedList;
import android.content.Intent;
import android.os.Parcelable;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.WorkInfo;

import android.view.View;
import android.widget.Toast;

import com.virex.admclient.db.entity.Forum;
import com.virex.admclient.ui.FooterAdapter;
import com.virex.admclient.ui.ForumAdapter;

import static androidx.work.WorkInfo.State.*;
import static com.virex.admclient.repository.ForumsWorker.EXTRA_RESULT;

/**
 * Активность списка форумов
 */
public class ForumActivity extends BaseAppCompatActivity {
    RecyclerView recyclerView;
    SwipeRefreshLayout swipeRefreshLayout;
    ForumAdapter forumAdapter;
    FooterAdapter footerAdapter;

    private static final String BUNDLE_RECYCLER_LAYOUT = "recycler_layout";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view);

        final MyViewModel model = ViewModelProviders.of(this).get(MyViewModel.class);

        recyclerView = findViewById(R.id.tv_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        swipeRefreshLayout = findViewById(R.id.swipe_container);


        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(false);
                model.refreshForums();
            }
        });

        forumAdapter = new ForumAdapter(Forum.DIFF_CALLBACK, new ForumAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, Forum forum) {
                Intent intent = new Intent(getBaseContext(), TopicActivity.class);
                intent.putExtra("n",forum.n);
                startActivity(intent);
            }
        });

        footerAdapter = new FooterAdapter(new FooterAdapter.OnItemClickListener() {
            @Override
            public void onReloadClick() {
                swipeRefreshLayout.setRefreshing(false);
                model.refreshForums();
            }
        });

        ConcatAdapter concatAdapter=new ConcatAdapter(forumAdapter,footerAdapter);
        recyclerView.setAdapter(concatAdapter);

        model.allForums().observe(this, new Observer<PagedList<Forum>>() {
            @Override
            public void onChanged(@Nullable PagedList<Forum> forums) {
                forumAdapter.submitList(forums);

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
                            Toast.makeText(ForumActivity.this, workInfo.getOutputData().getString(EXTRA_RESULT), Toast.LENGTH_SHORT).show();
                        }
                    }

                    if (workInfo.getState()==SUCCEEDED || workInfo.getState()==CANCELLED ){
                        footerAdapter.setStatus(FooterAdapter.Status.SUCCESS,null);
                    }
                }
            }
        });

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
}
