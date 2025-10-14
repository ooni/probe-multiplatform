# Makefile for NetworkTypeFinder and UpdateBridge JNI Bridges

# Variables
COMPILER = clang
NETWORK_FILES = c/NetworkTypeFinder.m
UPDATE_FILES_MAC = c/UpdateBridge.c c/SparkeBridge.m
UPDATE_FILES_WIN = c/UpdateBridge.c c/WinSparkleBridge.c
NETWORK_LIBRARY_NAME = networktypefinder
UPDATE_LIBRARY_NAME = updatebridge
NETWORK_LIBRARY_FILE_MAC = lib$(NETWORK_LIBRARY_NAME).dylib
NETWORK_LIBRARY_FILE_LINUX = lib$(NETWORK_LIBRARY_NAME).so
UPDATE_LIBRARY_FILE_MAC = lib$(UPDATE_LIBRARY_NAME).dylib
UPDATE_LIBRARY_FILE_WIN = $(UPDATE_LIBRARY_NAME).dll
JAVA_HOME ?= $(shell java -XshowSettings:properties -version 2>&1 > /dev/null | grep 'java.home' | awk '{print $$3}')
RESOURCES_BASE_DIR = resources

# Detect platform
UNAME_S := $(shell uname -s)

ifeq ($(UNAME_S),Darwin)
PLATFORM_TARGET := macos
NETWORK_LIBRARY_FILE := $(NETWORK_LIBRARY_FILE_MAC)
UPDATE_LIBRARY_FILE := $(UPDATE_LIBRARY_FILE_MAC)
else ifeq ($(UNAME_S),Linux)
PLATFORM_TARGET := linux
NETWORK_LIBRARY_FILE := $(NETWORK_LIBRARY_FILE_LINUX)
UPDATE_LIBRARY_FILE :=
else ifeq ($(OS),Windows_NT)
PLATFORM_TARGET := windows
NETWORK_LIBRARY_FILE := $(NETWORK_LIBRARY_NAME).dll
UPDATE_LIBRARY_FILE := $(UPDATE_LIBRARY_FILE_WIN)
else
PLATFORM_TARGET := unknown
NETWORK_LIBRARY_FILE :=
UPDATE_LIBRARY_FILE :=
endif

# Default target: build only for detected platform
all: $(PLATFORM_TARGET)

macos:
	@echo "Compiling NetworkTypeFinder for macOS..."
	@mkdir -p $(RESOURCES_BASE_DIR)/macos
	# Ensure Sparkle.framework exists in the processed resources location
	@if [ ! -d "../../build/processedResources/desktop/main/macos/Sparkle.framework" ]; then \
		echo "❌ Sparkle.framework not found in build/processedResources/desktop/main/macos"; \
		echo "   Run: ./gradlew :composeApp:setupSparkle"; \
		exit 1; \
	fi
	$(COMPILER) -dynamiclib -o $(RESOURCES_BASE_DIR)/macos/$(NETWORK_LIBRARY_FILE_MAC) $(NETWORK_FILES) -framework Foundation -framework SystemConfiguration -framework CoreFoundation -framework Network -framework CFNetwork -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/darwin
	@echo "NetworkTypeFinder library created at $(RESOURCES_BASE_DIR)/macos/$(NETWORK_LIBRARY_FILE_MAC)"
	@echo "Compiling UpdateBridge for macOS..."
	@mkdir -p $(RESOURCES_BASE_DIR)/macos
	$(COMPILER) -dynamiclib -o $(RESOURCES_BASE_DIR)/macos/$(UPDATE_LIBRARY_FILE_MAC) $(UPDATE_FILES_MAC) \
		-framework Foundation \
		-F $(RESOURCES_BASE_DIR)/macos \
		-F ../../build/processedResources/desktop/main/macos \
		-framework Sparkle \
		-I$(JAVA_HOME)/include \
		-I$(JAVA_HOME)/include/darwin \
		-I ../../build/processedResources/desktop/main/macos/Sparkle.framework/Headers \
		-install_name @loader_path/lib$(UPDATE_LIBRARY_NAME).dylib \
		-Wl,-rpath,@loader_path
	# Fix Sparkle framework reference to use @loader_path
	install_name_tool -change "@rpath/Sparkle.framework/Versions/B/Sparkle" "@loader_path/Sparkle.framework/Versions/B/Sparkle" $(RESOURCES_BASE_DIR)/macos/$(UPDATE_LIBRARY_FILE_MAC)
	@echo "UpdateBridge library created at $(RESOURCES_BASE_DIR)/macos/$(UPDATE_LIBRARY_FILE_MAC)"

linux:
	@echo "Compiling NetworkTypeFinder for Linux..."
	@mkdir -p $(RESOURCES_BASE_DIR)/linux
	$(COMPILER) -shared -fPIC -o $(RESOURCES_BASE_DIR)/linux/$(NETWORK_LIBRARY_FILE_LINUX) $(NETWORK_FILES) -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux
	@echo "NetworkTypeFinder library created at $(RESOURCES_BASE_DIR)/linux/$(NETWORK_LIBRARY_FILE_LINUX)"
	@echo "UpdateBridge not supported on Linux"

windows:
	@echo "Compiling NetworkTypeFinder for Windows..."
	@mkdir -p $(RESOURCES_BASE_DIR)/windows
	$(COMPILER) -shared -m64 -o $(RESOURCES_BASE_DIR)/windows/$(NETWORK_LIBRARY_NAME).dll $(NETWORK_FILES) '-I$(JAVA_HOME)\\include' '-I$(JAVA_HOME)\\include\\win32' -lws2_32 -liphlpapi
	@echo "NetworkTypeFinder library created at $(RESOURCES_BASE_DIR)/windows/$(NETWORK_LIBRARY_NAME).dll"
	@echo "Compiling UpdateBridge for Windows..."
	$(COMPILER) -shared -m64 -o $(RESOURCES_BASE_DIR)/windows/$(UPDATE_LIBRARY_FILE_WIN) $(UPDATE_FILES_WIN) '-I$(JAVA_HOME)\\include' '-I$(JAVA_HOME)\\include\\win32'
	@echo "UpdateBridge library created at $(RESOURCES_BASE_DIR)/windows/$(UPDATE_LIBRARY_FILE_WIN)"

# Install the library to system path
install: all
	@echo "Installing libraries to system path..."
ifeq ($(PLATFORM_TARGET),macos)
	cp $(RESOURCES_BASE_DIR)/macos/$(NETWORK_LIBRARY_FILE_MAC) $(HOME)/Library/Java/Extensions
	cp $(RESOURCES_BASE_DIR)/macos/$(UPDATE_LIBRARY_FILE_MAC) $(HOME)/Library/Java/Extensions
else ifeq ($(PLATFORM_TARGET),linux)
	sudo cp $(RESOURCES_BASE_DIR)/linux/$(NETWORK_LIBRARY_FILE_LINUX) /usr/lib/
else ifeq ($(PLATFORM_TARGET),windows)
	copy $(RESOURCES_BASE_DIR)\\windows\\$(NETWORK_LIBRARY_NAME).dll %JAVA_HOME%\\bin
	copy $(RESOURCES_BASE_DIR)\\windows\\$(UPDATE_LIBRARY_FILE_WIN) %JAVA_HOME%\\bin
else
	@echo "Unknown platform, not installing."
endif
	@echo "Libraries installed"

clean:
	@echo "Cleaning build artifacts..."
	# Preserve Sparkle framework during clean
	@if [ -d "$(RESOURCES_BASE_DIR)/macos/Sparkle.framework" ]; then \
		cp -R $(RESOURCES_BASE_DIR)/macos/Sparkle.framework /tmp/sparkle-backup; \
	fi
	rm -rf $(RESOURCES_BASE_DIR)
	@if [ -d "/tmp/sparkle-backup" ]; then \
		mkdir -p $(RESOURCES_BASE_DIR)/macos; \
		cp -R /tmp/sparkle-backup $(RESOURCES_BASE_DIR)/macos/Sparkle.framework; \
		rm -rf /tmp/sparkle-backup; \
	fi
	@echo "Clean complete"

help:
	@echo "NetworkTypeFinder and UpdateBridge JNI Bridges Makefile"
	@echo ""
	@echo "Targets:"
	@echo "  all (default): Compile both libraries for the current platform"
	@echo "  install: Install the libraries to system path for the current platform"
	@echo "  clean: Clean build artifacts"
	@echo "  help: Show this help message"
	@echo ""
	@echo "Libraries built:"
	@echo "  NetworkTypeFinder: Network type detection"
	@echo "  UpdateBridge: Sparkle (macOS) / WinSparkle (Windows) integration"
	@echo ""
	@echo "Example usage:"
	@echo "  make all      # Compile the libraries for your platform"
	@echo "  make install  # Install the libraries for your platform"

.PHONY: all macos linux windows install clean help
