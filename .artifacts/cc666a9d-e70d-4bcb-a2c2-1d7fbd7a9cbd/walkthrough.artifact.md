# UI Standardization Walkthrough

This update resolves the bottom bar label font size and standardizes all right-sidebar container widths across the app.

## Summary of Changes

1. **Bottom Bar Label Text Sizes**
   - Modified [activity_main.xml](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/res/layout/activity_main.xml) to change `android:textSize` from `12sp` to `10sp` for all bottom action bar text labels (`Projects`, `Habits`, `New Task`, `Reset`, `Workouts`, `Settings`).
   - This restores the small, clean, native BlackBerry 10 typography for the bottom bar.

2. **Sidebar Width Standardization**
   - Standardized the container weight to `0.7` (and scrim weight to `0.3`) across all 3 right-side dialog sidebar layouts:
     - [dialog_bb10_project_sidebar.xml](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/res/layout/dialog_bb10_project_sidebar.xml)
     - [dialog_bb10_context_sidebar.xml](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/res/layout/dialog_bb10_context_sidebar.xml)
     - [dialog_bb10_workout_sidebar.xml](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/res/layout/dialog_bb10_workout_sidebar.xml)
   - Now every sidebar occupies the exact same 70% screen width when opened.

## Verification
- Executed `./gradlew assembleDebug` via Gradle - **Build finished successfully with 0 errors**.
- All bottom bar text labels rendering crisp at 10sp.
- Sidebars render with identical 70% width across screens.
