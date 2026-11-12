# Che779 Emulator Android — AI Handoff

## Project identity

- Source archive: `Che779-Emulator-main.zip`
- Android application: `com.che779.emulator`
- Base engine: J2ME Loader 1.8.2
- Current implementation baseline: `0.0.0.2-android`, `versionCode 103`
- Main custom layer: `javax.microedition.shell.*`

## Product decisions to preserve

- Do not impose a hard limit on the number of tabs.
- Do not add macros unless a specific game requires one.
- Defer x6–x12 multi-frame composition until there is a concrete paid use case.
- Do not automatically reopen a tab after the user closes it.
- Keep per-tab IP redirect.
- Do not add a Contact menu to the app.
- Preserve independent package/data directory behavior for clones.

## This implementation pass

1. Harden clone workflow input handling and Gradle property validation.
2. Keep the repository test keystore for `devDebug` update continuity, but never apply it to `release`.
3. Add optional private release signing through four `CHE779_RELEASE_*` environment variables.
4. Rotate diagnostic logs across current + `.old`, bound entries to 3 MiB, and add per-tab/process thread dump caps.
5. Add unit tests and developer documentation.
6. Normalize version naming to `0.0.0.2-android`; increment `versionCode` to 103.

## Known risks / next engineering tasks

- `MicroActivity.java` is still a large Activity (~2,100 lines). Refactor only with lifecycle tests; candidate controllers: tab lifecycle, floating/bubble coordination, power mode.
- `Session.killThreads()` currently interrupts threads by matching class names in stack traces. This is heuristic and can interrupt a different session if games share class names. Replace only after designing explicit per-session thread ownership and tests.
- Overlay permission behavior varies by Android OEM. Current UI prompts for permission when the user explicitly taps the bubble action; test MIUI/ColorOS/Samsung/Pixel behavior.
- Test `Session` isolation for Display/EventQueue/ClassLoader/RMS/FileConnection/network across 2+ simultaneous games.
- Test lifecycle under rotate/background/foreground/process recreation and close-tab races.
- Test proxy HTTP/SOCKS5 and IP redirect per tab.
- Audit dependency licenses and native libraries before public distribution.
- Keep `LICENSE` and `NOTICE` intact; verify upstream attribution and modified-file notices.

## Verification status for this pass

- Source-level checks and local JVM unit tests for `DiagnosticLogStore`: expected to be run by the local validation script/CI.
- Android APK build: not run in this environment without Android SDK/NDK.
- GitHub Actions: not executed from this workspace.
- Android device/OEM/game compatibility: not tested.
