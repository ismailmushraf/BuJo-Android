# Architectural Cleanup & Optimization Plan

This implementation plan addresses the anti-patterns and performance bottlenecks identified in the codebase audit prior to starting the Profile page redesign.

## Overview of Proposed Changes

### 1. Gamification Logic Centralization
#### [NEW] [GamificationManager.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/utils/GamificationManager.java)
- Create a dedicated manager class to house point values, level rank threshold definitions, and XP award calculations.
- Centralize points constants:
  - `POINTS_WORKOUT_SET = 10`
  - `POINTS_DAILY_COMMITMENT = 5`
  - `POINTS_TASK_COMPLETION = ...`
  - `POINTS_HABIT_LOG = ...`
- Refactor point awards in [DailyLogFragment.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/fragments/DailyLogFragment.java), [WorkoutFragment.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/fragments/WorkoutFragment.java), [HabitsFragment.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/fragments/HabitsFragment.java), [InboxFragment.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/fragments/InboxFragment.java), [ProjectDetailFragment.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/fragments/ProjectDetailFragment.java), and [EntryUIHelper.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/utils/EntryUIHelper.java) to delegate calculations to `GamificationManager`.

---

### 2. Preference Storage Centralization
#### [NEW] [AppPreferences.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/utils/AppPreferences.java)
- Create `AppPreferences.java` to encapsulate `SharedPreferences` operations and key definitions:
  - `KEY_STARTUP_SCREEN = "startup_screen"`
  - `KEY_ENABLE_ANIMATIONS = "enable_animations"`
  - `KEY_ENABLE_SOUNDS = "enable_sounds"`
  - `KEY_SOUND_EFFECTS = "enable_sound_effects"`
- Refactor [MainActivity.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/MainActivity.java), [SettingsFragment.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/fragments/SettingsFragment.java), and [SoundHelper.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/utils/SoundHelper.java) to use `AppPreferences`.

---

### 3. ListView Adapter Performance Optimization (Eliminate Scroll Jank)
#### [MODIFY] [EntryAdapter.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/adapters/EntryAdapter.java)
- Cache Calendar instances and reusable listener instances instead of instantiating `Calendar.getInstance()`, `Date`, `SpannableString`, `CustomStrikethroughSpan`, and `OnLongClickListener` on every `getView()` pass.
- Reduce object allocations during list scrolling to prevent GC pauses.

---

### 4. Fragment Navigation Method Centralization
#### [MODIFY] [MainActivity.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/MainActivity.java)
- Expose a public `pushFragment(Fragment fragment)` method that encapsulates the custom animation transaction (`R.anim.slide_in_right`, etc.) and automatically calls `updateBottomBarButtons(fragment)`.
- Refactor fragment replacements in `DailyLogFragment`, `WorkoutFragment`, `ProjectDetailFragment`, and `InboxFragment` to call `((MainActivity) getActivity()).pushFragment(...)`.

---

### 5. Background Offloading for Database Operations
#### [NEW] [AppExecutors.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/utils/AppExecutors.java)
- Create a lightweight background executor class (`AppExecutors`) containing a single background disk thread executor and a main UI thread handler.
- Wrap heavy database reads and mutations (such as `evaluateDailyStreak()`, audit evaluations, complex habit log calculations) in `AppExecutors.diskIO().execute(...)` so that database tasks run off the UI main thread and post callbacks safely to the main thread.

---

### 6. Robust Exception & Resource Cleanup
#### [MODIFY] [DatabaseManager.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/db/DatabaseManager.java) & [SoundHelper.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/utils/SoundHelper.java)
- Ensure all cursor operations in `DatabaseManager` utilize `try-finally` blocks so cursors are guaranteed to close even on unexpected exceptions.
- Add fallback handling in `SoundHelper` if audio playback initialization fails.

## Verification Plan

### Automated Build Verification
- Execute `./gradlew assembleDebug` via `gradle_build` to verify zero compile or resource errors across all components.

### Manual Functional Verification
- Test task/workout/habit points animations to confirm `GamificationManager` correctly awards XP.
- Verify settings toggle options (sounds, animations, startup screen) persist through `AppPreferences`.
- Perform smooth scrolling on Daily Log and Inbox lists to verify zero stuttering.
- Verify background database execution using `AppExecutors` operates smoothly without thread lock or UI freezing.
- Verify back button and screen transitions function properly with centralized `pushFragment(...)`.
