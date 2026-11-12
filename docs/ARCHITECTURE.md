# Architecture notes

## Runtime model

The app is based on J2ME Loader 1.8.2. The upstream engine remains responsible for MIDP/CLDC APIs, rendering, storage and platform integration. Che779 adds a multi-session shell around it.

Each `Session` represents one running MIDlet tab. The shell stores the display, event queue, virtual keyboard, app/data paths, class loader, proxy settings and UI state on that session. `Session.current()` resolves the inherited session for game-created threads and falls back to the foreground tab for Android UI callbacks.

`MicroActivity` currently assembles the tab bar, grid view, floating controller and lifecycle transitions. It is still large; future extraction should preserve the order of attach/detach, active-session changes, event-queue shutdown and MIDlet destruction. Avoid moving lifecycle code without tests for normal mode and grid mode.

## Data separation

- Each tab owns a session-specific data directory and class loader.
- Proxy and IP redirect settings are loaded/saved using tab-specific preference keys.
- A clone changes `applicationId` and `DATA_DIR_NAME`, so Android package storage and emulator data folder remain distinct from the base app.
- Closing a tab must not delete another tab's data or automatically reopen a closed tab.

## Diagnostics

`Che779Diag` records to `che779-diag.txt`; `DiagnosticLogStore` retains the current file and one `.old` generation. Each generation is bounded to 3 MiB. Thread dumps are capped at 3 per `Session` and 20 per process.

## Thread ownership limitation

`Session.killThreads()` still identifies game threads by scanning stack traces for class names loaded by that tab. This is not a perfect ownership model: two games may load classes with identical names. Do not treat the current heuristic as a hard isolation boundary. A future redesign should track explicit thread ownership at creation time and prove it with concurrency tests before replacing the heuristic.

## Build variants

- `devDebug`: test build used by CI and testers; signed with the repository test key to preserve update continuity.
- `playDebug`, `openDebug`, `fdroidDebug`: upstream product flavors.
- `release`: unsigned unless a private keystore is supplied via the documented `CHE779_RELEASE_*` environment variables.
- `midlet`: special Android port flavor whose package/version are derived from the MIDlet manifest.

Clone parameters are validated in Gradle even when a build is started outside GitHub Actions. The clone workflow validates them again before passing them to Gradle.
