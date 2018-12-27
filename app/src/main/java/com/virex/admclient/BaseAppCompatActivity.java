package com.virex.admclient;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SearchView;

import com.virex.admclient.ui.CheckBoxActionProvider;
import com.virex.admclient.ui.MyResources;

/**
 * Базовый класс
 * Отображает меню, диалоговое окно выхода из приложения
 */
public class BaseAppCompatActivity extends AppCompatActivity {

    private MyResources res;

    SearchManager searchManager;
    SearchView searchView;
    public boolean showSearchMenuItem=false;
    public boolean showBookmarkMenuItem=false;
    public boolean showGoUpDownMenuItem=false;
    public SharedPreferences options;

    int last_pref_colorPrimary=-1;
    public int colorAccent=-1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        options=PreferenceManager.getDefaultSharedPreferences(this);

        Utils.changeOverScrollGlowColor(this);
        Utils.setColoredTheme(this);

        colorAccent=Utils.getColorByAttributeId(this,R.attr.colorAccent);
        last_pref_colorPrimary=colorAccent;

    }

    //хитрый трюк: если цвет поменяли, а активити была неактивной - пересоздаем для применения темы
    //в onCreate
    @Override
    protected void onResume() {
        super.onResume();
        int pref_colorPrimary = options.getInt("pref_colorPrimary", 0);
        if (pref_colorPrimary !=0 && last_pref_colorPrimary!=pref_colorPrimary) {
            recreate();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);

        //ассоциируем настройку поиска с SearchView
        searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        //меню настроек
        Intent intent = new Intent(this, OptionsActivity.class);
        MenuItem menuItem = menu.findItem(R.id.options);
        menuItem.setIntent(intent);

        //меню поиска текста
        MenuItem searchMenuItem = menu.findItem(R.id.search);
        searchMenuItem.setVisible(showSearchMenuItem);

        //меню фильтра "закладка"
        MenuItem checkBookmarkMenuItem = menu.findItem(R.id.check_bookmark);
        checkBookmarkMenuItem.setVisible(showBookmarkMenuItem);
        checkBookmarkMenuItem.getActionView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox checkBox = v.findViewById(R.id.checkbox);
                if (checkBox!=null) onBookmarkCheck(checkBox.isChecked());
            }
        });

        CheckBoxActionProvider checkBoxActionProvider=(CheckBoxActionProvider)MenuItemCompat.getActionProvider(checkBookmarkMenuItem);
        checkBoxActionProvider.setOnClick(new CheckBoxActionProvider.OnClickListener() {
            @Override
            public void onCheckedChanged(boolean isChecked) {
                onBookmarkCheck(isChecked);
            }
        });

        //меню перехода на первый пост
        MenuItem goUp = menu.findItem(R.id.goUp);
        goUp.setVisible(showGoUpDownMenuItem);
        goUp.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onGoUp();
                return false;
            }
        });

        //меню перехода на последний пост
        MenuItem goBottom = menu.findItem(R.id.goBottom);
        goBottom.setVisible(showGoUpDownMenuItem);
        goBottom.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onGoBottom();
                return false;
            }
        });

        return true;
    }

    //метод поиска текста - задается в \res\menu\options_menu.xml
    public void onSearchItemClick(MenuItem item) {
        final SearchView searchView = (SearchView) item.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.onActionViewExpanded();
        searchView.setFocusable(true);
        searchView.requestFocusFromTouch();

        //отслеживаем изменения текста в поисковом поле
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            //нажали "enter" в поиске
            @Override
            public boolean onQueryTextSubmit(String query) {
                onSearchTextSubmit(query);
                return false;
            }

            //изменение текста в поиске
            @Override
            public boolean onQueryTextChange(String query) {
                onSearchTextChange(query);
                return false;
            }
        });

        //закрыли меню поиска "крестиком"
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                //очищаем поиск
                onSearchTextChange("");
                searchView.onActionViewCollapsed();
                return true;
            }
        });

        item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                //разрешаем открыться меню поиска
                return true;
            }

            //меню поиска "схлопнулось"
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                //очищаем поиск
                onSearchTextChange("");
                searchView.onActionViewCollapsed();
                return true;
            }
        });
    }

    //метод открытия "о программе" - задается в \res\menu\options_menu.xml
    public void onAboutItemClick(MenuItem item) {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (searchView!=null){
            if (!searchView.isIconified()) {
                searchView.onActionViewCollapsed();
            }
        } else {
            //если предок - не главное активити - разрешаем нажатие "назад"
            if (!isTaskRoot()) {
                super.onBackPressed();
            } else {
                //иначе - спрашиваем: вы действительно хотите выйти?
                if (!options.getBoolean("pref_confirm_exit_dialog",true)){
                    super.onBackPressed();
                    return;
                }

                View checkBoxView = View.inflate(this, R.layout.checkbox, null);
                CheckBox checkBox = checkBoxView.findViewById(R.id.checkbox);
                checkBox.setChecked(options.getBoolean("pref_confirm_exit_dialog",true));
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        options.edit().putBoolean("pref_confirm_exit_dialog",isChecked).apply();
                    }
                });
                checkBox.setText(getString(R.string.pref_confirm_exit_dialog_message));

                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setCancelable(false);
                dialog.setTitle(this.getTitle());
                dialog.setMessage(getString(R.string.exit_dialog_message));
                dialog.setView(checkBoxView);
                dialog.setPositiveButton(getString(R.string.exit), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                }).setNegativeButton(getString(R.string.cancel),null).show();
            }
        }
    }

    public Snackbar showSnackBarScroll(final RecyclerView recucleview, final int position, String text, String action_text){
        Snackbar snackbar=
        Snackbar.make(recucleview, text, Snackbar.LENGTH_INDEFINITE)
                .setAction(action_text, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //recucleview.smoothScrollToPosition(position); --если постов около 100 то это долго
                        recucleview.scrollToPosition(position);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                recucleview.scrollToPosition(position);
                                recucleview.smoothScrollToPosition(position);
                            }
                        }, 10);

                    }
                });
        snackbar.show();
        return snackbar;
    }

    public Snackbar showSnackBarInfo(final View view, String text, String action_text){
        Snackbar snack=
        Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE)
                .setAction(action_text, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                });
        snack.show();
        return snack;
    }

    //методы поиска, фильтрации (используются предками)
    public void onSearchTextSubmit(String query) {
    }

    public void onSearchTextChange(String query) {
    }

    public void onBookmarkCheck(boolean checked){
    }

    public void onGoUp(){
    }

    public void onGoBottom(){
    }

    /*
    @Override
    public Resources getResources() {
        //return super.getResources();
        if (res == null) {
            res = new MyResources(super.getResources());
        }
        return res;
    }
    */

}
