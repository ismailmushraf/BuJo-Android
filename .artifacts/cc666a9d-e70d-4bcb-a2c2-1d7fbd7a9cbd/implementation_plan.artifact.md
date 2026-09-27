# Profile Page Redesign: Concise BB10 Widgets

This plan outlines the redesign of the Profile page (`ProfileFragment` & `fragment_profile.xml`) to make all widgets short, compact, and aligned with BlackBerry 10 typography and layout standards.

## Proposed Changes

### 1. Compact Rank / Level Widget
#### [fragment_profile.xml](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/res/layout/fragment_profile.xml)
- **Compact Horizontal Layout**: Replace the current large centered vertical box with a compact, single-row container:
  - **Far Left**: Rank Icon (`tv_rank_icon`, `36dp x 36dp`).
  - **Center Column**:
    - **Row 1**: Level Title (`tv_rank_title`, `14sp` bold).
    - **Row 2**: XP Progress Text (`tv_rank_progress_text`, `11sp`).
    - **Row 3**: Slim Level Progress Bar (`progress_rank`, `4dp` height).
  - **Far Right**: Expansion Chevron Icon (`iv_expand_chevron`, `16dp x 16dp`, `@drawable/ic_bb10_chevron_down`).
- **Points Breakdown Layout**: Keep `layout_points_breakdown` hidden by default directly below the rank row.

#### [ProfileFragment.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/fragments/ProfileFragment.java)
- **Smooth Slide Animation**: Replace `View.GONE` / `View.VISIBLE` instant toggle with a smooth expand/collapse slide transition (`TransitionManager.beginDelayedTransition` or custom height/alpha animation).
- **Chevron Rotation**: Flip `iv_expand_chevron` between `@drawable/ic_bb10_chevron_down` and `@drawable/ic_bb10_chevron_up` when toggled.

---

### 2. Compact Discipline Metrics Row
#### [fragment_profile.xml](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/res/layout/fragment_profile.xml)
- Combine **Current Streak**, **Rest Tokens**, and **Longest Streak** into a single compact horizontal 3-column row separated by thin `1dp` dividers (`@color/bujo_divider`):
  - Each item: `20dp` icon, `13sp` bold value, and `10sp` caption.
  - Eliminates the large stacked boxes and reduces section height by over 60%.

---

### 3. Compact Habit Discipline Summary
#### [fragment_profile.xml](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/res/layout/fragment_profile.xml)
- Reduce vertical padding of the **Habit Discipline** (30-day summary) bar.
- Format **Perfect Days** and **Avg Success** into a slim horizontal split-bar with `14sp` bold numbers and `10sp` captions.

---

### 4. Compact Goal Efficiency List
#### [item_goal_efficiency.xml](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/res/layout/item_goal_efficiency.xml)
- Reduce row height and padding (`12dp` top/bottom).
- Slim down the goal progress bar height to `4dp` with `12sp` project names and `11sp` efficiency percentage labels.

---

## User Review Required

### Additional Suggestions
1. **Unified Flat Backgrounds**: Use flat white/surface backgrounds for all profile sections with standard `1dp` dividers (`@color/bujo_divider`) rather than rounded cards, matching the BB10 Settings and Workouts design.
2. **Animation**: Use Android's native `AutoTransition` / `TransitionManager` for smooth, zero-jank slide expansion of the points breakdown.

Please review this implementation plan and let me know if you approve so I can begin execution!
