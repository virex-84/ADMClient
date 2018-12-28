package com.virex.admclient;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import com.virex.admclient.repository.MyRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.virex.admclient.Utils.URLEncodeString;


/**
 * Класс настроек
 */
public class OptionsActivity extends PreferenceActivity
{
    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //устанавливаем текущую тему (с актуальным colorPrimary)
        Utils.setColoredTheme(this);
        getFragmentManager().beginTransaction().replace(android.R.id.content,  new MyPreferenceFragment()).commit();
    }

    @Nullable
    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {

        final View result = super.onCreateView(name, context, attrs);
        if (result != null) {
            return result;
        }

        //создаем элементы AppCompat
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            switch (name) {
                case "TextView":
                    return new AppCompatTextView(this, attrs);
                case "EditText":
                    return new AppCompatEditText(this, attrs);
                case "Spinner":
                    return new AppCompatSpinner(this, attrs);
                case "CheckBox":
                    return new AppCompatCheckBox(this, attrs);
                case "RadioButton":
                    return new AppCompatRadioButton(this, attrs);
                case "CheckedTextView":
                    return new AppCompatCheckedTextView(this, attrs);
            }
        }

        return null;
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

            if (preference.getKey().equals("pref_check_login")){
                String pref_login= getPreferenceManager().getSharedPreferences().getString("pref_login","");
                String pref_password=getPreferenceManager().getSharedPreferences().getString("pref_password","");
                String login=URLEncodeString(pref_login);
                String password=URLEncodeString(pref_password);
                String edit=URLEncodeString("Редактировать");
                ((App)getActivity().getApplication()).getAnketaApi().checkLogin(login,password,edit).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
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
                                        showToast(getActivity().getApplicationContext(), pre,false);
                                        return;
                                    }

                                    if (!TextUtils.isEmpty(h4)){
                                        if (h4.toLowerCase().contains("Редактирование анкеты".toLowerCase())) {
                                            //зашли в редактирование
                                            showToast(getActivity().getApplicationContext(), getString(R.string.check_login_sucess),true);
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
                            text=String.format("%s: [%d] %s",text,response.code(), response.message());
                            showToast(getActivity().getApplicationContext(),text,true);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        showToast(getActivity().getApplicationContext(), getString(R.string.check_login_failure),false);
                    }
                });
            }

            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        public static void showToast(Context context, String text, boolean isSuccess) {
            /*
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.toast_message, null);

            ImageView toast_image = layout.findViewById(R.id.toast_image);
            if (isSuccess){
                toast_image.setImageResource(R.drawable.ic_done);
            } else {
                toast_image.setImageResource(R.drawable.ic_error);
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