package com.whx.diary.utils.imageload;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.FileDescriptor;

/**
 * 图片压缩功能
 * Created by whx on 2016/10/23.
 */

public class ImageResizer {

    private static final String TAG = "ImageResizer";

    public ImageResizer() {

    }

    /**
     * 从资源文件中获取bitmap
     *
     * @param res
     * @param resId     id
     * @param reqWidth  所需宽
     * @param reqHeight 所需高
     * @return
     */
    public Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                  int reqWidth, int reqHeight) {

        //inJustDecodeBounds 设为true时，BitmapFactory只会解析图片的原始宽高
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeResource(res, resId, options);

        //计算采样率
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        //
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * 从FileDescriptor（文件描述符）中解析图片
     *
     * @param fd
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public Bitmap decodeSampledBitmapFromFileDescriptor(FileDescriptor fd, int reqWidth,
                                                        int reqHeight) {
        //inJustDecodeBounds 设为true时，BitmapFactory只会解析图片的原始宽高
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFileDescriptor(fd, null, options);

        //计算采样率
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        //
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fd, null, options);
    }

    /**
     * 计算采样率
     *
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth,
                                      int reqHeight) {
        if (reqWidth == 0 || reqHeight == 0) {
            return 1;
        }
        //图片的原始宽高
        final int width = options.outWidth;
        final int height = options.outHeight;

        Log.e(TAG, "origin, w = " + width + " , h = " + height);

        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            //计算最大采样率，为2的幂，并且保持原始宽高比要求宽高大
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize <<= 1;
            }
        }
        Log.e(TAG, "inSampleSize = " + inSampleSize);

        return inSampleSize;
    }
}
