package com.virex.admclient.ui;

import android.content.Context;

import android.graphics.PorterDuff;
import android.support.v4.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.virex.admclient.R;

public class CheckBoxActionProvider extends ActionProvider implements
        CompoundButton.OnCheckedChangeListener {


    public interface OnClickListener {
        void onCheckedChanged(boolean isChecked);
    }

    /**
     * Creates a new instance. ActionProvider classes should always implement a
     * constructor that takes a single Context parameter for inflating from menu XML.
     *
     * @param context Context for accessing resources.
     */

    OnClickListener onClickListener;
    Context context;
    CheckBox checkbox;

    public CheckBoxActionProvider(Context context) {
        super(context);
        this.context=context;
    }

    public void setOnClick(OnClickListener onClickListener ){
        this.onClickListener=onClickListener;
    }

    @Override
    public View onCreateActionView() {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.checkbox_action_provider,null);
        checkbox = view.findViewById(R.id.checkbox);

        checkbox.setOnCheckedChangeListener(this);
        /*
        view.setOnClickListener (new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkbox.setChecked(!checkbox.isChecked());
            }
        });
        */

        return view;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (onClickListener!=null){
            onClickListener.onCheckedChanged(isChecked);
        }
    }

}
