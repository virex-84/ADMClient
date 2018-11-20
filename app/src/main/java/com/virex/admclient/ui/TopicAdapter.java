package com.virex.admclient.ui;

import android.arch.paging.PagedList;
import android.arch.paging.PagedListAdapter;
import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.virex.admclient.R;
import com.virex.admclient.Utils;
import com.virex.admclient.db.entity.Topic;

/**
 * Адаптер для отображения топиков
 */
public class TopicAdapter extends PagedListAdapter<Topic, TopicAdapter.TopicViewHolder> {

    OnItemClickListener onItemClickListener;
    String markText="";
    int foregroundColor=-1;
    int backgroundColor=-1;
    int highLightedColor=-1;

    public interface OnItemClickListener {
        void onItemClick(View view, int position, Topic topic);
        void onBookMarkClick(Topic topic, int position);
    }

    public void markText(String text){
        this.markText=text;
    }

    public TopicAdapter(DiffUtil.ItemCallback<Topic> diffUtilCallback, TopicAdapter.OnItemClickListener onItemClickListener) {
        super(diffUtilCallback);
        this.onItemClickListener=onItemClickListener;
    }

    public void setColors(int foregroundColor, int backgroundColor, int highLightedColor) {
        this.foregroundColor=foregroundColor;
        this.backgroundColor=backgroundColor;
        this.highLightedColor=highLightedColor;
    }

    @NonNull
    @Override
    public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.topic_items, viewGroup, false);
        return new TopicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicViewHolder topicViewHolder, final int i) {
        final Topic topic = getItem(i);
        if (topic != null) {

            topicViewHolder.tv_title.setText(topic.title);
            topicViewHolder.tv_dsc.setText(topic.dsc);
            if (topic.state.equals("closed")){
                if(topic.isReaded){
                    topicViewHolder.img_isread.setImageResource(R.drawable.ic_lock_outline);
                } else {
                    topicViewHolder.img_isread.setImageResource(R.drawable.ic_lock);
                }
            } else if (topic.isReaded){
                topicViewHolder.img_isread.setImageResource(R.drawable.ic_read);
                topicViewHolder.tv_dsc.setTypeface(null, Typeface.NORMAL);
                topicViewHolder.tv_count.setTypeface(null, Typeface.NORMAL);
            } else {
                topicViewHolder.img_isread.setImageResource(R.drawable.ic_unread);
                topicViewHolder.tv_dsc.setTypeface(null, Typeface.BOLD);
                topicViewHolder.tv_count.setTypeface(null, Typeface.BOLD);
            }

            if (!TextUtils.isEmpty(markText)) {
                SpannableStringBuilder txtTitle=Utils.makeSpanText(topicViewHolder.itemView.getContext(),topic.title,markText,foregroundColor,backgroundColor);
                if (txtTitle!=null) topicViewHolder.tv_title.setText(txtTitle);

                SpannableStringBuilder txtDsc=Utils.makeSpanText(topicViewHolder.itemView.getContext(),topic.dsc,markText,foregroundColor,backgroundColor);
                if (txtDsc!=null) topicViewHolder.tv_dsc.setText(txtDsc);
            }

            //количество постов в топике
            String count=String.format("%d", topic.count);
            if (topic.count>topic.lastcount) count=String.format("%d(+%d)", topic.count, topic.count-topic.lastcount);
            topicViewHolder.tv_count.setText(count);
            //выделяем дополнительный текст
            Utils.setHighLightedText(topicViewHolder.tv_count,String.format("+%d", topic.count-topic.lastcount),highLightedColor);

            topicViewHolder.tv_name.setText(topic.name);
            topicViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onItemClick(v,i,topic);
                }
            });

            if(topic.isBookMark){
                topicViewHolder.img_isBookMark.setImageResource(R.drawable.ic_bookmark);
            } else {
                topicViewHolder.img_isBookMark.setImageResource(R.drawable.ic_bookmark_border);
            }
            topicViewHolder.img_isBookMark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onBookMarkClick(topic, i);
                }
            });

        } else {
            topicViewHolder.tv_title.setText("Loading ...");
            topicViewHolder.tv_dsc.setText("...");
        }
    }

    class TopicViewHolder extends RecyclerView.ViewHolder {
        TextView tv_title;
        TextView tv_dsc;
        TextView tv_count;
        TextView tv_name;
        ImageView img_isread;
        ImageView img_isBookMark;
        TopicViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_title = itemView.findViewById(R.id.tv_title);
            tv_dsc =  itemView.findViewById(R.id.tv_dsc);
            tv_count =  itemView.findViewById(R.id.tv_count);
            tv_name = itemView.findViewById(R.id.tv_content);
            img_isread = itemView.findViewById(R.id.img_isRead);
            img_isBookMark = itemView.findViewById(R.id.img_isBookMark);
        }
    }

}