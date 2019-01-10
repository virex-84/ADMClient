package com.virex.admclient.ui;

import android.arch.paging.PagedListAdapter;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;

import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.virex.admclient.R;
import com.virex.admclient.Utils;
import com.virex.admclient.db.entity.Page;

/**
 * Адаптер для отображения постов
 */
public class PagesAdapter extends PagedListAdapter<Page, PagesAdapter.ForumViewHolder> {

    OnItemClickListener onItemClickListener;
    String markText="";
    int foregroundColor=-1;
    int backgroundColor=-1;

    public interface OnItemClickListener {
        void onItemClick(int position, Page page);
        void onReplyClick(int position,Page page);
        void onQuoteClick(int position, Page page);
        void onLinkClick(String link);
        void onBookMarkClick(Page page, int position);
        void onPreviewPostClick(int position);
    }

    public void markText(String text){
        this.markText=text;
    }

    public PagesAdapter(DiffUtil.ItemCallback<Page> diffUtilCallback, @NonNull OnItemClickListener onItemClickListener) {
        super(diffUtilCallback);
        this.onItemClickListener=onItemClickListener;
    }

    public void setColors(int foregroundColor, int backgroundColor) {
        this.foregroundColor=foregroundColor;
        this.backgroundColor=backgroundColor;
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @NonNull
    @Override
    public ForumViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.page_items, viewGroup, false);
        return new ForumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForumViewHolder forumViewHolder, final int i) {
        final Page page = getItem(i);
        if (page != null) {

            SpannableStringBuilder txt= (SpannableStringBuilder) Html.fromHtml(Utils.createContentShortLinks(page.content), new Html.ImageGetter() {
                @Override
                public Drawable getDrawable(String source) {
                    //тут должна быть реализация загрузки изображения
                    //можно грузить например из assets или указать R.drawable.image
                    return null;
                }
            }, null);

            //при поиске - выделяем текст
            if (!TextUtils.isEmpty(markText) && !TextUtils.isEmpty(page.parcedContent)) {
                //убираем специфические для поиска символы
                Pattern word = Pattern.compile(markText.toLowerCase().replaceAll("[-\\[\\]^/,'*:.!><~@#$%+=?|\"\\\\()]+", ""),Pattern.CASE_INSENSITIVE);
                //Matcher match = word.matcher(txt.toString().toLowerCase());
                Matcher match = word.matcher(page.parcedContent.toLowerCase());

                while (match.find()) {
                    //ForegroundColorSpan fcs = new ForegroundColorSpan(ContextCompat.getColor(forumViewHolder.itemView.getContext(), R.color.white));
                    ForegroundColorSpan fcs = new ForegroundColorSpan(foregroundColor);
                    //BackgroundColorSpan bcs = new BackgroundColorSpan(ContextCompat.getColor(forumViewHolder.itemView.getContext(), R.color.colorPrimary));
                    BackgroundColorSpan bcs = new BackgroundColorSpan(backgroundColor);
                    txt.setSpan(fcs, match.start(), match.end(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    txt.setSpan(bcs, match.start(), match.end(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
            }
            forumViewHolder.tv_content.setText(txt);
            forumViewHolder.tv_content.setMovementMethod(new LinkMovementMethod(){
                @Override
                public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
                    if (event.getAction() != MotionEvent.ACTION_UP)
                        return super.onTouchEvent(widget, buffer, event);

                    int x = (int) event.getX();
                    int y = (int) event.getY();

                    x -= widget.getTotalPaddingLeft();
                    y -= widget.getTotalPaddingTop();

                    x += widget.getScrollX();
                    y += widget.getScrollY();

                    Layout layout = widget.getLayout();
                    int line = layout.getLineForVertical(y);
                    int off = layout.getOffsetForHorizontal(line, x);

                    URLSpan[] spanLink = buffer.getSpans(off, off, URLSpan.class);
                    if (spanLink.length != 0) {
                        String link=spanLink[0].getURL();
                        //если кликнули на ссылке сформированной в Utils.createContentShortLinks
                        if (link.contains("content:")){
                            int position=Integer.parseInt(link.replace("content:",""));
                            onItemClickListener.onPreviewPostClick(position);
                        } else
                            onItemClickListener.onLinkClick(link);
                    } else {
                        //т.к. TextView занимает бОльшую часть элемента recyclerview
                        //имитируем нажатие на элемент
                        onItemClickListener.onItemClick(i, page);
                    }
                    return true;
                }
            });
            forumViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onItemClick(i, page);
                }
            });
            forumViewHolder.btn_reply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onReplyClick(i,page);
                }
            });
            forumViewHolder.btn_quote.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onQuoteClick(i,page);
                }
            });

            if(page.isBookMark){
                forumViewHolder.img_isBookMark.setImageResource(R.drawable.ic_bookmark);
                //forumViewHolder.img_isBookMark.setBackgroundResource(R.drawable.ic_bookmark);
                //forumViewHolder.img_isBookMark.setImageDrawable(ContextCompat.getDrawable(forumViewHolder.itemView.getContext(), R.drawable.ic_bookmark));
                //forumViewHolder.img_isBookMark.setImageDrawable(ResourcesCompat.getDrawable(forumViewHolder.itemView.getContext().getResources(), R.drawable.ic_bookmark,null));
            } else {
                forumViewHolder.img_isBookMark.setImageResource(R.drawable.ic_bookmark_border);
            }
            //forumViewHolder.img_isBookMark.invalidate();
            forumViewHolder.img_isBookMark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onBookMarkClick(page, i);
                }
            });

            forumViewHolder.tv_num.setText(String.format("[%d]",page.num));
        } else {
            forumViewHolder.tv_content.setText("Loading ...");
            forumViewHolder.tv_num.setText("[?]");
        }
    }

    class ForumViewHolder extends RecyclerView.ViewHolder {
        TextView tv_content;
        Button btn_reply;
        Button btn_quote;
        ImageView img_isBookMark;
        TextView tv_num;
        ForumViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_content = itemView.findViewById(R.id.tv_content);
            btn_reply = itemView.findViewById(R.id.btn_reply);
            btn_quote = itemView.findViewById(R.id.btn_quote);
            img_isBookMark = itemView.findViewById(R.id.img_isBookMark);
            tv_num = itemView.findViewById(R.id.tv_num);
        }
    }
}