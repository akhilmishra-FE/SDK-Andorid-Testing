# JitPack Usage Guide

## 🎯 Understanding JitPack Interface

When you visit: https://jitpack.io/#akhilmishra-FE/SDK-Andorid-Testing

You'll see something like this:

```
┌────────────────────────────────────────────────────────┐
│  akhilmishra-FE / SDK-Andorid-Testing                  │
├────────────────────────────────────────────────────────┤
│                                                         │
│  Version      Status         Get it                    │
│  ─────────────────────────────────────────────────     │
│  v1.0.6       🔄 Building    [Get it]                  │
│  v1.0.5       ❌ Failed       [Get it]                  │
│  v1.0.4       ❌ Failed       [Get it]                  │
│                                                         │
└────────────────────────────────────────────────────────┘
```

## ❓ What is "Get it" Button?

### For Publishers (YOU)
**You DON'T need to click it!**

The "Get it" button is for **consumers** (developers who want to use your library).

### For Consumers (Other Developers)
When they click "Get it", it shows:

```gradle
// Add JitPack repository
repositories {
    maven { url 'https://jitpack.io' }
}

// Add dependency
dependencies {
    implementation 'com.github.akhilmishra-FE:SDK-Andorid-Testing:v1.0.6'
}
```

## 📦 How JitPack Works (For Publishers)

### Step 1: Push Code & Create Tag
```bash
git add .
git commit -m "Your changes"
git push origin main
git tag -a v1.0.6 -m "Release v1.0.6"
git push origin v1.0.6
```
✅ **This is what you just did!**

### Step 2: JitPack Detects the Tag
- JitPack monitors your GitHub repository
- It sees the new tag `v1.0.6`
- It adds it to the list on the dashboard

### Step 3: Build is Triggered
JitPack builds your library in one of two ways:

#### Option A: Automatic (Lazy Build)
- Someone requests the library for the first time
- JitPack starts building automatically
- Takes 2-5 minutes
- Once built, it's cached forever

#### Option B: Manual (Immediate Build)
1. Go to JitPack dashboard
2. Find your version in the list
3. Look for a small icon next to version (🔄 or 📋)
4. Click it to trigger build immediately

## 🔍 Understanding Build Status

### Status Icons

| Icon | Status | Meaning |
|------|--------|---------|
| 🔄 | Building | JitPack is currently building your library |
| ✅ | Success | Build completed, library is ready to use |
| ❌ | Failed | Build failed, check logs |
| ⏸️ | Not Built | Tag exists but not built yet (lazy build) |

### Checking Build Logs

Click on the status icon or version number to see:
- **Build Log**: Detailed output of Gradle build
- **Build Time**: How long it took
- **Artifacts**: List of generated files (AAR, POM, etc.)

## 🎯 What You Should Do Now

### Option 1: Wait (Recommended)
1. Wait 2-3 minutes
2. Refresh the JitPack page
3. Look for ✅ next to v1.0.6

### Option 2: Manual Trigger
1. Go to: https://jitpack.io/#akhilmishra-FE/SDK-Andorid-Testing
2. Find v1.0.6 in the version list
3. Click on it to trigger build
4. Watch the build log in real-time

## 📋 Build Log Locations

### Current Version (v1.0.6)
- **Dashboard**: https://jitpack.io/#akhilmishra-FE/SDK-Andorid-Testing/v1.0.6
- **Build Log**: https://jitpack.io/com/github/akhilmishra-FE/SDK-Andorid-Testing/v1.0.6/build.log

### Check for Success
Look for this at the end of build log:
```
BUILD SUCCESSFUL in 45s
Installing...
Done
```

### Check for Failure
If you see:
```
BUILD FAILED
```
Read the error message above it and compare with local build.

## ✅ After Successful Build

### Test Integration
Create a test Android project and add:

```gradle
// In settings.gradle or build.gradle (project level)
repositories {
    google()
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

// In build.gradle (app level)
dependencies {
    implementation 'com.github.akhilmishra-FE:SDK-Andorid-Testing:v1.0.6'
}
```

### Use in Code
```kotlin
import com.dec.andorid_autopay_demo_lib.UPIAutoPaySDK

// Launch SDK
UPIAutoPaySDK.launchLogin(context)
```

## 🐛 Troubleshooting

### Build Shows "Error building" with No Details

**Cause**: Usually a compatibility issue or missing configuration

**Solution**:
1. Check build log URL directly
2. Compare with local `./gradlew assembleRelease` output
3. Ensure all configs match between local and JitPack

### Build Takes Too Long (>5 minutes)

**Possible causes**:
- JitPack is busy (many builds in queue)
- Large dependencies being downloaded
- First-time setup for your configuration

**What to do**:
- Wait patiently
- Refresh page periodically
- Check build log for progress

### Build Succeeds but Can't Use Library

**Check**:
1. Correct repository added (`maven { url 'https://jitpack.io' }`)
2. Correct dependency format (`com.github.USERNAME:REPO:VERSION`)
3. Sync Gradle after adding dependency
4. Check internet connection (Gradle needs to download from JitPack)

## 📞 Getting Help

If build fails repeatedly:

1. **Compare Environments**:
   - Local build: `./gradlew clean assembleRelease --info`
   - JitPack build log
   - Look for differences

2. **Common Issues**:
   - Java version mismatch (check `jitpack.yml`)
   - Gradle version incompatibility
   - Missing dependencies
   - ProGuard errors

3. **JitPack Support**:
   - Email: contact@jitpack.io
   - Include: repo URL, tag, build log link
   - Mention: "Build fails on JitPack but succeeds locally"

## 🎉 Summary

- **"Get it" button**: For consumers, not publishers
- **You're done**: Code pushed, tag created ✓
- **Next**: Wait for JitPack to build (auto or manual)
- **Success**: ✅ icon appears next to v1.0.6
- **Use**: Others can now add your library with JitPack

---

**Current Status**: v1.0.6 released, waiting for JitPack build
**Last Updated**: December 30, 2025

