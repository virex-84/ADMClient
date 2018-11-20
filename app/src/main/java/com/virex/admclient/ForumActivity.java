package com.virex.admclient;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.arch.paging.PagedList;
import android.content.Intent;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;

import com.virex.admclient.db.entity.Forum;
import com.virex.admclient.ui.ForumAdapter;

/**
 * Активность списка форумов
 */
public class ForumActivity extends BaseAppCompatActivity {
    RecyclerView recyclerView;
    ProgressBar progressBar;
    SwipeRefreshLayout swipeRefreshLayout;
    ForumAdapter adapter;

    private static final String BUNDLE_RECYCLER_LAYOUT = "recycler_layout";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view);

        final MyViewModel model = ViewModelProviders.of(this).get(MyViewModel.class);

        progressBar = findViewById(R.id.progressBar);
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

        adapter = new ForumAdapter(Forum.DIFF_CALLBACK, new ForumAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, Forum forum) {
                Intent intent = new Intent(getBaseContext(), TopicActivity.class);
                intent.putExtra("n",forum.n);
                startActivity(intent);
            }
        });

        recyclerView.setAdapter(adapter);

        model.allForums().observe(this, new Observer<PagedList<Forum>>() {
            @Override
            public void onChanged(@Nullable PagedList<Forum> forums) {
                adapter.submitList(forums);

                if (forums == null) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    if (forums.size()>0){
                        progressBar.setVisibility(View.GONE);
                    } else
                        progressBar.setVisibility(View.VISIBLE);
                }

                //восстанавливаем позицию списка
                if (savedInstanceState != null) {
                    Parcelable state = savedInstanceState.getParcelable(BUNDLE_RECYCLER_LAYOUT);
                    recyclerView.getLayoutManager().onRestoreInstanceState(state);
                    savedInstanceState.remove(BUNDLE_RECYCLER_LAYOUT);
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //сохраняем позицию списка
        outState.putParcelable(BUNDLE_RECYCLER_LAYOUT,recyclerView.getLayoutManager().onSaveInstanceState());
    }
}
