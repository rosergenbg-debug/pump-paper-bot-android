# PumpSignal signing recovery — READ BEFORE BUILDING

This repository is **public**. Never commit the private signing key, password file, or private backup ZIP here.

## App identity

- Android package: `com.example.pumppaperbot.v8`
- Signing alias: `pump-signal-update`
- Required certificate SHA-256: `1f778c4291c9d11c5f89f4de8773bda35a0125031adc05785daee23f27dc7823`

## Canonical runtime paths

After restoring the private signing kit, place the files here:

- `/mnt/data/pumpsignal-signing/v34kit/PumpSignal-Update-Key.jks`
- `/mnt/data/pumpsignal-signing/v34kit/PumpSignal-Update-Key.password`

Do **not** use `/mnt/data/pumpsignal-signing/private` for this package. That is different signing material.

## Permanent recovery locations

### 1. ChatGPT Library

Folder: `/Биткоин/`

Preferred bundle:
- `PumpSignal-SIGNING-RECOVERY-BUNDLE.zip`

Older compatible backups also present:
- `PumpSignal-V3.4-Private-Signing-Kit.zip`
- `PumpSignal-PRIVATE-UPDATE-KEY-BACKUP.zip`

A new chat should search the user's ChatGPT Library for one of these exact filenames, materialize/download the ZIP into its sandbox, and extract the `.jks` and `.password` files to the canonical runtime paths above.

### 2. Google Drive

Folder: `PumpSignal-Signing-Backup`

Drive folder URL:
`https://drive.google.com/drive/folders/1v4_9elHaUqYODphFk5v6A_ZGxATw-NE7`

Primary file:
- `PumpSignal-SIGNING-RECOVERY-BUNDLE.zip`

If ChatGPT Library recovery is unavailable, search the connected Google Drive for the exact folder/file names above and download the bundle.

## Verification before signing

Do not sign anything until the key is verified.

```bash
PASS=$(cat /mnt/data/pumpsignal-signing/v34kit/PumpSignal-Update-Key.password)
keytool -list -v \
  -keystore /mnt/data/pumpsignal-signing/v34kit/PumpSignal-Update-Key.jks \
  -storepass "$PASS" \
  -alias pump-signal-update
```

Expected certificate SHA-256:

`1F:77:8C:42:91:C9:D1:1C:5F:89:F4:DE:87:73:BD:A3:5A:01:25:03:1A:DC:05:78:5D:AE:E2:3F:27:DC:78:23`

Current verified file SHA-256 values from the recovery copy created 2026-08-22:

- JKS file: `972e56caf237e8ad432540e28ff9d52e39c39077d14e44939c2e43021438f0a3`
- Recovery source archive `PumpSignal-V3.4-Private-Signing-Kit.zip`: `1a09fa987372e2b698b2a13028c671aef1af17c4c3736184cfe86b35fe811efc`

## Critical rules

1. **Never generate a replacement key** if the APK must update the already-installed `.v8` app without uninstalling it.
2. **Never commit the JKS/password/backup ZIP to GitHub**, including private branches of this public repository.
3. Always verify the alias and certificate SHA-256 before building a release APK.
4. If the required key cannot be recovered from ChatGPT Library or Google Drive, stop the release build rather than signing with another key.
