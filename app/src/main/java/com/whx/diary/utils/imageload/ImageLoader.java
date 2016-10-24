package com.whx.diary.utils.imageload;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.whx.diary.R;
import com.whx.diary.utils.CloseUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 图片加载
 * Created by whx on 2016/10/24.
 */

public class ImageLoader {

    private static final String TAG = "ImageLoader";

    private static final int MESSAGE_POST_RESULT = 1;

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    /**
     * 线程池中核心线程数
     */
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    /**
     * 线程池中最大线程数
     */
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    /**
     * 闲置线程超时时间
     */
    private static final long KEEP_ALIVE = 10L;

    private static final int TAG_KEY_URI = R.id.imageloader_uri;
    /**
     * 磁盘缓存大小50M
     */
    private static final long DISK_CACHE_SIZE = 1024 * 1024 * 50;
    private static final int IO_BUFFER_SIZE = 8 * 1024;
    private static final int DISK_CACHE_INDEX = 0;

    private boolean mIsDiskLruCacheCreated = false;

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {

        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "ImageLoader# " + mCount.getAndIncrement());
        }
    };

    private static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE,
            MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS,
            new LinkedBlockingDeque<Runnable>(), sThreadFactory);

    /**
     * 更新UI的handler
     */
    private Handler mMainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            LoaderResult result = (LoaderResult) msg.obj;
            ImageView imageView = result.imageView;

            String url = (String) imageView.getTag(TAG_KEY_URI);

            if (url.equals(result.url)) {
                imageView.setImageBitmap(result.bitmap);
            } else {
                Log.e(TAG, "set image bitmap,but url has changed,ignored");
            }
        }
    };

    private Context mContext;
    private ImageResizer mImageResizer = new ImageResizer();
    private LruCache<String, Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;

    private ImageLoader(Context context) {
        mContext = context.getApplicationContext();

        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight() / 1024;
            }
        };

        File diskCacheDir = getDiskCacheDir(mContext, "bitmap");

        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs();
        }
        if (getUsableSpace(diskCacheDir) > DISK_CACHE_SIZE) {
            try {
                mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, DISK_CACHE_SIZE);
                mIsDiskLruCacheCreated = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 创建一个ImageLoader的对象
     *
     * @param context
     * @return
     */
    public static ImageLoader build(Context context) {
        return new ImageLoader(context);
    }

    public void bindBitmap(final String url, final ImageView imageView) {
        bindBitmap(url, imageView, 0, 0);
    }

    public void bindBitmap(final String url, final ImageView imageView,
                           final int reqWidth, final int reqHeight) {
        imageView.setTag(TAG_KEY_URI, url);

        Bitmap bitmap = loadBitmapFromMemCache(url);

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return;
        }

        Runnable loadBitmapTask = new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = loadBitmap(url, reqWidth, reqHeight);
                if (bitmap != null) {
                    LoaderResult result = new LoaderResult(imageView, url, bitmap);

                    mMainHandler.obtainMessage(MESSAGE_POST_RESULT, result).sendToTarget();
                }
            }
        };
        THREAD_POOL_EXECUTOR.execute(loadBitmapTask);
    }

    /**
     * 从内存缓存或磁盘缓存获取图片或从网络加载图片
     *
     * @param url
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public Bitmap loadBitmap(String url, int reqWidth, int reqHeight) {
        Bitmap bitmap = loadBitmapFromMemCache(url);
        if (bitmap != null) {
            Log.e(TAG, "load bitmap from memory, url = " + url);
            return bitmap;
        }
        try {
            bitmap = loadBitmapFromDiskCache(url, reqWidth, reqHeight);
            if (bitmap != null) {
                Log.e(TAG, "load bitmap from disk, url = " + url);
                return bitmap;
            }
            bitmap = loadBitmapFromHttp(url, reqWidth, reqHeight);
            Log.e(TAG, "load bitmap from net, url = " + url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (bitmap == null && !mIsDiskLruCacheCreated) {
            Log.e(TAG, "encounter error, DiskLruCache is not created");

            bitmap = downloadBitmapFromUrl(url);
        }
        return bitmap;
    }

    private Bitmap loadBitmapFromHttp(String url, int reqWidth, int reqHeight) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("visit network from UI thread");
        }
        if (mDiskLruCache == null) {
            return null;
        }

        String key = hashKeyFromUrl(url);
        DiskLruCache.Editor editor = mDiskLruCache.edit(key);
        if (editor != null) {
            OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX);
            if (downloadUrlToStream(url, outputStream)) {
                editor.commit();
            } else {
                editor.abort();
            }

            mDiskLruCache.flush();
        }
        return loadBitmapFromDiskCache(url, reqWidth, reqHeight);
    }

    private Bitmap loadBitmapFromDiskCache(String url, int reqWidth, int reqHeight)
            throws IOException {

        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.e(TAG, "load bitmap from UI thread, not recommend");
        }
        if (mDiskLruCache == null) {
            return null;
        }

        Bitmap bitmap = null;
        String key = hashKeyFromUrl(url);
        DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);

        if (snapshot != null) {
            FileInputStream fileInputStream = (FileInputStream) snapshot
                    .getInputStream(DISK_CACHE_INDEX);

            FileDescriptor fileDescriptor = fileInputStream.getFD();
            bitmap = mImageResizer.decodeSampledBitmapFromFileDescriptor(fileDescriptor,
                    reqWidth, reqHeight);

            if (bitmap != null) {
                addBitmapToMemoryCache(key, bitmap);
            }
        }
        return bitmap;
    }

    private boolean downloadUrlToStream(String urlStr, OutputStream outputStream) {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;

        try {
            final URL url = new URL(urlStr);
            urlConnection = (HttpURLConnection) url.openConnection();

            in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE);
            out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);

            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }

            CloseUtil.close(out);
            CloseUtil.close(in);
        }
        return false;
    }

    /**
     * 通过url下载图片
     *
     * @param urlStr
     * @return
     */
    private Bitmap downloadBitmapFromUrl(String urlStr) {
        Bitmap bitmap = null;
        HttpURLConnection connection = null;
        BufferedInputStream in = null;

        try {
            final URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();

            in = new BufferedInputStream(connection.getInputStream(), IO_BUFFER_SIZE);

            bitmap = BitmapFactory.decodeStream(in);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            CloseUtil.close(in);
        }
        return bitmap;
    }

    private Bitmap loadBitmapFromMemCache(String url) {
        final String key = hashKeyFromUrl(url);

        return getBitmapFromMemCache(key);
    }

    private String hashKeyFromUrl(String url) {
        String key;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(url.getBytes());
            key = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            key = String.valueOf(url.hashCode());
        }

        return key;
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    private Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private long getUsableSpace(File path) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return path.getUsableSpace();
        }
        final StatFs statFs = new StatFs(path.getPath());
        return (long) statFs.getBlockSize() * (long) statFs.getAvailableBlocks();
    }

    private File getDiskCacheDir(Context context, String uniqueName) {
        boolean externalStorageAvailable = Environment.getExternalStorageState()
                .equals(Environment.MEDIA_MOUNTED);
        final String cachePath;

        if (externalStorageAvailable) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }

        return new File(cachePath + File.separator + uniqueName);
    }

    private static class LoaderResult {
        ImageView imageView;
        String url;
        Bitmap bitmap;

        LoaderResult(ImageView imageView, String url, Bitmap bitmap) {
            this.imageView = imageView;
            this.url = url;
            this.bitmap = bitmap;
        }
    }
}
