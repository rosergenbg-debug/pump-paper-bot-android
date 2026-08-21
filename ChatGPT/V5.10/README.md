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

## Verification

- Pull request: #55, branch `chatgpt/v5-10-deepsig-trading`.
- GitHub Actions run 238 passed `testDebugUnitTest`, `lintDebug`, `assembleDebug`, APK integrity and package/version checks on the final application-code revision.
- CI verified package `com.example.pumppaperbot.v8`, versionCode 89, versionName 5.10 and launchable `MainActivity`.
- The CI artifact is intermediate only.
- The final APK was re-signed outside the repository with the permanent personal update key. No key material or password was committed to GitHub.
- Final and installed-line V5.9 certificates match exactly: SHA-256 `1f778c4291c9d11c5f89f4de8773bda35a0125031adc05785daee23f27dc7823`.
- Final APK verifies with APK Signature Scheme v2 and v3.
- Final APK size: 7,502,196 bytes.
- Final APK SHA-256: `d10767a210d2f44eaa986564b24b297b2f06bf0d11b88b7f14e701bf15613f11`.

The V5.9 source remains preserved in Git history. Development source for V5.10 remains isolated on branch `chatgpt/v5-10-deepsig-trading` until PR #55 is merged.
