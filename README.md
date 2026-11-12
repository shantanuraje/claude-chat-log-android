# Claude History Viewer for Android

An Android app for reading Claude Code conversation history on a phone. Claude Code stores each session as a JSONL file under `~/.claude/projects/<project>/`. This app reads a copy of that folder from the phone's shared storage, groups the sessions by project, and shows each conversation as a plain user-and-assistant dialogue.

It is a small personal tool, written in July 2025. It works for the session format Claude Code used at the time. Newer formats may need parser changes.

## What it does

- Lists sessions with their title, project, start time, message count and total token count.
- Searches sessions by title or project.
- Opens a session and shows the messages in order. Filter chips show or hide each message type, including tool calls and tool results, so the conversation text can be read on its own.
- Caches parsed sessions in a local Room database, so reopening the app doesn't re-parse every file.

## How it is built

Kotlin and Jetpack Compose (Material 3), with an MVVM structure: a JSONL parser feeds a repository backed by Room, and ViewModels expose state to the Compose screens.

```
app/src/main/java/com/claude/historyviewer/
├── data/
│   ├── parser/       JSONL parsing (ClaudeHistoryParser)
│   ├── database/     Room database, DAO and type converters
│   ├── model/        session and entry models
│   └── repository/   parser + database
└── ui/
    ├── screens/      session list and conversation detail
    ├── components/   session card
    ├── viewmodel/    list and detail ViewModels
    └── theme/
```

Minimum Android version is 7.0 (API 24). The app targets API 34.

## Setup

1. Copy the `projects` folder from `~/.claude/` on your computer to the phone, so the files end up at `/storage/emulated/0/.claude/projects/<project>/<session>.jsonl`.
2. Build and install the app:

   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

   Or open the project in Android Studio and run it.
3. On first launch, grant file access. On Android 11 and later this is the "All files access" setting, because the app reads a folder outside its own storage.
4. Tap refresh to load the sessions.

## Limitations

- The folder path is fixed at `/storage/emulated/0/.claude/projects`.
- Sessions are read from a copy. Nothing syncs from the computer automatically.
- There are no tests.

## License

MIT. See [LICENSE](LICENSE).
