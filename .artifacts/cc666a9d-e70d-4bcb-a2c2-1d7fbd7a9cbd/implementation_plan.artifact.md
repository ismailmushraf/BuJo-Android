# BlackBerry 10 Active Frame Widget & Task Locking Restriction

Yes! In BlackBerry 10, when an app is minimized to the multitasking grid, it transforms into an **Active Frame** (a live card displaying key real-time metrics like overdue tasks and daily status).

On Android, we can achieve this exact experience using **two complementary features**, alongside the requested restriction on task locking for non-today items:

1. **Android Home Screen App Widget (BB10 Active Frame)**: A dedicated home screen widget styled identically to a BlackBerry 10 Active Frame. It updates in real-time with your "X Overdue Tasks" and task titles.
2. **Task Lock Restriction**: Restrict the "Lock Task" sidebar option so it only appears for tasks scheduled for Today. Overdue and future tasks cannot be locked.

---

## Proposed Changes

### 1. BB10 Active Frame App Widget Layout
#### [NEW] [widget_active_frame.xml](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/res/layout/widget_active_frame.xml)
- Create a BB10 Active Frame card layout for the Home Screen Widget:
  - **Header**: Dark background with blue accent bar (`@color/bb10_blue`) titled "BULLET JOURNAL".
  - **Status Section**: Displays "X Overdue Tasks" in bold red (`@color/bb10_folder_red`) or "All Tasks Clear!".
  - **Task List Area**: Shows up to 3-4 top overdue/today task titles.
  - **Footer**: Displays completion ratio (e.g. "3/5 Completed") and current streak.

#### [NEW] [active_frame_widget_info.xml](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/res/xml/active_frame_widget_info.xml)
- Configure widget provider metadata (min width/height, update interval, initial layout).

---

### 2. Widget Provider Implementation
#### [NEW] [BujoActiveFrameWidget.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/utils/BujoActiveFrameWidget.java)
- Create an `AppWidgetProvider` class:
  - Queries `DatabaseManager` for overdue tasks and daily completion stats.
  - Updates `RemoteViews` with the overdue count and task list.
  - Handles tap events (tapping the widget launches `MainActivity` directly into the Today/Overdue view).

#### [MODIFY] [AndroidManifest.xml](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/AndroidManifest.xml)
- Register `BujoActiveFrameWidget` as a broadcast receiver with `android.appwidget.action.APPWIDGET_UPDATE`.

#### [MODIFY] [MainActivity.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/MainActivity.java)
- In `onUserLeaveHint()` / `onPause()`: Trigger a widget broadcast update so the Home Screen widget refreshes instantly whenever the app is minimized.

---

### 3. Restrict "Lock Task" Sidebar Option to Today Only
#### [MODIFY] [EntryUIHelper.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/utils/EntryUIHelper.java)
- Update `showContextDialog()`:
  - Check if the task is for Today (`DatabaseManager.isToday(entry.getDeadline())` or `entry.getDeadline() == 0`).
  - Only add `"Lock Task"` to `optionsList` if the item is a task scheduled for Today. Overdue and future tasks will not show the "Lock Task" option.

## User Review Required

Please review the updated plan above. Let me know if you approve so I can begin execution!
