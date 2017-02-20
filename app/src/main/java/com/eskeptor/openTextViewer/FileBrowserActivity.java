package com.eskeptor.openTextViewer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Pattern;

/**
 * Created by eskeptor on 17. 1. 26.
 * Copyright (C) 2017 Eskeptor(Jeon Ye Chan)
 */

public class FileBrowserActivity extends AppCompatActivity
{
    private TextView txtPath;
    private ListView lvFilecontrol;
    private Spinner menu_spinner;
    private LinearLayout saveLayout;
    private Button btnSave;
    private EditText etxtSave;

    private String str_filename;
    private String str_root;

    private ArrayList<FileObject> fileObjects;
    private FileObjectAdaptor fileObjectAdaptor;

    private int browserType;
    private int sortType;

    private Pattern pattern;

    private Context context_this;
    private AlertDialog.Builder dialog;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filebrowser);

        context_this = getApplicationContext();

        browserType = getIntent().getIntExtra(Constant.INTENT_EXTRA_BROWSER_TYPE, 0);
        str_root = Environment.getExternalStorageDirectory().getAbsolutePath();
        sortType = Constant.BROWSER_MENU_SORT_ASC;

        txtPath = (TextView)findViewById(R.id.browser_txtPath);
        lvFilecontrol = (ListView)findViewById(R.id.browser_lvFilecontrol);
        saveLayout = (LinearLayout)findViewById(R.id.browser_saveLayout);
        etxtSave = (EditText)findViewById(R.id.browser_etxtSave);
        switch (browserType)
        {
            case Constant.BROWSER_TYPE_OPEN_EXTERNAL:
            {
                setTitle(R.string.browser_name_open);
                saveLayout.setVisibility(View.GONE);
                break;
            }
            case Constant.BROWSER_TYPE_SAVE_EXTERNAL_NONE_OPENEDFILE:
            {
                setTitle(R.string.browser_name_save);
                saveLayout.setVisibility(View.VISIBLE);
                btnSave = (Button)findViewById(R.id.browser_btnSave);
                break;
            }
        }

        getDirectory(str_root);

        lvFilecontrol.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                final File file = new File(fileObjects.get(position).url);

                if(file.isDirectory())
                {
                    if(file.canRead())
                    {
                        getDirectory(fileObjects.get(position).url);
                    }
                }
                else if(file.isFile())
                {
                    if(file.length() >= Constant.MEGABYTE)
                    {
                        dialog = new AlertDialog.Builder(FileBrowserActivity.this);
                        dialog.setTitle(R.string.dialog_filebrower_alert_title);
                        dialog.setMessage(R.string.dialog_filebrowser_alert_context);
                        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which)
                                {
                                    case AlertDialog.BUTTON_POSITIVE:
                                    {
                                        Intent intent = new Intent();

                                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                        intent.setClass(context_this, MemoActivity.class);
                                        intent.putExtra(Constant.INTENT_EXTRA_MEMO_OPEN_FILEURL, fileObjects.get(position).url);
                                        intent.putExtra(Constant.INTENT_EXTRA_MEMO_OPEN_FILENAME, file.getName());
                                        intent.putExtra(Constant.INTENT_EXTRA_MEMO_TYPE, Constant.MEMO_TYPE_OPEN_EXTERNAL);
                                        intent.putExtra(Constant.INTENT_EXTRA_FILE_SIZE, file.length() / Constant.KILOBYTE);
                                        startActivity(intent);
                                        overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
                                        finish();
                                        break;
                                    }
                                    case AlertDialog.BUTTON_NEGATIVE:
                                    {
                                        break;
                                    }
                                }
                                dialog.dismiss();
                            }
                        };
                        dialog.setPositiveButton(R.string.memo_btnOpen, clickListener);
                        dialog.setNegativeButton(R.string.dialog_folder_button_cancel, clickListener);
                        dialog.show();
                    }
                    else
                    {
                        Intent intent = new Intent();

                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        intent.setClass(context_this, MemoActivity.class);
                        intent.putExtra(Constant.INTENT_EXTRA_MEMO_OPEN_FILEURL, fileObjects.get(position).url);
                        intent.putExtra(Constant.INTENT_EXTRA_MEMO_OPEN_FILENAME, file.getName());
                        intent.putExtra(Constant.INTENT_EXTRA_MEMO_TYPE, Constant.MEMO_TYPE_OPEN_EXTERNAL);
                        intent.putExtra(Constant.INTENT_EXTRA_FILE_SIZE, file.length() / Constant.KILOBYTE);
                        startActivity(intent);
                        overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
                        finish();
                    }
                }
            }
        });
    }

    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.browser_btnSave:
            {
                pattern = Pattern.compile(Constant.REGEX);
                if(!etxtSave.getText().toString().equals("") && pattern.matcher(etxtSave.getText().toString()).matches())
                {
                    Intent intent = new Intent();
                    intent.putExtra(Constant.INTENT_EXTRA_MEMO_SAVE_FOLDERURL, str_filename + File.separator);
                    intent.putExtra(Constant.INTENT_EXTRA_MEMO_SAVE_FILEURL, etxtSave.getText().toString());
                    setResult(RESULT_OK, intent);
                    finish();
                }
                else
                {
                    Toast.makeText(this, getResources().getString(R.string.browser_toast_error_regex), Toast.LENGTH_SHORT).show();
                    etxtSave.setText("");
                }
                break;
            }
        }
    }

    public void getDirectory(final String dir)
    {
        fileObjects = new ArrayList<>();

        File file = new File(dir);
        File files[];
        if(browserType == Constant.BROWSER_TYPE_OPEN_EXTERNAL)
        {
            files = file.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    String name = pathname.getName();
                    return pathname.isDirectory() || name.endsWith(Constant.FILE_EXTENSION);
                }
            });
        }
        else if(browserType == Constant.BROWSER_TYPE_SAVE_EXTERNAL_NONE_OPENEDFILE)
        {
            files = file.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory();
                }
            });
        }
        else
        {
            files = null;
        }

        if(sortType == Constant.BROWSER_MENU_SORT_ASC)
        {
            sortFileArray(files, sortType);
        }
        else if(sortType == Constant.BROWSER_MENU_SORT_DES)
        {
            sortFileArray(files, sortType);
        }

        for(int i = 0; i < files.length; i++)
        {
            fileObjects.add(new FileObject(files[i]));
        }

        txtPath.setText(getResources().getString(R.string.browser_Location) + " " + dir);
        fileObjectAdaptor = new FileObjectAdaptor(this, fileObjects);
        lvFilecontrol.setAdapter(fileObjectAdaptor);
        str_filename = dir;
    }

    private void sortFileArray(File[] files, final int sortType)
    {
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if(sortType == Constant.BROWSER_MENU_SORT_ASC)
                {
                    return (o1.getName().compareTo(o2.getName()));
                }
                else
                {
                    return (o2.getName().compareTo(o1.getName()));
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_filebrowser, menu);
        MenuItem item = menu.findItem(R.id.menu_spinner);
        menu_spinner = (Spinner) MenuItemCompat.getActionView(item);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.menu_spinner_sort, R.layout.spinner_layout);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        menu_spinner.setAdapter(adapter);
        menu_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position)
                {
                    case Constant.BROWSER_MENU_SORT_ASC:
                    {
                        sortType = Constant.BROWSER_MENU_SORT_ASC;
                        getDirectory(str_filename);
                        break;
                    }
                    case Constant.BROWSER_MENU_SORT_DES:
                    {
                        sortType = Constant.BROWSER_MENU_SORT_DES;
                        getDirectory(str_filename);
                        break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        return true;
    }

    @Override
    public void onBackPressed() {
        if (str_filename.equals(str_root))
        {
            super.onBackPressed();
        }
        else
        {
            getDirectory(new File(str_filename).getParent());
        }
    }
}
