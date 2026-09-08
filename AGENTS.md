# Agent Guidelines & Rules

## Automatic Git Commits & Remote Sync

Upon successfully completing a user task, feature, or bug fix:
- Automatically stage all relevant changes (`git add`).
- Create a Git commit with a concise, descriptive commit message explaining the change (e.g., `feat: ...`, `fix: ...`, `docs: ...`).
- Push commits to the remote GitHub repository (`git push origin <branch>`) to keep the remote synced.
- Verify that the commit and push succeeded before concluding the response.

## Automatic Device Installation

Upon completing a UI feature, fix, or user task:
- Check if an Android device or emulator is connected (`adb devices`).
- If an active device is attached, automatically install the updated debug build (`./gradlew installDebug`) so the user can immediately test on device.
