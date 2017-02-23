package com.eskeptor.openTextViewer;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;


import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Created by eskeptor on 17. 1. 28.
 * Copyright (C) 2017 Eskeptor(Jeon Ye Chan)
 */

public class MainActivity extends AppCompatActivity
{
    private long backPressedTime;
    private String curFolderURL;
    private SwipeRefreshLayout refreshLayout;
    private GridView curFolderGridView;
    private TextFileAdaptor curFileAdapter;
    private ArrayList<TextFile> curFolderFileList;
    private Drawable folderIcon;
    private AlertDialog.Builder dialog;
    private AdapterView.OnItemClickListener clickListener;
    private AdapterView.OnItemLongClickListener longClickListener;
    private Context context_this;
    private FloatingActionButton fab;

    private static SharedPreferences pref;
    private static SharedPreferences.Editor editor;

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(item.getItemId() == android.R.id.home)
        {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.setClass(context_this, FolderActivity.class);
            startActivityForResult(intent, Constant.REQUEST_CODE_OPEN_FOLDER);
            overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
        }
        else if(item.getItemId() == R.id.menu_main_settings)
        {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.setClass(context_this, SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.anim_slide_in_top, R.anim.anim_slide_out_bottom);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK)
        {
            if(requestCode == Constant.REQUEST_CODE_OPEN_FOLDER)
            {
                curFolderURL = data.getStringExtra(Constant.INTENT_EXTRA_CURRENT_FOLDERURL);
                curFolderFiles();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == Constant.REQUEST_CODE_APP_PERMISSION_STORAGE)
        {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED)
            {
                dialog = new AlertDialog.Builder(this);
                dialog.setTitle(R.string.dialog_main_restart_title);
                dialog.setMessage(R.string.dialog_main_restart_context);
                DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which)
                        {
                            case AlertDialog.BUTTON_POSITIVE:
                            {
                                ActivityCompat.finishAffinity(MainActivity.this);
                                System.exit(0);
                                break;
                            }
                        }
                        dialog.dismiss();
                    }
                };
                dialog.setPositiveButton(R.string.dialog_settings_info_ok, clickListener);
                dialog.show();
                Log.i("Debug", "Permission Allowed");
            }
            else
            {
                dialog = new AlertDialog.Builder(this);
                dialog.setTitle(R.string.dialog_main_restart_title_no);
                dialog.setMessage(R.string.dialog_main_restart_context_no);
                DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(which == AlertDialog.BUTTON_POSITIVE)
                        {
                            ActivityCompat.finishAffinity(MainActivity.this);
                            System.exit(0);
                        }
                        dialog.dismiss();
                    }
                };
                dialog.setPositiveButton(R.string.dialog_settings_info_ok, clickListener);
                dialog.show();
                Log.i("Debug", "Permission Denied");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context_this = getApplicationContext();

        pref = getSharedPreferences(Constant.APP_SETTINGS_PREFERENCE, MODE_PRIVATE);
        editor = pref.edit();

        if(Build.VERSION.SDK_INT >= 21)
        {
            folderIcon = getResources().getDrawable(R.drawable.ic_folder_open_white, null);
        }
        else
        {
            folderIcon = getResources().getDrawable(R.drawable.ic_folder_open_white);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(folderIcon);

        curFolderGridView = (GridView)findViewById(R.id.main_curFolderFileList);
        curFolderFileList = new ArrayList<>();
        refreshLayout = (SwipeRefreshLayout)findViewById(R.id.main_swipeRefresh);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh()
            {
                curFolderFiles();
                refreshLayout.setRefreshing(false);
            }
        });

        clickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.setClass(context_this, MemoActivity.class);
                intent.putExtra(Constant.INTENT_EXTRA_MEMO_OPEN_FILEURL, curFolderFileList.get(position).url);
                intent.putExtra(Constant.INTENT_EXTRA_MEMO_OPEN_FILENAME, curFolderFileList.get(position).title);
                intent.putExtra(Constant.INTENT_EXTRA_MEMO_TYPE, Constant.MEMO_TYPE_OPEN_INTERNAL);
                intent.putExtra(Constant.INTENT_EXTRA_MEMO_OPEN_FOLDERURL, curFolderURL);
                startActivity(intent);
                overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
            }
        };

        longClickListener = new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
            {
                deleteFile(position);
                return true;
            }
        };

        fab = (FloatingActionButton) findViewById(R.id.main_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.setClass(context_this, MemoActivity.class);
                intent.putExtra(Constant.INTENT_EXTRA_MEMO_TYPE, Constant.MEMO_TYPE_NEW);
                intent.putExtra(Constant.INTENT_EXTRA_MEMO_OPEN_FOLDERURL, curFolderURL);
                startActivity(intent);
                overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
            }
        });

        checkPermission();
    }

    @Override
    public void onBackPressed()
    {
        long tempTime = System.currentTimeMillis();
        long intervalTime = tempTime - backPressedTime;

        if(0 <= intervalTime && Constant.WAIT_FOR_SECOND >= intervalTime)
        {
            super.onBackPressed();
        }
        else
        {
            backPressedTime = tempTime;
            Toast.makeText(this, getResources().getString(R.string.back_press), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(pref.getBoolean(Constant.APP_FIRST_SETUP_PREFERENCE, Constant.APP_FIRST_EXECUTE))
        {
            curFolderFiles();
        }
    }

    private void checkPermission()
    {
        if(Build.VERSION.SDK_INT >= 23)
        {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            {
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, Constant.REQUEST_CODE_APP_PERMISSION_STORAGE);
                }
                else
                {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, Constant.REQUEST_CODE_APP_PERMISSION_STORAGE);
                }
            }
            else
            {
                defaultFolderCheck();
                curFolderFiles();
                checkFirstExcecute();
            }
        }
        else
        {
            defaultFolderCheck();
            curFolderFiles();
            checkFirstExcecute();
        }
    }

    private void defaultFolderCheck()
    {
        File file = new File(Constant.APP_INTERNAL_URL);
        if(!file.exists())
        {
            file.mkdir();
        }
        file = new File(Constant.APP_INTERNAL_URL + File.separator+ Constant.FOLDER_DEFAULT_NAME);
        if(!file.exists())
        {
            file.mkdir();
        }
        curFolderURL = file.getPath();
    }

    private void curFolderFiles()
    {
        File file = new File(curFolderURL);
        File files[] = file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname)
            {
                return pathname.getName().endsWith(Constant.FILE_EXTENSION);
            }
        });

        curFolderFileList.clear();
        if(files != null)
        {
            for(int i = 0; i < files.length; i++)
            {
                curFolderFileList.add(new TextFile(files[i], getResources().getString(R.string.file_noname), new SimpleDateFormat(getResources().getString(R.string.file_dateformat))));
            }
        }
        curFileAdapter = new TextFileAdaptor(this, curFolderFileList);
        curFolderGridView.setAdapter(curFileAdapter);
        curFolderGridView.setOnItemClickListener(clickListener);
        curFolderGridView.setOnItemLongClickListener(longClickListener);
    }

    private void deleteFile(final int index)
    {
        dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.dialog_file_title_delete);
        dialog.setMessage(R.string.dialog_file_message_question_delete);
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                if(which == AlertDialog.BUTTON_POSITIVE)
                {
                    File file = new File(curFolderFileList.get(index).url);
                    if(file.exists())
                    {
                        if(file.delete())
                        {
                            Toast.makeText(context_this, getResources().getString(R.string.dialog_file_toast_delete), Toast.LENGTH_SHORT).show();
                            curFolderFiles();
                        }
                        else
                        {
                            Toast.makeText(context_this, "Error code : " + ErrorCode.NO_FOLDER, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                else if(which == AlertDialog.BUTTON_NEGATIVE)
                {

                }
                dialog.dismiss();
            }
        };
        dialog.setNegativeButton(R.string.dialog_folder_button_cancel, clickListener);
        dialog.setPositiveButton(R.string.dialog_folder_button_delete, clickListener);
        dialog.show();
    }

    private void checkFirstExcecute()
    {
        if(!pref.getBoolean(Constant.APP_FIRST_SETUP_PREFERENCE, Constant.APP_FIRST_EXECUTE))
        {
            dialog = new AlertDialog.Builder(this);
            dialog.setTitle(R.string.dialog_main_first_title);
            dialog.setMessage(R.string.dialog_main_first_context);
            DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(which == AlertDialog.BUTTON_POSITIVE)
                    {
                        editor.putBoolean(Constant.APP_FIRST_SETUP_PREFERENCE, Constant.APP_TWICE_EXECUTE);
                        editor.commit();
                    }
                    dialog.dismiss();
                }
            };
            dialog.setPositiveButton(R.string.dialog_settings_info_ok, clickListener);
            dialog.show();
        }
    }
}
