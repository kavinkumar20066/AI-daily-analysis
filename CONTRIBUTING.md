# Contributing to Daily Work Tracker

Thank you for your interest in contributing! This document outlines the process for proposing changes and submitting pull requests.

## Developer Workflow

1. **Fork and Clone**
   Fork the repository to your own GitHub account and clone it to your local machine:
   ```bash
   git clone https://github.com/YOUR_USERNAME/AI-daily-analysis.git
   cd AI-daily-analysis
   ```

2. **Open in Android Studio**
   Open the project in Android Studio (Hedgehog 2023.1.1 or newer). Gradle sync should begin automatically.

3. **Create a Branch**
   Create a new branch for your feature or bugfix:
   ```bash
   git checkout -b feature/my-new-feature
   ```

4. **Make Changes**
   Implement your feature or fix the bug. Make sure your code follows the existing architecture (MVVM, Room, POI).

5. **Test Your Changes**
   Build and run the project on an emulator or physical device. Ensure that your changes do not break existing functionality, particularly the Excel synchronization.
   
   To run a clean build from the command line:
   ```powershell
   .\gradlew.bat clean
   .\gradlew.bat assembleDebug
   ```

6. **Commit**
   Commit your changes with a clear and descriptive commit message:
   ```bash
   git add .
   git commit -m "feat: added new analytics chart"
   ```

7. **Push and Create a Pull Request**
   Push your branch to your fork:
   ```bash
   git push origin feature/my-new-feature
   ```
   Then, open a Pull Request against the `main` branch of this repository.

## Guidelines

- **Architecture:** Keep the UI layer separate from the data layer. Data should flow through ViewModels and the `ActivityRepository`.
- **Dependencies:** Do not add new heavy dependencies unless absolutely necessary.
- **Documentation:** If you add a new feature, update the `README.md` and `PROJECT_STATUS.md` accordingly.

Thank you for contributing!
