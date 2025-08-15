# Cleaning Features Overview

Smart Cleaner provides multiple cleaning modules built on a shared cleanup architecture. All cleaners avoid the Storage Access Framework (SAF) and any foreground service types, running WorkManager jobs for a streamlined file-deletion flow.

All cleanup jobs enqueue a `FileCleanupWorker` via the centralized [FileCleanWorkEnqueuer](../app/src/main/kotlin/com/d4rk/cleaner/core/work/FileCleanWorkEnqueuer.kt). Work request IDs for active jobs are stored in [DataStore](../app/src/main/kotlin/com/d4rk/cleaner/core/data/datastore/DataStore.kt) so each feature tracks its own progress.

## WhatsApp Cleaner
- Summaries and cleans WhatsApp media.
- ViewModel: [WhatsappCleanerSummaryViewModel](../app/src/main/kotlin/com/d4rk/cleaner/app/clean/whatsapp/summary/ui/WhatsappCleanerSummaryViewModel.kt)
- Use cases: [GetWhatsAppMediaSummaryUseCase](../app/src/main/kotlin/com/d4rk/cleaner/app/clean/whatsapp/summary/domain/usecases/GetWhatsAppMediaSummaryUseCase.kt), [GetWhatsAppMediaFilesUseCase](../app/src/main/kotlin/com/d4rk/cleaner/app/clean/whatsapp/summary/domain/usecases/GetWhatsAppMediaFilesUseCase.kt)
- Work ID key: `whatsappCleanWorkId`

## Large Files Cleaner
- Finds and deletes oversized files across storage.
- ViewModel: [LargeFilesViewModel](../app/src/main/kotlin/com/d4rk/cleaner/app/clean/largefiles/ui/LargeFilesViewModel.kt)
- Use case: [GetLargestFilesUseCase](../app/src/main/kotlin/com/d4rk/cleaner/app/clean/scanner/domain/usecases/GetLargestFilesUseCase.kt)
- Work ID key: `largeFilesCleanWorkId`

## Trash
- Handles user trash operations and recovery.
- ViewModel: [TrashViewModel](../app/src/main/kotlin/com/d4rk/cleaner/app/clean/trash/ui/TrashViewModel.kt)
- Use cases: [GetTrashFilesUseCase](../app/src/main/kotlin/com/d4rk/cleaner/app/clean/trash/domain/usecases/GetTrashFilesUseCase.kt), [RestoreFromTrashUseCase](../app/src/main/kotlin/com/d4rk/cleaner/app/clean/trash/domain/usecases/RestoreFromTrashUseCase.kt)
- Work ID key: `trashCleanWorkId`

## Dashboard
- Entry point aggregating storage analysis and quick clean actions.
- ViewModel: [ScannerViewModel](../app/src/main/kotlin/com/d4rk/cleaner/app/clean/scanner/ui/ScannerViewModel.kt)
- Use case: [AnalyzeFilesUseCase](../app/src/main/kotlin/com/d4rk/cleaner/app/clean/scanner/domain/usecases/AnalyzeFilesUseCase.kt)
- Work ID key: `scannerCleanWorkId`

For details on the job lifecycle and WorkManager integration, see [Cleanup Job System](cleanup_jobs.md).
