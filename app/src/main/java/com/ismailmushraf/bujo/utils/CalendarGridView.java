package com.ismailmushraf.bujo.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

/** Calendar grid exposes the click path alongside its month-swipe gesture. */
public class CalendarGridView extends GridView {
    public CalendarGridView(Context context, AttributeSet attributes) { super(context, attributes); }
    @Override public boolean performClick() { return super.performClick(); }
}
