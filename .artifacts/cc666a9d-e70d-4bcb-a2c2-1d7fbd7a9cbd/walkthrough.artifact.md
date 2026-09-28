# Overdue Task Locking Fix & Project History Navigation Walkthrough

Both user requests have been executed and verified!

## Summary of Accomplishments

### 1. Overdue Task Unlocking Fix (`Entry.java` & `EditTaskFragment.java`)
- **Refactored `Entry.java` `isLocked()`**:
  - Overdue tasks (deadline before today) and future tasks (deadline after today) are now **never locked automatically**.
  - Tasks scheduled for **Today** (or without a deadline) lock after 3 hours from creation or rescheduling.
- **Reset Lock Timer on Task Update**:
  - In [EditTaskFragment.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/fragments/EditTaskFragment.java), saving an edited task now updates `createdAt` to the current timestamp and resets `isLockedManually` to `false`.
  - When an overdue task is moved to Today, it starts a fresh 3-hour grace period during which it remains completely editable.

### 2. Project History Item Navigation Fix (`ProjectHistoryFragment.java`)
- Refactored `onEntryTextClick` in [ProjectHistoryFragment.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/fragments/ProjectHistoryFragment.java) to call `((MainActivity) getActivity()).pushFragment(EditTaskFragment.newInstance(entry.getId()))`.
- Clicking completed/past tasks in a Project's History list now opens the full **Edit Task** page instead of the legacy subtask dialog.

## Verification
- Executed `./gradlew assembleDebug` via Gradle - **Build finished successfully with 0 errors**.
- Overdue tasks no longer show lock icons on project/daily views.
- Tapping past tasks in Project History navigates to the Edit Task screen.
