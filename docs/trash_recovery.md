# Trash Recovery

The Trash screen lets users restore or permanently delete files that were previously moved to the app's trash.

## Trigger
- User opens the Trash screen from the dashboard or notification.

## UI States
1. **Loading** – retrieves trash entries from disk.
2. **Ready** – lists trashed files with selection checkboxes.
3. **Empty** – `ScreenState.NoData` when the trash is empty.
4. **Error** – displays a snackbar on failure.

## State Persistence
- Trash file paths and aggregate size are stored in DataStore for quick loading【F:app/src/main/kotlin/com/d4rk/cleaner/app/clean/trash/ui/TrashViewModel.kt†L41-L55】.
- Individual selection states live only in memory.

## Operations
- **Restore:** selected files restored to their original locations and trash size reduced【F:app/src/main/kotlin/com/d4rk/cleaner/app/clean/trash/ui/TrashViewModel.kt†L126-L174】.
- **Delete Permanently:** selected files deleted and size updated【F:app/src/main/kotlin/com/d4rk/cleaner/app/clean/trash/ui/TrashViewModel.kt†L178-L199】.

## Error Handling
- When restore or delete fails, a snackbar is added to the UI state's `errors` list for user feedback.

## System Events
- The ViewModel observes DataStore flows to update size and file list, so UI stays consistent after process death.
- No WorkManager jobs are used; operations run directly on a background dispatcher.

## TODO
- Add a confirmation dialog before permanently deleting large selections.
