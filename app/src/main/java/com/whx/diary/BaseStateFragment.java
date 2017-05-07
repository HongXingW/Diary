package com.whx.diary;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

/**
 * 这个模板类可以保存Fragment状态，使用时只需继承，并重写onSaveState 和 onRestoreState方法即可
 * Created by whx on 2016/12/11.
 */

public class BaseStateFragment extends Fragment{

    Bundle savedState;

    public BaseStateFragment() {
        super();

        if(getArguments() == null)
            setArguments(new Bundle());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //restore state
        if (!restoreStateFromArguments()) {
            //first time, initialize something
            onFirstTimeLaunched();
        }
    }

    protected void onFirstTimeLaunched() {

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        saveStateToArguments();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        saveStateToArguments();
    }

    private void saveStateToArguments() {
        if(getView() != null) {
            savedState = saveState();
        }
        if (savedState != null) {
            Bundle b = getArguments();
            b.putBundle("internalSavedViewState", savedState);
        }
    }

    /**
     * 从arguments保存的bundle中恢复状态
     * @return
     */
    private boolean restoreStateFromArguments() {
        Bundle b = getArguments();
        savedState = b.getBundle("internalSavedViewState");

        if(savedState != null) {
            restoreState();
            return true;
        }
        return false;
    }

    private void restoreState() {
        if (savedState != null) {
            onRestoreState(savedState);
        }
    }

    protected void onRestoreState(Bundle savedState) {

    }

    private Bundle saveState() {
        Bundle state = new Bundle();
        onSaveState(state);

        return state;
    }

    protected void onSaveState(Bundle state) {

    }
}
