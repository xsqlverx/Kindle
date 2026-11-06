# Assistant and personal capabilities

## Google AI Studio debug build report (2026-11-06)

The user supplied a build log in which `:app:kspDebugKotlin` first failed with
`Unexpected owner function: null`. A subsequent automatic build, after AI Studio reported
`app/build.gradle.kts` changed, completed `:app:assembleDebug` and reported `BUILD SUCCESSFUL`.
The exact Studio source revision and the intervening change are not confirmed. The reported
`AWT-EventQueue-0` exception appeared in both builds; GitHub Actions run `42101043548` also
showed that exception while its debug KSP/test task succeeded. It matches the non-fatal KSP
2.3.5 issue https://github.com/google/ksp/issues/2763; it does not explain the separate
`Unexpected owner function` failure by itself. No root cause for that intermittent failure
is established. Do not change processors or database code solely from this excerpt.

The subsequent Studio log shows an emulator process, not installation on the user's phone.
An APK from the successful Studio build is needed to compare its package and certificate to
the original phone app before a data-preserving update. Keep both existing apps and their
separate data untouched; a successful build alone does not prove signing compatibility.

2026-11-06. User initially requested a private AI APK, then explicitly changed that decision:
"just build and push to main" and "fck privacy". Public origin is confirmed public; this
version and its source go to main by that later instruction. Preserve existing builder and data.

## Stages

1. Build a native assistant accessible from the app. Connect Groq, OpenRouter free router,
   NVIDIA and Cerebras with user-entered keys, explicit per-provider network/data disclosure,
   honest retry/fallback, useful errors, secure on-device credential storage and no hardcoded
   keys or paid OpenRouter models. No Gemini. Current app/student identity and relevant live
   app records accompany each opted-in conversation without repeated setup. The model cannot
   silently mutate real records or claim unavailable data.
2. Add four distinct practical capabilities, integrated with real app data and the existing
   focus/planning flow. Keep surprise details in the private implementation/handoff only.
   New suggestions remain editable; no inferred attendance, progress, marks or commitments.
3. Keep all existing user data, 13 destinations, additive migrations and working flows. Store
   private assistant settings, conversation and user-owned memory locally; provide deletion.
4. Source/static checks then GitHub Actions build. Existing app signer is unavailable; an
   independently signed `.assistant` preview can coexist with the original. Never uninstall,
   clear data or use a mismatched APK to replace the original. Copy existing records only if
   a safe, verified local transfer is possible. No local Android compilation or Gradle.

## Evidence and blockers

The public repository is public; its branches are public. Phone SM-E156B is connected, and
Kindle is already installed. Installed signer does not match local Android debug key. Its
debuggable flag permits `run-as`; a copy-based transfer into a side-by-side preview may be
possible after force-stopping both packages and validating the snapshot. Copies do not sync.
Automatic approval review rejected uploading a dedicated signing key to the public repository's
Actions secrets. The workflow generates an ephemeral preview signer instead. Subsequent APKs
cannot update that preview without another safe migration. No existing builder key changed.
At that stage the cloud build, API keys, provider requests and device behavior were unverified;
the later evidence below supersedes that status.

## Implemented source

Assistant is a 14th assignable destination and is also reachable from Today. It automatically
provides its native Android/student identity, a bounded live snapshot of actual app data, relevant
syllabus excerpts and user-authored memory; it never pretends missing records are known. Groq,
Cerebras, OpenRouter's free router and NVIDIA have configured endpoints, selected-provider and
configured-key fallback. Keys, personal memory and up to 30 conversation messages are encrypted
with Android Keystore; excluded from Android cloud/device backup. User supplies provider keys
inside the app. No Gemini API. Fallback and context sharing are disclosed and configurable.
Provider requests are only made after an explicit user action; they may have provider charges
if the account has paid billing. The app does not autonomously edit records.

Four capabilities: a contextual daily briefing; interactive exam rehearsal from an actual
syllabus source; a coding clinic using a selected saved problem; a collision-aware rescue
window that hands a real saved action to the existing editable composer. Assistant replies
can also become editable next moves. No suggestions silently mark anything complete.

GitHub Actions adds only a separate `assistantPreview` build type and generates a temporary
debug keystore; the external builder's existing debug/release configuration is unchanged.
No AI keys, chat, personal app records, keystores or generated artifacts are committed.
AssistantRoutingTest supplies three cloud unit tests; none has run locally.

First Actions run `42092469356` failed before compilation: setup-android v3 requested the
obsolete SDK package `tools`. Workflow now requests `platform-tools` explicitly; a new cloud
run is needed. This is not an Android source/build pass.

Actions run `42092689409` successfully compiled `assembleAssistantPreview`, then failed because
the specified preview unit-test task did not exist; artifact upload was skipped. The workflow
now uploads the compiled APK first and attempts the debug unit test only if that task exists.
Actions run `42093811467` compiled the preview, uploaded the APK and executed the three
`AssistantRoutingTest` unit tests successfully. The labeled variant was built and its APK
uploaded successfully in run `42094542167`; its debug routing test task was FROM-CACHE.
The verified APK package `com.aistudio.lifeos.vkmqzt.assistant` is installed separately on
SM-E156B as `Assistant Preview`. No local Gradle or Android tests were run.

The original version-5 database was snapshotted without stopping the original app, passed
SQLite quick_check and had stable file hashes. An initial transfer into the fresh preview
failed partway; the original app was never modified. The preview subsequently started and
the user confirmed entering something there. Both databases passed SQLite quick_check at
that stage, and the copy was paused pending clarification. The later completed-copy evidence
below supersedes that paused status. Neither compilation nor installation proves live AI calls.

### One-time data copy completed

The user clarified that they entered only an AI provider key or assistant settings in the
preview, authorizing a data-preserving transfer. A stable original snapshot was checked with
SQLite quick_check, version 5, before briefly force-stopping only the preview. Raw binary ADB
transfer truncated a database file; the original remained untouched and the preview's encrypted
assistant preferences stayed unchanged. After an all-byte transfer probe, the copy was repaired
using base64 transport. Eight original database/preference files matched SHA-256 in the preview;
SQLite quick_check and all database table counts matched the original snapshot. The preview's
encrypted assistant settings matched their pre-copy hash. Its own reminder preferences were
retained rather than copied. Both packages remain installed, and this is a one-time snapshot,
not ongoing synchronization. No API request, Android test or post-copy user-interface result
has been observed. The ephemeral Actions signer limitation still applies to future APKs.
