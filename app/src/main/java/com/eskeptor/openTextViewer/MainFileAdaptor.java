package com.eskeptor.openTextViewer;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.eskeptor.openTextViewer.datatype.MainFileObject;

import java.util.ArrayList;

/*
 * Created by eskeptor on 17. 2. 4.
 * Copyright (C) 2017 Eskeptor(Jeon Ye Chan)
 */

/**
 * Interface that defined the click action of the main page
 */
interface ClickAction {
    void onClick(final View _view, final int _position);
    void onLongClick(final View _view, final int _position);
}

/**
 * View holders for main page files
 */
class MainFileViewHolder extends RecyclerView.ViewHolder {
    public ImageView image;
    public TextView title;
    public TextView contents;
    public TextView date;
    public View view;

    MainFileViewHolder(final View _view) {
        super(_view);
        view = _view;
        image = itemView.findViewById(R.id.item_mainfile_image);
        title = itemView.findViewById(R.id.item_mainfile_title);
        contents = itemView.findViewById(R.id.item_mainfile_context);
        date = itemView.findViewById(R.id.item_mainfile_date);
    }
}

/**
 * For Padding in the RecyclerView
 */
class RecyclerViewPadding extends RecyclerView.ItemDecoration {
    private int mBottom;
    private int mLeft;
    private int mRight;

    RecyclerViewPadding(final int _bottom, final int _right, final int _left) {
        this.mBottom = _bottom;
        this.mRight = _right;
        this.mLeft = _left;
    }

    @Override
    public void getItemOffsets(Rect _outRect, View _view, RecyclerView _parent, RecyclerView.State _state) {
        super.getItemOffsets(_outRect, _view, _parent, _state);
        _outRect.bottom = mBottom;
        _outRect.right = mRight;
        _outRect.left = mLeft;
    }
}

/**
 * Adapter for main file
 */
public class MainFileAdaptor extends RecyclerView.Adapter<MainFileViewHolder> {
    private ArrayList<MainFileObject> mMainFiles;
    private ClickAction mAction;
    private SharedPreferences mSharedPref;

    MainFileAdaptor(final ArrayList<MainFileObject> _mainFiles, SharedPreferences _sharedPref) {
        this.mMainFiles = _mainFiles;
        mSharedPref = _sharedPref;
    }

    public void setClickAction(final ClickAction _action) {
        this.mAction = _action;
    }

    @NonNull
    @Override
    public MainFileViewHolder onCreateViewHolder(@NonNull final ViewGroup _parent, final int _viewType) {
        View view = LayoutInflater.from(_parent.getContext()).inflate(R.layout.item_mainfile_layout, null);
        return new MainFileViewHolder(view);
    }

    @Override
    public int getItemViewType(final int _position) {
        return mMainFiles.get(_position).mFileType.getValue();
    }

    @Override
    public void onBindViewHolder(@NonNull final MainFileViewHolder _holder, final int _position) {
        boolean isViewImage = mSharedPref.getBoolean(Constant.APP_VIEW_IMAGE, true);
        if (getItemViewType(_position) == Constant.FileType.Image.getValue()) {
            if(isViewImage) {
                Bitmap bitmap = decodeBitmapFromResource(mMainFiles.get(_position).mFilePath, 100, 100);
                _holder.image.setImageBitmap(bitmap);
                _holder.image.setVisibility(View.VISIBLE);
                _holder.contents.setVisibility(View.GONE);
            } else {
                _holder.image.setVisibility(View.GONE);
                _holder.contents.setVisibility(View.VISIBLE);
                _holder.contents.setText(mMainFiles.get(_position).mOneLinePreview);
            }
            _holder.title.setText(mMainFiles.get(_position).mFileTitle);
            _holder.date.setText(mMainFiles.get(_position).mModifyDate);
        } else {
            _holder.title.setText(mMainFiles.get(_position).mFileTitle);
            _holder.contents.setText(mMainFiles.get(_position).mOneLinePreview);
            _holder.date.setText(mMainFiles.get(_position).mModifyDate);
            _holder.image.setVisibility(View.GONE);
        }

        _holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View _v) {
                if (mAction != null) {
                    mAction.onClick(_v, _holder.getAdapterPosition());
                }
            }
        });
        _holder.view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View _v) {
                if (mAction != null) {
                    mAction.onLongClick(_v, _holder.getAdapterPosition());
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mMainFiles.size();
    }

    /**
     * How to calculate the size of a bitmap
     * @param _options Option
     * @param _reqWidth Width
     * @param _reqHeight Height
     * @return Size
     */
    private static int calculateInBitmapSize(BitmapFactory.Options _options, int _reqWidth, int _reqHeight) {
        // Raw mBottom and width of image
        final int height = _options.outHeight;
        final int width = _options.outWidth;
        int inSampleSize = 1;

        if (height > _reqHeight || width > _reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // mBottom and width larger than the requested mBottom and width.
            while ((halfHeight / inSampleSize) >= _reqHeight
                    && (halfWidth / inSampleSize) >= _reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * How to Convert Resources to Bitmap
     * @param _bitmap Path to Bitmap
     * @param _reqWidth Width
     * @param _reqHeight Height
     * @return Bitmap
     */
    private static Bitmap decodeBitmapFromResource(final String _bitmap, int _reqWidth, int _reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(_bitmap, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInBitmapSize(options, _reqWidth, _reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(_bitmap, options);
    }
}