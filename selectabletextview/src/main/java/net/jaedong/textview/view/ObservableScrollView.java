package net.jaedong.textview.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * Created by jd on 2017. 8. 13..
 */

public class ObservableScrollView extends ScrollView {

    private ArrayList<OnScrollChangedListener> mOnScrollChangedListeners;
    private Context context;
    private CustomTextView customTextView;

    public CustomTextView getCustomTextView() {
        return customTextView;
    }

    public ObservableScrollView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public ObservableScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        init();
    }

    public ObservableScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public void init() {
        mOnScrollChangedListeners = new ArrayList<>(2);
        customTextView = new CustomTextView(context);
        this.addView(customTextView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    public void setText(String text) {
        customTextView.setText(text, TextView.BufferType.SPANNABLE);
    }

    public void selectAll() {
        customTextView.showSelectionControls(0, customTextView.getText().toString().length());
    }

    public void hideCursor() {
        customTextView.hideCursor();
    }

    public void addOnScrollChangedListener(OnScrollChangedListener onScrollChangedListener) {
        mOnScrollChangedListeners.add(onScrollChangedListener);
    }

    public void removeOnScrollChangedListener(OnScrollChangedListener onScrollChangedListener) {
        mOnScrollChangedListeners.remove(onScrollChangedListener);
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldx, int oldy) {
        super.onScrollChanged(x, y, oldx, oldy);
        for (OnScrollChangedListener listener : mOnScrollChangedListeners) {
            listener.onScrollChanged(this, x, y, oldx, oldy);
        }
    }
}
