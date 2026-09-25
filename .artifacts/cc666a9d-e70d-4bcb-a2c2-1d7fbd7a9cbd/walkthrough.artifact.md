# Side Bar and Workouts Tweaks Complete

I've executed all items detailed in the plan, addressing both the Workout page adjustments and the global Sidebar changes.

## Summary of Changes

1. **FAB Removal from Workouts Page**
   - Modified `MainActivity.java` inside `updateBottomBarButtons()` to listen for `WorkoutFragment` and `WorkoutHistoryFragment`.
   - The central Floating Action Button (FAB), its label, and the Overflow button are now explicitly hidden when browsing the Workouts sections.

2. **Removed "Journal Index" Header**
   - Removed the hardcoded section header `DrawerItem.TYPE_SECTION` in `MainActivity.java`'s `refreshDrawer()` method. The drawer list now starts directly with "Inbox".

3. **Centered Primary Sidebar Items (Global Change)**
   - Modified `dialog_bb10_context_sidebar.xml` (Task/Habit/Workout Long Press Menu).
   - Modified `dialog_bb10_project_sidebar.xml` (Project Long Press Menu).
   - Modified `activity_main.xml` (Main Navigation Drawer).
   - For all 3 files, I wrapped the middle `ListView` inside a `RelativeLayout` and applied `android:layout_centerVertical="true"` alongside `android:layout_height="wrap_content"`.
   - This effectively floats the primary interaction items perfectly in the vertical center of the sidebar, leaving the fixed top headers and fixed bottom action bars (like "Delete") undisturbed.

## Verification
- Open the app and open the Main Navigation drawer. Observe that "Journal Index" is gone, and the navigation items sit securely in the middle of the screen.
- Long press on a Task or Project to verify that the Context/Project sidebar items are vertically centered, while the Title stays at the top and "Delete" stays at the bottom.
- Navigate to the Workouts page. Verify the central BB10 FAB (and its "New Task" text) completely disappears.