# Modular Primary Navigation Sidebar with Profile Header

This plan extracts the primary navigation drawer into its own dedicated layout file (`sidebar_primary_nav.xml`) and adds a fixed top header displaying the user's Profile Level, Rank Icon, and Total XP Points.

## Proposed Changes

### 1. Create Primary Navigation Sidebar Layout
#### [NEW] [sidebar_primary_nav.xml](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/res/layout/sidebar_primary_nav.xml)
- Create a dedicated layout file for the primary right sidebar (`300dp` width, `layout_gravity="end"`):
  - **Top Fixed Section** (`#drawer_top_section`):
    - **Rank Icon** (`#drawer_profile_icon`, `32dp x 32dp` `AppCompatImageView` tinted `@color/bb10_blue`).
    - **Text Column**:
      - **Level Title** (`#drawer_profile_level`, `14sp` bold) e.g., "LEVEL 1: WANDERER".
      - **Points** (`#drawer_profile_points`, `12sp`) e.g., "350 XP".
  - **Top Divider** (`#drawer_top_divider`, `1dp` `@color/bb10_sidebar_divider`).
  - **Centered Navigation List** (`#nav_drawer_list` `ListView` positioned below `#drawer_top_divider`).

---

### 2. Include Modular Sidebar in Main Activity
#### [activity_main.xml](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/res/layout/activity_main.xml)
- Replace inline `#nav_drawer_container` definition with a clean `<include layout="@layout/sidebar_primary_nav.xml" />` tag inside `DrawerLayout`.

---

### 3. Wire Up Dynamic Profile Stats & Navigation
#### [MainActivity.java](file:///Users/ismailmushraf/AndroidStudioProjects/BuJo-Android/app/src/main/java/com/ismailmushraf/bujo/MainActivity.java)
- In `onCreate()`: Bind `#drawer_top_section`, `#drawer_profile_level`, `#drawer_profile_points`, and `#drawer_profile_icon`.
- Implement `refreshProfileIcon()`:
  - Fetch user stats from `dbManager.getUserStats()`.
  - Calculate active level title and select appropriate rank icon (`ic_bb10_profile_wanderer`, `ic_bb10_profile_book`, `ic_bb10_profile_scholar`, `ic_bb10_profile_mountain`, `ic_bb10_profile_temple`).
  - Update level title and points text.
- Add click listener on `#drawer_top_section`: Tapping the header closes the sidebar and navigates directly to `ProfileFragment`.

## User Review Required

Please review the proposed plan above. Let me know if you approve so I can begin execution!
