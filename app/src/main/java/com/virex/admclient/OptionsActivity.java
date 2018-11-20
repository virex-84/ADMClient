package com.virex.admclient;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.AlertDialog;

import com.virex.admclient.repository.MyRepository;

//import android.support.v14.preference.PreferenceFragment;


/**
 * Класс настроек
 */
public class OptionsActivity extends PreferenceActivity
{
    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content,  new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.options);
        }

        /*
        @Override
        public void onCreatePreferences(Bundle bundle, String s) {

        }
        */

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

            //редактирование анкеты
            if (preference.getKey().equals("pref_edit_anketa")){
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.delphimaster.ru/anketa/index.html"));
                startActivity(intent);
                return false;
            }

            //очистка базы
            if (preference.getKey().equals("pref_dbclear")){
                Context context = preference.getContext();
                Utils.setColoredTheme(context);
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                alertDialog
                        .setMessage(getString(R.string.clear_database_dialog_message))
                        .setPositiveButton(getString(R.string.cancel), null)
                        .setNegativeButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                MyRepository myRepository=new MyRepository(getActivity());
                                myRepository.clearDataBase();

                                //очищаем историю активити, переходим в "корневой" - форумы
                                Intent intent = new Intent(getActivity(),ForumActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                });
                alertDialog.show();
            }

            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
    }
}