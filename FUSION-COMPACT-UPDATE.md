# PUMP Signal V6.9.4 — Bitpanda Fusion compact dashboard

**Base:** V6.9 branch `codex/v6.9-logs-dashboard`. Old V6.9 functionality and position state remain in the codebase. Compact Fusion screen is the launcher.

## Update over installed Android application

- **applicationId:** `com.example.pumppaperbot.v8` (UNCHANGED)
- **versionCode:** `133` (greater than original V6.9's 129, and current main's 132)
- **versionName:** `6.9.4`
- **release signature:** MUST match SHA-256 certificate digest
  `1f778c4291c9d11c5f89f4de8773bda35a0125031adc05785daee23f27dc7823`
  recorded in V6.9-compatible-signing CI. Do not assume the installed APK uses this certificate without verifying it.
- **Never distribute** `app-debug.apk` as an in-place compatible update: debug signing is not verified to match the installed package.
- The isolated workflow `.github/workflows/fusion-compact.yml` compiles, unit-tests, lints, and makes a compatibility-signed APK only if the matching old keystore and passwords are present in GitHub Actions secrets. If missing, signing is skipped, and the debug APK is for test devices only.
- **Do not uninstall the old app. Do not clear app data.** If a signing mismatch appears during install, stop and recover/check original signing certificate rather than uninstalling or changing package ID.
- Before installation, back up position/trading data using existing safe backup facilities. No reset or delete is part of this change.

## Data & controls

- Bitpanda Fusion Read API key is saved in Android Keystore (existing `BitpandaFusionSecureKeyStore`), never committed.
- PUMP/EUR and BTC/EUR chart values from Fusion market-data API; PUMP `bid` is used to mark an open position to market before fees; actual buy/sell execution prices can be manually corrected and confirmed.
- Both charts show clock axis + separate left price scale, no right-side 1m/5m/15m/30m flow gauges.
- PUMP intervals: 5m / 15m / 30m / 1h; pressing **Я КУПИЛ** forces display to 30m (without changing the strategy's candle timeframe).
- Keeps old `PumpBotEngine` strategy, background monitoring, account state, `ManualPositionStore` and `UserPaperStore`.
- Removes old duplicate paper account controls, human factor banner, logs, trade-history button and x2 overview **from the launch screen**, not by deleting data or modules.

## Validation checklist

1. CI passes `testDebugUnitTest lintDebug assembleDebug`.
2. CI checks `package`, `versionCode`, launcher.
3. Compatible APK is signed with the exact expected certificate and verified by `apksigner`.
4. Compare that certificate against the actual installed V6.9 application's signer before installing.
5. Check unchanged saved open position, 30m display after buy, manually confirmed bid/ask price capture, and all intervals with a user-created **Read** key.
6. Check offline or stale API responses do not prompt trading against stale prices.
7. Do not merge this branch into main or install on the primary phone until checks pass.

**Current status:** source changes only; no verified signed APK has been produced in this conversation.
