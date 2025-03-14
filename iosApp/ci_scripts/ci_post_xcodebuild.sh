#!/bin/sh

# Fail this script if any subcommand fails.
set -e

cd $CI_PRIMARY_REPOSITORY_PATH # change working directory to the root of your cloned repo.

bundle exec fastlane sentry_upload_debug_symbols auth_token:$SENTRY_AUTH_TOKEN org_slug:ooni project_slug:probe-multiplatform-ios path: $CI_ARCHIVE_PATH
