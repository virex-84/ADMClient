package com.virex.admclient.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.Preference;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;

import com.virex.admclient.R;
import com.virex.admclient.Utils;


public class ColorPickerPreference extends Preference implements SeekBar.OnSeekBarChangeListener {

    private int value;
    private ConstraintLayout color_picker_preview;

    @SuppressLint("NewApi")
    public ColorPickerPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setWidgetLayoutResource(R.layout.color_picker_preview);

        value=context.getColor(R.color.colorPrimary);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWidgetLayoutResource(R.layout.color_picker_preview);

        value=context.getResources().getColor(R.color.colorPrimary);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.color_picker_preview);

        value=context.getResources().getColor(R.color.colorPrimary);
    }

    public ColorPickerPreference(Context context) {
        super(context);
        setWidgetLayoutResource(R.layout.color_picker_preview);

        value=context.getResources().getColor(R.color.colorPrimary);
    }

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        final View box = view.findViewById(R.id.color_picker_box);
        if (box != null) {
            box.setBackgroundColor(value);
        }
    }

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onClick() {
        View color_picker_dialog = View.inflate(getContext(), R.layout.color_picker_dialog, null);
        SeekBar seekBar_Red=color_picker_dialog.findViewById(R.id.seekBar_Red);
        SeekBar seekBar_Green=color_picker_dialog.findViewById(R.id.seekBar_Green);
        SeekBar seekBar_Blue=color_picker_dialog.findViewById(R.id.seekBar_Blue);
        //SeekBar seekBar_Alpha=color_picker_dialog.findViewById(R.id.seekBar_Alpha);
        color_picker_preview=color_picker_dialog.findViewById(R.id.color_picker_preview);

        color_picker_preview.setBackgroundColor(value);
        seekBar_Red.setProgress(Color.red(value));//seekBar_Red.getThumb().setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
        seekBar_Green.setProgress(Color.green(value));//seekBar_Green.getThumb().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
        seekBar_Blue.setProgress(Color.blue(value));//seekBar_Blue.getThumb().setColorFilter(Color.BLUE, PorterDuff.Mode.MULTIPLY);
        //seekBar_Alpha.setProgress(Color.alpha(value));

        seekBar_Red.setOnSeekBarChangeListener(this);
        seekBar_Green.setOnSeekBarChangeListener(this);
        seekBar_Blue.setOnSeekBarChangeListener(this);
        //seekBar_Alpha.setOnSeekBarChangeListener(this);

        Context context = getContext();
        Utils.setColoredTheme(context);
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setCancelable(false);
        dialog.setTitle(this.getTitle());
        dialog.setView(color_picker_dialog);
        dialog.setPositiveButton(getContext().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                persistInt(value);
                notifyChanged();
            }
        }).setNegativeButton(getContext().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                value=getPersistedInt(value);
            }
        }).show();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        //return super.onGetDefaultValue(a, index);
        return a.getInteger(index, R.color.colorPrimary);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) { // Restore state
            value = getPersistedInt(value);
        } else { // Set state
            int value = (Integer) defaultValue;
            this.value = value;
            persistInt(value);
        }
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        //в файле res\values-v9\styles.xml
        //сгенерированы стили с шагом 15 цветов (без альфа канала)
        //поэтому ограничиваем выбор пользователю
        if (progress % 15 == 0) {

            //int alpha = Color.alpha(value);
            int red = Color.red(value);
            int green = Color.green(value);
            int blue = Color.blue(value);

            if (seekBar.getId() == R.id.seekBar_Red) {
                red = progress;
            } else if (seekBar.getId() == R.id.seekBar_Green) {
                green = progress;
            } else if (seekBar.getId() == R.id.seekBar_Blue) {
                blue = progress;
            }
            //else if (seekBar.getId() == R.id.seekBar_Alpha) {
            //    alpha = progress;
            //}

            value = Color.rgb(red, green, blue);
            color_picker_preview.setBackgroundColor(value);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
