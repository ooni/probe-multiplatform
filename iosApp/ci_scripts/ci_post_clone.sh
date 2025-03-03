#!/bin/sh

# Fail this script if any subcommand fails.
set -e

# Install CocoaPods using Homebrew.
HOMEBREW_NO_AUTO_UPDATE=1 # disable homebrew's automatic updates.
brew install cocoapods

brew install openjdk@17

# The default execution directory of this script is the ci_scripts directory.
cd $CI_PRIMARY_REPOSITORY_PATH # change working directory to the root of your cloned repo.

# Install CocoaPods dependencies.
cd iosApp && pod install # run `pod install` in the `ios` directory.

cd $CI_PRIMARY_REPOSITORY_PATH && ./gradlew podInstall -Porganization=$ORGANIZATION
