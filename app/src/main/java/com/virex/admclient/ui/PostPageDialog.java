package com.virex.admclient.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.text.Html;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.virex.admclient.R;

import java.util.Locale;

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
    private LinearLayout ll_reply_to;
    private ProgressBar progressBar;
    private String author;
    private String citate;
    private int position;
    private boolean isOnlyPreview=false;

    public PostPageDialog(String author, String citate, int position, OnDialogClickListener onDialogClickListener){
        this.onDialogClickListener = onDialogClickListener;
        this.author=author;
        if (citate!=null) this.citate=Html.fromHtml(citate).toString().trim();
        this.position=position;
    }

    public void setOnlyPreview(boolean isOnlyPreview){
        this.isOnlyPreview=isOnlyPreview;
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
        ll_reply_to =rootview.findViewById(R.id.ll_reply_to);
        progressBar=rootview.findViewById(R.id.progressBar);

        tv_reply_to.setText(author);

        tv_citate.setVisibility(View.GONE);
        if (citate!=null){
            if (!citate.isEmpty()) {
                tv_citate.setVisibility(View.VISIBLE);
                tv_citate.setText(citate);
            }
        }

        tv_position.setText(String.format(Locale.ENGLISH,"[%d]",position));



        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        if (isOnlyPreview){
            ll_reply_to.setVisibility(View.GONE);
            tv_text.setVisibility(View.GONE);
            builder
                    //.setTitle(getString(R.string.app_name))
                    .setCancelable(false)
                    .setView(rootview)
                    .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
        } else {
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
        }

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

        //выравниваем кнопку по центру
        if (isOnlyPreview) {
            Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); //create a new one
            layoutParams.weight = 1.0f;
            layoutParams.gravity = Gravity.CENTER; //this is layout_gravity
            neutralButton.setLayoutParams(layoutParams);
        }
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
