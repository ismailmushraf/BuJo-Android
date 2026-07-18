package com.ismailmushraf.bujo.models;

public class DrawerItem {
    public String title;
    public int iconResId; // 0 if no icon
    public boolean isHeader;

    public DrawerItem(String title, int iconResId, boolean isHeader) {
        this.title = title;
        this.iconResId = iconResId;
        this.isHeader = isHeader;
    }
}
