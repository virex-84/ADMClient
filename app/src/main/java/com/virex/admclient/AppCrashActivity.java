package com.virex.admclient;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Активити вызываемое при необработанном исключении в программе
 */
public class AppCrashActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_appcrash);

        TextView error_message = findViewById(R.id.error_message);
        if (error_message!=null) error_message.setText(getIntent().getStringExtra("error_message"));

        Button btn_show_error = findViewById(R.id.btn_show_error);
        if (btn_show_error!=null) btn_show_error.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (error_message!=null){
                    if (error_message.getVisibility()!=View.VISIBLE)
                        error_message.setVisibility(View.VISIBLE);
                    else
                        error_message.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish(); //теряем "фокус" - программа открывается заново
        //android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        //android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }
}