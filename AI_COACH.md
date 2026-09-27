# BuJo Coach — first release

## Setup

Open **Coach** from Today, Plan Day, Settings, or the sidebar. Open **Coach settings**:

1. Paste your own Gemini API key and enter a model ID that supports structured JSON output.
2. Choose **Save and test connection** in the BB10 settings modal. This is a real, potentially billable API request, but sends no journal data.
3. Enable online sharing if desired. Task text and habit details are separate opt-ins, both off initially.
4. Optionally enable reminders and choose a morning/evening hour.
5. Select your mood, energy, progress, available minutes and optional note. Choose **Get guidance**, or **Save offline check-in** to send nothing.

Do not send your key in chat or commit it to source control.

## Included

- Morning planning, progress check-ins and evening reflection.
- Gemini REST over OkHttp 3.12.13 and Conscrypt 2.5.2, matching the reference BlackBerry app.
- Complete structured responses, bounded response size, timeouts and validation of referenced task IDs.
- Opt-in context: up to 30 of today's tasks and 20 current habits. Task content is truncated to 300 characters and habit names to 200.
- Local check-in persistence before the network request; offline guidance on failure.
- Explicit retry, connection diagnostics, private settings and history deletion.
- A separate, user-approved focus plan. Applying it never edits journal tasks, deadlines, habits or points. Undo restores the preceding accepted plan for that day.
- Inexact local reminders at the morning hour, four-hour slots and the evening hour. No background Gemini polling.
- Snooze, waking-hour limits, duplicate suppression, active-focus deferral, and rescheduling after reboot/time changes.
- Notification taps open Coach. Missed alarms older than two hours are not replayed.

## Deliberate limits

This is a guidance-first release, not an autonomous journal editor. It does not move deadlines, schedule calendar blocks, create or complete tasks, or change habit commitments. It does not yet include voice, weekly trend analysis, full chat, configurable tone, long-term AI memory, per-project sharing filters or coach-history export. Existing journal exports intentionally exclude coach history.

Reminders are approximate: power saving, device shutdown, notification permissions, force-stop and the BlackBerry runtime may affect delivery. Force-stopped apps must be opened again. The start hour must precede the evening hour within one calendar day; overnight waking windows are not supported yet.

The reminder asks for a check-in locally. Gemini runs only when you request online guidance or a connection test. There is no automated API retry or recurring API charge.

## Privacy

The key is kept in a dedicated app-private SharedPreferences file, never the repository, URL, logs, or database. It is not encrypted by this feature. Private storage is not strong protection on a rooted/compromised legacy device.

Check-ins and guidance use a separate private SQLite database. Key/settings and coach database files are excluded from Android cloud backup and device transfer rules; journal backup exports only the existing journal database. Local coach history remains until explicitly deleted or app data is cleared. Data sent to Google is subject to the selected API service's policies; deleting local history does not retract API requests.

Sharing controls apply to the next request. Retries create a new attempt using the saved check-in with fresh, currently permitted task/habit context. Historical snapshots are never sent automatically.

## Engineering / verification

- The app's minimum SDK remains 18. No Google Play Services, modern AI SDK, always-running service or backend is required.
- The engine owns background work outside the fragment. Destroyed views ignore results; interrupted requests are marked on process restart.
- Model responses are plain text plus a validated plan schema. No SQL, HTML or model-generated tool execution is accepted.
- Only one coach operation runs at a time. HTTP redirects are disabled; credentials are sent only to the fixed Google endpoint.
- Database tests use an isolated temporary database. Protocol tests use synthetic responses and never call Gemini.
- Run: `./gradlew --offline testDebugUnitTest connectedDebugAndroidTest lintDebug assembleDebug`.
- Before relying on the release, use **Save and test connection** on the actual BlackBerry and verify notification delivery across sleep and reboot. Emulator tests cannot certify the BlackBerry runtime or a live API model/key.
