# FieldPin

Android app for field site inspections: browse sites on a list and a map, draw new site boundaries, check in only when you are physically inside a site, and capture stamped photos. It works offline and syncs to a mock server when a connection is back.

Kotlin, Jetpack Compose, Hilt, Room, CameraX, Fused Location, MapLibre (OpenStreetMap tiles).

## Build and run

Requirements: Android Studio (recent) or JDK 17 plus the Android SDK with platform 37 installed.

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

No extra files are needed on a clean checkout. The APK lands in `app/build/outputs/apk/debug/`. A prebuilt debug APK is attached to the GitHub Release.

Optional overrides go in a git-ignored `local.properties` (see `local.properties.example`): `FIELDPIN_API_BASE_URL`, `FIELDPIN_TILE_URL`, and the demo credentials. Nothing secret is required; the map uses OpenStreetMap raster tiles, so there is no map API key.

## Demo login

| Email | Password |
| --- | --- |
| `demo@fieldpin.app` | `Field@123` |

The login screen also has a "Use demo account" button. The session is stored in DataStore and survives restarts. Logout wipes the database, saved photos and the session.

## Architecture

```
ui (Compose screens + ViewModels, StateFlow)
  |
domain (models, repository interfaces, use cases, GIS maths)   <- pure Kotlin, unit tested
  |
data (Room, Retrofit + in-process fake server, location, camera/stamp, DataStore)
```

- Single activity, Navigation Compose, MVVM with unidirectional state (`StateFlow`, collected with `collectAsStateWithLifecycle`).
- Repository interfaces live in `domain`, implementations in `data`, wired with Hilt (`di/`).
- Room is the single source of truth. The UI never reads from the network.
- Package layout: `domain/{gis,model,repository,usecase,location}`, `data/{local,remote,repository,session,sync,seed,location,camera}`, `ui/{login,main,sites,map,newsite,inspection,camera,common,navigation,theme}`.

## How area and the inside check are calculated

All in `domain/gis/GeoMath.kt`, covered by unit tests.

- **Area**: spherical polygon area (Chamberlain and Duquette), on a sphere of radius 6,371,008.8 m. It is geodesic, not a flat projection, so it holds up for large polygons. Acres are `m² / 4046.8564224`.
- **Perimeter and distance**: haversine great-circle distance, summed around the ring.
- **Inside check**: ray casting (point in polygon) on lat/lng. Points exactly on an edge are undefined, so GPS accuracy is the real tolerance there.
- **"X m away"**: shortest distance from the user to any polygon edge (point to segment, in a local equirectangular frame). This is the distance shown when you are outside.
- **Self-crossing boundaries** (bonus): segment intersection by orientation test. A crossing outline disables Save.

## Check-in and photo stamp

- Start inspection is enabled only when the latest fix is inside the polygon. Once checked in, later GPS drift does not undo it.
- Location uses the Fused provider when Google Play Services exists, with a plain GPS `LocationManager` fallback for devices without it. Updates are collected only while the screen is visible, so they stop when it is not. No background location permission is requested.
- The camera is CameraX. After capture, `PhotoStamper` decodes the JPEG, applies the EXIF rotation, draws a box with the site name, coordinates with accuracy, and date-time directly onto the pixels, and writes it back. The stamp is part of the saved file, not an overlay.

## Permissions

Asked at first use of the feature, after an explanation card. Deny shows the card again with a retry. "Don't ask again" (or a previously denied permission) turns the button into "Open settings". A separate card appears when device location is switched off.

## Offline and sync

- Every write goes to Room first and marks the row `PENDING`. The list, map and sheet show a "Pending sync" badge and the toolbar shows a pending count.
- **Sync now** pushes pending sites and inspections in one request to the mock server and merges the response back. Sync also runs automatically when connectivity returns while the app is open. With no connection it reports offline and keeps the changes queued.
- The mock server is an in-process OkHttp interceptor with in-memory state and a small delay. It is a stand-in, so its data resets when the app process dies.

### Two phones editing the same site offline

Each site carries a `version` (bumped by the server on every accepted change) and an `updatedAt`. A phone sends the version it last saw.

- If the server version still matches, the change is applied and the version is bumped.
- If the version is stale (someone else synced first), it is a conflict. The server resolves it as **last write wins by `updatedAt`**, bumps the version, and reports the site as conflicted so the client can tell the user.
- Inspections are append-only records with their own ids, so they never overwrite each other. Both phones' check-ins survive; only the site's mutable fields (status, last inspection date) go through the conflict rule.

Trade-off: last write wins can silently drop an edit, and it trusts device clocks. A production system would use server timestamps or field-level merges, and show the user a choice for real conflicts.

## Seed data

22 sites around Ahmedabad, generated deterministically around real landmark centres with 7-vertex jittered outlines. **The outlines are approximated, not surveyed.**

## Decisions worth knowing

- **MapLibre with OpenStreetMap tiles** instead of Google Maps, so there is no API key to leak. The OpenGL artifact (`android-sdk-opengl`) is used because the default Vulkan build crashed on emulators without a Vulkan GPU. OSM's public tile server is for light use; a real release should use a proper tile provider or offline packs.
- **compileSdk 37, targetSdk 36, minSdk 26.** Several current AndroidX libraries require compileSdk 37; the target stays within the allowed 35 or 36.
- `kotlinx-serialization` 1.9.0 and coroutines 1.10.2 are pinned on purpose: newer ones pull a Kotlin 2.4 stdlib that this toolchain cannot read.
- Base URLs, tile URL and demo credentials are `BuildConfig` fields per build type, not strings in Kotlin.

## Tests

`./gradlew testDebugUnitTest` runs the unit tests: area, perimeter, distance, point in polygon, edge cases and self-intersection (`GeoMathTest`); seed data validity; the sync merge and conflict rules; check-in decision; stamp text; GeoJSON export; the walk-mode filter.

## Bonus features

- **Reject self-crossing boundaries**: Save is disabled when the outline crosses itself (orientation test, unit tested).
- **Walk mode**: on the new-site screen, walk the boundary and every GPS fix of 20 m accuracy or better, at least 5 m from the last corner, becomes a corner. Worse fixes are ignored (`WalkFilter`, unit tested).
- **GeoJSON export**: the share button in the toolbar exports all sites as an RFC 7946 FeatureCollection (`[lng, lat]`, closed counter-clockwise rings) through the system share sheet, using a `FileProvider`.
- **Attachments**: an inspection can carry files picked with the system document picker (SAF). They are copied into app storage so they work offline, and are deleted on logout.
- **Caretaker from Contacts**: pick a phone entry for a site, and call it from the site sheet. It uses `ACTION_PICK`, which grants one-off access to the chosen row, so the app never asks for the Contacts permission. It is a normal site edit, so it goes through sync and conflict handling.
- **WorkManager background sync**: a periodic job (15 minutes, the platform minimum) with a network constraint and exponential backoff. It does nothing when signed out. Workers are built through a Hilt-injected `WorkerFactory`.
- **Dark mode**: a full green Material 3 scheme for light and dark, following the system setting.
- **GitHub Actions**: `.github/workflows/android.yml` runs unit tests and builds the debug APK on every push.
- Room schema moved from v1 to v3 with explicit migrations (no destructive fallback); the schema JSON files are committed.

## What I cut, and why

- **Offline map tiles**: OpenStreetMap's tile usage policy forbids bulk downloading from its public tile servers, so doing this properly needs a paid or self-hosted tile source, which is out of scope here. Tiles the map has already viewed are cached by MapLibre.
- No instrumented or Compose UI tests, and the Room migrations are checked by hand against the exported schema rather than by a migration test.
- Attachments and photos stay on the device; sync sends only their counts to the mock server.

## Next steps

Field-level merge and a conflict screen, offline tile packs from a licensed tile source, a real backend, photo and attachment upload during sync, instrumented tests for the check-in flow, and a proper dark theme.

## AI use

I used Claude (Anthropic's Claude Code) as a pair programmer throughout this project. It wrote most of the first drafts of the code, tests and this README, from the requirements in the brief and my review. I chose the scope, reviewed the code, and tested on an emulator with mocked GPS; bugs found that way (for example the photo not appearing on the form after the camera, and sync failing with a saved session after a restart) were found and fixed during that testing. I can explain the architecture, the geometry and the sync design above. The seed outlines and the fake server are generated and approximate.
