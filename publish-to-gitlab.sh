#!/bin/bash

# ============================================
# GitLab Maven Publishing Script
# Publishes UPI AutoPay SDK to GitLab Maven Repository
# ============================================

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  UPI AutoPay SDK - GitLab Maven Publisher${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# Check if GITLAB_PRIVATE_TOKEN is set
if [ -z "$GITLAB_PRIVATE_TOKEN" ]; then
    echo -e "${YELLOW}⚠️  Warning: GITLAB_PRIVATE_TOKEN is not set${NC}"
    echo -e "${YELLOW}Please set it using one of these methods:${NC}"
    echo -e "${YELLOW}  1. Export in terminal: export GITLAB_PRIVATE_TOKEN=your_token${NC}"
    echo -e "${YELLOW}  2. Add to ~/.gradle/gradle.properties: GITLAB_PRIVATE_TOKEN=your_token${NC}"
    echo ""
    read -p "Do you want to enter the token now? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        read -sp "Enter GitLab Private Token: " token
        echo
        export GITLAB_PRIVATE_TOKEN=$token
        echo -e "${GREEN}✓ Token set for this session${NC}"
    else
        echo -e "${RED}✗ Cannot proceed without token. Exiting.${NC}"
        exit 1
    fi
fi

# Get current version from gradle.properties
VERSION=$(grep "VERSION_NAME=" gradle.properties | cut -d'=' -f2)
echo -e "${BLUE}Current version: ${GREEN}${VERSION}${NC}"
echo ""

# Ask if user wants to bump version
read -p "Do you want to change the version? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Current version: $VERSION"
    read -p "Enter new version (e.g., 1.0.1): " new_version
    if [ ! -z "$new_version" ]; then
        # Update version in gradle.properties
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            sed -i '' "s/VERSION_NAME=.*/VERSION_NAME=$new_version/" gradle.properties
        else
            # Linux
            sed -i "s/VERSION_NAME=.*/VERSION_NAME=$new_version/" gradle.properties
        fi
        VERSION=$new_version
        echo -e "${GREEN}✓ Version updated to $VERSION${NC}"
    fi
fi
echo ""

# Confirm publication
echo -e "${YELLOW}About to publish:${NC}"
echo -e "  Group ID: ${BLUE}com.decentro.autopay${NC}"
echo -e "  Artifact ID: ${BLUE}upi-autopay-sdk${NC}"
echo -e "  Version: ${BLUE}${VERSION}${NC}"
echo -e "  Repository: ${BLUE}GitLab (decentro1/decentro-autopay-android)${NC}"
echo ""
read -p "Continue with publication? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${RED}✗ Publication cancelled${NC}"
    exit 0
fi

echo ""
echo -e "${BLUE}Step 1: Cleaning previous builds...${NC}"
./gradlew :andorid-autopay-demo-lib:clean
echo -e "${GREEN}✓ Clean completed${NC}"
echo ""

echo -e "${BLUE}Step 2: Building release AAR...${NC}"
./gradlew :andorid-autopay-demo-lib:assembleRelease
echo -e "${GREEN}✓ Build completed${NC}"
echo ""

echo -e "${BLUE}Step 3: Publishing to GitLab Maven...${NC}"
./gradlew :andorid-autopay-demo-lib:publishMavenPublicationToGitLabRepository
echo -e "${GREEN}✓ Publication completed${NC}"
echo ""

# Success message
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  ✓ SDK Published Successfully!${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "${BLUE}Published Details:${NC}"
echo -e "  Dependency: ${GREEN}implementation 'com.decentro.autopay:upi-autopay-sdk:${VERSION}'${NC}"
echo -e "  Repository URL: ${GREEN}https://gitlab.com/api/v4/projects/77552064/packages/maven${NC}"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo -e "  1. Verify publication at: ${BLUE}https://gitlab.com/decentro1/decentro-autopay-android/-/packages${NC}"
echo -e "  2. Create Git tag: ${BLUE}git tag v${VERSION} && git push origin v${VERSION}${NC}"
echo -e "  3. Update changelog/release notes"
echo -e "  4. Notify consumers about the new version"
echo ""
echo -e "${GREEN}Done!${NC}"

