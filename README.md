# Daily Work Tracker

## Project Overview
Daily Work Tracker is a personal daily activity and productivity tracking application for Android. It allows users to log their daily tasks, categorize them, track time, and visualize their productivity through extensive analytics.

Unlike typical to-do list apps, this project serves as a life/work analytics tool, where the user's Excel file (`Daily Work.xlsx`) acts as the permanent source of truth for their data. The app connects to this file, ensuring users always retain full ownership of their data.

## Key Features

### 🟢 Completed
- **Project Setup & Architecture**: Full Android project scaffold with MVVM and Room offline-first cache.
- **Excel Engine Integration**: Full Apache POI implementation reading/writing to an external `.xlsx` file using the Android Storage Access Framework (SAF) and a temp-file safe write strategy.
- **CRUD Operations**: Complete Add, Edit, Delete flows for activities with auto-calculated duration and rich form inputs.
- **Dashboard**: Home screen with reactive daily progress, category breakdowns, quick stats, and animated shimmer loading state.
- **Calendar & History**: Monthly productivity heatmap and date-scoped activity lists.
- **Detailed Analytics**:
  - Daily Summaries (highlights, areas to improve).
  - Weekly View (7-day completion bars, productive hours).
  - Monthly Progress (category distribution, exercise tracking).
  - Yearly Overview (best month trophy, monthly trends).
- **Search & Filter**: Debounced search and multi-filtering (status, category, exercise).
- **Settings & Data Management**: Backup/export Excel data, manual sync controls.
- **Notification Scheduler**: WorkManager-based daily reminders with an in-app toggle and Material3 time picker.
- **Runtime Permission Flow**: In-app rationale dialog + system `POST_NOTIFICATIONS` prompt for Android 13+, with graceful denial handling.
- **UI/UX Animations**:
  - Screen transitions: fade crossfade for tab switches; horizontal slide for detail/sub-screens.
  - `ActivityCard` entrance: `AnimatedVisibility` fade-in + spring slide-up on each list item.
  - `HomeScreen` shimmer: animated sweep-gradient skeleton placeholders while Excel data loads.
- **Runnable Build**: `gradlew` + `gradlew.bat` wrapper scripts and `gradle-wrapper.jar` in place — project syncs and builds without errors in Android Studio.

### ⚪ Low Priority / Future
- **Advanced Charts**: Wire Vico library (already a dependency) into analytics screens for richer interactive charts.
- **Performance**: Lazy loading improvements and database indexing tweaks.

## Technology Stack
- **Frontend / UI**: Jetpack Compose (Material 3)
- **Language**: Kotlin
- **Architecture**: MVVM + Repository Pattern
- **Local Cache**: Room Database
- **Async Operations**: Kotlin Coroutines + StateFlow
- **Background Scheduling**: WorkManager (`PeriodicWorkRequest`)
- **Data Source / File Engine**: Apache POI (Excel `.xlsx` processing) + Storage Access Framework (SAF)
- **Dependency Management**: Manual DI (AppContainer)
- **Preferences**: DataStore

## Project Architecture
The application uses an offline-first MVVM architecture:
- **UI Layer**: Jetpack Compose screens and ViewModels.
- **Data Layer**:
  - `ActivityRepository`: Coordinates between Room and Excel.
  - `ExcelManager`: Handles low-level file I/O with Apache POI.
  - `ActivityDao`: Room database interface for fast querying and caching.
- **Data Flow**: Read from Room (fast UI). Writes go to Room first, then sync to Excel.

## Project Structure
```text
app/src/main/java/com/dailyworktracker/
 ├── data/
 │    ├── db/            # Room Database, DAO, Converters
 │    ├── excel/         # ExcelManager (Apache POI logic)
 │    ├── model/         # DailyActivity entity, Enums
 │    ├── preferences/   # DataStore preferences
 │    └── repository/    # ActivityRepository
 ├── notification/       # DailyReminderWorker, NotificationScheduler, BootReceiver
 ├── ui/
 │    ├── components/    # Reusable UI (ActivityCard with animation, Dialogs)
 │    ├── navigation/    # NavGraph with full screen transitions (14 screens)
 │    ├── screens/       # Features (Activities, Calendar, Home, Progress, Search, Settings)
 │    ├── theme/         # Color, Type, Theme settings
 │    └── viewmodel/     # Shared ViewModels (ExcelViewModel)
 ├── DailyWorkTrackerApp.kt
 ├── AppContainer.kt
 └── MainActivity.kt
```

## Installation and Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/kavinkumar20066/AI-daily-analysis.git
   ```
2. Open the project folder in **Android Studio** (Hedgehog 2023.1.1 or newer).
3. Android Studio will detect `gradlew.bat` and prompt "Gradle project sync" — click **Sync Now**.
4. Wait for Gradle to download dependencies (Apache POI is large — first sync may take 2–5 minutes).
5. Run on an emulator (API 26+) or physical device via **Run ▶**.

> **CLI build** (optional): After sync, you can also build from PowerShell:
> ```powershell
> .\gradlew.bat assembleDebug
> ```

## Usage
1. **Connect Data**: On the Home screen, tap the banner to upload your `Daily Work.xlsx` file.
2. **Log Activities**: Use the `+` FAB to add tasks. Fill in times, categories, and exercise details.
3. **Track Progress**: Navigate to the Progress tab to view Weekly, Monthly, and Yearly insights.
4. **Enable Reminders**: Go to Settings → toggle Daily Notification on and pick a time.
5. **Backup**: Go to Settings to manually back up or share your Excel file.

## Future Enhancements
- Data import/export from other formats (CSV, JSON).
- Custom goal setting (e.g., "Exercise 3x a week").
- Advanced charting using Vico (library already declared as a dependency).

## License
MIT License
