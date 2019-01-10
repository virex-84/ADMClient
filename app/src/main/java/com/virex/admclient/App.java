package com.virex.admclient;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDelegate;
import android.util.TypedValue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import com.virex.admclient.network.AnketaWebService;
import com.virex.admclient.network.ForumWebService;
import com.virex.admclient.network.PageWebService;
import com.virex.admclient.network.TopicWebService;
import com.virex.admclient.repository.PagesWorker;
import com.virex.admclient.ui.MyResources;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Класс Application
 */
public class App extends Application implements SharedPreferences.OnSharedPreferenceChangeListener {

    //позволяет разрешить использование векторной графики в стилях
    //см: res\values\styles.xml
    //см: res\drawable\bookmark_checkbox.xml
    static
    {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

    }

    SharedPreferences options;

    private static ForumWebService forumApi;
    private static TopicWebService topicApi;
    private static PageWebService pageApi;
    private static AnketaWebService anketaApi;

    public static ForumWebService getForumApi() {
        return forumApi;
    }
    public static TopicWebService getTopicApi() {
        return topicApi;
    }
    public static PageWebService getPageApi() {
        return pageApi;
    }
    public static AnketaWebService getAnketaApi() {
        return anketaApi;
    }

    Resources resources;

    @Override
    public void onCreate() {
        super.onCreate();

        final String userAgent="ADMClient.".concat(BuildConfig.VERSION_NAME);

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(new Interceptor() {
                                      @Override
                                      public Response intercept(@NonNull Interceptor.Chain chain) throws IOException {
                                          Request original = chain.request();

                                          Request request = original.newBuilder()
                                                  .header("User-Agent", userAgent)
                                                  //.addHeader("Content-Type", "text/html; charset=windows-1251")
                                                  .method(original.method(), original.body())
                                                  .build();
                                          return chain.proceed(request);
                                      }
                                  });
        OkHttpClient client = httpClient.build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://www.delphimaster.ru") //Базовая часть адреса
                .addConverterFactory(GsonConverterFactory.create()) //Конвертер, необходимый для преобразования JSON'а в объекты при POST запросах
                .client(client)
                .build();

        //создаем объект, при помощи которого будем выполнять запросы
        forumApi = retrofit.create(ForumWebService.class);
        topicApi = retrofit.create(TopicWebService.class);
        pageApi = retrofit.create(PageWebService.class);
        anketaApi = retrofit.create(AnketaWebService.class);

        //отлавливаем необработанные исключения
        final Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                handleUncaughtException(thread, throwable);
                handler.uncaughtException(thread, throwable);
            }
        });

        //инициализируем настройки
        options = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        options.registerOnSharedPreferenceChangeListener(this);

        //инициализируем периодическую работу
        boolean reInicializeWorkers = options.getBoolean("pref_update_bookmarked_topics", false);
        reInicializeWorkers(reInicializeWorkers);

        boolean set_dark_theme = options.getBoolean("pref_set_dark_theme", false);
        Utils.changeTheme(set_dark_theme);

        super.onCreate();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        options.unregisterOnSharedPreferenceChangeListener(this);
    }

    /*
    @Override
    public Resources.Theme getTheme() {
        boolean set_dark_theme = options.getBoolean("pref_set_dark_theme", false);

        Resources.Theme theme= getResources().newTheme();
        if (set_dark_theme) {
            theme.applyStyle(R.style.AppThemeDark,true);
        } else {
            theme.applyStyle(R.style.AppTheme,true);
        }

        return theme;
    }
    */

    public void reInicializeWorkers(boolean loadBookmarkedTopicsFromNetwork){
        if (loadBookmarkedTopicsFromNetwork) {
            //передаем данные задаче
            Data data = new Data.Builder()
                    .putInt(PagesWorker.EXTRA_ACTION, PagesWorker.ACTION_LOAD_BOOKMARKED_TOPICS_FROM_NETWORK)
                    .build();
            //критерии запуска задачи
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED) //обязательно должен быть подключен интернет
                    .build();
            //переодическая задача с минимальным интервалом (15 минут)
            PeriodicWorkRequest periodicRequest = new PeriodicWorkRequest.Builder(PagesWorker.class, PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MINUTES)
                    .setInputData(data)
                    .setConstraints(constraints)
                    .build();
            //ExistingPeriodicWorkPolicy.REPLACE - можно прервать загрузку
            WorkManager.getInstance().enqueueUniquePeriodicWork("loadBookmarkedTopicsFromNetwork", ExistingPeriodicWorkPolicy.REPLACE, periodicRequest);
        } else {
            WorkManager.getInstance().cancelAllWorkByTag("loadBookmarkedTopicsFromNetwork");
        }
    }

    //выцепляем из всего стека записи о своих модулях (.java файлах)
    private String extractSelfCause(Throwable cause, String searchPackage){
        StackTraceElement[] items=cause.getStackTrace();
        String result="";

        for (int i = 0; i < items.length; i++) {
            StackTraceElement item = items[i];
            if (item.getClassName().contains(searchPackage)){
                result=result.concat("\n");
                result=result.concat(String.format("filename=%s\n", item.getFileName()));
                result=result.concat(String.format("class=%s\n", item.getClassName()));
                result=result.concat(String.format("method=%s\n", item.getMethodName()));
                result=result.concat(String.format("line=%s\n", item.getLineNumber()));
            }
        }

        return result;
    }

    //получаем текст ошибки и открываем новое активити
    public void handleUncaughtException (Thread thread, Throwable exception)
    {
        exception.printStackTrace();

        String LINE_SEPARATOR = "\n";

        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        StringBuilder errorReport = new StringBuilder();

        errorReport.append("************ CAUSE OF ERROR ************");
        errorReport.append(LINE_SEPARATOR);
        errorReport.append(stackTrace.toString());

        Throwable cause=exception.getCause();
        if(cause==null) cause=exception;
        if(cause!=null) {
            errorReport.append(extractSelfCause(cause,  "virex"));
            errorReport.append(LINE_SEPARATOR);

            errorReport.append(String.format("error=%s\n", cause.getLocalizedMessage()));
            errorReport.append(LINE_SEPARATOR);
        }

        errorReport.append("************ DEVICE INFORMATION ***********");
        errorReport.append(LINE_SEPARATOR);

        errorReport.append(String.format("Brand: %s\n",Build.BRAND));
        errorReport.append(String.format("Device: %s\n",Build.DEVICE));
        errorReport.append(String.format("Model: %s\n",Build.MODEL));
        errorReport.append(String.format("ID: %s\n",Build.ID));
        errorReport.append(String.format("Product: %s\n",Build.PRODUCT));
        errorReport.append(LINE_SEPARATOR);

        errorReport.append("************ FIRMWARE ************");
        errorReport.append(LINE_SEPARATOR);
        errorReport.append(String.format("SDK: %s\n",Build.VERSION.SDK));
        errorReport.append(String.format("Release: %s\n",Build.VERSION.RELEASE));
        errorReport.append(String.format("Incremental: %s\n",Build.VERSION.INCREMENTAL));

        //запуск своего окна ошибки
        Intent intent = new Intent(getApplicationContext(), AppCrashActivity.class);
        intent.putExtra("error_message", errorReport.toString());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK  | Intent.FLAG_ACTIVITY_CLEAR_TOP); //FLAG_ACTIVITY_CLEAR_TOP - при resume открывает главное окно
        startActivity(intent);

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }

    //перехватываем изменение настроек
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.equals("pref_update_bookmarked_topics")) {
            boolean reInicializeWorkers = sharedPreferences.getBoolean(key, false);
            reInicializeWorkers(reInicializeWorkers);
        }

        if (key.equals("pref_set_dark_theme")) {
            boolean set_dark_theme = sharedPreferences.getBoolean(key, false);
            /*
            if (set_dark_theme){
                setTheme(R.style.AppThemeDark);
            } else {
                setTheme(R.style.AppTheme);
            }
            */
            Utils.changeTheme(set_dark_theme);
        }


        if (key.equals("pref_colorPrimary")) {
            /*
            int pref_colorPrimary = sharedPreferences.getInt(key, 0);
            //String pref_colorPrimaryString="002d00";
            //очищаем от alpha
            int red = Color.red(pref_colorPrimary);
            int green = Color.green(pref_colorPrimary);
            int blue = Color.blue(pref_colorPrimary);
            pref_colorPrimary=Color.argb(0,red, green, blue);
            String pref_colorPrimaryString=String.format("%X", pref_colorPrimary).toLowerCase();

            getApplicationContext().setTheme(getApplicationContext().getResources().getIdentifier("T_" + pref_colorPrimaryString, "style", getApplicationContext().getPackageName()));
            */
        }


    }

    /*
    @Override
    public Resources getResources() {
        //return super.getResources();
        if (resources==null){
            resources=new MyResources(super.getResources());
        }
        return resources;
    }
    */



    @Override
    public AssetManager getAssets() {
        return super.getAssets();
    }
}
