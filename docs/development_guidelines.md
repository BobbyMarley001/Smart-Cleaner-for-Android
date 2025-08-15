# Development Guidelines

## Adding a New Cleaning Feature
1. **UI and ViewModel** – place under `app/clean/<feature>` using the existing `UiStateScreen` + `ScreenViewModel` pattern.
2. **Handlers** – heavy work should live in a dedicated handler similar to `CleanOperationHandler` and run inside a `DispatcherProvider` scope.
3. **Persistence** – allocate a new WorkRequest ID key in `DataStore` if the feature runs a cleanup job.
4. **Notifications** – reuse `FileCleanupWorker` when possible. Ensure all strings are localized.
5. **Docs** – update `docs/README.md` and create a feature page following the examples in this directory.

## ViewModel & UiState Placement
- ViewModel in `app/clean/<feature>/ui`.
- Domain models and use cases in `app/clean/<feature>/domain`.
- Persistent preferences in `core/data/datastore/DataStore.kt`.

## Localization
- Add strings to `app/src/main/res/values/strings.xml` and provide translations under the appropriate `values-<lang>` directories.
- Use `UiTextHelper` to display messages so testing and localization remain consistent.

## File Preview Integration
- Extend `PreviewType` and update `getPreviewType` in `FilePreviewHelper` to support new file formats.
- Respect memory limits: previews should decode on `Dispatchers.IO` and rely on the shared `LruCache`.

## Feature Module Template
- Refer to the [Feature Module Template](feature_module_template.md) for scaffolding new cleanup features.
