package com.ismailmushraf.bujo.models;

public class DrawerItem {
    public static final int TYPE_SECTION = 0;
    public static final int TYPE_ITEM = 1;
    public static final int TYPE_PROJECT = 2;

    private int type;
    public String title;
    public String icon;
    public int projectId; // for project items

    public DrawerItem(int type, String title, String icon) {
        this.type = type;
        this.title = title;
        this.icon = icon;
    }

    public DrawerItem(int type, String title, String icon, int projectId) {
        this.type = type;
        this.title = title;
        this.icon = icon;
        this.projectId = projectId;
    }

    public int getType() {
        return type;
    }
}
