# Fin Tweaks

A personal [Morphe](https://github.com/MorpheApp) patch bundle that removes in-app
ads at the source — they are never fetched, loaded or rendered (not just
network-blocked).

## Patches

The patch matches methods by exact class + method name (the target app's ad layer
is not obfuscated), which is robust across app updates. Source lives in
`patches/src/main/kotlin/app/template/patches/example/`.

| # | Surface | Change |
|---|---------|--------|
| 1 | Feed / list ads | ad provider returns an empty ad unit |
| 2 | Home banner | "can show ad" → false |
| 3 | Markets top ad | "can show ad" → false |
| 4 | Detail-page top banner | row builder returns null |
| 5 | Detail-page native ads | row builder returns null |

Hooks 4 & 5 reuse the app's own "no ad row" path, so there is no placeholder gap.

## Build the bundle (`.mpp`)

Requires JDK 21+, the Android SDK, and a GitHub `read:packages` token in
`~/.gradle/gradle.properties` (`gpr.user` / `gpr.key`).

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@24       # any JDK 21+
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew buildAndroid
# -> patches/build/libs/patches-1.0.0.mpp
```

## Use it

**In Morphe Manager:** add this repo's release as a patch source (URL), or import
the built `.mpp` from device storage (Manager v1.16.0+). Then select the target app
and patch on-device — Manager merges split APKs and signs automatically, so future
app updates are re-patched on the phone.

**On the desktop:** `./repatch.sh <app>.apkm` then `adb install -r out/app-noads.apk`.

## Updating

Fingerprints are name-based, so minor app updates usually need no changes — just
rebuild. If a hook stops matching, the patch log names the failed fingerprint;
update that class/method name in `Fingerprints.kt`.
