# Clipboard Cleaner

## Triggers
- **Manual:** user launches `ClipboardCleanerActivity`, which wipes the clipboard and shows a toast before finishing【F:app/src/main/kotlin/com/d4rk/cleaner/app/clean/clipboard/ui/ClipboardCleanerActivity.kt†L10-L20】.
- **Automatic:** when notification listener permission is granted, `ClipboardNotificationListenerService` clears the clipboard whenever the screen turns off【F:app/src/main/kotlin/com/d4rk/cleaner/app/clean/clipboard/services/ClipboardNotificationListenerService.kt†L15-L36】.

## UI States
- No dedicated UI. Manual trigger shows a short toast confirming the clipboard was cleared.

## State Persistence
- Clipboard contents live only in memory; nothing is persisted.

## Error Handling
- If the clipboard service is unavailable, an error toast notifies the user before closing.

## System Events
- Screen-off events trigger automatic cleaning when the service is active.
- Process death has no effect beyond stopping automatic clearing until the service is restarted.
