package com.whx.diary.utils;

import java.io.Closeable;
import java.io.IOException;

/**
 * 关闭资源对象的工具
 * Created by whx on 2016/10/24.
 */

public class CloseUtil {

    public static void close(Closeable closeable) {

        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
