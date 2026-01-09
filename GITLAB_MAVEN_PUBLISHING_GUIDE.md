# GitLab Maven Repository - Publishing & Usage Guide

This guide covers how to publish the UPI AutoPay SDK to GitLab's Maven repository and how to consume it in other Android projects.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Publishing to GitLab Maven](#publishing-to-gitlab-maven)
- [Using the Published SDK](#using-the-published-sdk)
- [Version Management](#version-management)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### 1. GitLab Project Setup
- **Repository**: `git@gitlab.com:decentro1/decentro-autopay-android.git`
- **Project ID**: `65173949` (already configured)
- **Repository Type**: Private

### 2. GitLab Personal Access Token

You need a GitLab Personal Access Token with the following scopes:
- `api` - Full API access
- `write_repository` - Write access to repository

#### How to Generate Token:

1. Go to GitLab: https://gitlab.com
2. Click on your profile picture (top right) → **Edit Profile**
3. Navigate to **Access Tokens** in the left sidebar
4. Click **Add new token**
5. Fill in the details:
   - **Token name**: `Maven Publishing Token` (or any name)
   - **Expiration date**: Set as needed (recommended: 1 year)
   - **Select scopes**: ✓ `api` and ✓ `write_repository`
6. Click **Create personal access token**
7. **IMPORTANT**: Copy the token immediately (it won't be shown again!)

---

## Publishing to GitLab Maven

### Step 1: Configure Your Local Environment

You have two options to provide your GitLab token:

#### Option A: Local gradle.properties (Recommended for Local Development)

Create or edit `~/.gradle/gradle.properties` (in your home directory):

```properties
GITLAB_PRIVATE_TOKEN=your_gitlab_token_here
```

**⚠️ NEVER commit this file to Git!**

#### Option B: Environment Variable (Recommended for CI/CD)

Set the environment variable in your terminal:

**macOS/Linux:**
```bash
export GITLAB_PRIVATE_TOKEN=your_gitlab_token_here
```

**Windows (Command Prompt):**
```cmd
set GITLAB_PRIVATE_TOKEN=your_gitlab_token_here
```

**Windows (PowerShell):**
```powershell
$env:GITLAB_PRIVATE_TOKEN="your_gitlab_token_here"
```

To make it permanent, add to your shell profile (`~/.zshrc`, `~/.bashrc`, etc.):
```bash
echo 'export GITLAB_PRIVATE_TOKEN=your_gitlab_token_here' >> ~/.zshrc
source ~/.zshrc
```

### Step 2: Update Version Number (Optional)

Edit `gradle.properties` in your project root:

```properties
VERSION_NAME=1.0.0  # Update this for each release
```

### Step 3: Build the SDK

```bash
cd /Users/akhil.mishra/upiautopaysdk2
./gradlew :andorid-autopay-demo-lib:clean
./gradlew :andorid-autopay-demo-lib:assembleRelease
```

### Step 4: Publish to GitLab Maven

```bash
./gradlew :andorid-autopay-demo-lib:publishMavenPublicationToGitLabRepository
```

### Step 5: Verify Publication

1. Go to your GitLab project: https://gitlab.com/decentro1/decentro-autopay-android
2. Navigate to **Deploy** → **Package Registry** in the left sidebar
3. You should see your package: `com.decentro.autopay:upi-autopay-sdk:1.0.0`

---

## Using the Published SDK

Once published, here's how to integrate the SDK into any Android project:

### Step 1: Configure Authentication in Consumer Project

Developers consuming your SDK need a GitLab token with `read_api` scope.

#### Generate Read-Only Token (For SDK Consumers):

1. Go to GitLab → Profile → **Access Tokens**
2. Create token with:
   - **Token name**: `Maven Read Token`
   - **Scopes**: ✓ `read_api` only
3. Copy the token

### Step 2: Configure Consumer Project

In the **consumer project** (the app that will use your SDK):

#### 2.1 Add Token to Local gradle.properties

Create or edit `~/.gradle/gradle.properties`:

```properties
GITLAB_READ_TOKEN=your_read_token_here
```

#### 2.2 Update settings.gradle (or settings.gradle.kts)

Add GitLab Maven repository:

**For Groovy (settings.gradle):**
```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        
        // GitLab Maven Repository for Decentro UPI AutoPay SDK
        maven {
            url = uri("https://gitlab.com/api/v4/projects/77552064/packages/maven")
            name = "GitLab"
            credentials(HttpHeaderCredentials) {
                name = "Private-Token"
                value = project.findProperty('GITLAB_READ_TOKEN') ?: System.getenv('GITLAB_READ_TOKEN')
            }
            authentication {
                header(HttpHeaderAuthentication)
            }
        }
    }
}
```

**For Kotlin (settings.gradle.kts):**
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        
        // GitLab Maven Repository for Decentro UPI AutoPay SDK
        maven {
            url = uri("https://gitlab.com/api/v4/projects/77552064/packages/maven")
            name = "GitLab"
            credentials(HttpHeaderCredentials::class) {
                name = "Private-Token"
                value = project.findProperty("GITLAB_READ_TOKEN")?.toString() 
                    ?: System.getenv("GITLAB_READ_TOKEN")
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }
}
```

#### 2.3 Add Dependency in app/build.gradle

```groovy
dependencies {
    // Decentro UPI AutoPay SDK
    implementation 'com.decentro.autopay:upi-autopay-sdk:1.0.0'
    
    // Your other dependencies...
}
```

#### 2.4 Sync and Build

```bash
./gradlew clean build
```

### Step 3: Initialize and Use the SDK

In your Application or Activity:

```kotlin
import com.dec.andorid_autopay_demo_lib.UPIAutoPaySDK
import com.dec.andorid_autopay_demo_lib.UPIAutoPaySDKManager

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize the SDK
        val sdkConfig = UPIAutoPaySDK.Configuration(
            clientId = "your_client_id",
            clientSecret = "your_client_secret",
            providerId = "your_provider_id",
            environment = UPIAutoPaySDK.Environment.SANDBOX // or PRODUCTION
        )
        
        UPIAutoPaySDKManager.initialize(this, sdkConfig)
        
        // Launch UPI AutoPay flow
        findViewById<Button>(R.id.btnStartUPI).setOnClickListener {
            val mandateDetails = UPIAutoPaySDK.MandateDetails(
                amount = "100.00",
                recurrence = "MONTHLY",
                // ... other details
            )
            
            UPIAutoPaySDKManager.startMandateCreation(
                context = this,
                mandateDetails = mandateDetails,
                callback = object : UPIAutoPaySDK.MandateCallback {
                    override fun onSuccess(mandateId: String) {
                        // Handle success
                    }
                    
                    override fun onFailure(error: String) {
                        // Handle failure
                    }
                }
            )
        }
    }
}
```

---

## Version Management

### Semantic Versioning

Follow semantic versioning (MAJOR.MINOR.PATCH):
- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes

### Publishing a New Version

1. Update `VERSION_NAME` in `gradle.properties`:
   ```properties
   VERSION_NAME=1.1.0
   ```

2. Commit the version change:
   ```bash
   git add gradle.properties
   git commit -m "Bump version to 1.1.0"
   git tag v1.1.0
   git push origin main --tags
   ```

3. Publish to GitLab:
   ```bash
   ./gradlew :andorid-autopay-demo-lib:clean
   ./gradlew :andorid-autopay-demo-lib:assembleRelease
   ./gradlew :andorid-autopay-demo-lib:publishMavenPublicationToGitLabRepository
   ```

### Using Specific Versions

Consumers can specify exact versions:

```groovy
dependencies {
    // Use specific version
    implementation 'com.decentro.autopay:upi-autopay-sdk:1.0.0'
    
    // Or use version ranges (not recommended for production)
    implementation 'com.decentro.autopay:upi-autopay-sdk:1.+'
}
```

---

## Troubleshooting

### Issue: "401 Unauthorized" when publishing

**Solution:**
- Verify your `GITLAB_PRIVATE_TOKEN` is correct
- Check token has `api` and `write_repository` scopes
- Token hasn't expired

### Issue: "Could not find com.decentro.autopay:upi-autopay-sdk:1.0.0"

**Solution:**
- Verify the package exists in GitLab Package Registry
- Check `GITLAB_READ_TOKEN` in consumer project
- Ensure repository URL and Project ID are correct in `settings.gradle`
- Try `./gradlew --refresh-dependencies`

### Issue: "Project ID not found"

**Solution:**
- Verify `GITLAB_PROJECT_ID=77552064` in gradle.properties
- Ensure you have access to the GitLab project

### Issue: Token stored in version control

**Solution:**
- **NEVER** commit tokens to Git!
- Add to `.gitignore`:
  ```
  # Local Gradle properties
  local.properties
  gradle.properties
  ```
- Use environment variables for CI/CD
- Rotate compromised tokens immediately

### Issue: Dependency resolution fails

**Solution:**
```bash
# Clear Gradle cache
./gradlew clean
rm -rf ~/.gradle/caches/

# Refresh dependencies
./gradlew --refresh-dependencies
```

### Issue: Multiple versions causing conflicts

**Solution:**
```groovy
// Force specific version
configurations.all {
    resolutionStrategy {
        force 'com.decentro.autopay:upi-autopay-sdk:1.0.0'
    }
}
```

---

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Publish SDK

on:
  push:
    tags:
      - 'v*'

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Publish to GitLab Maven
        env:
          GITLAB_PRIVATE_TOKEN: ${{ secrets.GITLAB_PRIVATE_TOKEN }}
        run: |
          ./gradlew :andorid-autopay-demo-lib:publishMavenPublicationToGitLabRepository
```

### GitLab CI/CD Example

Create `.gitlab-ci.yml`:

```yaml
stages:
  - build
  - publish

build:
  stage: build
  image: openjdk:17-jdk
  script:
    - ./gradlew :andorid-autopay-demo-lib:assembleRelease
  artifacts:
    paths:
      - andorid-autopay-demo-lib/build/outputs/

publish:
  stage: publish
  image: openjdk:17-jdk
  only:
    - tags
  script:
    - ./gradlew :andorid-autopay-demo-lib:publishMavenPublicationToGitLabRepository
  variables:
    GITLAB_PRIVATE_TOKEN: $CI_JOB_TOKEN
```

---

## Security Best Practices

1. **Never commit tokens** to version control
2. **Use read-only tokens** (`read_api`) for SDK consumers
3. **Use scoped tokens** with minimum required permissions
4. **Rotate tokens** regularly (every 90-180 days)
5. **Use environment variables** in CI/CD pipelines
6. **Revoke tokens** immediately if compromised
7. **Store tokens securely** using password managers

---

## Quick Reference

### Publishing Command
```bash
./gradlew :andorid-autopay-demo-lib:publishMavenPublicationToGitLabRepository
```

### SDK Coordinates
```
groupId: com.decentro.autopay
artifactId: upi-autopay-sdk
version: 1.0.0
```

### Repository URL
```
https://gitlab.com/api/v4/projects/77552064/packages/maven
```

### Full Dependency String
```groovy
implementation 'com.decentro.autopay:upi-autopay-sdk:1.0.0'
```

---

## Support

For issues or questions:
- **GitLab Issues**: https://gitlab.com/decentro1/decentro-autopay-android/-/issues
- **Documentation**: See project README.md
- **Email**: Contact Decentro support

---

**Last Updated**: January 2026  
**SDK Version**: 1.0.0  
**GitLab Project**: decentro1/decentro-autopay-android

