package com.ismailmushraf.bujo.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.LineBackgroundSpan;

/** Draws a strike-through per laid-out line without replacing or remeasuring the text. */
public final class RedStrikethroughLineSpan implements LineBackgroundSpan {
    private final int color;

    public RedStrikethroughLineSpan(int color) {
        this.color = color;
    }

    @Override
    public void drawBackground(Canvas canvas, Paint paint, int left, int right, int top,
                               int baseline, int bottom, CharSequence text, int start,
                               int end, int lineNumber) {
        if (start >= end) return;

        Paint linePaint = new Paint(paint);
        linePaint.setColor(color);
        linePaint.setStrokeWidth(2.5f);
        float textEnd = Math.min(right, left + linePaint.measureText(text, start, end));
        float centerY = baseline + (paint.ascent() + paint.descent()) / 2f;
        canvas.drawLine(left, centerY, textEnd, centerY, linePaint);
    }
}
