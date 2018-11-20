package com.virex.admclient.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.virex.admclient.R;

/**
 * Диалог добавления поста
 */

@SuppressLint("ValidFragment")
public class PostPageDialog extends DialogFragment {

    public interface OnDialogClickListener {
        void onOkClick(String citate, String text);
    }

    private OnDialogClickListener onDialogClickListener;
    private TextView tv_citate;
    private TextView tv_text;
    private TextView tv_error;
    private TextView tv_position;
    private ProgressBar progressBar;
    String author;
    String citate;
    int position;


    public PostPageDialog(String author, String citate, int position, OnDialogClickListener onDialogClickListener){
        this.onDialogClickListener = onDialogClickListener;
        this.author=author;
        this.citate=citate;
        this.position=position;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View rootview = getActivity().getLayoutInflater().inflate(R.layout.dialog_postpage, null);
        TextView tv_reply_to = rootview.findViewById(R.id.tv_reply_to);
        tv_citate = rootview.findViewById(R.id.tv_citate);
        tv_text = rootview.findViewById(R.id.tv_text);
        tv_error = rootview.findViewById(R.id.tv_error);
        tv_position = rootview.findViewById(R.id.tv_position);
        progressBar=rootview.findViewById(R.id.progressBar);

        tv_reply_to.setText(author);

        tv_citate.setVisibility(View.GONE);
        if (citate!=null){
            if (!citate.isEmpty()) {
                tv_citate.setVisibility(View.VISIBLE);
                tv_citate.setText(citate);
            }
        }

        tv_position.setText(String.format("[%d]",position));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                //.setTitle(getString(R.string.app_name))
                .setCancelable(false)
                .setView(rootview)
                .setPositiveButton(R.string.reply, null)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        //перехватываем нажатие кнопки "ок"
        AlertDialog dialog = (AlertDialog)getDialog();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(tv_text.getText())) {
                    //ругаемся на пустой текст
                    tv_text.setError(getString(R.string.error_empty_text));
                } else {
                    tv_error.setVisibility(View.GONE);
                    onDialogClickListener.onOkClick(citate, tv_text.getText().toString());
                }
            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //сохраняем состояние при перевороте экрана
        setRetainInstance(true);
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    public void setError(String message){
        tv_error.setVisibility(View.VISIBLE);
        tv_error.setText(Html.fromHtml(message).toString());
    }

    public void setStartLoading(){
        progressBar.setVisibility(View.VISIBLE);
    }

    public void setFinishLoading(){
        progressBar.setVisibility(View.GONE);
    }


}
