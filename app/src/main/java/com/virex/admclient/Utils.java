package com.virex.admclient;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.core.app.TaskStackBuilder;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.text.PrecomputedTextCompat;

import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.Future;
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
        //создаем историю перехода: форумы->топики(+id форума в параметрах)->открываемый список постов
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        Intent activityForums = new Intent(context, ForumActivity.class);
        Intent activityTopics = new Intent(context, TopicActivity.class);
        activityTopics.putExtra("n", forumID);
        Intent activityPost = new Intent(context, PageActivity.class);
        activityPost.putExtra("n",forumID);
        activityPost.putExtra("id",topicID);
        activityPost.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        stackBuilder.addNextIntent(activityForums).addNextIntent(activityTopics).addNextIntent(activityPost);

        //обязательно необходимо указать recuestCode - что-бы система различала "нотификации"
        PendingIntent pendingIntent = null;
        //PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_ONE_SHOT);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE);
        }
        else
        {
            pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_ONE_SHOT);
        }

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

    public static String createContentShortLinks(String source){
        String regex = "(?<=\\[)(\\d+)(?=\\])";

        //добавляем ссылку content:123
        return source.replaceAll(regex, "<a href=\"content:$0\">$0</a>");
    }

    //добавляем в текст все ссылки (http:google.com и просто google.com)
    public static SpannableStringBuilder createSpannableContent(String source){
        SpannableStringBuilder txt= (SpannableStringBuilder) Html.fromHtml(Utils.createContentShortLinks(source), new Html.ImageGetter() {
            @Override
            public Drawable getDrawable(String source) {
                //тут должна быть реализация загрузки изображения
                //можно грузить например из assets или указать R.drawable.image
                return null;
            }
        }, null);

        URLSpan[] currentSpans = txt.getSpans(0, txt.length(), URLSpan.class);
        SpannableStringBuilder buffer = new SpannableStringBuilder(txt);
        Linkify.addLinks(buffer, Linkify.WEB_URLS);
        for (URLSpan span : currentSpans) {
            int end = txt.getSpanEnd(span);
            int start = txt.getSpanStart(span);
            buffer.setSpan(span, start, end, 0);
        }
        return buffer;
    }

    public static  Future<PrecomputedTextCompat> createSpannableContent(String source,PrecomputedTextCompat.Params params){
        SpannableStringBuilder ttt= (SpannableStringBuilder) Html.fromHtml(Utils.createContentShortLinks(source), new Html.ImageGetter() {
            @Override
            public Drawable getDrawable(String source) {
                //тут должна быть реализация загрузки изображения
                //можно грузить например из assets или указать R.drawable.image
                return null;
            }
        }, null);

        Future<PrecomputedTextCompat> txt = PrecomputedTextCompat.getTextFuture(ttt,params,null);

        try {
            URLSpan[] currentSpans = txt.get().getSpans(0, txt.get().length(), URLSpan.class);
            SpannableStringBuilder buffer = new SpannableStringBuilder(txt.get());
            Linkify.addLinks(buffer, Linkify.WEB_URLS);
            for (URLSpan span : currentSpans) {
                int end = txt.get().getSpanEnd(span);
                int start = txt.get().getSpanStart(span);
                //buffer.setSpan(span, start, end, 0);
                txt.get().setSpan(span, start, end, 0);
            }

        }catch(Exception e){

        }

        return txt;
    }

    public static boolean canResolveBroadcast(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> receivers = packageManager.queryBroadcastReceivers(intent, 0);
        return receivers != null && receivers.size() > 0;
    }

    public static void setBadge(Context context, Notification notification, int count){
        final String GOOGLE = "google";
        final String HUAWEI = "huawei";
        final String MEIZU = "meizu";
        final String XIAOMI = "xiaomi";
        final String OPPO = "oppo";
        final String VIVO = "vivo";
        final String SAMSUNG = "samsung";
        final String LG = "lg";
        final String SONY = "sony";
        final String HTC = "htc";
        final String NOVA = "nova";

        String launcherClassName = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName()).getComponent().getClassName();

        if (!TextUtils.isEmpty(Build.MANUFACTURER))
            switch (Build.MANUFACTURER.toLowerCase()) {
                case HUAWEI:{
                    Bundle bundle = new Bundle();
                    bundle.putString("package", context.getPackageName()); // com.test.badge is your package name
                    bundle.putString("class", launcherClassName); // com.test. badge.MainActivity is your apk main activity
                    bundle.putInt("badgenumber", count);
                    context.getContentResolver().call(Uri.parse("content://com.huawei.android.launcher.settings/badge/"), "change_badge", null, bundle);
                }
                break;
                case XIAOMI:{
                    NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    //Notification.Builder builder = new Notification.Builder(context).setContentTitle("title").setContentText("text").setSmallIcon(R.mipmap.ic_launcher);
                    //Notification notification = builder.build();
                    try {
                        Field field = notification.getClass().getDeclaredField("extraNotification");
                        Object extraNotification = field.get(notification);
                        Method method = extraNotification.getClass().getDeclaredMethod("setMessageCount", int.class);
                        method.invoke(extraNotification, count);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mNotificationManager.notify(0, notification);
                }
                break;
                case VIVO:
                    break;
                case OPPO:
                    break;
                case LG:
                case SAMSUNG:{
                    Intent intent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
                    intent.putExtra("badge_count", count);
                    intent.putExtra("badge_count_package_name", context.getPackageName());
                    intent.putExtra("badge_count_class_name", launcherClassName);

                    if (canResolveBroadcast(context, intent)) {
                        context.sendBroadcast(intent);
                    } else {
                        //throw new Exception(UNABLE_TO_RESOLVE_INTENT_ERROR_ + intent.toString());
                    }
                }
                break;
                case MEIZU:
                    break;
                case SONY: {
                    if (launcherClassName == null) {
                        return;
                    }
                    Intent intent = new Intent();
                    intent.setAction("com.sonyericsson.home.action.UPDATE_BADGE");
                    intent.putExtra("com.sonyericsson.home.intent.extra.badge.SHOW_MESSAGE", true);
                    intent.putExtra("com.sonyericsson.home.intent.extra.badge.ACTIVITY_NAME", launcherClassName);
                    intent.putExtra("com.sonyericsson.home.intent.extra.badge.MESSAGE", String.valueOf(count));
                    intent.putExtra("com.sonyericsson.home.intent.extra.badge.PACKAGE_NAME", context.getPackageName());
                    context.sendBroadcast(intent);
                }
                    break;
                case HTC:{
                    Intent intent = new Intent("com.htc.launcher.action.SET_NOTIFICATION");
                    ComponentName localComponentName = new ComponentName(context.getPackageName(), launcherClassName);
                    intent.putExtra("com.htc.launcher.extra.COMPONENT", localComponentName.flattenToShortString());
                    intent.putExtra("com.htc.launcher.extra.COUNT", count);
                    context.sendBroadcast(intent);

                    Intent intentShortcut = new Intent("com.htc.launcher.action.UPDATE_SHORTCUT");
                    intentShortcut.putExtra("packagename", context.getPackageName());
                    intentShortcut.putExtra("count", count);
                    context.sendBroadcast(intentShortcut);
                }
                break;
                case NOVA:{
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("tag", context.getPackageName() + "/" + launcherClassName);
                    contentValues.put("count", count);
                    context.getContentResolver().insert(Uri.parse("content://com.teslacoilsw.notifier/unread_count"),contentValues);
                }
                break;
                case "unknown":
                case GOOGLE:{
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        Intent intent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
                        intent.putExtra("badge_count", count);
                        intent.putExtra("badge_count_package_name", context.getPackageName());
                        intent.putExtra("badge_count_class_name", launcherClassName);
                        context.sendBroadcast(intent);
                    }
                    break;
                }

                default:
                    ;//throw new Exception(NOT_SUPPORT_MANUFACTURER_ + Build.MANUFACTURER);
            }
    }

    public static void applyTheme(Context context, String colorTheme){
        //берем из настроек и применяем
        String pref_colorThemeString=PreferenceManager.getDefaultSharedPreferences(context).getString(colorTheme, "AppTheme");
        //context.setTheme(context.getResources().getIdentifier(pref_colorThemeString, "style", context.getPackageName()));
        context.getTheme().applyStyle(context.getResources().getIdentifier(pref_colorThemeString, "style", context.getPackageName()), true);
    }
}
