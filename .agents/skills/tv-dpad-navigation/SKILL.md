---
name: tv-dpad-navigation
description: Enforces 10-foot UI design, overscan safety, and strict D-pad remote control focus traversal for Android TV.
---

# Skill: Android TV D-Pad & 10-Foot UI Architecture

## Rules
1. **Overscan Margins:** Every top-level screen container MUST apply `Modifier.padding(horizontal = 48.dp, vertical = 27.dp)` so content never clips on older TV panels.
2. **Focus Visibility:** Every interactive composable (`Button`, `Card`, `IconButton`, date arrows, audio selectors) MUST use `androidx.tv.material3` components or explicit `Modifier.onFocusChanged` styling that increases scale (`1.05f`) and applies a high-contrast border (`2.dp`) when focused.
3. **Initial Focus:** Use `FocusRequester` + `LaunchedEffect(Unit)` to assign deterministic initial focus when a screen or dialog opens so the user never gets "lost" with an invisible cursor.
4. **D-Pad Date Traversal (Home Mode):**
   - Map `Key.DirectionLeft` and `Key.DirectionRight` on the date header bar to decrement/increment the selected `LocalDate` by 1 day.
   - Provide a quick action button or `Key.Back` handler (when viewing a non-today date) to snap immediately back to `LocalDate.now()`.
5. **Typography Scale:**
   - Active/Next Prayer Time: `64.sp` to `80.sp` Bold.
   - Inactive Prayer Times: `32.sp` to `40.sp` SemiBold.
   - Mosque Name / Main Header: `36.sp` to `44.sp` Bold.
   - Minimum legible label size anywhere in the app: `18.sp`.