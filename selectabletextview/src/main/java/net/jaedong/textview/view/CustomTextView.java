package net.jaedong.textview.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatTextView;
import android.text.Layout;
import android.text.style.BackgroundColorSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.PopupWindow;
import android.widget.ScrollView;

import net.jaedong.selectabletextview.R;


/**
 * Created by jd on 2017. 8. 13..
 */
public class CustomTextView extends AppCompatTextView {

    private static final String TAG = "drag ";
    private int mDefaultSelectionColor;

    /**
     * the selection information used by the cursor
     */
    private CustomInfo mCursorSelection;

    private final int[] mTempCoords = new int[2];

    private OnCursorStateChangedListener mOnCursorStateChangedListener;

    /**
     * DONT ACCESS DIRECTLY, use getSelectionController() instead
     */
    private SelectionCursorController mSelectionController;

    @SuppressWarnings("unused")
    public CustomTextView(Context context) {
        super(context);
        init();
    }

    @SuppressWarnings("unused")
    public CustomTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @SuppressWarnings("unused")
    public CustomTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setDefaultSelectionColor(ContextCompat.getColor(getContext(), R.color.textColorHighlight));
        mCursorSelection = new CustomInfo();
        mSelectionController = new SelectionCursorController();

        final ViewTreeObserver observer = getViewTreeObserver();
        if (observer != null) {
            observer.addOnTouchModeChangeListener(mSelectionController);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // TextView will take care of the color span of the text when it's being scroll by
        // its parent ScrollView. But the cursors position is not handled. Calling snapToSelection
        // will move the cursors along with the selection.
        if (getParent() instanceof ObservableScrollView) {
            ((ObservableScrollView) getParent()).addOnScrollChangedListener(
                    new OnScrollChangedListener() {
                        @Override
                        public void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx,
                                                    int oldy) {
                            mSelectionController.snapToSelection();
                        }
                    });
        }
    }

    public void setDefaultSelectionColor(int color) {
        mDefaultSelectionColor = color;
    }

    public void setSelection(int color, int start, int length, int duration) {

        int end = start + length >= getText().length() ? getText().length() : start + length;

        if (start + length > getText().length() || end <= start) {
            return;
        }
        mCursorSelection = new CustomInfo(getText(), new BackgroundColorSpan(color), start, end);
        mCursorSelection.select();
        removeSelection(duration);
    }

    public void setSelection(int start, int length, int duration) {
        setSelection(mDefaultSelectionColor, start, length, duration);
    }

    public void setSelection(int start, int length) {
        setSelection(start, length, -1);
    }

    /**
     * removes the current selection immediately
     */
    public void removeSelection() {
        mCursorSelection.remove();
    }

    public void removeSelection(int delay) {
        removeSelection(mCursorSelection, delay);
    }

    public void removeSelection(final CustomInfo selection, int delay) {
        if (delay >= 0) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    selection.remove();
                }
            };
            postDelayed(runnable, delay);
        }
    }

    public void showSelectionControls(int start, int end) {
        if (start < 0) {
            start = 0;
        } else if (start > getText().length()) {
            start = getText().length();
        }

        if (end < 0) {
            end = 0;
        } else if (end > getText().length()) {
            end = getText().length();
        }

        mSelectionController.show(start, end);
    }

    public CustomInfo getCursorSelection() {
        return mCursorSelection;
    }

    public void setOnCursorStateChangedListener(
            OnCursorStateChangedListener onCursorStateChangedListener) {
        mOnCursorStateChangedListener = onCursorStateChangedListener;
    }

    private int getScrollYInternal() {
        int y = this.getScrollY();
        if (this.getParent() instanceof ScrollView) {
            y += ((ScrollView) this.getParent()).getScrollY();
            final int[] coords = mTempCoords;
            ((ScrollView) this.getParent()).getLocationInWindow(coords);
            y -= coords[1];
        }

        return y;
    }

    private int getScrollXInternal() {
        int x = this.getScrollX();

        // a TextView inside a ScrollView is not scrolled, so getScrollX() returns 0.
        // We must use getScrollX() from the ScrollView instead
        if (this.getParent() instanceof ScrollView) {
            ScrollView scrollView = (ScrollView) this.getParent();

            x += scrollView.getScrollX();

            final int[] coords = mTempCoords;
            scrollView.getLocationInWindow(coords);
            x -= coords[0];
            x -= scrollView.getPaddingLeft();
        }
        return x;
    }

    public int getOffset(int x, int y) {
        Layout layout = getLayout();
        int offset = -1;

        if (layout != null) {
            int topVisibleLine = layout.getLineForVertical(y);
            offset = layout.getOffsetForHorizontal(topVisibleLine, x);
        }

        return offset;
    }

    public int getPreciseOffset(int x, int y) {
        Layout layout = getLayout();

        if (layout != null) {
            int topVisibleLine = layout.getLineForVertical(y);
            int offset = layout.getOffsetForHorizontal(topVisibleLine, x);

            int offset_x = (int) layout.getPrimaryHorizontal(offset);
            if (offset_x > x) {
                return layout.getOffsetToLeftOf(offset);
            }
        }
        return getOffset(x, y);
    }

    private int getHysteresisOffset(int x, int y, int previousOffset) {
        final Layout layout = getLayout();
        if (layout == null) return -1;

        y += getScrollYInternal();
        x += getScrollXInternal();

        int line = getLayout().getLineForVertical(y);
        // The "HACK BLOCK"S in this function is required because of how Android Layout for
        // TextView works - if 'offset' equals to the last character of a line, then
        //
        // * getLineForOffset(offset) will result the NEXT line
        // * getPrimaryHorizontal(offset) will return 0 because the next insertion point is on the next line
        // * getOffsetForHorizontal(line, x) will not return the last offset of a line no matter where x is
        // These are highly undesired and is worked around with the HACK BLOCK
        //
        // @see Moon+ Reader/Color Note - see how it can't select the last character of a line unless you move
        // the cursor to the beginning of the next line.
        //
        ////////////////////HACK BLOCK////////////////////////////////////////////////////
        if (isEndOfLineOffset(previousOffset)) {
            // we have to minus one from the offset so that the code below to find
            // the previous line can work correctly.
            int left = (int) layout.getPrimaryHorizontal(previousOffset - 1);
            int right = (int) layout.getLineRight(line);
            int threshold = (right - left) / 2; // half the width of the last character
            if (x > right - threshold) {
                previousOffset -= 1;
            }
        }
        ///////////////////////////////////////////////////////////////////////////////////

        final int previousLine = layout.getLineForOffset(previousOffset);
        final int previousLineTop = layout.getLineTop(previousLine);
        final int previousLineBottom = layout.getLineBottom(previousLine);
        final int hysteresisThreshold = (previousLineBottom - previousLineTop) / 2;

        // If new line is just before or after previous line and y position is less than
        // hysteresisThreshold away from previous line, keep cursor on previous line.
        if (((line == previousLine + 1) && ((y - previousLineBottom) < hysteresisThreshold)) || ((line
                == previousLine - 1) && ((previousLineTop - y) < hysteresisThreshold))) {
            line = previousLine;
        }

        int offset = layout.getOffsetForHorizontal(line, x);
        // This allow the user to select the last character of a line without moving the
        // cursor to the next line. (As Layout.getOffsetForHorizontal does not return the
        // offset of the last character of the specified line)
        //
        // But this function will probably get called again immediately, must decrement the offset
        // by 1 to compensate for the change made below. (see previous HACK BLOCK)
        /////////////////////HACK BLOCK///////////////////////////////////////////////////

        if (offset < getText().length() - 1) {
            if (isEndOfLineOffset(offset + 1)) {
                int left = (int) layout.getPrimaryHorizontal(offset);
                int right = (int) layout.getLineRight(line);
                int threshold = (right - left) / 2; // half the width of the last character
                if (x > right - threshold) {
                    offset += 1;
                }
            }
        }

        //////////////////////////////////////////////////////////////////////////////////

        String str = String.valueOf(getText());
        if (offset > 1 && offset < str.length()) {
            for (int i = offset; i >= 0; i--) {
                if (str.charAt(i) == ' ' || str.charAt(i) == '\n') {
                    offset = i;
                    break;
                } else {
                    offset--;
                }
            }
        }

        if (offset < 0) {
            offset = 0;
        }

        String[] test = str.split(" ");
        if (test.length == 1) {
            //offset =
            if (offset == 0) {
                offset = 0;
            } else {
                offset = str.length();
            }
        }
        return offset;
    }

    private boolean isEndOfLineOffset(int offset) {
        if (offset > 0) {
            return getLayout().getLineForOffset(offset) == getLayout().getLineForOffset(offset - 1) + 1;
        }
        return false;
    }

    private int getOffsetForHorizontal(int line, int x) {
        x -= getTotalPaddingLeft();
        // Clamp the position to inside of the view.
        x = Math.max(0, x);
        x = Math.min(getWidth() - getTotalPaddingRight() - 1, x);
        x += getScrollXInternal();

        return getLayout().getOffsetForHorizontal(line, x);
    }

    private void getXY(int offset, int scroll_x, int scroll_y, int[] coords) {
        assert (coords.length >= 2);

        coords[0] = coords[1] = -1;
        Layout layout = getLayout();

        if (layout != null) {
            int line = layout.getLineForOffset(offset);
            int base = layout.getLineBottom(line);

            coords[0] = (int) layout.getPrimaryHorizontal(offset) - scroll_x; // x
            coords[1] = base - scroll_y; // y
        }
    }

    private void getAdjusteStartXY(int offset, int scroll_x, int scroll_y, int[] coords) {
        if (offset < getText().length()) {
            final Layout layout = getLayout();
            if (layout != null) {
                if (isEndOfLineOffset(offset + 1)) {
                    float a = layout.getPrimaryHorizontal(offset);
                    float b = layout.getLineRight(layout.getLineForOffset(offset));
                    if (a == b) {
                        // this means the we encounter a new line character, i think.
                        offset += 1;
                    }
                }
            }
        }
        getXY(offset, scroll_x, scroll_y, coords);
    }

    private void getAdjustedEndXY(int offset, int scroll_x, int scroll_y, int[] coords) {
        if (offset > 0) {
            final Layout layout = getLayout();
            if (layout != null) {
                if (isEndOfLineOffset(offset)) {
                    int prev_line = layout.getLineForOffset(offset - 1);
                    float right = layout.getLineRight(prev_line);
                    int y = layout.getLineBottom(prev_line);
                    coords[0] = (int) right - scroll_x;
                    coords[1] = y - scroll_y;
                    return;
                }
            }
        }
        getXY(offset, scroll_x, scroll_y, coords);
    }

    public void hideCursor() {
        mSelectionController.hide();
    }

    private class SelectionCursorController implements ViewTreeObserver.OnTouchModeChangeListener {

        private CursorHandle mStartHandle;
        private CursorHandle mEndHandle;

        private boolean mIsShowing;

        public SelectionCursorController() {
            mStartHandle = new CursorHandle(this);
            mEndHandle = new CursorHandle(this);
        }

        public void snapToSelection() {

            if (mIsShowing) {
                int a = CustomTextView.this.getCursorSelection().getStart();
                int b = CustomTextView.this.getCursorSelection().getEnd();

                int start = Math.min(a, b);
                int end = Math.max(a, b);

                // find the corresponding handle for the start/end calculated above
                CursorHandle startHandle = start == a ? mStartHandle : mEndHandle;
                CursorHandle endHandle = end == b ? mEndHandle : mStartHandle;

                final int[] coords = mTempCoords;

                int scroll_y = CustomTextView.this.getScrollYInternal();
                int scroll_x = CustomTextView.this.getScrollXInternal();

                CustomTextView.this.getAdjusteStartXY(start, scroll_x, scroll_y, coords);
                startHandle.pointTo(coords[0], coords[1]);

                CustomTextView.this.getAdjustedEndXY(end, scroll_x, scroll_y, coords);
                endHandle.pointTo(coords[0], coords[1]);
                mOnCursorStateChangedListener.onDragEnds(coords[0], coords[1]);
            }
        }

        public void show(int start, int end) {

            int a = Math.min(start, end);
            int b = Math.max(start, end);

            final int[] coords = mTempCoords;
            int scroll_y = CustomTextView.this.getScrollY();
            int scroll_x = CustomTextView.this.getScrollX();

            CustomTextView.this.getAdjusteStartXY(a, scroll_x, scroll_y, coords);
            mStartHandle.show(coords[0], coords[1]);

            CustomTextView.this.getAdjustedEndXY(b, scroll_x, scroll_y, coords);
            mEndHandle.show(coords[0], coords[1]);

            if (mOnCursorStateChangedListener != null) {
                mOnCursorStateChangedListener.onDragEnds(coords[0], coords[1]);
            }

            mIsShowing = true;
            select(a, b);
        }

        public void hide() {
            if (mIsShowing) {
                CustomTextView.this.removeSelection();
                mStartHandle.hide();
                mEndHandle.hide();
                mIsShowing = false;

                //if (mOnCursorStateChangedListener != null) {
                //  mOnCursorStateChangedListener.onHideCursors(SelectableTextView.this);
                //}
            }
        }

        public void updatePosition(CursorHandle cursorHandle, int x, int y, int oldx, int oldy) {
            if (!mIsShowing) {
                return;
            }

            int old_offset =
                    cursorHandle == mStartHandle ? CustomTextView.this.getCursorSelection().getStart()
                            : CustomTextView.this.getCursorSelection().getEnd();

            int offset = CustomTextView.this.getHysteresisOffset(x, y, old_offset);

            if (offset != old_offset) {
                if (cursorHandle == mStartHandle) {
                    CustomTextView.this.getCursorSelection().setStart(offset);
                } else {
                    CustomTextView.this.getCursorSelection().setEnd(offset);
                }
                CustomTextView.this.getCursorSelection().select();
            }

            //
            cursorHandle.pointTo(x, y);
            if (mOnCursorStateChangedListener != null) {
                mOnCursorStateChangedListener.onPositionChanged(CustomTextView.this, x, y, oldx, oldy);
            }
        }

        private void select(int start, int end) {
            CustomTextView.this.setSelection(Math.min(start, end), Math.abs(end - start));
        }

        @SuppressWarnings("unused")
        public boolean isShowing() {
            return mIsShowing;
        }

        @SuppressWarnings("unused")
        public void onDetached() {
            // don't know what to do here
        }

        @Override
        public void onTouchModeChanged(boolean isInTouchMode) {
            if (!isInTouchMode) {
                hide();
            }
        }
    }

    /**
     * represents a single cursor
     */
    private class CursorHandle extends View {

        private final PopupWindow mContainer;

        /**
         * the drawble of the cursor
         */
        private Drawable mDrawable;

        /**
         * whether the user is dragging the cursor
         */
        @SuppressWarnings("unused")
        private boolean mIsDragging;

        /**
         * the controller that's controlling the cursor
         */
        private SelectionCursorController mController;

        /**
         * the height of the cursor
         */
        private int mHeight;

        /**
         * the width of the cursor
         */
        private int mWidth;

        /**
         * the x coordinate of the "pointer" of the cursor
         */
        private int mHotspotX;

        /**
         * the y coordinate of the "pointer" of the cursor which is usually the top, so it's zero.
         */
        private int mHotspotY;

        /**
         * Adjustment to add to the Raw x, y coordinate of the touch position to get the location of
         * where
         * the cursor is pointing to
         */
        private int mAdjustX;
        private int mAdjustY;

        private int mOldX;
        private int mOldY;

        public CursorHandle(SelectionCursorController controller) {
            super(CustomTextView.this.getContext());

            mController = controller;

            mDrawable = ContextCompat.getDrawable(getContext(), R.drawable.cursor);

			/* My Note
             At first I tried using mContainer = new PopupWindow(SelectableTextView.this.getContext())
             and mContainer.setContentView(this) in the show() method AND FAILED to draw the
             PopupWindow properly (e.g. PopupWindow can't contain the whole drawable, background
             of the PopupWindow is not transparent. I think it's because calling
             new PopupWindow(context) uses the internal's default style which messes up
			 */
            mContainer = new PopupWindow(this);
            // mContainer.setSplitTouchEnabled(true);
            mContainer.setClippingEnabled(false);

			/* My Note
             getIntrinsicWidth() returns the width of the drawable after it has been
             scaled to the current device's density
             e.g. if the drawable is a 15 x 20 image and we load the image on a Nexus 4 (which
             has a density of 2.0), getIntrinsicWidth() shall return 15 * 2 = 30
			 */
            mHeight = mDrawable.getIntrinsicHeight();
            mWidth = mDrawable.getIntrinsicWidth();

            // the PopupWindow has an initial dimension of (0, 0)
            // must set the width/height of the popupwindow in order for it to be drawn
            mContainer.setWidth(mWidth);
            mContainer.setHeight(mHeight);

            // this is the location of where the pointer is relative to the cursor itself
            // if the left and right cursor are different, mHotspotX will need to be calculated
            // differently for each cursor. Currently, I'm using the same left and right cursor
            mHotspotX = mWidth / 2;
            mHotspotY = 0;

            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(mWidth, mHeight);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            mDrawable.setBounds(0, 0, mWidth, mHeight);
            mDrawable.draw(canvas);
        }

        @Override
        public boolean /*CursorHandle::*/onTouchEvent(MotionEvent event) {

            int rawX = (int) event.getRawX();
            int rawY = (int) event.getRawY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    // calculate distance from the (x,y) of the finger to where the cursor
                    // points to
                    mAdjustX = mHotspotX - (int) event.getX();
                    mAdjustY = mHotspotY - (int) event.getY();
                    mOldX = mAdjustX + rawX;
                    mOldY = mAdjustY + rawY;

                    mIsDragging = true;
                    if (CustomTextView.this.mOnCursorStateChangedListener != null) {
                        CustomTextView.this.mOnCursorStateChangedListener.onDragStarts(
                                CustomTextView.this);
                    }
                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    mIsDragging = false;
                    mController.snapToSelection();
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    // calculate the raw (x, y) the cursor is POINTING TO
                    int x = mAdjustX + rawX;
                    int y = mAdjustY + rawY;

                    mController.updatePosition(this, x, y, mOldX, mOldY);
                    mOldX = x;
                    mOldY = y;

                    break;
                }
            }
            return true; // consume the event
        }

        public boolean isShowing() {
            return mContainer.isShowing();
        }

        public void show(int x, int y) {
            final int[] coords = mTempCoords;
            CustomTextView.this.getLocationInWindow(coords);

            coords[0] += x - mHotspotX;
            coords[1] += y - mHotspotY;
            mContainer.showAtLocation(CustomTextView.this, Gravity.NO_GRAVITY, coords[0], coords[1]);
        }

        private void pointTo(int x, int y) {
            if (isShowing()) {
                mContainer.update(x - mHotspotX, y - mHotspotY, -1, -1);
            }
        }

        /**
         * hide this cursor
         */
        public void hide() {
            mIsDragging = false;
            mContainer.dismiss();
        }
    }

    public interface OnCursorStateChangedListener {

        void onDragStarts(View v);

        void onPositionChanged(View v, int x, int y, int oldx, int oldy);

        void onDragEnds(int endHandleX, int endHandleY);
    }
}