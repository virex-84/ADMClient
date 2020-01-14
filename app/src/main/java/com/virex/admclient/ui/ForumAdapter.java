package com.virex.admclient.ui;

import androidx.paging.PagedListAdapter;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.virex.admclient.R;
import com.virex.admclient.db.entity.Forum;

/**
 * Адаптер для отображения форумов
 */
public class ForumAdapter extends PagedListAdapter<Forum, ForumAdapter.ForumViewHolder> {

    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, int position, Forum forum);
    }

    public ForumAdapter(DiffUtil.ItemCallback<Forum> diffUtilCallback, OnItemClickListener onItemClickListener) {
        super(diffUtilCallback);
        this.onItemClickListener=onItemClickListener;
    }

    @NonNull
    @Override
    public ForumViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.forum_items, viewGroup, false);
        return new ForumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForumViewHolder forumViewHolder, final int i) {
        final Forum forum = getItem(i);
        if (forum != null) {
            forumViewHolder.title.setText(forum.title);
            forumViewHolder.dsc.setText(forum.dsc);
            forumViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onItemClick(v,i,forum);
                }
            });
        }
    }

    class ForumViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView dsc;
        ForumViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_title);
            dsc = itemView.findViewById(R.id.tv_dsc);
        }
    }
}