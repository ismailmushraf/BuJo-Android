# Bottom Bar Text Size & Sidebar Width Standardization Plan

I have researched your request and identified the cause of both UI issues:

1. **Bottom Bar Text Size**: In `activity_main.xml`, the bottom bar labels (`Projects`, `Habits`, `New Task`, `Reset`, `Workouts`, `Settings`) are set to `12sp`. Lowering them back to `10sp` will make them smaller and match BB10 guidelines.
2. **Sidebar Width Inconsistency**: The right-side dialog sidebars should all share an identical `0.3` (scrim) / `0.7` (container) weight ratio (70% screen width). Standardizing this across all sidebar layouts ensures every sidebar takes the exact same width.

## Proposed Changes

Only the following **4 XML layout files** will be modified. No other code, existing functionality, or UI components will be touched:

### [activity_main.xml](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/res/layout/activity_main.xml)
- Change `android:textSize` from `12sp` to `10sp` for all 6 bottom action bar text labels.

### [dialog_bb10_project_sidebar.xml](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/res/layout/dialog_bb10_project_sidebar.xml)
- Set container `layout_weight` to `0.7` (and scrim weight to `0.3`) to match the primary context sidebar width.

### [dialog_bb10_context_sidebar.xml](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/res/layout/dialog_bb10_context_sidebar.xml)
- Ensure container `layout_weight` is set to `0.7` and scrim weight is `0.3`.

### [dialog_bb10_workout_sidebar.xml](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/res/layout/dialog_bb10_workout_sidebar.xml)
- Ensure container `layout_weight` is set to `0.7` and scrim weight is `0.3`.

---

## User Review Required

Please review the list of **4 files to modify** above. Let me know if you approve so I can begin execution!
