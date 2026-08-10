# Changelog

All notable changes to this project will be documented in this file.

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
