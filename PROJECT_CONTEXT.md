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

First built on September 23, 2026, on Eric's say-so (the Gradle wrapper
and build scripts are third-party code). `local.properties` (untracked)
carries `sdk.dir=/Users/elorson/Library/Android/sdk`. The build:

```sh
./gradlew --no-daemon --max-workers=1 -Dorg.gradle.jvmargs="-Xmx1536m" :app:testDebugUnitTest :app:assembleDebug
```

A debug build carries Eric's shared debug certificate, not the author's,
so it could not install over the upstream release: on September 23, 2026
Eric uninstalled that, installed 1.5.2 (versionCode 7) and logged in
again ("installed"). The phone now runs this fork, and later builds
install over it like any other of Eric's apps. The package name is
unchanged, so Health Connect sees one continuous glucose source across
the switch.

## Eric's 1.5.1: everything a poll offers

Eric asked whether "days until the sensor ends" could be had, then for as
much as possible while the app is being rebuilt. LibreView's
`/llu/connections` reply carries far more than the one reading upstream
kept, and Moshi was silently dropping it. Now read and kept:

- **The sensor**: serial, activation (`a`, Unix seconds), warm-up (`w`,
  60 minutes on a Libre 3) and product type (`pt`). LibreView sends no
  expiry, so `SensorLife` adds the sensor's life to the activation. The
  life is a setting on the app's screen (1.5.2), **15 days by default**:
  Eric put on a new sensor and it said 15, so he is on the Libre 3 Plus
  (the original Libre 3 and the Libre 2 ran 14; 1.5.1 had assumed 14).
  Changing the setting recomputes the end from the stored activation at
  once, and the provider's `life_days` says which life was used.
- **The latest reading with its trend**: mg/dL, trend arrow (1 falling
  quickly to 5 rising quickly, `Trend`), LibreView's colour (1 in range,
  2 outside target, 3 alarm) and the high/low flags. Health Connect's
  glucose record has none of these.
- **The target range and the alarms** set in the Libre app: target low
  and high, the high, low and fixed-low alarm thresholds, the unit the
  account uses, the Libre app's version, the patient's name.
- **The last twelve hours of readings**, from `/llu/connections/{patientId}/graph`,
  a point every fifteen minutes or so. Upstream wrote only the latest
  reading each poll, so a phone off the network left holes in Health
  Connect; every graph point is now written too. Every record carries
  `clientRecordId = "llu-<epoch seconds>"` with the same number as its
  version, so re-sending a reading updates it in place rather than adding
  a twin. The timestamp handling is upstream's, pulled into
  `SyncWorker.parseTime` and used for the graph points as well.

Where it goes: `SensorStore` (a plain SharedPreferences file, `sensor`)
holds the last poll; the screen shows two lines under the login status
("Sensor ends in 6 days (3 Oct 2026)", "Last reading 112 mg/dL ↗ rising,
in range, 7:45 PM. Target 70 to 180."); and `SensorProvider` publishes it
for HealthView.

### 1.5.3: the screen says what the sync did

After the switch to the fork nothing arrived and nothing said why (Eric,
September 24, 2026: "not getting blood glucose either"). Two things the
screen never showed: whether Health Connect had allowed the app (a
reinstall clears that grant, and the first-launch prompt is easy to
miss), and how the last poll ended. Now:

- A red line and an **Allow in Health Connect** button appear whenever
  the read and write glucose permissions are not both granted; the
  permission launcher is registered once in `onCreate` and shared.
- **Last sync** line: "Last sync 9:41 PM: OK, 47 readings written", or
  "Failed: ..." with the exception, "not logged in", "LibreView returned
  no connection", or "Health Connect refused the write: ...". The worker
  now waits for Health Connect's answer (30 s) instead of firing the
  insert into a callback nobody read. A **Sync now** button runs one poll
  and the line follows it.
- Every new JSON field is boxed (`Long`, `Integer`, `Boolean`), so a null
  in an account's reply cannot sink the poll, and the sensor details are
  saved in their own try so a surprise there never costs the glucose.

### 1.5.4: the wrong account is named at login

1.5.3's "LibreView returned no connection" (Eric, September 24, 2026) is
what the API says when the login is a LibreView account that nobody
shares with: LibreView answers status 0 and an empty list. That is the
patient's own account; only the LibreLinkUp *follower* account, the one
the Libre 3 app was told to share with, receives connections. Now the
login step asks for the connections straight away and says "Sharing from
<name>" or that no one shares with the account and which login to use,
and the sync line says the same instead of "no connection". A reply
without a data list is reported separately, with the status code.

Confirmed on the S23 on September 24, 2026: it was the wrong account.
With the follower login, 1.5.4 (versionCode 9) syncs; glucose and the
sensor reach Health Connect and HealthView. **Lesson for next time the
login is entered:** the LibreLinkUp follower account, never the Libre 3
app's own.

### The provider contract

Authority `org.c99.healthconnect_librelinkup.sensor`, read-only, guarded
by the signature permission
`org.c99.healthconnect_librelinkup.permission.READ_SENSOR` (HealthView and
this build share Eric's debug certificate). Both paths return one row, or
none until the first poll, and notify on every poll:

- `content://org.c99.healthconnect_librelinkup.sensor/sensor`:
  `serial` (text), `activated_at_epoch_millis`, `ends_at_epoch_millis`,
  `life_days`, `warmup_minutes`, `product_type`, `last_sync_epoch_millis`.
- `content://org.c99.healthconnect_librelinkup.sensor/reading`:
  `reading_at_epoch_millis`, `mgdl`, `trend_arrow`, `trend` (text),
  `color`, `is_high`, `is_low` (0/1), `target_low_mgdl`,
  `target_high_mgdl`, `alarm_high_mgdl`, `alarm_low_mgdl`,
  `alarm_fixed_low_mgdl`, `unit` (1 mg/dL, 0 mmol/L),
  `libre_app_version` (text), `patient` (text), `last_sync_epoch_millis`.

A reader needs `<uses-permission>` for the permission above and a
`<queries><package android:name="org.c99.healthconnect_librelinkup"/>`.
Tests: `SensorLifeTest` (4) and `TrendTest` (2); JUnit is the one added
test dependency.

## Not decided

- Whether to offer these changes upstream. They are additive (new files,
  new fields, one extracted method) so a pull request would be small.
- Whether HealthView should show the reading's source app. Health Connect
  records the data origin as this app's package; HealthView does not
  surface it today.
