package com.virex.admclient.ui;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import androidx.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class MyResources extends Resources {
    /**
     * Create a new Resources object on top of an existing set of assets in an
     * AssetManager.
     *
     * @param assets  Previously created AssetManager.
     * @param metrics Current display metrics to consider when
     *                selecting/computing resource values.
     * @param config  Desired device configuration to consider when
     * @deprecated Resources should not be constructed by apps.
     * See {@link Context#createConfigurationContext(Configuration)}.
     */
    public MyResources(AssetManager assets, DisplayMetrics metrics, Configuration config) {
        super(assets, metrics, config);
    }

    public MyResources(Resources original) {
        super(original.getAssets(), original.getDisplayMetrics(), original.getConfiguration());
    }

    //не работает
    @Override
    public int getColor(int id, @Nullable Resources.Theme theme) throws NotFoundException {
        switch (getResourceEntryName(id)) {
            case "colorPrimary":
                // You can change the return value to an instance field that loads from SharedPreferences.
                return Color.RED; // used as an example. Change as needed.
            default:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return super.getColor(id, theme);
                }
                return super.getColor(id);
        }
    }


    //нет colorPrimary
    @Override
    public void getValue(int id, TypedValue outValue, boolean resolveRefs) throws NotFoundException {
        //super.getValue(id, outValue, resolveRefs);
        String s=getResourceEntryName(id);
        switch (s) {
            case "colorPrimary":
                // You can change the return value to an instance field that loads from SharedPreferences.
                //return Color.RED; // used as an example. Change as needed.
            default:
                super.getValue(id, outValue, resolveRefs);
        }
    }



}
