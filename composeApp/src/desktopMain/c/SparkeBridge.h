#ifndef SPARKLE_BRIDGE_H
#define SPARKLE_BRIDGE_H

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Initialize Sparkle updater with appcast URL and optional public key
 * @param appcast_url The URL to the appcast feed (UTF-8 encoded)
 * @param public_key The EdDSA public key for signature verification (UTF-8 encoded, can be NULL)
 * @return 0 on success, non-zero on error
 */
int sparkle_init(const char* appcast_url, const char* public_key);

/**
 * Check for updates
 * @param show_ui Whether to show the update UI dialog
 */
void sparkle_check_for_updates(int show_ui);

/**
 * Set whether automatic update checks are enabled
 * @param enabled 1 to enable, 0 to disable
 */
void sparkle_set_automatic_check_enabled(int enabled);

/**
 * Set the update check interval
 * @param hours Interval in hours between automatic checks
 */
void sparkle_set_update_check_interval(int hours);

/**
 * Cleanup Sparkle resources
 */
void sparkle_cleanup(void);

#ifdef __cplusplus
}
#endif

#endif // SPARKLE_BRIDGE_H