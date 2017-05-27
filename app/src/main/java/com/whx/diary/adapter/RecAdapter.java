package com.whx.diary.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.whx.diary.R;
import com.whx.diary.obj.Item;
import com.whx.diary.utils.imageload.ImageLoader;

import java.util.List;

/**
 * Created by whx on 2017/5/25.
 */

public class RecAdapter extends RecyclerView.Adapter<RecAdapter.MyViewHolder>{

    private List<Item> mData;
    private ImageLoader imageLoader;
    private LayoutInflater inflater;


    public RecAdapter(Context context) {
        imageLoader = ImageLoader.build(context);
        inflater = LayoutInflater.from(context);
    }

    public void setData(List<Item> data) {
        mData = data;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        imageLoader.bindBitmap(mData.get(position).getImageUrl(), holder.imageView);
        holder.textView.setText(mData.get(position).getDescribe());
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.list_item, parent, false);

        return new MyViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textView;

        public MyViewHolder(View v) {
            super(v);
            imageView = (ImageView) v.findViewById(R.id.item_image);
            textView = (TextView) v.findViewById(R.id.item_text);
        }
    }
}
