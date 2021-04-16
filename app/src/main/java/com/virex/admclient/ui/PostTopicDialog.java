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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.virex.admclient.R;

/**
 * Диалог добавления топика
 */

@SuppressLint("ValidFragment")
public class PostTopicDialog extends DialogFragment {

    public interface OnDialogClickListener {
        void onOkClick(String title, String text);
    }

    private PostTopicDialog.OnDialogClickListener onDialogClickListener;
    private TextView tv_title;
    private TextView tv_text;
    private TextView tv_error;
    private ProgressBar progressBar;


    public PostTopicDialog(PostTopicDialog.OnDialogClickListener onDialogClickListener){
        this.onDialogClickListener = onDialogClickListener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View rootview = getActivity().getLayoutInflater().inflate(R.layout.dialog_posttopic, null);
        tv_title = rootview.findViewById(R.id.tv_title);
        tv_text = rootview.findViewById(R.id.tv_text);
        tv_error = rootview.findViewById(R.id.tv_error);
        progressBar=rootview.findViewById(R.id.progressBar);

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
                if (TextUtils.isEmpty(tv_title.getText())){
                    tv_title.requestFocus();
                    tv_title.setError(getString(R.string.error_empty_text));
                    return;
                }
                if (TextUtils.isEmpty(tv_text.getText())){
                    tv_text.requestFocus();
                    tv_text.setError(getString(R.string.error_empty_text));
                    return;
                }
                tv_error.setVisibility(View.GONE);
                onDialogClickListener.onOkClick(tv_title.getText().toString(), tv_text.getText().toString());
            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //сохраняем состояние при перевороте экрана
        //setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setRetainInstance(true);
        return super.onCreateView(inflater, container, savedInstanceState);
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        setTargetFragment(null, -1);
    }

}
