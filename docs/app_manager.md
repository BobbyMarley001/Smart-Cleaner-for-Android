# App Manager

The App Manager lists installed applications and APK files and supports sharing or uninstalling them.

## Trigger
- User navigates to App Manager from the dashboard.

## UI States
1. **Loading** – installed apps and APK files are fetched concurrently.
2. **Ready** – apps and APKs displayed with search and action menus.
3. **Error** – failures add a snackbar while retaining last known data.

## State Persistence
- Lists are reloaded on each visit; no persistence of selections.
- Broadcast receivers update the list when packages are added or removed at runtime【F:app/src/main/kotlin/com/d4rk/cleaner/app/apps/manager/ui/AppManagerViewModel.kt†L68-L112】.

## Operations
- **Uninstall App:** enqueues `UninstallAppUseCase` and waits for `ACTION_PACKAGE_REMOVED` to confirm success via snackbar【F:app/src/main/kotlin/com/d4rk/cleaner/app/apps/manager/ui/AppManagerViewModel.kt†L68-L89】.
- **Install APK:** listens for `ACTION_PACKAGE_ADDED` after invoking `InstallApkUseCase` to show completion【F:app/src/main/kotlin/com/d4rk/cleaner/app/apps/manager/ui/AppManagerViewModel.kt†L95-L112】.
- **Share/Info:** direct intents handled through respective use cases.

## Error Handling
- Use cases report errors via `UiSnackbar` messages; list loading continues with partial data when possible.

## System Events
- Package add/remove broadcasts keep the UI in sync while the ViewModel is active.
- On process death, the ViewModel reloads all data and re-registers receivers in `init`【F:app/src/main/kotlin/com/d4rk/cleaner/app/apps/manager/ui/AppManagerViewModel.kt†L114-L123】.

## TODO
- Persist search queries across configuration changes.
