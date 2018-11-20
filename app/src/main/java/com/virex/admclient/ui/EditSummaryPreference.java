package com.virex.admclient.ui;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.preference.EditTextPreference;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.virex.admclient.R;

/**
 * Класс EditSummaryPreference используемый в выводе настроек см. res\xml\options.xml
 * позволяет отображать в комментарии содержимое, либо "пусто",
 * либо скрывать содержимое если это пароль
 */
public class EditSummaryPreference extends EditTextPreference {
    private boolean isPassword=false;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public EditSummaryPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        isPassword=attrs.getAttributeBooleanValue("http://schemas.android.com/apk/res/android","password",false);
    }

    public EditSummaryPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        isPassword=attrs.getAttributeBooleanValue("http://schemas.android.com/apk/res/android","password",false);
    }

    public EditSummaryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        isPassword=attrs.getAttributeBooleanValue("http://schemas.android.com/apk/res/android","password",false);
    }

    public EditSummaryPreference(Context context) {
        super(context);
    }

    @Override
    public CharSequence getSummary() {
        CharSequence summary=super.getSummary();

        if (TextUtils.isEmpty(summary)) {
            summary=getContext().getString(R.string.empty);
        }

        //помечено как android:password="true"
        if (isPassword){
            if (TextUtils.isEmpty(super.getText())) {
                summary=getContext().getString(R.string.empty);
            } else {
                summary="*****";
            }
        }
        return summary;
    }


    @Override
    public void setText(String text) {
        super.setText(text);
        setSummary(text);
    }


}
