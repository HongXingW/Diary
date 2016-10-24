package com.whx.diary;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrInterface;

/**
 * Created by whx on 2016/10/23.
 */

public class BaseSlideActivity extends BaseActivity {

    private SlidrInterface slidrInterface;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        slidrInterface = Slidr.attach(this);
    }
}
