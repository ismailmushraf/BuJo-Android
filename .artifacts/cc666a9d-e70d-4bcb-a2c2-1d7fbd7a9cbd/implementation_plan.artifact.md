# BB10 Redesign for New Habit Modal

This plan details updating the "New Habit" creation modal from a native Android `AlertDialog` to the custom BlackBerry 10 dialog style used by the "Plan Day" modal (`dialog_plan_day.xml`).

## Proposed Changes

### 1. New BB10 Dialog Layout
#### [NEW] [dialog_add_habit.xml](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/res/layout/dialog_add_habit.xml)
- Create a dedicated BB10 modal layout file matching the structure of `dialog_plan_day.xml`:
  - **Outer Container**: Card shape `@drawable/shape_bb10_confirm_card`.
  - **Header**: Bold title ("New Habit Commitment") on `@color/surface`.
  - **Divider**: Full-width `1dp` `@color/bujo_divider`.
  - **Content Body**:
    - Habit Name `EditText` (`@color/surface`).
    - Commitment Duration Label & `EditText` (defaults to 30 days).
    - Optional Target Time Selection Button (`btn_habit_time`).
  - **Divider**: Full-width `1dp` `@color/bujo_divider`.
  - **Footer Action Bar**: Horizontal side-by-side action buttons for **Cancel** and **Commit** (`@color/bb10_blue`) separated by a vertical `1dp` divider.

---

### 2. Update Habits Fragment Logic
#### [HabitsFragment.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/fragments/HabitsFragment.java)
- Refactor `showAddHabitDialog()`:
  - Inflate `dialog_add_habit.xml` instead of building native `LinearLayout` / `EditText` views in Java.
  - Wire up the "Set Target Time" button to open the TimePickerDialog and update button text upon selection.
  - Wire up the "Cancel" and "Commit" button click listeners.
  - Apply `dialog.setOnShowListener` to set 88% screen width (`metrics.widthPixels * 0.88`) for consistent BB10 card sizing.

## User Review Required

Please review the proposed plan above. Let me know if you approve so I can begin execution!
