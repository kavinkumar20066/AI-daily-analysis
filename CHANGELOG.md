# Changelog

All notable changes to this project will be documented in this file.

## [1.3.0-dev] - 2026-08-11

### Added — Phase 11: UI/UX Polish & Make Runnable
- **`gradlew` / `gradlew.bat`**: Gradle wrapper shell scripts (Unix + Windows) added to the project root. The project can now be opened in Android Studio without the "Gradle wrapper not found" error.
- **`gradle/wrapper/gradle-wrapper.jar`**: Downloaded and committed. CLI builds via `gradlew assembleDebug` are now fully supported (targeting Gradle 8.6).
- **Screen Entry/Exit Animations** (`NavGraph.kt`): All 14 NavGraph destinations now have explicit `enterTransition`, `exitTransition`, `popEnterTransition`, and `popExitTransition` animations.
  - Bottom-tab switches: smooth `fadeIn(220ms)` / `fadeOut(180ms)` crossfade.
  - Detail/sub-screens: `slideInHorizontally` from the right (300ms tween) on push; reverse slide on back-press.
- **`ActivityCard` Entrance Animation** (`ActivityCard.kt`): Each card fades in + slides up slightly (spring: dampingRatio 0.8, stiffness 400) when it first appears in any list. Uses `AnimatedVisibility` + `LaunchedEffect(activity.id)` to trigger the animation.
- **Shimmer Loading State** (`HomeScreen.kt`): When Excel data is loading, the `HomeScreen` now shows animated shimmer skeleton placeholders (sweep `LinearGradient` on `infiniteRepeatable` transition) instead of a plain `LinearProgressIndicator`. Three shimmer layers are shown: TodayProgressCard skeleton, QuickStats skeleton, and three activity card skeletons.
- **`Offset` import** (`HomeScreen.kt`): Added `androidx.compose.ui.geometry.Offset` import required by the shimmer brush.

### Changed
- **`NavGraph.kt`**: Added `import androidx.compose.animation.core.tween` and replaced the simple, parameterless `NavHost` call with one that declares default animations. Each `composable()` for detail screens now overrides the defaults with horizontal slide animations.
- **`HomeScreen.kt`**: Replaced the `LinearProgressIndicator` loading item with `ShimmerLoadingContent()`.

---

## [1.2.0-dev] - 2026-08-11

### Added
- **`MainActivity.requestNotificationPermission(onResult)`**: Public helper function that requests the `POST_NOTIFICATIONS` runtime permission on Android 13+ (API 33). Internally registers an `ActivityResultLauncher` using `ActivityResultContracts.RequestPermission()`. On API < 33 it calls `onResult(true)` immediately since the permission is not required. The result callback is stored transiently (`notificationPermissionCallback`) and cleared after firing, ensuring no memory leaks.
- **`NotificationPermissionRationaleDialog`** (`SettingsScreen.kt`): A new private composable `AlertDialog` that explains why the `POST_NOTIFICATIONS` permission is needed (friendly message + icon). Shown to the user *before* the system prompt whenever they attempt to enable notifications and the permission is not yet granted.

### Changed
- **`SettingsScreen.kt`**: The notification toggle (`Switch`) now performs a three-step permission gate when the user tries to enable notifications on Android 13+:
  1. Check `ContextCompat.checkSelfPermission()` for `POST_NOTIFICATIONS`.
  2. If not granted → show `NotificationPermissionRationaleDialog`.
  3. On dialog confirm → call `MainActivity.requestNotificationPermission()` → on system grant, enable the notification schedule. If the user denies, the toggle remains off.
  On API < 33 (or if permission was already granted), the toggle enables directly without any dialogs.
- **`MainActivity.kt`**: Removed unused `collectAsState` / `getValue` imports (cleanup). Added `android.Manifest` and `android.os.Build` imports to support the permission launcher.

### Fixed
- N/A

---



## [1.1.0-dev] - 2026-08-11

### Added
- **`DailyReminderWorker`** (`notification/DailyReminderWorker.kt`): WorkManager `CoroutineWorker` that posts a styled push notification via `NotificationCompat`, opening the app when tapped.
- **`NotificationScheduler`** (`notification/NotificationScheduler.kt`): Utility object wrapping WorkManager `PeriodicWorkRequest` scheduling. Computes precise initial delay so the first notification fires at the user-chosen wall-clock time (e.g., 8:00 PM), then repeats every 24 hours. Uses `ExistingPeriodicWorkPolicy.UPDATE` to replace existing schedules cleanly.
- **`BootReceiver`** (`notification/BootReceiver.kt`): `BroadcastReceiver` listening for `ACTION_BOOT_COMPLETED`. Reads saved notification prefs from DataStore and re-queues the WorkManager job if notifications were enabled, ensuring reminders survive device reboots.
- **`SettingsViewModel`** (`ui/screens/settings/SettingsViewModel.kt`): `AndroidViewModel` that exposes `notificationsEnabled`, `notificationHour`, `notificationMinute` as `StateFlow`s and provides `setNotificationEnabled()` / `setNotificationTime()` methods that persist to DataStore and update WorkManager atomically.
- **`BootReceiver` manifest registration** (`AndroidManifest.xml`): Registered under `RECEIVE_BOOT_COMPLETED` intent filter (permission was already declared).

### Changed
- **`SettingsScreen.kt`**: Replaced the previous "Notification Settings" item (which only opened the system settings page) with a full in-app notification section:
  - A `Switch` card to enable/disable the daily reminder (persists and schedules/cancels via `SettingsViewModel`).
  - An `AnimatedVisibility`-wrapped "Reminder Time" row (only visible when enabled) that opens a Material3 `TimePicker` dialog.
  - Time is displayed in 12-hour AM/PM format.

### Fixed
- N/A

### In Progress
- Runtime `POST_NOTIFICATIONS` permission request for Android 13+ (not yet prompted in-app).
- Final UI animations and layout polishing.

---

## [1.0.0-dev] - 2026-08-10

### Added
- Complete project scaffolding with Jetpack Compose, MVVM, and Room Database.
- Apache POI integration for reading and writing `.xlsx` files.
- Storage Access Framework (SAF) integration for secure file management.
- `ExcelManager` for safe temp-file based Excel writes.
- `ActivityRepository` coordinating offline-first Room caching and Excel synchronization.
- **UI Screens**:
  - `HomeScreen`: Dashboard with daily stats and progress.
  - `ActivitiesScreen`: Paginated list of activities with CRUD operations.
  - `AddEditActivityScreen`: Form to input activities.
  - `CalendarScreen`: Productivity heatmap.
  - `DailySummaryScreen`, `WeeklyScreen`, `MonthlyProgressScreen`, `YearlyProgressScreen` for detailed analytics.
  - `SearchFilterScreen`: Debounced search with status/category filters.
  - `SettingsScreen`: File management and backup options.
- Complete `NavGraph` routing all 14 screens.
- Native Compose chart visualizations (bar charts, progress rings).
- Application theme (dark mode prioritized) and color palette.

### Changed
- Transitioned analytics calculations from basic lists to comprehensive Room DAO queries for performance.

### Fixed
- Addressed potential SAF corruption by implementing a copy-to-temp-and-overwrite strategy in `ExcelManager`.

### In Progress
- Final UI animations and layout polishing.
- Notification scheduling.
