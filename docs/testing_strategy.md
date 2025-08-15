# Testing Strategy

## Manual Tests
- Run each cleaner (Analyze, WhatsApp, Large Files, Trash) on small and large data sets.
- Cancel a cleanup mid-way and confirm notifications show "cancelled" and job IDs clear.
- Reboot or force-stop the app during a job to verify recovery and continued notifications.
- Test error paths by locking files or removing permissions.
- Validate UI on slow or nearly-full storage by creating large dummy files.

## Automated Tests
- Unit-test `CleanOperationHandler` and `FilePreviewHelper` where possible.
- Instrumented tests should verify WorkManager job recovery and DataStore ID clearing.

## Job Recovery Scenarios
1. Start a cleanup, kill the process, relaunch and ensure progress resumes.
2. Remove the notification while the job runs; reopen the app and confirm state synchronizes.

## User Communication
- Each job must post a final notification, so automated tests should assert the message appears and is dismissed after ~4 s.

## Tips
- Use emulators with throttled IO to simulate slow file systems.
- Corrupt media or archives can test fallback paths in `FilePreviewHelper`.
