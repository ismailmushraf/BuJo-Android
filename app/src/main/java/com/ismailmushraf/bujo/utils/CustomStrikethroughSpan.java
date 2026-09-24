package com.ismailmushraf.bujo.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.ReplacementSpan;

public class CustomStrikethroughSpan extends ReplacementSpan {

    private final int textColor;
    private final int lineColor;

    public CustomStrikethroughSpan(int textColor, int lineColor) {
        this.textColor = textColor;
        this.lineColor = lineColor;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        if (fm != null) {
            Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
            fm.top = fontMetrics.top;
            fm.ascent = fontMetrics.ascent;
            fm.descent = fontMetrics.descent;
            fm.bottom = fontMetrics.bottom;
        }
        return (int) paint.measureText(text, start, end);
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        int originalColor = paint.getColor();
        paint.setColor(textColor);
        canvas.drawText(text, start, end, x, y, paint);

        Paint linePaint = new Paint(paint);
        linePaint.setColor(lineColor);
        linePaint.setStrokeWidth(2.5f);
        float width = paint.measureText(text, start, end);
        float centerY = y + (paint.ascent() + paint.descent()) / 2f;
        canvas.drawLine(x, centerY, x + width, centerY, linePaint);

        paint.setColor(originalColor);
    }
}