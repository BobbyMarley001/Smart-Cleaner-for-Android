# Feature Module Template

This template helps scaffold a new cleaning feature. Replace `example` with your feature name.

## Directory Structure

```
app/
 └── clean/
     └── example/
         ├── domain/
         │   └── ExampleUseCase.kt
         ├── handler/
         │   └── ExampleHandler.kt
         └── ui/
             ├── ExampleScreen.kt
             └── ExampleViewModel.kt
```

## Sample Activity

```kotlin
class ExampleCleanerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExampleScreen(viewModel = hiltViewModel())
        }
    }
}
```

## Next Steps
- Add strings in `strings.xml` and provide translations.
- Write a doc page for the feature and link it from `docs/README.md`.
- Register any WorkManager jobs or notifications as needed.
