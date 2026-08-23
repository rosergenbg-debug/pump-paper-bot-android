# PumpSignal V5.25 — six-account comparison

## Fix

V5.24 correctly added the independent `PUMP 2% NET` paper account, but the full comparison screen was still hard-coded to five chart slots. PM2 therefore existed and traded, but was missing from the comparison view.

V5.25 changes only that presentation/integration defect:

1. `PUMP 3% NET`
2. `PUMP 2% NET`
3. `DEEPSIGX`
4. `APP`
5. `DEEPSIG FUSION`
6. `СЕРЖ`

The screen title now says `СРАВНЕНИЕ ШЕСТИ СЧЕТОВ`, allocates six synchronized chart slots, and the main comparison button also says `СРАВНИТЬ 6`.

## Safety / state

- No trading thresholds or entry/exit logic changed.
- No preference migration.
- No reset.
- Existing Fusion, PM3, PM2, APP, DeepSigX and user history/state are preserved.
- Package remains `com.example.pumppaperbot.v8` so a correctly signed APK updates in place.

## Regression check

`CompetitionAccountSpecTest` locks the comparison contract to six accounts and explicitly requires both PM3 and PM2.

## Version

- versionName: `5.25`
- versionCode: `105`
