# Daily Work Tracker

## Project Overview
Daily Work Tracker is a personal daily activity and productivity tracking application for Android. It allows users to log their daily tasks, categorize them, track time, and visualize their productivity through extensive analytics. 

Unlike typical to-do list apps, this project serves as a life/work analytics tool, where the user's Excel file (`Daily Work.xlsx`) acts as the permanent source of truth for their data. The app connects to this file, ensuring users always retain full ownership of their data.

## Key Features

### 🟢 Completed
- **Project Setup & Architecture**: Full Android project scaffold with MVVM and Room offline-first cache.
- **Excel Engine Integration**: Full Apache POI implementation reading/writing to an external `.xlsx` file using the Android Storage Access Framework (SAF) and a temp-file safe write strategy.
- **CRUD Operations**: Complete Add, Edit, Delete flows for activities with auto-calculated duration and rich form inputs.
- **Dashboard**: Home screen with reactive daily progress, category breakdowns, and quick stats.
- **Calendar & History**: Monthly productivity heatmap and date-scoped activity lists.
- **Detailed Analytics**: 
  - Daily Summaries (highlights, areas to improve).
  - Weekly View (7-day completion bars, productive hours).
  - Monthly Progress (category distribution, exercise tracking).
  - Yearly Overview (best month trophy, monthly trends).
- **Search & Filter**: Debounced search and multi-filtering (status, category, exercise).
- **Settings & Data Management**: Backup/export Excel data, manual sync controls.

### 🟡 In Progress
- **UI/UX Polish**: Micro-animations and layout refinements.

### ⚪ Planned
- **Splash Screen**: Animated launch screen.
- **Notification Scheduler**: Daily reminders to log activities.
- **Performance Optimization**: Lazy loading improvements and database indexing tweaks.

## Technology Stack
- **Frontend / UI**: Jetpack Compose (Material 3)
- **Language**: Kotlin
- **Architecture**: MVVM + Repository Pattern
- **Local Cache**: Room Database
- **Async Operations**: Kotlin Coroutines + StateFlow
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
 ├── ui/
 │    ├── components/    # Reusable UI (ActivityCard, Dialogs)
 │    ├── navigation/    # NavGraph, Screen definitions
 │    ├── screens/       # Features (Activities, Calendar, Home, Progress, Search, Settings)
 │    ├── theme/         # Color, Type, Theme settings
 │    └── viewmodel/     # Shared ViewModels (e.g., ExcelViewModel)
 ├── DailyWorkTrackerApp.kt
 ├── AppContainer.kt
 └── MainActivity.kt
```

## Installation and Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/kavinkumar20066/AI-daily-analysis.git
   ```
2. Open the project in Android Studio.
3. Allow Gradle to sync and download dependencies (including Apache POI).
4. Run the application on an Android emulator or a physical device.

## Usage
1. **Connect Data**: On the Home screen, tap the banner to upload/create your `Daily Work.xlsx` file.
2. **Log Activities**: Use the `+` button to add tasks. Fill in times, categories, and exercise details.
3. **Track Progress**: Navigate to the Progress tab to view Weekly, Monthly, and Yearly insights.
4. **Backup**: Go to Settings to manually back up or share your Excel file.

## Future Enhancements
- Data import/export from other formats (CSV, JSON).
- Custom goal setting (e.g., "Exercise 3x a week").
- Advanced charting using a dedicated library like Vico (currently using native Compose canvas drawing).

## License
MIT License
