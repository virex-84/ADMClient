package com.virex.admclient;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDelegate;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Вспомогательные функции
 */
public class Utils {

    //десериализация из текста в объект
    @SuppressWarnings("unchecked")
    public static <T> T parceLineToObject(Class klass, String line){
        //T obj=(T)new Object();
        T obj = null;

        for (Field field : klass.getDeclaredFields()) {
            String paramName = field.getName();
            String paramValue = parseString(line, paramName);
            if (paramValue!=null) {
                field.setAccessible(true);
                try {
                    if (obj==null) obj = (T) klass.newInstance();
                    Class<?> type = field.getType();

                    if (type.getSimpleName().equals("int")){
                        field.set(obj,Integer.valueOf(paramValue));
                    } else
                        field.set(obj,type.cast(paramValue));
                } catch (IllegalAccessException ignore) {
                    //e.printStackTrace();
                } catch (InstantiationException ignore) {
                    //e.printStackTrace();
                }
            }
        }
        return obj;
    }

    // парсинг строки "param1=value1    param2=value2"
    public static String parseString(String source, String param) {
        if ((source==null) || (param==null)) return null;
        //разбиваем строку с табами в строки
        String[] parameters = source.split("\t");

        for (String line:parameters){
            String[] valuepair = line.trim().split("=");
            if (valuepair.length>1)
                if (valuepair[0].equals(param)) return valuepair[1].trim();
        }
        return null;
    }

    //вытаскиваем содержимое из тега, например <pre>text</pre>
    public static String extractHTMLTag(String tag, String source){
        Pattern p = Pattern.compile("<"+tag+">(.*?)</"+tag+">", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(source);
        String result=null;

        if (m.find()) {
            result=m.group(1).trim();
        }
        return result;
    }

    //кодирование для post запросов
    public static String URLEncodeString(String source){
        source=source.replace("UTF-8","windows-1251");
        try {
            source=URLEncoder.encode(source, "windows-1251");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return source;
    }

    //поиск значения по атрибуту
    public static int getColorByAttributeId(Context context, @AttrRes int attrIdForColor){
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attrIdForColor, typedValue, true);
        return typedValue.data;
    }

    //выделяем текст красным (например 99(+1))
    @SuppressLint("ResourceType")
    public static void setHighLightedText(TextView tv, String textToHighlight, int accentColour) {
        //int accentColour=0;

        String tvt = tv.getText().toString();
        /*
        if (tvt.contains(textToHighlight)){
            accentColour=getColorByAttributeId(tv.getContext(),R.attr.colorAccent);
        }
        */
        int ofe = tvt.indexOf(textToHighlight, 0);
        Spannable wordToSpan = new SpannableString(tv.getText());

        for (int ofs = 0; ofs < tvt.length() && ofe != -1; ofs = ofe + 1) {
            ofe = tvt.indexOf(textToHighlight, ofs);
            if (ofe == -1)
                break;
            else {
                //wordToSpan.setSpan(new ForegroundColorSpan(ContextCompat.getColor(tv.getContext(), R.color.colorAccent)), ofe, ofe + textToHighlight.length(), 0);
                wordToSpan.setSpan(new ForegroundColorSpan(accentColour), ofe, ofe + textToHighlight.length(), 0);
                tv.setText(wordToSpan, TextView.BufferType.SPANNABLE);
            }
        }
    }

    //выделяем текст синим
    public static SpannableStringBuilder makeSpanText(Context context, String source, String findtext, int foregroundColor, int backgroundColor){
        if (context==null || TextUtils.isEmpty(source) || TextUtils.isEmpty(findtext)) return null;

        SpannableStringBuilder txt = new SpannableStringBuilder();
        txt.append(source);

        //убираем специфические для поиска символы
        Pattern word = Pattern.compile(findtext.toLowerCase().replaceAll("[-\\[\\]^/,'*:.!><~@#$%+=?|\"\\\\()]+", ""),Pattern.CASE_INSENSITIVE);
        Matcher match = word.matcher(source.toLowerCase());

        while (match.find()) {
            //ForegroundColorSpan fcs = new ForegroundColorSpan(ContextCompat.getColor(context, R.color.white));
            ForegroundColorSpan fcs = new ForegroundColorSpan(foregroundColor);
            //BackgroundColorSpan bcs = new BackgroundColorSpan(ContextCompat.getColor(context, R.color.colorPrimary));
            BackgroundColorSpan bcs = new BackgroundColorSpan(backgroundColor);
            txt.setSpan(fcs, match.start(), match.end(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            txt.setSpan(bcs, match.start(), match.end(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }

        return txt;
    }

    /*
      Создание уведомления с историей перехода
      Например: при нажатии на уведомление, переходим на список постов, если нажать "назад" -
      перейдём на список веток выбранного форума, еще раз "назад" - выходим в список форумов
     */
    public static void sendNotification(Context context, int forumID, int topicID, String messageBody) {
        Intent intent = new Intent(context, PageActivity.class);
        intent.putExtra("n",forumID);
        intent.putExtra("id",topicID);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        //создаем историю перехода: форумы->топики(+id форума в параметрах)->открываемый список постов
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        Intent activityForums = new Intent(context, ForumActivity.class);
        Intent activityTopics = new Intent(context, TopicActivity.class);activityTopics.putExtra("n", forumID);
        stackBuilder.addNextIntent(activityForums).addNextIntent(activityTopics).addNextIntent(intent);

        //обязательно необходимо указать recuestCode - что-бы система различала "нотификации"
        PendingIntent pendingIntent=stackBuilder.getPendingIntent(topicID,PendingIntent.FLAG_ONE_SHOT);

        String channelId = context.getString(R.string.default_notification_channel_id);
        Notification.Builder notificationBuilder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationBuilder = new Notification.Builder(context, channelId);
        } else {
            notificationBuilder = new Notification.Builder(context);
        }

        notificationBuilder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager!=null) {
            //для android 8 и выше - обязательно
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(channelId, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
            }
            notificationManager.notify(topicID /* ID of notification */, notificationBuilder.build());
        }
        //Log.e("sendNotification","sendNotification");
    }

    //смена темы "дневная/ночная"
    public static void changeTheme(boolean is_dark_theme){
        if (is_dark_theme){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    //получаем "главный цвет"
    private static int getColorPrimary(Context context){
        int pref_colorPrimary = PreferenceManager.getDefaultSharedPreferences(context).getInt("pref_colorPrimary", 0);
        if (pref_colorPrimary==0){
            pref_colorPrimary=context.getResources().getColor(R.color.colorPrimary);
        }
        //очищаем от alpha
        int red = Color.red(pref_colorPrimary);
        int green = Color.green(pref_colorPrimary);
        int blue = Color.blue(pref_colorPrimary);
        pref_colorPrimary=Color.argb(0,red, green, blue);
        return pref_colorPrimary;
    }

    //устанавливаем (выбранный пользователем) цвет для темы
    public static void setColoredTheme(Context context){
        String pref_colorPrimaryString=String.format("%X",getColorPrimary(context)).toLowerCase();
        context.getTheme().applyStyle(context.getResources().getIdentifier("T_" + pref_colorPrimaryString, "style", context.getPackageName()), true);
    }

    //переопределяем эффект наплыва при оверскролле recycleview - меняем цвет
    public static void changeOverScrollGlowColor(Context context) {
        try {
            Resources res = context.getResources();
            //берём цвет с альфаканалом
            @ColorInt int colorPrimary = PreferenceManager.getDefaultSharedPreferences(context).getInt("pref_colorPrimary", 0);
            final int glowDrawableId = res.getIdentifier("overscroll_glow", "drawable", "android");
            final Drawable overscrollGlow = res.getDrawable(glowDrawableId);
            overscrollGlow.setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);

            final int edgeDrawableId = res.getIdentifier("overscroll_edge", "drawable", "android");
            final Drawable overscrollEdge = res.getDrawable(edgeDrawableId);
            overscrollEdge.setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        } catch (Exception ignored) {
        }
    }
}
