# Side Bar Redesign & FAB Removal

I have fully understood your request. Here is the concise analysis of the right-side bar menus and the plan to execute all your requirements.

## 1. Analysis of Right Side Bar Menus
There are exactly **3 right-side bar menus** in the app:
1. **Context Sidebar Menu** (`dialog_bb10_context_sidebar.xml`): Used when long-pressing tasks, habits, and workouts. (Contains top & bottom fixed rows).
2. **Project Sidebar Menu** (`dialog_bb10_project_sidebar.xml`): Used when long-pressing projects. (Contains top & bottom fixed rows).
3. **Main Navigation Drawer** (`activity_main.xml`): The default startup screen side bar menu. (Does *not* contain top & bottom fixed rows).

## 2. Execution Plan

### A. Remove the FAB from the Workout Page
- **File:** `MainActivity.java`
- **Action:** Update the `updateBottomBarButtons()` method to hide the FAB whenever the active fragment is `WorkoutFragment` (or `WorkoutHistoryFragment`).

### B. Remove "Journal Index" Text
- **File:** `MainActivity.java`
- **Action:** Remove the `DrawerItem.TYPE_SECTION` line that adds the "JOURNAL INDEX" text header in the `refreshDrawer()` method.

### C. Center Primary Items in all Sidebars
- **File:** `dialog_bb10_context_sidebar.xml`
  - **Action:** Wrap the `ListView` inside a `RelativeLayout` that fills the middle section, and apply `layout_centerVertical="true"` and `layout_height="wrap_content"` to the `ListView` so the items float in the middle.
- **File:** `dialog_bb10_project_sidebar.xml`
  - **Action:** Same as above for its `ListView`.
- **File:** `activity_main.xml` (Navigation Drawer)
  - **Action:** Wrap the `nav_drawer_list` `ListView` inside a `RelativeLayout` (handling the `layout_gravity="end"`), and apply `layout_centerVertical="true"` and `layout_height="wrap_content"` to the `ListView`.

## User Review Required
Please review the analysis and plan. Let me know if you approve so I can proceed with the execution.