# HealthConnect-LibreLinkUp: current project state

- Upstream: https://github.com/c99koder/HealthConnect-LibreLinkUp (Sam Steele, Apache 2.0)
- Fork: https://github.com/erockpmz/HealthConnect-LibreLinkUp (branch `main`, remote `upstream` for the original)
- Version: upstream **1.5**, versionCode 5 (August 7, 2026); no changes of Eric's yet
- Application ID `org.c99.healthconnect_librelinkup`; Android 9+; Health Connect

## Why it is here

Eric wears a Freestyle Libre sensor. HealthView shows blood glucose, but
HealthView only reads `BloodGlucoseRecord`s from Health Connect; it never
talks to the sensor or to Abbott. This app is the missing link. Eric
installed the upstream release on his S23 in September 2026 and asked for
the repo to be managed with the other health apps, so the whole chain is
on record:

1. The Libre sensor reports to the Freestyle Libre app on the phone.
2. Abbott's LibreLinkUp sharing makes the readings available through the
   LibreView API to an invited "follower" account.
3. This app logs in to that follower account (region chosen in the app;
   the default host is `api-us.libreview.io`) and, every 15 minutes
   through a WorkManager periodic job, fetches the latest reading and
   writes it to Health Connect as a `BloodGlucoseRecord` (specimen source
   interstitial fluid, relation to meal unknown, in mmol/L).
4. HealthView reads it from Health Connect and charts it.

There is also a `wearable` module: a WearOS complication and tile showing
the latest reading, fed over the data layer. Whether Eric uses it is not
recorded.

## What the app does with credentials

The LibreLinkUp email and password are sent to LibreView to log in; the
returned token, the chosen region URL and related state are kept in
`EncryptedSharedPreferences` (AES-256). The password itself is not kept
after login. Uninstalling the app discards all of this and nothing else.

## Upstream quirk worth knowing

Commit `51d1775` (1.5) converts each reading's timestamp to the phone's
local zone before storing it, to work around a display bug in the new
Fitbit / Google Health app. HealthView reads the instant, so this does
not affect it, but anyone comparing HealthView with the Google app should
know the two may disagree on the hour.

## Building here

Not yet done. The Gradle wrapper and build scripts are third-party code,
and running them needs Eric's explicit go-ahead on this Mac. Until then
the phone runs the upstream release, and updates come from upstream's
GitHub Releases page. A debug build from this fork would carry Eric's
shared debug certificate, not the author's, so it cannot install over the
release (see `AGENTS.md`).

## Not decided

- Whether to build and install Eric's own copy at all. The upstream
  release works; the reason to build would be a fix Eric needs before
  upstream ships it.
- Whether HealthView should show the reading's source app. Health Connect
  records the data origin as this app's package; HealthView does not
  surface it today.
