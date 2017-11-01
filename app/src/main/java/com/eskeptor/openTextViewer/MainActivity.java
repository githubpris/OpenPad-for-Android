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
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import com.eskeptor.openTextViewer.datatype.MainFileObject;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * Created by eskeptor on 17. 1. 28.
 * Copyright (C) 2017 Eskeptor(Jeon Ye Chan)
 */

public class MainActivity extends AppCompatActivity {
    private long mBackPressedTime;
    private String mCurFolderURL;
    private SwipeRefreshLayout mRefreshLayout;
    private StaggeredGridLayoutManager mLayoutManager;
    private RecyclerView mCurFolderGridView;
    private MainFileAdaptor mCurFileAdapter;
    private ArrayList<MainFileObject> mCurFolderFileList;
    private Runnable mRefreshListRunnable;
    private Context mContextThis;
    private View mContextView;
    private FloatingActionButton mFloatingActionButton;

    private AdView mAdView;

    private static SharedPreferences mSharedPref;
    private static SharedPreferences.Editor mSharedPrefEditor;


    @Override
    public boolean onCreateOptionsMenu(Menu _menu) {
        getMenuInflater().inflate(R.menu.menu_main, _menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem _item) {
        int id = _item.getItemId();
        switch (id) {
            case android.R.id.home:
                Intent intent1 = new Intent();
                intent1.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent1.setClass(mContextThis, FolderActivity.class);
                startActivityForResult(intent1, Constant.REQUEST_CODE_OPEN_FOLDER);
                overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
                break;
            case R.id.menu_main_settings:
                Intent intent2 = new Intent();
                intent2.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent2.setClass(mContextThis, SettingsActivity.class);
                startActivity(intent2);
                overridePendingTransition(R.anim.anim_slide_in_top, R.anim.anim_slide_out_bottom);
                break;
        }
        return super.onOptionsItemSelected(_item);
    }

    @Override
    protected void onActivityResult(int _requestCode, int _resultCode, Intent _data) {
        if (_resultCode == RESULT_OK) {
            if (_requestCode == Constant.REQUEST_CODE_OPEN_FOLDER) {
                mCurFolderURL = _data.getStringExtra(Constant.INTENT_EXTRA_CURRENT_FOLDERURL);
                if (mCurFolderURL != null) {
                    refreshList();
                    mCurFileAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int _requestCode, @NonNull String[] _permissions, @NonNull int[] _grantResults) {
        super.onRequestPermissionsResult(_requestCode, _permissions, _grantResults);
        if (_requestCode == Constant.REQUEST_CODE_APP_PERMISSION_STORAGE) {
            if (_grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    _grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setTitle(R.string.main_dialog_restart_title);
                dialog.setMessage(R.string.main_dialog_restart_context);
                DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface _dialog, int _which) {
                        switch (_which) {
                            case AlertDialog.BUTTON_POSITIVE: {
                                ActivityCompat.finishAffinity(MainActivity.this);
                                System.exit(0);
                                break;
                            }
                        }
                        _dialog.dismiss();
                    }
                };
                dialog.setPositiveButton(R.string.settings_dialog_info_ok, clickListener);
                dialog.show();
            } else {
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setTitle(R.string.main_dialog_restart_title_no);
                dialog.setMessage(R.string.main_dialog_restart_context_no);
                DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface _dialog, int _which) {
                        if (_which == AlertDialog.BUTTON_POSITIVE) {
                            ActivityCompat.finishAffinity(MainActivity.this);
                            System.exit(0);
                        }
                        _dialog.dismiss();
                    }
                };
                dialog.setPositiveButton(R.string.settings_dialog_info_ok, clickListener);
                dialog.show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        setContentView(R.layout.activity_main);

        mContextThis = getApplicationContext();
        mContextView = findViewById(R.id.content_main);

        mSharedPref = getSharedPreferences(Constant.APP_SETTINGS_PREFERENCE, MODE_PRIVATE);
        mSharedPrefEditor = mSharedPref.edit();

        // 튜토리얼 테스트
        if (!mSharedPref.getBoolean(Constant.APP_TUTORIAL, false)) {
            Intent intent = new Intent();
            intent.setClass(mContextThis, FirstStartActivity.class);
            startActivity(intent);
        }

        Drawable folderIcon;
        if (Build.VERSION.SDK_INT >= 21) {
            folderIcon = getResources().getDrawable(R.drawable.ic_folder_open_white, null);
        } else {
            folderIcon = getResources().getDrawable(R.drawable.ic_folder_open_white);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(folderIcon);
        }

        mCurFolderGridView = (RecyclerView) findViewById(R.id.main_curFolderFileList);
        mCurFolderFileList = new ArrayList<>();
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.main_swipeRefresh);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mCurFileAdapter.notifyDataSetChanged();
                mRefreshLayout.setRefreshing(false);
            }
        });
        mLayoutManager = new StaggeredGridLayoutManager(2, 1);

        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.main_add);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            Context wrapper = new ContextThemeWrapper(mContextThis, R.style.AppTheme);
            PopupMenu addFabMenu;
            MenuInflater menuInflater;
            Menu menu;

            @Override
            public void onClick(View view) {
                if (addFabMenu == null && menuInflater == null && menu == null) {
                    addFabMenu = new PopupMenu(wrapper, view);
                    menuInflater = addFabMenu.getMenuInflater();
                    menu = addFabMenu.getMenu();
                    menuInflater.inflate(R.menu.menu_main_add, menu);
                }
                addFabMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem _item) {
                        int id = _item.getItemId();
                        switch (id) {
                            case R.id.menu_main_add_text:
                                Intent intent1 = new Intent();
                                intent1.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                intent1.setClass(mContextThis, MemoActivity.class);
                                intent1.putExtra(Constant.INTENT_EXTRA_MEMO_OPEN_FOLDERURL, mCurFolderURL);
                                startActivity(intent1);
                                overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
                                break;
                            default:
                                Intent intent2 = new Intent();
                                intent2.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                intent2.setClass(mContextThis, PaintActivity.class);
                                intent2.putExtra(Constant.INTENT_EXTRA_MEMO_OPEN_FOLDERURL, mCurFolderURL);
                                startActivity(intent2);
                                overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
                                break;
                        }
                        return false;
                    }
                });
                addFabMenu.show();
            }
        });

        mRefreshListRunnable = new Runnable() {
            @Override
            public void run() {
                refreshList();
                mCurFileAdapter = new MainFileAdaptor(mCurFolderFileList);
                mCurFileAdapter.setClickAction(new ClickAction() {
                    @Override
                    public void onClick(View _view, int _position) {
                        Intent intent = new Intent();
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        if (mCurFolderFileList.get(_position).mFileType == Constant.LISTVIEW_FILE_TYPE_IMAGE) {
                            intent.setClass(mContextThis, PaintActivity.class);
                        } else {
                            intent.setClass(mContextThis, MemoActivity.class);
                        }
                        intent.putExtra(Constant.INTENT_EXTRA_MEMO_OPEN_FILEURL, mCurFolderFileList.get(_position).mFilePath);
                        intent.putExtra(Constant.INTENT_EXTRA_MEMO_OPEN_FILENAME, mCurFolderFileList.get(_position).mFileTitle);
                        intent.putExtra(Constant.INTENT_EXTRA_MEMO_OPEN_FOLDERURL, mCurFolderURL);
                        startActivity(intent);
                        overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
                    }

                    @Override
                    public void onLongClick(View _view, int _position) {
                        deleteFile(_position);
                    }
                });
                mLayoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
                mLayoutManager.invalidateSpanAssignments();
                mCurFolderGridView.setHasFixedSize(true);
                mCurFolderGridView.setLayoutManager(mLayoutManager);
                mCurFolderGridView.setAdapter(mCurFileAdapter);
                mCurFolderGridView.addItemDecoration(new RecyclerViewPadding(10, 5, 5));
            }
        };
        checkPermission();
    }

    @Override
    public void onBackPressed() {
        long tempTime = System.currentTimeMillis();
        long intervalTime = tempTime - mBackPressedTime;

        if (0 <= intervalTime && Constant.WAIT_FOR_SECOND >= intervalTime) {
            super.onBackPressed();
        } else {
            mBackPressedTime = tempTime;
            Snackbar.make(mContextView, R.string.back_press, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSharedPref.getBoolean(Constant.APP_FIRST_SETUP_PREFERENCE, Constant.APP_FIRST_EXECUTE)) {
            refreshList();
            mCurFileAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRefreshLayout = null;
        mLayoutManager = null;
        mCurFolderGridView = null;
        mCurFileAdapter = null;
        if (!mCurFolderFileList.isEmpty())
            mCurFolderFileList.clear();
        mCurFolderFileList = null;
        mRefreshListRunnable = null;
        mContextThis = null;
        if (mAdView != null)
            mAdView = null;
        mSharedPref = null;
        mSharedPrefEditor = null;
        mCurFolderURL = null;
        mFloatingActionButton = null;
        mContextView = null;
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, Constant.REQUEST_CODE_APP_PERMISSION_STORAGE);
                } else {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, Constant.REQUEST_CODE_APP_PERMISSION_STORAGE);
                }
            } else {
                defaultFolderCheck();
                runOnUiThread(mRefreshListRunnable);
                checkFirstExcecute();
                adMob();
            }
        } else {
            defaultFolderCheck();
            runOnUiThread(mRefreshListRunnable);
            checkFirstExcecute();
            adMob();
        }
    }

    // 기본폴더 체크 메소드
    private void defaultFolderCheck() {
        // 어플의 기본 폴더 체크
        File file = new File(Constant.APP_INTERNAL_URL);
        if (!file.exists()) {
            file.mkdir();
        }

        // 어플의 위젯용 폴더 체크
        file = new File(Constant.APP_WIDGET_URL);
        if (!file.exists()) {
            file.mkdir();
        }

        // 어플의 기본 메모 폴더 체크
        file = new File(Constant.APP_INTERNAL_URL + File.separator + Constant.FOLDER_DEFAULT_NAME);
        if (!file.exists()) {
            file.mkdir();
        }
        mCurFolderURL = file.getPath();
    }

    // 파일 제거 메소드
    private void deleteFile(final int _index) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.file_dialog_title_delete);
        dialog.setMessage(R.string.file_dialog_message_question_delete);
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface _dialog, int _which) {
                if (_which == AlertDialog.BUTTON_POSITIVE) {
                    File file = new File(mCurFolderFileList.get(_index).mFilePath);
                    if (file.exists()) {
                        if (file.delete()) {
                            Snackbar.make(mContextView, R.string.file_dialog_toast_delete, Snackbar.LENGTH_SHORT).show();
                            mCurFolderFileList.remove(_index);
                            mCurFolderGridView.removeViewAt(_index);
                            mCurFileAdapter.notifyItemRemoved(_index);
                            mCurFileAdapter.notifyItemRangeChanged(_index, mCurFolderFileList.size());
                            mCurFileAdapter.notifyDataSetChanged();
                        } else {
                            Snackbar.make(mContextView, R.string.error_folder_not_exist, Snackbar.LENGTH_SHORT).show();
                        }
                    }
                }
                _dialog.dismiss();
            }
        };
        dialog.setNegativeButton(R.string.folder_dialog_button_cancel, clickListener);
        dialog.setPositiveButton(R.string.folder_dialog_button_delete, clickListener);
        dialog.show();
    }

    // 메인 리스트 새로고침 메소드
    private void refreshList() {
        mCurFolderFileList.clear();
        File file = new File(mCurFolderURL);
        File files[] = file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File _pathname) {
                return _pathname.getName().endsWith(Constant.FILE_TEXT_EXTENSION) ||
                        _pathname.getName().endsWith(Constant.FILE_IMAGE_EXTENSION);
            }
        });

        sortFileArray(files);

        if (files != null) {
            for (File newFile : files) {
                if (newFile.getName().charAt(0) == 'w')
                    mCurFolderFileList.add(new MainFileObject(newFile, getResources().getString(R.string.file_noname), getResources().getString(R.string.file_imagememo),
                            Locale.getDefault().getDisplayCountry(), true));
                else
                    mCurFolderFileList.add(new MainFileObject(newFile, getResources().getString(R.string.file_noname), getResources().getString(R.string.file_imagememo),
                            Locale.getDefault().getDisplayCountry(), false));
            }
        }
    }

    // 맨 처음 설치시 확인하는 메소드
    private void checkFirstExcecute() {
        if (!mSharedPref.getBoolean(Constant.APP_FIRST_SETUP_PREFERENCE, Constant.APP_FIRST_EXECUTE)) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(R.string.main_dialog_first_title);
            dialog.setMessage(R.string.main_dialog_first_context);
            DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface _dialog, int _which) {
                    if (_which == AlertDialog.BUTTON_POSITIVE) {
                        mSharedPrefEditor.putBoolean(Constant.APP_FIRST_SETUP_PREFERENCE, Constant.APP_TWICE_EXECUTE);
                        mSharedPrefEditor.commit();
                    }
                    _dialog.dismiss();
                }
            };
            dialog.setPositiveButton(R.string.settings_dialog_info_ok, clickListener);
            dialog.show();
        }

        if (!mSharedPref.getString(Constant.APP_VERSION_CHECK, "1.0.0").equals(Constant.APP_LASTED_VERSION)) {
            InputStream inputStream = null;
            ByteArrayOutputStream byteArrayOutputStream = null;
            String update = "";

            try {
                inputStream = getResources().openRawResource(R.raw.update);
                byteArrayOutputStream = new ByteArrayOutputStream();
                int i;
                while ((i = inputStream.read()) != -1) {
                    byteArrayOutputStream.write(i);
                }
                update = byteArrayOutputStream.toString();
            } catch (Exception e) {
                Log.e("MainActivity(check-)", e.getMessage());
            } finally {
                if (byteArrayOutputStream != null) {
                    try {
                        byteArrayOutputStream.close();
                    } catch (Exception e) {
                        Log.e("MainActivity(check-)", e.getMessage());
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                        Log.e("MainActivity(check-)", e.getMessage());
                    }
                }
            }

            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(R.string.main_dialog_update_title);
            dialog.setMessage(update);
            DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface _dialog, int _which) {
                    if (_which == AlertDialog.BUTTON_POSITIVE) {
                        mSharedPrefEditor.putString(Constant.APP_VERSION_CHECK, Constant.APP_LASTED_VERSION);
                        mSharedPrefEditor.commit();
                    }
                    _dialog.dismiss();
                }
            };
            dialog.setPositiveButton(R.string.settings_dialog_info_ok, clickListener);
            dialog.show();
        }
    }

    // 애드몹 메소드
    private void adMob() {
        if (mSharedPref.getBoolean(Constant.APP_ADMOB_VISIBLE, true)) {
            MobileAds.initialize(mContextThis, getResources().getString(R.string.app_id));
            //adRequest = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build();
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView = (AdView) findViewById(R.id.adView);

            mAdView.setEnabled(true);
            mAdView.setVisibility(View.VISIBLE);
            mAdView.loadAd(adRequest);
            CoordinatorLayout.LayoutParams layoutParams = new CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.WRAP_CONTENT, CoordinatorLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(0, 0, DPtoPixel(16), DPtoPixel(70));
            layoutParams.gravity = Gravity.END | Gravity.BOTTOM;
            mFloatingActionButton.setLayoutParams(layoutParams);
        } else {
            CoordinatorLayout.LayoutParams layoutParams = new CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.WRAP_CONTENT, CoordinatorLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(0, 0, DPtoPixel(16), DPtoPixel(16));
            layoutParams.gravity = Gravity.END | Gravity.BOTTOM;
            mFloatingActionButton.setLayoutParams(layoutParams);
        }
    }

    // DP 단위를 Pixel 단위로 변경시켜주는 메소드
    private int DPtoPixel(final int _DP) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, _DP, mContextThis.getResources().getDisplayMetrics());
    }

    // 파일 정렬 메소드
    private void sortFileArray(File[] _files) {
        // 최근 날짜순으로 정렬
        Arrays.sort(_files, new Comparator<File>() {
            @Override
            public int compare(File _o1, File _o2) {
                Date d1 = new Date(_o1.lastModified());
                Date d2 = new Date(_o2.lastModified());
                return d2.compareTo(d1);
            }
        });
    }
}
