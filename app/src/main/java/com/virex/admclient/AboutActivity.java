package com.virex.admclient;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.Menu;
import android.widget.TextView;

/**
 * Активити "О программе"
 */
public class AboutActivity extends BaseAppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        if (getSupportActionBar()!=null){
            getSupportActionBar().setTitle(getString(R.string.about));
        }

        //версия приложения
        ((TextView)findViewById(R.id.tv_app_version)).setText(BuildConfig.VERSION_NAME);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //не отображаем никаких меню
        return true;
    }
}
