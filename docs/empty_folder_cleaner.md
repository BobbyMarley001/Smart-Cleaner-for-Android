# Empty Folder Cleaner

Empty folder detection runs as part of the Analyze screen rather than a separate UI.

## Trigger
- Activated when the user starts a full device scan. `CleanOperationHandler` collects empty directories through `GetEmptyFoldersUseCase` during analysis【F:app/src/main/kotlin/com/d4rk/cleaner/app/clean/scanner/ui/CleanOperationHandler.kt†L100-L105】.

## UI States
1. **Analyzing** – empty folders are discovered alongside files.
2. **ReadyToClean** – `UiScannerModel.analyzeState.emptyFolderList` lists folders that can be removed【F:app/src/main/kotlin/com/d4rk/cleaner/app/clean/scanner/ui/CleanOperationHandler.kt†L140-L146】.
3. **Cleaning** – when selected, folders are deleted by the same WorkManager job as other files.

## State Persistence
- The list of empty folders exists only in memory inside `UiScannerModel`.
- Whether empty folders are included in scans is controlled by a DataStore flag (`deleteEmptyFolders`)【F:app/src/main/kotlin/com/d4rk/cleaner/core/data/datastore/DataStore.kt†L178-L188】.

## Error Handling
- Failures surface as snackbars during analysis or deletion; no special recovery is implemented.

## System Events
- If the process dies after analysis, the list is lost and requires a rescan.
- During an active cleanup job, the job ID persists for recovery like other scans.

## TODO
- Add a dedicated screen if deeper preview or per-folder selection is required.
