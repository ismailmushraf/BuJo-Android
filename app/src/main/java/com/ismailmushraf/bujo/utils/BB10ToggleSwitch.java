package com.ismailmushraf.bujo.utils;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

public class BB10ToggleSwitch extends FrameLayout {

    public interface OnCheckedChangeListener {
        void onCheckedChanged(BB10ToggleSwitch toggleSwitch, boolean isChecked);
    }

    private boolean isChecked = false;
    private View thumb;
    private GradientDrawable trackDrawable;
    private OnCheckedChangeListener listener;

    private static final int COLOR_OFF = 0xFFCCCCCC; // Light Grey
    private static final int COLOR_ON = 0xFF00A8DF;  // Cyan Blue

    public BB10ToggleSwitch(Context context) {
        super(context);
        init(context);
    }

    public BB10ToggleSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BB10ToggleSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int widthPx = (int) (44 * dm.density);
        int heightPx = (int) (24 * dm.density);
        int thumbSizePx = (int) (20 * dm.density);
        int marginPx = (int) (2 * dm.density);

        setLayoutParams(new LayoutParams(widthPx, heightPx));

        trackDrawable = new GradientDrawable();
        trackDrawable.setShape(GradientDrawable.RECTANGLE);
        trackDrawable.setCornerRadius(12 * dm.density);
        trackDrawable.setColor(COLOR_OFF);
        setBackgroundDrawable(trackDrawable);

        thumb = new View(context);
        LayoutParams thumbLp = new LayoutParams(thumbSizePx, thumbSizePx);
        thumbLp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
        thumbLp.leftMargin = marginPx;
        thumb.setLayoutParams(thumbLp);

        GradientDrawable thumbDrawable = new GradientDrawable();
        thumbDrawable.setShape(GradientDrawable.OVAL);
        thumbDrawable.setColor(Color.WHITE);
        thumb.setBackgroundDrawable(thumbDrawable);

        addView(thumb);

        setClickable(true);
        setFocusable(true);
        setOnClickListener(v -> toggle());
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        setChecked(checked, false);
    }

    public void setChecked(boolean checked, boolean animate) {
        if (this.isChecked == checked) return;
        this.isChecked = checked;

        DisplayMetrics dm = getResources().getDisplayMetrics();
        float travelDistance = 20 * dm.density;

        if (animate) {
            float startX = checked ? 0f : travelDistance;
            float endX = checked ? travelDistance : 0f;

            ValueAnimator animator = ValueAnimator.ofFloat(startX, endX);
            animator.setDuration(200);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                float val = (float) animation.getAnimatedValue();
                thumb.setTranslationX(val);

                float fraction = animation.getAnimatedFraction();
                int currentColor = evaluateColor(checked ? fraction : (1f - fraction), COLOR_OFF, COLOR_ON);
                trackDrawable.setColor(currentColor);
            });
            animator.start();
        } else {
            thumb.setTranslationX(checked ? travelDistance : 0f);
            trackDrawable.setColor(checked ? COLOR_ON : COLOR_OFF);
        }

        if (listener != null) {
            listener.onCheckedChanged(this, this.isChecked);
        }
    }

    public void toggle() {
        setChecked(!isChecked, true);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    private int evaluateColor(float fraction, int startValue, int endValue) {
        int startA = (startValue >> 24) & 0xff;
        int startR = (startValue >> 16) & 0xff;
        int startG = (startValue >> 8) & 0xff;
        int startB = startValue & 0xff;

        int endA = (endValue >> 24) & 0xff;
        int endR = (endValue >> 16) & 0xff;
        int endG = (endValue >> 8) & 0xff;
        int endB = endValue & 0xff;

        return ((startA + (int) (fraction * (endA - startA))) << 24) |
                ((startR + (int) (fraction * (endR - startR))) << 16) |
                ((startG + (int) (fraction * (endG - startG))) << 8) |
                ((startB + (int) (fraction * (endB - startB))));
    }
}
