# New Cleaner Job Lifecycle Checklist

## Start
- [ ] Triggered only by explicit user action
- [ ] Enqueue via `FileCleaner` and persist the per-feature WorkManager ID to DataStore immediately
- [ ] Block additional jobs while a job ID exists and notify the user

## Progress
- [ ] Display a determinate foreground notification showing processed/total files
- [ ] Observe progress with `observeFileCleanWork` and update UI from `WorkInfo`

## Completion
- [ ] Show a final notification summarizing success, failure, or cancellation
- [ ] Report partial failures using `KEY_FAILED_PATHS` output
- [ ] Clear the persisted job ID and reset UI state

## Process Death & Recovery
- [ ] On restart, reattach to the running job via the stored ID
- [ ] Remove stale IDs if no matching `WorkInfo` is found
