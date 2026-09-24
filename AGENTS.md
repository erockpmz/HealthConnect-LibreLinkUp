# HealthConnect-LibreLinkUp: project instructions

Read `PROJECT_CONTEXT.md` before changing this app. This is Eric's fork of
Sam Steele's app; the upstream `README.md` is kept as it is.

- Work from this repository's root. GitHub: https://github.com/erockpmz/HealthConnect-LibreLinkUp
  (fork of https://github.com/c99koder/HealthConnect-LibreLinkUp); default
  branch `main`; the original is the `upstream` remote. Pull upstream
  releases in with `git fetch upstream && git merge upstream/main`, and
  keep Eric's own changes on top of them rather than rewriting upstream
  files wholesale, so merges stay small.
- Application ID `org.c99.healthconnect_librelinkup`. **The copy on Eric's
  phone is the upstream release APK, signed by the author.** A build from
  here carries a different certificate and cannot install over it: the
  phone would have to uninstall first, which loses only the LibreLinkUp
  login and the cached token, not any glucose data (that lives in Health
  Connect). Say so before delivering a build, and never treat the
  uninstall as routine.
- The app's only job is to move readings from LibreView to Health Connect.
  Keep it to that: no analytics, no accounts beyond the LibreLinkUp one,
  no other network hosts. The one network call is to the LibreView API
  region the user picked, carrying the login and the token.
- Never invent a glucose value, and never change a reading's timestamp
  policy without understanding the upstream workaround (see
  `PROJECT_CONTEXT.md`); HealthView reads what this writes.
- Do not commit LibreLinkUp credentials, tokens, exports, signing keys,
  local SDK paths, APKs or build caches.
- Toolchain: Gradle 8.13, AGP 8.13.2, Kotlin 1.9.0, compile/target SDK 34,
  minimum 28; JDK 17 (`/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`).
  On this Mac run one build at a time:

  ```sh
  ./gradlew --no-daemon --max-workers=1 -Dorg.gradle.jvmargs="-Xmx1536m" :app:assembleDebug
  ```

  Upstream has no tests; Eric's fork adds JUnit tests under `app/src/test`
  (run them: `:app:testDebugUnitTest`). First built on this Mac on
  September 23, 2026 with Eric's say-so (see `PROJECT_CONTEXT.md`,
  "Building here"); `local.properties` is untracked and carries `sdk.dir`.
- If a build is ever delivered: bump `versionCode` first, check the APK
  with `aapt2 dump badging`, verify the signer with `apksigner verify
  --print-certs`, and copy it to Dropbox `/Phone Apps` as
  `HealthConnect-LibreLinkUp-<version>-<change>-<commit>-debug.apk`,
  replacing the previous one. That folder holds exactly one APK per app;
  never touch another app's.
- Every completed change is committed and pushed to `origin/main`, then
  `~/Development/backup-repos.sh`.
