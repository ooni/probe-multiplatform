#ifndef SPARKLE_BRIDGE_H
#define SPARKLE_BRIDGE_H

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Log levels for callback logging
 */
typedef enum {
    SPARKLE_LOG_DEBUG = 0,
    SPARKLE_LOG_INFO = 1,
    SPARKLE_LOG_WARN = 2,
    SPARKLE_LOG_ERROR = 3
} SparkleLogLevel;

/**
 * Log callback function type
 * @param level The log level
 * @param operation The operation being performed (e.g., "init", "check_updates", "cleanup")
 * @param message The log message
 */
typedef void (*SparkleLogCallback)(SparkleLogLevel level, const char* operation, const char* message);

/**
 * Set log callback for receiving log messages
 * @param callback Function pointer to log callback, or NULL to disable
 */
void sparkle_set_log_callback(SparkleLogCallback callback);

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
 * @return 0 on success, non-zero on error
 */
int sparkle_check_for_updates(int show_ui);

/**
 * Set whether automatic update checks are enabled
 * @param enabled 1 to enable, 0 to disable
 * @return 0 on success, non-zero on error
 */
int sparkle_set_automatic_check_enabled(int enabled);

/**
 * Set the update check interval
 * @param hours Interval in hours between automatic checks
 * @return 0 on success, non-zero on error
 */
int sparkle_set_update_check_interval(int hours);

/**
 * Cleanup Sparkle resources
 * @return 0 on success, non-zero on error
 */
int sparkle_cleanup(void);

#ifdef __cplusplus
}
#endif

#endif // SPARKLE_BRIDGE_H