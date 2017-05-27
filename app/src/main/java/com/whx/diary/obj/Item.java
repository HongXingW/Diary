package com.whx.diary.obj;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by whx on 2017/5/25.
 */

public class Item implements Parcelable{

    private String imageUrl;
    private String describe;

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

    public Item() {}
    public Item(String imageUrl, String describe) {
        this.imageUrl = imageUrl;
        this.describe = describe;
    }
    private Item(Parcel in) {
        imageUrl = in.readString();
        describe = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(imageUrl);
        dest.writeString(describe);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Item> CREATOR = new Creator<Item>() {
        @Override
        public Item createFromParcel(Parcel source) {
            return new Item(source);
        }

        @Override
        public Item[] newArray(int size) {
            return new Item[size];
        }
    };
}
