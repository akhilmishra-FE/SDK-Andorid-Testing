#!/bin/bash

# Prepare JitPack environment for Android build
# This script sets up the environment for building Android libraries on JitPack

# Make gradlew executable
chmod +x ./gradlew

# Set Android SDK path (JitPack provides this)
export ANDROID_HOME=$HOME/android-sdk-linux
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# Accept Android SDK licenses
yes | sdkmanager --licenses || true

echo "JitPack environment prepared successfully"

