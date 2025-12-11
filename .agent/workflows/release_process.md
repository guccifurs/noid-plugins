---
description: How to build and release the GearSwapper plugin correctly (Secure Source)
---

# Release Process Guide

**CRITICAL**: This repository is PUBLIC, but the source code must remain PRIVATE.
The `main` branch must remain EMPTY (containing only README).
All source code stays local. Releases are uploaded as JAR attachments.

## 1. Bump Version
Open `plugins/build.gradle.kts` and update:
```kotlin
// Update manifest version
"Plugin-Version" to "x.x.x",

// Update archive version (scroll up to gearSwapperJar task)
archiveVersion.set("x.x.x")
```

## 2. Build the JAR
Run the following command to generate the release artifact:
```bash
./gradlew clean gearSwapperJar
```
_Wait for the build to complete successfully._

## 3. Verify Artifact
Check that the JAR exists:
```bash
ls plugins/build/libs/gearswapper-x.x.x.jar
```

## 4. Create GitHub Release
**DO NOT** push source code to `main`.
Use the GitHub CLI to create a release targeting the empty `main` branch and upload the JAR.

```bash
gh release create vX.X.X plugins/build/libs/gearswapper-X.X.X.jar \
  --title "vX.X.X - Release Title" \
  --notes "Release notes here..." \
  --target main
```

## Summary
1.  Bump version (`build.gradle.kts`).
2.  `./gradlew clean gearSwapperJar`
3.  `gh release create ... --target main`
