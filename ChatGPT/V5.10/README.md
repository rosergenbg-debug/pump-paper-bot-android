# ChatGPT V5.10 — DeepSig trading correction

This directory records the isolated ChatGPT workstream requested by Serge. The compilable Android project remains at repository root so Gradle, package identity, signing compatibility and installed-state continuity are not broken.

## Scope

- Base: canonical V5.9 `main`.
- Target: V5.10 / versionCode 89.
- Primary change: reconnect `DeepSeekTradeIntentPolicy.normalize()` to the live DeepSig entry gate so a high-readiness WATCH/HOLD can become an executable BUY candidate when fresh 5/15-minute market flow independently confirms it.
- Remove the redundant second persistence cycle for already-strong independent DeepSig setups; the separate trade-verification AI call remains mandatory before any virtual BUY/EXIT.
- Add an earlier profit-protection EXIT lane: when an already-profitable DeepSig position has fresh selling and 5/15-minute deterioration, the model's EXIT can proceed to independent verification without waiting for deeply negative 30/60-minute layers.
- Preserve package `com.example.pumppaperbot.v8`, all V5 preference/database/file names, V4 archive and research ledger.
- No real exchange order path is added; all autonomous trades remain paper-only.

The V5.9 source remains preserved in Git history. Development source for V5.10 is isolated on branch `chatgpt/v5-10-deepsig-trading` until verification and merge.
