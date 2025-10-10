# buildSrc

This directory contains shared build logic for the Probe multiplatform project.

## What's Included

### Core Classes
- **`AppConfig`** – Data class defining app configuration for different variants (OONI, DW, etc.)
- **`Organization`** – Sealed class representing build flavors/organizations, each with its own `AppConfig`

### Utilities
- **`BuildUtils`** – Utility functions for build scripts:
  - `isFdroidTaskRequested()` – Check if F-Droid build task is requested
  - `isDebugTaskRequested()` – Check if Debug build task is requested
  - Other helpers for platform suffixes, file copying, and .gitignore management
- **`TaskRegistration`** – Functions for registering custom Gradle tasks (Android, Desktop, Resources, etc.)

### Plugins
- **`ConfigurationPlugin`** – Main plugin for common configuration and automatic task registration

## How It Works
- The plugins and utilities in this directory are used to standardize build logic, configuration, and task setup across all multiplatform modules.
- Organization-specific configuration is handled via the `Organization` sealed class and its associated `AppConfig`.
- Custom tasks are registered using functions in `TaskRegistration`.
