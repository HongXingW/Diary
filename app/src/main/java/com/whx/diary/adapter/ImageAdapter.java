package com.whx.diary.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.whx.diary.R;
import com.whx.diary.utils.imageload.ImageLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by whx on 2016/10/24.
 */

public class ImageAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private List<String> mList = new ArrayList<>();
    private ImageLoader mLoader;
    private Drawable defaultDrawable;
    private int mImageWidth = 0;
    private boolean mGridSlide = false;

    public ImageAdapter(Context context) {
        inflater = LayoutInflater.from(context);
        mLoader = ImageLoader.build(context);

        defaultDrawable = context.getResources().getDrawable(R.drawable.default_pic);
    }

    public void setData(List<String> list) {
        mList = list;
    }
    public void setImageWidth(int width) {
        mImageWidth = width;
    }
    public void setGridSlide(boolean slide) {
        mGridSlide = slide;
    }
    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if(convertView == null) {
            convertView = inflater.inflate(R.layout.grid_item, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.image);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        ImageView imageView = viewHolder.imageView;
        final String tag = (String) imageView.getTag();
        final String url = mList.get(position);

        if(!url.equals(tag)){
            imageView.setImageDrawable(defaultDrawable);
        }

        if(!mGridSlide) {
            imageView.setTag(url);
            mLoader.bindBitmap(url, imageView, mImageWidth, mImageWidth);
        }

        return convertView;
    }

    private static class ViewHolder {
        ImageView imageView;
    }
}
