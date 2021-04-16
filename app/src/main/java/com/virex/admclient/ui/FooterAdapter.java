package com.virex.admclient.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.virex.admclient.R;

public class FooterAdapter extends RecyclerView.Adapter<FooterAdapter.FooterViewHolder> {

    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onReloadClick();
    }

    public FooterAdapter(OnItemClickListener onItemClickListener) {
        super();
        this.onItemClickListener=onItemClickListener;
    }

    public enum Status{
        LOADING,
        ERROR,
        MESSAGE,
        SUCCESS
    }

    public void setStatus(Status status, String message){
        this.status=status;
        this.message=message;
        notifyDataSetChanged();
    }

    private Status status=Status.SUCCESS;
    private String message;

    @NonNull
    @Override
    public FooterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.footer_items, parent, false);
        return new FooterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FooterViewHolder holder, int position) {
        ProgressBar progressBar = holder.itemView.findViewById(R.id.progressBar);
        TextView errorMessage=holder.itemView.findViewById(R.id.errorMessage);
        Button reloadButton=holder.itemView.findViewById(R.id.reloadButton);
        reloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onItemClickListener!=null)
                    onItemClickListener.onReloadClick();
            }
        });

        switch (status){
            case LOADING:
                progressBar.setVisibility(View.VISIBLE);
                errorMessage.setVisibility(View.GONE);
                reloadButton.setVisibility(View.GONE);
                break;
            case MESSAGE:
                progressBar.setVisibility(View.GONE);
                errorMessage.setVisibility(View.VISIBLE);
                reloadButton.setVisibility(View.GONE);
                errorMessage.setText(message==null ? "" : message);
                break;
            case ERROR:
                progressBar.setVisibility(View.GONE);
                errorMessage.setVisibility(View.VISIBLE);
                reloadButton.setVisibility(View.VISIBLE);
                errorMessage.setText(message==null ? "" : message);
                break;
            default: ;
        }
    }

    @Override
    public int getItemCount() {
        if (status==Status.SUCCESS)
            return 0;//скрываем
        else
            return 1;
    }

    public static class FooterViewHolder extends RecyclerView.ViewHolder {
        public FooterViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
