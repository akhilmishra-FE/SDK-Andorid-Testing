# 🔧 Java Version Compatibility Fix

## ❌ Problem
You're getting this error:
```
BUG! exception in phase 'semantic analysis' in source unit '_BuildScript_' 
Unsupported class file major version 69
```

## 🔍 What This Means

**Class file major version 69** = **Java 25**

But **Gradle 8.9** only supports up to **Java 21** (major version 65).

## ✅ Solution Applied

I've updated `gradle.properties` to force Gradle to use Android Studio's bundled JDK:

```properties
# Force Gradle to use Android Studio's bundled Java 17/21 (compatible with Gradle 8.9)
org.gradle.java.home=/Applications/Android Studio.app/Contents/jbr/Contents/Home

# Disable auto-detection to prevent using system Java 25
org.gradle.java.installations.auto-detect=false
org.gradle.java.installations.auto-download=false
```

## 🚀 Try Building Now

```bash
cd /Users/akhil.mishra/upiautopaysdk2

# Stop any running Gradle daemons
./gradlew --stop

# Clean and build with correct Java version
./gradlew clean
./gradlew :andorid-autopay-demo-lib:assembleRelease
```

## 📋 Java Version Compatibility Matrix

| Java Version | Major Version | Gradle 8.9 Support |
|--------------|---------------|-------------------|
| Java 8 | 52 | ✅ Supported |
| Java 11 | 55 | ✅ Supported |
| Java 17 | 61 | ✅ Supported |
| Java 21 | 65 | ✅ Supported |
| Java 25 | 69 | ❌ **NOT Supported** |

## 🔍 Verification

Check which Java version Gradle is now using:

```bash
./gradlew --version
```

You should see something like:
```
Java:          17.0.x or 21.0.x (JetBrains s.r.o.)
JVM:          17.0.x or 21.0.x
```

## 🐛 Alternative Solutions (If Above Doesn't Work)

### Option 1: Use JAVA_HOME Environment Variable

```bash
# Set JAVA_HOME to Android Studio's JDK
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# Verify
echo $JAVA_HOME
java -version

# Build
./gradlew :andorid-autopay-demo-lib:assembleRelease
```

### Option 2: Use Specific Java Installation

If you have Java 17 or 21 installed elsewhere:

```bash
# Find Java installations
/usr/libexec/java_home -V

# Use specific version (example)
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# Build
./gradlew :andorid-autopay-demo-lib:assembleRelease
```

### Option 3: Install Java 21 via Homebrew

```bash
# Install Java 21
brew install openjdk@21

# Set JAVA_HOME
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"

# Build
./gradlew :andorid-autopay-demo-lib:assembleRelease
```

## 🎯 Expected Result

After applying the fix, you should see:
- ✅ `./gradlew --version` shows Java 17 or 21
- ✅ `./gradlew :andorid-autopay-demo-lib:assembleRelease` succeeds
- ✅ `BUILD SUCCESSFUL` message

## 🔧 Android Studio Configuration

To ensure Android Studio uses the correct JDK:

1. **File** → **Settings** (or **Preferences** on Mac)
2. **Build, Execution, Deployment** → **Build Tools** → **Gradle**
3. **Gradle JDK**: Select **"Android Studio default JDK"** or **"JetBrains Runtime version 17/21"**
4. Click **Apply** and **OK**
5. **File** → **Sync Project with Gradle Files**

## 🚨 Important Notes

- **Java 25 is too new** for current Android development tools
- **Java 17 or 21** are the recommended versions for Android development
- **Android Studio's bundled JDK** is always a safe choice

## ✅ Next Steps

Once the build succeeds:

1. **Publish to GitLab**:
   ```bash
   ./publish-to-gitlab.sh
   ```

2. **Verify publication**:
   - Go to: https://gitlab.com/decentro1/decentro-autopay-android/-/packages

---

**Status**: ✅ Java version compatibility fixed  
**Next**: Build should now succeed with Java 17/21
