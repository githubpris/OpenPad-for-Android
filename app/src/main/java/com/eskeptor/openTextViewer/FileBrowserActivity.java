package com.eskeptor.openTextViewer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.eskeptor.openTextViewer.datatype.FileObject;
import com.tsengvn.typekit.Typekit;
import com.tsengvn.typekit.TypekitContextWrapper;

import java.io.File;
import java.io.FileFilter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Pattern;

/*
 * Created by eskeptor on 17. 1. 26.
 * Copyright (C) 2017 Eskeptor(Jeon Ye Chan)
 */

/**
 * Embedded File Browser Class
 */
public class FileBrowserActivity extends AppCompatActivity {
    private TextView mTxtPath;                              // Current File Paths
    private ListView mFileList;                             // File List
    private String mStrFilename;                            // File Name
    private ArrayList<FileObject> mFileListObjects;         // List of arrays to save the list of files
    private FileObjectAdaptor mFileListObjectAdaptor;       // Custom Adapter for File
    private Context mContextThis;                           // Context
    private View mContextView;
    private Constant.BrowserType mBrowserType;              // Browser Type
    private Constant.BrowserMenuSortType mSortType;         // Sort by

    // Menu Items
    private MenuItem mMenuItemDES;                          // Descending order
    private MenuItem mMenuItemASC;                          // Ascending order

    // Used to save files under a different name in a different folder
    private LinearLayout mSaveLayout;
    private EditText mEditTxtSave;

    private RefreshList mHandler;
    private Thread mDirectoryThread;
    private static final int HANDLER_REFRESH_DIR = 1;
    private static final int HANDLER_PREV_DIR = 2;
    private static final int HANDLER_NEXT_DIR = 3;
    private static final int HANDLER_CHANGE_SORT = 4;
    private static final String DEVICE_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath();

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        setContentView(R.layout.activity_filebrowser);

        mContextThis = getApplicationContext();
        mContextView = findViewById(R.id.activity_filebrowser);
        mHandler = new RefreshList(this);

        int browserType = getIntent().getIntExtra(Constant.INTENT_EXTRA_BROWSER_TYPE, 0);
        mSortType = Constant.BrowserMenuSortType.Asc;

        mTxtPath = findViewById(R.id.browser_txtPath);
        mFileList = findViewById(R.id.browser_lvFilecontrol);
        mFileListObjects = new ArrayList<>();
        mSaveLayout = findViewById(R.id.browser_saveLayout);
        mEditTxtSave = findViewById(R.id.browser_etxtSave);

        if (browserType == Constant.BrowserType.OpenExternal.getValue()) {
            setTitle(R.string.filebrowser_name_open);
            mSaveLayout.setVisibility(View.GONE);
            mBrowserType = Constant.BrowserType.OpenExternal;
        } else if (browserType == Constant.BrowserType.SaveExternalOpenedFile.getValue()) {
            setTitle(R.string.filebrowser_name_save);
            mSaveLayout.setVisibility(View.VISIBLE);
            mBrowserType = Constant.BrowserType.SaveExternalOpenedFile;
        }

        mDirectoryThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(HANDLER_REFRESH_DIR);
            }
        });
        mDirectoryThread.start();

        mFileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> _parent, View _view, final int _position, long _id) {
                final File file = new File(mFileListObjects.get(_position).mFilePath);

                if (file.isDirectory()) {
                    if (file.canRead()) {
                        if (mDirectoryThread != null) {
                            mDirectoryThread.interrupt();
                        }
                        mDirectoryThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Message message = new Message();
                                message.what = HANDLER_NEXT_DIR;
                                message.arg1 = _position;
                                mHandler.sendMessage(message);
                            }
                        });
                        mDirectoryThread.start();
                    }
                } else if (file.isFile()) {
                    Intent intent = new Intent();

                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.setClass(mContextThis, MemoActivity.class);
                    intent.putExtra(Constant.INTENT_EXTRA_MEMO_OPEN_FILEURL, mFileListObjects.get(_position).mFilePath);
                    intent.putExtra(Constant.INTENT_EXTRA_MEMO_OPEN_FILENAME, file.getName());
                    intent.putExtra(Constant.INTENT_EXTRA_MEMO_DIVIDE, true);
                    startActivity(intent);
                    overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
                    finish();
                }
            }
        });

        SharedPreferences sharedPref = getSharedPreferences(Constant.APP_SETTINGS_PREFERENCE, MODE_PRIVATE);
        int font = sharedPref.getInt(Constant.APP_FONT, Constant.FontType.Default.getValue());
        if (font == Constant.FontType.BaeDal_JUA.getValue()) {
            Typekit.getInstance().addNormal(Typekit.createFromAsset(mContextThis, "fonts/bmjua.ttf"))
                    .addBold(Typekit.createFromAsset(mContextThis, "fonts/bmjua.ttf"));
        } else if (font == Constant.FontType.KOPUB_Dotum.getValue()) {
            Typekit.getInstance().addNormal(Typekit.createFromAsset(mContextThis, "fonts/kopub_dotum_medium.ttf"))
                    .addBold(Typekit.createFromAsset(mContextThis, "fonts/kopub_dotum_medium.ttf"));
        } else {
            Typekit.getInstance().addNormal(Typeface.DEFAULT).addBold(Typeface.DEFAULT_BOLD);
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(TypekitContextWrapper.wrap(newBase));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu _menu) {
        getMenuInflater().inflate(R.menu.menu_filebrowser, _menu);
        mMenuItemDES = _menu.findItem(R.id.menu_des);
        mMenuItemASC = _menu.findItem(R.id.menu_asc);
        mMenuItemASC.setChecked(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem _item) {
        int id = _item.getItemId();

        switch (id) {
            case R.id.menu_asc:
                mSortType = Constant.BrowserMenuSortType.Asc;
                if (mDirectoryThread != null) {
                    mDirectoryThread.interrupt();
                }
                mDirectoryThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mHandler.sendEmptyMessage(HANDLER_CHANGE_SORT);
                    }
                });
                mDirectoryThread.start();
                mMenuItemASC.setChecked(true);
                mMenuItemDES.setChecked(false);
                break;
            case R.id.menu_des:
                mSortType = Constant.BrowserMenuSortType.Des;
                if (mDirectoryThread != null) {
                    mDirectoryThread.interrupt();
                }
                mDirectoryThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mHandler.sendEmptyMessage(HANDLER_CHANGE_SORT);
                    }
                });
                mDirectoryThread.start();
                mMenuItemASC.setChecked(false);
                mMenuItemDES.setChecked(true);
                break;
        }
        return super.onOptionsItemSelected(_item);
    }

    @Override
    public void onBackPressed() {
        if (mStrFilename.equals(DEVICE_ROOT)) {
            super.onBackPressed();
            overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
        } else {
            if (mDirectoryThread != null) {
                mDirectoryThread.interrupt();
            }
            mDirectoryThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    mHandler.sendEmptyMessage(HANDLER_PREV_DIR);
                }
            });
            mDirectoryThread.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTxtPath = null;
        mFileList = null;
        mSaveLayout = null;
        mEditTxtSave = null;
        if (!mFileListObjects.isEmpty()) {
            mFileListObjects.clear();
        }
        mFileListObjects = null;
        mFileListObjectAdaptor = null;
        mContextThis = null;
        mStrFilename = null;
        mContextView = null;
        mHandler = null;
        if (mDirectoryThread != null) {
            mDirectoryThread.interrupt();
        }
        mDirectoryThread = null;
    }

    public void onClick(View _v) {
        if (_v.getId() == R.id.browser_btnSave) {
            Pattern pattern = Pattern.compile(Constant.REGEX);
            if (!mEditTxtSave.getText().toString().equals("") && pattern.matcher(mEditTxtSave.getText().toString()).matches()) {
                if (!isExist(mEditTxtSave.getText().toString())) {
                    Intent intent = new Intent();
                    intent.putExtra(Constant.INTENT_EXTRA_MEMO_SAVE_FOLDERURL, mStrFilename + File.separator);
                    intent.putExtra(Constant.INTENT_EXTRA_MEMO_SAVE_FILEURL, mEditTxtSave.getText().toString());
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(FileBrowserActivity.this);
                    dialog.setTitle(R.string.filebrowser_dialog_exist_title);
                    dialog.setMessage(R.string.filebrowser_dialog_exist_context);
                    DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface _dialog, int _which) {
                            if (_which == AlertDialog.BUTTON_POSITIVE) {
                                Intent intent = new Intent();
                                intent.putExtra(Constant.INTENT_EXTRA_MEMO_SAVE_FOLDERURL, mStrFilename + File.separator);
                                intent.putExtra(Constant.INTENT_EXTRA_MEMO_SAVE_FILEURL, mEditTxtSave.getText().toString());
                                setResult(RESULT_OK, intent);
                                finish();
                            } else if (_which == AlertDialog.BUTTON_NEGATIVE) {
                                mEditTxtSave.setText("");
                            }
                            _dialog.dismiss();
                        }
                    };
                    dialog.setPositiveButton(R.string.filebrowser_dialog_exist_overwrite, clickListener);
                    dialog.setNegativeButton(R.string.folder_dialog_button_cancel, clickListener);
                    dialog.show();
                }
            } else {
                Snackbar.make(mContextView, R.string.filebrowser_toast_error_regex, Snackbar.LENGTH_SHORT).show();
                mEditTxtSave.setText("");
            }
        }
    }

    /**
     * Import a file directory
     * @param _dir Directory
     */
    public void getDirectory(final String _dir) {
        if (!mFileListObjects.isEmpty()) {
            mFileListObjects.clear();
        }

        File file = new File(_dir);
        File files[];

        switch (mBrowserType) {
            case OpenExternal:
                files = file.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File _pathname) {
                        String name = _pathname.getName();
                        return _pathname.isDirectory() || name.endsWith(Constant.FILE_TEXT_EXTENSION);
                    }
                });
                break;
            case SaveExternalOpenedFile:
                files = file.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File _pathname) {
                        return _pathname.isDirectory();
                    }
                });
                break;
            default:
                files = null;
                break;
        }

        sortFileArray(files, mSortType);

        if (files != null) {
            for (File newFile : files) {
                mFileListObjects.add(new FileObject(newFile));
            }
        }

        String path = getResources().getString(R.string.filebrowser_Location) + " " + _dir;
        mTxtPath.setText(path);

        if (mFileListObjectAdaptor == null) {
            mFileListObjectAdaptor = new FileObjectAdaptor(mContextThis, mFileListObjects);
            mFileList.setAdapter(mFileListObjectAdaptor);
        }

        mStrFilename = _dir;
    }

    /**
     * Sort and show the file directory
     * @param _files File Directory
     * @param _sortType Sort by
     */
    private void sortFileArray(File[] _files, final Constant.BrowserMenuSortType _sortType) {
        Arrays.sort(_files, new Comparator<File>() {
            @Override
            public int compare(File _o1, File _o2) {
                switch (_sortType) {
                    case Asc:
                        return (_o1.getName().compareTo(_o2.getName()));
                    default:
                        return (_o2.getName().compareTo(_o1.getName()));
                }
            }
        });
    }

    /**
     * Returns whether a file exists on that path
     * @param _filename File name
     * @return Exist or nothing
     */
    private boolean isExist(final String _filename) {
        for (int i = 0; i < mFileListObjects.size(); i++) {
            if (_filename.equals(mFileListObjects.get(i).mFileName)) {
                return true;
            }
        }
        return false;
    }


    private void handleMessage(Message _msg) {
        int what = _msg.what;
        switch (what) {
            case HANDLER_REFRESH_DIR: {
                getDirectory(DEVICE_ROOT);
                mFileListObjectAdaptor.notifyDataSetChanged();
                break;
            }
            case HANDLER_PREV_DIR: {
                getDirectory(new File(mStrFilename).getParent());
                mFileListObjectAdaptor.notifyDataSetChanged();
                break;
            }
            case HANDLER_NEXT_DIR: {
                getDirectory(mFileListObjects.get(_msg.arg1).mFilePath);
                mFileListObjectAdaptor.notifyDataSetChanged();
                break;
            }
            case HANDLER_CHANGE_SORT: {
                getDirectory(mStrFilename);
                mFileListObjectAdaptor.notifyDataSetChanged();
                break;
            }
        }
    }

    /**
     * Handler class for updating the file list
     */
    static class RefreshList extends Handler {
        private final WeakReference<FileBrowserActivity> mActivity;
        RefreshList(FileBrowserActivity _activity) {
            mActivity = new WeakReference<>(_activity);
        }

        @Override
        public void handleMessage(Message msg) {
            FileBrowserActivity activity = mActivity.get();
            if (activity != null) {
                activity.handleMessage(msg);
            }
        }
    }
}
