#include <windows.h>
#include <stdio.h>
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

static HMODULE winsparkle_dll = NULL;
static win_sparkle_init_func ws_init = NULL;
static win_sparkle_cleanup_func ws_cleanup = NULL;
static win_sparkle_set_appcast_url_func ws_set_appcast_url = NULL;
static win_sparkle_check_update_with_ui_func ws_check_update_with_ui = NULL;
static win_sparkle_check_update_without_ui_func ws_check_update_without_ui = NULL;
static win_sparkle_set_automatic_check_for_updates_func ws_set_automatic_check = NULL;
static win_sparkle_set_update_check_interval_func ws_set_update_interval = NULL;
static win_sparkle_set_app_details_func ws_set_app_details = NULL;

static int load_winsparkle_dll() {
    if (winsparkle_dll != NULL) {
        return 0; // Already loaded
    }
    
    winsparkle_dll = LoadLibraryA("WinSparkle.dll");
    if (winsparkle_dll == NULL) {
        printf("WinSparkleHelper: Failed to load WinSparkle.dll\n");
        return -1;
    }
    
    // Load function pointers
    ws_init = (win_sparkle_init_func)GetProcAddress(winsparkle_dll, "win_sparkle_init");
    ws_cleanup = (win_sparkle_cleanup_func)GetProcAddress(winsparkle_dll, "win_sparkle_cleanup");
    ws_set_appcast_url = (win_sparkle_set_appcast_url_func)GetProcAddress(winsparkle_dll, "win_sparkle_set_appcast_url");
    ws_check_update_with_ui = (win_sparkle_check_update_with_ui_func)GetProcAddress(winsparkle_dll, "win_sparkle_check_update_with_ui");
    ws_check_update_without_ui = (win_sparkle_check_update_without_ui_func)GetProcAddress(winsparkle_dll, "win_sparkle_check_update_without_ui");
    ws_set_automatic_check = (win_sparkle_set_automatic_check_for_updates_func)GetProcAddress(winsparkle_dll, "win_sparkle_set_automatic_check_for_updates");
    ws_set_update_interval = (win_sparkle_set_update_check_interval_func)GetProcAddress(winsparkle_dll, "win_sparkle_set_update_check_interval");
    ws_set_app_details = (win_sparkle_set_app_details_func)GetProcAddress(winsparkle_dll, "win_sparkle_set_app_details");
    
    if (!ws_init || !ws_cleanup || !ws_set_appcast_url || 
        !ws_check_update_with_ui || !ws_check_update_without_ui ||
        !ws_set_automatic_check || !ws_set_update_interval) {
        printf("WinSparkleHelper: Failed to load required functions\n");
        FreeLibrary(winsparkle_dll);
        winsparkle_dll = NULL;
        return -2;
    }
    
    return 0;
}

int winsparkle_init(const char* appcast_url) {
    if (load_winsparkle_dll() != 0) {
        return -1;
    }
    
    ws_set_appcast_url(appcast_url);
    ws_init();
    
    printf("WinSparkleHelper: Initialized with appcast URL: %s\n", appcast_url);
    return 0;
}

int winsparkle_check_for_updates(int show_ui) {
    if (winsparkle_dll == NULL) {
        printf("WinSparkleHelper: WinSparkle not initialized\n");
        return -1;
    }
    
    __try {
        if (show_ui) {
            ws_check_update_with_ui();
        } else {
            ws_check_update_without_ui();
        }
        return 0;
    } __except(EXCEPTION_EXECUTE_HANDLER) {
        printf("WinSparkleHelper: Exception occurred while checking for updates\n");
        return -2;
    }
}

int winsparkle_set_automatic_check_enabled(int enabled) {
    if (winsparkle_dll == NULL) {
        printf("WinSparkleHelper: WinSparkle not initialized\n");
        return -1;
    }
    
    __try {
        ws_set_automatic_check(enabled);
        return 0;
    } __except(EXCEPTION_EXECUTE_HANDLER) {
        printf("WinSparkleHelper: Exception occurred while setting automatic check\n");
        return -2;
    }
}

int winsparkle_set_update_check_interval(int hours) {
    if (winsparkle_dll == NULL) {
        printf("WinSparkleHelper: WinSparkle not initialized\n");
        return -1;
    }
    
    __try {
        int seconds = hours * 3600;
        ws_set_update_interval(seconds);
        return 0;
    } __except(EXCEPTION_EXECUTE_HANDLER) {
        printf("WinSparkleHelper: Exception occurred while setting update interval\n");
        return -2;
    }
}

int winsparkle_set_app_details(const char* company_name, const char* app_name, const char* app_version) {
    if (winsparkle_dll == NULL) {
        printf("WinSparkleHelper: WinSparkle not initialized\n");
        return -1;
    }
    
    if (ws_set_app_details == NULL) {
        printf("WinSparkleHelper: win_sparkle_set_app_details not available\n");
        return -3;
    }
    
    __try {
        // Convert UTF-8 to wide strings
        wchar_t company_wide[256];
        wchar_t app_wide[256];
        wchar_t version_wide[256];
        
        if (MultiByteToWideChar(CP_UTF8, 0, company_name, -1, company_wide, 256) == 0 ||
            MultiByteToWideChar(CP_UTF8, 0, app_name, -1, app_wide, 256) == 0 ||
            MultiByteToWideChar(CP_UTF8, 0, app_version, -1, version_wide, 256) == 0) {
            printf("WinSparkleHelper: Failed to convert strings to wide chars\n");
            return -4;
        }
        
        ws_set_app_details(company_wide, app_wide, version_wide);
        return 0;
    } __except(EXCEPTION_EXECUTE_HANDLER) {
        printf("WinSparkleHelper: Exception occurred while setting app details\n");
        return -2;
    }
}

int winsparkle_cleanup(void) {
    if (winsparkle_dll != NULL) {
        printf("WinSparkleHelper: Cleaning up\n");
        __try {
            ws_cleanup();
            FreeLibrary(winsparkle_dll);
            winsparkle_dll = NULL;
            return 0;
        } __except(EXCEPTION_EXECUTE_HANDLER) {
            printf("WinSparkleHelper: Exception occurred during cleanup\n");
            winsparkle_dll = NULL;
            return -2;
        }
    }
    return 0;
}