#ifndef WINSPARKLE_BRIDGE_H
#define WINSPARKLE_BRIDGE_H

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Initialize WinSparkle updater with appcast URL
 * @param appcast_url The URL to the appcast feed (UTF-8 encoded)
 * @return 0 on success, non-zero on error
 */
int winsparkle_init(const char* appcast_url);

/**
 * Check for updates
 * @param show_ui Whether to show the update UI dialog
 */
void winsparkle_check_for_updates(int show_ui);

/**
 * Set whether automatic update checks are enabled
 * @param enabled 1 to enable, 0 to disable
 */
void winsparkle_set_automatic_check_enabled(int enabled);

/**
 * Set the update check interval
 * @param hours Interval in hours between automatic checks
 */
void winsparkle_set_update_check_interval(int hours);

/**
 * Set application details manually (alternative to VERSIONINFO resources)
 * @param company_name Company name
 * @param app_name Application name
 * @param app_version Application version
 */
void winsparkle_set_app_details(const char* company_name, const char* app_name, const char* app_version);

/**
 * Cleanup WinSparkle resources
 */
void winsparkle_cleanup(void);

#ifdef __cplusplus
}
#endif

#endif // WINSPARKLE_BRIDGE_H