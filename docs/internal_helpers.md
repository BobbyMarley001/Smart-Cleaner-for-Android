# Internal Helpers

## CleanOperationHandler
- Orchestrates scanning and cleaning actions so the ViewModel stays lean.
- Collects files and empty folders, then builds grouped and duplicate lists before updating `UiScannerModel`【F:app/src/main/kotlin/com/d4rk/cleaner/app/clean/scanner/ui/CleanOperationHandler.kt†L58-L155】.
- Before enqueuing a cleanup job it checks `DataStore` for an existing job ID, clearing it if the previous work finished【F:app/src/main/kotlin/com/d4rk/cleaner/app/clean/scanner/ui/CleanOperationHandler.kt†L184-L213】.
- Contains several `FIXME` comments for unused dependencies and a `resultDelayMs` constant that can be removed or implemented.

## UiScannerModel
- Holds transient scan data: file lists, empty folders, duplicate groups and selections【F:app/src/main/kotlin/com/d4rk/cleaner/app/clean/scanner/domain/data/model/ui/UiScannerModel.kt†L8-L32】.
- All fields are in-memory; no persistence beyond the current process.

## JobIdStore (DataStore)
- `scannerCleanWorkId` and `largeFilesCleanWorkId` store WorkRequest IDs so the app can recover from process death【F:app/src/main/kotlin/com/d4rk/cleaner/core/data/datastore/DataStore.kt†L88-L121】.
- Exposes helpers to save and clear each ID. Stale IDs are removed when no matching work exists.

## Notification Handling
- `FileCleanupWorker` displays a foreground notification with determinate progress and localized messages.
- After completion the notification remains for `FINISH_DELAY_MS` (4 s) before being cancelled, giving users time to read the result【F:app/src/main/kotlin/com/d4rk/cleaner/app/clean/scanner/work/FileCleanupWorker.kt†L147-L153】.

## TODO
- Implement or remove the unused parameters in `CleanOperationHandler`.
- Consider consolidating duplicated job-ID logic into a shared helper.
