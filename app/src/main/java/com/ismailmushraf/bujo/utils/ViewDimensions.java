package com.ismailmushraf.bujo.utils;

import android.view.View;

public final class ViewDimensions {
    private ViewDimensions() {}
    public static void setPaddingDp(View view, int start, int top, int end, int bottom) {
        float density = view.getResources().getDisplayMetrics().density;
        view.setPaddingRelative(Math.round(start * density), Math.round(top * density),
                Math.round(end * density), Math.round(bottom * density));
    }
}
