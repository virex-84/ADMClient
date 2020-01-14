package com.virex.admclient;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Toast;

import com.google.gson.Gson;
import com.virex.admclient.repository.MyRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.virex.admclient.Utils.URLEncodeString;


/**
 * Класс настроек
 */
public class OptionsActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //устанавливаем текущую тему (с актуальным colorPrimary)
        Utils.setColoredTheme(this);
        //форма
        setContentView(R.layout.activity_options);
        //содержимое
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content,  new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener
    {

        private String SHARED_OPTIONS_LIST_POSITION = "SHARED_OPTIONS_LIST_POSITION";

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            addPreferencesFromResource(R.xml.options);
        }

        @Override
        public boolean onPreferenceTreeClick(androidx.preference.Preference preference) {

            //редактирование анкеты
            if (preference.getKey().equals("pref_edit_anketa")){
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://forum.delphimaster.net/anketa/index.html"));
                startActivity(intent);
                return false;
            }

            //очистка базы
            if (preference.getKey().equals("pref_dbclear")){
                final Context context = preference.getContext();
                Utils.setColoredTheme(context);
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                alertDialog
                        .setMessage(getString(R.string.clear_database_dialog_message))
                        .setPositiveButton(getString(R.string.cancel), null)
                        .setNegativeButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                MyRepository myRepository=new MyRepository(context);
                                myRepository.clearDataBase();

                                //очищаем историю активити, переходим в "корневой" - форумы
                                Intent intent = new Intent(getActivity(),ForumActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                        });
                alertDialog.show();
            }

            if (preference.getKey().equals("pref_check_login")){
                String pref_login= getPreferenceManager().getSharedPreferences().getString("pref_login","");
                String pref_password=getPreferenceManager().getSharedPreferences().getString("pref_password","");
                String login=URLEncodeString(pref_login);
                String password=URLEncodeString(pref_password);
                String edit=URLEncodeString("Редактировать");
                App.getAnketaApi().checkLogin(login,password,edit).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            try {
                                BufferedReader br = new BufferedReader(new InputStreamReader(response.body().byteStream(), "windows-1251"));
                                String line;

                                //в теге pre обычно есть сообщение об ошибке
                                while ((line = br.readLine()) != null) {
                                    String pre = Utils.extractHTMLTag("pre",line);
                                    String h4 = Utils.extractHTMLTag("H4",line);

                                    if (!TextUtils.isEmpty(pre)){
                                        //ошибка
                                        showToast(getContext(), pre,false);
                                        return;
                                    }

                                    if (!TextUtils.isEmpty(h4)){
                                        if (h4.toLowerCase().contains("Редактирование анкеты".toLowerCase())) {
                                            //зашли в редактирование
                                            showToast(getContext(), getString(R.string.check_login_sucess),true);
                                            return;
                                        }
                                    }
                                    /*
                                    String title = Utils.extractHTMLTag("title",line);
                                    if (!TextUtils.isEmpty(title)){
                                        if (title.toLowerCase().contains("Error !".toLowerCase())) {
                                            //ошибка

                                        }
                                    }
                                    */
                                }
                            } catch (IOException e) {
                            }
                        } else {
                            String text= getString(R.string.check_login_failure);
                            text=String.format(Locale.ENGLISH,"%s: [%d] %s",text,response.code(), response.message());
                            showToast(getContext(),text,false);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        showToast(getContext(), getString(R.string.check_login_failure),false);
                    }
                });
            }

            return super.onPreferenceTreeClick(preference);
        }

        @Override
        public void onResume() {
            super.onResume();
            // Set up a listener whenever a key changes
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

            //восстанавливаем положение списка
            String pos = getPreferenceScreen().getSharedPreferences().getString(SHARED_OPTIONS_LIST_POSITION,"");
            LinearLayoutManager.SavedState position =new Gson().fromJson(pos, LinearLayoutManager.SavedState.class);
            try {
                getListView().getLayoutManager().onRestoreInstanceState(position);
            } catch (Exception ignore){
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            // Set up a listener whenever a key changes
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

            //сохраняем позицию списка
            try {
                String pos=new Gson().toJson(getListView().getLayoutManager().onSaveInstanceState());
                getPreferenceScreen().getSharedPreferences().edit().putString(SHARED_OPTIONS_LIST_POSITION, pos).apply();
            } catch (Exception ignore){
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("pref_colorPrimary")){
                this.getActivity().recreate();
            }
            if (key.equals("pref_set_dark_theme")){
                this.getActivity().recreate();
            }
        }

        public void showToast(Context context, String text, boolean isSuccess) {

/*

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.toast_message, null);

            ImageView toast_image = layout.findViewById(R.id.toast_image);

            Drawable drawable;
            if (isSuccess){
                //!!! R.drawable.ic_done должен быть без android:tint="?attr/colorPrimary"
                drawable = AppCompatResources.getDrawable(context, R.drawable.ic_done);

                //DrawableCompat.setTint(drawable, R.color.colorAccent);
                //toast_image.setImageDrawable(drawable);
                //toast_image.setImageResource(R.drawable.ic_done);
                //toast_image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_done));
                //toast_image.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_bookmark,null));
            } else {
                //toast_image.setImageResource(R.drawable.ic_error);
                drawable = AppCompatResources.getDrawable(context, R.drawable.ic_error);
            }

            //int colorAccent= Utils.getColorByAttributeId(context,R.attr.colorAccent);
            if (drawable!=null) {
                //drawable.mutate().setColorFilter(colorAccent, PorterDuff.Mode.SRC_IN);
                toast_image.setImageDrawable(drawable);
            }

            TextView toast_text = layout.findViewById(R.id.toast_text);
            toast_text.setText(text);

            Toast toast = new Toast(context);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);
            toast.show();
*/

            Toast toast = Toast.makeText(context,text, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();

        }
    }

}