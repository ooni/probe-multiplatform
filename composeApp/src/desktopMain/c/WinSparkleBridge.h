#ifndef WINSPARKLE_BRIDGE_H
#define WINSPARKLE_BRIDGE_H

/**
 * Optionally set a root directory to search for WinSparkle DLLs.
 * Pass UTF-8 path or NULL to clear.
 */
void winsparkle_set_dll_root(const char* root_utf8);

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Log levels for callback logging
 */
typedef enum {
    WINSPARKLE_LOG_DEBUG = 0,
    WINSPARKLE_LOG_INFO = 1,
    WINSPARKLE_LOG_WARN = 2,
    WINSPARKLE_LOG_ERROR = 3
} WinSparkleLogLevel;

/**
 * Log callback function type
 * @param level The log level
 * @param operation The operation being performed (e.g., "init", "check_updates", "cleanup")
 * @param message The log message
 */
typedef void (*WinSparkleLogCallback)(WinSparkleLogLevel level, const char* operation, const char* message);

/**
 * Shutdown callback function type for update installation
 * Called when WinSparkle needs the application to shut down for update installation
 */
typedef void (*WinSparkleShutdownCallback)(void);

/**
 * Set log callback for receiving log messages
 * @param callback Function pointer to log callback, or NULL to disable
 */
void winsparkle_set_log_callback(WinSparkleLogCallback callback);

/**
 * Set shutdown callback for receiving shutdown requests during update installation
 * @param callback Function pointer to shutdown callback, or NULL to disable
 */
void winsparkle_set_shutdown_callback(WinSparkleShutdownCallback callback);

/**
 * Initialize WinSparkle updater with appcast URL
 * @param appcast_url The URL to the appcast feed (UTF-8 encoded)
 * @return 0 on success, non-zero on error
 */
int winsparkle_init(const char* appcast_url);

/**
 * Check for updates
 * @param show_ui Whether to show the update UI dialog
 * @return 0 on success, non-zero on error
 */
int winsparkle_check_for_updates(int show_ui);

/**
 * Set whether automatic update checks are enabled
 * @param enabled 1 to enable, 0 to disable
 * @return 0 on success, non-zero on error
 */
int winsparkle_set_automatic_check_enabled(int enabled);

/**
 * Set the update check interval
 * @param hours Interval in hours between automatic checks
 * @return 0 on success, non-zero on error
 */
int winsparkle_set_update_check_interval(int hours);

/**
 * Set application details manually (alternative to VERSIONINFO resources)
 * @param company_name Company name
 * @param app_name Application name
 * @param app_version Application version
 * @return 0 on success, non-zero on error
 */
int winsparkle_set_app_details(const char* company_name, const char* app_name, const char* app_version);

/**
 * Cleanup WinSparkle resources
 * @return 0 on success, non-zero on error
 */
int winsparkle_cleanup(void);

#ifdef __cplusplus
}
#endif

#endif // WINSPARKLE_BRIDGE_H