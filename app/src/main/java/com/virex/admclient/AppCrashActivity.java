package com.virex.admclient;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Активити вызываемое при необработанном исключении в программе
 */
public class AppCrashActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_appcrash);

        TextView error_message = findViewById(R.id.error_message);
        if (error_message!=null) error_message.setText(getIntent().getStringExtra("error_message"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish(); //теряем "фокус" - программа открывается заново
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
