#include <windows.h>
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include "WinSparkleBridge.h"

// WinSparkle function pointers
typedef void (__stdcall *win_sparkle_init_func)(void);
typedef void (__stdcall *win_sparkle_cleanup_func)(void);
typedef void (__stdcall *win_sparkle_set_appcast_url_func)(const char* url);
typedef void (__stdcall *win_sparkle_check_update_with_ui_func)(void);
typedef void (__stdcall *win_sparkle_check_update_without_ui_func)(void);
typedef void (__stdcall *win_sparkle_set_automatic_check_for_updates_func)(int state);
typedef void (__stdcall *win_sparkle_set_update_check_interval_func)(int interval);
typedef void (__stdcall *win_sparkle_set_app_details_func)(const wchar_t* company_name, const wchar_t* app_name, const wchar_t* app_version);
typedef void (__stdcall *win_sparkle_set_can_shutdown_callback_func)(int (__stdcall *)(void));
typedef void (__stdcall *win_sparkle_set_shutdown_request_callback_func)(void (__stdcall *)(void));

static HMODULE winsparkle_dll = NULL;
static WinSparkleLogCallback logCallback = NULL;
static WinSparkleShutdownCallback shutdownCallback = NULL;
static char g_dll_root[MAX_PATH] = {0};

// Function pointers
static win_sparkle_init_func ws_init = NULL;
static win_sparkle_cleanup_func ws_cleanup = NULL;
static win_sparkle_set_appcast_url_func ws_set_appcast_url = NULL;
static win_sparkle_check_update_with_ui_func ws_check_update_with_ui = NULL;
static win_sparkle_check_update_without_ui_func ws_check_update_without_ui = NULL;
static win_sparkle_set_automatic_check_for_updates_func ws_set_automatic_check = NULL;
static win_sparkle_set_update_check_interval_func ws_set_update_interval = NULL;
static win_sparkle_set_app_details_func ws_set_app_details = NULL;
static win_sparkle_set_can_shutdown_callback_func ws_set_can_shutdown_callback = NULL;
static win_sparkle_set_shutdown_request_callback_func ws_set_shutdown_request_callback = NULL;

// Internal logging function that handles both printf and callback
static void winsparkle_log(WinSparkleLogLevel level, const char* operation, const char* format, ...) {
    va_list args;
    va_start(args, format);

    // Create message string
    char message[1024];
    vsnprintf(message, sizeof(message), format, args);
    va_end(args);

    // Always log to stdout with level prefix
    const char* levelStr = "";
    switch (level) {
        case WINSPARKLE_LOG_DEBUG: levelStr = "DEBUG"; break;
        case WINSPARKLE_LOG_INFO:  levelStr = "INFO";  break;
        case WINSPARKLE_LOG_WARN:  levelStr = "WARN";  break;
        case WINSPARKLE_LOG_ERROR: levelStr = "ERROR"; break;
    }

    printf("WinSparkleHelper [%s] %s: %s\n", levelStr, operation, message);
    fflush(stdout);

    // Call callback if set
    if (logCallback != NULL) {
        logCallback(level, operation, message);
    }
}

// Callbacks for WinSparkle state tracking
static int __stdcall can_shutdown_callback(void) {
    winsparkle_log(WINSPARKLE_LOG_INFO, "update_lifecycle", "Application can shutdown for update installation");
    return 1; // Allow shutdown
}

static void __stdcall shutdown_request_callback(void) {
    winsparkle_log(WINSPARKLE_LOG_INFO, "update_lifecycle", "Shutdown requested for update installation");
    // Call the shutdown callback if set
    if (shutdownCallback != NULL) {
        winsparkle_log(WINSPARKLE_LOG_INFO, "update_lifecycle", "Calling application shutdown callback");
        shutdownCallback();
    }
}

void winsparkle_set_log_callback(WinSparkleLogCallback callback) {
    logCallback = callback;
    winsparkle_log(WINSPARKLE_LOG_INFO, "callback", "Log callback %s", callback ? "enabled" : "disabled");
}

void winsparkle_set_shutdown_callback(WinSparkleShutdownCallback callback) {
    shutdownCallback = callback;
    winsparkle_log(WINSPARKLE_LOG_INFO, "callback", "Shutdown callback %s", callback ? "enabled" : "disabled");
}

void winsparkle_set_dll_root(const char* root_utf8) {
    if (root_utf8 == NULL || root_utf8[0] == '\0') {
        g_dll_root[0] = '\0';
        winsparkle_log(WINSPARKLE_LOG_INFO, "dll_root", "Cleared DLL root");
        return;
    }
    // Copy with truncation safety
    size_t len = strlen(root_utf8);
    if (len >= MAX_PATH) len = MAX_PATH - 1;
    memcpy(g_dll_root, root_utf8, len);
    g_dll_root[len] = '\0';
    winsparkle_log(WINSPARKLE_LOG_INFO, "dll_root", "Set DLL root to: %s", g_dll_root);

    // Add the DLL root directory to the DLL search path to ensure dependencies like
    // libwinpthread-1.dll can be found when loading WinSparkle.dll
    if (SetDllDirectoryA(g_dll_root)) {
        winsparkle_log(WINSPARKLE_LOG_INFO, "dll_root", "Successfully added DLL directory to search path");
    } else {
        DWORD error = GetLastError();
        winsparkle_log(WINSPARKLE_LOG_WARN, "dll_root", "Failed to set DLL directory (error %lu)", error);
    }
}

static int load_winsparkle_dll() {
    if (winsparkle_dll != NULL) {
        winsparkle_log(WINSPARKLE_LOG_DEBUG, "dll_load", "WinSparkle.dll already loaded");
        return 0; // Already loaded
    }

    // Detect system architecture and try loading the appropriate DLL
    SYSTEM_INFO sysInfo;
    GetNativeSystemInfo(&sysInfo);

    const char* dllNames[] = {NULL, NULL, NULL}; // Will hold up to 3 DLL names to try
    int dllCount = 0;

    dllNames[dllCount++] = "WinSparkle.dll";

    // Try loading DLLs in order of preference, first from g_dll_root if set
    DWORD lastError = 0;
    for (int i = 0; i < dllCount && dllNames[i] != NULL; i++) {
        winsparkle_log(WINSPARKLE_LOG_INFO, "dll_load", "Attempting to load %s", dllNames[i]);

        if (g_dll_root[0] != '\0') {
            char candidate[MAX_PATH];
            // Build path g_dll_root + '\\' + dllName
            snprintf(candidate, MAX_PATH, "%s\\%s", g_dll_root, dllNames[i]);
            winsparkle_log(WINSPARKLE_LOG_DEBUG, "dll_load", "Trying from root: %s", candidate);
            winsparkle_dll = LoadLibraryA(candidate);
        }

        if (winsparkle_dll == NULL) {
            winsparkle_dll = LoadLibraryA(dllNames[i]);
        }

        if (winsparkle_dll != NULL) {
            winsparkle_log(WINSPARKLE_LOG_INFO, "dll_load", "Successfully loaded %s", dllNames[i]);
            char modPath[MAX_PATH];
            DWORD n = GetModuleFileNameA(winsparkle_dll, modPath, MAX_PATH);
            if (n == 0) {
                DWORD err = GetLastError();
                winsparkle_log(WINSPARKLE_LOG_WARN, "dll_load", "Loaded but could not resolve path (err %lu)", err);
            } else {
                if (n >= MAX_PATH) {
                    winsparkle_log(WINSPARKLE_LOG_WARN, "dll_load", "Resolved path may be truncated: %s", modPath);
                }
                winsparkle_log(WINSPARKLE_LOG_INFO, "dll_load", "Loaded from: %s", modPath);
            }
            break;
        } else {
            lastError = GetLastError();
            winsparkle_log(WINSPARKLE_LOG_WARN, "dll_load", "Failed to load %s (error %lu)", dllNames[i], lastError);
        }
    }

    if (winsparkle_dll == NULL) {
        winsparkle_log(WINSPARKLE_LOG_ERROR, "dll_load", "Failed to load any WinSparkle DLL (last error %lu)", lastError);
        return -1;
    }

    winsparkle_log(WINSPARKLE_LOG_DEBUG, "dll_load", "Loading function pointers");

    // Load function pointers
    ws_init = (win_sparkle_init_func)GetProcAddress(winsparkle_dll, "win_sparkle_init");
    ws_cleanup = (win_sparkle_cleanup_func)GetProcAddress(winsparkle_dll, "win_sparkle_cleanup");
    ws_set_appcast_url = (win_sparkle_set_appcast_url_func)GetProcAddress(winsparkle_dll, "win_sparkle_set_appcast_url");
    ws_check_update_with_ui = (win_sparkle_check_update_with_ui_func)GetProcAddress(winsparkle_dll, "win_sparkle_check_update_with_ui");
    ws_check_update_without_ui = (win_sparkle_check_update_without_ui_func)GetProcAddress(winsparkle_dll, "win_sparkle_check_update_without_ui");
    ws_set_automatic_check = (win_sparkle_set_automatic_check_for_updates_func)GetProcAddress(winsparkle_dll, "win_sparkle_set_automatic_check_for_updates");
    ws_set_update_interval = (win_sparkle_set_update_check_interval_func)GetProcAddress(winsparkle_dll, "win_sparkle_set_update_check_interval");
    ws_set_app_details = (win_sparkle_set_app_details_func)GetProcAddress(winsparkle_dll, "win_sparkle_set_app_details");
    ws_set_can_shutdown_callback = (win_sparkle_set_can_shutdown_callback_func)GetProcAddress(winsparkle_dll, "win_sparkle_set_can_shutdown_callback");
    ws_set_shutdown_request_callback = (win_sparkle_set_shutdown_request_callback_func)GetProcAddress(winsparkle_dll, "win_sparkle_set_shutdown_request_callback");

    if (!ws_init || !ws_cleanup || !ws_set_appcast_url ||
        !ws_check_update_with_ui || !ws_check_update_without_ui ||
        !ws_set_automatic_check || !ws_set_update_interval) {
        winsparkle_log(WINSPARKLE_LOG_ERROR, "dll_load", "Failed to load required functions from WinSparkle.dll");
        FreeLibrary(winsparkle_dll);
        winsparkle_dll = NULL;
        return -2;
    }

    winsparkle_log(WINSPARKLE_LOG_INFO, "dll_load", "Successfully loaded WinSparkle.dll and all required functions");
    return 0;
}

int winsparkle_init(const char* appcast_url) {
    winsparkle_log(WINSPARKLE_LOG_INFO, "init", "Starting WinSparkle initialization");

    if (load_winsparkle_dll() != 0) {
        winsparkle_log(WINSPARKLE_LOG_ERROR, "init", "Failed to load WinSparkle.dll");
        return -1;
    }

    winsparkle_log(WINSPARKLE_LOG_INFO, "init", "Using appcast URL: %s", appcast_url);

    __try {
        ws_set_appcast_url(appcast_url);
        winsparkle_log(WINSPARKLE_LOG_DEBUG, "init", "Set appcast URL successfully");

        // Set up callbacks for state tracking
        if (ws_set_can_shutdown_callback && ws_set_shutdown_request_callback) {
            ws_set_can_shutdown_callback(can_shutdown_callback);
            ws_set_shutdown_request_callback(shutdown_request_callback);
            winsparkle_log(WINSPARKLE_LOG_DEBUG, "init", "Set up lifecycle callbacks");
        } else {
            winsparkle_log(WINSPARKLE_LOG_WARN, "init", "Lifecycle callbacks not available in this WinSparkle version");
        }

        ws_init();
        winsparkle_log(WINSPARKLE_LOG_INFO, "init", "Successfully initialized WinSparkle");
        return 0;
    } __except(EXCEPTION_EXECUTE_HANDLER) {
        winsparkle_log(WINSPARKLE_LOG_ERROR, "init", "Exception occurred during initialization");
        return -5;
    }
}

int winsparkle_check_for_updates(int show_ui) {
    winsparkle_log(WINSPARKLE_LOG_INFO, "check_updates", "Starting update check (show_ui: %s)", show_ui ? "true" : "false");

    if (winsparkle_dll == NULL) {
        winsparkle_log(WINSPARKLE_LOG_ERROR, "check_updates", "WinSparkle not initialized");
        return -1;
    }

    __try {
        if (show_ui) {
            winsparkle_log(WINSPARKLE_LOG_DEBUG, "check_updates", "Checking for updates with UI");
            ws_check_update_with_ui();
        } else {
            winsparkle_log(WINSPARKLE_LOG_DEBUG, "check_updates", "Checking for updates in background");
            ws_check_update_without_ui();
        }
        winsparkle_log(WINSPARKLE_LOG_INFO, "check_updates", "Update check initiated successfully");
        return 0;
    } __except(EXCEPTION_EXECUTE_HANDLER) {
        winsparkle_log(WINSPARKLE_LOG_ERROR, "check_updates", "Exception occurred during update check");
        return -2;
    }
}

int winsparkle_set_automatic_check_enabled(int enabled) {
    winsparkle_log(WINSPARKLE_LOG_INFO, "set_automatic", "Setting automatic checks to: %s", enabled ? "enabled" : "disabled");

    if (winsparkle_dll == NULL) {
        winsparkle_log(WINSPARKLE_LOG_ERROR, "set_automatic", "WinSparkle not initialized");
        return -1;
    }

    __try {
        ws_set_automatic_check(enabled);
        winsparkle_log(WINSPARKLE_LOG_INFO, "set_automatic", "Automatic checks successfully %s", enabled ? "enabled" : "disabled");
        return 0;
    } __except(EXCEPTION_EXECUTE_HANDLER) {
        winsparkle_log(WINSPARKLE_LOG_ERROR, "set_automatic", "Exception occurred while setting automatic check");
        return -2;
    }
}

int winsparkle_set_update_check_interval(int hours) {
    winsparkle_log(WINSPARKLE_LOG_INFO, "set_interval", "Setting update check interval to %d hours", hours);

    if (winsparkle_dll == NULL) {
        winsparkle_log(WINSPARKLE_LOG_ERROR, "set_interval", "WinSparkle not initialized");
        return -1;
    }

    __try {
        int seconds = hours * 3600;
        ws_set_update_interval(seconds);
        winsparkle_log(WINSPARKLE_LOG_INFO, "set_interval", "Update check interval set to %d hours (%d seconds)", hours, seconds);
        return 0;
    } __except(EXCEPTION_EXECUTE_HANDLER) {
        winsparkle_log(WINSPARKLE_LOG_ERROR, "set_interval", "Exception occurred while setting update interval");
        return -2;
    }
}

int winsparkle_set_app_details(const char* company_name, const char* app_name, const char* app_version) {
    winsparkle_log(WINSPARKLE_LOG_INFO, "set_app_details", "Setting app details: %s / %s / %s", company_name, app_name, app_version);

    if (winsparkle_dll == NULL) {
        winsparkle_log(WINSPARKLE_LOG_ERROR, "set_app_details", "WinSparkle not initialized");
        return -1;
    }

    if (ws_set_app_details == NULL) {
        winsparkle_log(WINSPARKLE_LOG_ERROR, "set_app_details", "win_sparkle_set_app_details not available in this WinSparkle version");
        return -3;
    }

    __try {
        // Convert UTF-8 to wide strings
        wchar_t company_wide[256];
        wchar_t app_wide[256];
        wchar_t version_wide[256];

        winsparkle_log(WINSPARKLE_LOG_DEBUG, "set_app_details", "Converting strings to wide characters");

        if (MultiByteToWideChar(CP_UTF8, 0, company_name, -1, company_wide, 256) == 0 ||
            MultiByteToWideChar(CP_UTF8, 0, app_name, -1, app_wide, 256) == 0 ||
            MultiByteToWideChar(CP_UTF8, 0, app_version, -1, version_wide, 256) == 0) {
            DWORD error = GetLastError();
            winsparkle_log(WINSPARKLE_LOG_ERROR, "set_app_details", "Failed to convert strings to wide chars (error %lu)", error);
            return -4;
        }

        ws_set_app_details(company_wide, app_wide, version_wide);
        winsparkle_log(WINSPARKLE_LOG_INFO, "set_app_details", "App details set successfully");
        return 0;
    } __except(EXCEPTION_EXECUTE_HANDLER) {
        winsparkle_log(WINSPARKLE_LOG_ERROR, "set_app_details", "Exception occurred while setting app details");
        return -2;
    }
}

int winsparkle_cleanup(void) {
    winsparkle_log(WINSPARKLE_LOG_INFO, "cleanup", "Starting WinSparkle cleanup");

    if (winsparkle_dll != NULL) {
        __try {
            winsparkle_log(WINSPARKLE_LOG_DEBUG, "cleanup", "Calling WinSparkle cleanup");
            ws_cleanup();

            winsparkle_log(WINSPARKLE_LOG_DEBUG, "cleanup", "Freeing WinSparkle.dll");
            FreeLibrary(winsparkle_dll);
            winsparkle_dll = NULL;

            // Clear function pointers
            ws_init = NULL;
            ws_cleanup = NULL;
            ws_set_appcast_url = NULL;
            ws_check_update_with_ui = NULL;
            ws_check_update_without_ui = NULL;
            ws_set_automatic_check = NULL;
            ws_set_update_interval = NULL;
            ws_set_app_details = NULL;
            ws_set_can_shutdown_callback = NULL;
            ws_set_shutdown_request_callback = NULL;

            winsparkle_log(WINSPARKLE_LOG_INFO, "cleanup", "WinSparkle cleanup completed successfully");
            return 0;
        } __except(EXCEPTION_EXECUTE_HANDLER) {
            winsparkle_log(WINSPARKLE_LOG_ERROR, "cleanup", "Exception occurred during cleanup");
            winsparkle_dll = NULL;
            return -2;
        }
    } else {
        winsparkle_log(WINSPARKLE_LOG_DEBUG, "cleanup", "WinSparkle was not initialized, nothing to clean up");
    }
    return 0;
}
