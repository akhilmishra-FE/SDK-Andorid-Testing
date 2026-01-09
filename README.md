# UPI AutoPay SDK for Android

A comprehensive SDK for integrating UPI AutoPay (Mandate-based recurring payments) into Android applications.

## 🚀 Quick Setup

### 1. Configure Credentials

Copy the template and add your credentials:
```bash
cp local.properties.template local.properties
```

Edit `local.properties` with your actual values:
```properties
DECENTRO_CLIENT_ID=your_client_id
DECENTRO_CLIENT_SECRET=your_client_secret
GITLAB_PRIVATE_TOKEN=your_gitlab_token
```

### 2. Build SDK

```bash
./gradlew clean assembleRelease
```

### 3. Publish to GitLab Maven

```bash
./publish-to-gitlab.sh
```

## 📦 For SDK Consumers

Add to your `settings.gradle.kts`:
```kotlin
maven {
    url = uri("https://gitlab.com/api/v4/projects/77552064/packages/maven")
    credentials(HttpHeaderCredentials::class) {
        name = "Private-Token"
        value = project.findProperty("GITLAB_READ_TOKEN")?.toString()
    }
    authentication {
        create<HttpHeaderAuthentication>("header")
    }
}
```

Add dependency:
```groovy
implementation 'com.decentro.autopay:upi-autopay-sdk:1.0.0'
```

## 🔧 Tech Stack

- **Language**: Kotlin
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **UI**: Jetpack Compose
- **Networking**: Retrofit 2
- **Publishing**: GitLab Maven

## 🔐 Security

All sensitive data (API keys, secrets, tokens) are stored in `local.properties` which is git-ignored.

**Never commit credentials to Git!**

## 📞 Support

- **Repository**: https://gitlab.com/decentro1/decentro-autopay-android
- **Issues**: https://gitlab.com/decentro1/decentro-autopay-android/-/issues

---

**Latest Version**: 1.0.0  
**Maintained by**: Decentro Team