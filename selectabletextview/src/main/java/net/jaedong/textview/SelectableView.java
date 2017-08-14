package net.jaedong.textview;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.jaedong.selectabletextview.R;
import net.jaedong.textview.util.CommonUtil;
import net.jaedong.textview.view.CustomInfo;
import net.jaedong.textview.view.CustomTextView;
import net.jaedong.textview.view.ObservableScrollView;
import net.jaedong.textview.view.SelectableListener;


/**
 * Created by jd on 2017. 8. 13..
 */
public class SelectableView extends FrameLayout {

    private Context context;
    private ObservableScrollView scrollView;
    private TextView saveBtn;
    private SelectableListener selectableListener;
    private boolean hasActionBar;

    public void setText(String text) {
        scrollView.setText(text);
    }

    public void selectAll() {
        saveBtn.setVisibility(VISIBLE);
        scrollView.selectAll();
    }

    public void hideCursor() {
        saveBtn.setVisibility(GONE);
        scrollView.hideCursor();
    }

    public SelectableView(@NonNull Context context) {
        super(context);
        this.context = context;
        init();
    }

    public SelectableView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public SelectableView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init();
    }

    private void init() {
        scrollView = new ObservableScrollView(context);
        this.addView(scrollView, new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        initSaveButton();
        addOnCursorStateChangedListener();
    }

    private void initSaveButton() {
        saveBtn = new TextView(context);
        saveBtn.setGravity(Gravity.CENTER);
        saveBtn.setText(context.getResources().getString(R.string.save_conversation));
        saveBtn.setTextColor(context.getResources().getColor(R.color.main_progressbar));
        saveBtn.setBackgroundResource(R.drawable.selector_highlight_btn_bg);
        saveBtn.setVisibility(GONE);
        saveBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectableListener != null) {
                    CustomInfo customInfo = scrollView.getCustomTextView().getCursorSelection();
                    selectableListener.selectedText(customInfo.getSelectedText());
                }
            }
        });
        //saveBtn.setTextSize(Unit);

        // LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(CommonUtil.dpTpPx(75, context), CommonUtil.dpTpPx(35, context));
        this.addView(saveBtn);
    }

    private void setHighlightBtnCoods(int x, int y) {
        scrollView.getCustomTextView().measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (hasActionBar) {
            y = y - CommonUtil.dpTpPx(37, context);
        } else {
            y = y + CommonUtil.dpTpPx(20, context);
        }
        int deviceWidth = getResources().getDisplayMetrics().widthPixels;
        if (x > deviceWidth - CommonUtil.dpTpPx(75, context)) {
            x = deviceWidth - CommonUtil.dpTpPx(95, context);
        }
        LayoutParams params =
                new LayoutParams(CommonUtil.dpTpPx(75, context), CommonUtil.dpTpPx(35, context));
        params.leftMargin = x;
        params.topMargin = y;
        saveBtn.setLayoutParams(params);

//        LayoutParams paramsForFinger =
//                new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
//                        ViewGroup.LayoutParams.WRAP_CONTENT);
//        paramsForFinger.leftMargin = x + CommonUtil.dpTpPx(35, context);
//            paramsForFinger.topMargin = y + CommonUtil.dpTpPx(25, context);
    }

    private void addOnCursorStateChangedListener() {
        scrollView.getCustomTextView().setOnCursorStateChangedListener(
                new CustomTextView.OnCursorStateChangedListener() {

                    @Override
                    public void onDragStarts(View v) {
                        saveBtn.setVisibility(View.GONE);
                    }

                    @Override
                    public void onPositionChanged(View v, int x, int y, int oldx, int oldy) {

                    }

                    @Override
                    public void onDragEnds(int endHandleX, int endHandleY) {
                        if (isHighlightButtonVisible()) {
                            saveBtn.setVisibility(VISIBLE);
                        }

                        setHighlightBtnCoods(endHandleX, endHandleY);
                    }
                });
    }

    private boolean isHighlightButtonVisible() {
        return scrollView.getCustomTextView().getCursorSelection().getEnd() != scrollView.getCustomTextView().getCursorSelection().getStart();
    }

    public void addOnSaveClickListener(SelectableListener selectableListener) {
        this.selectableListener = selectableListener;
    }

    public void setActivity(AppCompatActivity act) {
        if (act.getSupportActionBar() == null) {
            this.hasActionBar = false;
        } else {
            this.hasActionBar = true;
        }
    }

    public void setActivity(Activity act) {
        if (act.getActionBar() == null) {
            this.hasActionBar = false;
        } else {
            this.hasActionBar = true;
        }
    }
}
