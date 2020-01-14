package com.virex.admclient.repository;

import android.content.Context;
import androidx.annotation.NonNull;

import com.virex.admclient.App;
import com.virex.admclient.Utils;
import com.virex.admclient.db.database.AppDataBase;
import com.virex.admclient.db.entity.Forum;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import androidx.work.Worker;
import androidx.work.WorkerParameters;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * "Воркер" для загрузки форумов в БД
 */
public class ForumsWorker extends Worker {

    private AppDataBase database = AppDataBase.getAppDatabase(getApplicationContext());

    public ForumsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        try {
            Response<ResponseBody> result=App.getForumApi().getForums(1).execute();
            if (result.isSuccessful()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(result.body().byteStream(), "windows-1251"));
                String line;
                while ((line = br.readLine()) != null) {
                    Forum forum = Utils.parceLineToObject(Forum.class, line);
                    if (forum != null) database.forumDao().insert(forum);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Result.success();
    }
}
