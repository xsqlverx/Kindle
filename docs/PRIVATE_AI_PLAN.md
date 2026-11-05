# Assistant and personal capabilities

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
Cloud build, API keys, provider requests and device behavior remain unverified.

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
