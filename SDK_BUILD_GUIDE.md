# UPI AutoPay SDK - Build & Publishing Guide

## Project Status
✅ All issues resolved and fully functional

## Build Configuration

### Java Version
- **Source/Target Compatibility**: Java 17
- **Kotlin JVM Target**: 17
- **JitPack JDK**: openjdk17
- **Gradle**: 8.13

### Key Files
- **Library Module**: `andorid-autopay-demo-lib/`
- **AAR Output**: `andorid-autopay-demo-lib/build/outputs/aar/andorid-autopay-demo-lib-release.aar`
- **Maven Coordinates**: `com.github.akhilmishra-FE:SDK-Andorid-Testing:1.0.2`

## Build Commands

### 1. Build Release AAR
```bash
./gradlew :andorid-autopay-demo-lib:clean :andorid-autopay-demo-lib:assembleRelease
```

### 2. Publish to Maven Local
```bash
./gradlew :andorid-autopay-demo-lib:publishToMavenLocal
```

### 3. Build App
```bash
./gradlew :app:assembleDebug
```

### 4. View Available Tasks
```bash
./gradlew tasks
```

## Published Artifacts

### Maven Local Repository
Location: `~/.m2/repository/com/github/akhilmishra-FE/SDK-Andorid-Testing/1.0.2/`

Files:
- `SDK-Andorid-Testing-1.0.2.aar` (125KB) - Main library
- `SDK-Andorid-Testing-1.0.2-sources.jar` (28KB) - Source code
- `SDK-Andorid-Testing-1.0.2.pom` (4.3KB) - Maven metadata
- `SDK-Andorid-Testing-1.0.2.module` (5.5KB) - Gradle metadata

## JitPack Publishing

### Prerequisites
1. Push code to GitHub
2. Create a release tag

### Steps
```bash
# 1. Commit all changes
git add .
git commit -m "Release version 1.0.2"

# 2. Create and push tag
git tag v1.0.2
git push origin main
git push origin v1.0.2
```

### Usage in Other Projects
```gradle
// In settings.gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}

// In app/build.gradle
dependencies {
    implementation 'com.github.akhilmishra-FE:SDK-Andorid-Testing:v1.0.2'
}
```

## SDK Usage

### Initialize SDK
```kotlin
import com.dec.andorid_autopay_demo_lib.UPIAutoPaySDK

// Launch login screen
UPIAutoPaySDK.launchLogin(context)

// Launch details screen with data
UPIAutoPaySDK.launchDetails(
    context = context,
    name = "John Doe",
    accountNumber = "1234567890",
    ifsc = "ABCD0123456",
    upiId = "john@upi"
)
```

## Troubleshooting

### Gradle Tasks Command
If `./gradlew tasks` fails, the workaround is already in `build.gradle`:
```groovy
tasks.configureEach {
    if (name == 'outgoingVariants') {
        enabled = false
    }
}
```

### Build Errors
1. Clean build: `./gradlew clean`
2. Stop daemon: `./gradlew --stop`
3. Rebuild: `./gradlew :andorid-autopay-demo-lib:assembleRelease`

### JitPack Build Failures
- Check `jitpack.yml` configuration
- Verify Java version matches (openjdk17)
- Check build logs at: `https://jitpack.io/com/github/akhilmishra-FE/SDK-Andorid-Testing/<version>/build.log`

## Configuration Files

### jitpack.yml
```yaml
jdk:
  - openjdk17

build:
  - chmod +x ./gradlew
  - ./gradlew :andorid-autopay-demo-lib:clean
  - ./gradlew :andorid-autopay-demo-lib:assembleRelease --stacktrace
```

### gradle.properties
- No machine-specific paths
- Compatible with CI/CD
- Works on all systems

## Features

### SDK Capabilities
- ✅ UPI AutoPay integration
- ✅ Login screen with Compose UI
- ✅ Account details display
- ✅ Retrofit API integration
- ✅ Material 3 design
- ✅ Fully reusable library

### Build Features
- ✅ AAR generation
- ✅ Maven publishing
- ✅ JitPack compatible
- ✅ Gradle 9.0 compatible
- ✅ No deprecated warnings
- ✅ ProGuard configured

## Version History

### v1.0.2 (Current)
- Fixed Gradle 8.13 outgoingVariants bug
- Updated to Java 17
- Gradle 9.0 compatibility
- All deprecated syntax removed
- Maven publishing configured
- JitPack ready

## Support

For issues or questions:
- GitHub: https://github.com/akhilmishra-FE/SDK-Andorid-Testing
- JitPack: https://jitpack.io/#akhilmishra-FE/SDK-Andorid-Testing

