# Release Guide

## Creating a Release from GitHub

### Method 1: Using GitHub Web Interface (Recommended)

1. **Go to your repository on GitHub**
   - Navigate to: `https://github.com/akhilmishra-FE/SDK-Andorid-Testing`

2. **Create a new release**
   - Click on "Releases" in the right sidebar
   - Click "Create a new release"
   - Or go to: `https://github.com/akhilmishra-FE/SDK-Andorid-Testing/releases/new`

3. **Fill in release details**
   - **Tag version**: Enter version like `v1.0.0` (e.g., v1.0.0, v1.1.0)
   - **Release title**: Same as tag (e.g., v1.0.0)
   - **Description**: Add release notes describing changes
   - Select "Set as the latest release"

4. **Publish the release**
   - Click "Publish release"
   - GitHub Actions will automatically:
     - Build the AAR file
     - Upload it as a release asset
     - Make it available for download

5. **Using with JitPack**
   - After publishing, JitPack will automatically build your library
   - Use in your project:
     ```gradle
     dependencies {
         implementation 'com.github.akhilmishra-FE:SDK-Andorid-Testing:v1.0.0'
     }
     ```

### Method 2: Using Git Tags (Manual)

1. **Create and push a tag**
   ```bash
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
   ```

2. **Create release on GitHub**
   - Go to Releases page
   - Click "Draft a new release"
   - Select the tag you just pushed
   - Fill in details and publish

### Method 3: Using GitHub Actions Workflow

1. **Go to Actions tab**
   - Navigate to: `https://github.com/akhilmishra-FE/SDK-Andorid-Testing/actions`

2. **Run workflow manually**
   - Select "Build and Release AAR" workflow
   - Click "Run workflow"
   - Enter version number
   - Click "Run workflow"

## File Locations

- **AAR in root**: `./andorid-autopay-demo-lib-release.aar` (created during build)
- **AAR in build folder**: `./andorid-autopay-demo-lib/build/outputs/aar/andorid-autopay-demo-lib-release.aar`
- **Published to Maven Local**: `~/.m2/repository/com/github/akhilmishra-FE/SDK-Andorid-Testing/`

## Versioning

- Use semantic versioning: `MAJOR.MINOR.PATCH` (e.g., 1.0.0, 1.1.0, 2.0.0)
- Tag format: `v1.0.0` (with 'v' prefix)
- JitPack will use the tag name as version

## Testing Before Release

1. **Build locally**:
   ```bash
   ./gradlew :andorid-autopay-demo-lib:assembleRelease
   ```

2. **Test the AAR**:
   ```bash
   # AAR will be at:
   andorid-autopay-demo-lib/build/outputs/aar/andorid-autopay-demo-lib-release.aar
   ```

3. **Publish to local Maven**:
   ```bash
   ./gradlew :andorid-autopay-demo-lib:publishToMavenLocal
   ```

