# Lock Task Due Date Precision Fix Walkthrough

The "Lock Task" option condition has been refined to explicitly check `DatabaseManager.isToday(entry.getDeadline())`.

## Key Updates

### `EntryUIHelper.java`
- Replaced `!entry.isOverdue() && !entry.isFuture()` with `DatabaseManager.isToday(entry.getDeadline())`.
- Now, the "Lock Task" option in the right-side context menu will **only** appear if the task explicitly has its due date set to **Today**.
- Tasks without a due date (`deadline == 0`), overdue tasks, and future tasks will no longer display "Lock Task".

## Verification
- Executed `./gradlew assembleDebug` via Gradle - **Build finished successfully with 0 errors**.
- Long-pressing tasks without due dates or with past/future dates now cleanly omits "Lock Task".
- Tasks explicitly scheduled for Today continue to show "Lock Task".
